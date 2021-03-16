package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.project.extensions.Extensions.toKey;
import static java.util.stream.Collectors.toMap;

import io.quarkus.bootstrap.model.AppArtifactCoords;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionOrigin;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Instances of this class are thread-safe. It lists extensions according to the options passed in as properties of
 * {@link QuarkusCommandInvocation}
 */
public class ListExtensionsCommandHandler implements QuarkusCommandHandler {

    private static final String FULL_FORMAT = "%-8s %-50s %-50s %-25s%s";
    private static final String CONCISE_FORMAT = "%-50s %-50s";
    private static final String NAME_FORMAT = "%-50s";
    private static final String ORIGINS_FORMAT = "%-50s %-60s %s";

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

        final Collection<Extension> extensions = search == null ? invocation.getExtensionsCatalog().getExtensions()
                : QuarkusCommandHandlers.select(search, invocation.getExtensionsCatalog().getExtensions(), true)
                        .getExtensions();

        if (extensions.isEmpty()) {
            log.info("No extension found with pattern '%s'", search);
            return QuarkusCommandOutcome.success();
        }

        if (!cli) {
            String extensionStatus = all ? "available" : "installable";
            if (installedOnly)
                extensionStatus = "installed";
            log.info("%nCurrent Quarkus extensions %s: ", extensionStatus);
        }

        BiConsumer<MessageWriter, Object[]> currentFormatter;
        switch (format.toLowerCase()) {
            case "name":
                currentFormatter = this::nameFormatter;
                break;
            case "full":
                currentFormatter = this::fullFormatter;
                log.info(String.format(FULL_FORMAT, "Status", "Extension", "ArtifactId", "Updated Version", "Guide"));
                break;
            case "origins":
                currentFormatter = this::originsFormatter;
                break;
            case "concise":
            default:
                currentFormatter = this::conciseFormatter;
        }

        Map<AppArtifactKey, AppArtifactCoords> installedByKey;
        try {
            installedByKey = extensionManager.getInstalled().stream()
                    .collect(toMap(AppArtifactCoords::getKey, Function.identity()));
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to determine the list of installed extensions", e);
        }
        extensions.stream()
                .filter(e -> !e.isUnlisted())
                .forEach(e -> display(log, e, installedByKey.get(toKey(e)), all, installedOnly, currentFormatter));
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

        return QuarkusCommandOutcome.success();
    }

    private void conciseFormatter(MessageWriter writer, Object[] cols) {
        Extension e = (Extension) cols[1];
        writer.info(String.format(CONCISE_FORMAT, e.getName(), e.getArtifact().getArtifactId()));
    }

    private void fullFormatter(MessageWriter writer, Object[] cols) {
        Extension e = (Extension) cols[1];
        writer.info(String.format(FULL_FORMAT, cols[0], e.getName(), e.getArtifact().getArtifactId(), cols[2],
                e.getGuide() == null ? "" : e.getGuide()));
    }

    private void nameFormatter(MessageWriter writer, Object[] cols) {
        Extension e = (Extension) cols[1];
        writer.info(String.format(NAME_FORMAT, e.getArtifact().getArtifactId()));
    }

    private void originsFormatter(MessageWriter writer, Object[] cols) {
        Extension e = (Extension) cols[1];
        String origin = null;
        int i = 0;
        final List<ExtensionOrigin> origins = e.getOrigins();
        while (i < origins.size() && origin == null) {
            final ExtensionOrigin o = origins.get(i++);
            if (o.isPlatform()) {
                origin = o.getBom().toString();
            }
        }
        writer.info(String.format(ORIGINS_FORMAT, e.getName(), e.getArtifact().getVersion(), origin == null ? "" : origin));
        while (i < origins.size()) {
            final ExtensionOrigin o = origins.get(i++);
            if (o.isPlatform()) {
                writer.info(String.format(ORIGINS_FORMAT, "", "", o.getBom().toString()));
            }
        }
    }

    private void display(MessageWriter messageWriter, final Extension e, final AppArtifactCoords installed,
            boolean all,
            boolean installedOnly,
            BiConsumer<MessageWriter, Object[]> formatter) {
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
                version = e.getArtifact().getVersion();
            } else if (installedVersion.equalsIgnoreCase(e.getArtifact().getVersion())) {
                label = "custom";
                version = installedVersion;
            } else {
                label = "custom*";
                version = String.format("%s* <> %s", installedVersion, e.getArtifact().getVersion());
            }
        }

        formatter.accept(messageWriter, new Object[] { label, e, version });
    }

}
