package io.quarkus.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

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
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalWorkspace;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.bootstrap.util.IoUtils;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.dependencies.Extension;
import io.quarkus.platform.tools.ToolsConstants;

/**
 * This goal generates a list of extensions for a given BOM
 * and stores it in a JSON format file that is later used by the tools
 * as the catalog of available extensions.
 */
@Mojo(name = "generate-extensions-json")
public class GenerateExtensionsJsonMojo extends AbstractMojo {

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

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    @Component
    private MavenProject project;
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Group ID's that we know don't contain extensions. This can speed up the process
     * by preventing the download of artifacts that are not required.
     */
    @Parameter
    private Set<String> ignoredGroupIds = new HashSet<>(0);

    @Parameter
    private boolean skipArtifactIdCheck;

    @Parameter(property = "skipBomCheck")
    private boolean skipBomCheck;

    @Parameter(property = "resolveDependencyManagement")
    boolean resolveDependencyManagement;

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

        // Create a JSON array of extension descriptors
        final JsonArrayBuilder extListJson = Json.createArrayBuilder();
        String quarkusCoreVersion = null;
        boolean jsonFoundInBom = false;
        for (Dependency dep : deps) {
            final Artifact artifact = dep.getArtifact();

            if (!skipBomCheck && !jsonFoundInBom) {
                jsonFoundInBom = artifact.getArtifactId().equals(jsonArtifact.getArtifactId())
                        && artifact.getGroupId().equals(jsonArtifact.getGroupId())
                        && artifact.getExtension().equals(jsonArtifact.getExtension())
                        && artifact.getClassifier().equals(jsonArtifact.getClassifier())
                        && artifact.getVersion().equals(jsonArtifact.getVersion());
            }

            if (ignoredGroupIds.contains(artifact.getGroupId())
                    || !artifact.getExtension().equals("jar")
                    || "javadoc".equals(artifact.getClassifier())
                    || "tests".equals(artifact.getClassifier())
                    || "sources".equals(artifact.getClassifier())) {
                continue;
            }

            if (artifact.getArtifactId().equals(ToolsConstants.QUARKUS_CORE_ARTIFACT_ID)
                    && artifact.getGroupId().equals(ToolsConstants.QUARKUS_CORE_GROUP_ID)) {
                quarkusCoreVersion = artifact.getVersion();
            }
            ArtifactResult resolved = null;
            try {
                resolved = repoSystem.resolveArtifact(repoSession,
                        new ArtifactRequest().setRepositories(repos).setArtifact(artifact));
                JsonObject extObject = processDependency(resolved.getArtifact());
                if (extObject != null) {
                    String key = extensionId(extObject);
                    for (OverrideInfo info : allOverrides) {
                        JsonObject extOverride = info.getExtOverrides().get(key);
                        if (extOverride != null) {
                            extObject = mergeObject(extObject, extOverride);
                        }
                    }
                    extListJson.add(extObject);
                }
            } catch (ArtifactResolutionException e) {
                // there are some parent poms that appear as jars for some reason
                debug("Failed to resolve dependency %s defined in %s", artifact, bomArtifact);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to process dependency " + artifact, e);
            }
        }

        if (!skipBomCheck && !jsonFoundInBom) {
            throw new MojoExecutionException(
                    "Failed to locate " + jsonArtifact + " in the dependencyManagement section of " + bomArtifact);
        }
        if (quarkusCoreVersion == null) {
            throw new MojoExecutionException("Failed to determine the Quarkus Core version for " + bomArtifact);
        }

        // Create the toplevel JSON
        final JsonObjectBuilder platformJson = Json.createObjectBuilder();
        // Add information about the BOM to it
        final JsonObjectBuilder bomJson = Json.createObjectBuilder();
        bomJson.add(Extension.GROUP_ID, bomGroupId);
        bomJson.add(Extension.ARTIFACT_ID, bomArtifactId);
        bomJson.add(Extension.VERSION, bomVersion);
        platformJson.add("bom", bomJson.build());
        // Add Quarkus version
        platformJson.add("quarkus-core-version", quarkusCoreVersion);
        // And add the list of extensions
        platformJson.add("extensions", extListJson.build());

