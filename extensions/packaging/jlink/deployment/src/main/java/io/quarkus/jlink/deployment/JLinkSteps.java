package io.quarkus.jlink.deployment;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.spi.ToolProvider;

import org.jboss.logging.Logger;

import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.jlink.launcher.JLinkAppLauncher;
import io.quarkus.jlink.spi.JLinkImageBuildItem;
import io.quarkus.jlink.spi.JLinkStagedOutputItem;
import io.quarkus.modular.spi.ModuleWriter;
import io.quarkus.modular.spi.items.ApplicationModuleInfoBuildItem;
import io.quarkus.modular.spi.items.BootModulePathBuildItem;
import io.quarkus.modular.spi.model.AppModuleModel;
import io.quarkus.modular.spi.model.DependencyInfo;
import io.quarkus.modular.spi.model.ModuleInfo;
import io.smallrye.common.io.Files2;
import io.smallrye.common.resource.MemoryResource;
import io.smallrye.common.resource.Resource;
import io.smallrye.modules.desc.Dependency;

public final class JLinkSteps {
    private static final Logger log = Logger.getLogger("io.quarkus.jlink");
    private static final Logger jlinkOut = Logger.getLogger("io.quarkus.jlink.out");
    public static final String APP_MAIN = "io.quarkus.jlink.launcher.AppMain";

    private final JLinkConfig config;

    public JLinkSteps(final JLinkConfig config) {
        this.config = config;
    }

    @BuildStep
    public BootModulePathBuildItem bootPath() {
        return new BootModulePathBuildItem("io.quarkus.jlink.launcher");
    }

    @BuildStep
    public JLinkStagedOutputItem stageOutput(
            CurateOutcomeBuildItem curateOutcome,
            ApplicationModuleInfoBuildItem moduleInfoItem) throws IOException {
        // fetch and patch each JAR file
        final Path staging = config.outputDirectory().resolve(config.stagingDirectory());
        Files.createDirectories(staging);
        final Map<String, Path> bmp = new HashMap<>();
        AppModuleModel model = moduleInfoItem.model();
        Map<String, ModuleInfo> modulesByName = model.modulesByName();
        for (String moduleName : model.bootModules()) {
            ModuleInfo moduleInfo = modulesByName.get(moduleName);
            if (moduleInfo == null) {
                throw new IllegalStateException("Boot module listed in model is missing from the module index");
            }
            if (moduleName.equals("io.quarkus.jlink.launcher")) {
                List<Resource> dynModuleResources = new ArrayList<>();
                // now generate the main class
                Gizmo gizmo = Gizmo.create((path, bytes) -> dynModuleResources.add(new MemoryResource(path, bytes)));
                gizmo.class_(APP_MAIN, cc -> {
                    cc.public_();
                    cc.staticMethod("main", mc -> {
                        mc.public_();
                        ParamVar args = mc.parameter("args", String[].class);
                        mc.body(b0 -> {
                            b0.invokeStatic(
                                    MethodDesc.of(JLinkAppLauncher.class, "run", void.class, String.class, String[].class),
                                    Const.of(moduleInfoItem.model().appModuleInfo().name()),
                                    args);
                            b0.return_();
                        });
                    });
                });
                moduleInfo = moduleInfo
                        .withMoreResources(dynModuleResources)
                        .withMoreDependencies(model.bootModules().stream().filter(n -> !n.equals(moduleName))
                                .map(n -> new DependencyInfo(n,
                                        Dependency.Modifier.set(Dependency.Modifier.LINKED, Dependency.Modifier.READ,
                                                Dependency.Modifier.SERVICES),
                                        Map.of()))
                                .toList())
                        .withMainClass(APP_MAIN);
            }
            // write the JAR
            // TODO: create an actual manifest
            bmp.put(moduleName, ModuleWriter.writeModule(moduleInfo, staging, new Manifest(), true));
        }
        return new JLinkStagedOutputItem(staging, bmp);
    }

