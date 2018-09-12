package org.jboss.shamrock.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    @Parameter(defaultValue = "${native-image.debug-build-process}")
    private boolean debugBuildProcess;

    @Parameter(readonly = true, required = true, defaultValue = "${project.build.finalName}")
    private String finalName;

    @Parameter(defaultValue = "${native-image.new-server}")
    private boolean cleanupServer;

    @Parameter
    private boolean enableHttpUrlHandler;

    @Parameter
    private boolean enableRetainedHeapReporting;

    @Parameter
    private boolean enableCodeSizeReporting;

    @Parameter(defaultValue = "${env.GRAALVM_HOME}")
    private String graalvmHome;

    @Parameter(defaultValue = "false")
    private boolean enableServer;

    @Parameter(defaultValue = "false")
    private boolean enableJni;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        HashMap<String, String> env = new HashMap<>(System.getenv());
        List<String> nativeImage;
        String graalvmCmd = System.getenv("GRAALVM_NATIVE_IMAGE_CMD");
        if (graalvmCmd != null) {
            // E.g. "/usr/bin/docker run -v {{PROJECT_DIR}}:/project --rm protean/graalvm-native-image"
            nativeImage = new ArrayList<>();
            Collections.addAll(nativeImage, graalvmCmd.replace("{{PROJECT_DIR}}", outputDirectory.getAbsolutePath()).split(" "));
        } else {
            if (graalvmHome == null) {
                throw new MojoFailureException("GRAALVM_HOME was not set");
            }
            env.put("GRAALVM_HOME", graalvmHome);
            nativeImage = Collections.singletonList(graalvmHome + File.separator + "bin" + File.separator + "native-image");
        }

        try {
            List<String> command = new ArrayList<>();
            command.addAll(nativeImage);
            if (cleanupServer) {
                List<String> cleanup = new ArrayList<>(nativeImage);
                cleanup.add("--server-shutdown");
                Process process = Runtime.getRuntime().exec(cleanup.toArray(new String[0]), null, outputDirectory);
                new Thread(new ProcessReader(process.getInputStream(), false)).start();
                new Thread(new ProcessReader(process.getErrorStream(), true)).start();
                process.waitFor();
            }
            command.add("-jar");
            command.add(finalName + "-runner.jar");
            //https://github.com/oracle/graal/issues/660
            command.add("-J-Djava.util.concurrent.ForkJoinPool.common.parallelism=1");
            if (reportErrorsAtRuntime) {
                command.add("-H:+ReportUnsupportedElementsAtRuntime");
            }
            if (debugSymbols) {
                command.add("-g");
            }
            if (debugBuildProcess) {
                command.add("-J-Xdebug");
                command.add("-J-Xnoagent");
                command.add("-J-Djava.compiler=NONE");
                command.add("-J-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y");
            }
            if(enableHttpUrlHandler) {
                command.add("-H:EnableURLProtocols=http");
            }
            if (enableRetainedHeapReporting) {
                command.add("-H:+PrintRetainedHeapHistogram");
            }
            if (enableCodeSizeReporting) {
                command.add("-H:+PrintCodeSizeReport");
            }
            if (enableJni) {
                command.add("-H:+JNI");
            }
            if(!enableServer) {
                command.add("--no-server");
            }
            //command.add("-H:+AllowVMInspection");
            System.out.println(command);
            Process process = Runtime.getRuntime().exec(command.toArray(new String[0]), null, outputDirectory);
            new Thread(new ProcessReader(process.getInputStream(), false)).start();
            new Thread(new ProcessReader(process.getErrorStream(), true)).start();
            if (process.waitFor() != 0) {
                throw new RuntimeException("Image generation failed");
            }
            System.setProperty("native.image.path", finalName + "-runner");

        } catch (Exception e) {
            throw new MojoFailureException("Failed to build native image", e);
        }
    }

}
