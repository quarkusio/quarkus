package io.quarkus.arc.impl;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * This code was mainly copied from Weld codebase.
 * 
 * Implementation of {@link WildcardType}.
 *
 * Note that per JLS a wildcard may define either the upper bound or the lower bound. A wildcard may not have multiple bounds.
 *
 * @author Jozef Hartinger
 *
 */
public class WildcardTypeImpl implements WildcardType {

    public static WildcardType defaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static WildcardType withUpperBound(Type type) {
        return new WildcardTypeImpl(new Type[] { type }, DEFAULT_LOWER_BOUND);
    }

    public static WildcardType withLowerBound(Type type) {
        return new WildcardTypeImpl(DEFAULT_UPPER_BOUND, new Type[] { type });
    }

    private static final Type[] DEFAULT_UPPER_BOUND = new Type[] { Object.class };
    private static final Type[] DEFAULT_LOWER_BOUND = new Type[0];
    private static final WildcardType DEFAULT_INSTANCE = new WildcardTypeImpl(DEFAULT_UPPER_BOUND, DEFAULT_LOWER_BOUND);

    private final Type[] upperBound;
    private final Type[] lowerBound;

    private WildcardTypeImpl(Type[] upperBound, Type[] lowerBound) {
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    @Override
    public Type[] getUpperBounds() {
        return upperBound;
    }

    @Override
    public Type[] getLowerBounds() {
        return lowerBound;
    }
}