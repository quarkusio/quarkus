package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.string.StringCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

public class NumericCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;

    static String key = "key-sort";
    private StringCommands<String, Long> num;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(redis, api, Duration.ofSeconds(5));
        num = ds.string(Long.class);
    }

    @AfterEach
    public void clear() {
        ds.flushall();
    }

    @Test
    void decr() {
        assertThat(num.decr(key)).isEqualTo(-1);
        assertThat(num.decr(key)).isEqualTo(-2);
    }

    @Test
    void decrby() {
        assertThat(num.decrby(key, 3)).isEqualTo(-3);
        assertThat(num.decrby(key, 3)).isEqualTo(-6);
    }

    @Test
    void incr() {
        assertThat(num.incr(key)).isEqualTo(1);
        assertThat(num.incr(key)).isEqualTo(2);
    }

    @Test
    void incrby() {
        assertThat(num.incrby(key, 3)).isEqualTo(3);
        assertThat(num.incrby(key, 3)).isEqualTo(6);
    }

    @Test
    void incrbyfloat() {
        assertThat(num.incrbyfloat(key, 3.0)).isEqualTo(3.0, offset(0.1));
        assertThat(num.incrbyfloat(key, 0.2)).isEqualTo(3.2, offset(0.1));
    }

}
