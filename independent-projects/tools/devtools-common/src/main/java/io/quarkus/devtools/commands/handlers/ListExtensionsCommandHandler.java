package io.quarkus.devtools.commands.handlers;

import static io.quarkus.devtools.project.extensions.Extensions.toKey;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getGuide;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.quarkus.devtools.commands.ListExtensions;
import io.quarkus.devtools.commands.data.QuarkusCommandException;
import io.quarkus.devtools.commands.data.QuarkusCommandInvocation;
import io.quarkus.devtools.commands.data.QuarkusCommandOutcome;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.maven.dependency.ArtifactCoords;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.platform.catalog.processor.ExtensionProcessor;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionOrigin;

/**
 * Instances of this class are thread-safe. It lists extensions according to the options passed in as properties of
 * {@link QuarkusCommandInvocation}
 */
public class ListExtensionsCommandHandler implements QuarkusCommandHandler {

    // private static final String NAME_FORMAT = "%-50s";
    // private static final String CONCISE_FORMAT = "%-50s %-50s";
    // private static final String FULL_FORMAT = "%-8s %-50s %-50s %-25s%s";
    // private static final String ORIGINS_FORMAT = "%-50s %-60s %s";
    private static final String ID_FORMAT = "%-1s %s";
    private static final String CONCISE_FORMAT = "%-1s %-50s %s";
    private static final String ORIGINS_FORMAT = "%-1s %-50s %-50s %-25s %s";
    private static final String FULL_FORMAT = "%-1s %-50s %-60s %-25s %s";

