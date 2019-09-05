package io.quarkus.flyway.runtime.graal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.flywaydb.core.internal.clazz.ClassProvider;
import org.flywaydb.core.internal.resolver.java.FixedJavaMigrationResolver;
import org.flywaydb.core.internal.resolver.java.ScanningJavaMigrationResolver;
import org.flywaydb.core.internal.resolver.sql.SqlMigrationResolver;
import org.flywaydb.core.internal.resource.ResourceProvider;
import org.flywaydb.core.internal.sqlscript.SqlScriptExecutorFactory;
import org.flywaydb.core.internal.sqlscript.SqlScriptFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.flywaydb.core.internal.resolver.CompositeMigrationResolver")
public final class CompositeMigrationResolverSubstitutions {
    @Alias
    private Collection<MigrationResolver> migrationResolvers = new ArrayList<>();

    /**
     * Substitution to remove Spring-data migration resolver as the spring-data dependency is optional.
     * This method removes the inclusion of {@link org.flywaydb.core.internal.resolver.spring.SpringJdbcMigrationResolver}
     * in the resolvers list to avoid native image errors because of the incomplete classpath
     *
     * @see org.flywaydb.core.internal.resolver.spring.SpringJdbcMigrationResolver
     * @see org.flywaydb.core.internal.resolver.CompositeMigrationResolver#CompositeMigrationResolver(ResourceProvider,
     *      ClassProvider, Configuration, SqlScriptExecutorFactory, SqlScriptFactory, MigrationResolver...)
     */
    @Substitute
    public CompositeMigrationResolverSubstitutions(ResourceProvider resourceProvider,
            ClassProvider classProvider,
            Configuration configuration,
            SqlScriptExecutorFactory sqlScriptExecutorFactory,
            SqlScriptFactory sqlScriptFactory,
            MigrationResolver... customMigrationResolvers) {
        if (!configuration.isSkipDefaultResolvers()) {
            migrationResolvers.add(new SqlMigrationResolver(resourceProvider, sqlScriptExecutorFactory, sqlScriptFactory,
                    configuration));
            migrationResolvers.add(new ScanningJavaMigrationResolver(classProvider, configuration));
        }
        migrationResolvers.add(new FixedJavaMigrationResolver(configuration.getJavaMigrations()));

        migrationResolvers.addAll(Arrays.asList(customMigrationResolvers));
    }

}
