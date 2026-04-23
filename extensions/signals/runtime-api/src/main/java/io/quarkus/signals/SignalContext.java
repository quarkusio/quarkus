package io.quarkus.signals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * Immutable contextual information about an emitted signal.
 *
 * @param <T> the signal type
 */
public interface SignalContext<T> {

    /**
     * @return the metadata attached to the emission, never {@code null}
     * @see Signal#putMetadata(String, Object)
     * @see Signal#setMetadata(Map)
     */
    Map<String, Object> metadata();

    /**
     * @return the signal object
     */
    T signal();

    /**
     * @return the type of the signal object
     */
    Type signalType();

    /**
     * @return the expected response type, or {@code null} if the emission is not a request-reply
     * @see EmissionType#REQUEST
     */
    Type responseType();

    /**
     * @return the qualifiers specified at the emission point
     */
    Set<Annotation> qualifiers();

    /**
     * @return the type of the emission
     */
    SignalContext.EmissionType emissionType();

    /**
     * The type of emission that triggered the receiver.
     *
     * @see Signal#publish(Object)
     * @see Signal#send(Object)
     * @see Signal#request(Object, Class)
     */
    enum EmissionType {

        /**
         * The signal was emitted via {@link Signal#publish(Object)} (multicast).
         */
        PUBLISH,

        /**
         * The signal was emitted via {@link Signal#request(Object, Class)} (unicast, request-reply).
         */
        REQUEST,

        /**
         * The signal was emitted via {@link Signal#send(Object)} (unicast, fire-and-forget).
         */
        SEND
    }

}