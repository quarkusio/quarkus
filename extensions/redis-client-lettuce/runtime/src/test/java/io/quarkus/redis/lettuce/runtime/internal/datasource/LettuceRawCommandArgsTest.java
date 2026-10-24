package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.CommandArgs;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.impl.CommandMap;

/**
 * The arguments of a raw {@code execute(...)} command carry their keys, so that a cluster connection routes the
 * command to the node owning the first one, the way the Vert.x cluster connection does.
 */
class LettuceRawCommandArgsTest {

    @Test
    void marksTheKeysOfKnownCommands() {
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(Command.GET, "k"))).isEqualTo("k");
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(Command.SET, "k", "v"))).isEqualTo("k");
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(Command.MSET, "k1", "v1", "k2", "v2")))
                .isEqualTo("k1");
        // the number of keys of EVAL comes from the numkeys argument
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(Command.EVAL, "return 1", "1", "k", "arg")))
                .isEqualTo("k");
        // a command name resolves to the same table
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(CommandMap.getKnownCommand("get"), "k")))
                .isEqualTo("k");
    }

    @Test
    void leavesCommandsWithoutKeysAlone() {
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(Command.CONFIG, "GET", "port"))).isNull();
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(Command.PING))).isNull();
        // unknown to the Vert.x client: no key, as with its cluster connection
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(null, "k", "v"))).isNull();
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(Command.create("MODULE.CMD"), "k"))).isNull();
    }

    @Test
    void keepsEveryNonNullArgumentInOrder() {
        CommandArgs<byte[], byte[]> args = LettuceReactiveRedisDataSourceImpl.rawArgs(Command.SET, "k", null, "v", "EX",
                "10");
        assertThat(args.count()).isEqualTo(4);
        // Lettuce renders a key argument as key<...> and a plain byte argument in base64
        assertThat(args.toCommandString()).isEqualTo("key<k> dg== RVg= MTA=");
        assertThat(LettuceReactiveRedisDataSourceImpl.rawArgs(Command.PING, (String[]) null).count()).isZero();
    }

    private static String firstKey(CommandArgs<byte[], byte[]> args) {
        ByteBuffer key = args.getFirstEncodedKey();
        return key == null ? null : StandardCharsets.UTF_8.decode(key).toString();
    }
}
