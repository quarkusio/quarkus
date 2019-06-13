package io.quarkus.maven.extensionlist;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.quarkus.dependencies.Extension;
import io.quarkus.maven.AbstractExtensionMojo;

/**
 * @author <a href="http://kenfinnigan.me">Ken Finnigan</a>
 */
@Mojo(name = "extension-list", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresDependencyCollection = ResolutionScope.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ExtensionListMojo extends AbstractExtensionMojo {

    //TODO Change file name when we replace the current hand coded file
    private static final String FILE_NAME = "extension-list";

    @Parameter(defaultValue = "${project.version}", readonly = true)
    private String version;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Extension> extensions = extensions();

        generateJSON(extensions);
        generateJavascript(extensions);
    }

    private void generateJSON(Set<Extension> extensions) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        File outFile = new File(this.project.getBuild().getOutputDirectory(), FILE_NAME + ".json");

        outFile.getParentFile().mkdirs();

        try {
            mapper.writeValue(outFile, extensions);
        } catch (IOException e) {
            getLog().error(e);
        }

        addArtifact("json", outFile);
    }

    private void generateJavascript(Set<Extension> extensions) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);

        File outFile = new File(this.project.getBuild().getOutputDirectory(), FILE_NAME + ".js");
        try (BufferedWriter writer = Files.newBufferedWriter(outFile.toPath())) {
            writer.write("version = '" + version + "';");
            writer.write("extensionList = ");
            mapper.writeValue(writer, extensions);
            writer.write(";");
            writer.flush();
        } catch (IOException e) {
            getLog().error(e);
        }

        addArtifact("js", outFile);
    }

    private void addArtifact(String type, File file) {
        DefaultArtifact artifact = new DefaultArtifact(
                this.project.getGroupId(),
                this.project.getArtifactId(),
                this.project.getVersion(),
                "compile",
                type,
                "",
                new DefaultArtifactHandler(type));

        artifact.setFile(file);
        this.project.addAttachedArtifact(artifact);
    }
}
