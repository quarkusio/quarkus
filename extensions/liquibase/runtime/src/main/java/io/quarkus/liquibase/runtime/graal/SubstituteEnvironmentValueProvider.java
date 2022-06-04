package io.quarkus.liquibase.runtime.graal;

import java.util.Map;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "liquibase.configuration.pro.EnvironmentValueProvider")
final class SubstituteEnvironmentValueProvider {

    @Delete
    private Map<String, String> environment;

    @Substitute
    protected Map<?, ?> getMap() {
        return System.getenv();
    }

}
