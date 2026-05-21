package io.quarkus.email.authentication;

import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * Email authentication code storage. Should be created as a CDI bean implementing this interface.
 */
public interface EmailAuthenticationCodeStorage {

    /**
     * Stores generated email authentication code. This method must complete before the HTTP request to generate
     * the code is completed. If you wish to reject the request, we recommend to return {@link Uni} with the failure.
     * This method is called for every request to generate code, regardless of whether the user with the email address
     * exists or not. Depending on your storage, you may want to validate whether the associated user exists.
     *
     * @param codeRequest {@link EmailAuthenticationCodeRequest}; the code array will be cleared on {@link Uni} termination
     * @param emailAddress email address
     * @param routingContext incoming HTTP request event
     * @return {@link Uni} with void if the code was stored successfully, or failure if the request to store this code
     *         was rejected
     */
    Uni<Void> storeCode(EmailAuthenticationCodeRequest codeRequest, String emailAddress, RoutingContext routingContext);

    /**
     * Retrieves the email address based on the given email authentication code.
     *
     * @param code email authentication code
     * @param routingContext incoming HTTP request event
     * @return {@link Uni} with email address or null item for unknown, rejected or invalid code
     */
    Uni<String> findEmailAddressByCode(String code, RoutingContext routingContext);

    /**
     * Email authentication code request.
     */
    interface EmailAuthenticationCodeRequest {

        /**
         * Retrieves email authentication code (generated the first time this method is invoked).
         *
         * @return generated email authentication code
         */
        char[] code();

    }

    /**
     * Default {@link EmailAuthenticationCodeStorage} implementation. This CDI bean is useful when you are implementing
     * mitigation for security risks such as a replay attack, brute-force protection, or events for failed logins.
     * Example usage:
     *
     * <pre>
     * {@code
     * import io.quarkus.security.AuthenticationFailedException;
     * import io.smallrye.mutiny.Uni;
     * import io.vertx.ext.web.RoutingContext;
     * import jakarta.enterprise.context.ApplicationScoped;
     *
     * &#64;ApplicationScoped
     * class CustomEmailAuthenticationCodeStorage implements EmailAuthenticationCodeStorage {
     *
     *     private final EmailAuthenticationCodeStorage delegate;
     *
     *     CustomEmailAuthenticationCodeStorage(DefaultEmailAuthenticationCodeStorage defaultStorage) {
     *         this.delegate = defaultStorage;
     *     }
     *
     *     &#64;Override
     *     public Uni<Void> storeCode(char[] code, String emailAddress, RoutingContext routingContext) {
     *         if (exceededNumberOfRequests(emailAddress) || isBlacklistedIpAddress(routingContext)) {
     *             return Uni.createFrom().failure(new AuthenticationFailedException());
     *         }
     *         return delegate.storeCode(code, emailAddress, routingContext);
     *     }
     *
     *     @Override
     *     public Uni<String> findEmailAddressByCode(String code, RoutingContext routingContext) {
     *         if (codeAlreadyUsedToAuthenticate(code)) {
     *             return Uni.createFrom().failure(new AuthenticationFailedException());
     *         }
     *         return delegate.findEmailAddressByCode(code, routingContext);
     *     }
     *
     *     // your implementation comes here
     * }
     * }
     * </pre>
     */
    interface DefaultEmailAuthenticationCodeStorage extends EmailAuthenticationCodeStorage {

    }

}
