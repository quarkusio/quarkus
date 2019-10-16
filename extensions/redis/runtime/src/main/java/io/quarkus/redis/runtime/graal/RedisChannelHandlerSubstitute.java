package io.quarkus.redis.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.quarkus.redis.runtime.commands.QuarkusRedisSyncCommand;

@TargetClass(RedisChannelHandler.class)
final public class RedisChannelHandlerSubstitute {

    /**
     * Return a Sync command handler which delegates invocation calls to {@link RedisAsyncCommands}
     * This avoid the usage of {@link java.lang.invoke.MethodHandle}
     */
    @Substitute
    @SuppressWarnings("unchecked")
    protected <T> T syncHandler(Object asyncApi, Class<?>... interfaces) {
        return (T) new QuarkusRedisSyncCommand((RedisAsyncCommands) asyncApi);
    }
}
