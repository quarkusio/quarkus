package io.quarkus.cli.create;

import static io.quarkus.devtools.commands.CreateExtension.extractQuarkiverseExtensionId;
import static io.quarkus.devtools.commands.CreateExtension.isQuarkiverseGroupId;

import io.quarkus.devtools.commands.CreateExtension;
import picocli.CommandLine;

public class ExtensionGAVMixin {
    final static String DEFAULT_EXTENSION_ID = "custom";
    final static String DEFAULT_GAV = "io.quarkiverse.custom:"
            + DEFAULT_EXTENSION_ID + ":"
            + CreateExtension.DEFAULT_VERSION;

    String groupId = null;
    String extensionId = null;
    String version = CreateExtension.DEFAULT_VERSION;

    @CommandLine.Parameters(arity = "0..1", paramLabel = "[GROUP-ID:]EXTENSION-ID[:VERSION]", description = "Quarkus extension project identifiers%n"
            + "  " + DEFAULT_GAV + "%n"
            + "  Examples:%n"
            + "     my-extension%n"
            + "     my.group:my-extension%n"
            + "     my.group:my-extension:0.1%n")
    String gav = null;

    void projectGav() {
        if (extensionId == null) {
            if (gav != null) {
                // process new gav parameter
                int firstPos = gav.indexOf(":");
                int lastPos = gav.lastIndexOf(":");
                if (firstPos < 0) {
                    // extension-id  -- use defaults for group id and version (common/demo)
                    extensionId = gav;
                } else {
                    // g::   -- (uncommon)
                    // g::v  -- (uncommon)
                    // g:a   -- COMMON
                    // g:a:  -- (uncommon alternate)
                    // g:a:v -- COMMON
                    // :a:   -- (uncommon alternate)
                    // :a:v  -- (uncommon)
                    if (firstPos != 0) {
                        groupId = gav.substring(0, firstPos);
                    }
                    if (lastPos == firstPos) {
                        extensionId = gav.substring(firstPos + 1);
                    } else if (lastPos >= firstPos + 2) {
                        extensionId = gav.substring(firstPos + 1, lastPos);
                    }
                    if (lastPos > firstPos && lastPos <= gav.length() - 2) {
                        version = gav.substring(lastPos + 1);
                    }
                }
            }

            if (isQuarkiverseGroupId(groupId) && extensionId == null) {
                extensionId = extractQuarkiverseExtensionId(groupId);
            }
            if (extensionId == null || extensionId.isBlank()) {
                extensionId = DEFAULT_EXTENSION_ID;
            }
            if (groupId == null || groupId.isBlank()) {
                groupId = CreateExtension.DEFAULT_QUARKIVERSE_PARENT_GROUP_ID + "." + extensionId;
            }
        }
    }

    public String getGroupId() {
        projectGav();
        return groupId;
    }

    public String getExtensionId() {
        projectGav();
        return extensionId;
    }

    public String getVersion() {
        projectGav();
        return version;
    }

    @Override
    public String toString() {
        return "ExtensionTargetGVGroup [groupId=" + groupId + ", extensionId=" + extensionId + ", version=" + version
                + ", gav=" + gav + "]";
    }
}
