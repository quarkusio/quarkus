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
import org.cyclonedx.Format;
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
import io.quarkus.sbom.ComponentDependencies;
import io.quarkus.sbom.ComponentDescriptor;
import io.quarkus.sbom.Purl;
import io.quarkus.sbom.SbomContribution;

public class CycloneDxSbomGenerator {

    private static final Logger log = Logger.getLogger(CycloneDxSbomGenerator.class);

    private static final String QUARKUS_COMPONENT_SCOPE = "quarkus:component:scope";
    private static final String QUARKUS_COMPONENT_LOCATION = "quarkus:component:location";

    private static final String CLASSIFIER_CYCLONEDX = "cyclonedx";
    private static final String FORMAT_ALL = "all";
    private static final String DEFAULT_FORMAT = "json";

    public static CycloneDxSbomGenerator newInstance() {
        return new CycloneDxSbomGenerator();
    }

    private boolean generated;
    private Path outputDir;
    private Path outputFile;
    private String schemaVersion;
    private String format;
    private EffectiveModelResolver modelResolver;
    private boolean includeLicenseText;
    private boolean prettyPrint;
    private List<SbomContribution> contributions = List.of();

    private Version effectiveSchemaVersion;

    // resolved from contributions at generation time
    private String mainComponentBomRef;
    private Path runnerPath;

