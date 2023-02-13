package io.quarkus.arc.test.bean.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;

public class GenericBeanTypesTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(MyBean.class, Producer.class);

    @Test
    public void recursiveGeneric() {
        InjectableBean<Object> bean = Arc.container().instance("myBean").getBean();
        Set<Type> types = bean.getTypes();

        assertEquals(3, types.size());
        assertTrue(types.contains(Object.class));
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                Type genericClass = ((ParameterizedType) type).getRawType();
                assertTrue(MyBean.class.equals(genericClass) || Iterable.class.equals(genericClass));

                assertEquals(1, ((ParameterizedType) type).getActualTypeArguments().length);
                Type typeArg = ((ParameterizedType) type).getActualTypeArguments()[0];
                assertTrue(typeArg instanceof TypeVariable);
                assertEquals("T", ((TypeVariable<?>) typeArg).getName());

                assertEquals(1, ((TypeVariable<?>) typeArg).getBounds().length);
                Type bound = ((TypeVariable<?>) typeArg).getBounds()[0];
                assertTrue(bound instanceof ParameterizedType);
                assertEquals(Comparable.class, ((ParameterizedType) bound).getRawType());

                assertEquals(1, ((ParameterizedType) bound).getActualTypeArguments().length);
                Type boundTypeArg = ((ParameterizedType) bound).getActualTypeArguments()[0];
                assertTrue(boundTypeArg instanceof TypeVariable);
                assertEquals("T", ((TypeVariable<?>) boundTypeArg).getName());
                // recursive
            }
        }
    }

    @Test
    public void duplicateRecursiveGeneric() {
        InjectableBean<Object> bean = Arc.container().instance("foobar").getBean();
        Set<Type> types = bean.getTypes();
        assertEquals(2, types.size());
        assertTrue(types.contains(Object.class));
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                Type genericClass = ((ParameterizedType) type).getRawType();
                assertEquals(FooBar.class, genericClass);

                assertEquals(2, ((ParameterizedType) type).getActualTypeArguments().length);

                Type typeArg = ((ParameterizedType) type).getActualTypeArguments()[0];
                assertTrue(typeArg instanceof TypeVariable);
                assertEquals("T", ((TypeVariable<?>) typeArg).getName());
                assertEquals(1, ((TypeVariable<?>) typeArg).getBounds().length);
                Type bound = ((TypeVariable<?>) typeArg).getBounds()[0];
                assertTrue(bound instanceof ParameterizedType);
                assertEquals(FooBar.class, ((ParameterizedType) bound).getRawType());

                typeArg = ((ParameterizedType) type).getActualTypeArguments()[1];
                assertTrue(typeArg instanceof TypeVariable);
                assertEquals("U", ((TypeVariable<?>) typeArg).getName());
                assertEquals(1, ((TypeVariable<?>) typeArg).getBounds().length);
                bound = ((TypeVariable<?>) typeArg).getBounds()[0];
                assertTrue(bound instanceof ParameterizedType);
                assertEquals(Comparable.class, ((ParameterizedType) bound).getRawType());
            }
        }
    }

    @Test
    public void mutuallyRecursiveGeneric() {
        InjectableBean<Object> bean = Arc.container().instance("graph").getBean();
        Set<Type> types = bean.getTypes();
        System.out.println(types);
        assertEquals(2, types.size());
        assertTrue(types.contains(Object.class));
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                Type genericClass = ((ParameterizedType) type).getRawType();
                assertEquals(Graph.class, genericClass);

                assertEquals(3, ((ParameterizedType) type).getActualTypeArguments().length);

                Type typeArg = ((ParameterizedType) type).getActualTypeArguments()[0];
                assertTrue(typeArg instanceof TypeVariable);
                assertEquals("G", ((TypeVariable<?>) typeArg).getName());
                assertEquals(1, ((TypeVariable<?>) typeArg).getBounds().length);
                Type bound = ((TypeVariable<?>) typeArg).getBounds()[0];
                assertTrue(bound instanceof ParameterizedType);
                assertEquals(Graph.class, ((ParameterizedType) bound).getRawType());

                typeArg = ((ParameterizedType) type).getActualTypeArguments()[1];
                assertTrue(typeArg instanceof TypeVariable);
                assertEquals("E", ((TypeVariable<?>) typeArg).getName());
                assertEquals(1, ((TypeVariable<?>) typeArg).getBounds().length);
                bound = ((TypeVariable<?>) typeArg).getBounds()[0];
                assertTrue(bound instanceof ParameterizedType);
                assertEquals(Edge.class, ((ParameterizedType) bound).getRawType());

                typeArg = ((ParameterizedType) type).getActualTypeArguments()[2];
                assertTrue(typeArg instanceof TypeVariable);
                assertEquals("N", ((TypeVariable<?>) typeArg).getName());
                assertEquals(1, ((TypeVariable<?>) typeArg).getBounds().length);
                bound = ((TypeVariable<?>) typeArg).getBounds()[0];
                assertTrue(bound instanceof ParameterizedType);
                assertEquals(Node.class, ((ParameterizedType) bound).getRawType());
            }
        }
    }

    @Dependent
    @Named("myBean")
    static class MyBean<T extends Comparable<T>> implements Iterable<T> {
        @Override
        public Iterator<T> iterator() {
            return null;
        }
    }

    @Singleton
    static class Producer {
        @Produces
        @Dependent
        @Named("foobar")
        <T extends FooBar<T, U>, U extends Comparable<U>> FooBar<T, U> produceFooBar() {
            return new FooBar<>() {
            };
        }

        @Produces
        @Dependent
        @Named("graph")
        <G extends Graph<G, E, N>, E extends Edge<G, E, N>, N extends Node<G, E, N>> Graph<G, E, N> produceGraph() {
            return new Graph<>() {
            };
        }
    }

    interface FooBar<T extends FooBar<?, U>, U extends Comparable<U>> {
    }

    interface Graph<G extends Graph<G, E, N>, E extends Edge<G, E, N>, N extends Node<G, E, N>> {
    }

    interface Edge<G extends Graph<G, E, N>, E extends Edge<G, E, N>, N extends Node<G, E, N>> {
    }

    interface Node<G extends Graph<G, E, N>, E extends Edge<G, E, N>, N extends Node<G, E, N>> {
    }
}
