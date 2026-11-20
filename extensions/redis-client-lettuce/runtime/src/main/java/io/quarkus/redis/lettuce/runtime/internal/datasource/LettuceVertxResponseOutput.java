package io.quarkus.redis.lettuce.runtime.internal.datasource;

import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.NestedMultiOutput;
import io.vertx.core.buffer.Buffer;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.impl.types.BooleanType;
import io.vertx.redis.client.impl.types.BulkType;
import io.vertx.redis.client.impl.types.MultiType;
import io.vertx.redis.client.impl.types.NumberType;

/**
 * Lettuce {@link io.lettuce.core.output.CommandOutput} that captures arbitrary Redis
 * replies and converts them into Vert.x {@link Response} instances.
 * <p>
 * Used by the Lettuce-backed data source to implement {@code execute(...)} while keeping
 * the public {@code Uni<Response>} return type.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
final class LettuceVertxResponseOutput<K, V> extends NestedMultiOutput<K, V> {

    private boolean wasArray;

    LettuceVertxResponseOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public void multi(int count) {
        wasArray = true;
        super.multi(count);
    }

    Response toVertxResponse() {
        List<Object> raw = get();
        if (raw == null) {
            return null;
        }
        if (!wasArray) {
            if (raw.isEmpty()) {
                return null;
            }
            return convert(raw.get(0));
        }
        MultiType multi = MultiType.create(raw.size(), false);
        for (Object o : raw) {
            multi.add(convert(o));
        }
        return multi;
    }

    private static Response convert(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Long n) {
            return NumberType.create(n);
        }
        if (o instanceof Number n) {
            return NumberType.create(n);
        }
        if (o instanceof Boolean b) {
            return BooleanType.create(b);
        }
        if (o instanceof String s) {
            return BulkType.create(Buffer.buffer(s), false);
        }
        if (o instanceof byte[] bytes) {
            return BulkType.create(Buffer.buffer(bytes), false);
        }
        if (o instanceof List<?> list) {
            MultiType multi = MultiType.create(list.size(), false);
            for (Object item : list) {
                multi.add(convert(item));
            }
            return multi;
        }
        throw new IllegalStateException("Unsupported Lettuce response element type: " + o.getClass());
    }
}
