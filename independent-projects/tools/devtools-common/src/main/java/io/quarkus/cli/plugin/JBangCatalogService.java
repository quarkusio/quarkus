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

    private final MessageWriter output;
    private final String pluginPrefix;
    private final String remoteCatalog;

    public JBangCatalogService(MessageWriter output) {
        this(output, "quarkus", "quarkusio");
    }

    public JBangCatalogService(MessageWriter output, String pluginPrefix, String remoteCatalog) {
        super(JBangCatalog.class, GIT_ROOT, RELATIVE_PLUGIN_CATALOG);
        this.output = output;
        this.pluginPrefix = pluginPrefix;
        this.remoteCatalog = remoteCatalog;
    }

    @Override
    public JBangCatalog readCatalog(Path path) {
        JBangCatalog catalog = super.readCatalog(path);
        return new JBangCatalog(catalog.getCatalogs(),
                catalog.getAliases().entrySet().stream().filter(e -> e.getKey().startsWith(pluginPrefix + "-"))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())),
                catalog.getCatalogRef(), catalog.getCatalogLocation());
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
        Map<String, JBangCatalog> catalogs = new HashMap<>();
        Map<String, JBangAlias> aliases = new HashMap<>();

        Optional<JBangCatalog> projectCatalog = readProjectCatalog(projectDir);
        Optional<JBangCatalog> userCatalog = readUserCatalog(userDir);

        JBangSupport jbang = new JBangSupport(output);

        userCatalog.ifPresent(u -> {
            //Read local
            aliases.putAll(u.getAliases());
            List<String> lines = jbang.execute("alias", "list", "--verbose", remoteCatalog);
            aliases.putAll(readAliases(lines));

        });

        projectCatalog.ifPresent(p -> {
            //Read local
            aliases.putAll(p.getAliases());

            Optional<String> catalogFile = projectDir.map(d -> RELATIVE_PLUGIN_CATALOG.apply(d).toAbsolutePath().toString());
            catalogFile.ifPresent(f -> {
                List<String> lines = jbang.execute("alias", "list", "-f", f, "--verbose", remoteCatalog);
                aliases.putAll(readAliases(lines));
            });
        });
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
