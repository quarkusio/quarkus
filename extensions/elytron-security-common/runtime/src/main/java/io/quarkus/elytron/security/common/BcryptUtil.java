package io.quarkus.elytron.security.common;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

import org.wildfly.security.password.Password;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.spec.EncryptablePasswordSpec;
import org.wildfly.security.password.spec.IteratedSaltedPasswordAlgorithmSpec;
import org.wildfly.security.password.util.ModularCrypt;

/**
 * Utility class used to produce bcrypt hashes using the Modular Crypt Format.
 */
public class BcryptUtil {

    private static final SecureRandom random = new SecureRandom();
    private static final WildFlyElytronPasswordProvider provider = new WildFlyElytronPasswordProvider();

    /**
     * Produces a Modular Crypt Format bcrypt hash of the given password, using a generated salt and 10 iterations.
     * 
     * @param password the password to hash
     * @return the Modular Crypt Format bcrypt hash of the given password
     * @throws NullPointerException if the password is null
     */
    public static String bcryptHash(String password) {
        return bcryptHash(password, 10);
    }

    /**
     * Produces a Modular Crypt Format bcrypt hash of the given password, using a generated salt and the specified iteration
     * count.
     * 
     * @param password the password to hash
     * @param iterationCount the number of iterations to use while hashing
     * @return the Modular Crypt Format bcrypt hash of the given password
     * @throws NullPointerException if the password is null
     * @throws IllegalArgumentException if the iterationCount parameter is negative or zero
     */
    public static String bcryptHash(String password, int iterationCount) {
        byte[] salt = new byte[BCryptPassword.BCRYPT_SALT_SIZE];
        random.nextBytes(salt);
        return bcryptHash(password, iterationCount, salt);
    }

    /**
     * Produces a Modular Crypt Format bcrypt hash of the given password, using the specified salt and the specified iteration
     * count.
     * 
     * @param password the password to hash
     * @param iterationCount the number of iterations to use while hashing
     * @param salt the salt to use while hashing
     * @return the Modular Crypt Format bcrypt hash of the given password
     * @throws NullPointerException if the password or salt are null
     * @throws IllegalArgumentException if the iterationCount parameter is negative or zero, or if the salt length is not equal
     *         to 16
     */
    public static String bcryptHash(String password, int iterationCount, byte[] salt) {
        if (iterationCount <= 0) {
            throw new IllegalArgumentException("Iteration count must be greater than zero");
        }
        Objects.requireNonNull(password, "password is required");
        Objects.requireNonNull(salt, "salt is required");
        if (salt.length != BCryptPassword.BCRYPT_SALT_SIZE) {
            throw new IllegalArgumentException("Salt length must be exactly " + BCryptPassword.BCRYPT_SALT_SIZE + " bytes");
        }

        PasswordFactory passwordFactory;
        try {
            passwordFactory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT, provider);
        } catch (NoSuchAlgorithmException e) {
            // can't really happen
            throw new RuntimeException(e);
        }

        IteratedSaltedPasswordAlgorithmSpec iteratedAlgorithmSpec = new IteratedSaltedPasswordAlgorithmSpec(iterationCount,
                salt);
        EncryptablePasswordSpec encryptableSpec = new EncryptablePasswordSpec(password.toCharArray(), iteratedAlgorithmSpec);

        try {
            BCryptPassword original = (BCryptPassword) passwordFactory.generatePassword(encryptableSpec);
            return ModularCrypt.encodeAsString(original);
        } catch (InvalidKeySpecException e) {
            // can't really happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Matches a plain text string against an existing Modular Crypt Format bcrypt hash
     *
     * @param plainText the plain text string to check
     * @param passwordHash the Modular Crypt Format bcrypt hash to compare against
     * @return the boolean result of whether or not the plain text matches the decoded Modular Crypt Format bcrypt hash
     * @throws NullPointerException if the plainText password or passwordHash is null
     */
    public static boolean matches(String plainText, String passwordHash) {
        Objects.requireNonNull(plainText, "plainText password is required");
        Objects.requireNonNull(passwordHash, "passwordHash is required");
        try {
            PasswordFactory passwordFactory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT, provider);
            Password userPasswordDecoded = ModularCrypt.decode(passwordHash);
            Password userPasswordRestored = passwordFactory.translate(userPasswordDecoded);
            return passwordFactory.verify(userPasswordRestored, plainText.toCharArray());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException e) {
            // can't really happen
            throw new RuntimeException(e);
        }
    }
}
