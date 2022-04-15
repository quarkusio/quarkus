package io.quarkus.flyway.runtime.graal;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.api.output.CleanResult;
import org.flywaydb.core.internal.callback.CallbackExecutor;
import org.flywaydb.core.internal.command.DbClean;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.database.base.Schema;
import org.flywaydb.core.internal.schemahistory.SchemaHistory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Avoid using reflection
 */
@TargetClass(Flyway.class)
public final class FlywaySubstitutions {

    @Alias
    private ClassicConfiguration configuration;

    @Substitute
    private CleanResult doClean(
            Database database, SchemaHistory schemaHistory, Schema defaultSchema, Schema[] schemas,
            CallbackExecutor callbackExecutor) {
        return new DbClean(database, schemaHistory, defaultSchema, schemas, callbackExecutor, configuration).clean();
    }
}
