package io.quarkus.liquibase.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import liquibase.command.CommandResultsBuilder;

@TargetClass(className = "liquibase.command.core.StartH2CommandStep")
final class SubstituteStartH2CommandStep {

    @Substitute
    public void run(CommandResultsBuilder builder) {
        throw new UnsupportedOperationException("Starting h2 is not supported by quarkus-liquibase");
    }
}
