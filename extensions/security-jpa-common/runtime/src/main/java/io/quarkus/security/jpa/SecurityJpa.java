package io.quarkus.security.jpa;

import io.quarkus.arc.Arc;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.spi.runtime.IdentityProviderBuilder;
import io.smallrye.common.annotation.Experimental;

/**
 * A CDI bean used to build Quarkus Security JPA {@link IdentityProvider}s programmatically.
 * This bean should be used together with the CDI event 'HttpSecurity' in following situations:
 * <ul>
 * <li>
 * You want to configure the Basic authentication to use the Quarkus Security JPA {@link IdentityProvider},
 * while other authentication mechanism is using different {@link IdentityProvider}:
 *
 * <pre>
 * {@code
 * import static io.quarkus.security.jpa.SecurityJpa.jpa;
 * import io.quarkus.vertx.http.security.Form;
 * import io.quarkus.vertx.http.security.HttpSecurity;
 * import jakarta.enterprise.event.Observes;
 *
 * public class HttpSecurityConfiguration {
 *
 *     void configure(@Observes HttpSecurity httpSecurity) {
 *         httpSecurity
 *                 .basic(jpa())
 *                 .mechanism(Form.create(), createCustomIdentityProviders());
 *     }
 * }
 * }
 * </pre>
 *
 * </li>
 * <li>
 * If you want to store the identity information in a named datasource determined during the runtime:
 *
 * <pre>
 * {@code
 * import static io.quarkus.security.jpa.SecurityJpa.jpa;
 * import io.quarkus.vertx.http.security.Form;
 * import io.quarkus.vertx.http.security.HttpSecurity;
 * import jakarta.enterprise.event.Observes;
 * import org.eclipse.microprofile.config.inject.ConfigProperty;
 *
 * public class HttpSecurityConfiguration {
 *
 *     void configure(@Observes HttpSecurity httpSecurity, @ConfigProperty(name = "named-pu") String namedPersistenceUnit) {
 *         httpSecurity.basic(jpa().persistence(namedPersistenceUnit));
 *         // or maybe you need to use 2 different persistence units for each mechanism
 *         httpSecurity.mechanism(Form.create(), jpa().persistence("different-persistence-unit"));
 *     }
 * }
 * }
 * </pre>
 *
 * </li>
 * </ul>
 */
@Experimental("This API is currently experimental and might get changed")
public interface SecurityJpa extends IdentityProviderBuilder {

    /**
     * Selects the persistence unit used by the Security JPA identity providers.
     *
     * @param persistenceUnitName persistence unit name
     * @return {@link SecurityJpa}
     */
    SecurityJpa persistence(String persistenceUnitName);

    /**
     * Specifies the {@link SecurityIdentityAugmentor} used to augment the {@link io.quarkus.security.identity.SecurityIdentity}
     * produced by the Security JPA. When this method is used, only augmentors specified this way will be applied.
     *
     * @param securityIdentityAugmentor {@link SecurityIdentityAugmentor}
     * @return {@link SecurityJpa}
     */
    SecurityJpa augmentor(SecurityIdentityAugmentor securityIdentityAugmentor);

    /**
     * Looks up the {@link SecurityJpa} builder and returns it.
     *
     * @return {@link SecurityJpa}
     */
    static SecurityJpa jpa() {
        return Arc.requireContainer().instance(SecurityJpa.class).get();
    }
}
