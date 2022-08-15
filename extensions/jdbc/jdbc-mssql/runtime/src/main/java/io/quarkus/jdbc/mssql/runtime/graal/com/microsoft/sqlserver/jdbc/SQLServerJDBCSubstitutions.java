package io.quarkus.jdbc.mssql.runtime.graal.com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import javax.net.ssl.KeyManager;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.microsoft.sqlserver.jdbc.SQLServerStatement;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SqlFedAuthToken")
final class QuarkusSqlFedAuthToken {

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerConnection", innerClass = "SqlFedAuthInfo")
final class QuarkusSqlFedAuthInfo {

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.Parameter")
final class QuarkusSqlParameter {

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerConnection")
final class QuarkusSQLServerConnection {

    @Substitute
    private QuarkusSqlFedAuthToken getFedAuthToken(QuarkusSqlFedAuthInfo fedAuthInfo) {
        throw new IllegalStateException("Quarkus does not support Active Directory based authentication");
    }

    @Substitute
    private void setKeyVaultProvider(String keyStorePrincipalId) throws SQLServerException {
        throw new IllegalStateException("Quarkus does not support Keyvault-based column encryption");
    }

    @Substitute
    private void setKeyVaultProvider(String keyStorePrincipalId, String keyStoreSecret) throws SQLServerException {
        throw new IllegalStateException("Quarkus does not support Keyvault-based column encryption");
    }

    @Substitute
    ArrayList<byte[]> initEnclaveParameters(SQLServerStatement statement, String userSql, String preparedTypeDefinitions,
            QuarkusSqlParameter[] params, ArrayList<String> parameterNames) throws SQLServerException {
        throw new IllegalStateException("Quarkus does not support AAS Enclave");
    }
}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerCertificateUtils")
final class QuarkusSqlSQLServerCertificateUtils {
    @Substitute
    static KeyManager[] getKeyManagerFromFile(String certPath, String keyPath, String keyPassword)
            throws IOException, GeneralSecurityException, SQLServerException {
        throw new IllegalStateException("Quarkus does not support Client Certificate based authentication");
    }
}

class SQLServerJDBCSubstitutions {

}
