package io.quarkus.jdbc.mssql.runtime.graal.com.microsoft.sqlserver.jdbc;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerADAL4JUtils")
@Substitute
final class SQLServerADAL4JUtils {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    static final private java.util.logging.Logger adal4jLogger = null;

    @Substitute
    static QuarkusSqlFedAuthToken getSqlFedAuthToken(QuarkusSqlFedAuthInfo fedAuthInfo, String user, String password,
            String authenticationString) {
        throw new IllegalStateException("Quarkus does not support Active Directory based authentication");
    }

    @Substitute
    static QuarkusSqlFedAuthToken getSqlFedAuthTokenIntegrated(QuarkusSqlFedAuthInfo fedAuthInfo, String authenticationString) {
        throw new IllegalStateException("Quarkus does not support Active Directory based authentication");
    }

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SqlFedAuthToken")
final class QuarkusSqlFedAuthToken {

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerConnection", innerClass = "SqlFedAuthInfo")
final class QuarkusSqlFedAuthInfo {

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerConnection")
final class QuarkusSQLServerConnection {

    @Substitute
    private QuarkusSqlFedAuthToken getMSIAuthToken(String resource, String msiClientId) {
        throw new IllegalStateException("Quarkus does not support MSI based authentication");
    }

}

class SQLServerJDBCSubstitutions {

}