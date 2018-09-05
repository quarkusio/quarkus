package org.jboss.shamrock.example.datasource;

import java.util.Vector;

import org.hsqldb.Database;
import org.hsqldb.DatabaseManager;
import org.hsqldb.DatabaseType;
import org.hsqldb.Session;
import org.hsqldb.lib.Notified;
import org.hsqldb.persist.HsqlProperties;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(DatabaseManager.class)
final class DatabaseManagerReplacement {

    /**
     * Returns a vector containing the URI (type + path) for all the databases.
     */
    @Substitute
    public static Vector getDatabaseURIs() {
        throw new RuntimeException();
    }

    /**
     * Closes all the databases using the given mode.<p>
     * <p>
     * CLOSEMODE_IMMEDIATELY = 1;
     * CLOSEMODE_NORMAL      = 2;
     * CLOSEMODE_COMPACT     = 3;
     * CLOSEMODE_SCRIPT      = 4;
     */
    @Substitute
    public static void closeDatabases(int mode) {
        throw new RuntimeException();
    }

    /**
     * Used by server to open a new session
     */
    @Substitute
    public static Session newSession(int dbID, String user, String password,
                                     String zoneString, int timeZoneSeconds) {
        throw new RuntimeException();
    }

    /**
     * Used by in-process connections and by Servlet
     */
    @Substitute
    public static Session newSession(String type, String path, String user,
                                     String password, HsqlProperties props,
                                     String zoneString, int timeZoneSeconds) {
        throw new RuntimeException();
    }

    /**
     * Returns an existing session. Used with repeat HTTP connections
     * belonging to the same JDBC Connection / HSQL Session pair.
     */
    @Substitute
    public static Session getSession(int dbId, long sessionId) {
        throw new RuntimeException();
    }

    /**
     * Used by server to open or create a database
     */
    @Substitute
    public static int getDatabase(String type, String path, Notified server,
                                  HsqlProperties props) {
        throw new RuntimeException();
    }

    @Substitute
    public static Database getDatabase(int id) {
        throw new RuntimeException();
    }

    @Substitute
    public static void shutdownDatabases(Notified server, int shutdownMode) {
        throw new RuntimeException();
    }

    /**
     * This has to be improved once a threading model is in place.
     * Current behaviour:
     * <p>
     * Attempts to connect to different databases do not block. Two db's can
     * open simultaneously.
     * <p>
     * Attempts to connect to a db while it is opening or closing will block
     * until the db is open or closed. At this point the db state is either
     * DATABASE_ONLINE (after db.open() has returned) which allows a new
     * connection to be made, or the state is DATABASE_SHUTDOWN which means
     * the db can be reopened for the new connection).
     */
    @Substitute
    public static Database getDatabase(String dbtype, String path,
                                       HsqlProperties props) {
        throw new RuntimeException();
    }


    /**
     * Looks up database of a given type and path in the registry. Returns
     * null if there is none.
     */
    @Substitute
    public static synchronized Database lookupDatabaseObject(DatabaseType type,
                                                             String path) {
        throw new RuntimeException();
    }

    /**
     * Deregisters a server completely.
     */
    @Substitute
    public static void deRegisterServer(Notified server) {
        throw new RuntimeException();
    }
}
