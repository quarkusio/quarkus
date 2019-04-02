/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.flyway.runtime.graal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.flywaydb.core.internal.line.Line;
import org.flywaydb.core.internal.sqlscript.Delimiter;
import org.flywaydb.core.internal.sqlscript.SqlStatement;
import org.flywaydb.core.internal.sqlscript.StandardSqlStatement;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This substitution removes de PosgreSQL COPY statement support in Flyway to allow native image compilation
 * when the PostgreSQL Driver is not in the classpath.
 */
@TargetClass(className = "org.flywaydb.core.internal.database.postgresql.PostgreSQLSqlStatementBuilder", onlyWith = PostgreSQLSqlStatementBuilderSubstitutions.Selector.class)
public final class PostgreSQLSqlStatementBuilderSubstitutions {
    @Alias
    protected List<Line> lines = new ArrayList<>();
    @Alias
    protected Delimiter delimiter;
    @Alias
    private boolean pgCopy;

    /**
     * Returns only a {@link StandardSqlStatement} as the normal SQL migrations do or throw an exception
     * if pgCopy flag is true
     */
    @Substitute
    public SqlStatement getSqlStatement() {
        if (pgCopy) {
            throw new IllegalStateException("pgCopy is not supported yet!");
        }
        return new StandardSqlStatement(lines, delimiter);
    }

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("org.postgresql.Driver");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }
}
