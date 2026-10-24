package io.quarkus.redis.lettuce.runtime.internal.list;

import java.util.List;

import io.lettuce.core.protocol.CommandArgs;
import io.quarkus.redis.datasource.list.LPosArgs;
import io.quarkus.redis.datasource.list.Position;
import io.quarkus.redis.lettuce.runtime.internal.ArgReplay;

final class LettuceListCommandsConverters {

    private LettuceListCommandsConverters() {
        // Utility class
    }

    static io.lettuce.core.LPosArgs toLettuceLPosArgs(LPosArgs quarkus) {
        List<Object> tokens = quarkus.toArgs();
        return new io.lettuce.core.LPosArgs() {
            @Override
            public <K, V> void build(CommandArgs<K, V> args) {
                ArgReplay.replay(tokens, args);
            }
        };
    }

    static io.lettuce.core.LMoveArgs toLettuceLMoveArgs(Position positionInSource, Position positionInDest) {
        return switch (positionInSource) {
            case LEFT -> positionInDest == Position.LEFT
                    ? io.lettuce.core.LMoveArgs.Builder.leftLeft()
                    : io.lettuce.core.LMoveArgs.Builder.leftRight();
            case RIGHT -> positionInDest == Position.LEFT
                    ? io.lettuce.core.LMoveArgs.Builder.rightLeft()
                    : io.lettuce.core.LMoveArgs.Builder.rightRight();
        };
    }

    static io.lettuce.core.LMPopArgs toLettuceLMPopArgs(Position position) {
        return switch (position) {
            case LEFT -> io.lettuce.core.LMPopArgs.Builder.left();
            case RIGHT -> io.lettuce.core.LMPopArgs.Builder.right();
        };
    }

    static io.lettuce.core.LMPopArgs toLettuceLMPopArgs(Position position, long count) {
        return toLettuceLMPopArgs(position).count(count);
    }

}
