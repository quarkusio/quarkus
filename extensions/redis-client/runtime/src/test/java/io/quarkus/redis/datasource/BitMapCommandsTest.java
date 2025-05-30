package io.quarkus.redis.datasource;

import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.OverflowType.WRAP;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.offset;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.signed;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.typeWidthBasedOffset;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.unsigned;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.bitmap.BitMapCommands;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;

public class BitMapCommandsTest extends DatasourceTestBase {

    private RedisDataSource ds;
    private BitMapCommands<String> bitmap;

    @BeforeEach
    void initialize() {
        ds = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(1));
        bitmap = ds.bitmap();
    }

    @AfterEach
    void clear() {
        ds.flushall();
    }

    @Test
    void getDataSource() {
        assertThat(ds).isEqualTo(bitmap.getDataSource());
    }

    @Test
    void bitcount() {
        assertThat(bitmap.bitcount(key)).isEqualTo(0);

        bitmap.setbit(key, 0L, 1);
        bitmap.setbit(key, 1L, 1);
        bitmap.setbit(key, 2L, 1);

        assertThat(bitmap.bitcount(key)).isEqualTo(3);
        assertThat(bitmap.bitcount(key, 3, -1)).isEqualTo(0);
    }

    @Test
    void bitfieldType() {
        assertThat(signed(64).bits).isEqualTo(64);
        assertThat(signed(64).signed).isTrue();
        assertThat(unsigned(63).bits).isEqualTo(63);
        assertThat(unsigned(63).signed).isFalse();
    }

    @Test
    void bitfieldTypeSigned65() {
        assertThatThrownBy(() -> signed(65)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bitfieldTypeUnsigned64() {
        assertThatThrownBy(() -> unsigned(64)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bitfieldBuilderEmptyPreviousType() {
        assertThatThrownBy(() -> new BitFieldArgs().overflow(WRAP).get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void bitfieldArgsTest() {
        assertThat(signed(5).toString()).isEqualTo("i5");
        assertThat(unsigned(5).toString()).isEqualTo("u5");

        assertThat(Offset.offset(5).value).isEqualTo(5);
        assertThat(typeWidthBasedOffset(5).toString()).isEqualTo("#5");
    }

    @Test
    void bitfield() {
        BitFieldArgs bitFieldArgs = new BitFieldArgs().set(signed(8), 0, 1).set(5, 1).incrBy(2, 3).get().get(2);

        List<Long> values = bitmap.bitfield(key, bitFieldArgs);

        assertThat(values).containsExactly(0L, 32L, 3L, 0L, 3L);
    }

    @Test
    void bitfieldGetWithOffset() {
        BitFieldArgs bitFieldArgs = new BitFieldArgs().set(signed(8), 0, 1).get(signed(2), typeWidthBasedOffset(1));
        List<Long> values = bitmap.bitfield(key, bitFieldArgs);
        assertThat(values).containsExactly(0L, 0L);
    }

    @Test
    void bitfieldSet() {
        BitFieldArgs bitFieldArgs = new BitFieldArgs().set(signed(8), 0, 5).set(5);
        List<Long> values = bitmap.bitfield(key, bitFieldArgs);
        assertThat(values).containsExactly(0L, 5L);
    }

    @Test
    void bitfieldWithOffsetSet() {
        bitmap.bitfield(key, new BitFieldArgs().set(signed(8), typeWidthBasedOffset(2), 5));
        ds.key(String.class).del(key);
        bitmap.bitfield(key, new BitFieldArgs().set(signed(8), offset(2), 5));
    }

    @Test
    void bitfieldIncrBy() {
        BitFieldArgs bitFieldArgs = new BitFieldArgs().set(signed(8), 0, 5).incrBy(1);
        List<Long> values = bitmap.bitfield(key, bitFieldArgs);
        assertThat(values).containsExactly(0L, 6L);
    }

    @Test
    void bitfieldWithOffsetIncrBy() {
        bitmap.bitfield(key, new BitFieldArgs().incrBy(signed(8), typeWidthBasedOffset(2), 1));
        ds.key(String.class).del(key);
        bitmap.bitfield(key, new BitFieldArgs().incrBy(signed(8), offset(2), 1));
    }

    @Test
    void bitfieldOverflow() {
        BitFieldArgs bitFieldArgs = new BitFieldArgs().overflow(WRAP).set(signed(8), 9, Integer.MAX_VALUE).get(signed(8));
        List<Long> values = bitmap.bitfield(key, bitFieldArgs);
        assertThat(values).containsExactly(0L, 0L);
    }

    @Test
    void bitpos() {
        assertThat(bitmap.bitcount(key)).isEqualTo(0);
        bitmap.setbit(key, 0L, 0);
        bitmap.setbit(key, 1L, 1);
        assertThat(bitmap.bitpos(key, 1)).isEqualTo(1);
    }

    @Test
    void bitposOffset() {
        assertThat(bitmap.bitcount(key)).isEqualTo(0);
        bitmap.setbit(key, 0, 1);
        bitmap.setbit(key, 1, 1);
        bitmap.setbit(key, 2, 0);
        bitmap.setbit(key, 3, 0);
        bitmap.setbit(key, 4, 0);
        bitmap.setbit(key, 5, 1);
        bitmap.setbit(key, 16, 1);

        assertThat(bitmap.getbit(key, 1)).isEqualTo(1);
        assertThat(bitmap.getbit(key, 4)).isEqualTo(0);
        assertThat(bitmap.getbit(key, 5)).isEqualTo(1);
        assertThat(bitmap.bitpos(key, 1, 1)).isEqualTo(16);
        assertThat(bitmap.bitpos(key, 0, 0, 0)).isEqualTo(2);
    }

    @Test
    void bitopAnd() {
        bitmap.setbit("foo", 0, 1);
        bitmap.setbit("bar", 1, 1);
        bitmap.setbit("baz", 2, 1);
        assertThat(bitmap.bitopAnd(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat(bitmap.bitcount(key)).isEqualTo(0);
    }

    @Test
    void bitopNot() {
        bitmap.setbit("foo", 0, 1);
        bitmap.setbit("foo", 2, 1);

        assertThat(bitmap.bitopNot(key, "foo")).isEqualTo(1);
        assertThat(bitmap.bitcount(key)).isEqualTo(6);
    }

    @Test
    void bitopOr() {
        bitmap.setbit("foo", 0, 1);
        bitmap.setbit("bar", 1, 1);
        bitmap.setbit("baz", 2, 1);
        assertThat(bitmap.bitopOr(key, "foo", "bar", "baz")).isEqualTo(1);
    }

    @Test
    void bitopXor() {
        bitmap.setbit("foo", 0, 1);
        bitmap.setbit("bar", 0, 1);
        bitmap.setbit("baz", 2, 1);
        assertThat(bitmap.bitopXor(key, "foo", "bar", "baz")).isEqualTo(1);
    }

    @Test
    void getbit() {
        assertThat(bitmap.getbit(key, 0)).isEqualTo(0);
        bitmap.setbit(key, 0, 1);
        assertThat(bitmap.getbit(key, 0)).isEqualTo(1);
    }

    @Test
    void setbit() {
        assertThat(bitmap.setbit(key, 0, 1)).isEqualTo(0);
        assertThat(bitmap.setbit(key, 0, 0)).isEqualTo(1);
    }

    @Test
    void bitcountWithTypeReference() {
        var bitmap = ds.bitmap(new TypeReference<List<String>>() {
            // Empty on purpose
        });
        List<String> key = List.of("a", "b", "c");
        assertThat(bitmap.bitcount(key)).isEqualTo(0);

        bitmap.setbit(key, 0L, 1);
        bitmap.setbit(key, 1L, 1);
        bitmap.setbit(key, 2L, 1);

        assertThat(bitmap.bitcount(key)).isEqualTo(3);
        assertThat(bitmap.bitcount(key, 3, -1)).isEqualTo(0);
    }
}
