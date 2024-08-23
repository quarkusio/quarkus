package io.quarkus.security.jpa;

/**
 * Describes how the password is hashed in the database.
 */
public enum PasswordType {
    /**
     * The password is stored hashed using a custom format.
     */
    CUSTOM,
    /**
     * The password is stored hashed using bcrypt in the Modular Crypt Format.
     */
    MCF,
    /**
     * The password is stored in clear text. Do not use in production.
     */
    CLEAR;
}
