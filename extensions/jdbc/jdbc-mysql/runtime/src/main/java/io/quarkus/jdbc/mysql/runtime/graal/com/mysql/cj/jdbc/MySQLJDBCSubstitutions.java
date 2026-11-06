package io.quarkus.jdbc.mysql.runtime.graal.com.mysql.cj.jdbc;

import java.sql.SQLException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import com.mysql.cj.Messages;
import com.mysql.cj.exceptions.ExceptionFactory;
import com.mysql.cj.telemetry.TelemetryHandler;
import com.mysql.cj.telemetry.TelemetrySpan;
import com.mysql.cj.telemetry.TelemetrySpanName;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.mysql.cj.protocol.a.authentication.AuthenticationOciClient")
final class AuthenticationOciClient {

    @Substitute
    private void loadOciConfig() {
        throw ExceptionFactory
                .createException("OciClient authentication is not available in Quarkus when compiling to native-image:" +
                        " the MySQL JDBC driver team needs to cleanup the dependency requirements to make this possible." +
                        " If you need this resolved, please open a support request.");
    }

    @Substitute
    private void initializePrivateKey() {
        throw ExceptionFactory
                .createException("OciClient authentication is not available in Quarkus when compiling to native-image:" +
                        " the MySQL JDBC driver team needs to cleanup the dependency requirements to make this possible." +
                        " If you need this resolved, please open a support request.");
    }

}

@TargetClass(className = "com.mysql.cj.jdbc.ConnectionGroupManager")
final class ConnectionGroupManager {

    @Substitute
    public static void registerJmx() throws SQLException {
        throw new IllegalStateException("Not Implemented in native mode");
    }

}

@TargetClass(className = "com.mysql.cj.jdbc.jmx.LoadBalanceConnectionGroupManager")
final class LoadBalanceConnectionGroupManager {

    @Substitute
    public synchronized void registerJmx() throws java.sql.SQLException {
        throw new IllegalStateException("Not Implemented in native mode");
    }

}

@TargetClass(className = "com.mysql.cj.jdbc.jmx.ReplicationGroupManager")
final class ReplicationGroupManager {

    @Substitute
    public synchronized void registerJmx() throws SQLException {
        throw new IllegalStateException("Not Implemented in native mode");
    }

}

@TargetClass(className = "com.mysql.cj.jdbc.ha.ReplicationConnectionGroupManager")
final class ReplicationConnectionGroupManager {

    @Substitute
    public static void registerJmx() throws SQLException {
        throw new IllegalStateException("Not Implemented in native mode");
    }

}

@TargetClass(className = "com.mysql.cj.otel.OpenTelemetryHandler", onlyWith = OpenTelemetryUnavailable.class)
final class OpenTelemetryHandler implements TelemetryHandler {

    @Substitute
    public OpenTelemetryHandler() {
        throw ExceptionFactory.createException(Messages.getString("Connection.OtelApiNotFound"));
    }

    @Override
    @Substitute
    public TelemetrySpan startSpan(TelemetrySpanName telemetrySpanName, Object... objects) {
        return null;
    }

    @Override
    @Substitute
    public void addLinkTarget(TelemetrySpan span) {
    }

    @Override
    @Substitute
    public void removeLinkTarget(TelemetrySpan span) {
    }

    @Override
    @Substitute
    public void propagateContext(BiConsumer<String, String> traceparentConsumer) {
    }
}

final class OpenTelemetryUnavailable implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("io.opentelemetry.api.GlobalOpenTelemetry");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}

class MySQLJDBCSubstitutions {
}