        for (OverrideInfo info : allOverrides) {

            if (info.getTheRest() != null) {
                info.getTheRest().forEach((key, item) -> {
                    // Ignore the two keys we are explicitly managing
                    // but then add anything else found.
                    // TODO: if multiple files the last one wins!
                    // meaning effectively only "extensions" are merged, the rest are full overrides.
                    // TODO: make a real merge if needed eventually.
                    if (!"bom".equals(key) && !"extensions".equals(key)) {
                        platformJson.add(key, item);
                    }
                });
            }
        }
        // Write the JSON to the output file
        final File outputDir = outputFile.getParentFile();
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new MojoExecutionException("Failed to create output directory " + outputDir);
            }
        }
        final JsonWriterFactory jsonWriterFactory = Json
                .createWriterFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));
        try (JsonWriter jsonWriter = jsonWriterFactory.createWriter(new FileOutputStream(outputFile))) {
            jsonWriter.writeObject(platformJson.build());
        } catch (JsonException | IOException e) {
            throw new MojoExecutionException("Failed to persist " + outputFile, e);
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

    private JsonObject processDependency(Artifact artifact) throws IOException {
        JsonNode onode = processDependencyToObjectNode(artifact);

        if (onode != null) {
            // TODO: this is a dirty hack to avoid redoing existing javax.json code
            String json = getMapper(false).writeValueAsString(onode);
            try (JsonReader jsonReader = Json.createReader(new StringReader(json))) {
                return jsonReader.readObject();
            }
        } else {
            return null;
        }
    }

    private JsonNode processDependencyToObjectNode(Artifact artifact) throws IOException {
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
    private JsonNode processMetaInfDir(Artifact artifact, Path metaInfDir)
            throws IOException {

        ObjectMapper mapper = null;

        if (!Files.exists(metaInfDir)) {
            return null;
        }
        Path jsonOrYaml = null;

        Path yaml = metaInfDir.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME);
        if (Files.exists(yaml)) {
            mapper = getMapper(true);
            jsonOrYaml = yaml;
        } else {
            mapper = getMapper(false);
            Path json = metaInfDir.resolve(BootstrapConstants.EXTENSION_PROPS_JSON_FILE_NAME);
            if (!Files.exists(json)) {
                final Path props = metaInfDir.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME);
                if (Files.exists(props)) {
                    return mapper.createObjectNode()
                            .put(Extension.ARTIFACT_ID, artifact.getArtifactId())
                            .put(Extension.GROUP_ID, artifact.getGroupId())
                            .put("version", artifact.getVersion())
                            .put("name", artifact.getArtifactId());
                } else {
                    return null;
                }
            } else {
                jsonOrYaml = json;
            }
        }
        return processPlatformArtifact(artifact, jsonOrYaml, mapper);
    }

    private JsonNode processPlatformArtifact(Artifact artifact, Path descriptor, ObjectMapper mapper)
            throws IOException {
        try (InputStream is = Files.newInputStream(descriptor)) {
            ObjectNode object = mapper.readValue(is, ObjectNode.class);
            transformLegacyToNew(object, mapper);
            debug("Adding Quarkus extension %s:%s", object.get(Extension.GROUP_ID), object.get(Extension.ARTIFACT_ID));
            return object;
        } catch (IOException io) {
            throw new IOException("Failed to parse " + descriptor, io);
        }
    }

    private ObjectMapper getMapper(boolean yaml) {

        if (yaml) {
            YAMLFactory yf = new YAMLFactory();
            return new ObjectMapper(yf)
                    .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        } else {
            return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                    .enable(JsonParser.Feature.ALLOW_COMMENTS).enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS)
                    .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        }
    }

    private String extensionId(JsonObject extObject) {
        String artId = extObject.getString(Extension.ARTIFACT_ID, "");
        if (artId.isEmpty()) {
            getLog().warn("Missing artifactId in extension overrides in " + extObject.toString());
        }
        String groupId = extObject.getString(Extension.GROUP_ID, "");
        if (groupId.isEmpty()) {
            return artId;
        } else {
            return extObject.getString(Extension.GROUP_ID, "") + ":" + artId;
        }
    }

    private JsonObject mergeObject(JsonObject extObject, JsonObject extOverride) {
        final JsonObjectBuilder mergedObject = Json.createObjectBuilder(extObject);
        for (Map.Entry<String, JsonValue> e : extOverride.entrySet()) {
            JsonValue.ValueType tp = e.getValue().getValueType();
            if (tp == JsonValue.ValueType.OBJECT
                    && extObject.containsKey(e.getKey())
                    && extObject.get(e.getKey()).getValueType() == JsonValue.ValueType.OBJECT) {
                mergedObject.add(e.getKey(),
                        mergeObject(extObject.get(e.getKey()).asJsonObject(), e.getValue().asJsonObject()));
            } else if (tp == JsonValue.ValueType.ARRAY) {
                final JsonArrayBuilder newArray = Json.createArrayBuilder(e.getValue().asJsonArray());
                mergedObject.add(e.getKey(), newArray.build());
            } else {
                mergedObject.add(e.getKey(), e.getValue());
            }
        }
        return mergedObject.build();
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

    //TODO: duplicate from ExtensionDescriptor - move to shared location?
    private void transformLegacyToNew(ObjectNode extObject, ObjectMapper mapper) {
        ObjectNode metadata = null;

        // Note: groupId and artifactId shouldn't normally be in the source json but
        // just putting it
        // here for completenes
        if (extObject.get("groupId") != null) {
            extObject.set(Extension.GROUP_ID, extObject.get("groupId"));
            extObject.remove("groupId");
        }

        if (extObject.get("artifactId") != null) {
            extObject.set(Extension.ARTIFACT_ID, extObject.get("artifactId"));
            extObject.remove("artifactId");
        }

        JsonNode mvalue = extObject.get("metadata");
        if (mvalue != null && mvalue.isObject()) {
            metadata = (ObjectNode) mvalue;
        } else {
            metadata = mapper.createObjectNode();
        }

        if (extObject.get("labels") != null) {
            metadata.set("keywords", extObject.get("labels"));
            extObject.remove("labels");
        }

        if (extObject.get("guide") != null) {
            metadata.set("guide", extObject.get("guide"));
            extObject.remove("guide");
        }

        if (extObject.get("shortName") != null) {
            metadata.set("short-name", extObject.get("shortName"));
            extObject.remove("shortName");
        }

        extObject.set("metadata", metadata);

    }

    public OverrideInfo getOverrideInfo(File overridesFile) throws MojoExecutionException {
        // Read the overrides file for the extensions (if it exists)
        HashMap extOverrides = new HashMap<>();
        JsonObject theRest = null;
        if (overridesFile.isFile()) {
            info("Found overrides file %s", overridesFile);
            try (JsonReader jsonReader = Json.createReader(new FileInputStream(overridesFile))) {
                JsonObject overridesObject = jsonReader.readObject();
                JsonArray extOverrideObjects = overridesObject.getJsonArray("extensions");
                if (extOverrideObjects != null) {
                    // Put the extension overrides into a map keyed to their GAV
                    for (JsonValue val : extOverrideObjects) {
                        JsonObject extOverrideObject = val.asJsonObject();
                        String key = extensionId(extOverrideObject);
                        extOverrides.put(key, extOverrideObject);
                    }
                }

                theRest = overridesObject;
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to read " + overridesFile, e);
            }
            return new OverrideInfo(extOverrides, theRest);

        } else {
            throw new MojoExecutionException(overridesFile + " not found.");
        }
    }

    private static class OverrideInfo {
        private Map<String, JsonObject> extOverrides;
        private JsonObject theRest;

        public OverrideInfo(Map<String, JsonObject> extOverrides, JsonObject theRest) {
            this.extOverrides = extOverrides;
            this.theRest = theRest;
        }

        public Map<String, JsonObject> getExtOverrides() {
            return extOverrides;
        }

        public JsonObject getTheRest() {
            return theRest;
        }

        public void setExtOverrides(Map<String, JsonObject> extOverrides) {
            this.extOverrides = extOverrides;
        }

        public void setTheRest(JsonObject theRest) {
            this.theRest = theRest;
        }
    }
}
