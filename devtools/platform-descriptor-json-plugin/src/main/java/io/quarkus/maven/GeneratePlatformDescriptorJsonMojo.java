package io.quarkus.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.platform.descriptor.ProjectPlatformDescriptorJsonUtil;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.registry.catalog.Category;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionOrigin;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonCategory;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;

/**
 * This goal generates a platform JSON descriptor for a given platform BOM.
 * 
 * @deprecated in favor of <code>io.quarkus:quarkus-platform-bom-maven-plugin:generate-platform-descriptor</code>
 */
@Deprecated
@Mojo(name = "generate-platform-descriptor-json", threadSafe = true)
public class GeneratePlatformDescriptorJsonMojo extends AbstractMojo {

    @Parameter(property = "quarkusCoreGroupId", defaultValue = ToolsConstants.QUARKUS_CORE_GROUP_ID)
    private String quarkusCoreGroupId;

    @Parameter(property = "quarkusCoreArtifactId", defaultValue = ToolsConstants.QUARKUS_CORE_ARTIFACT_ID)
    private String quarkusCoreArtifactId;

    @Parameter(property = "bomGroupId", defaultValue = "${project.groupId}")
    private String bomGroupId;

    @Parameter(property = "bomArtifactId", defaultValue = "${project.artifactId}")
    private String bomArtifactId;

    @Parameter(property = "bomVersion", defaultValue = "${project.version}")
    private String bomVersion;

    /** file used for overrides - overridesFiles takes precedence over this file. **/
    @Parameter(property = "overridesFile", defaultValue = "${project.basedir}/src/main/resources/extensions-overrides.json")
    private String overridesFile;

