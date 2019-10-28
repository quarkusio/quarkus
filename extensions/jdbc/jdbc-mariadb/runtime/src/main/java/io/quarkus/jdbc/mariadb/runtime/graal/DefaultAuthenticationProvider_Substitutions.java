package io.quarkus.jdbc.mariadb.runtime.graal;

import java.sql.SQLException;

import org.mariadb.jdbc.internal.com.send.authentication.AuthenticationPlugin;
import org.mariadb.jdbc.internal.com.send.authentication.ClearPasswordPlugin;
import org.mariadb.jdbc.internal.com.send.authentication.Ed25519PasswordPlugin;
import org.mariadb.jdbc.internal.com.send.authentication.NativePasswordPlugin;
import org.mariadb.jdbc.internal.com.send.authentication.OldPasswordPlugin;
import org.mariadb.jdbc.internal.com.send.authentication.SendGssApiAuthPacket;
// import org.mariadb.jdbc.internal.com.send.authentication.SendPamAuthPacket;
import org.mariadb.jdbc.internal.protocol.authentication.DefaultAuthenticationProvider;
import org.mariadb.jdbc.internal.util.Options;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(DefaultAuthenticationProvider.class)
@Substitute
public final class DefaultAuthenticationProvider_Substitutions {

    public static final String MYSQL_NATIVE_PASSWORD = "mysql_native_password";
    public static final String MYSQL_OLD_PASSWORD = "mysql_old_password";
    public static final String MYSQL_CLEAR_PASSWORD = "mysql_clear_password";
    public static final String MYSQL_ED25519_PASSWORD = "client_ed25519";
    private static final String GSSAPI_CLIENT = "auth_gssapi_client";
    private static final String DIALOG = "dialog";

    @Substitute
    public static AuthenticationPlugin processAuthPlugin(String plugin,
            String password,
            byte[] authData,
            Options options)
            throws SQLException {
        switch (plugin) {
            case MYSQL_NATIVE_PASSWORD:
                return new NativePasswordPlugin(password, authData, options.passwordCharacterEncoding);
            case MYSQL_OLD_PASSWORD:
                return new OldPasswordPlugin(password, authData);
            case MYSQL_CLEAR_PASSWORD:
                return new ClearPasswordPlugin(password, options.passwordCharacterEncoding);
            case DIALOG:
                throw new UnsupportedOperationException("Authentication strategy 'dialog' is not supported in GraalVM");
                //return new SendPamAuthPacket(password, authData, options.passwordCharacterEncoding);
            case GSSAPI_CLIENT:
                return new SendGssApiAuthPacket(authData, options.servicePrincipalName);
            case MYSQL_ED25519_PASSWORD:
                return new Ed25519PasswordPlugin(password, authData, options.passwordCharacterEncoding);

            default:
                throw new SQLException(
                        "Client does not support authentication protocol requested by server. "
                                + "Consider upgrading MariaDB client. plugin was = " + plugin,
                        "08004", 1251);
        }
    }

}
