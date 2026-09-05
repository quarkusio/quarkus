package io.quarkus.mongodb.panache.common.reactive;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;

import com.mongodb.reactivestreams.client.ClientSession;

import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.MongoClientBeanUtil;
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

    /**
     * Performs the given work within the scope of a MongoDB transaction.
     * The transaction will be rolled back if the work completes with an uncaught exception.
     *
     * @param <T> The function's return type
     * @param work The function to execute in the new transaction
     * @return the result of executing the function
     */
    public static <T> Uni<T> withTransaction(Supplier<Uni<T>> work) {
        Context context = vertxContext();
        ClientSession current = MongodbPanacheContextLocalsProvider.SESSION_LOCAL.get(context);
        if (current != null && current.hasActiveTransaction()) {
            // reactive session exists - reuse this session
            return work.get();
        } else {
            // reactive session does not exist - open a new one and close it when the returned Uni completes
            return Panache.startSession()
                    .invoke(s -> s.startTransaction())
                    .invoke(s -> MongodbPanacheContextLocalsProvider.SESSION_LOCAL.put(context, s))
                    .chain(s -> work.get())
                    .call(() -> commitTransaction())
                    .onFailure().call(() -> abortTransaction())
                    .eventually(() -> Panache.closeSession());
        }
    }

    /**
     * Allow to access the current MongoDB session.
     * The session will only exist in the context of a reactive MongoDB with Panache transaction started with
     * <code>Panache.withTransaction()</code>.
     *
     * @see #withTransaction(Supplier)
     * @return the current ClientSession or null if none.
     */
    public static ClientSession getCurrentSession() {
        Context context = Vertx.currentContext();
        if (context == null) {
            return null;
        }
        return MongodbPanacheContextLocalsProvider.SESSION_LOCAL.get(context);
    }

    private static Uni<?> abortTransaction() {
        Context context = vertxContext();
        ClientSession current = MongodbPanacheContextLocalsProvider.SESSION_LOCAL.get(context);
        return toUni(current.abortTransaction());
    }

    private static Uni<?> commitTransaction() {
        Context context = vertxContext();
        ClientSession current = MongodbPanacheContextLocalsProvider.SESSION_LOCAL.get(context);
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
        ReactiveMongoClient client = MongoClientBeanUtil.reactiveMongoClient();
        return client.startSession();
    }

    private static void closeSession() {
        Context context = vertxContext();
        ClientSession current = MongodbPanacheContextLocalsProvider.SESSION_LOCAL.get(context);
        try {
            current.close();
        } finally {
            MongodbPanacheContextLocalsProvider.SESSION_LOCAL.remove(context);
        }
    }

    /**
     *
     * @return the current vertx duplicated context
     * @throws IllegalStateException If no vertx context is found or is not a safe context as mandated by the
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
