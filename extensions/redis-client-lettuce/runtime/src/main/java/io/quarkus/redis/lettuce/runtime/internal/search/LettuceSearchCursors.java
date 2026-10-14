package io.quarkus.redis.lettuce.runtime.internal.search;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.search.AggregationReply;
import io.lettuce.core.search.AggregationReply.Cursor;

public final class LettuceSearchCursors {

    private static final Duration DEFAULT_MAX_IDLE = Duration.ofHours(1);

    private final ConcurrentMap<CursorKey, CursorState> cursors = new ConcurrentHashMap<>();

    public Cursor cursor(String index, long cursorId) {
        CursorKey key = new CursorKey(index, cursorId);
        CursorState state = cursors.get(key);
        if (state != null && state.expiredAt(now())) {
            cursors.remove(key, state);
            state = null;
        }
        return Cursor.of(cursorId, state == null ? null : state.nodeId());
    }

    public void track(String index, long requestedCursorId, AggregationReply<?> reply) {
        track(index, requestedCursorId, reply, null);
    }

    public void track(String index, long requestedCursorId, AggregationReply<?> reply, Duration maxIdle) {
        Optional<Cursor> cursor = reply == null ? Optional.empty() : reply.getCursor();
        long nextCursorId = cursor.map(Cursor::getCursorId).orElse(0L);
        if (requestedCursorId > 0 && nextCursorId != requestedCursorId) {
            forget(index, requestedCursorId);
        }
        if (nextCursorId > 0) {
            cursor.flatMap(Cursor::getNodeId).ifPresent(nodeId -> remember(index, nextCursorId, nodeId, maxIdle));
        }
    }

    public void forget(String index, long cursorId) {
        cursors.remove(new CursorKey(index, cursorId));
    }

    public void forgetIndex(String index) {
        cursors.keySet().removeIf(key -> key.index().equals(index));
    }

    public void forgetIfGone(String index, long cursorId, Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause()
                : failure;
        if (cause instanceof RedisCommandExecutionException) {
            forget(index, cursorId);
        }
    }

    private void remember(String index, long cursorId, String nodeId, Duration maxIdle) {
        long now = now();
        CursorKey key = new CursorKey(index, cursorId);
        CursorState previous = cursors.get(key);
        long maxIdleNanos = maxIdle != null ? maxIdle.toNanos()
                : previous != null ? previous.maxIdleNanos() : DEFAULT_MAX_IDLE.toNanos();
        if (previous == null) {
            sweep(now);
        }
        cursors.put(key, new CursorState(nodeId, maxIdleNanos, now + maxIdleNanos));
    }

    private void sweep(long now) {
        cursors.values().removeIf(state -> state.expiredAt(now));
    }

    private static long now() {
        return System.nanoTime();
    }

    private record CursorKey(String index, long cursorId) {
    }

    private record CursorState(String nodeId, long maxIdleNanos, long expiresAtNanos) {

        boolean expiredAt(long now) {
            return now - expiresAtNanos >= 0;
        }

    }

}
