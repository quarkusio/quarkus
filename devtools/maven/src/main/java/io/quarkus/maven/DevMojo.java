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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
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
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.deployment.ApplicationInfoUtil;
import io.quarkus.dev.DevModeContext;
import io.quarkus.dev.DevModeMain;
import io.quarkus.maven.components.MavenVersionEnforcer;
import io.quarkus.maven.utilities.MojoUtils;

/**
 * The dev mojo, that runs a quarkus app in a forked process. It's mean hot deployment with background compilation.
 * <p>
 * You can use this dev mode in a remote container environment with {@code remote-dev}
 */
@Mojo(name = "dev", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DevMojo extends AbstractMojo {
    /**
     * The directory for compiled classes.
     */
    @Parameter(readonly = true, required = true, defaultValue = "${project.build.outputDirectory}")
    private File outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * If this server should be started in debug mode. The default is to start in debug mode without suspending and listen on
     * port 5005. It supports the following options:
     * <table>
     * <tr>
     * <td><b>Value</b></td>
     * <td>Effect</td>
     * </tr>
     * <tr>
     * <td><b>false</b></td>
     * <td>The JVM is not started in debug mode</td>
     * </tr>
     * <tr>
     * <td><b>true</b></td>
     * <td>The JVM is started in debug mode and suspends until a debugger is attached to port 5005</td>
     * </tr>
     * <tr>
     * <td><b>client</b></td>
     * <td>The JVM is started in client mode, and attempts to connect to localhost:5005</td>
     * </tr>
     * <tr>
     * <td><b>{port}</b></td>
     * <td>The JVM is started in debug mode and suspends until a debugger is attached to {port}</td>
     * </tr>
     * </table>
     */
    @Parameter(defaultValue = "${debug}")
    private String debug;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

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
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    private List<RemoteRepository> repos;

    /**
     * This value is intended to be set to true when some generated bytecode
     * is erroneous causing the JVM to crash when the verify:none option is set (which is on by default)
     */
    @Parameter(defaultValue = "${preventnoverify}")
    private boolean preventnoverify = false;

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
                    "skipping quarkus:dev as this is assumed to be a support library. If you want to run quarkus dev" +
                    " on this project make sure the quarkus-maven-plugin is configured with a build goal.");
            return;
        }

        if (!sourceDir.isDirectory()) {
            getLog().warn("The `src/main/java` directory does not exist");
        }

        if (!buildDir.isDirectory() || !new File(buildDir, "classes").isDirectory()) {
            throw new MojoFailureException("The project has no output yet, run `mvn compile quarkus:dev`.");
        }

        try {
            List<String> args = new ArrayList<>();
            String javaTool = findJavaTool();
            getLog().debug("Using javaTool: " + javaTool);
            args.add(javaTool);
            if (debug == null) {
                // debug mode not specified
                // make sure 5005 is not used, we don't want to just fail if something else is using it
                try (Socket socket = new Socket(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), 5005)) {
                    getLog().error("Port 5005 in use, not starting in debug mode");
                } catch (IOException e) {
                    args.add("-Xdebug");
                    args.add("-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=n");
                }
            } else if (debug.toLowerCase().equals("client")) {
                args.add("-Xdebug");
                args.add("-Xrunjdwp:transport=dt_socket,address=localhost:5005,server=n,suspend=n");
            } else if (debug.toLowerCase().equals("true")) {
                args.add("-Xdebug");
                args.add("-Xrunjdwp:transport=dt_socket,address=localhost:5005,server=y,suspend=y");
            } else if (!debug.toLowerCase().equals("false")) {
                try {
                    int port = Integer.parseInt(debug);
                    if (port <= 0) {
                        throw new MojoFailureException("The specified debug port must be greater than 0");
                    }
                    args.add("-Xdebug");
                    args.add("-Xrunjdwp:transport=dt_socket,address=" + port + ",server=y,suspend=y");
                } catch (NumberFormatException e) {
                    throw new MojoFailureException(
                            "Invalid value for debug parameter: " + debug + " must be true|false|client|{port}");
                }
            }
            if (jvmArgs != null) {
                args.addAll(Arrays.asList(jvmArgs.split(" ")));
            }

            // the following flags reduce startup time and are acceptable only for dev purposes
            args.add("-XX:TieredStopAtLevel=1");
            if (!preventnoverify) {
                args.add("-Xverify:none");
            }

            //build a class-path string for the base platform
            //this stuff does not change
            // Do not include URIs in the manifest, because some JVMs do not like that
            StringBuilder classPathManifest = new StringBuilder();
            DevModeContext devModeContext = new DevModeContext();
            for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
                devModeContext.getSystemProperties().put(e.getKey().toString(), (String) e.getValue());
            }

            final AppModel appModel;
            try {
                final LocalProject localProject = LocalProject
                        .resolveLocalProjectWithWorkspace(LocalProject.locateCurrentProjectDir(outputDirectory.toPath()));
                //we need to establish a partial ordering of the projects (i.e. 'reactor build order')

                List<AppArtifactKey> orderedProjects = new ArrayList<>();
                HashSet<AppArtifactKey> toplace = new HashSet<>();
                for (Map.Entry<AppArtifactKey, LocalProject> i : localProject.getWorkspace().getProjects().entrySet()) {
                    toplace.add(i.getKey());
                }
                //TODO: there is probably a better algorithm than this to establish the partial ordering
                //basically we just iterate and add anything to the list that has not dependencies in 'toplace'
                //this has worst case performance of O(n^2), if there is a linear relationship between
                //the modules and the original order is reversed. As N is generally fairly small we can live with this for now
                for (;;) {
                    boolean changed = false;
                    Iterator<AppArtifactKey> it = toplace.iterator();
                    while (it.hasNext()) {
                        AppArtifactKey current = it.next();
                        LocalProject project = localProject.getWorkspace().getProjects().get(current);
                        boolean canPlace = true;
                        for (Dependency dep : project.getRawModel().getDependencies()) {
                            AppArtifactKey key = new AppArtifactKey(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(),
                                    dep.getType());
                            if (toplace.contains(key)) {
                                canPlace = false;
                                break;
                            }
                        }
                        if (canPlace) {
                            changed = true;
                            orderedProjects.add(current);
                            it.remove();
                        }
                    }
                    if (toplace.isEmpty()) {
                        break;
                    }
                    if (!changed) {
                        throw new MojoFailureException("Failed to establish partial ordering between projects "
                                + localProject.getWorkspace().getProjects().keySet());
                    }

                }
                for (AppArtifactKey i : orderedProjects) {
                    String sourcePath = null;
                    String classesPath = null;
                    String resourcePath = null;
                    LocalProject project = localProject.getWorkspace().getProjects().get(i);
                    Path javaSourcesDir = project.getSourcesSourcesDir();
                    if (Files.isDirectory(javaSourcesDir)) {
                        sourcePath = javaSourcesDir.toAbsolutePath().toString();
                    }
                    Path classesDir = project.getClassesDir();
                    if (Files.isDirectory(classesDir)) {
                        classesPath = classesDir.toAbsolutePath().toString();
                    }
                    Path resourcesSourcesDir = project.getResourcesSourcesDir();
                    if (Files.isDirectory(resourcesSourcesDir)) {
                        resourcePath = resourcesSourcesDir.toAbsolutePath().toString();
                    }
                    DevModeContext.ModuleInfo moduleInfo = new DevModeContext.ModuleInfo(sourcePath, classesPath, resourcePath);
                    devModeContext.getModules().add(moduleInfo);
                }

                String resources = null;
                for (Resource i : project.getBuild().getResources()) {
                    //todo: support multiple resources dirs for config hot deployment
                    resources = i.getDirectory();
                    break;
                }

                appModel = new BootstrapAppModelResolver(MavenArtifactResolver.builder()
                        .setRepositorySystem(repoSystem)
                        .setRepositorySystemSession(repoSession)
                        .setRemoteRepositories(repos)
                        .setWorkspace(localProject.getWorkspace())
                        .build())
                                .resolveModel(localProject.getAppArtifact());
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to resolve Quarkus application model", e);
            }
            for (AppDependency appDep : appModel.getAllDependencies()) {
                addToClassPaths(classPathManifest, devModeContext, appDep.getArtifact().getPath().toFile());
            }

            args.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
            File wiringClassesDirectory = new File(buildDir, "wiring-devmode");
            wiringClassesDirectory.mkdirs();
            addToClassPaths(classPathManifest, devModeContext, wiringClassesDirectory);

            //we also want to add the maven plugin jar to the class path
            //this allows us to just directly use classes, without messing around copying them
            //to the runner jar
            URL classFile = DevModeMain.class.getClassLoader()
                    .getResource(DevModeMain.class.getName().replace('.', '/') + ".class");
            File path;
            if (classFile.getProtocol().equals("jar")) {
                String jarPath = classFile.getPath().substring(0, classFile.getPath().lastIndexOf('!'));
                if (jarPath.startsWith("file:"))
                    jarPath = jarPath.substring(5);
                // The resource will be URL encoded, so decode is so when addToClassPaths is called the encoding is correct
                path = new File(URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name()));
            } else if (classFile.getProtocol().equals("file")) {
                String filePath = classFile.getPath().substring(0,
                        classFile.getPath().lastIndexOf(DevModeMain.class.getName().replace('.', '/')));
                path = new File(URLDecoder.decode(filePath, StandardCharsets.UTF_8.name()));
            } else {
                throw new MojoFailureException("Unsupported DevModeMain artifact URL:" + classFile);
            }
            addToClassPaths(classPathManifest, devModeContext, path);

            //now we need to build a temporary jar to actually run

            File tempFile = new File(buildDir, project.getArtifactId() + "-dev.jar");
            tempFile.delete();
            // Only delete the -dev.jar on exit if requested
            if (deleteDevJar) {
                tempFile.deleteOnExit();
            }
            getLog().debug("Executable jar: " + tempFile.getAbsolutePath());

            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile))) {
                out.putNextEntry(new ZipEntry("META-INF/"));
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classPathManifest.toString());
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, DevModeMain.class.getName());
                out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                manifest.write(out);

                out.putNextEntry(new ZipEntry(DevModeMain.DEV_MODE_CONTEXT));
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream obj = new ObjectOutputStream(new DataOutputStream(bytes));
                obj.writeObject(devModeContext);
                obj.close();
                out.write(bytes.toByteArray());
            }

            outputDirectory.mkdirs();
            ApplicationInfoUtil.writeApplicationInfoProperties(appModel.getAppArtifact(), outputDirectory.toPath());

            args.add("-jar");
            args.add(tempFile.getAbsolutePath());
            args.add(outputDirectory.getAbsolutePath());
            args.add(wiringClassesDirectory.getAbsolutePath());
            args.add(new File(buildDir, "transformer-cache").getAbsolutePath());
            // Display the launch command line in debug mode
            getLog().debug("Launching JVM with command line: " + args.toString());
            ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[0]));
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.directory(outputDirectory);
            Process p = pb.start();

            //https://github.com/jbossas/quarkus/issues/232
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    p.destroy();
                }
            }, "Development Mode Shutdown Hook"));
            try {
                int ret = p.waitFor();
                if (ret != 0) {
                    throw new MojoFailureException("JVM exited with error code: " + String.valueOf(ret));
                }
            } catch (Exception e) {
                p.destroy();
                throw e;
            }

        } catch (Exception e) {
            throw new MojoFailureException("Failed to run", e);
        }
    }

    private static void addProperty(List<String> args, String name, Object value) {
        args.add("-D" + name + "=" + value);
    }

    /**
     * Search for the java command in the order:
     * 1. maven-toolchains plugin configuration
     * 2. java.home location
     * 3. java[.exe] on the system path
     *
     * @return the java command to use
     */
    protected String findJavaTool() {
        String java = null;

        // See if a toolchain is configured
        if (getToolchainManager() != null) {
            Toolchain toolchain = getToolchainManager().getToolchainFromBuildContext("jdk", getSession());
            if (toolchain != null) {
                java = toolchain.findTool("java");
                getLog().debug("JVM from toolchain: " + java);
            }
        }
        if (java == null) {
            // use the same JVM as the one used to run Maven (the "java.home" one)
            java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            File javaCheck = new File(java);
            getLog().debug("Checking: " + javaCheck.getAbsolutePath());
            if (!javaCheck.canExecute()) {
                getLog().debug(javaCheck.getAbsolutePath() + " is not executable");

                java = null;
                // Try executable extensions if windows
                if (OS.determineOS() == OS.WINDOWS && System.getenv().containsKey("PATHEXT")) {
                    String extpath = System.getenv("PATHEXT");
                    String[] exts = extpath.split(";");
                    for (String ext : exts) {
                        File winExe = new File(javaCheck.getAbsolutePath() + ext);
                        getLog().debug("Checking: " + winExe.getAbsolutePath());
                        if (winExe.canExecute()) {
                            java = winExe.getAbsolutePath();
                            getLog().debug("Executable: " + winExe.getAbsolutePath());
                            break;
                        }
                    }
                }
                // Fallback to java on the path
                if (java == null) {
                    if (OS.determineOS() == OS.WINDOWS) {
                        java = "java.exe";
                    } else {
                        java = "java";
                    }
                }
            }
        }
        getLog().debug("findJavaTool, selected JVM: " + java);
        return java;
    }

    private void addToClassPaths(StringBuilder classPathManifest, DevModeContext classPath, File file) {
        URI uri = file.toPath().toAbsolutePath().toUri();
        try {
            classPath.getClassPath().add(uri.toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String path = uri.getRawPath();
        classPathManifest.append(path);
        if (file.isDirectory()) {
            classPathManifest.append("/");
        }
        classPathManifest.append(" ");
    }

    /**
     * Enum to classify the os.name system property
     */
    static enum OS {
        WINDOWS,
        LINUX,
        MAC,
        OTHER;

        private String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        static OS determineOS() {
            OS os = OS.OTHER;
            String osName = System.getProperty("os.name");
            osName = osName.toLowerCase();
            if (osName.contains("windows")) {
                os = OS.WINDOWS;
            } else if (osName.contains("linux")
                    || osName.contains("freebsd")
                    || osName.contains("unix")
                    || osName.contains("sunos")
                    || osName.contains("solaris")
                    || osName.contains("aix")) {
                os = OS.LINUX;
            } else if (osName.contains("mac os")) {
                os = OS.MAC;
            } else {
                os = OS.OTHER;
            }

            os.setVersion(System.getProperty("os.version"));
            return os;
        }
    }
}
