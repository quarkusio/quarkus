package io.quarkus.deployment.jvm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;

import org.jboss.logging.Logger;

import io.quarkus.builder.BuildException;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.ModuleEnableNativeAccessBuildItem;
import io.quarkus.deployment.builditem.ModuleExportBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * Represents requirements and restrictions on the runtime.
 * Currently only used to track add-opens requirements; we'd like to eventually
 * support tracking, for example, the required JVM version as we start to see
 * some libraries having more specific restrictions.
 * Another use could be, for example, to force enabling experimental features such
 * as needing jdk.incubator.vector and implied version requirements.
 */
public final class ResolvedJVMRequirements extends SimpleBuildItem {

    private static final Attributes.Name ADD_OPENS_JARATTRIBUTENAME = new Attributes.Name("Add-Opens");
    private static final Attributes.Name ADD_EXPORTS_JARATTRIBUTENAME = new Attributes.Name("Add-Exports");
    private static final Attributes.Name ENABLE_NATIVE_JARATTRIBUTENAME = new Attributes.Name("Enable-Native-Access");

    private final List<ModuleOpenBuildItem> addOpens;
    private final List<ModuleExportBuildItem> addExports;
    private final List<ModuleEnableNativeAccessBuildItem> enableNativeAccesses;

    public ResolvedJVMRequirements(final List<ModuleOpenBuildItem> addOpens,
            List<ModuleEnableNativeAccessBuildItem> enableNativeAccesses) throws BuildException {
        this(addOpens, List.of(), enableNativeAccesses);
    }

    public ResolvedJVMRequirements(final List<ModuleOpenBuildItem> addOpens,
            final List<ModuleExportBuildItem> addExports,
            final List<ModuleEnableNativeAccessBuildItem> enableNativeAccesses) throws BuildException {
        this.addOpens = addOpens;
        this.addExports = addExports;
        this.enableNativeAccesses = enableNativeAccesses;
    }

    public void renderAddOpensElementToJarManifest(final Attributes attributes) {
        //N.B. in the format of the Add-Opens attribute in the MANIFEST.MF, the "target module" to which things
        //get opened to is implicitly the Jar itself, so it will always point to ALL-UNNAMED:
        //we're ignoring the ModuleOpenBuildItem#openingModuleName in this context.
        //N.B.2: if the app is a module, this Manifest attribute will apparently be ignored!
        // See: https://docs.oracle.com/en/java/javase/25/docs/specs/jar/jar.html#main-attributes
        final Collection<String> modulesToAddOpens = modulePackagesToOpen();
        if (!modulesToAddOpens.isEmpty()) {
            if (attributes.getValue(ADD_OPENS_JARATTRIBUTENAME) != null) {
                Logger.getLogger(ResolvedJVMRequirements.class)
                        .warn(
                                "An 'Add-Opens' entry was already defined in your MANIFEST.MF or using the property quarkus.package.jar.manifest.attributes.\"Add-Opens\". Quarkus has overwritten this existing entry.");
            }
            attributes.put(ADD_OPENS_JARATTRIBUTENAME, String.join(" ", modulesToAddOpens));
        }
        final Collection<String> modulesToAddExports = modulePackagesToExport();
        if (!modulesToAddExports.isEmpty()) {
            if (attributes.getValue(ADD_EXPORTS_JARATTRIBUTENAME) != null) {
                Logger.getLogger(ResolvedJVMRequirements.class)
                        .warn(
                                "An 'Add-Exports' entry was already defined in your MANIFEST.MF or using the property quarkus.package.jar.manifest.attributes.\"Add-Exports\". Quarkus has overwritten this existing entry.");
            }
            attributes.put(ADD_EXPORTS_JARATTRIBUTENAME, String.join(" ", modulesToAddExports));
        }
        if (!enableNativeAccesses.isEmpty()) {
            attributes.put(ENABLE_NATIVE_JARATTRIBUTENAME, "ALL-UNNAMED");//This is the only supported value for now
        }
    }

    /**
     * Renders the requirements as JVM command line arguments, for launchers that start the application
     * class directly rather than through the manifest of a packaged jar. As with the manifest, modules are
     * opened to the unnamed module; the {@link ModuleOpenBuildItem#openingModuleName()} is ignored.
     *
     * @return the {@code --add-opens} and {@code --enable-native-access} arguments, in a stable order
     */
    public List<String> renderAsJvmArguments() {
        final Collection<String> toOpen = modulePackagesToOpen();
        final Collection<String> toExport = modulePackagesToExport();
        final List<String> arguments = new ArrayList<>(toOpen.size() + toExport.size() + 1);
        for (String modulePackage : toOpen) {
            arguments.add("--add-opens=" + modulePackage + "=ALL-UNNAMED");
        }
        for (String modulePackage : toExport) {
            arguments.add("--add-exports=" + modulePackage + "=ALL-UNNAMED");
        }
        if (!enableNativeAccesses.isEmpty()) {
            arguments.add("--enable-native-access=ALL-UNNAMED");
        }
        return arguments;
    }

    private List<String> modulePackagesToOpen() {
        // a list but we keep it sorted
        final List<String> modulesToAddOpens = new ArrayList<>(addOpens.size() * 3);
        for (ModuleOpenBuildItem moduleOpenBuildItem : addOpens) {
            for (String packageName : moduleOpenBuildItem.packageNames()) {
                //When there are multiple packages to be opened within the same module, the whole definition needs to be repeated; e.g.:
                //Add-Opens: java.base/java.lang java.base/java.util
                String str = moduleOpenBuildItem.openedModuleName() + '/' + packageName;
                int idx = Collections.binarySearch(modulesToAddOpens, str);
                if (idx < 0) {
                    modulesToAddOpens.add(-idx - 1, str);
                }
            }
        }
        return modulesToAddOpens;
    }

    private List<String> modulePackagesToExport() {
        // a list but we keep it sorted
        final List<String> modulesToAddExports = new ArrayList<>(addExports.size() * 3);
        for (ModuleExportBuildItem moduleExportBuildItem : addExports) {
            for (String packageName : moduleExportBuildItem.packageNames()) {
                //When there are multiple packages to be exported within the same module, the whole definition needs to be repeated; e.g.:
                //Add-Exports: java.base/java.lang java.base/java.util
                String str = moduleExportBuildItem.exportedModuleName() + '/' + packageName;
                int idx = Collections.binarySearch(modulesToAddExports, str);
                if (idx < 0) {
                    modulesToAddExports.add(-idx - 1, str);
                }
            }
        }
        return modulesToAddExports;
    }

    public void applyJavaModuleConfigurationToRuntime(JvmModulesReconfigurer reconfigurer,
            ClassLoader referenceClassloader) {
        if (addOpens.isEmpty() && addExports.isEmpty())
            return;
        ModulesClassloaderContext context = new ModulesClassloaderContext(referenceClassloader);
        if (!addOpens.isEmpty()) {
            reconfigurer.openJavaModules(addOpens, context);
        }
        if (!addExports.isEmpty()) {
            reconfigurer.exportJavaModules(addExports, context);
        }
    }

}
