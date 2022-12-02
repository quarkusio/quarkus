package io.quarkus.test.junit.mockito.internal;

import static org.mockito.internal.util.ObjectMethodsGuru.isCompareToMethod;
import static org.mockito.internal.util.ObjectMethodsGuru.isToStringMethod;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.mockito.internal.stubbing.defaultanswers.ReturnsEmptyValues;
import org.mockito.internal.util.JavaEightUtil;
import org.mockito.internal.util.Primitives;
import org.mockito.invocation.InvocationOnMock;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@SuppressWarnings("serial")
public class MutinyAnswer extends ReturnsEmptyValues {

    @Override
    public Object answer(InvocationOnMock inv) {
        if (isToStringMethod(inv.getMethod())
                || isCompareToMethod(inv.getMethod())) {
            return super.answer(inv);
        }

        Class<?> returnType = inv.getMethod().getReturnType();
        // save the user some time figuring out this issue when it happens
        if ((returnType.getName().equals(Multi.class.getName()) && returnType != Multi.class)
                || (returnType.getName().equals(Uni.class.getName()) && returnType != Uni.class)) {
            throw new IllegalStateException("Class loader issue: we have two Multi classes with different class loaders. "
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
            //new instances are used instead of Collections.emptyList(), etc.
            //to avoid UnsupportedOperationException if code under test modifies returned collection
        } else if (type == Iterable.class) {
            return new ArrayList<Object>(0);
        } else if (type == Collection.class) {
            return new LinkedList<Object>();
        } else if (type == Set.class) {
            return new HashSet<Object>();
        } else if (type == HashSet.class) {
            return new HashSet<Object>();
        } else if (type == SortedSet.class) {
            return new TreeSet<Object>();
        } else if (type == TreeSet.class) {
            return new TreeSet<Object>();
        } else if (type == LinkedHashSet.class) {
            return new LinkedHashSet<Object>();
        } else if (type == List.class) {
            return new LinkedList<Object>();
        } else if (type == LinkedList.class) {
            return new LinkedList<Object>();
        } else if (type == ArrayList.class) {
            return new ArrayList<Object>();
        } else if (type == Map.class) {
            return new HashMap<Object, Object>();
        } else if (type == HashMap.class) {
            return new HashMap<Object, Object>();
        } else if (type == SortedMap.class) {
            return new TreeMap<Object, Object>();
        } else if (type == TreeMap.class) {
            return new TreeMap<Object, Object>();
        } else if (type == LinkedHashMap.class) {
            return new LinkedHashMap<Object, Object>();
        } else if ("java.util.Optional".equals(type.getName())) {
            return JavaEightUtil.emptyOptional();
        } else if ("java.util.OptionalDouble".equals(type.getName())) {
            return JavaEightUtil.emptyOptionalDouble();
        } else if ("java.util.OptionalInt".equals(type.getName())) {
            return JavaEightUtil.emptyOptionalInt();
        } else if ("java.util.OptionalLong".equals(type.getName())) {
            return JavaEightUtil.emptyOptionalLong();
        } else if ("java.util.stream.Stream".equals(type.getName())) {
            return JavaEightUtil.emptyStream();
        } else if ("java.util.stream.DoubleStream".equals(type.getName())) {
            return JavaEightUtil.emptyDoubleStream();
        } else if ("java.util.stream.IntStream".equals(type.getName())) {
            return JavaEightUtil.emptyIntStream();
        } else if ("java.util.stream.LongStream".equals(type.getName())) {
            return JavaEightUtil.emptyLongStream();
        }

        //Let's not care about the rest of collections.
        return null;
    }
}
