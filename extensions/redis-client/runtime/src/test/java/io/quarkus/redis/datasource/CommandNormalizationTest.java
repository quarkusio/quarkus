package io.quarkus.redis.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.runtime.datasource.RedisCommand;
import io.vertx.redis.client.impl.CommandImpl;
import io.vertx.redis.client.impl.RequestImpl;

public class CommandNormalizationTest {
    @Test
    void test() {
        RequestImpl req = (RequestImpl) RedisCommand.of(io.vertx.mutiny.redis.client.Command.create("hset"))
                .put("key").put("field").put("value").toRequest().getDelegate();
        CommandImpl cmd = (CommandImpl) req.command();
        assertEquals("hset", cmd.toString());
        assertNotEquals(-1, cmd.getArity());
        assertFalse(cmd.needsGetKeys());
        assertEquals(1, req.keys().size());
        assertEquals("key", new String(req.keys().get(0), StandardCharsets.UTF_8));
    }
}
