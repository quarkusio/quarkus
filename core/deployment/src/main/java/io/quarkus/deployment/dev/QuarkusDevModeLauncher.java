package io.quarkus.deployment.dev;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

import org.apache.maven.shared.utils.cli.CommandLineUtils;

import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.deployment.dev.DevModeContext.ModuleInfo;
import io.quarkus.runtime.util.JavaVersionUtil;
import io.quarkus.utilities.JavaBinFinder;

public abstract class QuarkusDevModeLauncher {
    final Pattern validDebug = Pattern.compile("^(true|false|client|[0-9]+)$");
    final Pattern validPort = Pattern.compile("^[0-9]+$");

    public class Builder<R extends QuarkusDevModeLauncher, B extends Builder<R, B>> {

        protected Builder(String java) {
            args = new ArrayList<>();
            final String javaTool = java == null ? JavaBinFinder.findBin() : java;
            QuarkusDevModeLauncher.this.debug("Using javaTool: %s", javaTool);
            args.add(javaTool);

        }

        @SuppressWarnings("unchecked")
        public B preventnoverify(boolean preventnoverify) {
            if (!preventnoverify) {
                // in Java 13 and up, preventing verification is deprecated - see https://bugs.openjdk.java.net/browse/JDK-8218003
                // this test isn't absolutely correct in the sense that depending on the user setup, the actual Java binary
                // that is used might be different that the one running Maven, but given how small of an impact this has
                // it's probably better than running an extra command on 'javaTool' just to figure out the version
                if (!JavaVersionUtil.isJava13OrHigher()) {
                    args.add("-Xverify:none");
                }
            }
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B jvmArgs(String jvmArgs) {
            args.add(jvmArgs);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B jvmArgs(List<String> jvmArgs) {
            args.addAll(jvmArgs);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B debug(String debug) {
            QuarkusDevModeLauncher.this.debug = debug;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B debugPortOk(Boolean debugPortOk) {
            QuarkusDevModeLauncher.this.debugPortOk = debugPortOk;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B suspend(String suspend) {
            QuarkusDevModeLauncher.this.suspend = suspend;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B projectDir(File projectDir) {
            QuarkusDevModeLauncher.this.projectDir = projectDir;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B buildDir(File buildDir) {
            QuarkusDevModeLauncher.this.buildDir = buildDir;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B outputDir(File outputDir) {
            QuarkusDevModeLauncher.this.outputDir = outputDir;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B buildSystemProperties(Map<String, String> buildSystemProperties) {
            QuarkusDevModeLauncher.this.buildSystemProperties = buildSystemProperties;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B buildSystemProperty(String name, String value) {
            QuarkusDevModeLauncher.this.buildSystemProperties.put(name, value);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B applicationName(String appName) {
            QuarkusDevModeLauncher.this.applicationName = appName;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B applicationVersion(String appVersion) {
            QuarkusDevModeLauncher.this.applicationVersion = appVersion;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B applicationArgs(String appArgs) {
            QuarkusDevModeLauncher.this.applicationArgs = appArgs;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B sourceEncoding(String srcEncoding) {
            QuarkusDevModeLauncher.this.sourceEncoding = srcEncoding;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B compilerOption(String option) {
            compilerOptions.add(option);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B compilerOptions(List<String> options) {
            compilerOptions.addAll(options);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B compilerPluginArtifacts(List<String> artifacts) {
            compilerPluginArtifacts = artifacts;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B compilerPluginOptions(List<String> options) {
            compilerPluginOptions = options;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B sourceJavaVersion(String sourceJavaVersion) {
            QuarkusDevModeLauncher.this.sourceJavaVersion = sourceJavaVersion;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B targetJavaVersion(String targetJavaVersion) {
            QuarkusDevModeLauncher.this.targetJavaVersion = targetJavaVersion;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B watchedBuildFile(Path buildFile) {
            QuarkusDevModeLauncher.this.buildFiles.add(buildFile);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B deleteDevJar(boolean deleteDevJar) {
            QuarkusDevModeLauncher.this.deleteDevJar = deleteDevJar;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B baseName(String baseName) {
            QuarkusDevModeLauncher.this.baseName = baseName;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B remoteDev(boolean remoteDev) {
            QuarkusDevModeLauncher.this.entryPointCustomizer = new Consumer<DevModeContext>() {
                @Override
                public void accept(DevModeContext devModeContext) {
                    devModeContext.setMode(QuarkusBootstrap.Mode.REMOTE_DEV_CLIENT);
                    devModeContext.setAlternateEntryPoint(IsolatedRemoteDevModeMain.class.getName());
                }
            };
            return (B) this;
        }

        public B entryPointCustomizer(Consumer<DevModeContext> consumer) {
            QuarkusDevModeLauncher.this.entryPointCustomizer = consumer;
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B localArtifact(AppArtifactKey localArtifact) {
            localArtifacts.add(localArtifact);
            return (B) this;
        }

        public boolean isLocal(AppArtifactKey artifact) {
            return localArtifacts.contains(artifact);
        }

        @SuppressWarnings("unchecked")
        public B mainModule(ModuleInfo mainModule) {
            main = mainModule;
            return (B) this;
        }

        public boolean isTestsPresent() {
            if (main == null) {
                return false;
            }
            return main.getTest().isPresent();
        }

        @SuppressWarnings("unchecked")
        public B dependency(ModuleInfo module) {
            dependencies.add(module);
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public B classpathEntry(File f) {
            classpath.add(f);
            return (B) this;
        }

        public B debugHost(String host) {
            if ((null != host) && !host.isEmpty()) {
                QuarkusDevModeLauncher.this.debugHost = host;
            }
            return (B) this;
        }

        public B debugPort(String port) {
            if ((null != port) && !port.isEmpty()) {
                QuarkusDevModeLauncher.this.debugPort = port;
            }
            return (B) this;
        }

        @SuppressWarnings("unchecked")
        public R build() throws Exception {
            prepare();
            return (R) QuarkusDevModeLauncher.this;
        }
    }

    private List<String> args = new ArrayList<>(0);
    private String debug;
    private Boolean debugPortOk;
    private String suspend;
    private String debugHost = "localhost";
    private String debugPort = "5005";
    private File projectDir;
    private File buildDir;
    private File outputDir;
    private Map<String, String> buildSystemProperties = new HashMap<>(0);
    private String applicationName;
    private String applicationVersion;
    private String sourceEncoding;
    private List<String> compilerOptions = new ArrayList<>(0);
    private List<String> compilerPluginArtifacts;
    private List<String> compilerPluginOptions;
    private String sourceJavaVersion;
    private String targetJavaVersion;
    private Set<Path> buildFiles = new HashSet<>(0);
    private boolean deleteDevJar = true;
    private String baseName;
    private Consumer<DevModeContext> entryPointCustomizer;
    private String applicationArgs;
    private Set<AppArtifactKey> localArtifacts = new HashSet<>();
    private ModuleInfo main;
    private List<ModuleInfo> dependencies = new ArrayList<>(0);
    private List<File> classpath = new ArrayList<>(0);

    protected QuarkusDevModeLauncher() {
    }

    /**
     * Attempts to prepare the dev mode runner.
     */
    protected void prepare() throws Exception {

        if (!JavaVersionUtil.isGraalvmJdk()) {
            // prevent C2 compiler for kicking in - makes startup a little faster
            // it only makes sense in dev-mode but it is not available when GraalVM is used as the JDK
            args.add("-XX:TieredStopAtLevel=1");
        }

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
                    warn("Ignoring invalid value \"" + suspend + "\" for \"suspend\" param and defaulting to \"n\"");
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
        if (port <= 0) {
            throw new Exception("The specified debug port must be greater than 0");
        }

        if (debug != null && debug.toLowerCase().equals("client")) {
            args.add("-agentlib:jdwp=transport=dt_socket,address=" + debugHost + ":" + port + ",server=n,suspend=" + suspend);
        } else if (debug == null || !debug.toLowerCase().equals("false")) {
            // make sure the debug port is not used, we don't want to just fail if something else is using it
            // we don't check this on restarts, as the previous process is still running
            if (debugPortOk == null) {
                try (Socket socket = new Socket(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), port)) {
                    error("Port " + port + " in use, not starting in debug mode");
                    debugPortOk = false;
                } catch (IOException e) {
                    debugPortOk = true;
                }
            }
            if (debugPortOk) {
                args.add("-agentlib:jdwp=transport=dt_socket,address=" + debugHost + ":" + port + ",server=y,suspend="
                        + suspend);
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
        devModeContext.setProjectDir(projectDir);
        devModeContext.getBuildSystemProperties().putAll(buildSystemProperties);

        //  this is a minor hack to allow ApplicationConfig to be populated with defaults
        devModeContext.getBuildSystemProperties().putIfAbsent("quarkus.application.name", applicationName);
        devModeContext.getBuildSystemProperties().putIfAbsent("quarkus.application.version", applicationVersion);

        devModeContext.setSourceEncoding(sourceEncoding);
        devModeContext.setCompilerOptions(compilerOptions);

        if (compilerPluginArtifacts != null) {
            devModeContext.setCompilerPluginArtifacts(compilerPluginArtifacts);
        }
        if (compilerPluginOptions != null) {
            devModeContext.setCompilerPluginsOptions(compilerPluginOptions);
        }

        devModeContext.setSourceJavaVersion(sourceJavaVersion);
        devModeContext.setTargetJvmVersion(targetJavaVersion);

        devModeContext.getLocalArtifacts().addAll(localArtifacts);
        devModeContext.setApplicationRoot(main);
        devModeContext.getAdditionalModules().addAll(dependencies);

        args.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");

        File tempFile = new File(buildDir, applicationName + "-dev.jar");
        tempFile.delete();
        // Only delete the -dev.jar on exit if requested
        if (deleteDevJar) {
            tempFile.deleteOnExit();
        }
        debug("Executable jar: %s", tempFile.getAbsolutePath());

        devModeContext.setBaseName(baseName);
        devModeContext.setCacheDir(new File(buildDir, "transformer-cache").getAbsoluteFile());

        // this is the jar file we will use to launch the dev mode main class
        devModeContext.setDevModeRunnerJarFile(tempFile);

        if (entryPointCustomizer != null) {
            entryPointCustomizer.accept(devModeContext);
        }

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile))) {
            out.putNextEntry(new ZipEntry("META-INF/"));
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

            if (!classpath.isEmpty()) {
                classpath.forEach(file -> {
                    final URI uri = file.toPath().toAbsolutePath().toUri();
                    classPathManifest.append(uri).append(" ");
                });
            }
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

        outputDir.mkdirs();
        // if the --enable-preview flag was set, then we need to enable it when launching dev mode as well
        if (devModeContext.isEnablePreview()) {
            args.add(DevModeContext.ENABLE_PREVIEW_FLAG);
        }

        args.add("-jar");
        args.add(tempFile.getAbsolutePath());
        if (applicationArgs != null) {
            args.addAll(Arrays.asList(CommandLineUtils.translateCommandline(applicationArgs)));
        }
    }

    public Collection<Path> watchedBuildFiles() {
        return buildFiles;
    }

    public List<String> args() {
        return args;
    }

    protected abstract boolean isDebugEnabled();

    protected void debug(Object msg, Object... args) {
        if (!isDebugEnabled()) {
            return;
        }
        if (msg == null) {
            return;
        }
        if (args.length == 0) {
            debug(msg);
            return;
        }
        debug(String.format(msg.toString(), args));
    }

    protected abstract void debug(Object msg);

    protected abstract void error(Object msg);

    protected abstract void warn(Object msg);
}
