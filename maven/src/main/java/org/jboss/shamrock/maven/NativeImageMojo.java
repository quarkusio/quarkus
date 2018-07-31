package org.jboss.shamrock.maven;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "native-image", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class NativeImageMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.directory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/wiring-classes")
    private File wiringClassesDirectory;

    @Parameter(defaultValue = "false")
    private boolean reportErrorsAtRuntime;

    @Parameter(defaultValue = "false")
    private boolean debugSymbols;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.finalName}")
    private String finalName;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        List<String> nativeImage;
        String graalvmCmd = System.getenv("GRAALVM_NATIVE_IMAGE_CMD");
        if (graalvmCmd != null) {
            // E.g. "/usr/bin/docker run -v {{PROJECT_DIR}}:/project --rm protean/graalvm-native-image"
            nativeImage = new ArrayList<>();
            Collections.addAll(nativeImage, graalvmCmd.replace("{{PROJECT_DIR}}", outputDirectory.getAbsolutePath()).split(" "));
        } else {
            String graalvmHome = System.getenv("GRAALVM_HOME");
            if (graalvmHome == null) {
                throw new MojoFailureException("GRAALVM_HOME was not set");
            }
            nativeImage = Collections.singletonList(graalvmHome + File.separator + "bin" + File.separator + "native-image");
        }

        try {
            List<String> command = new ArrayList<>();
            command.addAll(nativeImage);
            command.add("--no-server");
            command.add("-jar");
            command.add(finalName + "-runner.jar");
            command.add("-H:IncludeResources=META-INF/.*");
            if (reportErrorsAtRuntime) {
                command.add("-H:+ReportUnsupportedElementsAtRuntime");
            }
            if (debugSymbols) {
                command.add("-g");
            }
            System.out.println(command);
            Process process = Runtime.getRuntime().exec(command.toArray(new String[0]), null, outputDirectory);
            new Thread(new ProcessReader(process.getInputStream())).start();
            new Thread(new ProcessReader(process.getErrorStream())).start();
            if (process.waitFor() != 0) {
                throw new RuntimeException("Image generation failed");
            }
            System.setProperty("native.image.path", finalName + "-runner");

        } catch (Exception e) {
            throw new MojoFailureException("Failed to build native image", e);
        }
    }

    private static final class ProcessReader implements Runnable {

        private final InputStream inputStream;

        private ProcessReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            byte[] b = new byte[100];
            int i;
            try {
                while ((i = inputStream.read(b)) > 0) {
                    System.out.print(new String(b, 0, i));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
