package io.quarkus.liquibase.common;

import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import liquibase.change.Change;
import liquibase.change.core.CreateProcedureChange;
import liquibase.change.core.CreateViewChange;
import liquibase.change.core.LoadDataChange;
import liquibase.change.core.SQLFileChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;

/**
 * Collects classpath resource paths and logical↔physical aliases for Liquibase changelogs.
 * <p>
 * When {@code logicalFilePath} is set, {@link DatabaseChangeLog#getFilePath()} returns the logical
 * path while the file on the classpath keeps its physical path. Native image registration must
 * account for both.
 */
public final class LiquibaseChangeLogResourceDiscovery {

    private LiquibaseChangeLogResourceDiscovery() {
    }

    /**
     * @param logical normalized logical path (as Liquibase resolves it for resource lookup)
     * @param physical normalized physical classpath path of the same changelog file
     */
    public record LogicalPhysicalAlias(String logical, String physical) {
    }

    public record ScanResult(Set<String> resourcePaths, Set<LogicalPhysicalAlias> logicalPhysicalAliases) {
    }

    public static ScanResult scan(DatabaseChangeLog changelog) {
        LinkedHashSet<String> resources = new LinkedHashSet<>();
        LinkedHashSet<LogicalPhysicalAlias> aliases = new LinkedHashSet<>();
        for (ChangeSet changeSet : changelog.getChangeSets()) {
            addChangeSetResources(changeSet, resources, aliases);
            changeSet.getChanges().stream()
                    .map(change -> extractChangeFile(change, changeSet.getFilePath()))
                    .forEach(path -> path.ifPresent(p -> resources.add(DatabaseChangeLog.normalizePath(p))));

            DatabaseChangeLog parent = changeSet.getChangeLog();
            while (parent != null) {
                addDatabaseChangeLog(parent, resources, aliases);
                parent = parent.getParentChangeLog();
            }
        }
        addDatabaseChangeLog(changelog, resources, aliases);
        return new ScanResult(resources, aliases);
    }

    private static void addChangeSetResources(ChangeSet changeSet, Set<String> resources,
            Set<LogicalPhysicalAlias> aliases) {
        String changeSetFile = normalizeNullable(changeSet.getFilePath());
        DatabaseChangeLog owningLog = changeSet.getChangeLog();
        String physical = owningLog != null ? normalizeNullable(owningLog.getPhysicalFilePath()) : null;
        if (changeSetFile != null) {
            resources.add(changeSetFile);
        }
        if (physical != null) {
            resources.add(physical);
        }
        maybeAddAlias(aliases, changeSetFile, physical);
    }

    private static void addDatabaseChangeLog(DatabaseChangeLog log, Set<String> resources,
            Set<LogicalPhysicalAlias> aliases) {
        if (log == null) {
            return;
        }
        String physical = normalizeNullable(log.getPhysicalFilePath());
        String logical = normalizeNullable(log.getFilePath());
        if (physical != null) {
            resources.add(physical);
        }
        if (logical != null) {
            resources.add(logical);
        }
        maybeAddAlias(aliases, logical, physical);
    }

    private static void maybeAddAlias(Set<LogicalPhysicalAlias> aliases, String logical, String physical) {
        if (logical != null && physical != null && !logical.equals(physical)) {
            aliases.add(new LogicalPhysicalAlias(logical, physical));
        }
    }

    private static String normalizeNullable(String path) {
        if (path == null) {
            return null;
        }
        return DatabaseChangeLog.normalizePath(path);
    }

    private static Optional<String> extractChangeFile(Change change, String changeSetFilePath) {
        String path = null;
        Boolean relative = null;
        if (change instanceof LoadDataChange loadDataChange) {
            path = loadDataChange.getFile();
            relative = loadDataChange.isRelativeToChangelogFile();
        } else if (change instanceof SQLFileChange sqlFileChange) {
            path = sqlFileChange.getPath();
            relative = sqlFileChange.isRelativeToChangelogFile();
        } else if (change instanceof CreateProcedureChange createProcedureChange) {
            path = createProcedureChange.getPath();
            relative = createProcedureChange.isRelativeToChangelogFile();
        } else if (change instanceof CreateViewChange createViewChange) {
            path = createViewChange.getPath();
            relative = createViewChange.getRelativeToChangelogFile();
        }

        if (path == null) {
            return Optional.empty();
        }
        if (relative == null || !relative || changeSetFilePath == null) {
            return Optional.of(path);
        }

        return Optional.of(Paths.get(changeSetFilePath).resolveSibling(path).toString().replace('\\', '/'));
    }
}
