package io.quarkus.elytron.security.common.runtime;

import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.util.ModularCrypt;

import io.quarkus.elytron.security.common.BcryptUtil;

public class BcryptUtilTest {

    static {
        Security.addProvider(new WildFlyElytronPasswordProvider());
    }

    @Test
    public void testHashesTheRightPassword() throws InvalidKeySpecException, NoSuchAlgorithmException {
        String testPassword = "fubar";

        String testPasswordHash = BcryptUtil.bcryptHash(testPassword);

        PasswordGuessEvidence correctPasswordEvidence = new PasswordGuessEvidence(testPassword.toCharArray());
        PasswordGuessEvidence incorrectPasswordEvidence = new PasswordGuessEvidence("stef".toCharArray());
        PasswordCredential producedPasswordCredential = new PasswordCredential(ModularCrypt.decode(testPasswordHash));
        Assertions.assertTrue(producedPasswordCredential.verify(correctPasswordEvidence));
        Assertions.assertFalse(producedPasswordCredential.verify(incorrectPasswordEvidence));
    }

    @Test
    public void testHashesTheSameHash() throws InvalidKeySpecException, NoSuchAlgorithmException {
        String adminKnownBcrypt = "$2a$10$YP9QWYOpxRNCNquTzCjRIuEpc.MiVPTjlMIZHNqHKckKN8FK9Xyh2";
        byte[] knownSalt = new byte[] { 105, 31, -46, 97, -92, 43, -51, 51, -60, 62, -52, 21, -44, 73, 83, 43 };
        String adminProducedBcrypt = BcryptUtil.bcryptHash("admin", 10, knownSalt);
        Assertions.assertEquals(adminKnownBcrypt, adminProducedBcrypt);
    }

    @Test
    public void testPasswordMatches() {
        String testPassword = "fubar";
        String testPasswordHash = BcryptUtil.bcryptHash(testPassword);
        Assertions.assertTrue(BcryptUtil.matches(testPassword, testPasswordHash));
    }

    @Test
    public void testPasswordNotMatches() {
        String testPassword = "fubar";
        String testPasswordHash = BcryptUtil.bcryptHash(testPassword);
        Assertions.assertFalse(BcryptUtil.matches("fubar2", testPasswordHash));
    }
}
