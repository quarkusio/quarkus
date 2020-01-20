package io.quarkus.elytron.security.ldap;

import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.realm.ldap.AttributeMapping;
import org.wildfly.security.auth.realm.ldap.DirContextFactory;
import org.wildfly.security.auth.realm.ldap.LdapSecurityRealmBuilder;
import org.wildfly.security.auth.realm.ldap.SimpleDirContextFactoryBuilder;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.elytron.security.ldap.config.AttributeMappingConfig;
import io.quarkus.elytron.security.ldap.config.DirContextConfig;
import io.quarkus.elytron.security.ldap.config.IdentityMappingConfig;
import io.quarkus.elytron.security.ldap.config.LdapSecurityRealmConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LdapRecorder {

    private static final Provider[] PROVIDERS = new Provider[] { new WildFlyElytronProvider() };

    /**
     * Create a runtime value for a {@linkplain LdapSecurityRealm}
     *
     * @param config - the realm config
     * @return - runtime value wrapper for the SecurityRealm
     */
    public RuntimeValue<SecurityRealm> createRealm(LdapSecurityRealmConfig config) {
        Supplier<Provider[]> providers = new Supplier<Provider[]>() {
            @Override
            public Provider[] get() {
                return PROVIDERS;
            }
        };
        LdapSecurityRealmBuilder builder = LdapSecurityRealmBuilder.builder()
                .setDirContextSupplier(createDirContextSupplier(config.dirContext))
                .setProviders(providers)
                .identityMapping()
                .map(createAttributeMappings(config.identityMapping))
                .setRdnIdentifier(config.identityMapping.rdnIdentifier)
                .setSearchDn(config.identityMapping.searchBaseDn)
                .build();

        if (config.directVerification) {
            builder.addDirectEvidenceVerification(false);
        }

        return new RuntimeValue<>(builder.build());
    }

    private ExceptionSupplier<DirContext, NamingException> createDirContextSupplier(DirContextConfig dirContext) {
        DirContextFactory dirContextFactory = SimpleDirContextFactoryBuilder.builder()
                .setProviderUrl(dirContext.url)
                .setSecurityPrincipal(dirContext.principal)
                .setSecurityCredential(dirContext.password)
                .build();

        return () -> dirContextFactory.obtainDirContext(DirContextFactory.ReferralMode.IGNORE);
    }

    private AttributeMapping[] createAttributeMappings(IdentityMappingConfig identityMappingConfig) {
        List<AttributeMapping> attributeMappings = new ArrayList<>();

        for (AttributeMappingConfig attributeMappingConfig : identityMappingConfig.attributeMappings.values()) {
            attributeMappings.add(AttributeMapping.fromFilter(attributeMappingConfig.filter)
                    .from(attributeMappingConfig.from)
                    .to(attributeMappingConfig.to)
                    .searchDn(attributeMappingConfig.filterBaseDn)
                    .build());
        }

        AttributeMapping[] attributeMappingsArray = new AttributeMapping[attributeMappings.size()];
        return attributeMappings.toArray(attributeMappingsArray);
    }
}
