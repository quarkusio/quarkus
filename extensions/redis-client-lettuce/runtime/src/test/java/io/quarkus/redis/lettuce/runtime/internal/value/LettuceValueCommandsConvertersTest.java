package io.quarkus.redis.lettuce.runtime.internal.value;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.value.SetArgs;

class LettuceValueCommandsConvertersTest {

    @Test
    void setArgsGetFlagIsFiltered() {
        assertThat(renderSetArgs(new SetArgs().get())).isEmpty();
    }

    @Test
    void setArgsGetFlagIsFilteredWithoutTouchingOtherOptions() {
        assertThat(renderSetArgs(new SetArgs().nx().ex(30).get())).containsExactly("EX", "30", "NX");
    }

    private static String[] renderSetArgs(SetArgs quarkus) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        LettuceValueCommandsConverters.toLettuceSetArgs(quarkus).build(args);
        // CommandArgs.toCommandString() renders tokens space-separated, unquoted
        String rendered = args.toCommandString();
        if (rendered == null || rendered.isEmpty()) {
            return new String[0];
        }
        return rendered.split(" ");
    }
}
