package io.quarkus.redis.lettuce.runtime.internal.bitmap;

import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.offset;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.signed;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.typeWidthBasedOffset;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.unsigned;
import static io.quarkus.redis.datasource.bitmap.BitFieldArgs.OverflowType.WRAP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.bitmap.BitMapCommands;
import io.quarkus.redis.datasource.bitmap.ReactiveBitMapCommands;
import io.quarkus.redis.lettuce.runtime.internal.CommandsTestBase;
import tools.jackson.core.type.TypeReference;

class LettuceBitMapCommandsTest extends CommandsTestBase {

    ReactiveRedisDataSource reactiveDs;
    RedisDataSource blockingDs;
    ReactiveBitMapCommands<String> reactiveBitMap;
    BitMapCommands<String> blockingBitMap;

    @BeforeEach
    void initialize() {
        reactiveDs = reactiveDataSource();
        blockingDs = blockingDataSource();
        reactiveBitMap = reactiveDs.bitmap(String.class);
        blockingBitMap = blockingDs.bitmap(String.class);
    }

    @Test
    void getDataSource() {
        assertThat(reactiveDs).isEqualTo(reactiveBitMap.getDataSource());
        assertThat(blockingDs).isEqualTo(blockingBitMap.getDataSource());
    }

    @Test
    void bitcount() {
        assertThat(blockingBitMap.bitcount(key)).isEqualTo(0);

        blockingBitMap.setbit(key, 0L, 1);
        blockingBitMap.setbit(key, 1L, 1);
        blockingBitMap.setbit(key, 2L, 1);

        assertThat(blockingBitMap.bitcount(key)).isEqualTo(3);
        assertThat(blockingBitMap.bitcount(key, 3, -1)).isEqualTo(0);
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

        List<Long> values = blockingBitMap.bitfield(key, bitFieldArgs);

        assertThat(values).containsExactly(0L, 32L, 3L, 0L, 3L);
    }

    @Test
    void bitfieldGetWithOffset() {
        BitFieldArgs bitFieldArgs = new BitFieldArgs().set(signed(8), 0, 1).get(signed(2), typeWidthBasedOffset(1));
        List<Long> values = blockingBitMap.bitfield(key, bitFieldArgs);
        assertThat(values).containsExactly(0L, 0L);
    }

    @Test
    void bitfieldSet() {
        BitFieldArgs bitFieldArgs = new BitFieldArgs().set(signed(8), 0, 5).set(5);
        List<Long> values = blockingBitMap.bitfield(key, bitFieldArgs);
        assertThat(values).containsExactly(0L, 5L);
    }

    @Test
    void bitfieldWithOffsetSet() {
        // #2 with an 8-bit type is absolute bit 16; reading back there catches a dropped `#` (which would write at bit 2)
        assertThat(blockingBitMap.bitfield(key,
                new BitFieldArgs().set(signed(8), typeWidthBasedOffset(2), 5).get(signed(8), 16)))
                .containsExactly(0L, 5L);
        blockingDs.key(String.class).del(key);
        assertThat(blockingBitMap.bitfield(key,
                new BitFieldArgs().set(signed(8), offset(2), 5).get(signed(8), 2).get(signed(8), typeWidthBasedOffset(2))))
                .containsExactly(0L, 5L, 0L);
    }

    @Test
    void bitfieldIncrBy() {
        BitFieldArgs bitFieldArgs = new BitFieldArgs().set(signed(8), 0, 5).incrBy(1);
        List<Long> values = blockingBitMap.bitfield(key, bitFieldArgs);
        assertThat(values).containsExactly(0L, 6L);
    }

    @Test
    void bitfieldWithOffsetIncrBy() {
        // #2 with an 8-bit type is absolute bit 16; reading back there catches a dropped `#` (which would write at bit 2)
        assertThat(blockingBitMap.bitfield(key,
                new BitFieldArgs().incrBy(signed(8), typeWidthBasedOffset(2), 1).get(signed(8), 16)))
                .containsExactly(1L, 1L);
        blockingDs.key(String.class).del(key);
        assertThat(blockingBitMap.bitfield(key,
                new BitFieldArgs().incrBy(signed(8), offset(2), 1).get(signed(8), 2).get(signed(8), typeWidthBasedOffset(2))))
                .containsExactly(1L, 1L, 0L);
    }

