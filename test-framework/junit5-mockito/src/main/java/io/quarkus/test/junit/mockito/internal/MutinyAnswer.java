package io.quarkus.test.junit.mockito.internal;

import static org.mockito.internal.util.ObjectMethodsGuru.isCompareToMethod;
import static org.mockito.internal.util.ObjectMethodsGuru.isToStringMethod;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.mockito.internal.stubbing.defaultanswers.ReturnsEmptyValues;
import org.mockito.internal.util.Primitives;
import org.mockito.invocation.InvocationOnMock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@SuppressWarnings("serial")
public class MutinyAnswer extends ReturnsEmptyValues {

    @Override
    public Object answer(InvocationOnMock inv) {
        if (isToStringMethod(inv.getMethod()) || isCompareToMethod(inv.getMethod())) {
            return super.answer(inv);
        }

        Class<?> returnType = inv.getMethod().getReturnType();
        // save the user some time figuring out this issue when it happens
        if ((returnType.getName().equals(Multi.class.getName()) && returnType != Multi.class)
                || (returnType.getName().equals(Uni.class.getName()) && returnType != Uni.class)) {
            throw new IllegalStateException(
                    "Class loader issue: we have two Multi classes with different class loaders. "
                            + "Make sure to initialize this class with the QuarkusClassLoader.");
        }
        if (returnType == Multi.class) {
            return Multi.createFrom().item(returnValueForMutiny(inv.getMethod().getGenericReturnType()));
        } else if (returnType == Uni.class) {
            return Uni.createFrom().item(returnValueForMutiny(inv.getMethod().getGenericReturnType()));
        }
        return returnValueForClass(returnType);
    }

    private Object returnValueForMutiny(Type uniOrMultiType) {
        // check for raw types
        if (uniOrMultiType instanceof Class)
            return returnValueForClass(Object.class);
        Type ret = ((ParameterizedType) uniOrMultiType).getActualTypeArguments()[0];
        return returnValueForType(ret);
    }

    private Object returnValueForType(Type type) {
        if (type instanceof Class)
            return returnValueForClass((Class<?>) type);
        if (type instanceof ParameterizedType)
            return returnValueForClass((Class<?>) ((ParameterizedType) type).getRawType());
        if (type instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>) type;
            // default upper bound is Object so we always have a value
            return returnValueForType(tv.getBounds()[0]);
        }
        return returnValueForClass(Object.class);
    }

    // copied from supertype due to access restriction :(
    Object returnValueForClass(Class<?> type) {
        if (Primitives.isPrimitiveOrWrapper(type)) {
            return Primitives.defaultValue(type);
            // new instances are used instead of Collections.emptyList(), etc.
            // to avoid UnsupportedOperationException if code under test modifies returned
            // collection
        } else if (type == Iterable.class) {
            return new ArrayList<>(0);
        } else if (type == Collection.class) {
            return new LinkedList<>();
        } else if (type == Set.class) {
            return new HashSet<>();
        } else if (type == HashSet.class) {
            return new HashSet<>();
        } else if (type == SortedSet.class) {
            return new TreeSet<>();
        } else if (type == TreeSet.class) {
            return new TreeSet<>();
        } else if (type == LinkedHashSet.class) {
            return new LinkedHashSet<>();
        } else if (type == List.class) {
            return new LinkedList<>();
        } else if (type == LinkedList.class) {
            return new LinkedList<>();
        } else if (type == ArrayList.class) {
            return new ArrayList<>();
        } else if (type == Map.class) {
            return new HashMap<>();
        } else if (type == HashMap.class) {
            return new HashMap<>();
        } else if (type == SortedMap.class) {
            return new TreeMap<>();
        } else if (type == TreeMap.class) {
            return new TreeMap<>();
        } else if (type == LinkedHashMap.class) {
            return new LinkedHashMap<>();
        } else if (type == Optional.class) {
            return Optional.empty();
        } else if (type == OptionalDouble.class) {
            return OptionalDouble.empty();
        } else if (type == OptionalInt.class) {
            return OptionalInt.empty();
        } else if (type == OptionalLong.class) {
            return OptionalLong.empty();
        } else if (type == Stream.class) {
            return Stream.empty();
        } else if (type == DoubleStream.class) {
            return DoubleStream.empty();
        } else if (type == IntStream.class) {
            return IntStream.empty();
        } else if (type == LongStream.class) {
            return LongStream.empty();
        } else if (type == Duration.class) {
            return Duration.ZERO;
        } else if (type == Period.class) {
            return Period.ZERO;
        }

        // Let's not care about the rest of collections.
        return null;
    }
}
