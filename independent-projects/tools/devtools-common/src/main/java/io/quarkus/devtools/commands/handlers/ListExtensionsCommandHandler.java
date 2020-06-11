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
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.devtools.project.extensions.ExtensionManager;
import io.quarkus.platform.tools.MessageWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        final boolean all = invocation.getValue(ListExtensions.ALL, true);
        final String format = invocation.getValue(ListExtensions.FORMAT, "concise");
        final String search = invocation.getValue(ListExtensions.SEARCH, "*");
        final ExtensionManager extensionManager = invocation.getValue(ListExtensions.EXTENSION_MANAGER,
                invocation.getQuarkusProject().getExtensionManager());

        Map<AppArtifactKey, AppArtifactCoords> installedByKey;
        try {
            installedByKey = extensionManager.getInstalled().stream()
                    .collect(toMap(AppArtifactCoords::getKey, Function.identity()));
        } catch (IOException e) {
            throw new QuarkusCommandException("Failed to determine the list of installed extensions", e);
        }

        Stream<Extension> platformExtensionsStream = invocation.getPlatformDescriptor().getExtensions().stream();
        platformExtensionsStream = platformExtensionsStream.filter(this::filterUnlisted);
        if (search != null && !"*".equalsIgnoreCase(search)) {
            final Pattern searchPattern = Pattern.compile(".*" + search + ".*", Pattern.CASE_INSENSITIVE);
            platformExtensionsStream = platformExtensionsStream.filter(e -> filterBySearch(searchPattern, e));
        }
        List<Extension> platformExtensions = platformExtensionsStream.collect(Collectors.toList());

        if (platformExtensions.isEmpty()) {
            invocation.log().info("No extension found with this pattern");
        } else {
            String extensionStatus = all ? "available" : "installable";
            invocation.log().info(String.format("%nCurrent Quarkus extensions %s: ", extensionStatus));

            BiConsumer<MessageWriter, String[]> currentFormatter;
            switch (format.toLowerCase()) {
                case "name":
                    currentFormatter = this::nameFormatter;
                    break;
                case "full":
                    currentFormatter = this::fullFormatter;
                    currentFormatter.accept(invocation.log(),
                            new String[] { "Status", "Extension", "ArtifactId", "Updated Version", "Guide" });
                    break;
                case "concise":
                default:
                    currentFormatter = this::conciseFormatter;
            }

            platformExtensions.forEach(platformExtension -> display(invocation.log(), platformExtension,
                    installedByKey.get(toKey(platformExtension)), all, currentFormatter));
            final BuildTool buildTool = invocation.getQuarkusProject().getBuildTool();
            if ("concise".equalsIgnoreCase(format)) {
                if (BuildTool.GRADLE.equals(buildTool)) {
                    invocation.log().info("\nTo get more information, append --format=full to your command line.");
                } else {
                    invocation.log().info(
                            "\nTo get more information, append -Dquarkus.extension.format=full to your command line.");
                }
            }

            if (BuildTool.GRADLE.equals(buildTool)) {
                invocation.log().info("\nAdd an extension to your project by adding the dependency to your " +
                        "build.gradle or use `./gradlew addExtension --extensions=\"artifactId\"`");
            } else {
                invocation.log().info("\nAdd an extension to your project by adding the dependency to your " +
                        "pom.xml or use `./mvnw quarkus:add-extension -Dextensions=\"artifactId\"`");
            }

        }

        return QuarkusCommandOutcome.success();
    }

    private boolean filterUnlisted(Extension e) {
        return !e.getMetadata().containsKey("unlisted");
    }

    private boolean filterBySearch(final Pattern searchPattern, Extension e) {
        return searchPattern.matcher(e.getName()).matches();
    }

    private void conciseFormatter(MessageWriter writer, String[] cols) {
        writer.info(String.format(CONCISE_FORMAT, cols[1], cols[2], cols[4]));
    }

    private void fullFormatter(MessageWriter writer, String[] cols) {
        writer.info(String.format(FULL_FORMAT, cols[0], cols[1], cols[2], cols[3], cols[4]));
    }

    private void nameFormatter(MessageWriter writer, String[] cols) {
        writer.info(String.format(NAME_FORMAT, cols[2]));
    }

    private void display(MessageWriter messageWriter, final Extension platformExtension, final AppArtifactCoords installed,
            boolean all,
            BiConsumer<MessageWriter, String[]> formatter) {
        if (!all && installed != null) {
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
