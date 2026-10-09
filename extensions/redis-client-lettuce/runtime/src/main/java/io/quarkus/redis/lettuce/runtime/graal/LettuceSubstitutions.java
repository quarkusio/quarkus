package io.quarkus.redis.lettuce.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.lettuce.core.api.ClusterPubSubCommandsFactory;
import io.lettuce.core.api.CommandsFactory;
import io.lettuce.core.api.PubSubCommandsFactory;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.pubsub.api.reactive.RedisClusterPubSubReactiveCommands;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;

/**
 * When Reactive Streams (and so Project Reactor) is not on the classpath, substitute the factories of Lettuce's reactive
 * command APIs with factories that throw an {@link UnsupportedOperationException}.
 * <p>
 * Lettuce makes Reactor optional, but its {@code reactive()} accessors stay reachable even if the application never calls
 * them, and they reach the constructors of the reactive command implementations through these factories. Linking those
 * implementations needs {@code org.reactivestreams.Publisher}, so a native image built without it fails. Lettuce creates
 * each factory in a {@code newFactory()} method of a {@code FactoryHolder} class precisely so that it can be substituted.
 * <p>
 * The {@code FactoryHolder} classes are initialized at run time (see {@code LettuceProcessor}): initialized at build time,
 * their static initializers would run the original {@code newFactory()} and store its factory in the image heap.
 */
@TargetClass(className = "io.lettuce.core.api.reactive.RedisReactiveCommands$FactoryHolder", onlyWith = ReactiveStreamsMissingSelector.class)
final class Target_RedisReactiveCommands_FactoryHolder {

    @Substitute
    @SuppressWarnings("rawtypes")
    private static CommandsFactory newFactory() {
        return CommandsFactory.of(RedisReactiveCommands.class, connection -> {
            throw LettuceSubstitutions.reactiveApiUnavailable();
        });
    }
}

@TargetClass(className = "io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands$FactoryHolder", onlyWith = ReactiveStreamsMissingSelector.class)
final class Target_RedisAdvancedClusterReactiveCommands_FactoryHolder {

    @Substitute
    @SuppressWarnings("rawtypes")
    private static CommandsFactory newFactory() {
        return CommandsFactory.of(RedisAdvancedClusterReactiveCommands.class, connection -> {
            throw LettuceSubstitutions.reactiveApiUnavailable();
        });
    }
}

@TargetClass(className = "io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands$FactoryHolder", onlyWith = ReactiveStreamsMissingSelector.class)
final class Target_RedisSentinelReactiveCommands_FactoryHolder {

    @Substitute
    @SuppressWarnings("rawtypes")
    private static CommandsFactory newFactory() {
        return CommandsFactory.of(RedisSentinelReactiveCommands.class, connection -> {
            throw LettuceSubstitutions.reactiveApiUnavailable();
        });
    }
}

@TargetClass(className = "io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands$FactoryHolder", onlyWith = ReactiveStreamsMissingSelector.class)
final class Target_RedisPubSubReactiveCommands_FactoryHolder {

    @Substitute
    @SuppressWarnings("rawtypes")
    private static PubSubCommandsFactory newFactory() {
        return PubSubCommandsFactory.of(RedisPubSubReactiveCommands.class, connection -> {
            throw LettuceSubstitutions.reactiveApiUnavailable();
        });
    }
}

@TargetClass(className = "io.lettuce.core.cluster.pubsub.api.reactive.RedisClusterPubSubReactiveCommands$FactoryHolder", onlyWith = ReactiveStreamsMissingSelector.class)
final class Target_RedisClusterPubSubReactiveCommands_FactoryHolder {

    @Substitute
    @SuppressWarnings("rawtypes")
    private static ClusterPubSubCommandsFactory newFactory() {
        return ClusterPubSubCommandsFactory.of(RedisClusterPubSubReactiveCommands.class, connection -> {
            throw LettuceSubstitutions.reactiveApiUnavailable();
        });
    }
}

final class ReactiveStreamsMissingSelector implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("org.reactivestreams.Publisher");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}

public class LettuceSubstitutions {

    static UnsupportedOperationException reactiveApiUnavailable() {
        return new UnsupportedOperationException(
                "The Lettuce reactive API requires Project Reactor (io.projectreactor:reactor-core), which is not on the"
                        + " classpath of this native image");
    }
}
