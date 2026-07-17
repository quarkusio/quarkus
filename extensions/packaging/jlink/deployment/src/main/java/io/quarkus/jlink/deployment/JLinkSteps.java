package io.quarkus.jlink.deployment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.JarTreeShakeBuildItem;
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
import io.smallrye.modules.desc.Dependency;

/**
 * The steps for producing a jlink image from the modular model.
 */
public final class JLinkSteps {
    private static final Logger log = Logger.getLogger("io.quarkus.jlink");
    private static final Logger jlinkOut = Logger.getLogger("io.quarkus.jlink.out");
    public static final String APP_MAIN = "io.quarkus.jlink.launcher.AppMain";

    private final JLinkConfig config;

    public JLinkSteps(final JLinkConfig config) {
        this.config = config;
    }

    /**
     * {@return the item for the jlink launcher module}
     */
    @BuildStep
    public BootModulePathBuildItem bootPath() {
        return new BootModulePathBuildItem("io.quarkus.jlink.launcher");
    }

    /**
     * Generate the jlink launcher main class early so it is visible to the tree-shaker
     * as a {@link GeneratedClassBuildItem}. This ensures the launcher module has a reachable
     * class and is not pruned by module tree-shaking.
     *
     * @param curateOutcome provides the application model (for the app module name)
     * @param generatedClasses producer for generated class build items
     */
    @BuildStep
    void generateLauncherMainClass(
            CurateOutcomeBuildItem curateOutcome,
            BuildProducer<GeneratedClassBuildItem> generatedClasses) {

        String appModuleName = curateOutcome.getApplicationModel().getAppArtifact().getModuleName();
        Gizmo gizmo = Gizmo.create((path, bytes) -> generatedClasses.produce(
                new GeneratedClassBuildItem(false, APP_MAIN, bytes)));
        gizmo.class_(APP_MAIN, cc -> {
            cc.public_();
            cc.staticMethod("main", mc -> {
                mc.public_();
                ParamVar args = mc.parameter("args", String[].class);
                mc.body(b0 -> {
                    b0.invokeStatic(
                            MethodDesc.of(JLinkAppLauncher.class, "run", void.class, String.class, String[].class),
                            Const.of(appModuleName),
                            args);
                    b0.return_();
                });
            });
        });
    }

