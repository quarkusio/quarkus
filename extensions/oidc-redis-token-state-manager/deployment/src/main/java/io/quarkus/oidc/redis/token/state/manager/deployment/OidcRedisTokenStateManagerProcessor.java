package io.quarkus.oidc.redis.token.state.manager.deployment;

import java.util.function.BooleanSupplier;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.oidc.TokenStateManager;
import io.quarkus.oidc.redis.token.state.manager.runtime.OidcRedisTokenStateManagerRecorder;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.deployment.client.RequestedRedisClientBuildItem;
import io.quarkus.redis.runtime.client.config.RedisConfig;

@BuildSteps(onlyIf = OidcRedisTokenStateManagerProcessor.IsEnabled.class)
public class OidcRedisTokenStateManagerProcessor {

    @BuildStep
    RequestedRedisClientBuildItem requestRedisClient(OidcRedisTokenStateManagerBuildConfig buildConfig) {
        return new RequestedRedisClientBuildItem(buildConfig.redisClientName());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    SyntheticBeanBuildItem createTokenStateManager(OidcRedisTokenStateManagerRecorder recorder,
            OidcRedisTokenStateManagerBuildConfig buildConfig) {
        var redisClientName = buildConfig.redisClientName();
        var beanConfigurator = SyntheticBeanBuildItem.configure(TokenStateManager.class)
                .priority(1)
                .alternative(true)
                .unremovable()
                .scope(ApplicationScoped.class);
        if (RedisConfig.isDefaultClient(redisClientName)) {
            beanConfigurator
                    .createWith(recorder.createTokenStateManager(null))
                    .addInjectionPoint(Type.create(ReactiveRedisDataSource.class));
        } else {
            beanConfigurator
                    .createWith(recorder.createTokenStateManager(redisClientName))
                    .addInjectionPoint(Type.create(ReactiveRedisDataSource.class),
                            AnnotationInstance.builder(RedisClientName.class).value(redisClientName).build());
        }
        return beanConfigurator.done();
    }

    static final class IsEnabled implements BooleanSupplier {

        private final boolean enabled;

        IsEnabled(OidcRedisTokenStateManagerBuildConfig buildConfig) {
            this.enabled = buildConfig.enabled();
        }

        @Override
        public boolean getAsBoolean() {
            return enabled;
        }
    }

}
