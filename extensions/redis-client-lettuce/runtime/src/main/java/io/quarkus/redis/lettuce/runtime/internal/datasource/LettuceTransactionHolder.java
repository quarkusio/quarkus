package io.quarkus.redis.lettuce.runtime.internal.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.lettuce.core.RedisException;
import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.runtime.datasource.OptimisticLockingTransactionResultImpl;
import io.quarkus.redis.runtime.datasource.TransactionResultImpl;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce equivalent of {@link io.quarkus.redis.runtime.datasource.TransactionHolder}.
 * <p>
 * Unlike the Vert.x backend — which receives a {@code QUEUED} reply per command issued between {@code MULTI} and
 * {@code EXEC} — Lettuce does not complete the future of a queued command before {@code EXEC}, and on a
 * master/replica connection (Sentinel and replication clients) it does not complete it at all. So instead of
 * awaiting the commands, this holder records the result mapper of each command, in enqueue order, and reconstructs
 * the typed {@link TransactionResult} from the {@code EXEC} reply: Lettuce decodes every element of that reply with
 * the output of the queued command it answers, so the elements are exactly what the commands' futures would carry,
 * or a {@link RedisException} for a command the server rejected.
 * <p>
 * Each entry is a {@link LettuceCommand} carrying the same mapper the non-transactional implementation applies via
 * {@link LettuceCommand#toUni()}, so {@code TransactionResult.get(index)} returns the same Java type the Vert.x
 * backend produces.
 */
public class LettuceTransactionHolder {

    private final List<Function<Object, Object>> mappers = new ArrayList<>();
    private volatile boolean discarded = false;

    /**
     * Issues a command into the open {@code MULTI} block and records its mapper for later assembly.
     * <p>
     * The command's {@link LettuceCommand#call() call} supplier is invoked eagerly so the command is enqueued on
     * the pinned connection in call order, which matches the order of the {@code EXEC} reply. A supplier that
     * throws instead of issuing a command — the command implementations defer some argument validations that way —
     * fails the returned {@link Uni} rather than the call, as the Vert.x backend and the non-transactional path do,
     * and records no entry. The command's {@link LettuceCommand#mapper() mapper} is applied to the matching element
     * of the {@code EXEC} reply when the {@link TransactionResult} is assembled.
     * <p>
     * Once {@link #discard()} has been called, {@code DISCARD} has already been sent on the pinned connection, and
     * it is no longer inside {@code MULTI}; issuing the command's call at that point would run it for real instead
     * of queuing it. So a call arriving after {@link #discard()} is rejected outright, without invoking
     * {@code call()}, matching the {@code IllegalStateException} the Vert.x backend raises when a queued command
     * doesn't come back {@code QUEUED}.
     *
     * @param command the command to issue, carrying its call and result mapper
     * @param <T> the raw Lettuce result type
     * @param <R> the Quarkus result type recorded in the {@link TransactionResult}
     * @return a {@link Uni} completing immediately — the result is only available after {@code EXEC}
     */
    @SuppressWarnings("unchecked")
    public <T, R> Uni<Void> enqueue(LettuceCommand<T, R> command) {
        if (discarded) {
            return Uni.createFrom().failure(new IllegalStateException("Unable to add command to the current transaction"));
        }
        try {
            command.call().get();
        } catch (RuntimeException e) {
            return Uni.createFrom().failure(e);
        }
        mappers.add((Function<Object, Object>) command.mapper());
        return Uni.createFrom().voidItem();
    }

    public void discard() {
        discarded = true;
    }

    public boolean discarded() {
        return discarded;
    }

    public int size() {
        return mappers.size();
    }

    /**
     * Builds the {@link TransactionResult} from the reply of a committed {@code EXEC}.
     *
     * @param exec the {@code EXEC} reply, one element per queued command, not discarded
     */
    public TransactionResult toResult(io.lettuce.core.TransactionResult exec) {
        boolean[] hasErrors = { false };
        List<Object> results = collect(exec, hasErrors);
        return new TransactionResultImpl(discarded, hasErrors[0], results);
    }

    /**
     * Builds an {@link OptimisticLockingTransactionResult} from the reply of a committed {@code EXEC}, attaching
     * the pre-transaction result.
     */
    public <I> OptimisticLockingTransactionResult<I> toOptimisticLockingResult(I input,
            io.lettuce.core.TransactionResult exec) {
        boolean[] hasErrors = { false };
        List<Object> results = collect(exec, hasErrors);
        return new OptimisticLockingTransactionResultImpl<>(discarded, hasErrors[0], input, results);
    }

    private List<Object> collect(io.lettuce.core.TransactionResult exec, boolean[] hasErrors) {
        if (exec.size() != mappers.size()) {
            throw new IllegalStateException("The EXEC reply carries " + exec.size() + " results for " + mappers.size()
                    + " queued commands");
        }
        List<Object> results = new ArrayList<>(mappers.size());
        for (int i = 0; i < mappers.size(); i++) {
            Object raw = exec.get(i);
            if (raw instanceof RedisException failure) {
                hasErrors[0] = true;
                results.add(failure);
            } else {
                results.add(mappers.get(i).apply(raw));
            }
        }
        return results;
    }

}
