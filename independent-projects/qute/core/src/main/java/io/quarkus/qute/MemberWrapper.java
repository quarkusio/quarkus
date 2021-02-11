package io.quarkus.qute;

import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Martin Kouba
 */
@FunctionalInterface
interface MemberWrapper {

    /**
     *
     * @param instance
     * @return the member value for the given instance
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    Object getValue(Object instance) throws IllegalAccessException,
            IllegalArgumentException, InvocationTargetException;

}
