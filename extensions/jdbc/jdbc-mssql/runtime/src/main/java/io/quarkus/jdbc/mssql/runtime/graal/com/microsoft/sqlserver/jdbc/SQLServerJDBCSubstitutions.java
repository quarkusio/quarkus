package io.quarkus.jdbc.mssql.runtime.graal.com.microsoft.sqlserver.jdbc;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

import javax.net.ssl.KeyManager;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.microsoft.sqlserver.jdbc.SQLServerStatement;
import com.microsoft.sqlserver.jdbc.SqlAuthenticationToken;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerConnection", innerClass = "SqlFedAuthInfo")
final class QuarkusSqlFedAuthInfo {

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.Parameter")
final class QuarkusSqlParameter {

}

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerConnection")
final class QuarkusSQLServerConnection {

    @Substitute
    private SqlAuthenticationToken getFedAuthToken(QuarkusSqlFedAuthInfo fedAuthInfo) {
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
    ArrayList<byte[]> initEnclaveParameters(SQLServerStatement statement, String userSql,
            String preparedTypeDefinitions, QuarkusSqlParameter[] params, ArrayList<String> parameterNames)
            throws SQLServerException {
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

@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerLexer")
@Delete // Deleting this one explicitly, so to help with maintenance with the substitutions of SQLServerFMTQuery
final class SQLServerLexerRemove {

}

/**
 * This will make sure the ANTLR4 Lexer included in the driver is not reachable; this was mostly prevented by not
 * allowing to explicitly set the useFmtOnly connection property, but this code path would also get activated on very
 * old SQL Server versions being detected on a connection. Since that's not a constant that the compiler can rely on, we
 * need one more substitution.
 */
@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerFMTQuery")
final class SQLServerFMTQuery {

    @Substitute
    SQLServerFMTQuery(String userSql) throws SQLServerException {
        throw new IllegalStateException("It is not supported to connect to SQL Server versions older than 2012");
    }

}

/**
 * This substitution is not strictly necessary, but it helps by providing a better error message to our users.
 */
@TargetClass(className = "com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement")
final class DisableFMTRemove {

    @Substitute
    public final boolean getUseFmtOnly() throws SQLServerException {
        return false;// Important for this to be disabled via a constant
    }

    @Substitute
    public final void setUseFmtOnly(boolean useFmtOnly) throws SQLServerException {
        if (useFmtOnly) {
            throw new IllegalStateException(
                    "It is not possible to enable the useFmtOnly option on Quarkus: this option is only useful on SQL Server version 2008 (which is not supported) and introduces several other problems.");
        }
    }

}

class SQLServerJDBCSubstitutions {

}