    /**
     * Steps to stage the jlink output into directories so it can be processed by the tool itself.
     *
     * @param moduleInfoItem the modular model item (must not be {@code null})
     * @return the staged jlink output (not {@code null})
     * @throws IOException if a file operation fails
     */
    @BuildStep
    public JLinkStagedOutputItem stageOutput(
            ApplicationModuleInfoBuildItem moduleInfoItem,
            JarTreeShakeBuildItem treeShakeResult) throws IOException {

        // gather information we'll need
        AppModuleModel model = moduleInfoItem.model();
        Map<String, ModuleInfo> modulesByName = model.modulesByName();
        Set<String> reachable = treeShakeResult.isClassesShaken() ? treeShakeResult.getReachableClassNames() : null;

        // create the staging directory
        final Path staging = config.outputDirectory().resolve(config.stagingDirectory());
        Files.createDirectories(staging);

        // create a mapping for each boot module path
        final Map<String, Path> bmp = new HashMap<>();
        for (String moduleName : model.bootModules()) {
            // get this boot module descriptor
            ModuleInfo moduleInfo = modulesByName.get(moduleName);
            if (moduleInfo == null) {
                throw new IllegalStateException("Boot module listed in model is missing from the module index");
            }
            // the launcher module gets extra dependencies on all other boot modules
            // and the main class set (the AppMain class itself was generated earlier
            // by generateLauncherMainClass so it is visible to the tree-shaker)
            if (moduleName.equals("io.quarkus.jlink.launcher")) {
                moduleInfo = moduleInfo
                        .withMoreDependencies(model.bootModules().stream().filter(n -> !n.equals(moduleName))
                                .map(n -> new DependencyInfo(n,
                                        Dependency.Modifier.Set.of(Dependency.Modifier.LINKED, Dependency.Modifier.READ,
                                                Dependency.Modifier.SERVICES),
                                        Map.of()))
                                .toList())
                        .withMainClass(APP_MAIN);
            }
            // find the first manifest file
            Manifest manifest = new Manifest();
            moduleInfo.resolvedArtifact().getContentTree().apply("META-INF/MANIFEST.MF", pv -> {
                if (pv != null) {
                    Path mfPath = pv.getPath();
                    try (InputStream is = Files.newInputStream(mfPath)) {
                        manifest.read(is);
                        return null;
                    } catch (FileNotFoundException | NoSuchFileException ignored) {
                    } catch (IOException e) {
                        throw sneak(e);
                    }
                }
                return null;
            });
            bmp.put(moduleName, ModuleWriter.writeModule(moduleInfo, staging, manifest, true, reachable));
        }
        return new JLinkStagedOutputItem(staging, bmp);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> RuntimeException sneak(Throwable t) throws E {
        throw (E) t;
    }

    /**
     * A step to execute jlink on the staged output and copy in the dynamic module set to produce the final image.
     *
     * @param stagedOutput the staged jlink output item (must not be {@code null})
     * @param moduleInfoItem the modular model item (must not be {@code null})
     * @return an item representing the built image (not {@code null})
     * @throws BuildException if there is a problem running jlink
     * @throws IOException if there is a filesystem problem
     */
    @BuildStep
    public JLinkImageBuildItem jlink(
            JLinkStagedOutputItem stagedOutput,
            ApplicationModuleInfoBuildItem moduleInfoItem,
            JarTreeShakeBuildItem treeShakeResult) throws BuildException, IOException {
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

        List<String> jdkModules = moduleInfoItem.model().jdkModulesUsed();
        log.debugf("JDK modules included in image (%d): %s", jdkModules.size(), jdkModules);
        String addedModules = "ALL-MODULE-PATH,jdk.jdwp.agent," + String.join(",", jdkModules);

        // JVM args to embed in the image launcher script.
        // These are written directly into the launcher after jlink runs,
        // because jlink's --add-options parser rejects values starting with "--".
        jvmArgs.add("--add-exports=java.base/jdk.internal.module=io.smallrye.modules");
        jvmArgs.add("--add-modules=" + addedModules);
        // todo: patch for loom
        //jvmArgs.add("--patch-module=java.base=" + javaBasePatchPath);
        config.minHeapSize().ifPresent(s -> jvmArgs.add("-Xms" + s.toLowerString()));
        config.maxHeapSize().ifPresent(s -> jvmArgs.add("-Xmx" + s.toLowerString()));
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
        jlinkArgs.add(addedModules);

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
        // patch the launcher scripts to include JVM args
        if (!jvmArgs.isEmpty()) {
            Path binDir = imagePath.resolve("bin");
            patchLauncherScript(binDir.resolve(config.launcherName()), jvmArgs);
            Path batFile = binDir.resolve(config.launcherName() + ".bat");
            if (Files.exists(batFile)) {
                patchWindowsLauncherScript(batFile, jvmArgs);
            }
        }
        // produce the dynamic lib files
        // bundle dynamic modules into the image
        Path lib = config.outputDirectory().resolve(config.imagePath()).resolve("lib").resolve("quarkus");
        Files.createDirectories(lib);
        final AppModuleModel model = moduleInfoItem.model();
        Map<String, ModuleInfo> modulesByName = model.modulesByName();
        Set<String> reachable = treeShakeResult.isClassesShaken() ? treeShakeResult.getReachableClassNames() : null;
        for (ModuleInfo moduleInfo : modulesByName.values()) {
            String moduleName = moduleInfo.name();
            if (model.bootModules().contains(moduleName)) {
                // skip boot modules
                continue;
            }
            // TODO: create an actual manifest (if needed)
            ModuleWriter.writeModule(moduleInfo, lib.resolve(moduleInfo.name()), new Manifest(), false, reachable);
        }

        log.infof("JLink image produced in %s at %s; to run the image, execute %s in that directory",
                format(duration), imagePath, Path.of("bin").resolve(config.launcherName()));
        return new JLinkImageBuildItem(imagePath, Set.of(config.launcherName()));
    }

    /**
     * Patch a jlink-generated launcher shell script to inject JVM arguments.
     * <p>
     * jlink's {@code --add-options} plugin cannot handle values starting with {@code "--"}
     * (e.g. {@code --add-exports}, {@code --add-modules}), so we inject them directly
     * into the launcher script by replacing the empty {@code JLINK_VM_OPTIONS=} line.
     *
     * @param launcherPath the path to the launcher script
     * @param jvmArgs the JVM arguments to inject
     * @throws IOException if the script cannot be read or written
     */
    private static void patchLauncherScript(Path launcherPath, List<String> jvmArgs) throws IOException {
        String content = Files.readString(launcherPath);
        String opts = String.join(" ", jvmArgs);
        String patched = content.replace("JLINK_VM_OPTIONS=\n",
                "JLINK_VM_OPTIONS=\"" + opts + "\"\n");
        Files.writeString(launcherPath, patched);
    }

    /**
     * Patch a jlink-generated Windows {@code .bat} launcher to inject JVM arguments.
     * The batch file uses {@code set JLINK_VM_OPTIONS=} syntax.
     *
     * @param launcherPath the path to the {@code .bat} launcher script
     * @param jvmArgs the JVM arguments to inject
     * @throws IOException if the script cannot be read or written
     */
    private static void patchWindowsLauncherScript(Path launcherPath, List<String> jvmArgs) throws IOException {
        String content = Files.readString(launcherPath);
        String opts = String.join(" ", jvmArgs);
        // handle both \r\n (Windows) and \n (Unix) line endings
        String patched = content.replace("set JLINK_VM_OPTIONS=\r\n",
                "set JLINK_VM_OPTIONS=" + opts + "\r\n");
        if (patched.equals(content)) {
            patched = content.replace("set JLINK_VM_OPTIONS=\n",
                    "set JLINK_VM_OPTIONS=" + opts + "\n");
        }
        Files.writeString(launcherPath, patched);
    }

    /**
     * Produce a classic artifact result build item from the jlink image item.
     *
     * @param imageItem the jlink image item (must not be {@code null})
     * @return the artifact result item (not {@code null})
     */
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
            millis -= minutes * 1000L * 60L;
        }
        if (millis >= 0L) {
            long seconds = millis / (1000L);
            millis %= 1000L;
            if (millis > 0 || seconds > 0) {
                b.append(seconds);
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
        }
        return b;
    }
}
