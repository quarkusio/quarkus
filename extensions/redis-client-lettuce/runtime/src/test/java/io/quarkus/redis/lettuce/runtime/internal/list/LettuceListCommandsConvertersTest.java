package io.quarkus.redis.lettuce.runtime.internal.list;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.list.Position;

class LettuceListCommandsConvertersTest {

    @Test
    void lMoveArgsCoverEveryPositionPair() {
        assertThat(renderLMoveArgs(Position.LEFT, Position.LEFT)).containsExactly("LEFT", "LEFT");
        assertThat(renderLMoveArgs(Position.LEFT, Position.RIGHT)).containsExactly("LEFT", "RIGHT");
        assertThat(renderLMoveArgs(Position.RIGHT, Position.LEFT)).containsExactly("RIGHT", "LEFT");
        assertThat(renderLMoveArgs(Position.RIGHT, Position.RIGHT)).containsExactly("RIGHT", "RIGHT");
    }

    @Test
    void lMPopArgsWithoutCount() {
        assertThat(renderToTokens(LettuceListCommandsConverters.toLettuceLMPopArgs(Position.LEFT)::build))
                .containsExactly("LEFT");
        assertThat(renderToTokens(LettuceListCommandsConverters.toLettuceLMPopArgs(Position.RIGHT)::build))
                .containsExactly("RIGHT");
    }

    @Test
    void lMPopArgsWithCount() {
        assertThat(renderToTokens(LettuceListCommandsConverters.toLettuceLMPopArgs(Position.RIGHT, 3)::build))
                .containsExactly("RIGHT", "COUNT", "3");
    }

    // ------------------------------------------------------------------ helpers

    private static String[] renderLMoveArgs(Position source, Position destination) {
        return renderToTokens(LettuceListCommandsConverters.toLettuceLMoveArgs(source, destination)::build);
    }

    private static String[] renderToTokens(Consumer<CommandArgs<String, String>> builder) {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
        builder.accept(args);
        // CommandArgs.toCommandString() renders tokens space-separated, unquoted
        String rendered = args.toCommandString();
        if (rendered == null || rendered.isEmpty()) {
            return new String[0];
        }
        return rendered.split(" ");
    }

}
