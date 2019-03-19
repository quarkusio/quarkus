package io.quarkus.flyway.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.flywaydb.core.internal.clazz.ClassProvider;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.resolver.java.JavaMigrationResolver;
import org.flywaydb.core.internal.resolver.jdbc.JdbcMigrationResolver;
import org.flywaydb.core.internal.resolver.sql.SqlMigrationResolver;
import org.flywaydb.core.internal.resource.ResourceProvider;
import org.flywaydb.core.internal.sqlscript.SqlStatementBuilderFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * @author cristhiank on 2019-03-19
 **/
@TargetClass(className = "org.flywaydb.core.internal.resolver.CompositeMigrationResolver")
public final class CompositeMigrationResolverSubstitutions {
    @Alias
    private Collection<MigrationResolver> migrationResolvers = new ArrayList<>();

    @Substitute
    public CompositeMigrationResolverSubstitutions(
            Database database,
            ResourceProvider resourceProvider,
            ClassProvider classProvider,
            Configuration configuration,
            SqlStatementBuilderFactory sqlStatementBuilderFactory,
            MigrationResolver... customMigrationResolvers) {
        if (!configuration.isSkipDefaultResolvers()) {
            migrationResolvers.add(new SqlMigrationResolver(
                    database,
                    resourceProvider,
                    sqlStatementBuilderFactory,
                    configuration));
            migrationResolvers.add(new JavaMigrationResolver(classProvider, configuration));
            migrationResolvers.add(new JdbcMigrationResolver(classProvider, configuration));
        }
        migrationResolvers.addAll(Arrays.asList(customMigrationResolvers));
    }
}
