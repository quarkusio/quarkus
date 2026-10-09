package io.quarkus.oidc.redis.token.state.manager.runtime;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;

public class OidcRedisTokenStateManagerCreator implements BeanCreator<TokenStateManager> {

    public static final String REDIS_CLIENT_NAME_PARAM = "redisClientName";

    @Override
    public TokenStateManager create(SyntheticCreationalContext<TokenStateManager> context) {
        String clientName = (String) context.getParams().get(REDIS_CLIENT_NAME_PARAM);
        final ReactiveRedisDataSource dataSource;
        if (clientName == null) {
            dataSource = context.getInjectedReference(ReactiveRedisDataSource.class);
        } else {
            dataSource = context.getInjectedReference(ReactiveRedisDataSource.class,
                    RedisClientName.Literal.of(clientName));
        }
        return new OidcRedisTokenStateManager(dataSource);
    }
}
