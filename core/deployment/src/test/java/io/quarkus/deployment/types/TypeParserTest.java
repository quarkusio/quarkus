package io.quarkus.deployment.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.util.TypeLiteral;

import org.junit.jupiter.api.Test;

public class TypeParserTest {
    @Test
    public void testVoid() {
        assertCorrect("void", void.class);
        assertCorrect(" void", void.class);
        assertCorrect("void ", void.class);
        assertCorrect(" void ", void.class);
    }

    @Test
    public void testPrimitive() {
        assertCorrect("boolean", boolean.class);
        assertCorrect(" byte", byte.class);
        assertCorrect("short ", short.class);
        assertCorrect(" int ", int.class);
        assertCorrect("\tlong", long.class);
        assertCorrect("float\t", float.class);
        assertCorrect("\tdouble\t", double.class);
        assertCorrect(" \n char \n ", char.class);
    }

    @Test
    public void testPrimitiveArray() {
        assertCorrect("boolean[]", boolean[].class);
        assertCorrect("byte [][]", byte[][].class);
        assertCorrect("short [] [] []", short[][][].class);
        assertCorrect("int [ ] [ ] [ ] [ ]", int[][][][].class);
        assertCorrect("long   [][][]", long[][][].class);
        assertCorrect(" float[][]", float[][].class);
        assertCorrect(" double [] ", double[].class);
        assertCorrect(" char [ ][ ]  ", char[][].class);
    }

    @Test
    public void testClass() {
        assertCorrect("java.lang.Object", Object.class);
        assertCorrect("java.lang.String", String.class);

        assertCorrect(" java.lang.Boolean", Boolean.class);
        assertCorrect("java.lang.Byte ", Byte.class);
        assertCorrect(" java.lang.Short ", Short.class);
        assertCorrect("\tjava.lang.Integer", Integer.class);
        assertCorrect("java.lang.Long\t", Long.class);
        assertCorrect("\tjava.lang.Float\t", Float.class);
        assertCorrect("   java.lang.Double", Double.class);
        assertCorrect("java.lang.Character   ", Character.class);
    }

    @Test
    public void testClassArray() {
        assertCorrect("java.lang.Object[]", Object[].class);
        assertCorrect("java.lang.String[][]", String[][].class);

        assertCorrect("java.lang.Boolean[][][]", Boolean[][][].class);
        assertCorrect("java.lang.Byte[][][][]", Byte[][][][].class);
        assertCorrect("java.lang.Short[][][]", Short[][][].class);
        assertCorrect("java.lang.Integer[][]", Integer[][].class);
        assertCorrect("java.lang.Long[]", Long[].class);
        assertCorrect("java.lang.Float[][]", Float[][].class);
        assertCorrect("java.lang.Double[][][]", Double[][][].class);
        assertCorrect("java.lang.Character[][][][]", Character[][][][].class);
    }

    @Test
    public void testParameterizedType() {
        assertCorrect("java.util.List<java.lang.Integer>", new TypeLiteral<List<Integer>>() {
        }.getType());
        assertCorrect("java.util.Map<java.lang.Integer, int[]>", new TypeLiteral<Map<Integer, int[]>>() {
        }.getType());

        assertCorrect("java.util.List<? extends java.lang.Integer>", new TypeLiteral<List<? extends Integer>>() {
        }.getType());
        assertCorrect("java.util.Map<? super int[][], java.util.List<?>>", new TypeLiteral<Map<? super int[][], List<?>>>() {
        }.getType());
    }

    @Test
    public void testParameterizedTypeArray() {
        assertCorrect("java.util.List<java.lang.Integer>[]", new TypeLiteral<List<Integer>[]>() {
        }.getType());
        assertCorrect("java.util.Map<java.lang.Integer, int[]>[][]", new TypeLiteral<Map<Integer, int[]>[][]>() {
        }.getType());
    }

    @Test
    public void testIncorrect() {
        assertIncorrect("");
        assertIncorrect(" ");
        assertIncorrect("\t");
        assertIncorrect("    ");
        assertIncorrect("  \n  ");

        assertIncorrect(".");
        assertIncorrect(",");
        assertIncorrect("[");
        assertIncorrect("]");
        assertIncorrect("<");
        assertIncorrect(">");

        assertIncorrect("int.");
        assertIncorrect("int,");
        assertIncorrect("int[");
        assertIncorrect("int]");
        assertIncorrect("int[[]");
        assertIncorrect("int[][");
        assertIncorrect("int[]]");
        assertIncorrect("int[0]");
        assertIncorrect("int<");
        assertIncorrect("int>");
        assertIncorrect("int<>");

        assertIncorrect("java.util.List<");
        assertIncorrect("java.util.List<>");
        assertIncorrect("java.util.List<java.lang.Integer");
        assertIncorrect("java.util.List<java.lang.Integer>>");
        assertIncorrect("java.util.List<java.util.List<java.lang.Integer");
        assertIncorrect("java.util.List<java.util.List<java.lang.Integer>");
        assertIncorrect("java.util.List<java.util.List<java.lang.Integer>>>");

        assertIncorrect("java.util.List<int>");
        assertIncorrect("java.util.Map<int, long>");

        assertIncorrect("java.lang.Integer.");
        assertIncorrect("java .lang.Integer");
        assertIncorrect("java. lang.Integer");
        assertIncorrect("java . lang.Integer");
        assertIncorrect(".java.lang.Integer");
        assertIncorrect(".java.lang.Integer.");

        assertIncorrect("java.lang.Integer[");
        assertIncorrect("java.lang.Integer[[]");
        assertIncorrect("java.lang.Integer[][");
        assertIncorrect("java.lang.Integer[]]");
        assertIncorrect("java.lang.Integer[0]");
    }

    private void assertCorrect(String str, Type expectedType) {
        assertEquals(expectedType, TypeParser.parse(str));
    }

    private void assertIncorrect(String str) {
        assertThrows(IllegalArgumentException.class, () -> TypeParser.parse(str));
    }
}