    @Override
    public QuarkusCommandOutcome execute(QuarkusCommandInvocation invocation) throws QuarkusCommandException {

        final MessageWriter log = invocation.log();
        final boolean all = invocation.getValue(ListExtensions.ALL, true);
        final boolean installedOnly = invocation.getValue(ListExtensions.INSTALLED, false);
        final String format = invocation.getValue(ListExtensions.FORMAT, "");
        final String search = invocation.getValue(ListExtensions.SEARCH, "*");
        final String category = invocation.getValue(ListExtensions.CATEGORY, "");
        final boolean batchMode = invocation.getValue(ListExtensions.BATCH_MODE, false);
        final ExtensionManager extensionManager = invocation.getValue(ListExtensions.EXTENSION_MANAGER,
                invocation.getQuarkusProject().getExtensionManager());

        final Collection<Extension> extensions = search == null ? invocation.getExtensionsCatalog().getExtensions()
                : QuarkusCommandHandlers.listExtensions(search, invocation.getExtensionsCatalog().getExtensions(), true)
                        .getExtensions();

        if (extensions.isEmpty()) {
            if (!format.equalsIgnoreCase("object")) {
                log.info("No extension found with pattern '%s'", search);
            }
            return QuarkusCommandOutcome.success();
        }

        if (!batchMode && !format.equalsIgnoreCase("object")) {
            String extensionStatus = all ? "available" : "installable";
            if (installedOnly)
                extensionStatus = "installed";
            log.info("Current Quarkus extensions %s: ", extensionStatus);
            log.info("");
        }

        BiConsumer<MessageWriter, DisplayData> currentFormatter;
        switch (format.toLowerCase()) {
            case "object":
                currentFormatter = null;
                break;
            case "id":
            case "name":
                currentFormatter = this::idFormatter;
                log.info(String.format(ID_FORMAT, "✬", "ArtifactId"));
                break;
            default:
            case "concise":
                currentFormatter = this::conciseFormatter;
                log.info(String.format(CONCISE_FORMAT, "✬", "ArtifactId", "Extension Name"));
                break;
            case "origins":
                currentFormatter = this::originsFormatter;
                log.info(String.format(ORIGINS_FORMAT, "✬", "ArtifactId", "Extension Name", "Version", "Origin"));
                break;
            case "full":
                currentFormatter = this::fullFormatter;
                log.info(String.format(FULL_FORMAT, "✬", "ArtifactId", "Extension", "Version", "Guide"));
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

        if (currentFormatter != null) {
            extensions.stream()
                    .filter(e -> !ExtensionProcessor.of(e).isUnlisted())
                    .filter(categoryFilter)
                    .sorted(Comparator.comparing(e -> e.getArtifact().getArtifactId()))
                    .forEach(e -> display(log, e, installedByKey.get(toKey(e)), all, installedOnly, currentFormatter));
        } else {
            List<Extension> filteredExtensions = extensions.stream()
                    .filter(e -> !ExtensionProcessor.of(e).isUnlisted())
                    .filter(categoryFilter)
                    .filter(e -> {
                        ArtifactCoords installed = installedByKey.get(toKey(e));
                        if (installedOnly && installed == null) {
                            return false;
                        }
                        if (!installedOnly && !all && installed != null) {
                            return false;
                        }
                        return true;
                    })
                    .sorted(Comparator.comparing(e -> e.getArtifact().getArtifactId()))
                    .collect(Collectors.toList());
            return QuarkusCommandOutcome.success(filteredExtensions);
        }

        return QuarkusCommandOutcome.success();
    }

    private void idFormatter(MessageWriter writer, DisplayData data) {
        writer.info(String.format(ID_FORMAT,
                data.isPlatform(),
                data.getExtensionArtifactId()));
    }

    private void conciseFormatter(MessageWriter writer, DisplayData data) {
        writer.info(String.format(CONCISE_FORMAT,
                data.isPlatform(),
                data.trimToFit(50, data.getExtensionArtifactId()),
                data.getExtensionName()));
    }

    private void originsFormatter(MessageWriter writer, DisplayData data) {
        List<String> origins = data.getOrigins();
        writer.info(String.format(ORIGINS_FORMAT,
                data.isPlatform(),
                data.trimToFit(50, data.getExtensionArtifactId()),
                data.trimToFit(50, data.getExtensionName()),
                data.getVersion(),
                origins.isEmpty() ? "" : origins.get(0)));

        // If there is more than one platform origin, list the others
        for (int i = 1; i < origins.size(); i++) {
            writer.info(String.format(ORIGINS_FORMAT,
                    "",
                    "",
                    "",
                    "",
                    origins.get(i)));
        }
    }

    private void fullFormatter(MessageWriter writer, DisplayData data) {
        final String guide = getGuide(data.e);
        writer.info(String.format(FULL_FORMAT,
                data.isPlatform(),
                data.trimToFit(50, data.getExtensionArtifactId()),
                data.trimToFit(60, data.getExtensionName()),
                data.getVersion(),
                guide != null ? guide : ""));
    }

    private void display(MessageWriter messageWriter, final Extension e, final ArtifactCoords installed,
            boolean all,
            boolean installedOnly,
            BiConsumer<MessageWriter, DisplayData> formatter) {
        if (installedOnly && installed == null) {
            return;
        }
        if (!installedOnly && !all && installed != null) {
            return;
        }

        formatter.accept(messageWriter, new DisplayData(e, installed));
    }

    class DisplayData {
        Extension e;
        ArtifactCoords installed;

        DisplayData(Extension e, ArtifactCoords installed) {
            this.e = e;
            this.installed = installed;
        }

        String getExtensionName() {
            return e.getName();
        }

        String getExtensionArtifactId() {
            return e.getArtifact().getArtifactId();
        }

        List<String> getOrigins() {
            ExtensionOrigin origin = null;
            int i = 0;
            final List<ExtensionOrigin> origins = e.getOrigins();
            while (i < origins.size() && origin == null) {
                final ExtensionOrigin o = origins.get(i++);
                if (o.isPlatform()) {
                    origin = o;
                }
            }
            if (origins.isEmpty() || origin == null) {
                return Arrays.asList("");
            }

            // Add the discovered origin first
            final List<String> result = new ArrayList<>();
            result.add(origin.getBom().toString().replace("::pom", ""));

            // Append any other platform origins
            final ExtensionOrigin o = origin;
            origins.stream()
                    .filter(e -> e.isPlatform())
                    .filter(e -> e != o)
                    .map(e -> e.getBom().toString().replace("::pom", ""))
                    .collect(Collectors.toCollection(() -> result));
            return result;
        }

        String getVersion() {
            if (installed == null || installed.getVersion() == null) {
                return e.getArtifact().getVersion();
            }

            return installed.getVersion();
        }

        String isPlatform() {
            return e.hasPlatformOrigin() ? "✬" : "";
        }

        String trimToFit(int max, String s) {
            if (s.length() >= max) {
                return s.substring(0, max - 4) + "...";
            } else {
                return s;
            }
        }
    }
}
