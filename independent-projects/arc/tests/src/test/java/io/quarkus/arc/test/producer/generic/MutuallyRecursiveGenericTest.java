package io.quarkus.arc.test.producer.generic;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class MutuallyRecursiveGenericTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Producer.class, Target.class)
            .additionalClasses(Graph.class, Edge.class, Node.class, Map.class, Route.class, City.class)
            .build();

    @Test
    public void test() {
        Target target = Arc.container().instance(Target.class).get();
        assertNotNull(target.graph);

        assertNotNull(Arc.container().instance(new TypeLiteral<Graph<Map, Route, City>>() {
        }).get());
    }

    @Singleton
    static class Producer {
        @Produces
        @Dependent
        <G extends Graph<G, E, N>, E extends Edge<G, E, N>, N extends Node<G, E, N>> Graph<G, E, N> produce() {
            return new Graph<>() {
            };
        }
    }

    @Singleton
    static class Target {
        @Inject
        Graph<Map, Route, City> graph;
    }

    interface Graph<G extends Graph<G, E, N>, E extends Edge<G, E, N>, N extends Node<G, E, N>> {
    }

    interface Edge<G extends Graph<G, E, N>, E extends Edge<G, E, N>, N extends Node<G, E, N>> {
    }

    interface Node<G extends Graph<G, E, N>, E extends Edge<G, E, N>, N extends Node<G, E, N>> {
    }

    static class Map implements Graph<Map, Route, City> {
    }

    static class Route implements Edge<Map, Route, City> {
    }

    static class City implements Node<Map, Route, City> {
    }
}
