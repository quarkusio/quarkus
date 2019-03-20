/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
     * @see org.flywaydb.core.internal.resolver.CompositeMigrationResolver#CompositeMigrationResolver(Database,
     *      ResourceProvider, ClassProvider, Configuration, SqlStatementBuilderFactory, MigrationResolver...)
     */
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