    private CycloneDxSbomGenerator() {
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

    public CycloneDxSbomGenerator setPrettyPrint(boolean prettyPrint) {
        ensureNotGenerated();
        this.prettyPrint = prettyPrint;
        return this;
    }

    /**
     * Sets the list of SBOM contributions to include in the generated SBOM.
     * A null value is treated as an empty list.
     *
     * @param contributions the SBOM contributions, or null
     * @return this generator for fluent chaining
     */
    public CycloneDxSbomGenerator setContributions(List<SbomContribution> contributions) {
        ensureNotGenerated();
        this.contributions = contributions == null ? List.of() : contributions;
        return this;
    }

    public List<String> generateText() {
        final Bom bom = createSbom();
        if (FORMAT_ALL.equalsIgnoreCase(format)) {
            Format[] formats = Format.values();
            final List<String> result = new ArrayList<>(formats.length);
            for (Format format : formats) {
                result.add(formatSbom(bom, format.getExtension()));
            }
            return result;
        }
        return List.of(formatSbom(bom, format == null ? DEFAULT_FORMAT : format));
    }

    public List<SbomResult> generate() {
        if (outputFile == null && outputDir == null) {
            throw new IllegalArgumentException("Either outputDir or outputFile must be provided");
        }
        final Bom bom = createSbom();

        if (FORMAT_ALL.equalsIgnoreCase(format)) {
            if (outputFile != null) {
                throw new IllegalArgumentException("Can't use output file " + outputFile + " with format '"
                        + FORMAT_ALL + "', since it implies generating multiple files");
            }
            Format[] formats = Format.values();
            final List<SbomResult> result = new ArrayList<>(formats.length);
            for (Format format : formats) {
                result.add(persistSbom(bom, getOutputFile(format.getExtension()), format.getExtension()));
            }
            return result;
        }
        var outputFile = getOutputFile(format == null ? DEFAULT_FORMAT : format);
        return List.of(persistSbom(bom, outputFile, getFormat(outputFile)));
    }

    // ---- SBOM Creation ----

    private Bom createSbom() {
        ensureNotGenerated();
        generated = true;

        // Resolve metadata from contributions
        resolveContributionMetadata();

        var bom = new Bom();
        bom.setMetadata(new Metadata());
        addToolInfo(bom);

        // Collect all components and dependencies from contributions
        final List<ComponentDescriptor> allDescriptors = new ArrayList<>();
        final List<ComponentDependencies> allDependencies = new ArrayList<>();
        for (SbomContribution contribution : contributions) {
            allDescriptors.addAll(contribution.components());
            allDependencies.addAll(contribution.dependencies());
        }

        // Sort for consistent ordering across builds
        allDescriptors.sort(Comparator.comparing(ComponentDescriptor::getBomRef));
        allDependencies.sort(Comparator.comparing(ComponentDependencies::getBomRef));

        // Render components
        for (ComponentDescriptor descriptor : allDescriptors) {
            if (descriptor.getBomRef().equals(mainComponentBomRef)) {
                renderMainComponent(bom, descriptor);
            } else {
                renderComponent(bom, descriptor);
            }
        }

        // Record dependency relationships
        final Map<String, Dependency> dependencyMap = new LinkedHashMap<>();
        for (ComponentDependencies dep : allDependencies) {
            Dependency d = new Dependency(dep.getBomRef());
            List<String> sortedDeps = new ArrayList<>(dep.getDependsOn());
            Collections.sort(sortedDeps);
            for (String depRef : sortedDeps) {
                d.addDependency(new Dependency(depRef));
            }
            dependencyMap.put(dep.getBomRef(), d);
        }

        // Link top-level extension components to the main component
        if (mainComponentBomRef != null) {
            addTopLevelDependencies(allDescriptors, dependencyMap);
        }

        for (Dependency d : dependencyMap.values()) {
            bom.addDependency(d);
        }

        return bom;
    }

    private void resolveContributionMetadata() {
        for (SbomContribution contribution : contributions) {
            if (contribution.mainComponentBomRef() != null) {
                mainComponentBomRef = contribution.mainComponentBomRef();
                runnerPath = contribution.runnerPath();
                return;
            }
        }
    }

    /**
     * Adds top-level extension components as dependencies of the main
     * application component.
     * <p>
     * Components marked as {@link ComponentDescriptor#isTopLevel() topLevel}
     * are added to the main component's dependency entry. If the main
     * component already has a dependency entry (from the core contribution),
     * the top-level refs are merged into it. Otherwise a new entry is created.
     *
     * @param allDescriptors all component descriptors across contributions
     * @param dependencyMap the mutable dependency map keyed by bom-ref
     */
    private void addTopLevelDependencies(List<ComponentDescriptor> allDescriptors,
            Map<String, Dependency> dependencyMap) {
        List<String> topLevelRefs = collectTopLevelBomRefs(allDescriptors);
        if (topLevelRefs.isEmpty()) {
            return;
        }
        Dependency mainDep = dependencyMap.computeIfAbsent(mainComponentBomRef, Dependency::new);
        Set<String> existingRefs = collectExistingDependencyRefs(mainDep);
        for (String ref : topLevelRefs) {
            if (!existingRefs.contains(ref)) {
                mainDep.addDependency(new Dependency(ref));
            }
        }
    }

    /**
     * Collects bom-refs of all top-level components, sorted for deterministic output.
     */
    private static List<String> collectTopLevelBomRefs(List<ComponentDescriptor> descriptors) {
        List<String> refs = new ArrayList<>();
        for (ComponentDescriptor d : descriptors) {
            if (d.isTopLevel()) {
                refs.add(d.getBomRef());
            }
        }
        Collections.sort(refs);
        return refs;
    }

    /**
     * Collects the set of existing dependency refs from a Dependency node.
     */
    private static Set<String> collectExistingDependencyRefs(Dependency dep) {
        if (dep.getDependencies() == null) {
            return Set.of();
        }
        Set<String> refs = new HashSet<>();
        for (Dependency d : dep.getDependencies()) {
            refs.add(d.getRef());
        }
        return refs;
    }

    // ---- PURL Conversion ----

    private static PackageURL toCycloneDxPurl(Purl purl) {
        try {
            TreeMap<String, String> qualifiers = purl.getQualifiers().isEmpty()
                    ? null
                    : new TreeMap<>(purl.getQualifiers());
            return new PackageURL(purl.getType(), purl.getNamespace(), purl.getName(),
                    purl.getVersion(), qualifiers, purl.getSubpath());
        } catch (MalformedPackageURLException e) {
            throw new RuntimeException("Failed to convert Purl to PackageURL: " + purl, e);
        }
    }

    private static ArtifactCoords toArtifactCoords(Purl purl) {
        return ArtifactCoords.of(
                purl.getNamespace(),
                purl.getName(),
                purl.getQualifiers().getOrDefault("classifier", ""),
                purl.getQualifiers().getOrDefault("type", "jar"),
                purl.getVersion());
    }

    // ---- Rendering ----

    private void renderMainComponent(Bom bom, ComponentDescriptor descriptor) {
        Component c = renderComponentCore(descriptor);
        c.setType(Component.Type.APPLICATION);
        bom.getMetadata().setComponent(c);
        bom.addComponent(c);
    }

    private void renderComponent(Bom bom, ComponentDescriptor descriptor) {
        bom.addComponent(renderComponentCore(descriptor));
    }

    private Component renderComponentCore(ComponentDescriptor descriptor) {
        Component c = new Component();

        // Identity from Purl
        Purl purl = descriptor.getPurl();
        PackageURL cdxPurl = toCycloneDxPurl(purl);
        c.setPurl(cdxPurl);
        c.setBomRef(descriptor.getBomRef());
        c.setName(purl.getName());
        c.setVersion(purl.getVersion());
        if (purl.getNamespace() != null) {
            c.setGroup(purl.getNamespace());
        }

        // Component type
        if (Purl.TYPE_GENERIC.equals(purl.getType())
                && (descriptor.getPath() != null || descriptor.getDistributionPath() != null)) {
            c.setType(Component.Type.FILE);
        } else {
            c.setType(Component.Type.LIBRARY);
        }

        // Scope property
        List<Property> props = new ArrayList<>(2);
        String scope = descriptor.getScope() != null ? descriptor.getScope()
                : ComponentDescriptor.SCOPE_RUNTIME;
        addProperty(props, QUARKUS_COMPONENT_SCOPE, scope);

        // POM metadata for Maven components
        if (Purl.TYPE_MAVEN.equals(purl.getType())) {
            addPomMetadata(toArtifactCoords(purl), c);
        }

        // Evidence/occurrence from distribution path
        if (descriptor.getDistributionPath() != null) {
            if (getSchemaVersion().getVersion() >= 1.5) {
                Occurrence occurrence = new Occurrence();
                occurrence.setLocation(descriptor.getDistributionPath());
                var evidence = new Evidence();
                evidence.setOccurrences(List.of(occurrence));
                c.setEvidence(evidence);
            } else {
                addProperty(props, QUARKUS_COMPONENT_LOCATION, descriptor.getDistributionPath());
            }
        }

        // Pedigree
        if (descriptor.getPedigree() != null) {
            var pedigree = new Pedigree();
            pedigree.setNotes(descriptor.getPedigree());
            c.setPedigree(pedigree);
        }

        // File hashes from path
        if (descriptor.getPath() != null) {
            try {
                c.setHashes(BomUtils.calculateHashes(descriptor.getPath().toFile(), getSchemaVersion()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        // SRI integrity hash (only if no file hashes were set)
        if (descriptor.getIntegrity() != null) {
            Hash hash = parseSriHash(descriptor.getIntegrity());
            if (hash != null && (c.getHashes() == null || c.getHashes().isEmpty())) {
                c.setHashes(List.of(hash));
            }
        }

        // Description from descriptor (only if POM metadata didn't set one)
        if (descriptor.getDescription() != null && c.getDescription() == null) {
            c.setDescription(descriptor.getDescription());
        }

        c.setProperties(props);
        return c;
    }

    // ---- SRI Hash Parsing ----

    private static Hash parseSriHash(String integrity) {
        int dashIndex = integrity.indexOf('-');
        if (dashIndex < 0) {
            return null;
        }
        String algorithmStr = integrity.substring(0, dashIndex);
        String base64Value = integrity.substring(dashIndex + 1);

        Hash.Algorithm algorithm;
        switch (algorithmStr) {
            case "sha1" -> algorithm = Hash.Algorithm.SHA1;
            case "sha256" -> algorithm = Hash.Algorithm.SHA_256;
            case "sha384" -> algorithm = Hash.Algorithm.SHA_384;
            case "sha512" -> algorithm = Hash.Algorithm.SHA_512;
            default -> {
                return null;
            }
        }

        byte[] decoded = Base64.getDecoder().decode(base64Value);
        StringBuilder hex = new StringBuilder(decoded.length * 2);
        for (byte b : decoded) {
            appendHexDigit(hex, b >> 4);
            appendHexDigit(hex, b);
        }
        return new Hash(algorithm, hex.toString());
    }

    private static void appendHexDigit(StringBuilder sb, int digit) {
        digit &= 0xf;
        sb.append((char) (digit < 10 ? '0' + digit : 'a' + digit - 10));
    }

    // ---- POM Metadata ----

    private void addPomMetadata(ArtifactCoords dep, org.cyclonedx.model.Component component) {
        var model = modelResolver == null ? null : modelResolver.resolveEffectiveModel(dep);
        if (model != null) {
            extractComponentMetadata(model, component);
        }
    }

    private void extractComponentMetadata(Model model, org.cyclonedx.model.Component component) {
        if (component.getPublisher() == null) {
            if (model.getOrganization() != null) {
                component.setPublisher(model.getOrganization().getName());
            }
        }
        if (component.getDescription() == null) {
            component.setDescription(model.getDescription());
        }
        var schemaVersion = getSchemaVersion();
        if (component.getLicenseChoice() == null || component.getLicenseChoice().getLicenses() == null
                || component.getLicenseChoice().getLicenses().isEmpty()) {
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

    // ---- Utilities ----

    static void addProperty(List<Property> props, String name, String value) {
        var prop = new Property();
        prop.setName(name);
        prop.setValue(value);
        props.add(prop);
    }

    // ---- Output / Persistence ----

    private SbomResult persistSbom(Bom bom, Path sbomFile, String format) {
        final String sbomContent = formatSbom(bom, format);

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

        return new SbomResult(sbomFile, "CycloneDX", bom.getSpecVersion(), format, CLASSIFIER_CYCLONEDX, runnerPath);
    }

    private String formatSbom(Bom bom, String format) {
        var specVersion = getSchemaVersion();
        final String sbomContent;
        if (format.equalsIgnoreCase("json")) {
            try {
                sbomContent = BomGeneratorFactory.createJson(specVersion, bom).toJsonString(prettyPrint);
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
            var msg = new StringBuilder("Unsupported SBOM format ").append(format);
            var supportedFormats = Format.values();
            msg.append(". Supported formats are ").append(supportedFormats[0].getExtension());
            for (int i = 1; i < supportedFormats.length; i++) {
                msg.append(", ").append(supportedFormats[i].getExtension());
            }
            throw new IllegalArgumentException(msg.toString());
        }
        return sbomContent;
    }

    private Path getOutputFile(String defaultFormat) {
        if (outputFile == null) {
            if (runnerPath == null) {
                throw new IllegalArgumentException(
                        "Cannot determine output file name: no runner path set and no output file specified");
            }
            String fileName = toSbomFileName(runnerPath.getFileName().toString(), defaultFormat);
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

    // ---- Tool Info ----

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

        ArtifactCoords coords = findToolCoords(toolLocation);

        if (getSchemaVersion().getVersion() >= 1.5) {
            final ToolInformation toolInfo = new ToolInformation();
            final Component toolComponent = new Component();
            toolComponent.setType(Component.Type.LIBRARY);
            if (coords != null) {
                initMavenToolComponent(coords, toolComponent);
            } else {
                toolComponent.setName(toolLocation.getFileName().toString());
            }
            if (hashes != null) {
                toolComponent.setHashes(hashes);
            }
            toolInfo.setComponents(List.of(toolComponent));
            bom.getMetadata().setToolChoice(toolInfo);
        } else {
            var tool = new Tool();
            if (coords != null) {
                tool.setVendor(coords.getGroupId());
                tool.setName(coords.getArtifactId());
                tool.setVersion(coords.getVersion());
            } else {
                tool.setName(toolLocation.getFileName().toString());
            }
            if (hashes != null) {
                tool.setHashes(hashes);
            }
            bom.getMetadata().setTools(List.of(tool));
        }
    }

    private ArtifactCoords findToolCoords(Path toolLocation) {
        // Try to find the tool among contributed components by path
        for (SbomContribution contribution : contributions) {
            for (ComponentDescriptor desc : contribution.components()) {
                if (desc.getPath() != null && desc.getPath().equals(toolLocation)
                        && Purl.TYPE_MAVEN.equals(desc.getPurl().getType())) {
                    return toArtifactCoords(desc.getPurl());
                }
            }
        }
        // Fall back to reading pom.properties from the jar
        return getMavenArtifact(toolLocation);
    }

    private void initMavenToolComponent(ArtifactCoords coords, Component c) {
        addPomMetadata(coords, c);
        c.setGroup(coords.getGroupId());
        c.setName(coords.getArtifactId());
        c.setVersion(coords.getVersion());
        PackageURL purl = toCycloneDxPurl(Purl.maven(coords.getGroupId(), coords.getArtifactId(), coords.getVersion(),
                coords.getType(), coords.getClassifier().isEmpty() ? null : coords.getClassifier()));
        c.setPurl(purl);
        c.setBomRef(purl.toString());
        c.setType(Component.Type.LIBRARY);
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
