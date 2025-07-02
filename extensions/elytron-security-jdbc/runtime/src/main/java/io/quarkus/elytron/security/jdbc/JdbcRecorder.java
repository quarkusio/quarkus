package io.quarkus.elytron.security.jdbc;

import java.security.Provider;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.wildfly.security.auth.realm.jdbc.JdbcSecurityRealm;
import org.wildfly.security.auth.realm.jdbc.JdbcSecurityRealmBuilder;
import org.wildfly.security.auth.realm.jdbc.QueryBuilder;
import org.wildfly.security.auth.realm.jdbc.mapper.AttributeMapper;
import org.wildfly.security.auth.server.SecurityRealm;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class JdbcRecorder {
    private static final Provider[] PROVIDERS = new Provider[] { new WildFlyElytronPasswordProvider() };

    private final RuntimeValue<JdbcSecurityRealmRuntimeConfig> runtimeConfig;

    public JdbcRecorder(final RuntimeValue<JdbcSecurityRealmRuntimeConfig> runtimeConfig) {
        this.runtimeConfig = runtimeConfig;
    }

    /**
     * Create a runtime value for a {@linkplain JdbcSecurityRealm}
     *
     * @return - runtime value wrapper for the SecurityRealm
     */
    public RuntimeValue<SecurityRealm> createRealm() {
        Supplier<Provider[]> providers = new Supplier<Provider[]>() {
            @Override
            public Provider[] get() {
                return PROVIDERS;
            }
        };
        JdbcSecurityRealmBuilder builder = JdbcSecurityRealm.builder().setProviders(providers);
        PrincipalQueriesConfig principalQueries = runtimeConfig.getValue().principalQueries();
        registerPrincipalQuery(principalQueries.defaultPrincipalQuery(), builder);
        principalQueries.namedPrincipalQueries()
                .forEach((name, principalQuery) -> registerPrincipalQuery(principalQuery, builder));
        return new RuntimeValue<>(builder.build());
    }

    private void registerPrincipalQuery(PrincipalQueryConfig principalQuery, JdbcSecurityRealmBuilder builder) {

        QueryBuilder queryBuilder = builder.principalQuery(principalQuery.sql().orElseThrow(
                () -> new IllegalStateException("quarkus.security.jdbc.principal-query.sql property must be set")))
                .from(getDataSource(principalQuery));

        AttributeMapper[] mappers = principalQuery.attributeMappings().entrySet()
                .stream()
                .map(entry -> new AttributeMapper(entry.getValue().index(), entry.getValue().to()))
                .toArray(size -> new AttributeMapper[size]);
        queryBuilder.withMapper(mappers);

        if (principalQuery.clearPasswordMapperConfig().enabled()) {
            queryBuilder.withMapper(principalQuery.clearPasswordMapperConfig().toPasswordKeyMapper());
        }
        if (principalQuery.bcryptPasswordKeyMapperConfig().enabled()) {
            queryBuilder.withMapper(principalQuery.bcryptPasswordKeyMapperConfig().toPasswordKeyMapper());
        }
    }

    private DataSource getDataSource(PrincipalQueryConfig principalQuery) {
        if (principalQuery.datasource().isPresent()) {
            return Arc.container()
                    .instance(DataSource.class,
                            new io.quarkus.agroal.DataSource.DataSourceLiteral(principalQuery.datasource().get()))
                    .get();
        }

        return Arc.container().instance(DataSource.class).get();
    }
}
