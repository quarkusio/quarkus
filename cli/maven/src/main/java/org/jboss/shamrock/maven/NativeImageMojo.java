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
import org.jboss.shamrock.creator.AppArtifact;
import org.jboss.shamrock.creator.AppCreator;
import org.jboss.shamrock.creator.AppCreatorException;
import org.jboss.shamrock.creator.phase.augment.AugmentOutcome;
import org.jboss.shamrock.creator.phase.nativeimage.NativeImagePhase;
import org.jboss.shamrock.creator.phase.runnerjar.RunnerJarOutcome;
import org.jboss.shamrock.creator.resolver.maven.ResolvedMavenArtifactDeps;

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
        try {
            new AppCreator()
            // init the resolver with the project dependencies
            .setArtifactResolver(new ResolvedMavenArtifactDeps(project.getGroupId(), project.getArtifactId(),
                    project.getVersion(), project.getArtifacts()))

            // this mojo runs on the assumption that the outcomes of the augmentation and runner jar building phases
            // are already available
            .pushOutcome(AugmentOutcome.class, new AugmentOutcome() {
                final Path classesDir = new File(outputDirectory, "classes").toPath();
                @Override
                public Path getAppClassesDir() {
                    return classesDir;
                }
                @Override
                public Path getWiringClassesDir() {
                    return wiringClassesDirectory.toPath();
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

            // add the native phase
            .addPhase(new NativeImagePhase()
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
                    .setReportErrorsAtRuntime(reportErrorsAtRuntime)
                    )
            .create(new AppArtifact(project.getGroupId(), project.getArtifactId(), project.getVersion()));
        } catch (AppCreatorException e) {
            throw new MojoExecutionException("Failed to create application", e);
        }
    }
}
