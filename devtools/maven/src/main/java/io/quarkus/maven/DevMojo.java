package io.quarkus.maven;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.cli.CommandLineUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.deployment.dev.DevModeContext;
import io.quarkus.deployment.dev.DevModeMain;
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
     * running any one of these phases means the compile phase will have been run, if these have
     * not been run we manually run compile
     */
    private static final Set<String> POST_COMPILE_PHASES = new HashSet<>(Arrays.asList(
            "compile",
            "process-classes",
            "generate-test-sources",
            "process-test-sources",
            "generate-test-resources",
            "process-test-resources",
            "test-compile",
            "process-test-classes",
            "test",
            "prepare-package",
            "package",
            "pre-integration-test",
            "integration-test",
            "post-integration-test",
            "verify",
            "install",
            "deploy"));

    private static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
    private static final String MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin";
    private static final String MAVEN_RESOURCES_PLUGIN = "maven-resources-plugin";

    private static final String ORG_JETBRAINS_KOTLIN = "org.jetbrains.kotlin";
    private static final String KOTLIN_MAVEN_PLUGIN = "kotlin-maven-plugin";

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

    @Parameter(defaultValue = "${quarkus.args}")
    private String argsString;

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

    /**
     * Whether or not to enforce the quarkus-maven-plugin build goal to be configured.
     * By default, a missing build goal is considered an inconsistency (although the build goal is not <i>required</i>
     * technically).
     * In this case a warning will be logged and the application will not be started.
     */
    @Parameter(defaultValue = "${quarkus.enforceBuildGoal}")
    private boolean enforceBuildGoal = true;

    @Component
    private WorkspaceReader wsReader;

    @Component
    private BuildPluginManager pluginManager;

    private Boolean debugPortOk;

    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {

        mavenVersionEnforcer.ensureMavenVersion(getLog(), session);

        //we always want to compile if needed, so if it is run from the parent it will compile dependent projects
        handleAutoCompile();

        if (enforceBuildGoal) {
            Plugin pluginDef = MojoUtils.checkProjectForMavenBuildPlugin(project);

            if (pluginDef == null) {
                getLog().warn("The quarkus-maven-plugin build goal was not configured for this project, " +
                        "skipping quarkus:dev as this is assumed to be a support library. If you want to run quarkus dev" +
                        " on this project make sure the quarkus-maven-plugin is configured with a build goal.");
                return;
            }
        }

        // don't warn if running in a module of a multi-module project
        if (!sourceDir.isDirectory() && (project.getParentFile() == null || !project.getParentFile().exists())) {
            getLog().warn("The project's sources directory does not exist " + sourceDir);
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

            runner.prepare(false);
            Map<Path, Long> pomFiles = readPomFileTimestamps(runner);
            runner.run();
            long nextCheck = System.currentTimeMillis() + 100;
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
                    final Set<Path> changed = new HashSet<>();
                    for (Map.Entry<Path, Long> e : pomFiles.entrySet()) {
                        long t = Files.getLastModifiedTime(e.getKey()).toMillis();
                        if (t > e.getValue()) {
                            changed.add(e.getKey());
                            pomFiles.put(e.getKey(), t);
                        }
                    }
                    if (!changed.isEmpty()) {
                        getLog().info("Changes detected to " + changed + ", restarting dev mode");
                        DevModeRunner newRunner = new DevModeRunner(args);
                        try {
                            newRunner.prepare(true);
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

    private void handleAutoCompile() throws MojoExecutionException {
        //we check to see if there was a compile (or later) goal before this plugin
        boolean compileNeeded = true;
        for (String goal : session.getGoals()) {
            if (POST_COMPILE_PHASES.contains(goal)) {
                compileNeeded = false;
                break;
            }
            if (goal.endsWith("quarkus:dev")) {
                break;
            }
        }

        //if the user did not compile we run it for them
        if (compileNeeded) {
            triggerCompile();
        }
    }

    private void triggerCompile() throws MojoExecutionException {
        handleResources();

        // compile the Kotlin sources if needed
        final String kotlinMavenPluginKey = ORG_JETBRAINS_KOTLIN + ":" + KOTLIN_MAVEN_PLUGIN;
        final Plugin kotlinMavenPlugin = project.getPlugin(kotlinMavenPluginKey);
        if (kotlinMavenPlugin != null) {
            executeCompileGoal(kotlinMavenPlugin, ORG_JETBRAINS_KOTLIN, KOTLIN_MAVEN_PLUGIN);
        }

        // Compile the Java sources if needed
        final String compilerPluginKey = ORG_APACHE_MAVEN_PLUGINS + ":" + MAVEN_COMPILER_PLUGIN;
        final Plugin compilerPlugin = project.getPlugin(compilerPluginKey);
        if (compilerPlugin != null) {
            executeCompileGoal(compilerPlugin, ORG_APACHE_MAVEN_PLUGINS, MAVEN_COMPILER_PLUGIN);
        }
    }

    /**
     * Execute the resources:resources goal if resources have been configured on the project
     */
    private void handleResources() throws MojoExecutionException {
        List<Resource> resources = project.getResources();
        if (!resources.isEmpty()) {
            Plugin resourcesPlugin = project.getPlugin(ORG_APACHE_MAVEN_PLUGINS + ":" + MAVEN_RESOURCES_PLUGIN);
            MojoExecutor.executeMojo(
                    MojoExecutor.plugin(
                            MojoExecutor.groupId(ORG_APACHE_MAVEN_PLUGINS),
                            MojoExecutor.artifactId(MAVEN_RESOURCES_PLUGIN),
                            MojoExecutor.version(resourcesPlugin.getVersion()),
                            resourcesPlugin.getDependencies()),
                    MojoExecutor.goal("resources"),
                    MojoExecutor.configuration(),
                    MojoExecutor.executionEnvironment(
                            project,
                            session,
                            pluginManager));
        }
    }

    private void executeCompileGoal(Plugin plugin, String groupId, String artifactId) throws MojoExecutionException {
        Xpp3Dom configuration = MojoExecutor.configuration();
        Xpp3Dom pluginConfiguration = (Xpp3Dom) plugin.getConfiguration();
        if (pluginConfiguration != null) {
            //Filter out `test*` configurations
            for (Xpp3Dom child : pluginConfiguration.getChildren()) {
                if (!child.getName().startsWith("test")) {
                    configuration.addChild(child);
                }
            }
        }
        MojoExecutor.executeMojo(
                MojoExecutor.plugin(
                        MojoExecutor.groupId(groupId),
                        MojoExecutor.artifactId(artifactId),
                        MojoExecutor.version(plugin.getVersion()),
                        plugin.getDependencies()),
                MojoExecutor.goal("compile"),
                configuration,
                MojoExecutor.executionEnvironment(
                        project,
                        session,
                        pluginManager));
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

    private void addProject(DevModeContext devModeContext, LocalProject localProject, boolean root) {

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
        DevModeContext.ModuleInfo moduleInfo = new DevModeContext.ModuleInfo(localProject.getKey(),
                localProject.getArtifactId(),
                projectDirectory,
                sourcePaths,
                classesPath,
                resourcePath);
        if (root) {
            devModeContext.setApplicationRoot(moduleInfo);
        } else {
            devModeContext.getAdditionalModules().add(moduleInfo);
        }
    }

    private void addToClassPaths(StringBuilder classPathManifest, File file) {
        final URI uri = file.toPath().toAbsolutePath().toUri();
        classPathManifest.append(uri).append(" ");
    }

    class DevModeRunner {

        private static final String KOTLIN_MAVEN_PLUGIN_GA = "org.jetbrains.kotlin:kotlin-maven-plugin";
        private final List<String> args;
        private Process process;
        private Set<Path> pomFiles = new HashSet<>();

        DevModeRunner(List<String> args) {
            this.args = new ArrayList<>(args);
        }

        /**
         * Attempts to prepare the dev mode runner.
         */
        void prepare(final boolean triggerCompile) throws Exception {
            if (triggerCompile) {
                triggerCompile();
            }
            if (debug == null) {
                // debug mode not specified
                // make sure 5005 is not used, we don't want to just fail if something else is using it
                // we don't check this on restarts, as the previous process is still running
                if (debugPortOk == null) {
                    try (Socket socket = new Socket(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), 5005)) {
                        getLog().error("Port 5005 in use, not starting in debug mode");
                        debugPortOk = false;
                    } catch (IOException e) {
                        debugPortOk = true;
                    }
                }
                if (debugPortOk) {
                    args.add("-Xdebug");
                    args.add("-Xrunjdwp:transport=dt_socket,address=0.0.0.0:5005,server=y,suspend=" + suspend);
                }
            } else if (debug.toLowerCase().equals("client")) {
                args.add("-Xdebug");
                args.add("-Xrunjdwp:transport=dt_socket,address=localhost:5005,server=n,suspend=" + suspend);
            } else if (debug.toLowerCase().equals("true")) {
                args.add("-Xdebug");
                args.add("-Xrunjdwp:transport=dt_socket,address=0.0.0.0:5005,server=y,suspend=" + suspend);
            } else if (!debug.toLowerCase().equals("false")) {
                try {
                    int port = Integer.parseInt(debug);
                    if (port <= 0) {
                        throw new MojoFailureException("The specified debug port must be greater than 0");
                    }
                    args.add("-Xdebug");
                    args.add("-Xrunjdwp:transport=dt_socket,address=0.0.0.0:" + port + ",server=y,suspend=" + suspend);
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
            devModeContext.setProjectDir(project.getFile().getParentFile());
            devModeContext.getBuildSystemProperties().putAll((Map) project.getProperties());

            //  this is a minor hack to allow ApplicationConfig to be populated with defaults
            devModeContext.getBuildSystemProperties().putIfAbsent("quarkus.application.name", project.getArtifactId());
            devModeContext.getBuildSystemProperties().putIfAbsent("quarkus.application.version", project.getVersion());

            devModeContext.setSourceEncoding(getSourceEncoding());

            // Set compilation flags.  Try the explicitly given configuration first.  Otherwise,
            // refer to the configuration of the Maven Compiler Plugin.
            final Optional<Xpp3Dom> compilerPluginConfiguration = findCompilerPluginConfiguration();
            if (compilerArgs != null) {
                devModeContext.setCompilerOptions(compilerArgs);
            } else if (compilerPluginConfiguration.isPresent()) {
                final Xpp3Dom compilerPluginArgsConfiguration = compilerPluginConfiguration.get().getChild("compilerArgs");
                if (compilerPluginArgsConfiguration != null) {
                    List<String> compilerPluginArgs = new ArrayList<>();
                    for (Xpp3Dom argConfiguration : compilerPluginArgsConfiguration.getChildren()) {
                        compilerPluginArgs.add(argConfiguration.getValue());
                    }
                    // compilerArgs can also take a value without using arg
                    if (compilerPluginArgsConfiguration.getValue() != null
                            && !compilerPluginArgsConfiguration.getValue().isEmpty()) {
                        compilerPluginArgs.add(compilerPluginArgsConfiguration.getValue().trim());
                    }
                    devModeContext.setCompilerOptions(compilerPluginArgs);
                }
            }
            if (source != null) {
                devModeContext.setSourceJavaVersion(source);
            } else if (compilerPluginConfiguration.isPresent()) {
                final Xpp3Dom javacSourceVersion = compilerPluginConfiguration.get().getChild("source");
                if (javacSourceVersion != null && javacSourceVersion.getValue() != null
                        && !javacSourceVersion.getValue().trim().isEmpty()) {
                    devModeContext.setSourceJavaVersion(javacSourceVersion.getValue().trim());
                }
            }
            if (target != null) {
                devModeContext.setTargetJvmVersion(target);
            } else if (compilerPluginConfiguration.isPresent()) {
                final Xpp3Dom javacTargetVersion = compilerPluginConfiguration.get().getChild("target");
                if (javacTargetVersion != null && javacTargetVersion.getValue() != null
                        && !javacTargetVersion.getValue().trim().isEmpty()) {
                    devModeContext.setTargetJvmVersion(javacTargetVersion.getValue().trim());
                }
            }

            setKotlinSpecificFlags(devModeContext);
            final LocalProject localProject;
            if (noDeps) {
                localProject = LocalProject.load(outputDirectory.toPath());
                addProject(devModeContext, localProject, true);
                pomFiles.add(localProject.getDir().resolve("pom.xml"));
                devModeContext.getLocalArtifacts()
                        .add(new AppArtifactKey(localProject.getGroupId(), localProject.getArtifactId(), null, "jar"));
            } else {
                localProject = LocalProject.loadWorkspace(outputDirectory.toPath());
                for (LocalProject project : filterExtensionDependencies(localProject)) {
                    addProject(devModeContext, project, project == localProject);
                    pomFiles.add(project.getDir().resolve("pom.xml"));
                    devModeContext.getLocalArtifacts()
                            .add(new AppArtifactKey(project.getGroupId(), project.getArtifactId(), null, "jar"));
                }
            }

            addQuarkusDevModeDeps(classPathManifest);

            args.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");

            //in most cases these are not used, however they need to be present for some
            //parent-first cases such as logging
            for (Artifact appDep : project.getArtifacts()) {
                addToClassPaths(classPathManifest, appDep.getFile());
            }

            //now we need to build a temporary jar to actually run

            File tempFile = new File(buildDir, project.getArtifactId() + "-dev.jar");
            tempFile.delete();
            // Only delete the -dev.jar on exit if requested
            if (deleteDevJar) {
                tempFile.deleteOnExit();
            }
            getLog().debug("Executable jar: " + tempFile.getAbsolutePath());

            devModeContext.setBaseName(project.getBuild().getFinalName());
            devModeContext.setCacheDir(new File(buildDir, "transformer-cache").getAbsoluteFile());

            // this is the jar file we will use to launch the dev mode main class
            devModeContext.setDevModeRunnerJarFile(tempFile);

            modifyDevModeContext(devModeContext);

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
            // if the --enable-preview flag was set, then we need to enable it when launching dev mode as well
            if (devModeContext.isEnablePreview()) {
                args.add(DevModeContext.ENABLE_PREVIEW_FLAG);
            }

            propagateUserProperties();

            args.add("-jar");
            args.add(tempFile.getAbsolutePath());
            if (argsString != null) {
                args.addAll(Arrays.asList(CommandLineUtils.translateCommandline(argsString)));
            }
        }

        private void propagateUserProperties() {
            final String mavenCmdLine = BootstrapMavenOptions.getMavenCmdLine();
            if (mavenCmdLine == null || mavenCmdLine.isEmpty()) {
                return;
            }
            int i = mavenCmdLine.indexOf("-D");
            if (i < 0) {
                return;
            }
            final StringBuilder buf = new StringBuilder();
            buf.append("-D");
            i += 2;
            while (i < mavenCmdLine.length()) {
                final char ch = mavenCmdLine.charAt(i++);
                if (!Character.isWhitespace(ch)) {
                    buf.append(ch);
                } else if (buf.length() > 2) {
                    args.add(buf.toString());
                    buf.setLength(2);
                    i = mavenCmdLine.indexOf("-D", i);
                    if (i < 0) {
                        break;
                    }
                    i += 2;
                }
            }
            if (buf.length() > 2) {
                args.add(buf.toString());
            }
        }

        private void addQuarkusDevModeDeps(StringBuilder classPathManifest)
                throws MojoExecutionException, DependencyResolutionException {
            final String pomPropsPath = "META-INF/maven/io.quarkus/quarkus-core-deployment/pom.properties";
            final InputStream devModePomPropsIs = DevModeMain.class.getClassLoader().getResourceAsStream(pomPropsPath);
            if (devModePomPropsIs == null) {
                throw new MojoExecutionException("Failed to locate " + pomPropsPath + " on the classpath");
            }
            final Properties devModeProps = new Properties();
            try (InputStream is = devModePomPropsIs) {
                devModeProps.load(is);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to load " + pomPropsPath + " from the classpath", e);
            }
            final String devModeGroupId = devModeProps.getProperty("groupId");
            if (devModeGroupId == null) {
                throw new MojoExecutionException("Classpath resource " + pomPropsPath + " is missing groupId");
            }
            final String devModeArtifactId = devModeProps.getProperty("artifactId");
            if (devModeArtifactId == null) {
                throw new MojoExecutionException("Classpath resource " + pomPropsPath + " is missing artifactId");
            }
            final String devModeVersion = devModeProps.getProperty("version");
            if (devModeVersion == null) {
                throw new MojoExecutionException("Classpath resource " + pomPropsPath + " is missing version");
            }

            final DefaultArtifact devModeJar = new DefaultArtifact(devModeGroupId, devModeArtifactId, "jar", devModeVersion);
            final DependencyResult cpRes = repoSystem.resolveDependencies(repoSession,
                    new DependencyRequest()
                            .setCollectRequest(
                                    new CollectRequest()
                                            .setRoot(new org.eclipse.aether.graph.Dependency(devModeJar, JavaScopes.RUNTIME))
                                            .setRepositories(repos)));

            for (ArtifactResult appDep : cpRes.getArtifactResults()) {
                //we only use the launcher for launching from the IDE, we need to exclude it
                if (!(appDep.getArtifact().getGroupId().equals("io.quarkus")
                        && appDep.getArtifact().getArtifactId().equals("quarkus-ide-launcher"))) {
                    addToClassPaths(classPathManifest, appDep.getArtifact().getFile());
                }
            }
        }

        private void setKotlinSpecificFlags(DevModeContext devModeContext) {
            Plugin kotlinMavenPlugin = null;
            for (Plugin plugin : project.getBuildPlugins()) {
                if (plugin.getKey().equals(KOTLIN_MAVEN_PLUGIN_GA)) {
                    kotlinMavenPlugin = plugin;
                    break;
                }
            }

            if (kotlinMavenPlugin == null) {
                return;
            }

            getLog().debug("Kotlin Maven plugin detected");

            List<String> compilerPluginArtifacts = new ArrayList<>();
            List<Dependency> dependencies = kotlinMavenPlugin.getDependencies();
            for (Dependency dependency : dependencies) {
                try {
                    ArtifactResult resolvedArtifact = repoSystem.resolveArtifact(repoSession,
                            new ArtifactRequest()
                                    .setArtifact(new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
                                            dependency.getClassifier(), dependency.getType(), dependency.getVersion()))
                                    .setRepositories(repos));

                    compilerPluginArtifacts.add(resolvedArtifact.getArtifact().getFile().toPath().toAbsolutePath().toString());
                } catch (ArtifactResolutionException e) {
                    getLog().warn("Unable to properly setup dev-mode for Kotlin", e);
                    return;
                }
            }
            devModeContext.setCompilerPluginArtifacts(compilerPluginArtifacts);

            List<String> options = new ArrayList<>();
            Xpp3Dom compilerPluginConfiguration = (Xpp3Dom) kotlinMavenPlugin.getConfiguration();
            if (compilerPluginConfiguration != null) {
                Xpp3Dom compilerPluginArgsConfiguration = compilerPluginConfiguration.getChild("pluginOptions");
                if (compilerPluginArgsConfiguration != null) {
                    for (Xpp3Dom argConfiguration : compilerPluginArgsConfiguration.getChildren()) {
                        options.add(argConfiguration.getValue());
                    }
                }
            }
            devModeContext.setCompilerPluginsOptions(options);
        }

        public Set<Path> getPomFiles() {
            return pomFiles;
        }

        public void run() throws Exception {
            // Display the launch command line in dev mode
            if (getLog().isDebugEnabled()) {
                getLog().debug("Launching JVM with command line: " + String.join(" ", args));
            }
            process = new ProcessBuilder(args)
                    .inheritIO()
                    .directory(workingDir)
                    .start();
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

    protected void modifyDevModeContext(DevModeContext devModeContext) {

    }

    private List<LocalProject> filterExtensionDependencies(LocalProject localProject) {
        List<LocalProject> ret = new ArrayList<>();
        Queue<LocalProject> toRemove = new ArrayDeque<>();
        Set<AppArtifactKey> extensionsAndDeps = new HashSet<>();
        Set<AppArtifactKey> inProject = new HashSet<>();

        for (LocalProject project : localProject.getSelfWithLocalDeps()) {
            inProject.add(project.getKey());
            final Artifact projectDep = this.project.getArtifactMap().get(project.getGroupId() + ':' + project.getArtifactId());
            if (project.getClassesDir() != null
                    && (
                    // if it is not found among project.getArtifacts() it shouldn't be included (e.g. a local test dependency)
                    (localProject != project && projectDep == null)
                            //if this project also contains Quarkus extensions we do no want to include these in the discovery
                            //a bit of an edge case, but if you try and include a sample project with your extension you will
                            //run into problems without this
                            || (Files.exists(project.getClassesDir().resolve("META-INF/quarkus-extension.properties")) ||
                                    Files.exists(project.getClassesDir().resolve("META-INF/quarkus-build-steps.list"))))) {
                toRemove.add(project);
                extensionsAndDeps.add(project.getKey());
            } else {
                ret.add(project);
            }
        }
        if (toRemove.isEmpty()) {
            return ret;
        }
        //we also remove transitive deps of the extensions
        //this is common in projects that provide a library, and a quarkus extension for that library
        //all in the same project
        while (!toRemove.isEmpty()) {
            LocalProject dep = toRemove.poll();
            //we don't need to resolve deps properly, this is all in the same project
            //so we have all the info we need locally
            for (Dependency i : dep.getRawModel().getDependencies()) {
                AppArtifactKey key = new AppArtifactKey(i.getGroupId(), i.getArtifactId());
                if (inProject.contains(key) && !extensionsAndDeps.contains(key)) {
                    extensionsAndDeps.add(key);
                    toRemove.add(localProject.getWorkspace().getProject(key));
                }
            }
        }
        Iterator<LocalProject> iterator = ret.iterator();
        while (iterator.hasNext()) {
            LocalProject obj = iterator.next();
            if (extensionsAndDeps.contains(obj.getKey())) {
                getLog().warn("Local Quarkus extension dependency " + obj.getKey() + " will not be hot-reloadable");
                iterator.remove();
            }
        }
        return ret;
    }

    private Optional<Xpp3Dom> findCompilerPluginConfiguration() {
        for (final Plugin plugin : project.getBuildPlugins()) {
            if (!plugin.getKey().equals("org.apache.maven.plugins:maven-compiler-plugin")) {
                continue;
            }
            final Xpp3Dom compilerPluginConfiguration = (Xpp3Dom) plugin.getConfiguration();
            if (compilerPluginConfiguration != null) {
                return Optional.of(compilerPluginConfiguration);
            }
        }
        return Optional.empty();
    }
}
