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

/**
 * We don't want to suck all of HSQL into the native image, just the client driver
 * this substitution means that only the client part of the jar will be included
 */
@TargetClass(DatabaseManager.class)
final class DatabaseManagerReplacement {

    @Substitute
    public static Vector getDatabaseURIs() {
        throw new RuntimeException();
    }

    @Substitute
    public static void closeDatabases(int mode) {
        throw new RuntimeException();
    }

    @Substitute
    public static Session newSession(int dbID, String user, String password,
                                     String zoneString, int timeZoneSeconds) {
        throw new RuntimeException();
    }

    @Substitute
    public static Session newSession(String type, String path, String user,
                                     String password, HsqlProperties props,
                                     String zoneString, int timeZoneSeconds) {
        throw new RuntimeException();
    }

    @Substitute
    public static Session getSession(int dbId, long sessionId) {
        throw new RuntimeException();
    }

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

    @Substitute
    public static Database getDatabase(String dbtype, String path,
                                       HsqlProperties props) {
        throw new RuntimeException();
    }


    @Substitute
    public static synchronized Database lookupDatabaseObject(DatabaseType type,
                                                             String path) {
        throw new RuntimeException();
    }

    @Substitute
    public static void deRegisterServer(Notified server) {
        throw new RuntimeException();
    }
}
