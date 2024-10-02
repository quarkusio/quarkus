package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.util.ArrayList;
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
    private final String fallbackCatalog;
    private final String[] remoteCatalogs;
    private final JBangSupport jbang;
    private final MessageWriter output;

    public JBangCatalogService(MessageWriter output) {
        this(output, "quarkus-", "quarkusio");
    }

    public JBangCatalogService(MessageWriter output, String pluginPrefix, String fallbackCatalog, String... remoteCatalogs) {
        this(false, output, pluginPrefix, fallbackCatalog, remoteCatalogs);
    }

    public JBangCatalogService(boolean interactiveMode, MessageWriter output, String pluginPrefix, String fallbackCatalog,
            String... remoteCatalogs) {
        super(JBangCatalog.class, GIT_ROOT, RELATIVE_PLUGIN_CATALOG);
        this.pluginPrefix = pluginPrefix;
        this.fallbackCatalog = fallbackCatalog;
        this.remoteCatalogs = remoteCatalogs;
        this.jbang = new JBangSupport(interactiveMode, output);
        this.output = output;
    }

    public boolean ensureJBangIsInstalled() {
        return jbang.ensureJBangIsInstalled();
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
     * @param output an {@link OutputOptionMixin} that can be used for tests to
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
                List<String> lines = new ArrayList<>();
                try {
                    lines.addAll(jbang.execute("alias", "list", "-f", f, "--verbose"));
                } catch (Exception e) {
                    output.debug("Failed to read catalog file: " + f + ". Ignoring.");
                }
                aliases.putAll(readAliases(lines));
            });
        });

        if (!jbang.isAvailable()) {
            //If jbang is not available, ignore aliases
        } else if (remoteCatalogs.length == 0) { //If not catalog have been specified use all available.
            aliases.putAll(listAliasesOrFallback(jbang, fallbackCatalog).entrySet()
                    .stream()
                    .filter(e -> !aliases.containsKey(e.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        } else {
            for (String remoteCatalog : remoteCatalogs) {
                aliases.putAll(listAliases(jbang, remoteCatalog).entrySet()
                        .stream()
                        .filter(e -> !aliases.containsKey(e.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            }
        }
        return new JBangCatalog(catalogs, aliases, Optional.empty(), Optional.empty());
    }

    private Map<String, JBangAlias> listAliases(JBangSupport jbang, String remoteCatalog) {
        List<String> lines = new ArrayList<>();
        try {
            lines.addAll(jbang.execute("alias", "list", "--verbose", remoteCatalog));
        } catch (Exception e) {
            this.output.debug("Failed to list aliases from remote catalog: " + remoteCatalog + ". Ignorning.");
        }

        return readAliases(lines);
    }

    private Map<String, JBangAlias> listAliasesOrFallback(JBangSupport jbang, String fallbackCatalog) {
        List<String> localCatalogs = new ArrayList<>();
        try {
            for (String catalog : jbang.execute("catalog", "list")) {
                localCatalogs.add(catalog.substring(0, catalog.indexOf(" ")));
            }
        } catch (Exception e) {
            this.output.debug("Failed to list jbang catalogs. Ignoring.");
        }

        //If there are locally installed catalogs, then go through every single one of them
        //and collect the aliases.
        //Unfortunaltely jbang can't return all alias in one go.
        //This is because it currently omits `@catalog` suffix in some cases.
        if (!localCatalogs.isEmpty()) {
            Map<String, JBangAlias> aliases = new HashMap<>();
            for (String catalog : localCatalogs) {
                aliases.putAll(listAliases(jbang, catalog));
            }
            return aliases;
        }
        //If no aliases found then there is not remote jbang catalog available
        //In this case we need to fallback to the default.
        return listAliases(jbang, fallbackCatalog);
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
