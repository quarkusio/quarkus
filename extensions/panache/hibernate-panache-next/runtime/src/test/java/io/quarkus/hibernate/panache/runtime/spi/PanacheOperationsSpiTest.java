/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package io.quarkus.hibernate.panache.runtime.spi;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

/**
 * Verifies that {@link PanacheOperations} does NOT carry Hibernate Reactive types in its generic
 * signature, so that projects using only blocking Panache (without HR on the classpath) do not
 * receive spurious build-time warnings from Jandex / ReflectiveHierarchyStep.
 *
 * Regression test for https://github.com/quarkusio/quarkus/issues/52270
 */
public class PanacheOperationsSpiTest {

    @Test
    public void panacheOperationsHasNoSessionTypeParams() {
        TypeVariable<?>[] typeParams = PanacheOperations.class.getTypeParameters();
        Assertions.assertEquals(6, typeParams.length,
                "PanacheOperations must have exactly 6 type parameters (Session/StatelessSession removed)");

        for (TypeVariable<?> tp : typeParams) {
            String name = tp.getName();
            Assertions.assertFalse(name.equalsIgnoreCase("Session") || name.equalsIgnoreCase("StatelessSession"),
                    "PanacheOperations must not have a Session or StatelessSession type parameter, found: " + name);
        }
    }

    @Test
    public void panacheOperationsDoesNotDeclareSessionMethods() {
        for (Method method : PanacheOperations.class.getDeclaredMethods()) {
            Assertions.assertFalse(
                    method.getName().equals("getSession") || method.getName().equals("getStatelessSession"),
                    "PanacheOperations must not declare getSession/getStatelessSession, found: " + method);
        }
    }

    @Test
    public void panacheReactiveOperationsGenericParamsContainNoMutinyTypes() {
        Type genericSuperinterface = null;
        for (Type iface : PanacheReactiveOperations.class.getGenericInterfaces()) {
            if (iface instanceof ParameterizedType pt && ((ParameterizedType) iface).getRawType() == PanacheOperations.class) {
                genericSuperinterface = pt;
                break;
            }
        }
        Assertions.assertNotNull(genericSuperinterface,
                "PanacheReactiveOperations must extend PanacheOperations");

        Type[] typeArgs = ((ParameterizedType) genericSuperinterface).getActualTypeArguments();
        Assertions.assertEquals(6, typeArgs.length,
                "PanacheReactiveOperations must pass exactly 6 type args to PanacheOperations");

        for (Type typeArg : typeArgs) {
            String typeStr = typeArg.getTypeName();
            Assertions.assertFalse(typeStr.contains("Mutiny"),
                    "PanacheOperations generic args in PanacheReactiveOperations must not reference Mutiny types, found: "
                            + typeStr);
        }
    }

    @Test
    public void panacheBlockingOperationsDeclaresTypedSessionMethods() throws NoSuchMethodException {
        Method getSession = PanacheBlockingOperations.class.getDeclaredMethod("getSession", Class.class);
        Assertions.assertEquals(Session.class, getSession.getReturnType(),
                "PanacheBlockingOperations.getSession must return org.hibernate.Session");

        Method getStatelessSession = PanacheBlockingOperations.class.getDeclaredMethod("getStatelessSession", Class.class);
        Assertions.assertEquals(StatelessSession.class, getStatelessSession.getReturnType(),
                "PanacheBlockingOperations.getStatelessSession must return org.hibernate.StatelessSession");
    }

    @Test
    public void panacheReactiveOperationsDeclaresTypedSessionMethods() throws NoSuchMethodException {
        Method getSession = PanacheReactiveOperations.class.getDeclaredMethod("getSession", Class.class);
        Assertions.assertEquals(Uni.class, getSession.getReturnType(),
                "PanacheReactiveOperations.getSession must return Uni");

        Type genericReturn = getSession.getGenericReturnType();
        Assertions.assertInstanceOf(ParameterizedType.class, genericReturn);
        Type[] typeArgs = ((ParameterizedType) genericReturn).getActualTypeArguments();
        Assertions.assertEquals(1, typeArgs.length);
        Assertions.assertEquals(Mutiny.Session.class, typeArgs[0],
                "PanacheReactiveOperations.getSession must return Uni<Mutiny.Session>");

        Method getStatelessSession = PanacheReactiveOperations.class.getDeclaredMethod("getStatelessSession", Class.class);
        Assertions.assertEquals(Uni.class, getStatelessSession.getReturnType(),
                "PanacheReactiveOperations.getStatelessSession must return Uni");

        Type genericStatelessReturn = getStatelessSession.getGenericReturnType();
        Assertions.assertInstanceOf(ParameterizedType.class, genericStatelessReturn);
        Type[] statelessTypeArgs = ((ParameterizedType) genericStatelessReturn).getActualTypeArguments();
        Assertions.assertEquals(1, statelessTypeArgs.length);
        Assertions.assertEquals(Mutiny.StatelessSession.class, statelessTypeArgs[0],
                "PanacheReactiveOperations.getStatelessSession must return Uni<Mutiny.StatelessSession>");
    }
}
