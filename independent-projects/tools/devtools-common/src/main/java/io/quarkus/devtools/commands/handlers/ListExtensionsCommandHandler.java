package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.project.extensions.Extensions.toKey;
import static java.util.stream.Collectors.toMap;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.registry.DefaultExtensionRegistry;
import io.quarkus.registry.ExtensionRegistry;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Instances of this class are thread-safe. It lists extensions according to the options passed in as properties of
 * {@link QuarkusCommandInvocation}
 */
public class ListExtensionsCommandHandler implements QuarkusCommandHandler {

    private static final String FULL_FORMAT = "%-8s %-50s %-50s %-25s%n%s";
    private static final String CONCISE_FORMAT = "%-50s %-50s";
    private static final String NAME_FORMAT = "%-50s";

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {

        final MessageWriter log = invocation.log();
        final boolean all = invocation.getValue(ListExtensions.ALL, true);
        final boolean installedOnly = invocation.getValue(ListExtensions.INSTALLED, false);
        final boolean cli = invocation.getValue(ListExtensions.FROM_CLI, false);
        final String format = invocation.getValue(ListExtensions.FORMAT, "concise");
        final String search = invocation.getValue(ListExtensions.SEARCH, "*");
        final ExtensionManager extensionManager = invocation.getValue(ListExtensions.EXTENSION_MANAGER,
                invocation.getQuarkusProject().getExtensionManager());
        ExtensionRegistry extensionRegistry = invocation.getValue(ListExtensions.EXTENSION_REGISTRY);
        if (extensionRegistry == null) {
            extensionRegistry = DefaultExtensionRegistry.fromPlatform(invocation.getPlatformDescriptor());
        }
        Map<AppArtifactKey, AppArtifactCoords> installedByKey;
        try {
            installedByKey = extensionManager.getInstalled().stream()
                    .collect(toMap(AppArtifactCoords::getKey, Function.identity()));
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to determine the list of installed extensions", e);
        }
        String quarkusVersion = invocation.getPlatformDescriptor().getQuarkusVersion();
        Collection<Extension> platformExtensions = extensionRegistry.list(quarkusVersion, search);
        if (platformExtensions.isEmpty()) {
            log.info("No extension found with pattern '%s'", search);
        } else {
            if (!cli) {
                String extensionStatus = all ? "available" : "installable";
                if (installedOnly)
                    extensionStatus = "installed";
                log.info("%nCurrent Quarkus extensions %s: ", extensionStatus);
            }

            BiConsumer<MessageWriter, String[]> currentFormatter;
            switch (format.toLowerCase()) {
                case "name":
                    currentFormatter = this::nameFormatter;
                    break;
                case "full":
                    currentFormatter = this::fullFormatter;
                    currentFormatter.accept(log,
                            new String[] { "Status", "Extension", "ArtifactId", "Updated Version", "Guide" });
                    break;
                case "concise":
                default:
                    currentFormatter = this::conciseFormatter;
            }

            platformExtensions.forEach(platformExtension -> display(log, platformExtension,
                    installedByKey.get(toKey(platformExtension)), all, installedOnly, currentFormatter));
            final BuildTool buildTool = invocation.getQuarkusProject().getBuildTool();
            boolean isGradle = BuildTool.GRADLE.equals(buildTool) || BuildTool.GRADLE_KOTLIN_DSL.equals(buildTool);

            if (!cli) {
                if ("concise".equalsIgnoreCase(format)) {
                    if (isGradle) {
                        log.info("\nTo get more information, append --format=full to your command line.");
                    } else {
                        log.info(
                                "\nTo get more information, append -Dquarkus.extension.format=full to your command line.");
                    }
                }

                if (isGradle) {
                    log.info("\nAdd an extension to your project by adding the dependency to your " +
                            "build.gradle or use `./gradlew addExtension --extensions=\"artifactId\"`");
                } else {
                    log.info("\nAdd an extension to your project by adding the dependency to your " +
                            "pom.xml or use `./mvnw quarkus:add-extension -Dextensions=\"artifactId\"`");
                }
            }

        }

        return QuarkusCommandOutcome.success();
    }

    private void conciseFormatter(MessageWriter writer, String[] cols) {
        writer.info(String.format(CONCISE_FORMAT, cols[1], cols[2]));
    }

    private void fullFormatter(MessageWriter writer, String[] cols) {
        writer.info(String.format(FULL_FORMAT, cols[0], cols[1], cols[2], cols[3], cols[4]));
    }

    private void nameFormatter(MessageWriter writer, String[] cols) {
        writer.info(String.format(NAME_FORMAT, cols[2]));
    }

    private void display(MessageWriter messageWriter, final Extension platformExtension, final AppArtifactCoords installed,
            boolean all,
            boolean installedOnly,
            BiConsumer<MessageWriter, String[]> formatter) {
        if (installedOnly && installed == null) {
            return;
        }
        if (!installedOnly && !all && installed != null) {
            return;
        }

        String label = "";
        String version = "";

        if (installed != null) {
            final String installedVersion = installed.getVersion();
            if (installedVersion == null) {
                label = "default";
                version = platformExtension.getVersion();
            } else if (installedVersion.equalsIgnoreCase(platformExtension.getVersion())) {
                label = "custom";
                version = installedVersion;
            } else {
                label = "custom*";
                version = String.format("%s* <> %s", installedVersion, platformExtension.getVersion());
            }
        }

        String[] result = new String[] { label, platformExtension.getName(), platformExtension.getArtifactId(), version,
                platformExtension.getGuide() };

        for (int i = 0; i < result.length; i++) {
            result[i] = Objects.toString(result[i], "");
        }

        formatter.accept(messageWriter, result);
    }

}
