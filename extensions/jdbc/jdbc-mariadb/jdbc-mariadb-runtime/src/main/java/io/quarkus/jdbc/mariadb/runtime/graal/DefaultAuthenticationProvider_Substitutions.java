package io.quarkus.jdbc.mariadb.runtime.graal;

import java.sql.SQLException;

import org.mariadb.jdbc.internal.com.send.InterfaceAuthSwitchSendResponsePacket;
import org.mariadb.jdbc.internal.com.send.SendClearPasswordAuthPacket;
import org.mariadb.jdbc.internal.com.send.SendEd25519PasswordAuthPacket;
import org.mariadb.jdbc.internal.com.send.SendGssApiAuthPacket;
import org.mariadb.jdbc.internal.com.send.SendNativePasswordAuthPacket;
import org.mariadb.jdbc.internal.com.send.SendOldPasswordAuthPacket;
import org.mariadb.jdbc.internal.io.input.PacketInputStream;
import org.mariadb.jdbc.internal.protocol.authentication.DefaultAuthenticationProvider;

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
    public static InterfaceAuthSwitchSendResponsePacket processAuthPlugin(PacketInputStream reader,
            String plugin, String password,
            byte[] authData, int seqNo, String passwordCharacterEncoding)
            throws SQLException {
        switch (plugin) {
            case MYSQL_NATIVE_PASSWORD:
                return new SendNativePasswordAuthPacket(password, authData, seqNo,
                        passwordCharacterEncoding);
            case MYSQL_OLD_PASSWORD:
                return new SendOldPasswordAuthPacket(password, authData, seqNo, passwordCharacterEncoding);
            case MYSQL_CLEAR_PASSWORD:
                return new SendClearPasswordAuthPacket(password, authData, seqNo,
                        passwordCharacterEncoding);
            case DIALOG:
                throw new UnsupportedOperationException("Authentication strategy 'dialog' is not supported in GraalVM");
            case GSSAPI_CLIENT:
                return new SendGssApiAuthPacket(reader, password, authData, seqNo,
                        passwordCharacterEncoding);
            case MYSQL_ED25519_PASSWORD:
                return new SendEd25519PasswordAuthPacket(password, authData, seqNo,
                        passwordCharacterEncoding);

            default:
                throw new SQLException(
                        "Client does not support authentication protocol requested by server. "
                                + "Consider upgrading MariaDB client. plugin was = " + plugin,
                        "08004", 1251);
        }
    }

}
