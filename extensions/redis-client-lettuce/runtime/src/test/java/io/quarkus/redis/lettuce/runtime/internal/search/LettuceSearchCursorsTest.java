package io.quarkus.redis.lettuce.runtime.internal.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.AggregationReply.Cursor;

class LettuceSearchCursorsTest {

    private final LettuceSearchCursors cursors = new LettuceSearchCursors();

    @Test
    void routesReadsOfAClusterCursorToTheOwningNode() {
        cursors.track("idx", 0, reply(42, "nodeA"));

        assertThat(cursors.cursor("idx", 42).getCursorId()).isEqualTo(42);
        assertThat(cursors.cursor("idx", 42).getNodeId()).contains("nodeA");
    }

    @Test
    void keysCursorsByIndex() {
        cursors.track("idx", 0, reply(42, "nodeA"));

        assertThat(cursors.cursor("other", 42).getNodeId()).isEmpty();
    }

    @Test
    void keepsTheNodeAcrossPages() {
        cursors.track("idx", 0, reply(42, "nodeA"));
        cursors.track("idx", 42, reply(42, "nodeA"));

        assertThat(cursors.cursor("idx", 42).getNodeId()).contains("nodeA");
    }

    @Test
    void forgetsAnExhaustedCursor() {
        cursors.track("idx", 0, reply(42, "nodeA"));
        cursors.track("idx", 42, reply(0, "nodeA"));

        assertThat(cursors.cursor("idx", 42).getNodeId()).isEmpty();
    }

    @Test
    void forgetsADeletedCursor() {
        cursors.track("idx", 0, reply(7, "nodeB"));
        cursors.forget("idx", 7);

        assertThat(cursors.cursor("idx", 7).getNodeId()).isEmpty();
    }

    @Test
    void staysEmptyOnAStandaloneServer() {
        cursors.track("idx", 0, reply(9, null));

        assertThat(cursors.cursor("idx", 9).getNodeId()).isEmpty();
    }

    @Test
    void toleratesRepliesWithoutACursor() {
        cursors.track("idx", 0, reply(42, "nodeA"));
        cursors.track("idx", 42, null);
        cursors.track("idx", 0, new AggregationReply<byte[]>());

        assertThat(cursors.cursor("idx", 42).getNodeId()).isEmpty();
    }

    @Test
    void forgetsACursorTheServerReportsGone() {
        cursors.track("idx", 0, reply(42, "nodeA"));
        cursors.forgetIfGone("idx", 42, new RedisCommandExecutionException("Cursor not found"));

        assertThat(cursors.cursor("idx", 42).getNodeId()).isEmpty();
    }

    @Test
    void forgetsACursorWhenTheServerErrorIsWrapped() {
        cursors.track("idx", 0, reply(42, "nodeA"));
        cursors.forgetIfGone("idx", 42,
                new CompletionException(new RedisCommandExecutionException("Cursor not found")));

        assertThat(cursors.cursor("idx", 42).getNodeId()).isEmpty();
    }

    @Test
    void keepsACursorAfterATransportFailure() {
        cursors.track("idx", 0, reply(42, "nodeA"));
        cursors.forgetIfGone("idx", 42, new RedisCommandTimeoutException("Command timed out"));
        cursors.forgetIfGone("idx", 42, new RedisConnectionException("Connection closed"));

        assertThat(cursors.cursor("idx", 42).getNodeId()).contains("nodeA");
    }

    @Test
    void forgetsEveryCursorOfADroppedIndex() {
        cursors.track("idx", 0, reply(1, "nodeA"));
        cursors.track("idx", 0, reply(2, "nodeB"));
        cursors.track("other", 0, reply(3, "nodeA"));

        cursors.forgetIndex("idx");

        assertThat(cursors.cursor("idx", 1).getNodeId()).isEmpty();
        assertThat(cursors.cursor("idx", 2).getNodeId()).isEmpty();
        assertThat(cursors.cursor("other", 3).getNodeId()).contains("nodeA");
    }

    private static AggregationReply<byte[]> reply(long cursorId, String nodeId) {
        AggregationReply<byte[]> reply = new AggregationReply<>();
        reply.setCursor(Cursor.of(cursorId, nodeId));
        return reply;
    }

}
