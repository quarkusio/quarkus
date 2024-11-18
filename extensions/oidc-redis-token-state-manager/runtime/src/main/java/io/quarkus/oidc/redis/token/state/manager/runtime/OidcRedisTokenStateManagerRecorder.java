package io.quarkus.oidc.redis.token.state.manager.runtime;

import java.util.function.Function;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OidcRedisTokenStateManagerRecorder {

    public Function<SyntheticCreationalContext<TokenStateManager>, TokenStateManager> createTokenStateManager(
            String clientName) {
        return new Function<>() {
            @Override
            public TokenStateManager apply(SyntheticCreationalContext<TokenStateManager> ctx) {
                final ReactiveRedisDataSource dataSource;
                if (clientName == null) {
                    dataSource = ctx.getInjectedReference(ReactiveRedisDataSource.class);
                } else {
                    dataSource = ctx.getInjectedReference(ReactiveRedisDataSource.class,
                            RedisClientName.Literal.of(clientName));
                }
                return new OidcRedisTokenStateManager(dataSource);
            }
        };
    }

}
