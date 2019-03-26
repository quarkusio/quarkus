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

package io.quarkus.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import io.quarkus.creator.AppCreator;
import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.phase.augment.AugmentOutcome;
import io.quarkus.creator.phase.nativeimage.NativeImageOutcome;
import io.quarkus.creator.phase.nativeimage.NativeImagePhase;
import io.quarkus.creator.phase.runnerjar.RunnerJarOutcome;

@Mojo(name = "native-image", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class NativeImageMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

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
    private boolean enableIsolates;

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
    private String dockerBuild;

    @Parameter(defaultValue = "false")
    private boolean enableVMInspection;

    @Parameter(defaultValue = "true")
    private boolean fullStackTraces;

    @Parameter(defaultValue = "${native-image.disable-reports}")
    private boolean disableReports;

    @Parameter
    private List<String> additionalBuildArgs;

    @Parameter(defaultValue = "false")
    private boolean addAllCharsets;

    public NativeImageMojo() {
        MojoLogger.logSupplier = this::getLog;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (!buildDir.isDirectory() || !new File(buildDir, "lib").isDirectory()) {
            throw new MojoFailureException("Unable to find the required build output. " +
                    "Please ensure that the 'build' goal has been properly configured for the project - since it is a prerequisite of the 'native-image' goal");
        }

        try (AppCreator appCreator = AppCreator.builder()
                // configure the build phase we want the app to go through
                .addPhase(new NativeImagePhase()
                        .setAddAllCharsets(addAllCharsets)
                        .setAdditionalBuildArgs(additionalBuildArgs)
                        .setAutoServiceLoaderRegistration(autoServiceLoaderRegistration)
                        .setOutputDir(buildDir.toPath())
                        .setCleanupServer(cleanupServer)
                        .setDebugBuildProcess(debugBuildProcess)
                        .setDebugSymbols(debugSymbols)
                        .setDisableReports(disableReports)
                        .setDockerBuild(dockerBuild)
                        .setDumpProxies(dumpProxies)
                        .setEnableAllSecurityServices(enableAllSecurityServices)
                        .setEnableCodeSizeReporting(enableCodeSizeReporting)
                        .setEnableHttpsUrlHandler(enableHttpsUrlHandler)
                        .setEnableHttpUrlHandler(enableHttpUrlHandler)
                        .setEnableIsolates(enableIsolates)
                        .setEnableJni(enableJni)
                        .setEnableRetainedHeapReporting(enableRetainedHeapReporting)
                        .setEnableServer(enableServer)
                        .setEnableVMInspection(enableVMInspection)
                        .setFullStackTraces(fullStackTraces)
                        .setGraalvmHome(graalvmHome)
                        .setNativeImageXmx(nativeImageXmx)
                        .setReportErrorsAtRuntime(reportErrorsAtRuntime))

                .build()) {

            appCreator
                    // this mojo runs on the assumption that the outcomes of the augmentation and runner jar building phases
                    // are already available
                    .pushOutcome(AugmentOutcome.class, new AugmentOutcome() {
                        final Path classesDir = new File(outputDirectory, "classes").toPath();

                        @Override
                        public Path getAppClassesDir() {
                            return classesDir;
                        }

                        @Override
                        public Path getTransformedClassesDir() {
                            // not relevant for this mojo
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public Path getWiringClassesDir() {
                            // not relevant for this mojo
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public Path getConfigDir() {
                            return classesDir;
                        }
                    })
                    .pushOutcome(RunnerJarOutcome.class, new RunnerJarOutcome() {
                        final Path runnerJar = buildDir.toPath().resolve(finalName + "-runner.jar");

                        @Override
                        public Path getRunnerJar() {
                            return runnerJar;
                        }

                        @Override
                        public Path getLibDir() {
                            return runnerJar.getParent().resolve("lib");
                        }
                    })
                    // resolve the outcome of the native image phase
                    .resolveOutcome(NativeImageOutcome.class);

        } catch (AppCreatorException e) {
            throw new MojoExecutionException("Failed to generate a native image", e);
        }
    }
}
