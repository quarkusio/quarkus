package io.quarkus.cli.plugin;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class PluginCatalogService extends CatalogService<PluginCatalog> {

    static final Function<Path, Path> RELATIVE_CATALOG_JSON = p -> p.resolve(".quarkus").resolve("cli")
            .resolve("plugins").resolve("quarkus-cli-catalog.json");

    public PluginCatalogService() {
        this(GIT_ROOT, RELATIVE_CATALOG_JSON);
    }

    public PluginCatalogService(Function<Path, Path> relativePath) {
        this(GIT_ROOT, relativePath);
    }

    public PluginCatalogService(Predicate<Path> projectRoot, Function<Path, Path> relativePath) {
        super(PluginCatalog.class, projectRoot, relativePath);
    }

    @Override
    public Optional<PluginCatalog> readUserCatalog(Optional<Path> userDir) {
        return super.readUserCatalog(userDir).map(u -> u.withCatalogLocation(userDir.map(RELATIVE_CATALOG_JSON)));
    }

    @Override
    public Optional<PluginCatalog> readProjectCatalog(Optional<Path> dir) {
        return super.readProjectCatalog(dir).map(p -> p.withCatalogLocation(dir.map(RELATIVE_CATALOG_JSON)));
    }

    /**
     * Read the {@link PluginCatalog} from project or fallback to global catalog.
     *
     * @param output an {@link OutputOptionMixin} that can be used for tests to substitute current dir with a test directory.
     * @param projectDir An optional path pointing to the project directory.
     * @param userdir An optional path pointing to the user directory
     * @return the catalog
     */
    public PluginCatalog readCombinedCatalog(Optional<Path> proejctDir, Optional<Path> userDir) {
        Map<String, Plugin> plugins = new HashMap<>();

        Optional<PluginCatalog> projectCatalog = readProjectCatalog(proejctDir);
        Optional<PluginCatalog> userCatalog = readUserCatalog(userDir);

        userCatalog.ifPresent(u -> {
            plugins.putAll(u.getPlugins());
        });

        projectCatalog.ifPresent(p -> {
            plugins.putAll(p.getPlugins());
        });

        LocalDateTime userCatalogTime = userCatalog.map(PluginCatalog::getLastUpdateDate).orElse(LocalDateTime.now());
        LocalDateTime projectCatalogTime = projectCatalog.map(PluginCatalog::getLastUpdateDate).orElse(LocalDateTime.now());
        LocalDateTime oldest = userCatalogTime.isBefore(projectCatalogTime) ? userCatalogTime : projectCatalogTime;
        return new PluginCatalog(PluginCatalog.VERSION, oldest, plugins, Optional.empty());
    }
}
