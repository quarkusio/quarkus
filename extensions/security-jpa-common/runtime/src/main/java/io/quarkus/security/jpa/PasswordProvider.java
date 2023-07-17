package io.quarkus.security.jpa;

import org.wildfly.security.password.Password;

/**
 * Provides the {@link Password} according to how the password is hashed in the database.
 */
public interface PasswordProvider {
    Password getPassword(String pass);
}
