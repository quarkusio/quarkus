package io.quarkus.arc;

/**
 * Represents a client proxy - a contextual reference to a bean with a normal scope.
 *
 * @author Martin Kouba
 */
public interface ClientProxy {

    Object arc_contextualInstance();

    InjectableBean<?> arc_bean();

}
