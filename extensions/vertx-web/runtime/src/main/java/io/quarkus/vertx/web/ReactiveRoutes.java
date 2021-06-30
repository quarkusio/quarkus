package io.quarkus.vertx.web;

import java.util.Objects;

import io.quarkus.vertx.web.runtime.JsonArrayMulti;
import io.quarkus.vertx.web.runtime.NdjsonMulti;
import io.quarkus.vertx.web.runtime.SSEMulti;
import io.smallrye.mutiny.Multi;

/**
 * Provides utility methods, mainly to handle {@code text/event-stream} responses.
 */
public class ReactiveRoutes {

    private ReactiveRoutes() {
        // Avoid direct instantiation.
    }

    /**
     * Indicates the the given stream should be written as server-sent-event in the response.
     * Returning a {@code multi} wrapped using this method produces a {@code text/event-stream} response. Each item
     * is written as an event in the response. The response automatically enables the chunked encoding and set the
     * content type.
     * <p>
     * If the item is a String, the {@code data} part of the event is this string. An {@code id} is automatically
     * generated.
     * If the item is a Buffer, the {@code data} part of the event is this buffer. An {@code id} is automatically
     * generated.
     * If the item is an Object, the {@code data} part of the event is the JSON representation of this object. An
     * {@code id} is automatically generated.
     * If the item is an {@link ServerSentEvent}, the {@code data} part of the event is the JSON representation of this
     * {@link ServerSentEvent#data()}. The {@code id} is computed from {@link ServerSentEvent#id()} (generated if not
     * implemented). The {@code event} section (ignored in all the other case) is computed from
     * {@link ServerSentEvent#event()}.
     * <p>
     * Example of usage:
     *
     * <pre>
     * &#64;Route(path = "/people")
     * Multi&lt;Person&gt; people(RoutingContext context) {
     *     return ReactiveRoutes.asEventStream(Multi.createFrom().items(
     *             new Person("superman", 1),
     *             new Person("batman", 2),
     *             new Person("spiderman", 3)));
     * }
     * </pre>
     *
     * @param multi the multi to be written
     * @param <T> the type of item, can be string, buffer, object or io.quarkus.vertx.web.ReactiveRoutes.ServerSentEvent
     * @return the wrapped multi
     */
    public static <T> Multi<T> asEventStream(Multi<T> multi) {
        return new SSEMulti<>(Objects.requireNonNull(multi, "The passed multi must not be `null`"));
    }

    /**
     * Indicates the the given stream should be written as a Json stream in the response.
     * Returning a {@code multi} wrapped using this method produces a {@code application/x-ndjson} response. Each item
     * is written as an serialized json on a new line in the response. The response automatically enables the chunked
     * encoding and set the content type.
     * <p>
     * If the item is a String, the content will be wrapped in quotes and written.
     * If the item is an Object, then the JSON representation of this object will be written.
     * <p>
     * Example of usage:
     * 
     * <pre>
     * &#64;Route(path = "/people")
     * Multi&lt;Person&gt; people(RoutingContext context) {
     *     return ReactiveRoutes.asJsonStream(Multi.createFrom().items(
     *             new Person("superman", 1),
     *             new Person("batman", 2),
     *             new Person("spiderman", 3)));
     * }
     * </pre>
     *
     * This example produces:
     * 
     * <pre>
     *  {"name":"superman", "id":1}
     *  {...}
     *  {...}
     * </pre>
     *
     * @param multi the multi to be written
     * @param <T> the type of item, can be string, object
     * @return the wrapped multi
     */
    public static <T> Multi<T> asJsonStream(Multi<T> multi) {
        return new NdjsonMulti<>(Objects.requireNonNull(multi, "The passed multi must not be `null`"));
    }

    /**
     * Indicates the the given stream should be written as a <em>chunked</em> JSON array in the response.
     * Returning a {@code multi} wrapped using this method produces a {@code application/json} response. Each item
     * is written as an JSON object in the response. The response automatically enables the chunked encoding and set the
     * content type.
     * <p>
     * If the item is a String, the content is written in the array.
     * If the item is an Object, the content is transformed to JSON and written in the array.
     * <p>
     * Note that the array is written in the response item by item, without accumulating the data.
     *
     * Example of usage:
     *
     * <pre>
     * &#64;Route(path = "/people")
     * Multi&lt;Person&gt; people(RoutingContext context) {
     *     return ReactiveRoutes.asJsonArray(Multi.createFrom().items(
     *             new Person("superman", 1),
     *             new Person("batman", 2),
     *             new Person("spiderman", 3)));
     * }
     * </pre>
     *
     * This example produces: {@code [{"name":"superman", "id":1}, {...}, {..,}]}
     *
     * @param multi the multi to be written
     * @param <T> the type of item, can be string or object
     * @return the wrapped multi
     */
    public static <T> Multi<T> asJsonArray(Multi<T> multi) {
        return new JsonArrayMulti<>(Objects.requireNonNull(multi, "The passed multi must not be `null`"));
    }

    /**
     * A class allowing to customized how the server sent events are written.
     * <p>
     * The {@code data} section of the resulting event is the JSON representation of the result from {@link #data()}.
     * If {@link #event()} does not return {@code null}, the {@code event} section is written with the result as value.
     * If {@link #id()} is implemented, the {@code id} section uses this value.
     *
     * @param <T> the type of payload, use for the {@code data} section of the event.
     */
    public interface ServerSentEvent<T> {

        /**
         * The {@code event} section.
         *
         * @return the name of the event. If {@code null}, the written event won't have an {@code event} section
         */
        default String event() {
            return null;
        }

        /**
         * The {@code data} section.
         *
         * @return the object that will be encoded to JSON. Must not be {@code null}
         */
        T data();

        /**
         * The {@code id} section.
         * If not implemented, an automatic id is inserted.
         *
         * @return the id
         */
        default long id() {
            return -1L;
        }

    }

}
