package io.quarkus.jdbc.mysql.runtime.graal.com.mysql.cj.jdbc;

import java.sql.SQLException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

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

class MySQLJDBCSubstitutions {
}
