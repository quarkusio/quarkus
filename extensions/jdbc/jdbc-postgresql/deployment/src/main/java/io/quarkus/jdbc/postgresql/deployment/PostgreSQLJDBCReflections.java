package io.quarkus.jdbc.postgresql.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

/**
 * Registers the {@code org.postgresql.Driver} so that it can be loaded
 * by reflection, as commonly expected.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class PostgreSQLJDBCReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of other users.
        final String driverName = "org.postgresql.Driver";
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(driverName).build());

        // We want to register these postgresql "object" types since they are used by a driver to build result set elements
        // and reflection is used to create their instances. While ORM might only use a `PGInterval` if a @JdbcType(PostgreSQLIntervalSecondJdbcType.class)
        // is applied to a Duration property, we still register other types as users might create their own JdbcTypes that
        // would rely on some subtype of a PGobject:
        final String[] pgObjectClasses = new String[] {
                "org.postgresql.util.PGobject",
                "org.postgresql.util.PGInterval",
                "org.postgresql.util.PGmoney",
                "org.postgresql.geometric.PGbox",
                "org.postgresql.geometric.PGcircle",
                "org.postgresql.geometric.PGline",
                "org.postgresql.geometric.PGlseg",
                "org.postgresql.geometric.PGpath",
                "org.postgresql.geometric.PGpoint",
                "org.postgresql.geometric.PGpolygon",
                // One more subtype of the PGobject, it doesn't look like that this one will be instantiated through reflection,
                // so let's not include it:
                // "org.postgresql.jdbc.PgResultSet.NullObject"
        };
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(pgObjectClasses).reason(getClass().getName()).build());

        // Needed when quarkus.datasource.jdbc.transactions=xa for the setting of the username and password
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("org.postgresql.ds.common.BaseDataSource").constructors(false)
                .methods().build());
    }

}
