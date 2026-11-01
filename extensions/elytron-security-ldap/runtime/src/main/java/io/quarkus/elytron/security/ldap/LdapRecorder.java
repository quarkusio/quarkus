package io.quarkus.elytron.security.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.auth.realm.CacheableSecurityRealm;
import org.wildfly.security.auth.realm.CachingSecurityRealm;
import org.wildfly.security.auth.realm.ldap.AttributeMapping;
import org.wildfly.security.auth.realm.ldap.DirContextFactory;
import org.wildfly.security.auth.realm.ldap.LdapSecurityRealmBuilder;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.cache.LRURealmIdentityCache;

import io.quarkus.elytron.security.ldap.config.AttributeMappingConfig;
import io.quarkus.elytron.security.ldap.config.DirContextConfig;
import io.quarkus.elytron.security.ldap.config.IdentityMappingConfig;
import io.quarkus.elytron.security.ldap.config.LdapSecurityRealmRuntimeConfig;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;

public class LdapRecorder {
    private static final Logger log = LoggerFactory.getLogger(LdapRecorder.class);

    public static SecurityRealm createRealm(LdapSecurityRealmRuntimeConfig runtimeConfig,
            TlsConfigurationRegistry tlsRegistry) {
        LdapSecurityRealmBuilder.IdentityMappingBuilder identityMappingBuilder = LdapSecurityRealmBuilder.builder()
                .setDirContextSupplier(createDirContextSupplier(runtimeConfig.dirContext(), tlsRegistry))
                .identityMapping();

        if (runtimeConfig.identityMapping().searchRecursive()) {
            identityMappingBuilder.searchRecursive();
        }

        LdapSecurityRealmBuilder ldapSecurityRealmBuilder = identityMappingBuilder
                .map(createAttributeMappings(runtimeConfig.identityMapping()))
                .setRdnIdentifier(runtimeConfig.identityMapping().rdnIdentifier())
                .setSearchDn(runtimeConfig.identityMapping().searchBaseDn())
                .build();

        if (runtimeConfig.directVerification()) {
            ldapSecurityRealmBuilder.addDirectEvidenceVerification(false);
        }

        SecurityRealm ldapRealm = ldapSecurityRealmBuilder.build();

        if (runtimeConfig.cache().enabled()) {
            if (ldapRealm instanceof CacheableSecurityRealm) {
                ldapRealm = new CachingSecurityRealm(ldapRealm,
                        new LRURealmIdentityCache(runtimeConfig.cache().size(), runtimeConfig.cache().maxAge().toMillis()));
            } else {
                log.warn(
                        "Created LDAP realm is not cacheable. Caching of the 'SecurityRealm' won't be available. Please, report this issue.");
            }
        }

        return ldapRealm;
    }

    private static ExceptionSupplier<DirContext, NamingException> createDirContextSupplier(DirContextConfig dirContext,
            TlsConfigurationRegistry tlsRegistry) {
        DirContextFactory dirContextFactory = new QuarkusDirContextFactory(
                dirContext.url(),
                dirContext.principal().orElse(null),
                dirContext.password().orElse(null),
                dirContext.connectTimeout(),
                dirContext.readTimeout(),
                configureTls(dirContext, tlsRegistry));
        return () -> dirContextFactory.obtainDirContext(dirContext.referralMode());
    }

    private static boolean configureTls(DirContextConfig dirContext, TlsConfigurationRegistry tlsRegistry) {
        if (dirContext.tlsConfigurationName().isEmpty()) {
            return false;
        }
        String name = dirContext.tlsConfigurationName().get();
        checkLdapsUrls(dirContext.url(), name);
        TlsConfiguration tlsConfiguration = tlsRegistry.get(name)
                .orElseThrow(() -> new IllegalArgumentException("Unable to find the TLS configuration '" + name
                        + "' set with 'quarkus.security.ldap.dir-context.tls-configuration-name': "
                        + "check that 'quarkus.tls." + name + ".*' is configured"));
        QuarkusLdapSocketFactory.configure(name, tlsConfiguration);
        return true;
    }

    /**
     * Checks that every URL of the dir context uses the {@code ldaps} scheme. The socket factory of a TLS configuration
     * opens TLS connections only, so a plain {@code ldap} URL cannot work with it.
     *
     * @param url the space-separated URLs of {@code quarkus.security.ldap.dir-context.url}
     * @param tlsConfigurationName the name of the TLS configuration
     */
    static void checkLdapsUrls(String url, String tlsConfigurationName) {
        for (String value : url.trim().split("\\s+")) {
            if (!value.regionMatches(true, 0, "ldaps://", 0, "ldaps://".length())) {
                throw new IllegalArgumentException("The URL '" + value + "' of 'quarkus.security.ldap.dir-context.url' "
                        + "does not use the 'ldaps' scheme, but 'quarkus.security.ldap.dir-context.tls-configuration-name' "
                        + "is set to '" + tlsConfigurationName
                        + "': use an 'ldaps://' URL, or remove the TLS configuration name");
            }
        }
    }

    private static AttributeMapping[] createAttributeMappings(IdentityMappingConfig identityMappingConfig) {
        List<AttributeMapping> attributeMappings = new ArrayList<>();

        for (AttributeMappingConfig attributeMappingConfig : identityMappingConfig.attributeMappings().values()) {
            attributeMappings.add(AttributeMapping.fromFilter(attributeMappingConfig.filter())
                    .from(attributeMappingConfig.from())
                    .to(attributeMappingConfig.to())
                    .searchDn(attributeMappingConfig.filterBaseDn())
                    .build());
        }

        AttributeMapping[] attributeMappingsArray = new AttributeMapping[attributeMappings.size()];
        return attributeMappings.toArray(attributeMappingsArray);
    }
}
