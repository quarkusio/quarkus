package io.quarkus.liquibase.runtime.produi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.liquibase.runtime.dev.ui.LiquibaseFactoriesSupplier;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;
import liquibase.Liquibase;
import liquibase.changelog.ChangeSetStatus;

/**
 * Read-only Prod UI view of the Liquibase changeset status and history. For each
 * datasource it exposes the changesets with their id/author/file, description,
 * applied-or-pending status, last-executed date and checksum, obtained from
 * {@code liquibase.getChangeSetStatuses(...)}. It is built on the always-present
 * {@link LiquibaseFactory} beans and performs no destructive action (no
 * update/rollback/clear) and exposes no database credentials. The Dev UI
 * component is entirely action-oriented (clear/migrate), so this is a bespoke
 * read-only view.
 */
@ApplicationScoped
public class LiquibaseProdUIService {

    private static final Logger LOG = Logger.getLogger(LiquibaseProdUIService.class);

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the Liquibase changeset status and history per datasource")
    public List<DatasourceChangeSets> getChangeSets() {
        List<DatasourceChangeSets> result = new ArrayList<>();
        Collection<LiquibaseFactory> factories = new LiquibaseFactoriesSupplier().get();
        for (LiquibaseFactory factory : factories) {
            result.add(describe(factory));
        }
        return result;
    }

    /**
     * Best-effort per-datasource status. Reading the changeset statuses requires
     * a live database connection; any failure is logged and surfaced as an error
     * string so the rest of the view still renders.
     */
    private DatasourceChangeSets describe(LiquibaseFactory factory) {
        String name = factory.getDataSourceName();
        List<ChangeSetEntry> changeSets = new ArrayList<>();
        String error = null;
        try (Liquibase liquibase = factory.createLiquibase()) {
            for (ChangeSetStatus status : liquibase.getChangeSetStatuses(factory.createContexts(), factory.createLabels())) {
                changeSets.add(toEntry(status));
            }
        } catch (Exception e) {
            error = e.getMessage();
            LOG.debugf(e, "Unable to read Liquibase changeset status for datasource '%s'", name);
        }
        return new DatasourceChangeSets(name, changeSets, error);
    }

    private ChangeSetEntry toEntry(ChangeSetStatus status) {
        return new ChangeSetEntry(
                status.getChangeSet().getId(),
                status.getChangeSet().getAuthor(),
                status.getChangeSet().getFilePath(),
                status.getDescription(),
                asStatus(status),
                status.getDateLastExecuted() == null ? null : status.getDateLastExecuted().toInstant().toString(),
                status.getStoredCheckSum() == null ? null : status.getStoredCheckSum().toString());
    }

    private static String asStatus(ChangeSetStatus status) {
        if (status.getPreviouslyRan()) {
            return status.getWillRun() ? "Applied (will re-run)" : "Applied";
        }
        return status.getWillRun() ? "Pending" : "-";
    }

    public record ChangeSetEntry(String id, String author, String filePath, String description, String status,
            String dateLastExecuted, String checksum) {
    }

    public record DatasourceChangeSets(String name, List<ChangeSetEntry> changeSets, String error) {
    }
}
