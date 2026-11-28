package io.quarkus.redis.lettuce.runtime.internal.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.CommandArgs;
import io.vertx.redis.client.Command;

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
        assertThat(LettuceReactiveRedisDataSourceImpl.vertxCommand("get")).isSameAs(Command.GET);
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(LettuceReactiveRedisDataSourceImpl.vertxCommand("get"),
                "k"))).isEqualTo("k");
    }

    @Test
    void aCommandNameUnknownToTheVertxClientHasNoKey() {
        Command unknown = LettuceReactiveRedisDataSourceImpl.vertxCommand("MODULE.CMD");
        assertThat(unknown).isNotNull();
        assertThat(firstKey(LettuceReactiveRedisDataSourceImpl.rawArgs(unknown, "k", "v"))).isNull();
    }

    /**
     * {@code Command} and the {@code CommandMap} behind it initialize each other, and the Vert.x client always enters
     * through {@code Command}. A class of this module entering through the map can deadlock with a thread entering
     * through {@code Command}, both in their class initializers: it hung the build of a native image. So no class of
     * the module refers to the map.
     */
    @Test
    void noClassOfTheModuleRefersToTheVertxCommandMap() throws Exception {
        Path classes = Path.of(LettuceReactiveRedisDataSourceImpl.class.getProtectionDomain().getCodeSource().getLocation()
                .toURI());
        byte[] commandMap = "io/vertx/redis/client/impl/CommandMap".getBytes(StandardCharsets.ISO_8859_1);
        List<Path> referring;
        try (Stream<Path> files = Files.walk(classes)) {
            referring = files.filter(file -> file.toString().endsWith(".class"))
                    .filter(file -> contains(read(file), commandMap))
                    .toList();
        }
        assertThat(Files.isDirectory(classes)).isTrue();
        assertThat(referring).isEmpty();
    }

    private static byte[] read(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean contains(byte[] bytes, byte[] sequence) {
        outer: for (int i = 0; i <= bytes.length - sequence.length; i++) {
            for (int j = 0; j < sequence.length; j++) {
                if (bytes[i + j] != sequence[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
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
