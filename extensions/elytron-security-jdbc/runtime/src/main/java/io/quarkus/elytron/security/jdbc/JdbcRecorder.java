package io.quarkus.elytron.security.jdbc;

import java.security.Provider;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.realm.jdbc.JdbcSecurityRealm;
import org.wildfly.security.auth.realm.jdbc.JdbcSecurityRealmBuilder;
import org.wildfly.security.auth.realm.jdbc.QueryBuilder;
import org.wildfly.security.auth.realm.jdbc.mapper.AttributeMapper;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JdbcRecorder {

    private static final Provider[] PROVIDERS = new Provider[] { new WildFlyElytronProvider() };

    /**
     * Create a runtime value for a {@linkplain JdbcSecurityRealm}
     *
     * @param config - the realm config
     * @return - runtime value wrapper for the SecurityRealm
     */
    public RuntimeValue<SecurityRealm> createRealm(JdbcSecurityRealmConfig config) {
        Supplier<Provider[]> providers = new Supplier<Provider[]>() {
            @Override
            public Provider[] get() {
                return PROVIDERS;
            }
        };
        JdbcSecurityRealmBuilder builder = JdbcSecurityRealm.builder().setProviders(providers);
        PrincipalQueriesConfig principalQueries = config.principalQueries;
        registerPrincipalQuery(principalQueries.defaultPrincipalQuery, builder);
        principalQueries.namedPrincipalQueries
                .forEach((name, principalQuery) -> registerPrincipalQuery(principalQuery, builder));
        return new RuntimeValue<>(builder.build());
    }

    private void registerPrincipalQuery(PrincipalQueryConfig principalQuery, JdbcSecurityRealmBuilder builder) {
        DataSource dataSource = (DataSource) principalQuery.datasource
                .map(name -> Arc.container().instance(name).get())
                .orElse(Arc.container().instance(DataSource.class).get());

        QueryBuilder queryBuilder = builder.principalQuery(principalQuery.sql).from(dataSource);

        AttributeMapper[] mappers = principalQuery.attributeMappings.entrySet()
                .stream()
                .map(entry -> new AttributeMapper(entry.getValue().index, entry.getValue().to))
                .toArray(size -> new AttributeMapper[size]);
        queryBuilder.withMapper(mappers);

        if (principalQuery.clearPasswordMapperConfig.enabled) {
            queryBuilder.withMapper(principalQuery.clearPasswordMapperConfig.toPasswordKeyMapper());
        }
        if (principalQuery.bcryptPasswordKeyMapperConfig.enabled) {
            queryBuilder.withMapper(principalQuery.bcryptPasswordKeyMapperConfig.toPasswordKeyMapper());
        }
    }
}
