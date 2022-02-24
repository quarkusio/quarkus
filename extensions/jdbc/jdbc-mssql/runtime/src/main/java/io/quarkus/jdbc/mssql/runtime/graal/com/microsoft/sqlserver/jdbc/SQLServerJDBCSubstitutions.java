package io.quarkus.jdbc.mssql.runtime.graal.com.microsoft.sqlserver.jdbc;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerADAL4JUtils")
@Delete
final class SQLServerADAL4JUtils {
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
    private void validateAdalLibrary(String errorMessage) {
        throw new IllegalStateException("Quarkus does not support Active Directory based authentication");
    }

    @Substitute
    private QuarkusSqlFedAuthToken getFedAuthToken(QuarkusSqlFedAuthInfo fedAuthInfo) {
        throw new IllegalStateException("Quarkus does not support Active Directory based authentication");
    }

    @Substitute
    private QuarkusSqlFedAuthToken getMSIAuthToken(String resource, String msiClientId) {
        throw new IllegalStateException("Quarkus does not support MSI based authentication");
    }

}

class SQLServerJDBCSubstitutions {

}
