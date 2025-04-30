package io.quarkus.cyclonedx.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.License;
import org.cyclonedx.model.LicenseChoice;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Pedigree;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Tool;
import org.cyclonedx.model.component.evidence.Occurrence;
import org.cyclonedx.model.metadata.ToolInformation;
import org.cyclonedx.util.BomUtils;
import org.cyclonedx.util.LicenseResolver;
import org.jboss.logging.Logger;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;

import io.quarkus.bootstrap.app.SbomResult;
import io.quarkus.bootstrap.resolver.maven.EffectiveModelResolver;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.paths.PathTree;
import io.quarkus.sbom.ApplicationComponent;
import io.quarkus.sbom.ApplicationManifest;

public class CycloneDxSbomGenerator {

    private static final Logger log = Logger.getLogger(CycloneDxSbomGenerator.class);

    private static final String QUARKUS_COMPONENT_SCOPE = "quarkus:component:scope";
    private static final String QUARKUS_COMPONENT_LOCATION = "quarkus:component:location";

    private static final Comparator<ArtifactCoords> ARTIFACT_COORDS_COMPARATOR = (c1, c2) -> {
        var i = c1.getGroupId().compareTo(c2.getGroupId());
        if (i != 0) {
            return i;
        }
        i = c1.getArtifactId().compareTo(c2.getArtifactId());
        if (i != 0) {
            return i;
        }
        i = c1.getVersion().compareTo(c2.getVersion());
        if (i != 0) {
            return i;
        }
        i = c1.getClassifier().compareTo(c2.getClassifier());
        if (i != 0) {
            return i;
        }
        return c1.getType().compareTo(c2.getType());
    };

    private static final String CLASSIFIER_CYCLONEDX = "cyclonedx";
    private static final String FORMAT_ALL = "all";
    private static final String FORMAT_JSON = "json";
    private static final String FORMAT_XML = "xml";
    private static final String DEFAULT_FORMAT = FORMAT_JSON;
    private static final List<String> SUPPORTED_FORMATS = List.of(FORMAT_JSON, FORMAT_XML);

    public static CycloneDxSbomGenerator newInstance() {
        return new CycloneDxSbomGenerator();
    }

    private boolean generated;
    private ApplicationManifest manifest;
    private Path outputDir;
    private Path outputFile;
    private String schemaVersion;
    private String format;
    private EffectiveModelResolver modelResolver;
    private boolean includeLicenseText;

    private Version effectiveSchemaVersion;

    private CycloneDxSbomGenerator() {
    }

    public CycloneDxSbomGenerator setManifest(ApplicationManifest manifest) {
        ensureNotGenerated();
        this.manifest = manifest;
        return this;
    }

    public CycloneDxSbomGenerator setOutputDirectory(Path outputDir) {
        ensureNotGenerated();
        this.outputDir = outputDir;
        return this;
    }

    public CycloneDxSbomGenerator setOutputFile(Path outputFile) {
        ensureNotGenerated();
        this.outputFile = outputFile;
        return this;
    }

    public CycloneDxSbomGenerator setFormat(String format) {
        ensureNotGenerated();
        this.format = format;
        return this;
    }

    public CycloneDxSbomGenerator setSchemaVersion(String schemaVersion) {
        ensureNotGenerated();
        this.schemaVersion = schemaVersion;
        return this;
    }

    public CycloneDxSbomGenerator setEffectiveModelResolver(EffectiveModelResolver modelResolver) {
        ensureNotGenerated();
        this.modelResolver = modelResolver;
        return this;
    }

    public CycloneDxSbomGenerator setIncludeLicenseText(boolean includeLicenseText) {
        ensureNotGenerated();
        this.includeLicenseText = includeLicenseText;
        return this;
    }

    public List<SbomResult> generate() {
        ensureNotGenerated();
        Objects.requireNonNull(manifest, "Manifest is null");
        if (outputFile == null && outputDir == null) {
            throw new IllegalArgumentException("Either outputDir or outputFile must be provided");
        }
        generated = true;

        var bom = new Bom();
        bom.setMetadata(new Metadata());
        addToolInfo(bom);

        addApplicationComponent(bom, manifest.getMainComponent());
        for (var c : manifest.getComponents()) {
            addComponent(bom, c);
        }
        if (FORMAT_ALL.equalsIgnoreCase(format)) {
            if (outputFile != null) {
                throw new IllegalArgumentException("Can't use output file " + outputFile + " with format '"
                        + FORMAT_ALL + "', since it implies generating multiple files");
            }
            final List<SbomResult> result = new ArrayList<>(SUPPORTED_FORMATS.size());
            for (String format : SUPPORTED_FORMATS) {
                result.add(persistSbom(bom, getOutputFile(format), format));
            }
            return result;
        }
        var outputFile = getOutputFile(format == null ? DEFAULT_FORMAT : format);
        return List.of(persistSbom(bom, outputFile, getFormat(outputFile)));
    }

