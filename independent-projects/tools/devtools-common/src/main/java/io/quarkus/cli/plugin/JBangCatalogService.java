package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.quarkus.devtools.messagewriter.MessageWriter;

public class JBangCatalogService extends CatalogService<JBangCatalog> {

    private static final Function<Path, Path> RELATIVE_PLUGIN_CATALOG = p -> p.resolve(".jbang").resolve("jbang-catalog.json");
    private static final String PATH_REGEX = "^\\s*\\((?<path>.*)\\)\\s*$";
    private static final Pattern PATH = Pattern.compile(PATH_REGEX);

    private final String pluginPrefix;
    private final String[] remoteCatalogs;
    private final JBangSupport jbang;

    public JBangCatalogService(MessageWriter output) {
        this(output, "quarkus", "quarkusio");
    }

    public JBangCatalogService(MessageWriter output, String pluginPrefix, String... remoteCatalogs) {
        this(false, output, pluginPrefix, remoteCatalogs);
    }

    public JBangCatalogService(boolean interactiveMode, MessageWriter output, String pluginPrefix, String... remoteCatalogs) {
        super(JBangCatalog.class, GIT_ROOT, RELATIVE_PLUGIN_CATALOG);
        this.pluginPrefix = pluginPrefix;
        this.remoteCatalogs = remoteCatalogs;
        this.jbang = new JBangSupport(interactiveMode, output);
    }

    @Override
    public JBangCatalog readCatalog(Path path) {
        if (!jbang.isAvailable() && !jbang.isInstallable()) {
            // When jbang is not available / installable just return an empty catalog.
            // We don't even return the parsed one as plugins won't be able to run without jbang anyway.
            return new JBangCatalog();
        }

        JBangCatalog localCatalog = super.readCatalog(path);
        Map<String, JBangAlias> aliases = localCatalog.getAliases().entrySet().stream()
                .filter(e -> e.getKey().startsWith(pluginPrefix + "-"))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        return new JBangCatalog(localCatalog.getCatalogs(), aliases, localCatalog.getCatalogRef(),
                localCatalog.getCatalogLocation());
    }

    /**
     * Read the {@link JBangCatalog} from project or fallback to global catalog.
     *
     * @param ouput an {@link OutputOptionMixin} that can be used for tests to
     *        substitute current dir with a test directory.
     * @param projectDir An optional path pointing to the project directory.
     * @param userdir An optional path pointing to the user directory
     * @return the catalog
     */
    public JBangCatalog readCombinedCatalog(Optional<Path> projectDir, Optional<Path> userDir) {
        if (!jbang.isAvailable() && !jbang.isInstallable()) {
            // When jbang is not available / installable just return an empty catalog.
            // We don't even return the parsed one as plugins won't be able to run without jbang anyway.
            return new JBangCatalog();
        }

        Map<String, JBangCatalog> catalogs = new HashMap<>();
        Map<String, JBangAlias> aliases = new HashMap<>();

        Optional<JBangCatalog> projectCatalog = readProjectCatalog(projectDir);
        Optional<JBangCatalog> userCatalog = readUserCatalog(userDir);

        userCatalog.ifPresent(u -> {
            aliases.putAll(u.getAliases());

        });

        projectCatalog.ifPresent(p -> {
            aliases.putAll(p.getAliases());
            Optional<String> catalogFile = projectDir
                    .map(d -> RELATIVE_PLUGIN_CATALOG.apply(d).toAbsolutePath().toString());
            catalogFile.ifPresent(f -> {
                List<String> lines = jbang.execute("alias", "list", "-f", f, "--verbose");
                aliases.putAll(readAliases(lines));
            });
        });

        for (String remoteCatalog : remoteCatalogs) {
            List<String> lines = jbang.execute("alias", "list", "--verbose", remoteCatalog);
            aliases.putAll(readAliases(lines).entrySet()
                    .stream()
                    .filter(e -> !aliases.containsKey(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        return new JBangCatalog(catalogs, aliases, Optional.empty(), Optional.empty());
    }

    private Map<String, JBangAlias> readAliases(List<String> lines) {
        Map<String, JBangAlias> aliases = new HashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith(pluginPrefix + "")) {
                String name = aliasName(line);
                Optional<String> remote = aliasRemote(line);
                Optional<String> next = i + 1 < lines.size() ? Optional.of(lines.get(i + 1)) : Optional.empty();
                Optional<String> path = next.filter(n -> n.matches(PATH_REGEX)).flatMap(JBangCatalogService::aliasPath);
                Optional<String> description = path.filter(JBangCatalogService::hasDescription)
                        .map(JBangCatalogService::aliasDescription);
                JBangAlias alias = new JBangAlias(name, description, remote);
                aliases.put(name, alias);
            }
        }
        return aliases;
    }

    private static final String aliasName(String s) {
        return s.split("=")[0].trim();
    }

    private static final Optional<String> aliasRemote(String s) {
        if (s == null || s.isEmpty()) {
            return Optional.empty();
        }
        String nameWithRemote = s.split("=")[0].trim();
        if (!nameWithRemote.contains("@")) {
            return Optional.empty();
        }
        return Optional.of(nameWithRemote.split("@")[1].trim());
    }

    private static final boolean hasDescription(String s) {
        return s.contains("=");
    }

    private static final String aliasDescription(String s) {
        return s.split("=")[1].trim();
    }

    private static final Optional<String> aliasPath(String s) {
        Matcher m = PATH.matcher(s);
        if (m.matches()) {
            return Optional.of(m.group("path"));
        }
        return Optional.empty();
    }
}
