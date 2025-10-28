package io.quarkus.redis.runtime.client.graal;

import java.util.Random;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

class RedisClientSubstitutions {

}

@TargetClass(className = "io.vertx.redis.client.impl.SentinelTopology")
final class Target_io_vertx_redis_client_impl_SentinelTopology {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    static Random RANDOM;
}