    private void addComponent(Bom bom, ApplicationComponent component) {
        final org.cyclonedx.model.Component c = getComponent(component);
        bom.addComponent(c);
        recordDependencies(bom, component, c);
    }

    private static void recordDependencies(Bom bom, ApplicationComponent component, Component c) {
        if (!component.getDependencies().isEmpty()) {
            final Dependency d = new Dependency(c.getBomRef());
            for (var depCoords : sortAlphabetically(component.getDependencies())) {
                d.addDependency(new Dependency(getPackageURL(depCoords).toString()));
            }
            bom.addDependency(d);
        }
    }

    private void addApplicationComponent(Bom bom, ApplicationComponent component) {
        var c = getComponent(component);
        c.setType(org.cyclonedx.model.Component.Type.APPLICATION);
        bom.getMetadata().setComponent(c);
        bom.addComponent(c);
        recordDependencies(bom, component, c);
    }

    private org.cyclonedx.model.Component getComponent(ApplicationComponent component) {
        final org.cyclonedx.model.Component c = new org.cyclonedx.model.Component();
        var dep = component.getResolvedDependency();
        if (dep != null) {
            initMavenComponent(dep, c);
        } else if (component.getDistributionPath() != null) {
            c.setBomRef(component.getDistributionPath());
            c.setType(org.cyclonedx.model.Component.Type.FILE);
            c.setName(component.getPath().getFileName().toString());
        } else if (component.getPath() != null) {
            final String fileName = component.getPath().getFileName().toString();
            c.setName(fileName);
            c.setBomRef(fileName);
            c.setType(org.cyclonedx.model.Component.Type.FILE);
        } else {
            throw new RuntimeException("Component is not associated with any file system path");
        }

        final List<Property> props = new ArrayList<>(2);
        String quarkusScope = component.getScope();
        if (quarkusScope == null) {
            quarkusScope = dep == null || dep.isRuntimeCp() ? ApplicationComponent.SCOPE_RUNTIME
                    : ApplicationComponent.SCOPE_DEVELOPMENT;
        }
        addProperty(props, QUARKUS_COMPONENT_SCOPE, quarkusScope);
        if (component.getDistributionPath() != null) {
            if (getSchemaVersion().getVersion() >= 1.5) {
                var occurence = new Occurrence();
                occurence.setLocation(component.getDistributionPath());
                var evidence = new Evidence();
                evidence.setOccurrences(List.of(occurence));
                c.setEvidence(evidence);
            } else {
                addProperty(props, QUARKUS_COMPONENT_LOCATION, component.getDistributionPath());
            }
        }
        c.setProperties(props);

        if (component.getPedigree() != null) {
            var pedigree = new Pedigree();
            pedigree.setNotes(component.getPedigree());
            c.setPedigree(pedigree);
        }

        if (component.getPath() != null) {
            try {
                c.setHashes(BomUtils.calculateHashes(component.getPath().toFile(), getSchemaVersion()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return c;
    }

    private void initMavenComponent(ArtifactCoords coords, Component c) {
        addPomMetadata(coords, c);
        c.setGroup(coords.getGroupId());
        c.setName(coords.getArtifactId());
        c.setVersion(coords.getVersion());
        final PackageURL purl = getPackageURL(coords);
        c.setPurl(purl);
        c.setBomRef(purl.toString());
        c.setType(Component.Type.LIBRARY);
    }

    private void addPomMetadata(ArtifactCoords dep, org.cyclonedx.model.Component component) {
        var model = modelResolver == null ? null : modelResolver.resolveEffectiveModel(dep);
        if (model != null) {
            extractComponentMetadata(model, component);
        }
    }

    private void extractComponentMetadata(Model model, org.cyclonedx.model.Component component) {
        if (component.getPublisher() == null) {
            // If we don't already have publisher information, retrieve it.
            if (model.getOrganization() != null) {
                component.setPublisher(model.getOrganization().getName());
            }
        }
        if (component.getDescription() == null) {
            // If we don't already have description information, retrieve it.
            component.setDescription(model.getDescription());
        }
        var schemaVersion = getSchemaVersion();
        if (component.getLicenseChoice() == null || component.getLicenseChoice().getLicenses() == null
                || component.getLicenseChoice().getLicenses().isEmpty()) {
            // If we don't already have license information, retrieve it.
            if (model.getLicenses() != null) {
                component.setLicenseChoice(resolveMavenLicenses(model.getLicenses(), schemaVersion, includeLicenseText));
            }
        }
        if (Version.VERSION_10 != schemaVersion) {
            addExternalReference(ExternalReference.Type.WEBSITE, model.getUrl(), component);
            if (model.getCiManagement() != null) {
                addExternalReference(ExternalReference.Type.BUILD_SYSTEM, model.getCiManagement().getUrl(), component);
            }
            if (model.getDistributionManagement() != null) {
                addExternalReference(ExternalReference.Type.DISTRIBUTION, model.getDistributionManagement().getDownloadUrl(),
                        component);
                if (model.getDistributionManagement().getRepository() != null) {
                    ExternalReference.Type type = (schemaVersion.getVersion() < 1.5) ? ExternalReference.Type.DISTRIBUTION
                            : ExternalReference.Type.DISTRIBUTION_INTAKE;
                    addExternalReference(type, model.getDistributionManagement().getRepository().getUrl(), component);
                }
            }
            if (model.getIssueManagement() != null) {
                addExternalReference(ExternalReference.Type.ISSUE_TRACKER, model.getIssueManagement().getUrl(), component);
            }
            if (model.getMailingLists() != null && !model.getMailingLists().isEmpty()) {
                for (MailingList list : model.getMailingLists()) {
                    String url = list.getArchive();
                    if (url == null) {
                        url = list.getSubscribe();
                    }
                    addExternalReference(ExternalReference.Type.MAILING_LIST, url, component);
                }
            }
            if (model.getScm() != null) {
                addExternalReference(ExternalReference.Type.VCS, model.getScm().getUrl(), component);
            }
        }
    }

    private LicenseChoice resolveMavenLicenses(final List<org.apache.maven.model.License> projectLicenses,
            final Version schemaVersion, boolean includeLicenseText) {
        final LicenseChoice licenseChoice = new LicenseChoice();
        for (org.apache.maven.model.License artifactLicense : projectLicenses) {
            boolean resolved = false;
            if (artifactLicense.getName() != null) {
                final LicenseChoice resolvedByName = LicenseResolver.resolve(artifactLicense.getName(), includeLicenseText);
                resolved = resolveLicenseInfo(licenseChoice, resolvedByName, schemaVersion);
            }
            if (artifactLicense.getUrl() != null && !resolved) {
                final LicenseChoice resolvedByUrl = LicenseResolver.resolve(artifactLicense.getUrl(), includeLicenseText);
                resolved = resolveLicenseInfo(licenseChoice, resolvedByUrl, schemaVersion);
            }
            if (artifactLicense.getName() != null && !resolved) {
                final License license = new License();
                license.setName(artifactLicense.getName().trim());
                if (StringUtils.isNotBlank(artifactLicense.getUrl())) {
                    try {
                        final URI uri = new URI(artifactLicense.getUrl().trim());
                        license.setUrl(uri.toString());
                    } catch (URISyntaxException e) {
                        // throw it away
                    }
                }
                licenseChoice.addLicense(license);
            }
        }
        return licenseChoice;
    }

    private boolean resolveLicenseInfo(final LicenseChoice licenseChoice, final LicenseChoice licenseChoiceToResolve,
            final Version schemaVersion) {
        if (licenseChoiceToResolve != null) {
            if (licenseChoiceToResolve.getLicenses() != null && !licenseChoiceToResolve.getLicenses().isEmpty()) {
                licenseChoice.addLicense(licenseChoiceToResolve.getLicenses().get(0));
                return true;
            } else if (licenseChoiceToResolve.getExpression() != null && Version.VERSION_10 != schemaVersion) {
                licenseChoice.setExpression(licenseChoiceToResolve.getExpression());
                return true;
            }
        }
        return false;
    }

    private static boolean doesComponentHaveExternalReference(final org.cyclonedx.model.Component component,
            final ExternalReference.Type type) {
        if (component.getExternalReferences() != null && !component.getExternalReferences().isEmpty()) {
            for (final ExternalReference ref : component.getExternalReferences()) {
                if (type == ref.getType()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void addExternalReference(final ExternalReference.Type referenceType, final String url,
            final org.cyclonedx.model.Component component) {
        if (url == null) {
            return;
        }
        try {
            final URI uri = new URI(url.trim());
            final ExternalReference ref = new ExternalReference();
            ref.setType(referenceType);
            ref.setUrl(uri.toString());
            component.addExternalReference(ref);
        } catch (URISyntaxException e) {
            // throw it away
        }
    }

    private static PackageURL getPackageURL(ArtifactCoords dep) {
        final TreeMap<String, String> qualifiers = new TreeMap<>();
        qualifiers.put("type", dep.getType());
        if (!dep.getClassifier().isEmpty()) {
            qualifiers.put("classifier", dep.getClassifier());
        }
        final PackageURL purl;
        try {
            purl = new PackageURL(PackageURL.StandardTypes.MAVEN,
                    dep.getGroupId(),
                    dep.getArtifactId(),
                    dep.getVersion(),
                    qualifiers, null);
        } catch (MalformedPackageURLException e) {
            throw new RuntimeException("Failed to generate Purl for " + dep.toCompactCoords(), e);
        }
        return purl;
    }

    static void addProperty(List<Property> props, String name, String value) {
        var prop = new Property();
        prop.setName(name);
        prop.setValue(value);
        props.add(prop);
    }

    private static List<ArtifactCoords> sortAlphabetically(Collection<ArtifactCoords> col) {
        var list = new ArrayList<>(col);
        list.sort(ARTIFACT_COORDS_COMPARATOR);
        return list;
    }

    private SbomResult persistSbom(Bom bom, Path sbomFile, String format) {

        var specVersion = getSchemaVersion();
        final String sbomContent;
        if (format.equalsIgnoreCase("json")) {
            try {
                sbomContent = BomGeneratorFactory.createJson(specVersion, bom).toJsonString();
            } catch (Throwable e) {
                throw new RuntimeException("Failed to generate an SBOM in JSON format", e);
            }
        } else if (format.equalsIgnoreCase("xml")) {
            try {
                sbomContent = BomGeneratorFactory.createXml(specVersion, bom).toXmlString();
            } catch (GeneratorException e) {
                throw new RuntimeException("Failed to generate an SBOM in XML format", e);
            }
        } else {
            throw new RuntimeException(
                    "Unsupported SBOM artifact type " + format + ", supported types are json and xml");
        }

        var outputDir = sbomFile.getParent();
        if (outputDir != null) {
            try {
                Files.createDirectories(outputDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("SBOM Content:" + System.lineSeparator() + sbomContent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(sbomFile)) {
            writer.write(sbomContent);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write to " + sbomFile, e);
        }

        return new SbomResult(sbomFile, "CycloneDX", bom.getSpecVersion(), format, CLASSIFIER_CYCLONEDX,
                manifest.getRunnerPath());
    }

    private Path getOutputFile(String defaultFormat) {
        if (outputFile == null) {
            var fileName = toSbomFileName(manifest.getRunnerPath().getFileName().toString(), defaultFormat);
            return outputDir == null ? Path.of(fileName) : outputDir.resolve(fileName);
        }
        return outputFile;
    }

    private String toSbomFileName(String deliverableName, String defaultFormat) {
        return stripExtension(deliverableName) + "-" + CLASSIFIER_CYCLONEDX + "." + defaultFormat;
    }

    private static String stripExtension(String fileName) {
        var lastDot = fileName.lastIndexOf('.');
        if (lastDot <= 0) {
            return fileName;
        }
        var lastDash = fileName.lastIndexOf('-');
        return lastDot < lastDash ? fileName : fileName.substring(0, lastDot);
    }

    private String getFormat(Path outputFile) {
        if (format == null || "all".equalsIgnoreCase(format)) {
            var name = outputFile.getFileName().toString();
            var lastDot = name.lastIndexOf('.');
            if (lastDot < 0 || lastDot == name.length() - 1) {
                throw new IllegalArgumentException("Failed to determine file extension of " + outputFile);
            }
            return name.substring(lastDot + 1);
        }
        return format;
    }

    private Version getSchemaVersion() {
        if (effectiveSchemaVersion == null) {
            if (schemaVersion == null) {
                effectiveSchemaVersion = Collections.max(List.of(Version.values()));
            } else {
                for (var v : Version.values()) {
                    if (schemaVersion.equals(v.getVersionString())) {
                        effectiveSchemaVersion = v;
                        break;
                    }
                }
                if (effectiveSchemaVersion == null) {
                    var versions = Version.values();
                    var sb = new StringBuilder();
                    sb.append("Requested CycloneDX schema version ").append(schemaVersion)
                            .append(" does not appear in the list of supported versions: ")
                            .append(versions[0].getVersionString());
                    for (int i = 1; i < versions.length; ++i) {
                        sb.append(", ").append(versions[i].getVersionString());
                    }
                    throw new IllegalArgumentException(sb.toString());
                }
            }
        }
        return effectiveSchemaVersion;
    }

    private void addToolInfo(Bom bom) {

        var toolLocation = getToolLocation();
        if (toolLocation == null) {
            return;
        }
        List<Hash> hashes = null;
        if (!Files.isDirectory(toolLocation)) {
            try {
                hashes = BomUtils.calculateHashes(toolLocation.toFile(), getSchemaVersion());
            } catch (IOException e) {
                throw new RuntimeException("Failed to calculate hashes for the tool at " + toolLocation, e);
            }
        } else {
            log.warn("skipping tool hashing because " + toolLocation + " appears to be a directory");
        }

        if (getSchemaVersion().getVersion() >= 1.5) {
            final ToolInformation toolInfo = new ToolInformation();
            final Component toolComponent = new Component();
            toolComponent.setType(Component.Type.LIBRARY);
            final ApplicationComponent appComponent = findApplicationComponent(toolLocation);
            if (appComponent != null && appComponent.getResolvedDependency() != null) {
                initMavenComponent(appComponent.getResolvedDependency(), toolComponent);
            } else {
                var coords = getMavenArtifact(toolLocation);
                if (coords != null) {
                    initMavenComponent(coords, toolComponent);
                } else {
                    toolComponent.setName(toolLocation.getFileName().toString());
                }
            }
            if (hashes != null) {
                toolComponent.setHashes(hashes);
            }
            toolInfo.setComponents(List.of(toolComponent));
            bom.getMetadata().setToolChoice(toolInfo);
        } else {
            var tool = new Tool();
            final ApplicationComponent appComponent = findApplicationComponent(toolLocation);
            if (appComponent != null && appComponent.getResolvedDependency() != null) {
                tool.setVendor(appComponent.getResolvedDependency().getGroupId());
                tool.setName(appComponent.getResolvedDependency().getArtifactId());
                tool.setVersion(appComponent.getResolvedDependency().getVersion());
            } else {
                var coords = getMavenArtifact(toolLocation);
                if (coords != null) {
                    tool.setVendor(coords.getGroupId());
                    tool.setName(coords.getArtifactId());
                    tool.setVersion(coords.getVersion());
                } else {
                    tool.setName(toolLocation.getFileName().toString());
                }
            }
            if (hashes != null) {
                tool.setHashes(hashes);
            }
            bom.getMetadata().setTools(List.of(tool));
        }
    }

    private ApplicationComponent findApplicationComponent(Path path) {
        for (var c : manifest.getComponents()) {
            if (c.getResolvedDependency().getResolvedPaths().contains(path)) {
                return c;
            }
        }
        return null;
    }

    private static ArtifactCoords getMavenArtifact(Path toolLocation) {
        final List<ArtifactCoords> toolArtifact = new ArrayList<>(1);
        PathTree.ofDirectoryOrArchive(toolLocation).walkIfContains("META-INF/maven", visit -> {
            if (!Files.isDirectory(visit.getPath()) && visit.getPath().getFileName().toString().equals("pom.properties")) {
                try (BufferedReader reader = Files.newBufferedReader(visit.getPath())) {
                    var props = new Properties();
                    props.load(reader);
                    final String groupId = props.getProperty("groupId");
                    if (isBlanc(groupId)) {
                        return;
                    }
                    final String artifactId = props.getProperty("artifactId");
                    if (isBlanc(artifactId)) {
                        return;
                    }
                    final String version = props.getProperty("version");
                    if (isBlanc(version)) {
                        return;
                    }
                    toolArtifact.add(ArtifactCoords.jar(groupId, artifactId, version));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });
        if (toolArtifact.size() != 1) {
            return null;
        }
        return toolArtifact.get(0);
    }

    private static boolean isBlanc(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Path getToolLocation() {
        var cs = getClass().getProtectionDomain().getCodeSource();
        if (cs == null) {
            log.warn("Failed to determine code source of the tool");
            return null;
        }
        var url = cs.getLocation();
        if (url == null) {
            log.warn("Failed to determine code source URL of the tool");
            return null;
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            log.warn("Failed to translate " + url + " to a file system path", e);
            return null;
        }
    }

    private void ensureNotGenerated() {
        if (generated) {
            throw new RuntimeException("This instance has already been used to generate an SBOM");
        }
    }
}
