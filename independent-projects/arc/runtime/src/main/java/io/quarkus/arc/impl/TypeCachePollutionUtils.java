package io.quarkus.arc.impl;

import java.lang.reflect.ParameterizedType;

final class TypeCachePollutionUtils {

    static boolean isParameterizedType(final Object o) {
        //Check for ParameterizedTypeImpl first, as it's very likely going
        //to be one; this prevents some cases of type cache pollution (see JDK-8180450).
        if (o instanceof ParameterizedTypeImpl) {
            return true;
        }
        return (o instanceof ParameterizedType);
    }

    static ParameterizedType asParameterizedType(final Object o) {
        //Check for ParameterizedTypeImpl first, as it's very likely going
        //to be one; this prevents some cases of type cache pollution (see JDK-8180450).
        if (o instanceof ParameterizedTypeImpl) {
            //N.B. it's crucial for the purposes of this optimisation that
            //we cast the to concrete type, not to the interface.
            return (ParameterizedTypeImpl) o;
        }
        return (ParameterizedType) o;
    }
}
