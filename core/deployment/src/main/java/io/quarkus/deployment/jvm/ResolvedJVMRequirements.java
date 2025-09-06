package io.quarkus.deployment.jvm;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.Attributes;

import org.jboss.logging.Logger;

import io.quarkus.builder.BuildException;
import io.quarkus.builder.item.SimpleBuildItem;
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

    private final List<ModuleOpenBuildItem> addOpens;

    public ResolvedJVMRequirements(final List<ModuleOpenBuildItem> addOpens) throws BuildException {
        this.addOpens = addOpens;
    }

    public void renderAddOpensElementToJarManifest(final Attributes attributes) {
        //N.B. in the format of the Add-Opens attribute in the MANIFEST.MF, the "target module" to which things
        //get opened to is implicitly the Jar itself, so it will always point to ALL-UNNAMED:
        //we're ignoring the ModuleOpenBuildItem#openingModuleName in this context.
        //N.B.2: if the app is a module, this Manifest attribute will apparently be ignored!
        final Collection<String> modulesToAddOpens = new TreeSet<>(); //Choose a TreeSet as it will sort them, providing a stable order for reproducibility
        for (ModuleOpenBuildItem moduleOpenBuildItem : addOpens) {
            for (String packageName : moduleOpenBuildItem.packageNames()) {
                //When there are multiple packages to be opened within the same module, the whole definition needs to be repeated; e.g.:
                //Add-Opens: java.base/java.lang java.base/java.util
                modulesToAddOpens.add(moduleOpenBuildItem.openedModuleName() + '/' + packageName);
            }
        }
        if (!modulesToAddOpens.isEmpty()) {
            if (attributes.getValue(ADD_OPENS_JARATTRIBUTENAME) != null) {
                Logger.getLogger(ResolvedJVMRequirements.class)
                        .warn(
                                "An 'Add-Opens' entry was already defined in your MANIFEST.MF or using the property quarkus.package.jar.manifest.attributes.\"Add-Opens\". Quarkus has overwritten this existing entry.");
            }
            attributes.put(ADD_OPENS_JARATTRIBUTENAME, String.join(" ", modulesToAddOpens));
        }
    }

    public void applyJavaModuleConfigurationToRuntime(JvmModulesReconfigurer reconfigurer,
            ClassLoader referenceClassloader) {
        if (addOpens.isEmpty())
            return;
        ModulesClassloaderContext context = new ModulesClassloaderContext(referenceClassloader);
        reconfigurer.openJavaModules(addOpens, context);
    }

}
