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
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LdapRecorder {
    private static final Logger log = LoggerFactory.getLogger(LdapRecorder.class);

    private final RuntimeValue<LdapSecurityRealmRuntimeConfig> runtimeConfig;

    public LdapRecorder(final RuntimeValue<LdapSecurityRealmRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Create a runtime value for a {@linkplain LdapSecurityRealm}
     *
     * @return runtime value wrapper for the SecurityRealm
     */
    public RuntimeValue<SecurityRealm> createRealm() {
        LdapSecurityRealmRuntimeConfig runtimeConfig = this.runtimeConfig.getValue();

        LdapSecurityRealmBuilder.IdentityMappingBuilder identityMappingBuilder = LdapSecurityRealmBuilder.builder()
                .setDirContextSupplier(createDirContextSupplier(runtimeConfig.dirContext()))
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

        return new RuntimeValue<>(ldapRealm);
    }

    private static ExceptionSupplier<DirContext, NamingException> createDirContextSupplier(DirContextConfig dirContext) {
        DirContextFactory dirContextFactory = new QuarkusDirContextFactory(
                dirContext.url(),
                dirContext.principal().orElse(null),
                dirContext.password().orElse(null),
                dirContext.connectTimeout(),
                dirContext.readTimeout());
        return () -> dirContextFactory.obtainDirContext(dirContext.referralMode());
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