    @BuildStep
    public JLinkImageBuildItem jlink(
            JLinkStagedOutputItem stagedOutput,
            ApplicationModuleInfoBuildItem moduleInfoItem) throws BuildException, IOException {
        Path imagePath = config.outputDirectory().resolve(config.imagePath());

        // the image must be deleted or jlink will complain
        if (Files.exists(imagePath)) {
            Files2.deleteRecursively(imagePath);
        }

        // use the ToolProvider interface for now; later, explore calling directly via jlink internal API
        ToolProvider jlinkProvider = ToolProvider.findFirst("jlink")
                .orElseThrow(() -> new BuildException("jlink is not present"));
        List<String> jvmArgs = new ArrayList<>();
        List<String> jlinkArgs = new ArrayList<>();

        // JVM args

        // access to module internals
        jvmArgs.add("--add-exports=java.base/jdk.internal.module=io.smallrye.modules");
        // add modules to JVM path
        // xxx replace with injected module dependencies on main module?
        jvmArgs.add("--add-modules=ALL-MODULE-PATH,jdk.jdwp.agent,jdk.compiler");
        // todo: patch for loom
        //jvmArgs.add("--patch-module");
        //jvmArgs.add("java.base=" + javaBasePatchPath);
        // memory
        // todo: scale these values so they aren't silly long
        config.minHeapSize().ifPresent(s -> jvmArgs.add("-Xms" + s.asBigInteger()));
        config.maxHeapSize().ifPresent(s -> jvmArgs.add("-Xmx" + s.asBigInteger()));
        jvmArgs.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
        // xxx CDS arguments

        // JLink args

        // bind services
        //jlinkArgs.add("--bind-services"); // XXX tries to link against jlink for some reason?
        // minimize the image
        jlinkArgs.add("--no-header-files");
        jlinkArgs.add("--no-man-pages");
        // avoid issues with signed JARs
        jlinkArgs.add("--ignore-signing-information");
        // launcher
        // todo: support multiple launchers? one per profile?
        jlinkArgs.add("--launcher");
        jlinkArgs.add(config.launcherName() + "=io.quarkus.jlink.launcher/" + APP_MAIN);
        // todo: verbose output
        //jlinkArgs.add("-v");
        // output path
        jlinkArgs.add("--output");
        jlinkArgs.add(imagePath.toString());

        // JLink plugin args

        // CDS arguments -- todo
        //jlinkArgs.add("--generate-cds-archive");
        // add necessary JDK options
        if (!jvmArgs.isEmpty()) {
            jlinkArgs.add("--add-options=" + String.join(" ", jvmArgs));
        }
        // just choose server VM
        jlinkArgs.add("--vm");
        jlinkArgs.add("server");

        // ↓↓ these options must be LAST ↓↓

        // build the module path from patched JARs
        jlinkArgs.add("--module-path");
        // todo: maybe just a directory instead?
        jlinkArgs.add(String.join(File.pathSeparator,
                stagedOutput.bootModulePath().values().stream().map(Path::toString).toArray(String[]::new)));

        // todo: include the whole boot path until --bind-services works
        jlinkArgs.add("--add-modules");
        jlinkArgs.add("ALL-MODULE-PATH,jdk.zipfs,jdk.jdwp.agent,jdk.compiler");

        // everything looks good; make the directory for the image output
        Files.createDirectories(imagePath.getParent());
        log.info("Building image with jlink");
        log.debugf("JLink arguments: %s", String.join("\n\t", jlinkArgs));
        Instant start = Instant.now();
        int result = jlinkProvider.run(
                new PrintWriter(new LogWriter(jlinkOut, Logger.Level.INFO)),
                new PrintWriter(new LogWriter(jlinkOut, Logger.Level.WARN)),
                jlinkArgs.toArray(String[]::new));
        Instant end = Instant.now();
        Duration duration = end.compareTo(start) < 0 ? Duration.ZERO : Duration.between(start, end);
        if (result != 0) {
            throw new BuildException("Build failed during jlink (exit code " + result + ")");
        }
        // produce the dynamic lib files
        // bundle dynamic modules into the image
        Path lib = config.outputDirectory().resolve(config.imagePath()).resolve("lib").resolve("quarkus");
        Files.createDirectories(lib);
        final AppModuleModel model = moduleInfoItem.model();
        Map<String, ModuleInfo> modulesByName = model.modulesByName();
        for (ModuleInfo moduleInfo : modulesByName.values()) {
            String moduleName = moduleInfo.name();
            if (model.bootModules().contains(moduleName)) {
                // skip boot modules
                continue;
            }
            // TODO: create an actual manifest
            ModuleWriter.writeModule(moduleInfo, lib.resolve(moduleInfo.name()), new Manifest(), false);
        }

        log.infof("JLink image produced in %s at %s; to run the image, execute %s in that directory",
                format(duration), imagePath, Path.of("bin").resolve(config.launcherName()));
        return new JLinkImageBuildItem(imagePath, Set.of(config.launcherName()));
    }

    @BuildStep
    public ArtifactResultBuildItem produceArtifactResult(JLinkImageBuildItem imageItem) {
        return new ArtifactResultBuildItem(imageItem.imagePath().toAbsolutePath(), "jlink", Map.of());
    }

    // todo: we should have a central Duration formatter
    static CharSequence format(Duration duration) {
        if (duration.isZero()) {
            return "0s";
        }
        StringBuilder b = new StringBuilder(16);
        long millis = duration.toMillis();
        if (millis >= 1000L * 60L * 60L) {
            long hours = millis / (1000L * 60L * 60L);
            b.append(hours);
            b.append('h');
            millis -= hours * 1000L * 60L * 60L;
        }
        if (millis >= 1000L * 60L) {
            long minutes = millis / (1000L * 60L);
            b.append(minutes);
            b.append('m');
        }
        if (millis >= 0L) {
            long seconds = millis / (1000L);
            b.append(seconds);
            millis %= 1000L;
            if (millis > 0) {
                b.append('.');
                if (millis < 100) {
                    b.append('0');
                }
                if (millis < 10) {
                    b.append('0');
                }
                b.append(millis);
            }
            b.append('s');
        }
        return b;
    }
}
