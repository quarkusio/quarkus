package io.quarkus.tls;

/**
 * Event fired when a certificate is updated.
 * <p>
 * IMPORTANT: Consumers of this event should be aware that the event is fired from a blocking context (worker thread),
 * and thus can perform blocking operations.
 *
 * @param name the name of the certificate (as configured in the configuration, {@code <default>} for the default certificate)
 * @param tlsConfiguration the updated TLS configuration - the certificate has already been updated
 */
public record CertificateUpdatedEvent(String name, TlsConfiguration tlsConfiguration) {

}
