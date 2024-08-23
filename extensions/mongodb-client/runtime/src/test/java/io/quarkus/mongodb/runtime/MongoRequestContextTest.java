package io.quarkus.mongodb.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import io.opentelemetry.context.Context;

public class MongoRequestContextTest {

    @Test
    void nonNulContext() {
        Context context = Context.current();
        MongoRequestContext requestContext = new MongoRequestContext(context);

        assertThat((Context) requestContext.get(MongoRequestContext.OTEL_CONTEXT_KEY)).isSameAs(context);
        assertThat(requestContext.isEmpty()).isFalse();
        assertThat(requestContext.size()).isOne();
    }

    @Test
    void withNullContext() {
        MongoRequestContext requestContext = new MongoRequestContext(null);

        assertThat((Context) requestContext.get(MongoRequestContext.OTEL_CONTEXT_KEY)).isNull();
        assertThat(requestContext.isEmpty()).isTrue();
        assertThat(requestContext.size()).isZero();
    }

    @Test
    void testPutAndGet() {
        MongoRequestContext requestContext = new MongoRequestContext(null);
        String key = "testKey";
        String value = "testValue";

        requestContext.put(key, value);
        assertThat((String) requestContext.get(key)).isEqualTo(value);
        assertThat(requestContext.hasKey(key)).isTrue();
        assertThat(requestContext.hasKey("tmp")).isFalse();
    }

    @Test
    void testDelete() {
        MongoRequestContext requestContext = new MongoRequestContext(null);
        String key = "testKey";
        String value = "testValue";

        requestContext.put(key, value);
        assertThat((String) requestContext.get(key)).isEqualTo(value);
        assertThat(requestContext.size()).isOne();

        requestContext.delete(key);
        assertThat((String) requestContext.get(key)).isNull();
        assertThat(requestContext.hasKey(key)).isFalse();
        assertThat(requestContext.size()).isZero();
    }

    @Test
    void testStream() {
        MongoRequestContext requestContext = new MongoRequestContext(null);
        requestContext.put("testKey1", "testValue1");
        requestContext.put("testKey2", "testValue2");

        Map<Object, Object> map = requestContext.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertThat(map).containsExactlyInAnyOrderEntriesOf(new ConcurrentHashMap<>() {
            {
                put("testKey1", "testValue1");
                put("testKey2", "testValue2");
            }
        });
    }
}
