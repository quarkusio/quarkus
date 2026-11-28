package io.quarkus.spiffe.svid.x509;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * Provisions an X.509-SVID from the SPIFFE Workload API into a TLS registry configuration.
 * <p>
 * The SPIFFE client fetches the workload certificate (X.509-SVID) together with its private key and the trust bundle
 * and registers them as a named TLS configuration that other extensions can reference to perform mutual TLS.
 * <p>
 * A single workload has one X.509-SVID, but it can be provisioned into more than one TLS configuration,
 * for example to validate a different peer {@link #spiffeId()} per target.
 * <p>
 * Using this annotation requires the Quarkus TLS registry extension ({@code quarkus-tls-registry}) to be present; the
 * build fails otherwise.
 * <p>
 * The generated TLS configuration enforces the {@link #spiffeId()} authorization through the TLS registry options. It
 * has no raw key store or trust store, so extensions that read {@code TlsConfiguration#getKeyStore()} or
 * {@code getTrustStore()} are not supported.
 */
@Experimental("This API is currently experimental and might get changed")
@Retention(RUNTIME)
@Target(TYPE)
@Repeatable(X509Svid.List.class)
public @interface X509Svid {

    /**
     * Built-in {@link #spiffeId()} authorization policy that authorizes any peer presenting an X.509-SVID issued by the
     * same trust domain as this workload.
     */
    String SAME_TRUST_DOMAIN = "<<same-trust-domain>>";

    /**
     * Name of the TLS registry configuration that the X.509-SVID is provisioned into. Other extensions, such as the REST
     * client, can then reference this TLS configuration by name.
     */
    String tls();

    /**
     * SPIFFE ID of the peer that this workload authorizes for mutual TLS, for example
     * {@code spiffe://example.org/keycloak}. The trust manager authorizes a connection only when the peer X.509-SVID
     * carries this SPIFFE ID in its URI Subject Alternative Name; set the {@link #SAME_TRUST_DOMAIN} policy to authorize
     * any peer from this workload's trust domain instead.
     * <p>
     * The value can be a property expression. In this case, the SPIFFE client attempts to use the configured value
     * instead: {@code @X509Svid(spiffeId = "${my.peer.spiffe-id}")}. Additionally, the property expression can specify a
     * default value: {@code @X509Svid(spiffeId = "${my.peer.spiffe-id:spiffe://example.org/keycloak}")}.
     * <p>
     * All {@code @X509Svid} declarations provision the same X.509-SVID and trust bundle, so they differ only in the peer
     * they authorize. Each {@code spiffeId}, including {@link #SAME_TRUST_DOMAIN}, must therefore be unique across
     * declarations. An invalid or duplicate SPIFFE ID fails the build. A SPIFFE ID from a property expression is
     * validated at application startup and is not checked for duplicates.
     */
    String spiffeId() default SAME_TRUST_DOMAIN;

    /**
     * The container annotation that holds several {@link X509Svid} declarations.
     */
    @Experimental("This API is currently experimental and might get changed")
    @Retention(RUNTIME)
    @Target(TYPE)
    @interface List {

        X509Svid[] value();

    }
}
