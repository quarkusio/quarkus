package io.quarkus.jdbc.postgresql.runtime.graal;

import java.util.Properties;

import org.postgresql.Driver;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(Driver.class)
public final class DriverSubstitutions {

    @Substitute
    private void setupLoggerFromProperties(final Properties props) {
        //We don't want it to mess with the logger config
    }

}
