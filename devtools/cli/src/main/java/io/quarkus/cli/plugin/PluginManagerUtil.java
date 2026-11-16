package io.quarkus.cli.plugin;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.maven.dependency.GACTV;
import io.quarkus.registry.catalog.Extension;

public class PluginManagerUtil {

    static final String ALIAS_SEPARATOR = ": ";
    private static final Pattern CLI_SUFFIX = Pattern.compile("(\\-cli)(@\\w+)?$");

    private final PluginManagerSettings settings;

    public static PluginManagerUtil getUtil(PluginManagerSettings settings) {
        return new PluginManagerUtil(settings);
    }

    public static PluginManagerUtil getUtil() {
        return getUtil(PluginManagerSettings.defaultSettings());
    }

    public PluginManagerUtil(PluginManagerSettings settings) {
        this.settings = settings;
    }

    /**
     * Create a {@link Plugin} from the specified location.
     *
     * @param the location
     * @return the {@link Plugin} that corresponds to the location.
     */
    public Plugin fromLocation(String location) {
        Optional<URL> url = PluginUtil.checkUrl(location);
        Optional<Path> path = PluginUtil.checkPath(location);
        Optional<GACTV> gactv = PluginUtil.checkGACTV(location);
        String name = getName(gactv, url, path);
        PluginType type = PluginUtil.getType(gactv, url, path);
        return new Plugin(name, type, Optional.of(location), Optional.empty());
    }

    /**
     * Create a {@link Plugin} from the specified alias.
     *
     * @param alias (e.g. name: location)
     * @return the {@link Plugin} that corresponds to the alias.
     */
    public Plugin fromAlias(String alias) {
        String name = null;
        String location = alias;
        if (alias.contains(ALIAS_SEPARATOR)) {
            name = alias.substring(0, alias.indexOf(ALIAS_SEPARATOR)).trim();
            location = alias.substring(alias.indexOf(ALIAS_SEPARATOR) + 1).trim();
        }
        return fromLocation(location).withName(name);
    }

    /**
     * Get the name that corresponds the the specified location.
     * The name is the filename (without the jar extension) of any of the specified gactv, url or path.
     *
     * @param location the location
     * @return the name.
     */
    public String getName(String location) {
        Optional<URL> url = PluginUtil.checkUrl(location);
        Optional<Path> path = PluginUtil.checkPath(location);
        Optional<GACTV> gactv = PluginUtil.checkGACTV(location);
        return getName(gactv, url, path);
    }

    /**
     * Get the name that corresponds the the specified locations.
     * The name is the filename (without the jar extension) of any of the specified gactv, url or path.
     *
     * @param url the url
     * @param path the path
     * @param gactv the gactv
     * @return the name.
     */
    public String getName(Optional<GACTV> gactv, Optional<URL> url, Optional<Path> path) {
        String prefix = settings.getPluginPrefix();
        return gactv.map(GACTV::getArtifactId)
                .or(() -> url.map(URL::getPath).map(s -> s.substring(s.lastIndexOf("/") + 1))
                        .map(s -> s.replaceAll("\\.jar$", "").replaceAll("\\.java$", "")))
                .or(() -> path.map(Path::getFileName).map(Path::toString)
                        .map(s -> s.replaceAll("\\.jar$", "").replaceAll("\\.java$", "")))
                .map(n -> stripCliSuffix(n))
                .map(n -> n.replaceAll("^" + prefix + "\\-cli\\-", prefix + "")) // stip cli prefix (after the quarkus bit)
                .map(n -> n.replaceAll("^" + prefix + "\\-", "")) // stip quarkus prefix (after the quarkus bit)
                .map(n -> n.replaceAll("@.*$", "")) // stip the @sufix
                .orElseThrow(() -> new IllegalStateException("Could not determinate name for location."));
    }

    /**
     * Collect all the transitive dependencies of the specified artifact.
     *
     * @param artifactKey the artifact key
     * @param allExtensions all the extensions
     * @return the set of transitive dependencies
     */
    public static List<ArtifactKey> getTransitives(ArtifactKey artifactKey, Map<ArtifactKey, Extension> allExtensions) {
        Extension extension = allExtensions.get(artifactKey);
        Map<String, Object> metadata = extension.getMetadata();
        List<ArtifactKey> result = new ArrayList<>();

        if (metadata.get("extension-dependencies") instanceof List extensionDependencies) {
            for (Object extensionDependency : extensionDependencies) {
                if (extensionDependency instanceof String artifactCoords) {
                    ArtifactKey k = ArtifactKey.fromString(artifactCoords);
                    result.add(k);
                }
            }
        }
        return result;
    }

    private String stripCliSuffix(String s) {
        Matcher m = CLI_SUFFIX.matcher(s);
        if (m.find()) {
            String replacement = m.group(2);
            replacement = replacement != null ? replacement : "";
            return m.replaceAll(replacement);
        }
        return s;
    }
}
