package io.quarkus.deployment;

import java.util.List;
import java.util.jar.Attributes;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.ModuleOpenBuildItem;

/**
 * Represents requirements and restrictions on the runtime.
 * Currently only used to track add-opens requirements; I'd like to eventually
 * support tracking, for example, the required JVM version as we start to see
 * some extensions having stronger opinions.
 * Another use could be, for example, to force enabling experimental features such
 * as needing jdk.incubator.vector (which would also inherently bump the minimum
 * JVM version).
 */
public final class ResolvedJVMRequirements extends SimpleBuildItem {
    private static final Logger LOG = Logger.getLogger(ResolvedJVMRequirements.class);
    private static final Attributes.Name ADD_OPENS_JARATTRIBUTENAME = new Attributes.Name("Add-Opens");
    private List<String> modulesToAddOpens;

    public ResolvedJVMRequirements(List<ModuleOpenBuildItem> addOpens) {
        this.modulesToAddOpens = addOpens.stream().map(ModuleOpenBuildItem::moduleName).distinct().sorted().toList();
    }

    public void renderAddOpensElementToJarManifest(Attributes attributes) {
        if (!modulesToAddOpens.isEmpty()) {
            if (attributes.getValue(ADD_OPENS_JARATTRIBUTENAME) != null) {
                LOG.warn(
                        "An 'Add-Opens' entry was already defined in your MANIFEST.MF or using the property quarkus.package.jar.manifest.attributes.\"Add-Opens\". Quarkus has overwritten this existing entry.");
            }
            attributes.put(ADD_OPENS_JARATTRIBUTENAME, String.join(" ", modulesToAddOpens));
        }
    }
}