    @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}-${project.version}.json")
    private File outputFile;

    @Component
    private RepositorySystem repoSystem;

    @Component
    RemoteRepositoryManager remoteRepoManager;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Component
    private MavenProject project;
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * A set of artifact group ID's that should be excluded from of the BOM and the descriptor.
     * This can speed up the process by preventing the download of artifacts that are not required.
     */
    @Parameter
    private Set<String> ignoredGroupIds = new HashSet<>(0);

    /**
     * A set of group IDs artifacts of which should be checked to be extensions and if so, included into the
     * generated descriptor. If this parameter is configured, artifacts with group IDs that aren't found
     * among the configured set will be ignored. However, this will not prevent extensions that are inherited
     * from parent platforms with different group IDs to be included into the generated descriptor.
     */
    @Parameter
    private Set<String> processGroupIds = new HashSet<>(1);

    /**
     * Skips the check for the descriptor's artifactId naming convention
     */
    @Parameter
    private boolean skipArtifactIdCheck;

    /**
     * Skips the check for the BOM to contain the generated platform JSON descriptor
     */
    @Parameter(property = "skipBomCheck")
    private boolean skipBomCheck;

    /**
     * Skips the check for categories referenced from the extensions to be listed in the generated descriptor
     */
    @Parameter(property = "skipCategoryCheck")
    boolean skipCategoryCheck;

    @Parameter(property = "resolveDependencyManagement")
    boolean resolveDependencyManagement;

    @Parameter(required = false)
    String quarkusCoreVersion;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        final Artifact jsonArtifact = new DefaultArtifact(project.getGroupId(), project.getArtifactId(), project.getVersion(),
                "json", project.getVersion());
        if (!skipArtifactIdCheck) {
            final String expectedArtifactId = bomArtifactId + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX;
            if (!jsonArtifact.getArtifactId().equals(expectedArtifactId)) {
                throw new MojoExecutionException(
                        "The project's artifactId " + project.getArtifactId() + " is expected to be " + expectedArtifactId);
            }
            if (!jsonArtifact.getGroupId().equals(bomGroupId)) {
                throw new MojoExecutionException("The project's groupId " + project.getGroupId()
                        + " is expected to match the groupId of the BOM which is " + bomGroupId);
            }
            if (!jsonArtifact.getVersion().equals(bomVersion)) {
                throw new MojoExecutionException("The project's version " + project.getVersion()
                        + " is expected to match the version of the BOM which is " + bomVersion);
            }
        }

        // Get the BOM artifact
        final DefaultArtifact bomArtifact = new DefaultArtifact(bomGroupId, bomArtifactId, "", "pom", bomVersion);
        info("Generating catalog of extensions for %s", bomArtifact);

        // if the BOM is generated and has replaced the original one, to pick up the generated content
        // we should read the dependencyManagement from the generated pom.xml
        List<Dependency> deps;
        if (resolveDependencyManagement) {
            getLog().debug("Resolving dependencyManagement from the artifact descriptor");
            deps = dependencyManagementFromDescriptor(bomArtifact);
        } else {
            deps = dependencyManagementFromProject();
            if (deps == null) {
                deps = dependencyManagementFromResolvedPom(bomArtifact);
            }
        }
        if (deps.isEmpty()) {
            getLog().warn("BOM " + bomArtifact + " does not include any dependency");
            return;
        }

        List<OverrideInfo> allOverrides = new ArrayList<>();
        for (String path : overridesFile.split(",")) {
            OverrideInfo overrideInfo = getOverrideInfo(new File(path.trim()));
            if (overrideInfo != null) {
                allOverrides.add(overrideInfo);
            }
        }

        final JsonExtensionCatalog platformJson = new JsonExtensionCatalog();
        final String platformId = jsonArtifact.getGroupId() + ":" + jsonArtifact.getArtifactId() + ":"
                + jsonArtifact.getClassifier()
                + ":" + jsonArtifact.getExtension() + ":" + jsonArtifact.getVersion();
        platformJson.setId(platformId);
        platformJson.setBom(ArtifactCoords.pom(bomGroupId, bomArtifactId, bomVersion));
        platformJson.setPlatform(true);

        final List<AppArtifact> importedDescriptors = deps.stream().filter(
                d -> d.getArtifact().getArtifactId().endsWith(BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)
                        && d.getArtifact().getExtension().equals("json")
                        && !(d.getArtifact().getArtifactId().equals(jsonArtifact.getArtifactId())
                                && d.getArtifact().getGroupId().equals(jsonArtifact.getGroupId())))
                .map(d -> new AppArtifact(d.getArtifact().getGroupId(), d.getArtifact().getArtifactId(),
                        d.getArtifact().getClassifier(), d.getArtifact().getExtension(), d.getArtifact().getVersion()))
                .collect(Collectors.toList());

        Map<ArtifactKey, Extension> inheritedExtensions = Collections.emptyMap();
        if (!importedDescriptors.isEmpty()) {
            final MavenArtifactResolver mvnResolver;
            try {
                mvnResolver = MavenArtifactResolver.builder()
                        .setRepositorySystem(repoSystem)
                        .setRemoteRepositoryManager(remoteRepoManager)
                        .setRepositorySystemSession(repoSession)
                        .setRemoteRepositories(repos)
                        .setWorkspaceDiscovery(false)
                        .build();
            } catch (BootstrapMavenException e) {
                throw new MojoExecutionException("Failed to initialize Maven artifact resolver", e);
            }
            final JsonExtensionCatalog baseCatalog;
            try {
                baseCatalog = ProjectPlatformDescriptorJsonUtil
                        .resolveCatalog(new BootstrapAppModelResolver(mvnResolver), importedDescriptors);
            } catch (AppModelResolverException e) {
                throw new MojoExecutionException("Failed to resolver inherited platform descriptor", e);
            }
            platformJson.setDerivedFrom(baseCatalog.getDerivedFrom());
            platformJson.setCategories(baseCatalog.getCategories());

            final List<Extension> extensions = baseCatalog.getExtensions();
            if (!extensions.isEmpty()) {
                inheritedExtensions = new HashMap<>(extensions.size());
                for (Extension e : extensions) {
                    inheritedExtensions.put(e.getArtifact().getKey(), e);
                }
            }

            platformJson.setMetadata(baseCatalog.getMetadata());
        }

        // Create a JSON array of extension descriptors
        final Set<String> referencedCategories = new HashSet<>();
        final List<io.quarkus.registry.catalog.Extension> extListJson = new ArrayList<>();
        platformJson.setExtensions(extListJson);
        boolean jsonFoundInBom = false;
        for (Dependency dep : deps) {
            final Artifact artifact = dep.getArtifact();

            // checking whether the descriptor is present in the BOM
            if (!skipBomCheck && !jsonFoundInBom) {
                jsonFoundInBom = artifact.getArtifactId().equals(jsonArtifact.getArtifactId())
                        && artifact.getGroupId().equals(jsonArtifact.getGroupId())
                        && artifact.getExtension().equals(jsonArtifact.getExtension())
                        && artifact.getClassifier().equals(jsonArtifact.getClassifier())
                        && artifact.getVersion().equals(jsonArtifact.getVersion());
            }

            // filtering non jar artifacts
            if (!artifact.getExtension().equals("jar")
                    || "javadoc".equals(artifact.getClassifier())
                    || "tests".equals(artifact.getClassifier())
                    || "sources".equals(artifact.getClassifier())
                    || artifact.getArtifactId().endsWith("-deployment")) {
                continue;
            }

            if (processGroupIds.isEmpty()) {
                if (ignoredGroupIds.contains(artifact.getGroupId())) {
                    continue;
                }
            } else if (!processGroupIds.contains(artifact.getGroupId())) {
                continue;
            }

            if (quarkusCoreVersion == null && artifact.getArtifactId().equals(quarkusCoreArtifactId)
                    && artifact.getGroupId().equals(quarkusCoreGroupId)) {
                quarkusCoreVersion = artifact.getVersion();
            }
            ArtifactResult resolved = null;
            JsonExtension extension = null;
            try {
                resolved = repoSystem.resolveArtifact(repoSession,
                        new ArtifactRequest().setRepositories(repos).setArtifact(artifact));
                extension = processDependency(resolved.getArtifact());
            } catch (ArtifactResolutionException e) {
                // there are some parent poms that appear as jars for some reason
                debug("Failed to resolve dependency %s defined in %s", artifact, bomArtifact);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to process dependency " + artifact, e);
            }

            if (extension == null) {
                continue;
            }

            Extension inherited = inheritedExtensions.get(extension.getArtifact().getKey());
            final List<ExtensionOrigin> origins;
            if (inherited != null) {
                origins = new ArrayList<>(inherited.getOrigins().size() + 1);
                origins.addAll(inherited.getOrigins());
                origins.add(platformJson);
            } else {
                origins = Arrays.asList(platformJson);
            }
            extension.setOrigins(origins);
            String key = extensionId(extension);
            for (OverrideInfo info : allOverrides) {
                io.quarkus.registry.catalog.Extension extOverride = info.getExtOverrides().get(key);
                if (extOverride != null) {
                    extension = mergeObject(extension, extOverride);
                }
            }
            extListJson.add(extension);

            if (!skipCategoryCheck) {
                try {
                    @SuppressWarnings("unchecked")
                    final Collection<String> extCategories = (Collection<String>) extension.getMetadata()
                            .get("categories");
                    if (extCategories != null) {
                        referencedCategories.addAll(extCategories);
                    }
                } catch (ClassCastException e) {
                    getLog().warn("Failed to cast the extension categories list to java.util.Collection<String>", e);
                }
            }
        }

        if (!skipBomCheck && !jsonFoundInBom) {
            throw new MojoExecutionException(
                    "Failed to locate " + jsonArtifact + " in the dependencyManagement section of " + bomArtifact);
        }
        if (quarkusCoreVersion == null) {
            throw new MojoExecutionException("Failed to determine the Quarkus Core version for " + bomArtifact);
        }
        platformJson.setQuarkusCoreVersion(quarkusCoreVersion);

        for (OverrideInfo info : allOverrides) {
            if (info.getTheRest() != null) {
                if (!info.getTheRest().getCategories().isEmpty()) {
                    if (platformJson.getCategories().isEmpty()) {
                        platformJson.setCategories(info.getTheRest().getCategories());
                    } else {
                        info.getTheRest().getCategories().stream().forEach(c -> {
                            boolean found = false;
                            for (Category platformC : platformJson.getCategories()) {
                                if (platformC.getId().equals(c.getId())) {
                                    found = true;
                                    JsonCategory jsonC = (JsonCategory) platformC;
                                    if (c.getDescription() != null) {
                                        jsonC.setDescription(c.getDescription());
                                    }
                                    if (!c.getMetadata().isEmpty()) {
                                        if (jsonC.getMetadata().isEmpty()) {
                                            jsonC.setMetadata(c.getMetadata());
                                        } else {
                                            jsonC.getMetadata().putAll(c.getMetadata());
                                        }
                                    }
                                    if (c.getName() != null) {
                                        jsonC.setName(c.getName());
                                    }
                                }
                                break;
                            }
                            if (!found) {
                                platformJson.getCategories().add(c);
                            }
                        });
                    }
                }
            }
            if (!info.getTheRest().getMetadata().isEmpty()) {
                if (platformJson.getMetadata().isEmpty()) {
                    platformJson.setMetadata(info.getTheRest().getMetadata());
                } else {
                    platformJson.getMetadata().putAll(info.getTheRest().getMetadata());
                }
            }
        }

        // make sure all the categories referenced by extensions are actually present in
        // the platform descriptor
        if (!skipCategoryCheck) {
            final Set<String> catalogCategories = platformJson.getCategories().stream().map(c -> c.getId())
                    .collect(Collectors.toSet());
            if (!catalogCategories.containsAll(referencedCategories)) {
                final List<String> missing = referencedCategories.stream().filter(c -> !catalogCategories.contains(c))
                        .collect(Collectors.toList());
                final StringBuilder buf = new StringBuilder();
                buf.append(
                        "The following categories referenced from extensions are missing from the generated catalog: ");
                buf.append(missing.get(0));
                for (int i = 1; i < missing.size(); ++i) {
                    buf.append(", ").append(missing.get(i));
                }
                throw new MojoExecutionException(buf.toString());
            }
        }

        // Write the JSON to the output file
        final File outputDir = outputFile.getParentFile();
        if (outputFile.exists()) {
            outputFile.delete();
        } else if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new MojoExecutionException("Failed to create output directory " + outputDir);
            }
        }
        try {
            JsonCatalogMapperHelper.serialize(platformJson, outputFile.toPath().getParent().resolve(outputFile.getName()));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to persist the platform descriptor", e);
        }
        info("Extensions file written to %s", outputFile);

        // this is necessary to sometimes be able to resolve the artifacts from the workspace
        final File published = new File(project.getBuild().getDirectory(), LocalWorkspace.getFileName(jsonArtifact));
        if (!outputDir.equals(published)) {
            try {
                IoUtils.copy(outputFile.toPath(), published.toPath());
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to copy " + outputFile + " to " + published);
            }
        }
        projectHelper.attachArtifact(project, jsonArtifact.getExtension(), jsonArtifact.getClassifier(), published);
    }

    private List<Dependency> dependencyManagementFromDescriptor(Artifact bomArtifact) throws MojoExecutionException {
        try {
            return repoSystem
                    .readArtifactDescriptor(repoSession,
                            new ArtifactDescriptorRequest().setRepositories(repos).setArtifact(bomArtifact))
                    .getManagedDependencies();
        } catch (ArtifactDescriptorException e) {
            throw new MojoExecutionException("Failed to read descriptor of " + bomArtifact, e);
        }
    }

    private List<Dependency> dependencyManagementFromResolvedPom(Artifact bomArtifact) throws MojoExecutionException {
        final Path pomXml;
        try {
            pomXml = repoSystem
                    .resolveArtifact(repoSession, new ArtifactRequest().setArtifact(bomArtifact).setRepositories(repos))
                    .getArtifact().getFile().toPath();
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Failed to resolve " + bomArtifact, e);
        }
        return readDependencyManagement(pomXml);
    }

    private List<Dependency> dependencyManagementFromProject() throws MojoExecutionException {
        // if the configured BOM coordinates are not matching the current project
        // the current project's POM isn't the right source
        if (!project.getArtifact().getArtifactId().equals(bomArtifactId)
                || !project.getArtifact().getVersion().equals(bomVersion)
                || !project.getArtifact().getGroupId().equals(bomGroupId)
                || !project.getFile().exists()) {
            return null;
        }
        return readDependencyManagement(project.getFile().toPath());
    }

    private List<Dependency> readDependencyManagement(Path pomXml) throws MojoExecutionException {
        if (getLog().isDebugEnabled()) {
            getLog().debug("Reading dependencyManagement from " + pomXml);
        }
        final Model bomModel;
        try {
            bomModel = ModelUtils.readModel(pomXml);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to parse " + project.getFile(), e);
        }

        // if the POM has a parent then we better resolve the descriptor
        if (bomModel.getParent() != null) {
            throw new MojoExecutionException(pomXml
                    + " has a parent, in which case it is recommended to set 'resolveDependencyManagement' parameter to true");
        }

        if (bomModel.getDependencyManagement() == null) {
            return Collections.emptyList();
        }
        final List<org.apache.maven.model.Dependency> modelDeps = bomModel.getDependencyManagement().getDependencies();
        if (modelDeps.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Dependency> deps = new ArrayList<>(modelDeps.size());
        for (org.apache.maven.model.Dependency modelDep : modelDeps) {
            final Artifact artifact = new DefaultArtifact(modelDep.getGroupId(), modelDep.getArtifactId(),
                    modelDep.getClassifier(), modelDep.getType(), modelDep.getVersion());
            // exclusions aren't relevant in this context
            deps.add(new Dependency(artifact, modelDep.getScope(), modelDep.isOptional(), Collections.emptyList()));
        }
        return deps;
    }

    private JsonExtension processDependency(Artifact artifact) throws IOException {
        final Path path = artifact.getFile().toPath();
        if (Files.isDirectory(path)) {
            return processMetaInfDir(artifact, path.resolve(BootstrapConstants.META_INF));
        } else {
            try (FileSystem artifactFs = ZipUtils.newFileSystem(path)) {
                return processMetaInfDir(artifact, artifactFs.getPath(BootstrapConstants.META_INF));
            }
        }
    }

    /**
     * Load and return javax.jsonObject based on yaml, json or properties file.
     *
     * @param artifact
     * @param metaInfDir
     * @return
     * @throws IOException
     */
    private JsonExtension processMetaInfDir(Artifact artifact, Path metaInfDir)
            throws IOException {

        ObjectMapper mapper = null;

        if (!Files.exists(metaInfDir)) {
            return null;
        }

        Path yaml = metaInfDir.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME);
        if (Files.exists(yaml)) {
            mapper = getMapper(true);
            return processPlatformArtifact(artifact, yaml, mapper);
        }

        JsonExtension e = null;
        mapper = getMapper(false);
        Path json = metaInfDir.resolve(BootstrapConstants.EXTENSION_PROPS_JSON_FILE_NAME);
        if (!Files.exists(json)) {
            final Path props = metaInfDir.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME);
            if (Files.exists(props)) {
                e = new JsonExtension();
                e.setArtifact(new ArtifactCoords(artifact.getGroupId(), artifact.getArtifactId(),
                        artifact.getClassifier(), artifact.getExtension(), artifact.getVersion()));
                e.setName(artifact.getArtifactId());
            }
        } else {
            e = processPlatformArtifact(artifact, json, mapper);
        }
        return e;
    }

    private JsonExtension processPlatformArtifact(Artifact artifact, Path descriptor, ObjectMapper mapper)
            throws IOException {
        try (InputStream is = Files.newInputStream(descriptor)) {
            JsonExtension legacy = mapper.readValue(is, JsonExtension.class);
            JsonExtension object = transformLegacyToNew(legacy);
            debug("Adding Quarkus extension %s", object.getArtifact());
            return object;
        } catch (IOException io) {
            throw new IOException("Failed to parse " + descriptor, io);
        }
    }

    private ObjectMapper getMapper(boolean yaml) {

        if (yaml) {
            YAMLFactory yf = new YAMLFactory();
            return JsonCatalogMapperHelper.initMapper(new ObjectMapper(yf));
        } else {
            return JsonCatalogMapperHelper.mapper();
        }
    }

    private String extensionId(io.quarkus.registry.catalog.Extension extObject) {
        return extObject.getArtifact().getGroupId() + ":" + extObject.getArtifact().getArtifactId();
    }

    private JsonExtension mergeObject(JsonExtension extObject, io.quarkus.registry.catalog.Extension extOverride) {
        if (extOverride.getArtifact() != null) {
            extObject.setArtifact(extOverride.getArtifact());
        }
        if (!extOverride.getMetadata().isEmpty()) {
            if (extObject.getMetadata().isEmpty()) {
                extObject.setMetadata(extOverride.getMetadata());
            } else {
                extObject.getMetadata().putAll(extOverride.getMetadata());
            }
        }
        if (extOverride.getName() != null) {
            extObject.setName(extOverride.getName());
        }
        if (!extOverride.getOrigins().isEmpty()) {
            extObject.setOrigins(extOverride.getOrigins());
        }
        return extObject;
    }

    private void info(String msg, Object... args) {
        if (!getLog().isInfoEnabled()) {
            return;
        }
        if (args.length == 0) {
            getLog().info(msg);
            return;
        }
        getLog().info(String.format(msg, args));
    }

    private void debug(String msg, Object... args) {
        if (!getLog().isDebugEnabled()) {
            return;
        }
        if (args.length == 0) {
            getLog().debug(msg);
            return;
        }
        getLog().debug(String.format(msg, args));
    }

    private JsonExtension transformLegacyToNew(JsonExtension extObject) {
        final Map<String, Object> metadata = extObject.getMetadata();
        final Object labels = metadata.get("labels");
        if (labels != null) {
            metadata.put("keywords", labels);
            metadata.remove("labels");
        }
        return extObject;
    }

    public OverrideInfo getOverrideInfo(File overridesFile) throws MojoExecutionException {
        // Read the overrides file for the extensions (if it exists)
        HashMap<String, io.quarkus.registry.catalog.Extension> extOverrides = new HashMap<>();
        JsonExtensionCatalog theRest = null;
        if (overridesFile.isFile()) {
            info("Found overrides file %s", overridesFile);
            try {
                JsonExtensionCatalog overridesObject = JsonCatalogMapperHelper.deserialize(overridesFile.toPath(),
                        JsonExtensionCatalog.class);
                List<io.quarkus.registry.catalog.Extension> extensionsOverrides = overridesObject.getExtensions();
                if (!extensionsOverrides.isEmpty()) {
                    // Put the extension overrides into a map keyed to their GAV
                    for (io.quarkus.registry.catalog.Extension extOverride : extensionsOverrides) {
                        String key = extensionId(extOverride);
                        extOverrides.put(key, extOverride);
                    }
                }

                theRest = overridesObject;
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to read " + overridesFile, e);
            }
            return new OverrideInfo(extOverrides, theRest);
        }
        return null;
    }

    private static class OverrideInfo {
        private Map<String, io.quarkus.registry.catalog.Extension> extOverrides;
        private JsonExtensionCatalog theRest;

        public OverrideInfo(Map<String, io.quarkus.registry.catalog.Extension> extOverrides,
                JsonExtensionCatalog theRest) {
            this.extOverrides = extOverrides;
            this.theRest = theRest;
        }

        public Map<String, io.quarkus.registry.catalog.Extension> getExtOverrides() {
            return extOverrides;
        }

        public JsonExtensionCatalog getTheRest() {
            return theRest;
        }
    }
}
