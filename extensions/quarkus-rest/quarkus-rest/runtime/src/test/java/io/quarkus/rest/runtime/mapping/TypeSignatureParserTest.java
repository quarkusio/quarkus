package io.quarkus.rest.runtime.mapping;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.List;
import java.util.Map;

import javax.enterprise.util.TypeLiteral;

import org.jboss.resteasy.reactive.common.util.types.TypeSignatureParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TypeSignatureParserTest {

    public static class StaticInner {
    }

    public class Inner {
    }

    public class InnerGeneric<T> {
        public class Inner<P> {
        }
    }

    public class Foo<T> extends InnerGeneric<T> {
    }

    @Test
    public void testSignatures() throws NoSuchMethodException, SecurityException {
        assertType("B", byte.class);
        assertType("C", char.class);
        assertType("D", double.class);
        assertType("F", float.class);
        assertType("I", int.class);
        assertType("J", long.class);
        assertType("S", short.class);
        assertType("Z", boolean.class);
        assertType("[Z", boolean[].class);
        assertType("[[Z", boolean[][].class);
        assertType("Ljava/lang/Class;", Class.class);
        assertType("[Ljava/lang/Class;", Class[].class);
        assertType("[[Ljava/lang/Class;", Class[][].class);
        assertType("Lio/quarkus/rest/runtime/mapping/TypeSignatureParserTest;", TypeSignatureParserTest.class);
        assertType("Lio/quarkus/rest/runtime/mapping/TypeSignatureParserTest.StaticInner;", StaticInner.class);
        assertType("Lio/quarkus/rest/runtime/mapping/TypeSignatureParserTest.Inner;", Inner.class);
        assertType("Ljava/util/List<Ljava/lang/String;>;", new TypeLiteral<List<String>>() {
        });
        assertType("[Ljava/util/List<Ljava/lang/String;>;", new TypeLiteral<List<String>[]>() {
        });
        assertType("[[Ljava/util/List<Ljava/lang/String;>;", new TypeLiteral<List<String>[][]>() {
        });
        assertType("Ljava/util/Map<Ljava/lang/String;Ljava/lang/Integer;>;", new TypeLiteral<Map<String, Integer>>() {
        });
        assertType("Lio/quarkus/rest/runtime/mapping/TypeSignatureParserTest.InnerGeneric<Ljava/lang/String;>;",
                new TypeLiteral<InnerGeneric<String>>() {
                });
        assertType(
                "Lio/quarkus/rest/runtime/mapping/TypeSignatureParserTest.InnerGeneric<Ljava/lang/String;>.Inner<Ljava/lang/Integer;>;",
                new TypeLiteral<InnerGeneric<String>.Inner<Integer>>() {
                });

        assertType("Ljava/util/List<+Ljava/lang/String;>;", new TypeLiteral<List<? extends String>>() {
        });
        assertType("Ljava/util/List<-Ljava/lang/String;>;", new TypeLiteral<List<? super String>>() {
        });
        assertType("Ljava/util/List<*>;", new TypeLiteral<List<?>>() {
        });

        //        assertType("TT;", ((ParameterizedType)Foo.class.getGenericSuperclass()).getActualTypeArguments()[0]);
    }

    private void assertType(String signature, TypeLiteral<?> actual) {
        assertType(signature, actual.getType());
    }

    private void assertType(String signature, Type actual) {
        Type parsedType = new TypeSignatureParser(signature).parseType();
        // the JDK impl has reasonable hashCode/equals for lots of stuff but not TypeVariable, so we do a switcheroo for those
        if (actual instanceof TypeVariable) {
            Assertions.assertTrue(parsedType.equals(actual), () -> "expecting " + actual + " but got " + parsedType);
        } else {
            Assertions.assertEquals(actual, parsedType);
        }
    }

}
