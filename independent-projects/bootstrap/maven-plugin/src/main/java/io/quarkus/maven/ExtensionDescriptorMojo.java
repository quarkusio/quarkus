package io.quarkus.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.quarkus.bootstrap.BootstrapConstants;

/**
 * Generates Quarkus extension descriptor for the runtime artifact.
 *
 * <p/>
 * Also generates META-INF/quarkus-extension.json which includes properties of
 * the extension such as name, labels, maven coordinates, etc that are used by
 * the tools.
 *
 * @author Alexey Loubyansky
 */
@Mojo(name = "extension-descriptor", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ExtensionDescriptorMojo extends AbstractMojo {

    private static final String GROUP_ID = "group-id";
    private static final String ARTIFACT_ID = "artifact-id";

    private static DefaultPrettyPrinter prettyPrinter = null;

    /**
     * The entry point to Aether, i.e. the component doing all the work.
     *
     * @component
     */
    @Component
    private RepositorySystem repoSystem;

    /**
     * The current repository/network configuration of Maven.
     *
     * @parameter default-value="${repositorySystemSession}"
     * @readonly
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    /**
     * The project's remote repositories to use for the resolution of artifacts and
     * their dependencies.
     *
     * @parameter default-value="${project.remoteProjectRepositories}"
     * @readonly
     */
    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(required = true, defaultValue = "${project.groupId}:${project.artifactId}-deployment:${project.version}")
    private String deployment;

    @Parameter(required = true, defaultValue = "${project.build.outputDirectory}/META-INF/quarkus-extension.yaml")
    private File extensionFile;

    @Parameter(defaultValue = "${project}")
    protected MavenProject project;

    /**
     * Artifacts that should never end up in the final build. Usually this should only be set if we know
     * this extension provides a newer version of a given artifact that is under a different GAV. E.g. this
     * can be used to make sure that the legacy javax API's are not included if an extension is using the new
     * Jakarta version.
     */
    @Parameter
    List<String> excludedArtifacts;

    /**
     * Artifacts that are always loaded parent first when running in dev or test mode. This is an advanced option
     * and should only be used if you are sure that this is the correct solution for the use case.
     *
     * A possible example of this would be logging libraries, as these need to be loaded by the system class loader.
     */
    @Parameter
    List<String> parentFirstArtifacts;

    @Override
    public void execute() throws MojoExecutionException {

        prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

        final Properties props = new Properties();
        props.setProperty(BootstrapConstants.PROP_DEPLOYMENT_ARTIFACT, deployment);
        final Path output = outputDirectory.toPath().resolve(BootstrapConstants.META_INF);

        if (parentFirstArtifacts != null && !parentFirstArtifacts.isEmpty()) {
            String val = String.join(",", parentFirstArtifacts);
            props.put(BootstrapConstants.PARENT_FIRST_ARTIFACTS, val);
        }

        if (excludedArtifacts != null && !excludedArtifacts.isEmpty()) {
            String val = String.join(",", excludedArtifacts);
            props.put(BootstrapConstants.EXCLUDED_ARTIFACTS, val);
        }

        try {
            Files.createDirectories(output);
            try (BufferedWriter writer = Files
                    .newBufferedWriter(output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME))) {
                props.store(writer, "Generated by extension-descriptor");
            }
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to persist extension descriptor " + output.resolve(BootstrapConstants.DESCRIPTOR_FILE_NAME),
                    e);
        }

        // extension.json
        if (extensionFile == null) {
            extensionFile = new File(outputDirectory,
                    "META-INF" + File.separator + BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME);
        }

        ObjectNode extObject;
        if (!extensionFile.exists()) {
            // if does not exist look for fallback .json
            extensionFile = new File(extensionFile.getParent(), "quarkus-extension.json");
        }

        ObjectMapper mapper = null;
        if (extensionFile.exists()) {
            mapper = getMapper(extensionFile.toString().endsWith(".yaml"));

            try {
                extObject = processPlatformArtifact(extensionFile.toPath(), mapper);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to parse " + extensionFile, e);
            }
        } else {
            mapper = getMapper(true);
            extObject = getMapper(true).createObjectNode();
        }

        transformLegacyToNew(output, extObject, mapper);

        if (extObject.get("groupId") == null) {
            extObject.put(GROUP_ID, project.getGroupId());
        }
        if (extObject.get("artifactId") == null) {
            extObject.put(ARTIFACT_ID, project.getArtifactId());
        }
        if (extObject.get("version") == null) {
            extObject.put("version", project.getVersion());
        }
        if (extObject.get("name") == null) {
            if (project.getName() != null) {
                extObject.put("name", project.getName());
            } else {
                JsonNode node = extObject.get(ARTIFACT_ID);
                String defaultName = node.asText();
                int i = 0;
                if (defaultName.startsWith("quarkus-")) {
                    i = "quarkus-".length();
                }
                final StringBuilder buf = new StringBuilder();
                boolean startWord = true;
                while (i < defaultName.length()) {
                    final char c = defaultName.charAt(i++);
                    if (c == '-') {
                        if (!startWord) {
                            buf.append(' ');
                            startWord = true;
                        }
                    } else if (startWord) {
                        buf.append(Character.toUpperCase(c));
                        startWord = false;
                    } else {
                        buf.append(c);
                    }
                }
                defaultName = buf.toString();
                getLog().warn("Extension name has not been provided for " + extObject.get(GROUP_ID).asText("") + ":"
                        + extObject.get("artifact-id").asText("") + "! Using '" + defaultName
                        + "' as the default one.");
                extObject.put("name", defaultName);
            }
        }
        if (!extObject.has("description") && project.getDescription() != null) {
            extObject.put("description", project.getDescription());
        }

        try (BufferedWriter bw = Files
                .newBufferedWriter(output.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME))) {
            bw.write(getMapper(true).writer(prettyPrinter).writeValueAsString(extObject));
        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Failed to persist " + output.resolve(BootstrapConstants.QUARKUS_EXTENSION_FILE_NAME), e);
        }
    }

    private void transformLegacyToNew(final Path output, ObjectNode extObject, ObjectMapper mapper)
            throws MojoExecutionException {
        ObjectNode metadata = null;

        // Note: groupId and artifactId shouldn't normally be in the source json but
        // just putting it
        // here for completenes
        if (extObject.get("groupId") != null) {
            extObject.set(GROUP_ID, extObject.get("groupId"));
            extObject.remove("groupId");
        }

        if (extObject.get("artifactId") != null) {
            extObject.set(ARTIFACT_ID, extObject.get("artifactId"));
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

        //   updateSourceFiles(output, extObject, mapper);

    }

    /**
     * parse yaml or json and then return jackson JSonNode for furhter processing
     * 
     ***/
    private ObjectNode processPlatformArtifact(Path descriptor, ObjectMapper mapper)
            throws IOException {
        try (InputStream is = Files.newInputStream(descriptor)) {
            return mapper.readValue(is, ObjectNode.class);
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
}
