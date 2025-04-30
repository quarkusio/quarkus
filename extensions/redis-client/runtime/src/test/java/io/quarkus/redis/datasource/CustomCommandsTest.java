package io.quarkus.redis.datasource;

import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.list.ListCommands;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;

public class CustomCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    static String key = "key-generic";
    private ListCommands<String, Person> lists;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(5));

    }

    @AfterEach
    public void clear() {
        ds.flushall();
    }

    @Test
    void testWithMutinyCommand() {
        Response response = ds.execute(Command.HSET, key, "field", "hello");
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(ds.hash(String.class, String.class, String.class).hget(key, "field")).isEqualTo("hello");
    }

    @Test
    void testBare() {
        Command cmd = Command.HSET;
        redis.send(Request.cmd(cmd).arg("my-key").arg("my-field").arg("value")).await().indefinitely();
    }

    @Test
    void testWithBareCommand() {
        io.vertx.redis.client.Command cmd = io.vertx.redis.client.Command.HSET;
        ds.execute(cmd, key, "field", "hello-bare");

        Assertions.assertThat(ds.hash(String.class, String.class, String.class).hget(key, "field")).isEqualTo("hello-bare");
    }

    @Test
    void testCommandInTransaction() {
        TransactionResult result = ds.withTransaction(tx -> tx.execute(Command.HSET, key, "a", "b"));
        Assertions.assertThat(ds.hash(String.class, String.class, String.class).hget(key, "a")).isEqualTo("b");
        Assertions.assertThat(result.size()).isEqualTo(1);
    }
}
