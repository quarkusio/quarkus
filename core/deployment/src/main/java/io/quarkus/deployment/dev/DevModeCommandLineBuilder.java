package io.quarkus.deployment.dev;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ExtensionDevModeConfig;
import io.quarkus.bootstrap.model.JvmOptions;
import io.quarkus.bootstrap.model.JvmOptionsBuilder;
import io.quarkus.deployment.util.CommandLineUtil;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.runtime.logging.JBossVersion;
import io.quarkus.utilities.JavaBinFinder;

public class DevModeCommandLineBuilder {

    private static final Logger log = Logger.getLogger(DevModeCommandLineBuilder.class);

    /**
     * Logs a warning about extensions enabling the C2 compiler
     *
     * @param extensions extensions enabling the C2 compiler
     */
    private static void extensionsEnableC2Warning(List<ArtifactKey> extensions) {
        var sb = new StringBuilder().append("Extension");
        if (extensions.size() > 1) {
            sb.append("s");
        }
        sb.append(" ").append(extensions.get(0).toGacString());
        for (int i = 1; i < extensions.size(); ++i) {
            sb.append(", ").append(extensions.get(i).toGacString());
        }
        sb.append(" enable");
        if (extensions.size() == 1) {
            sb.append("s");
        }
        sb.append(" the C2 compiler which is disabled by default in Dev mode for optimal performance.");
        log.info(sb.toString());
    }

    /**
     * Logs a warning about extensions disabling debug mode
     *
     * @param extensions extensions disabling debug mode
     */
    private static void extensionsDisablingDebugWarning(List<ArtifactKey> extensions) {
        var sb = new StringBuilder().append("Extension");
        if (extensions.size() > 1) {
            sb.append("s");
        }
        sb.append(" ").append(extensions.get(0).toGacString());
        for (int i = 1; i < extensions.size(); ++i) {
            sb.append(", ").append(extensions.get(i).toGacString());
        }
        sb.append(" disable");
        if (extensions.size() == 1) {
            sb.append("s");
        }
        sb.append(
                " the Debug mode for optimal performance. Debugging can still be enabled in the Quarkus plugin configuration or with -Ddebug on the command line.");
        log.info(sb.toString());
    }

    private static final String TIERED_STOP_AT_LEVEL = "TieredStopAtLevel";
    private static final String AGENTLIB_JDWP = "agentlib:jdwp";

    final Pattern validDebug = Pattern.compile("^(true|false|client|[0-9]+)$");
    final Pattern validPort = Pattern.compile("^-?[0-9]+$");

    private List<String> args = new ArrayList<>();
    private JvmOptionsBuilder jvmOptionsBuilder = JvmOptions.builder();
    private String debug;
    private String suspend;
    private String debugHost = "localhost";
    private String debugPort = "5005";
    private String actualDebugPort;
    private File projectDir;
    private File buildDir;
    private File outputDir;
    private Map<String, String> buildSystemProperties = new HashMap<>(0);
    private String applicationName;
    private String applicationVersion;
    private String sourceEncoding;
    private Map<String, Set<String>> compilerOptions = new HashMap<>(1);
    private List<String> compilerPluginArtifacts;
    private List<String> compilerPluginOptions;
    private String releaseJavaVersion;
    private String sourceJavaVersion;
    private String targetJavaVersion;
    private Set<Path> buildFiles = new HashSet<>(0);
    private boolean deleteDevJar = true;
    private Boolean forceC2;
    private String baseName;
    private Consumer<DevModeContext> entryPointCustomizer;
    private String applicationArgs;
    private Set<ArtifactKey> localArtifacts = new HashSet<>();
    private DevModeContext.ModuleInfo main;
    private List<DevModeContext.ModuleInfo> dependencies = new ArrayList<>(0);
    private LinkedHashMap<ArtifactKey, File> classpath = new LinkedHashMap<>();
    private Set<File> processorPaths;
    private List<String> processors;
    private Collection<ExtensionDevModeConfig> extDevModeConfig;
    private ExtensionDevModeJvmOptionFilter extDevModeJvmOptionFilter;

    protected DevModeCommandLineBuilder(String java) {
        final String javaTool = java == null ? JavaBinFinder.findBin() : java;
        log.debugf("Using javaTool: %s", javaTool);
        args.add(javaTool);
    }