    @Test
    void bitfieldOverflow() {
        BitFieldArgs bitFieldArgs = new BitFieldArgs().overflow(WRAP).set(signed(8), 9, Integer.MAX_VALUE).get(signed(8));
        List<Long> values = blockingBitMap.bitfield(key, bitFieldArgs);
        assertThat(values).containsExactly(0L, 0L);
    }

    @Test
    void bitpos() {
        assertThat(blockingBitMap.bitcount(key)).isEqualTo(0);
        blockingBitMap.setbit(key, 0L, 0);
        blockingBitMap.setbit(key, 1L, 1);
        assertThat(blockingBitMap.bitpos(key, 1)).isEqualTo(1);
    }

    @Test
    void bitposOffset() {
        assertThat(blockingBitMap.bitcount(key)).isEqualTo(0);
        blockingBitMap.setbit(key, 0, 1);
        blockingBitMap.setbit(key, 1, 1);
        blockingBitMap.setbit(key, 2, 0);
        blockingBitMap.setbit(key, 3, 0);
        blockingBitMap.setbit(key, 4, 0);
        blockingBitMap.setbit(key, 5, 1);
        blockingBitMap.setbit(key, 16, 1);

        assertThat(blockingBitMap.getbit(key, 1)).isEqualTo(1);
        assertThat(blockingBitMap.getbit(key, 4)).isEqualTo(0);
        assertThat(blockingBitMap.getbit(key, 5)).isEqualTo(1);
        assertThat(blockingBitMap.bitpos(key, 1, 1)).isEqualTo(16);
        assertThat(blockingBitMap.bitpos(key, 0, 0, 0)).isEqualTo(2);
    }

    @Test
    void bitopAnd() {
        blockingBitMap.setbit("foo", 0, 1);
        blockingBitMap.setbit("bar", 1, 1);
        blockingBitMap.setbit("baz", 2, 1);
        assertThat(blockingBitMap.bitopAnd(key, "foo", "bar", "baz")).isEqualTo(1);
        assertThat(blockingBitMap.bitcount(key)).isEqualTo(0);
    }

    @Test
    void bitopNot() {
        blockingBitMap.setbit("foo", 0, 1);
        blockingBitMap.setbit("foo", 2, 1);

        assertThat(blockingBitMap.bitopNot(key, "foo")).isEqualTo(1);
        assertThat(blockingBitMap.bitcount(key)).isEqualTo(6);
    }

    @Test
    void bitopOr() {
        blockingBitMap.setbit("foo", 0, 1);
        blockingBitMap.setbit("bar", 1, 1);
        blockingBitMap.setbit("baz", 2, 1);
        assertThat(blockingBitMap.bitopOr(key, "foo", "bar", "baz")).isEqualTo(1);
    }

    @Test
    void bitopXor() {
        blockingBitMap.setbit("foo", 0, 1);
        blockingBitMap.setbit("bar", 0, 1);
        blockingBitMap.setbit("baz", 2, 1);
        assertThat(blockingBitMap.bitopXor(key, "foo", "bar", "baz")).isEqualTo(1);
    }

    @Test
    void getbit() {
        assertThat(blockingBitMap.getbit(key, 0)).isEqualTo(0);
        blockingBitMap.setbit(key, 0, 1);
        assertThat(blockingBitMap.getbit(key, 0)).isEqualTo(1);
    }

    @Test
    void setbit() {
        assertThat(blockingBitMap.setbit(key, 0, 1)).isEqualTo(0);
        assertThat(blockingBitMap.setbit(key, 0, 0)).isEqualTo(1);
    }

    @Test
    void bitcountWithTypeReference() {
        var blockingBitMap = blockingDs.bitmap(new TypeReference<List<String>>() {
            // Empty on purpose
        });
        List<String> key = List.of("a", "b", "c");
        assertThat(blockingBitMap.bitcount(key)).isEqualTo(0);

        blockingBitMap.setbit(key, 0L, 1);
        blockingBitMap.setbit(key, 1L, 1);
        blockingBitMap.setbit(key, 2L, 1);

        assertThat(blockingBitMap.bitcount(key)).isEqualTo(3);
        assertThat(blockingBitMap.bitcount(key, 3, -1)).isEqualTo(0);
    }

    @Test
    void apiReturnsImmutableCollections() {
        assertImmutable(blockingBitMap.bitfield(key, new BitFieldArgs().set(signed(8), 0, 1).get().get(2)));
    }

}
