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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.toolchain.ToolchainManager;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.maven.components.MavenVersionEnforcer;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.remotedev.AgentRunner;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigProviderResolver;

/**
 * The dev mojo, that connects to a remote host
 * <p>
 * You can launch forked app directly with {@code dev}
 */
@Mojo(name = "remote-dev", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class RemoteDevMojo extends AbstractMojo {

    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceDir;

    @Parameter(defaultValue = "${jvm.args}")
    private String jvmArgs;

    @Parameter(defaultValue = "${session}")
    private MavenSession session;

    @Parameter(defaultValue = "TRUE")
    private boolean deleteDevJar;

    @Component
    private MavenVersionEnforcer mavenVersionEnforcer;

    @Component
    private ToolchainManager toolchainManager;

    public ToolchainManager getToolchainManager() {
        return toolchainManager;
    }

    public MavenSession getSession() {
        return session;
    }

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        mavenVersionEnforcer.ensureMavenVersion(getLog(), session);
        boolean found = false;
        for (Plugin i : project.getBuildPlugins()) {
            if (i.getGroupId().equals(MojoUtils.getPluginGroupId())
                    && i.getArtifactId().equals(MojoUtils.getPluginArtifactId())) {
                for (PluginExecution p : i.getExecutions()) {
                    if (p.getGoals().contains("build")) {
                        found = true;
                        break;
                    }
                }
            }
        }
        if (!found) {
            getLog().warn("The quarkus-maven-plugin build goal was not configured for this project, " +
                    "skipping quarkus:remote-dev as this is assumed to be a support library. If you want to run Quarkus remote-dev"
                    +
                    " on this project make sure the quarkus-maven-plugin is configured with a build goal.");
            return;
        }

        if (!sourceDir.isDirectory()) {
            throw new MojoFailureException("The `src/main/java` directory is required, please create it.");
        }

        String resources = null;
        for (Resource i : project.getBuild().getResources()) {
            //todo: support multiple resources dirs for config hot deployment
            resources = i.getDirectory();
            break;
        }

        String classes = outputDirectory.getAbsolutePath();
        String sources = sourceDir.getAbsolutePath();

        //first lets look for some config, as it is not on the current class path
        //and we need to load it to run the build process
        if (resources != null) {
            Path config = Paths.get(resources).resolve("application.properties");
            if (Files.exists(config)) {
                try {
                    Config built = SmallRyeConfigProviderResolver.instance().getBuilder()
                            .addDefaultSources()
                            .addDiscoveredConverters()
                            .addDiscoveredSources()
                            .withSources(new PropertiesConfigSource(config.toUri().toURL())).build();
                    SmallRyeConfigProviderResolver.instance().registerConfig(built,
                            Thread.currentThread().getContextClassLoader());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        Optional<String> url = ConfigProvider.getConfig().getOptionalValue("quarkus.hot-reload.url", String.class);
        Optional<String> password = ConfigProvider.getConfig().getOptionalValue("quarkus.hot-reload.password",
                String.class);
        if (!url.isPresent()) {
            throw new MojoFailureException("To use remote-dev you must specify quarkus.hot-reload.url");
        }
        if (!password.isPresent()) {
            throw new MojoFailureException("To use remote-dev you must specify quarkus.hot-reload.password");
        }
        System.out.println(sources);
        AgentRunner runner = new AgentRunner(resources, sources, classes, url.get() + "/quarkus/hot-reload",
                password.get());

        runner.run();
        for (;;) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