    public DevModeCommandLineBuilder forceC2(Boolean force) {
        forceC2 = force;
        return this;
    }

    public DevModeCommandLineBuilder jvmArgs(String jvmArgs) {
        args.add(jvmArgs);
        return this;
    }

    public DevModeCommandLineBuilder jvmArgs(List<String> jvmArgs) {
        args.addAll(jvmArgs);
        return this;
    }

    public DevModeCommandLineBuilder debug(String debug) {
        this.debug = debug;
        return this;
    }

    public DevModeCommandLineBuilder suspend(String suspend) {
        this.suspend = suspend;
        return this;
    }

    public DevModeCommandLineBuilder projectDir(File projectDir) {
        this.projectDir = projectDir;
        return this;
    }

    public DevModeCommandLineBuilder buildDir(File buildDir) {
        this.buildDir = buildDir;
        return this;
    }

    public DevModeCommandLineBuilder outputDir(File outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    public DevModeCommandLineBuilder buildSystemProperties(Map<String, String> buildSystemProperties) {
        this.buildSystemProperties = buildSystemProperties;
        return this;
    }

    public DevModeCommandLineBuilder buildSystemProperty(String name, String value) {
        this.buildSystemProperties.put(name, value);
        return this;
    }

    public DevModeCommandLineBuilder applicationName(String appName) {
        this.applicationName = appName;
        return this;
    }

    public DevModeCommandLineBuilder applicationVersion(String appVersion) {
        this.applicationVersion = appVersion;
        return this;
    }

    public DevModeCommandLineBuilder applicationArgs(String appArgs) {
        this.applicationArgs = appArgs;
        return this;
    }

    public DevModeCommandLineBuilder sourceEncoding(String srcEncoding) {
        this.sourceEncoding = srcEncoding;
        return this;
    }

    public DevModeCommandLineBuilder compilerOptions(String name, List<String> options) {
        compilerOptions.compute(name, (key, value) -> {
            if (value == null) {
                return new HashSet<>(options);
            }
            value.addAll(options);
            return value;
        });
        return this;
    }

    public DevModeCommandLineBuilder compilerOptions(Map<String, Set<String>> options) {
        compilerOptions.putAll(options);
        return this;
    }

    public DevModeCommandLineBuilder compilerPluginArtifacts(List<String> artifacts) {
        compilerPluginArtifacts = artifacts;
        return this;
    }

    public DevModeCommandLineBuilder compilerPluginOptions(List<String> options) {
        compilerPluginOptions = options;
        return this;
    }

    public DevModeCommandLineBuilder annotationProcessorPaths(Set<File> processorPaths) {
        this.processorPaths = processorPaths;
        return this;
    }

    public DevModeCommandLineBuilder annotationProcessors(List<String> processors) {
        this.processors = processors;
        return this;
    }

    public DevModeCommandLineBuilder releaseJavaVersion(String releaseJavaVersion) {
        this.releaseJavaVersion = releaseJavaVersion;
        return this;
    }

    public DevModeCommandLineBuilder sourceJavaVersion(String sourceJavaVersion) {
        this.sourceJavaVersion = sourceJavaVersion;
        return this;
    }

    public DevModeCommandLineBuilder targetJavaVersion(String targetJavaVersion) {
        this.targetJavaVersion = targetJavaVersion;
        return this;
    }

    public DevModeCommandLineBuilder watchedBuildFile(Path buildFile) {
        this.buildFiles.add(buildFile);
        return this;
    }

    public DevModeCommandLineBuilder deleteDevJar(boolean deleteDevJar) {
        this.deleteDevJar = deleteDevJar;
        return this;
    }

    public DevModeCommandLineBuilder baseName(String baseName) {
        this.baseName = baseName;
        return this;
    }

    public DevModeCommandLineBuilder remoteDev(boolean remoteDev) {
        this.entryPointCustomizer = devModeContext -> {
            devModeContext.setMode(QuarkusBootstrap.Mode.REMOTE_DEV_CLIENT);
            devModeContext.setAlternateEntryPoint(IsolatedRemoteDevModeMain.class.getName());
        };
        return this;
    }

    public DevModeCommandLineBuilder entryPointCustomizer(Consumer<DevModeContext> consumer) {
        this.entryPointCustomizer = consumer;
        return this;
    }

    public DevModeCommandLineBuilder localArtifact(ArtifactKey localArtifact) {
        localArtifacts.add(localArtifact);
        return this;
    }

    public boolean isLocal(ArtifactKey artifact) {
        return localArtifacts.contains(artifact);
    }

    public DevModeCommandLineBuilder mainModule(DevModeContext.ModuleInfo mainModule) {
        main = mainModule;
        return this;
    }

    public DevModeCommandLineBuilder dependency(DevModeContext.ModuleInfo module) {
        dependencies.add(module);
        return this;
    }

    public DevModeCommandLineBuilder classpathEntry(ArtifactKey key, File f) {
        final File prev = classpath.put(key, f);
        if (prev != null && !f.equals(prev)) {
            Logger.getLogger(getClass()).warn(key + " classpath entry " + prev + " was overriden with " + f);
        }
        return this;
    }

    public DevModeCommandLineBuilder debugHost(String host) {
        if ((null != host) && !host.isEmpty()) {
            this.debugHost = host;
        }
        return this;
    }

    public DevModeCommandLineBuilder debugPort(String port) {
        if ((null != port) && !port.isEmpty()) {
            this.debugPort = port;
        }
        return this;
    }

    public DevModeCommandLineBuilder addOpens(String value) {
        jvmOptionsBuilder.add("add-opens", value);
        return this;
    }

    public DevModeCommandLineBuilder addModules(Collection<String> modules) {
        jvmOptionsBuilder.addAll("add-modules", modules);
        return this;
    }

    public DevModeCommandLineBuilder extensionDevModeConfig(Collection<ExtensionDevModeConfig> extDevModeConfig) {
        this.extDevModeConfig = extDevModeConfig;
        return this;
    }

    public DevModeCommandLineBuilder extensionDevModeJvmOptionFilter(
            ExtensionDevModeJvmOptionFilter extDevModeJvmOptionFilter) {
        this.extDevModeJvmOptionFilter = extDevModeJvmOptionFilter;
        return this;
    }

    public DevModeCommandLine build() throws Exception {
        JBossVersion.disableVersionLogging();

        //build a class-path string for the base platform
        //this stuff does not change
        // Do not include URIs in the manifest, because some JVMs do not like that
        final DevModeContext devModeContext = new DevModeContext();
        for (Map.Entry<Object, Object> e : System.getProperties().entrySet()) {
            devModeContext.getSystemProperties().put(e.getKey().toString(), (String) e.getValue());
        }
        devModeContext.setProjectDir(projectDir);
        devModeContext.getBuildSystemProperties().putAll(buildSystemProperties);

        //  this is a minor hack to allow ApplicationConfig to be populated with defaults
        devModeContext.getBuildSystemProperties().putIfAbsent("quarkus.application.name", applicationName);
        devModeContext.getBuildSystemProperties().putIfAbsent("quarkus.application.version", applicationVersion);

        devModeContext.getBuildSystemProperties().putIfAbsent("quarkus.live-reload.ignore-module-info", "true");

        devModeContext.setSourceEncoding(sourceEncoding);
        devModeContext.setCompilerOptions(compilerOptions);

        if (compilerPluginArtifacts != null) {
            devModeContext.setCompilerPluginArtifacts(compilerPluginArtifacts);
        }
        if (compilerPluginOptions != null) {
            devModeContext.setCompilerPluginsOptions(compilerPluginOptions);
        }
        if (processorPaths != null) {
            devModeContext.setAnnotationProcessorPaths(processorPaths);
        }
        if (processors != null) {
            devModeContext.setAnnotationProcessors(processors);
        }

        devModeContext.setReleaseJavaVersion(releaseJavaVersion);
        devModeContext.setSourceJavaVersion(sourceJavaVersion);
        devModeContext.setTargetJvmVersion(targetJavaVersion);
        devModeContext.getLocalArtifacts().addAll(localArtifacts);
        devModeContext.setApplicationRoot(main);
        devModeContext.getAdditionalModules().addAll(dependencies);

        devModeContext.setBaseName(baseName);
        devModeContext.setCacheDir(new File(buildDir, "transformer-cache").getAbsoluteFile());

        if (entryPointCustomizer != null) {
            entryPointCustomizer.accept(devModeContext);
        }

        // if the --enable-preview flag was set, then we need to enable it when launching dev mode as well
        if (devModeContext.isEnablePreview()) {
            jvmOptionsBuilder.add("enable-preview");
        }

        setJvmOptions();
        args.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");

        outputDir.mkdirs();

        args.add("-jar");
        args.add(createDevJar(devModeContext).getAbsolutePath());
        if (applicationArgs != null) {
            args.addAll(Arrays.asList(CommandLineUtil.translateCommandline(applicationArgs)));
        }

        return new DevModeCommandLine(args, actualDebugPort, buildFiles);
    }

    private File createDevJar(DevModeContext devModeContext) throws IOException {

        final File tempFile = new File(buildDir, applicationName + "-dev.jar");
        tempFile.delete();
        // Only delete the -dev.jar on exit if requested
        if (deleteDevJar) {
            tempFile.deleteOnExit();
        }
        log.debugf("Executable jar: %s", tempFile.getAbsolutePath());

        // this is the jar file we will use to launch the dev mode main class
        devModeContext.setDevModeRunnerJarFile(tempFile);

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile))) {
            out.putNextEntry(new ZipEntry("META-INF/"));
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

            final StringBuilder classPathManifest = new StringBuilder();
            classpath.values().forEach(file -> {
                final URI uri = file.toPath().toAbsolutePath().toUri();
                classPathManifest.append(uri).append(" ");
            });

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
        return tempFile;
    }

    private void setJvmOptions() throws Exception {
        final Map<String, List<ArtifactKey>> lockedJvmOptions = addExtensionJvmOptions();
        if (isDisableC2(lockedJvmOptions)) {
            // prevent C2 compiler for kicking in - makes startup a little faster
            // it only makes sense in dev-mode but it is not available when GraalVM is used as the JDK
            args.add("-XX:" + TIERED_STOP_AT_LEVEL + "=1");
        }

        List<ArtifactKey> extensionsDisablingDebug;
        if (debug == null && (extensionsDisablingDebug = lockedJvmOptions.get(AGENTLIB_JDWP)) != null) {
            extensionsDisablingDebugWarning(extensionsDisablingDebug);
        } else {
            configureDebugging();
        }

        for (var jvmOption : jvmOptionsBuilder.build()) {
            if (forceC2 != null && jvmOption.getName().equals(TIERED_STOP_AT_LEVEL)) {
                continue;
            }
            args.addAll(jvmOption.toCliOptions());
        }
    }

    /**
     * Checks user and extension config options to decide whether to disable C2.
     * <p>
     * By default, the C2 compiler is disabled for dev mode to make startup a little faster.
     * It only makes sense in dev-mode but it is not available when GraalVM is used as the JDK.
     *
     * @param lockedJvmOptions JVM option locked by extensions
     * @return whether to disable the C2 compiler
     */
    private boolean isDisableC2(Map<String, List<ArtifactKey>> lockedJvmOptions) {
        // a user's choice
        if (forceC2 != null) {
            return !forceC2;
        }
        // an extension configured it
        if (jvmOptionsBuilder.contains(TIERED_STOP_AT_LEVEL)) {
            return false;
        }
        // if it's locked, don't set it
        final List<ArtifactKey> lockingExtensions = lockedJvmOptions.get(TIERED_STOP_AT_LEVEL);
        if (lockingExtensions != null) {
            extensionsEnableC2Warning(lockingExtensions);
            return false;
        }
        return true;
    }

    private void configureDebugging() throws Exception {
        if (suspend != null) {
            switch (suspend.toLowerCase(Locale.ENGLISH)) {
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
                    log.warn("Ignoring invalid value \"" + suspend + "\" for \"suspend\" param and defaulting to \"n\"");
                    suspend = "n";
                    break;
                }
            }
        } else {
            suspend = "n";
        }

        int port = 5005;

        if (debugPort != null && validPort.matcher(debugPort).matches()) {
            port = Integer.parseInt(debugPort);
        }
        if (debug != null) {
            if (!validDebug.matcher(debug).matches()) {
                throw new Exception(
                        "Invalid value for debug parameter: " + debug + " must be true|false|client|{port}");
            }
            if (validPort.matcher(debug).matches()) {
                port = Integer.parseInt(debug);
            }
        }
        int originalPort = port;
        if (port <= 0) {
            port = getRandomPort();
        }

        if (debug != null && debug.equalsIgnoreCase("client")) {
            args.add("-" + AGENTLIB_JDWP + "=transport=dt_socket,address=" + debugHost + ":" + port + ",server=n,suspend="
                    + suspend);
            actualDebugPort = String.valueOf(port);
        } else if (debug == null || !debug.equalsIgnoreCase("false")) {
            // if the debug port is used, we want to make an effort to pick another one
            // if we can't find an open port, we don't fail the process launch, we just don't enable debugging
            // Furthermore, we don't check this on restarts, as the previous process is still running
            boolean warnAboutChange = false;
            if (actualDebugPort == null) {
                int tries = 0;
                while (true) {
                    boolean isPortUsed;
                    try (Socket socket = new Socket(getInetAddress(debugHost), port)) {
                        // we can make a connection, that means the port is in use
                        isPortUsed = true;
                        warnAboutChange = warnAboutChange || (originalPort != 0); // we only want to warn if the user had not configured a random port
                    } catch (IOException e) {
                        // no connection made, so the port is not in use
                        isPortUsed = false;
                    }
                    if (!isPortUsed) {
                        actualDebugPort = String.valueOf(port);
                        break;
                    }
                    if (++tries >= 5) {
                        break;
                    } else {
                        port = getRandomPort();
                    }
                }
            }
            if (actualDebugPort != null) {
                if (warnAboutChange) {
                    log.warn("Changed debug port to " + actualDebugPort + " because of a port conflict");
                }
                args.add("-" + AGENTLIB_JDWP + "=transport=dt_socket,address=" + debugHost + ":" + port + ",server=y,suspend="
                        + suspend);
            } else {
                log.error("Port " + port + " in use, not starting in debug mode");
            }
        }
    }

    /**
     * Add JVM arguments configured by extensions.
     */
    private Map<String, List<ArtifactKey>> addExtensionJvmOptions() {
        if (extDevModeConfig == null || extDevModeJvmOptionFilter != null && extDevModeJvmOptionFilter.isDisableAll()) {
            return Map.of();
        }
        Map<String, List<ArtifactKey>> mergedLockedOptions = Map.of();
        for (var extDevConfig : extDevModeConfig) {
            if (extDevModeJvmOptionFilter != null && extDevModeJvmOptionFilter.isDisabled(extDevConfig.getExtensionKey())) {
                log.debugf("Skipped JVM options from %s", extDevConfig.getExtensionKey());
                continue;
            }
            final JvmOptions jvmOptions = extDevConfig.getJvmOptions();
            if (jvmOptions != null && !jvmOptions.isEmpty()) {
                jvmOptionsBuilder.addAll(jvmOptions);
                if (log.isDebugEnabled()) {
                    log.debugf("Adding JVM options from %s", extDevConfig.getExtensionKey());
                    for (var arg : jvmOptions.asCollection()) {
                        log.debug("  " + arg.getName() + ": " + arg.getValues());
                    }
                }
            }
            if (!extDevConfig.getLockJvmOptions().isEmpty()) {
                mergedLockedOptions = collectLockedOptions(mergedLockedOptions, extDevConfig);
            }
        }
        return mergedLockedOptions;
    }

    private static Map<String, List<ArtifactKey>> collectLockedOptions(Map<String, List<ArtifactKey>> allLockedOptions,
            ExtensionDevModeConfig extDevConfig) {
        final Collection<String> extLockedOptions = extDevConfig.getLockJvmOptions();
        if (allLockedOptions.isEmpty()) {
            allLockedOptions = new HashMap<>(extLockedOptions.size());
        }
        for (var option : extLockedOptions) {
            allLockedOptions.computeIfAbsent(option, k -> new ArrayList<>(1)).add(extDevConfig.getExtensionKey());
        }
        log.debugf("%s locks JVM options %s", extDevConfig.getExtensionKey(), extLockedOptions);
        return allLockedOptions;
    }

    private int getRandomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private InetAddress getInetAddress(String host) throws UnknownHostException {
        if ("localhost".equals(host)) {
            return InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
        }
        return InetAddress.getByName(host);
    }
}
