package io.quarkus.jdbc.mariadb.runtime.graal;

import java.sql.SQLException;

import org.mariadb.jdbc.authentication.AuthenticationPlugin;
import org.mariadb.jdbc.authentication.AuthenticationPluginLoader;
import org.mariadb.jdbc.internal.com.send.authentication.ClearPasswordPlugin;
import org.mariadb.jdbc.internal.com.send.authentication.Ed25519PasswordPlugin;
import org.mariadb.jdbc.internal.com.send.authentication.NativePasswordPlugin;
import org.mariadb.jdbc.internal.com.send.authentication.OldPasswordPlugin;
import org.mariadb.jdbc.internal.com.send.authentication.SendGssApiAuthPacket;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(AuthenticationPluginLoader.class)
@Substitute
public final class AuthenticationPluginLoader_Substitutions {

    public static final String MYSQL_NATIVE_PASSWORD = "mysql_native_password";
    public static final String MYSQL_OLD_PASSWORD = "mysql_old_password";
    public static final String MYSQL_CLEAR_PASSWORD = "mysql_clear_password";
    public static final String MYSQL_ED25519_PASSWORD = "client_ed25519";
    private static final String GSSAPI_CLIENT = "auth_gssapi_client";
    private static final String DIALOG = "dialog";

    @Substitute
    public static AuthenticationPlugin get(String type) throws SQLException {
        switch (type) {
            case MYSQL_NATIVE_PASSWORD:
                return new NativePasswordPlugin();
            case MYSQL_OLD_PASSWORD:
                return new OldPasswordPlugin();
            case MYSQL_CLEAR_PASSWORD:
                return new ClearPasswordPlugin();
            case DIALOG:
                throw new UnsupportedOperationException("Authentication strategy 'dialog' is not supported in GraalVM");
            //return new SendPamAuthPacket();
            case GSSAPI_CLIENT:
                return new SendGssApiAuthPacket();
            case MYSQL_ED25519_PASSWORD:
                return new Ed25519PasswordPlugin();

            default:
                throw new SQLException(
                        "Client does not support authentication protocol requested by server. "
                                + "Consider upgrading MariaDB client. plugin was = " + type,
                        "08004", 1251);
        }
    }

}
