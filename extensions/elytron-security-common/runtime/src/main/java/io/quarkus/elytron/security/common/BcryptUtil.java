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
     * @deprecated Use {@link #bcryptHash(char[])} instead, so the password does not have to be kept in a String
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public static String bcryptHash(String password) {
        return bcryptHash(password.toCharArray(), 10);
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
     * @deprecated Use {@link #bcryptHash(char[], int)} instead, so the password does not have to be kept in a String
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public static String bcryptHash(String password, int iterationCount) {
        return bcryptHash(password.toCharArray(), iterationCount);
    }

    /**
     * Produces a Modular Crypt Format bcrypt hash of the given password, using a generated salt and 10 iterations.
     *
     * @param password the password to hash; the caller is responsible for clearing this array once it is no longer
     *        needed
     * @return the Modular Crypt Format bcrypt hash of the given password
     * @throws NullPointerException if the password is null
     */
    public static String bcryptHash(char[] password) {
        return bcryptHash(password, 10);
    }

    /**
     * Produces a Modular Crypt Format bcrypt hash of the given password, using a generated salt and the specified iteration
     * count.
     *
     * @param password the password to hash; the caller is responsible for clearing this array once it is no longer
     *        needed
     * @param iterationCount the number of iterations to use while hashing
     * @return the Modular Crypt Format bcrypt hash of the given password
     * @throws NullPointerException if the password is null
     * @throws IllegalArgumentException if the iterationCount parameter is negative or zero
     */
    public static String bcryptHash(char[] password, int iterationCount) {
        byte[] salt = new byte[BCryptPassword.BCRYPT_SALT_SIZE];
        random.nextBytes(salt);
        return bcryptHash(password, iterationCount, salt);
    }

    /**
     * Produces a Modular Crypt Format bcrypt hash of the given password, using the specified salt and the specified iteration
     * count.
     *
     * @param password the password to hash; the caller is responsible for clearing this array once it is no longer
     *        needed
     * @param iterationCount the number of iterations to use while hashing
     * @param salt the salt to use while hashing
     * @return the Modular Crypt Format bcrypt hash of the given password
     * @throws NullPointerException if the password or salt are null
     * @throws IllegalArgumentException if the iterationCount parameter is negative or zero, or if the salt length is not equal
     *         to 16
     */
    public static String bcryptHash(char[] password, int iterationCount, byte[] salt) {
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
        EncryptablePasswordSpec encryptableSpec = new EncryptablePasswordSpec(password, iteratedAlgorithmSpec);

        try {
            BCryptPassword original = (BCryptPassword) passwordFactory.generatePassword(encryptableSpec);
            return ModularCrypt.encodeAsString(original);
        } catch (InvalidKeySpecException e) {
            // can't really happen
            throw new RuntimeException(e);
        }
    }

    /**
     * @deprecated Use {@link #bcryptHash(char[], int, byte[])} instead, so the password does not have to be kept in a
     *             String
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public static String bcryptHash(String password, int iterationCount, byte[] salt) {
        return bcryptHash(password.toCharArray(), iterationCount, salt);
    }

    /**
     * Matches a plain text password against an existing Modular Crypt Format bcrypt hash.
     * <p>
     * The hash must be a bcrypt hash in Modular Crypt Format, that is a {@code $2$}, {@code $2a$}, {@code $2x$} or
     * {@code $2y$} prefix, the cost as two digits, {@code $}, and 53 characters of radix-64 salt and hash, as
     * {@link #bcryptHash(String)} produces.
     *
     * @param plainText the plain text password to check
     * @param passwordHash the Modular Crypt Format bcrypt hash to compare against
     * @return the boolean result of whether the plain text matches the decoded Modular Crypt Format bcrypt hash
     * @throws NullPointerException if the plainText password or passwordHash is null
     * @throws IllegalArgumentException if the passwordHash is not a bcrypt hash of that form
     * @throws RuntimeException wrapping a {@link NoSuchAlgorithmException} if the security providers offer no bcrypt
     *         implementation
     * @deprecated Use {@link #matches(char[], String)} instead, so the password does not have to be kept in a String
     */
    @Deprecated(since = "4.0", forRemoval = true)
    public static boolean matches(String plainText, String passwordHash) {
        Objects.requireNonNull(plainText, "plainText password is required");
        Objects.requireNonNull(passwordHash, "passwordHash is required");
        return matches(plainText.toCharArray(), passwordHash);
    }

    /**
     * Matches a plain text password against an existing Modular Crypt Format bcrypt hash.
     * <p>
     * The hash must be a bcrypt hash in Modular Crypt Format, that is a {@code $2$}, {@code $2a$}, {@code $2x$} or
     * {@code $2y$} prefix, the cost as two digits, {@code $}, and 53 characters of radix-64 salt and hash, as
     * {@link #bcryptHash(String)} produces.
     *
     * @param plainText the plain text password to check; the caller is responsible for clearing this array once it is
     *        no longer needed
     * @param passwordHash the Modular Crypt Format bcrypt hash to compare against
     * @return the boolean result of whether the plain text matches the decoded Modular Crypt Format bcrypt hash
     * @throws NullPointerException if the plainText password or passwordHash is null
     * @throws IllegalArgumentException if the passwordHash is not a bcrypt hash of that form
     * @throws RuntimeException wrapping a {@link NoSuchAlgorithmException} if the security providers offer no bcrypt
     *         implementation
     */
    public static boolean matches(char[] plainText, String passwordHash) {
        Objects.requireNonNull(plainText, "plainText password is required");
        Objects.requireNonNull(passwordHash, "passwordHash is required");
        PasswordFactory passwordFactory;
        try {
            passwordFactory = PasswordFactory.getInstance(BCryptPassword.ALGORITHM_BCRYPT, provider);
        } catch (NoSuchAlgorithmException e) {
            // can't really happen
            throw new RuntimeException(e);
        }
        try {
            Password userPasswordRestored = passwordFactory.translate(ModularCrypt.decode(passwordHash));
            return passwordFactory.verify(userPasswordRestored, plainText);
        } catch (InvalidKeySpecException | InvalidKeyException | IllegalArgumentException e) {
            throw new IllegalArgumentException("The provided password hash is not a valid Modular Crypt Format bcrypt hash"
                    + " (expected a '$2$', '$2a$', '$2x$' or '$2y$' prefix, the cost, and 53 characters of salt and"
                    + " hash): " + e.getMessage(), e);
        }
    }
}
