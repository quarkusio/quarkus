package io.quarkus.mongodb.panache.common.reactive;

import java.util.UUID;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import com.mongodb.reactivestreams.client.ClientSession;

import io.quarkus.mongodb.panache.common.runtime.BeanUtils;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import mutiny.zero.flow.adapters.AdaptersToFlow;

/**
 * Utility class for reactive MongoDB with Panache.
 */
public class Panache {
    private static final String ERROR_MSG = "MongoDB reactive with Panache requires a safe (isolated) Vert.x sub-context, but the current context hasn't been flagged as such.";

    private static final UUID SESSION_KEY = UUID.randomUUID();

    /**
     * Performs the given work within the scope of a MongoDB transaction. The transaction will be rolled back if the
     * work completes with an uncaught exception.
     *
     * @param <T>
     *        The function's return type
     * @param work
     *        The function to execute in the new transaction
     *
     * @return the result of executing the function
     */
    public static <T> Uni<T> withTransaction(Supplier<Uni<T>> work) {
        Context context = vertxContext();
        ClientSession current = context.getLocal(SESSION_KEY);
        if (current != null && current.hasActiveTransaction()) {
            // reactive session exists - reuse this session
            return work.get();
        } else {
            // reactive session does not exist - open a new one and close it when the returned Uni completes
            return Panache.startSession().invoke(s -> s.startTransaction())
                    .invoke(s -> context.putLocal(SESSION_KEY, s)).chain(s -> work.get())
                    .call(() -> commitTransaction()).onFailure().call(() -> abortTransaction())
                    .eventually(() -> Panache.closeSession());
        }
    }

    /**
     * Allow to access the current MongoDB session. The session will only exist in the context of a reactive MongoDB
     * with Panache transaction started with <code>Panache.withTransaction()</code>.
     *
     * @see #withTransaction(Supplier)
     *
     * @return the current ClientSession or null if none.
     */
    public static ClientSession getCurrentSession() {
        Context context = Vertx.currentContext();
        return context != null ? context.getLocal(SESSION_KEY) : null;
    }

    private static Uni<?> abortTransaction() {
        Context context = vertxContext();
        ClientSession current = context.getLocal(SESSION_KEY);
        return toUni(current.abortTransaction());
    }

    private static Uni<?> commitTransaction() {
        Context context = vertxContext();
        ClientSession current = context.getLocal(SESSION_KEY);
        return toUni(current.commitTransaction());
    }

    private static <T> Uni<T> toUni(Publisher<T> publisher) {
        Context context = Vertx.currentContext();
        Uni<T> uni = Uni.createFrom().publisher(AdaptersToFlow.publisher(publisher));
        if (context != null) {
            return uni.emitOn(command -> context.runOnContext(x -> command.run()));
        }
        return uni;
    }

    private static Uni<ClientSession> startSession() {
        ReactiveMongoClient client = BeanUtils.clientFromArc(null, ReactiveMongoClient.class, true);
        return client.startSession();
    }

    private static void closeSession() {
        Context context = vertxContext();
        ClientSession current = context.getLocal(SESSION_KEY);
        try {
            current.close();
        } finally {
            context.removeLocal(SESSION_KEY);
        }
    }

    /**
     * @return the current vertx duplicated context
     *
     * @throws IllegalStateException
     *         If no vertx context is found or is not a safe context as mandated by the
     *         {@link VertxContextSafetyToggle}
     */
    private static Context vertxContext() {
        Context context = Vertx.currentContext();
        if (context != null) {
            VertxContextSafetyToggle.validateContextIfExists(ERROR_MSG, ERROR_MSG);
            return context;
        } else {
            throw new IllegalStateException("No current Vertx context found");
        }
    }
}
