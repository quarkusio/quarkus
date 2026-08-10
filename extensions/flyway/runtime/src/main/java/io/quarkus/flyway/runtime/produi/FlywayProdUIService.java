package io.quarkus.flyway.runtime.produi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.jboss.logging.Logger;

import io.quarkus.flyway.runtime.FlywayContainer;
import io.quarkus.flyway.runtime.FlywayContainersSupplier;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.runtime.annotations.JsonRpcUsage;
import io.quarkus.runtime.annotations.Usage;

/**
 * Read-only Prod UI view of the Flyway migration history. For each datasource it
 * exposes the applied and pending migrations (version, description, type, state,
 * install time, checksum, execution time) obtained from
 * {@code flyway.info().all()}. It is built on the always-present
 * {@link FlywayContainer} beans and performs no destructive action (no
 * migrate/clean/repair) and exposes no database credentials. The Dev UI
 * component is entirely action-oriented (migrate/clean/create), so this is a
 * bespoke read-only view.
 */
@ApplicationScoped
public class FlywayProdUIService {

    private static final Logger LOG = Logger.getLogger(FlywayProdUIService.class);

    @JsonRpcUsage(Usage.PROD_UI)
    @JsonRpcDescription("Get a read-only view of the Flyway migration history per datasource")
    public List<DatasourceMigrations> getMigrations() {
        List<DatasourceMigrations> result = new ArrayList<>();
        Collection<FlywayContainer> containers = new FlywayContainersSupplier().get();
        for (FlywayContainer container : containers) {
            result.add(describe(container));
        }
        result.sort((a, b) -> a.name().compareTo(b.name()));
        return result;
    }

    /**
     * Best-effort per-datasource history. Reading the schema history requires a
     * live database connection; any failure is logged and surfaced as an error
     * string so the rest of the view still renders.
     */
    private DatasourceMigrations describe(FlywayContainer container) {
        String name = container.getDataSourceName();
        List<MigrationEntry> migrations = new ArrayList<>();
        String error = null;
        try {
            Flyway flyway = container.getFlyway();
            for (MigrationInfo info : flyway.info().all()) {
                migrations.add(toEntry(info));
            }
        } catch (RuntimeException e) {
            error = e.getMessage();
            LOG.debugf(e, "Unable to read Flyway migration info for datasource '%s'", name);
        }
        return new DatasourceMigrations(name, migrations, error);
    }

    private MigrationEntry toEntry(MigrationInfo info) {
        return new MigrationEntry(
                info.getVersion() == null ? "" : info.getVersion().toString(),
                info.getDescription(),
                String.valueOf(info.getType()),
                info.getState() == null ? "" : info.getState().getDisplayName(),
                info.getInstalledOn() == null ? null : info.getInstalledOn().toInstant().toString(),
                info.getChecksum(),
                info.getExecutionTime(),
                info.getScript());
    }

    public record MigrationEntry(String version, String description, String type, String state, String installedOn,
            Integer checksum, Integer executionTime, String script) {
    }

    public record DatasourceMigrations(String name, List<MigrationEntry> migrations, String error) {
    }
}
