package io.quarkus.email.authentication;

import io.smallrye.mutiny.Uni;

/**
 * Email authentication code sender. Should be created as a CDI bean implementing this interface.
 */
public interface EmailAuthenticationCodeSender {

    /**
     * Sends email authentication code to given email address.
     *
     * @param code email authentication code; the array is emptied on {@link Uni} termination; most not be null
     * @param emailAddress which should receive the generated code; must not be null
     * @return {@link Uni} with void item or failure; never null
     */
    Uni<Void> sendCode(char[] code, String emailAddress);

}
