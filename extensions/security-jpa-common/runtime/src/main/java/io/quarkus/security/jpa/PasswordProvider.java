package io.quarkus.security.jpa;

import org.wildfly.security.password.Password;

/**
 * Returns a password stored in the database as {@link Password}.
 */
public interface PasswordProvider {
    /**
     * Return a password stored in the database.
     *
     * @param passwordFromDatabase - password in the database. If this password is hashed then
     *        {@link Password} implementation must provide a hashing algorithm information.
     *        Do not create a hash from this password - the security runtime will
     *        apply the hashing algorithm to the incoming user secret and compare it with this password.
     * @return {@link Password} representation of the password stored in the database.
     */
    Password getPassword(String passwordFromDatabase);
}
