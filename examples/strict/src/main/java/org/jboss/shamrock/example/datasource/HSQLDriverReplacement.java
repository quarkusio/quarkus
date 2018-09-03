package org.jboss.shamrock.example.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.hsqldb.DatabaseURL;
import org.hsqldb.error.ErrorCode;
import org.hsqldb.jdbc.JDBCConnection;
import org.hsqldb.jdbc.JDBCDriver;
import org.hsqldb.jdbc.JDBCUtil;
import org.hsqldb.persist.HsqlProperties;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(JDBCDriver.class)
final class HSQLDriverReplacement {


    @Substitute
    public static Connection getConnection(String url,
                                           Properties info) throws SQLException {

        final HsqlProperties props = DatabaseURL.parseURL(url, true, false);

        if (props == null) {

            // supposed to be an HSQLDB driver url but has errors
            throw JDBCUtil.invalidArgument();
        } else if (props.isEmpty()) {

            // is not an HSQLDB driver url
            return null;
        }

        long timeout = 0;

        if (info != null) {
            timeout = HsqlProperties.getIntegerProperty(info, "loginTimeout", 0);
        }

        props.addProperties(info);

        if (timeout == 0) {
            timeout = DriverManager.getLoginTimeout();
        }

        // @todo:  maybe impose some sort of sane restriction
        //         on network connections regardless of user
        //         specification?
        if (timeout == 0) {

            // no timeout restriction
            return new JDBCConnection(props);
        }

        String connType = props.getProperty("connection_type");

        if (DatabaseURL.isInProcessDatabaseType(connType)) {
            return new JDBCConnection(props);
        }

        // @todo: Better: ThreadPool? HsqlTimer with callback?
        final JDBCConnection[] conn = new JDBCConnection[1];
        final SQLException[]   ex   = new SQLException[1];
        Thread                 t    = new Thread() {

            public void run() {

                try {
                    conn[0] = new JDBCConnection(props);
                } catch (SQLException se) {
                    ex[0] = se;
                }
            }
        };

        t.start();

        try {
            t.join(1000 * timeout);
        } catch (InterruptedException ie) {
        }

        try {

            // PRE:
            // deprecated, but should be ok, since neither
            // the HSQLClientConnection or the HTTPClientConnection
            // constructor will ever hold monitors on objects in
            // an inconsistent state, such that damaged objects
            // become visible to other threads with the
            // potential of arbitrary behavior.
            //t.stop();
        } catch (Exception e) {
        } finally {
            try {
                t.setContextClassLoader(null);
            } catch (Throwable th) {
            }
        }

        if (ex[0] != null) {
            throw ex[0];
        }

        if (conn[0] != null) {
            return conn[0];
        }

        throw JDBCUtil.sqlException(ErrorCode.X_08501);
    }
}
