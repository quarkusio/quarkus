package io.quarkus.elytron.security.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.auth.realm.ldap.AttributeMapping;
import org.wildfly.security.auth.realm.ldap.DirContextFactory;
import org.wildfly.security.auth.realm.ldap.LdapSecurityRealmBuilder;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.elytron.security.ldap.config.AttributeMappingConfig;
import io.quarkus.elytron.security.ldap.config.DirContextConfig;
import io.quarkus.elytron.security.ldap.config.IdentityMappingConfig;
import io.quarkus.elytron.security.ldap.config.LdapSecurityRealmConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LdapRecorder {

    /**
     * Create a runtime value for a {@linkplain LdapSecurityRealm}
     *
     * @param config - the realm config
     * @return - runtime value wrapper for the SecurityRealm
     */
    public RuntimeValue<SecurityRealm> createRealm(LdapSecurityRealmConfig config) {
        LdapSecurityRealmBuilder builder = LdapSecurityRealmBuilder.builder()
                .setDirContextSupplier(createDirContextSupplier(config.dirContext))
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
        DirContextFactory dirContextFactory = new QuarkusDirContextFactory(
                dirContext.url,
                dirContext.principal,
                dirContext.password);
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
