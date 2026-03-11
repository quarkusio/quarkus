package io.quarkus.email.authentication;

import io.quarkus.security.spi.runtime.SecurityEvent;

/**
 * A security event used to report email authentication events such as a sending of the email authentication code.
 */
public interface EmailAuthenticationEvent extends SecurityEvent {

    /**
     * A {@link SecurityEvent#getEventProperties()} key of the email address in an authentication code request.
     * This event attribute is added together with the {@link #FAILURE_KEY} attribute.
     */
    String EMAIL_ADDRESS_KEY = "io.quarkus.email.authentication.EmailAuthenticationEvent#EMAIL_ADDRESS";

    /**
     * A {@link SecurityEvent#getEventProperties()} key of a {@link Throwable} when the authentication code request fails.
     */
    String FAILURE_KEY = "io.quarkus.email.authentication.EmailAuthenticationEvent#FAILURE";

    /**
     * A {@link SecurityEvent#getEventProperties()} key of the email authentication code.
     */
    String AUTHENTICATION_CODE_KEY = "io.quarkus.email.authentication.EmailAuthenticationEvent#AUTHENTICATION_CODE";

    enum EmailAuthenticationEventType {
        /**
         * Event fired when a user was successfully authenticated with a call to the Email mechanism POST location.
         */
        EMAIL_LOGIN,
        /**
         * Event fired when authentication code was requested, the request was processed and sent.
         * If Quarkus failed to send the code, this event is fired with the failure stored in the event properties.
         */
        AUTHENTICATION_CODE
    }

    /**
     * Email authentication event type.
     *
     * @return {@link EmailAuthenticationEventType}
     */
    EmailAuthenticationEventType getEventType();

}
