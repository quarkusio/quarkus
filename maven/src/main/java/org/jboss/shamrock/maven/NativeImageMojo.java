/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.microprofile.config.Config;

import io.smallrye.config.SmallRyeConfigProviderResolver;

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
    private boolean enableHttpsUrlHandler;

    @Parameter
    private boolean enableAllSecurityServices;

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

    @Parameter(defaultValue = "false")
    private boolean autoServiceLoaderRegistration;

    @Parameter(defaultValue = "false")
    private boolean dumpProxies;

    @Parameter(defaultValue = "${native-image.xmx}")
    private String nativeImageXmx;

    @Parameter(defaultValue = "${native-image.docker-build}")
    private boolean dockerBuild;

    @Parameter(defaultValue = "false")
    private boolean enableVMInspection;

    @Parameter(defaultValue = "true")
    private boolean fullStackTraces;

    @Parameter(defaultValue = "${native-image.disable-reports}")
    private boolean disableReports;

    @Parameter
    private List<String> additionalBuildArgs;

    public NativeImageMojo() {
        MojoLogger.logSupplier = this::getLog;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Config config = SmallRyeConfigProviderResolver.instance().getConfig();
        
        boolean vmVersionOutOfDate = isThisGraalVMRC7();

        HashMap<String, String> env = new HashMap<>(System.getenv());
        List<String> nativeImage;
        if (dockerBuild) {

            // E.g. "/usr/bin/docker run -v {{PROJECT_DIR}}:/project --rm protean/graalvm-native-image"
            nativeImage = new ArrayList<>();
            //TODO: use an 'official' image
            Collections.addAll(nativeImage, "docker", "run", "-v",outputDirectory.getAbsolutePath() + ":/project:z", "--rm", "swd847/centos-graal-native-image");

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
                ProcessBuilder pb = new ProcessBuilder(cleanup.toArray(new String[0]));
                pb.directory(outputDirectory);
                pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                Process process = pb.start();
                process.waitFor();
            }
            // TODO this is a temp hack
            final File propsFile = new File(outputDirectory, "classes/native-image.properties");
            if (propsFile.exists()) {
                final Properties properties = new Properties();
                try (FileInputStream is = new FileInputStream(propsFile)) {
                    try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        properties.load(isr);
                    }
                }
                for (String propertyName : properties.stringPropertyNames()) {
                    final String propertyValue = properties.getProperty(propertyName);
                    // todo maybe just -D is better than -J-D in this case
                    if (propertyValue == null) {
                        command.add("-J-D" + propertyName);
                    } else {
                        command.add("-J-D" + propertyName + "=" + propertyValue);
                    }
                }
            }
            if(config != null) {
                if(config.getOptionalValue("shamrock.ssl.native", Boolean.class).orElse(false)) {
                    enableHttpsUrlHandler = true;
                    enableJni = true;
                    enableAllSecurityServices = true;
                }
            }
            if (additionalBuildArgs != null) {
                additionalBuildArgs.forEach(command::add);
            }
            command.add("-H:InitialCollectionPolicy=com.oracle.svm.core.genscavenge.CollectionPolicy$BySpaceAndTime"); //the default collection policy results in full GC's 50% of the time
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
                command.add("-J-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=y");
            }
            if(!disableReports) {
                command.add("-H:+PrintAnalysisCallTree");
            }
            if (dumpProxies) {
                command.add("-Dsun.misc.ProxyGenerator.saveGeneratedFiles=true");
                if (enableServer) {
                    getLog().warn( "Options dumpProxies and enableServer are both enabled: this will get the proxies dumped in an unknown external working directory" );
                }
            }
            if(nativeImageXmx != null) {
                command.add("-J-Xmx" + nativeImageXmx);
            }
            List<String> protocols = new ArrayList<>(2);
            if(enableHttpUrlHandler) {
                protocols.add("http");
            }
            if(enableHttpsUrlHandler) {
                protocols.add("https");
            }
            if(!protocols.isEmpty()) {
                command.add("-H:EnableURLProtocols="+String.join(",", protocols));
            }
            if(enableAllSecurityServices) {
                command.add("--enable-all-security-services");
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
            else {
                command.add("-H:-JNI");
            }
            if(!enableServer) {
                command.add("--no-server");
            }
            if (enableVMInspection) {
                command.add("-H:+AllowVMInspection");
            }
            if (autoServiceLoaderRegistration) {
                if (!vmVersionOutOfDate) {
                    command.add( "-H:+UseServiceLoaderFeature" );
                    //When enabling, at least print what exactly is being added:
                    command.add( "-H:+TraceServiceLoaderFeature" );
                }
            }
            else {
                if (!vmVersionOutOfDate) {
                    command.add( "-H:-UseServiceLoaderFeature" );
                }
            }
            if (fullStackTraces) {
                command.add("-H:+StackTrace");
            }
            else {
                command.add("-H:-StackTrace");
            }

            System.out.println(command);
            CountDownLatch errorReportLatch = new CountDownLatch(1);


            ProcessBuilder pb = new ProcessBuilder(command.toArray(new String[0]));
            pb.directory(outputDirectory);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

            Process process = pb.start();
            new Thread(new ErrorReplacingProcessReader(process.getErrorStream(), new File(outputDirectory, "reports"), errorReportLatch)).start();
            errorReportLatch.await();
            if (process.waitFor() != 0) {
                throw new RuntimeException("Image generation failed");
            }
            System.setProperty("native.image.path", finalName + "-runner");

        } catch (Exception e) {
            throw new MojoFailureException("Failed to build native image", e);
        }
    }

    //FIXME remove after transition period
    private boolean isThisGraalVMRC7() {
        final String vmName = System.getProperty( "java.vm.name" );
        getLog().info( "Running Shamrock native-image plugin on " + vmName );
        if ( vmName.contains( "-rc7" ) ) {
            getLog().error( "GraalVM rc7 detected! Please upgrade" );
            return true;
        }
        return false;
    }

}
