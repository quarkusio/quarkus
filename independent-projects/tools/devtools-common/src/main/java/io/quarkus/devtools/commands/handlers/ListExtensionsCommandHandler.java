package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.project.extensions.Extensions.toKey;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getGuide;
import static java.util.stream.Collectors.toMap;

import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import io.quarkus.platform.catalog.processor.ExtensionProcessor;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionOrigin;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
        final String format = invocation.getValue(ListExtensions.FORMAT, "");
        final String search = invocation.getValue(ListExtensions.SEARCH, "*");
        final String category = invocation.getValue(ListExtensions.CATEGORY, "");
        final boolean batchMode = invocation.getValue(ListExtensions.BATCH_MODE, false);
        final ExtensionManager extensionManager = invocation.getValue(ListExtensions.EXTENSION_MANAGER,
                invocation.getQuarkusProject().getExtensionManager());

        final Collection<Extension> extensions = search == null ? invocation.getExtensionsCatalog().getExtensions()
                : QuarkusCommandHandlers.select(search, invocation.getExtensionsCatalog().getExtensions(), true)
                        .getExtensions();

        if (extensions.isEmpty()) {
            log.info("No extension found with pattern '%s'", search);
            return QuarkusCommandOutcome.success();
        }

        if (!batchMode) {
            String extensionStatus = all ? "available" : "installable";
            if (installedOnly)
                extensionStatus = "installed";
            log.info("Current Quarkus extensions %s: ", extensionStatus);
            log.info("");
        }

        BiConsumer<MessageWriter, Object[]> currentFormatter;
        switch (format.toLowerCase()) {
            case "full":
                currentFormatter = this::fullFormatter;
                log.info(String.format(FULL_FORMAT, "Status", "Extension", "ArtifactId", "Updated Version", "Guide"));
                break;
            case "origins":
                currentFormatter = this::originsFormatter;
                break;
            case "concise":
                currentFormatter = this::conciseFormatter;
                break;
            case "id":
            default:
                currentFormatter = this::nameFormatter;
                break;
        }

        Map<ArtifactKey, ArtifactCoords> installedByKey;
        try {
            installedByKey = extensionManager.getInstalled().stream()
                    .collect(toMap(ArtifactCoords::getKey, Function.identity()));
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to determine the list of installed extensions", e);
        }

        Predicate<Extension> categoryFilter;
        if (category != null && !category.isBlank()) {
            categoryFilter = e -> ExtensionProcessor.of(e).getCategories().contains(category);
        } else {
            categoryFilter = e -> true;
        }

        extensions.stream()
                .filter(e -> !ExtensionProcessor.of(e).isUnlisted())
                .filter(categoryFilter)
                .sorted(Comparator.comparing(e -> e.getArtifact().getArtifactId()))
                .forEach(e -> display(log, e, installedByKey.get(toKey(e)), all, installedOnly, currentFormatter));

        return QuarkusCommandOutcome.success();
    }

    private void conciseFormatter(MessageWriter writer, Object[] cols) {
        Extension e = (Extension) cols[1];
        writer.info(String.format(CONCISE_FORMAT, e.getName(), e.getArtifact().getArtifactId()));
    }

    private void fullFormatter(MessageWriter writer, Object[] cols) {
        Extension e = (Extension) cols[1];
        final String guide = getGuide(e);
        writer.info(String.format(FULL_FORMAT, cols[0], e.getName(), e.getArtifact().getArtifactId(), cols[2],
                guide == null ? "" : guide));
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

    private void display(MessageWriter messageWriter, final Extension e, final ArtifactCoords installed,
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
