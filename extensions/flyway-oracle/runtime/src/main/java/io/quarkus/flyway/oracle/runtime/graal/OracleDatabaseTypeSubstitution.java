package io.quarkus.flyway.oracle.runtime.graal;

import java.sql.Connection;
import java.util.Properties;

import org.flywaydb.core.api.configuration.Configuration;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(org.flywaydb.database.oracle.OracleDatabaseType.class)
public final class OracleDatabaseTypeSubstitution {

    @Substitute
    public Connection alterConnectionAsNeeded(Connection connection, Configuration configuration) {
        return connection;
    }

    @Substitute
    public void setDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        String osUser = System.getProperty("user.name");
        props.put("v$session.osuser", osUser.substring(0, Math.min(osUser.length(), 30)));
        props.put("v$session.program", "Flyway by Redgate");
        props.put("oracle.net.keepAlive", "true");
        props.put("oracle.net.disableOob", "true");
    }
}
