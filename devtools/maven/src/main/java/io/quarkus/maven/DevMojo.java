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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

import io.quarkus.bootstrap.model.AppDependency;
import io.quarkus.bootstrap.model.AppModel;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.util.PropertyUtils;
import io.quarkus.dev.DevModeContext;
import io.quarkus.dev.DevModeMain;
import io.quarkus.maven.components.MavenVersionEnforcer;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.utilities.JavaBinFinder;

/**
 * The dev mojo, that runs a quarkus app in a forked process. A background compilation process is launched and any changes are
 * automatically reflected in your running application.
 * <p>
 * You can use this dev mode in a remote container environment with {@code remote-dev}.
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
     * If this server should be started in debug mode. The default is to start in debug mode and listen on
     * port 5005. Whether or not the JVM is suspended waiting for a debugger to be attached,
     * depends on the value of {@link #suspend}. {@code debug} supports the following options:
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
     * <td>The JVM is started in debug mode and will be listening on port 5005</td>
     * </tr>
     * <tr>
     * <td><b>client</b></td>
     * <td>The JVM is started in client mode, and attempts to connect to localhost:5005</td>
     * </tr>
     * <tr>
     * <td><b>{port}</b></td>
     * <td>The JVM is started in debug mode and will be listening on {port}</td>
     * </tr>
     * </table>
     */
    @Parameter(defaultValue = "${debug}")
    private String debug;

    /**
     * Whether or not the JVM launch, in debug mode, should be suspended. This parameter is only
     * relevant when the JVM is launched in {@link #debug debug mode}. This parameter supports the
     * following values (all the allowed values are case insensitive):
     * <table>
     * <th>
     * <td>Value</td>
     * <td>Effect</td>
     * </th>
     * <tr>
     * <td>y or true</td>
     * <td>The debug mode JVM launch is suspended</td>
     * </tr>
     * <tr>
     * <td>n or false</td>
     * <td>The debug mode JVM is started without suspending</td>
     * </tr>
     * </table>
     */
    @Parameter(defaultValue = "${suspend}")
    private String suspend;

    @Parameter(defaultValue = "${project.build.directory}")
    private File buildDir;

    @Parameter(defaultValue = "${project.build.sourceDirectory}")
    private File sourceDir;

    @Parameter(defaultValue = "${project.build.directory}")
    private File workingDir;

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

    @Component
    private Invoker invoker;

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

    /**
     * Whether changes in the projects that appear to be dependencies of the project containing the application to be launched
     * should trigger hot-reload. By default they do.
     */
    @Parameter(defaultValue = "${noDeps}")
    private boolean noDeps = false;

    /**
     * Additional parameters to pass to javac when recompiling changed
     * source files.
     */
    @Parameter
    private List<String> compilerArgs;

    /**
     * The -source argument to javac.
     */
    @Parameter(defaultValue = "${maven.compiler.source}")
    private String source;

    /**
     * The -target argument to javac.
     */
    @Parameter(defaultValue = "${maven.compiler.target}")
    private String target;

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
        boolean found = MojoUtils.checkProjectForMavenBuildPlugin(project);

        if (!found) {
            getLog().warn("The quarkus-maven-plugin build goal was not configured for this project, " +
                    "skipping quarkus:dev as this is assumed to be a support library. If you want to run quarkus dev" +
                    " on this project make sure the quarkus-maven-plugin is configured with a build goal.");
            return;
        }

        if (!sourceDir.isDirectory()) {
            getLog().warn("The project's sources directory does not exist " + sourceDir);
        }

        if (!buildDir.isDirectory() || !new File(buildDir, "classes").isDirectory()) {
            try {
                InvocationRequest request = new DefaultInvocationRequest();
                request.setBatchMode(true);
                request.setGoals(Collections.singletonList("compile"));

                invoker.execute(request);
            } catch (MavenInvocationException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        try {
            List<String> args = new ArrayList<>();
            String javaTool = JavaBinFinder.findBin();
            getLog().debug("Using javaTool: " + javaTool);
            args.add(javaTool);
            if (this.suspend != null) {
                switch (this.suspend.toLowerCase(Locale.ENGLISH)) {
                    case "n":
                    case "false": {
                        suspend = "n";
                        break;
                    }
                    case "y":
                    case "true": {
                        suspend = "y";
                        break;
                    }
                    default: {
                        getLog().warn(
                                "Ignoring invalid value \"" + suspend + "\" for \"suspend\" param and defaulting to \"n\"");
                        suspend = "n";
                        break;
                    }
                }
            } else {
                suspend = "n";
            }

            if (jvmArgs != null) {
                args.addAll(Arrays.asList(jvmArgs.split(" ")));
            }

            // the following flags reduce startup time and are acceptable only for dev purposes
            args.add("-XX:TieredStopAtLevel=1");
            if (!preventnoverify) {
                args.add("-Xverify:none");
            }

            DevModeRunner runner = new DevModeRunner(args);

            runner.prepare();
            runner.run();
            long nextCheck = System.currentTimeMillis() + 100;
            Map<Path, Long> pomFiles = readPomFileTimestamps(runner);
            for (;;) {
                //we never suspend after the first run
                suspend = "n";
                long sleep = Math.max(0, nextCheck - System.currentTimeMillis()) + 1;
                Thread.sleep(sleep);
                if (System.currentTimeMillis() > nextCheck) {
                    nextCheck = System.currentTimeMillis() + 100;
                    if (!runner.process.isAlive()) {
                        return;
                    }
                    boolean changed = false;
                    for (Map.Entry<Path, Long> e : pomFiles.entrySet()) {
                        long t = Files.getLastModifiedTime(e.getKey()).toMillis();
                        if (t > e.getValue()) {
                            changed = true;
                            pomFiles.put(e.getKey(), t);
                        }
                    }
                    if (changed) {
                        DevModeRunner newRunner = new DevModeRunner(args);
                        try {
                            newRunner.prepare();
                        } catch (Exception e) {
                            getLog().info("Could not load changed pom.xml file, changes not applied", e);
                            continue;
                        }
                        runner.stop();
                        newRunner.run();
                        runner = newRunner;
                    }
                }

            }

        } catch (Exception e) {
            throw new MojoFailureException("Failed to run", e);
        }
    }

    private Map<Path, Long> readPomFileTimestamps(DevModeRunner runner) throws IOException {
        Map<Path, Long> ret = new HashMap<>();
        for (Path i : runner.getPomFiles()) {
            ret.put(i, Files.getLastModifiedTime(i).toMillis());
        }
        return ret;
    }

    private String getSourceEncoding() {
        Object sourceEncodingProperty = project.getProperties().get("project.build.sourceEncoding");
        if (sourceEncodingProperty != null) {
            return (String) sourceEncodingProperty;
        }
        return null;
    }

    private void addProject(DevModeContext devModeContext, LocalProject localProject) {

        String projectDirectory = null;
        Set<String> sourcePaths = null;
        String classesPath = null;
        String resourcePath = null;

        final MavenProject mavenProject = session.getProjectMap().get(
                String.format("%s:%s:%s", localProject.getGroupId(), localProject.getArtifactId(), localProject.getVersion()));

        if (mavenProject == null) {
            projectDirectory = localProject.getDir().toAbsolutePath().toString();
            Path sourcePath = localProject.getSourcesSourcesDir().toAbsolutePath();
            if (Files.isDirectory(sourcePath)) {
                sourcePaths = Collections.singleton(
                        sourcePath.toString());
            } else {
                sourcePaths = Collections.emptySet();
            }
        } else {
            projectDirectory = mavenProject.getBasedir().getPath();
            sourcePaths = mavenProject.getCompileSourceRoots().stream()
                    .map(Paths::get)
                    .filter(Files::isDirectory)
                    .map(src -> src.toAbsolutePath().toString())
                    .collect(Collectors.toSet());
        }

        Path classesDir = localProject.getClassesDir();
        if (Files.isDirectory(classesDir)) {
            classesPath = classesDir.toAbsolutePath().toString();
        }
        Path resourcesSourcesDir = localProject.getResourcesSourcesDir();
        if (Files.isDirectory(resourcesSourcesDir)) {
            resourcePath = resourcesSourcesDir.toAbsolutePath().toString();
        }
        DevModeContext.ModuleInfo moduleInfo = new DevModeContext.ModuleInfo(
                localProject.getArtifactId(),
                projectDirectory,
                sourcePaths,
                classesPath,
                resourcePath);
        devModeContext.getModules().add(moduleInfo);
    }

    private void addToClassPaths(StringBuilder classPathManifest, DevModeContext classPath, File file) {
        URI uri = file.toPath().toAbsolutePath().toUri();
        try {
            classPath.getClassPath().add(uri.toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        String path = uri.getRawPath();
        if (PropertyUtils.isWindows()) {
            if (path.length() > 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
                path = "/" + path;
            }
        }
        classPathManifest.append(path);
        if (file.isDirectory() && path.charAt(path.length() - 1) != '/') {
            classPathManifest.append("/");
        }
        classPathManifest.append(" ");
    }

    class DevModeRunner {

        private final List<String> args;
        private Process process;
        private Set<Path> pomFiles = new HashSet<>();

        DevModeRunner(List<String> args) {
            this.args = new ArrayList<>(args);
        }

        /**
         * Attempts to prepare the dev mode runner.
         */
        void prepare() throws Exception {
            if (debug == null) {
                boolean useDebugMode = true;
                // debug mode not specified
                // make sure 5005 is not used, we don't want to just fail if something else is using it
                try (Socket socket = new Socket(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), 5005)) {
                    getLog().error("Port 5005 in use, not starting in debug mode");
                    useDebugMode = false;
                } catch (IOException e) {
                }
                if (useDebugMode) {
                    args.add("-Xdebug");
                    args.add("-Xrunjdwp:transport=dt_socket,address=5005,server=y,suspend=" + suspend);
                }
            } else if (debug.toLowerCase().equals("client")) {
                args.add("-Xdebug");
                args.add("-Xrunjdwp:transport=dt_socket,address=localhost:5005,server=n,suspend=" + suspend);
            } else if (debug.toLowerCase().equals("true")) {
                args.add("-Xdebug");
                args.add("-Xrunjdwp:transport=dt_socket,address=localhost:5005,server=y,suspend=" + suspend);
            } else if (!debug.toLowerCase().equals("false")) {
                try {
                    int port = Integer.parseInt(debug);
                    if (port <= 0) {
                        throw new MojoFailureException("The specified debug port must be greater than 0");
                    }
                    args.add("-Xdebug");
                    args.add("-Xrunjdwp:transport=dt_socket,address=" + port + ",server=y,suspend=" + suspend);
                } catch (NumberFormatException e) {
                    throw new MojoFailureException(
                            "Invalid value for debug parameter: " + debug + " must be true|false|client|{port}");
                }
            }
            //build a class-path string for the base platform
            //this stuff does not change
            // Do not include URIs in the manifest, because some JVMs do not like that
            StringBuilder classPathManifest = new StringBuilder();
            final DevModeContext devModeContext = new DevModeContext();
            for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
                devModeContext.getSystemProperties().put(e.getKey().toString(), (String) e.getValue());
            }
            devModeContext.getBuildSystemProperties().putAll((Map) project.getProperties());
            devModeContext.setSourceEncoding(getSourceEncoding());
            devModeContext.setSourceJavaVersion(source);
            devModeContext.setTargetJvmVersion(target);

            // Set compilation flags.  Try the explicitly given configuration first.  Otherwise,
            // refer to the configuration of the Maven Compiler Plugin.
            if (compilerArgs != null) {
                devModeContext.setCompilerOptions(compilerArgs);
            } else {
                for (Plugin plugin : project.getBuildPlugins()) {
                    if (!plugin.getKey().equals("org.apache.maven.plugins:maven-compiler-plugin")) {
                        continue;
                    }
                    Xpp3Dom compilerPluginConfiguration = (Xpp3Dom) plugin.getConfiguration();
                    if (compilerPluginConfiguration == null) {
                        continue;
                    }
                    Xpp3Dom compilerPluginArgsConfiguration = compilerPluginConfiguration.getChild("compilerArgs");
                    if (compilerPluginArgsConfiguration == null) {
                        continue;
                    }
                    List<String> compilerPluginArgs = new ArrayList<>();
                    for (Xpp3Dom argConfiguration : compilerPluginArgsConfiguration.getChildren()) {
                        compilerPluginArgs.add(argConfiguration.getValue());
                    }
                    devModeContext.setCompilerOptions(compilerPluginArgs);
                    break;
                }
            }

            final AppModel appModel;
            try {
                final LocalProject localProject;
                if (noDeps) {
                    localProject = LocalProject.load(outputDirectory.toPath());
                    addProject(devModeContext, localProject);
                } else {
                    localProject = LocalProject.loadWorkspace(outputDirectory.toPath());
                    for (LocalProject project : localProject.getSelfWithLocalDeps()) {
                        if (project.getClassesDir() != null) {
                            //if this project also contains Quarkus extensions we do no want to include these in the discovery
                            //a bit of an edge case, but if you try and include a sample project with your extension you will
                            //run into problems without this
                            if (Files.exists(project.getClassesDir().resolve("META-INF/quarkus-extension.properties")) ||
                                    Files.exists(project.getClassesDir().resolve("META-INF/quarkus-build-steps.list"))) {
                                continue;
                            }
                        }
                        addProject(devModeContext, project);
                    }
                }
                for (LocalProject i : localProject.getSelfWithLocalDeps()) {
                    pomFiles.add(i.getDir().resolve("pom.xml"));
                }

                /*
                 * TODO: support multiple resources dirs for config hot deployment
                 * String resources = null;
                 * for (Resource i : project.getBuild().getResources()) {
                 * resources = i.getDirectory();
                 * break;
                 * }
                 */

                appModel = new BootstrapAppModelResolver(MavenArtifactResolver.builder()
                        .setRepositorySystem(repoSystem)
                        .setRepositorySystemSession(repoSession)
                        .setRemoteRepositories(repos)
                        .setWorkspace(localProject.getWorkspace())
                        .build())
                                .setDevMode(true)
                                .resolveModel(localProject.getAppArtifact());
                if (appModel.getAllDependencies().isEmpty()) {
                    throw new RuntimeException("Unable to resolve application dependencies");
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to resolve Quarkus application model", e);
            }
            for (AppDependency appDep : appModel.getAllDependencies()) {
                addToClassPaths(classPathManifest, devModeContext, appDep.getArtifact().getPath().toFile());
            }

            args.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
            //wiring devmode is used for CDI beans that are not part of the user application (i.e. beans in 3rd party jars)
            //we need this because these beans cannot be loaded by the runtime class loader, they must be loaded by the platform
            //class loader
            File wiringClassesDirectory = new File(buildDir, "wiring-devmode");
            wiringClassesDirectory.mkdirs();

            addToClassPaths(classPathManifest, devModeContext, wiringClassesDirectory);

            //we also want to add the maven plugin jar to the class path
            //this allows us to just directly use classes, without messing around copying them
            //to the runner jar
            URL classFile = DevModeMain.class.getClassLoader()
                    .getResource(DevModeMain.class.getName().replace('.', '/') + ".class");
            File path;
            if (classFile == null) {
                throw new MojoFailureException("No DevModeMain class found");
            }
            URI classUri = classFile.toURI();
            if (classUri.getScheme().equals("jar")) {
                String jarPath = classUri.getRawSchemeSpecificPart();
                final int marker = jarPath.indexOf('!');
                if (marker != -1) {
                    jarPath = jarPath.substring(0, marker);
                }
                URI jarUri = new URI(jarPath);
                path = Paths.get(jarUri).toFile();
            } else if (classUri.getScheme().equals("file")) {
                path = Paths.get(classUri).toFile();
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

            devModeContext.getClassesRoots().add(outputDirectory.getAbsoluteFile());
            devModeContext.setFrameworkClassesDir(wiringClassesDirectory.getAbsoluteFile());
            devModeContext.setCacheDir(new File(buildDir, "transformer-cache").getAbsoluteFile());

            // this is the jar file we will use to launch the dev mode main class
            devModeContext.setDevModeRunnerJarFile(tempFile);
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

            args.add("-jar");
            args.add(tempFile.getAbsolutePath());

        }

        public Set<Path> getPomFiles() {
            return pomFiles;
        }

        public void run() throws Exception {
            // Display the launch command line in debug mode
            getLog().debug("Launching JVM with command line: " + args.toString());
            ProcessBuilder pb = new ProcessBuilder(args.toArray(new String[0]));
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            pb.directory(workingDir);
            process = pb.start();

            //https://github.com/quarkusio/quarkus/issues/232
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    process.destroy();
                }
            }, "Development Mode Shutdown Hook"));
        }

        public void stop() throws InterruptedException {
            process.destroy();
            process.waitFor();
        }

    }
}
