package io.quarkus.redis.lettuce.runtime.internal;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;
import io.vertx.core.buffer.Buffer;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.impl.types.BooleanType;
import io.vertx.redis.client.impl.types.BulkType;
import io.vertx.redis.client.impl.types.MultiType;
import io.vertx.redis.client.impl.types.NumberType;

public final class VertxResponseParser implements ComplexDataParser<Response> {

    public static final VertxResponseParser INSTANCE = new VertxResponseParser();

    private VertxResponseParser() {
        // Singleton
    }

    @Override
    public Response parse(ComplexData data) {
        return toResponse(data);
    }

    public static Response toResponse(Object element) {
        return switch (element) {
            case null -> null;
            case ComplexData data -> {
                if (data.isMap()) {
                    Map<Object, Object> entries = data.getDynamicMap();
                    MultiType multi = MultiType.create(entries.size(), true);
                    for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                        multi.add(toResponse(entry.getKey()));
                        multi.add(toResponse(entry.getValue()));
                    }
                    yield multi;
                }
                Collection<Object> items = data.isSet() ? data.getDynamicSet() : data.getDynamicList();
                MultiType multi = MultiType.create(items.size(), false);
                for (Object item : items) {
                    multi.add(toResponse(item));
                }
                yield multi;
            }
            case ByteBuffer buffer -> {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.duplicate().get(bytes);
                yield BulkType.create(Buffer.buffer(bytes), false);
            }
            case byte[] bytes -> BulkType.create(Buffer.buffer(bytes), false);
            case String string -> BulkType.create(Buffer.buffer(string), false);
            case Number number -> NumberType.create(number);
            case Boolean bool -> BooleanType.create(bool);
            default -> throw new IllegalStateException("Unsupported reply element: " + element.getClass().getName());
        };
    }

}
