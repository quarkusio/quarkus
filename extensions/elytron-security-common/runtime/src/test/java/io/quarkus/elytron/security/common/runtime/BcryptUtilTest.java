package io.quarkus.elytron.security.common.runtime;

import io.quarkus.elytron.security.common.BcryptUtil;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.evidence.PasswordGuessEvidence;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.util.ModularCrypt;

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
    public void testHashesTheSameHashWithCharArrayPassword() throws InvalidKeySpecException, NoSuchAlgorithmException {
        String adminKnownBcrypt = "$2a$10$YP9QWYOpxRNCNquTzCjRIuEpc.MiVPTjlMIZHNqHKckKN8FK9Xyh2";
        byte[] knownSalt = new byte[] { 105, 31, -46, 97, -92, 43, -51, 51, -60, 62, -52, 21, -44, 73, 83, 43 };
        String adminProducedBcrypt = BcryptUtil.bcryptHash("admin".toCharArray(), 10, knownSalt);
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

    @Test
    public void testPasswordMatchesWithCharArrayPassword() {
        char[] testPassword = "fubar".toCharArray();
        String testPasswordHash = BcryptUtil.bcryptHash(testPassword, 10);
        Assertions.assertTrue(BcryptUtil.matches(testPassword, testPasswordHash));
    }

    @Test
    public void testPasswordNotMatchesWithCharArrayPassword() {
        char[] testPassword = "fubar".toCharArray();
        String testPasswordHash = BcryptUtil.bcryptHash(testPassword, 10);
        Assertions.assertFalse(BcryptUtil.matches("fubar2".toCharArray(), testPasswordHash));
    }

    @Test
    public void testHashesTheRightPasswordWithCharArrayPassword() throws InvalidKeySpecException, NoSuchAlgorithmException {
        char[] testPassword = "fubar".toCharArray();

        String testPasswordHash = BcryptUtil.bcryptHash(testPassword);

        PasswordGuessEvidence correctPasswordEvidence = new PasswordGuessEvidence(testPassword);
        PasswordGuessEvidence incorrectPasswordEvidence = new PasswordGuessEvidence("stef".toCharArray());
        PasswordCredential producedPasswordCredential = new PasswordCredential(ModularCrypt.decode(testPasswordHash));
        Assertions.assertTrue(producedPasswordCredential.verify(correctPasswordEvidence));
        Assertions.assertFalse(producedPasswordCredential.verify(incorrectPasswordEvidence));
    }

    @Test
    public void testHashesTheSameHashWithDeprecatedStringIterationCountOverload()
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        String testPassword = "fubar";
        String testPasswordHash = BcryptUtil.bcryptHash(testPassword, 4);
        Assertions.assertTrue(BcryptUtil.matches(testPassword, testPasswordHash));
    }

    @Test
    public void testMatchesAcceptsTheSupportedPrefixes() {
        char[] testPassword = "fubar".toCharArray();
        String bcrypt = BcryptUtil.bcryptHash(testPassword);
        for (String prefix : List.of("$2$", "$2a$", "$2x$", "$2y$")) {
            Assertions.assertTrue(BcryptUtil.matches(testPassword, prefix + bcrypt.substring(4)), prefix);
        }
    }

    @Test
    public void testMatchesRejectsAHashThatIsNotModularCryptFormat() {
        char[] testPassword = "fubar".toCharArray();
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> BcryptUtil.matches(testPassword, "not-a-hash"));
        assertInvalidHashMessage(e, "not-a-hash");
        Assertions.assertInstanceOf(InvalidKeySpecException.class, e.getCause());
        Assertions.assertTrue(e.getMessage().contains("ELY08003"), e.getMessage());
    }

    @Test
    public void testMatchesRejectsATruncatedBcryptHash() {
        char[] testPassword = "fubar".toCharArray();
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> BcryptUtil.matches(testPassword, "$2a$10$abc"));
        assertInvalidHashMessage(e, "$2a$10$abc");
        Assertions.assertInstanceOf(IllegalArgumentException.class, e.getCause());
        Assertions.assertTrue(e.getMessage().contains("ELY08021"), e.getMessage());
    }

    @Test
    public void testMatchesRejectsAHashOfAnotherAlgorithm() {
        char[] testPassword = "fubar".toCharArray();
        String md5Crypt = "$1$saltsalt$qjXMvbEw8oaL.CzflDugX/";
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> BcryptUtil.matches(testPassword, md5Crypt));
        assertInvalidHashMessage(e, md5Crypt);
        Assertions.assertInstanceOf(InvalidKeyException.class, e.getCause());
        Assertions.assertTrue(e.getMessage().contains("ELY08027"), e.getMessage());
    }

    @Test
    public void testMatchesRejectsThe2bPrefix() {
        char[] testPassword = "fubar".toCharArray();
        String notSupported = "$2b$" + BcryptUtil.bcryptHash(testPassword).substring(4);
        IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class,
                () -> BcryptUtil.matches(testPassword, notSupported));
        assertInvalidHashMessage(e, notSupported);
        Assertions.assertInstanceOf(InvalidKeySpecException.class, e.getCause());
    }

    private static void assertInvalidHashMessage(IllegalArgumentException e, String hash) {
        Assertions.assertTrue(e.getMessage().contains("not a valid Modular Crypt Format bcrypt hash"), e.getMessage());
        Assertions.assertTrue(e.getMessage().contains("'$2a$'"), e.getMessage());
        Assertions.assertTrue(e.getMessage().contains("53 characters of salt and hash"), e.getMessage());
        Assertions.assertFalse(e.getMessage().contains(hash), e.getMessage());
    }
}
