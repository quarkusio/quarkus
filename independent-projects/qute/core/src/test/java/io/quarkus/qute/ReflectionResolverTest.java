package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.ExpressionImpl.PartImpl;
import io.quarkus.qute.ParserTest.Foo;

public class ReflectionResolverTest {

    @Test
    public void testReflectionResolver() {
        Map<Integer, String> treeMap = new TreeMap<>(Integer::compare);
        treeMap.put(2, "bar");
        treeMap.put(1, "foo");
        assertEquals("foo::o", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{map.entrySet.iterator.next.value}::{str.charAt(1)}").data("map", treeMap, "str", "foo")
                .render());
    }

    @Test
    public void testFieldAccessor() {
        assertEquals("box", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{foo.name}").data("foo", new Foo("box")).render());
    }

    @Test
    public void testMethodWithParameter() {
        assertEquals("3", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{foo.computeLength(foo.name)}").data("foo", new Foo("box")).render());
    }

    @Test
    public void testMethodWithParameterNotFound() {
        assertEquals("NOT_FOUND", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{foo.computeLength(true) ?: 'NOT_FOUND'}").data("foo", new Foo("box")).render());
    }

    @Test
    public void testMethodWithVarargs() {
        assertEquals("box:box:", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{foo.compute(foo.name,1,2)}").data("foo", new Foo("box")).render());
    }

    @Test
    public void testStaticMembersIgnored() {
        assertEquals("baz::baz", Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{foo.bar ?: 'baz'}::{foo.BAR ?: 'baz'}").data("foo", new Foo("box")).render());
    }

    @Test
    public void testCachedResolver() {
        Template template = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver()).build()
                .parse("{foo.name}::{foo.name.repeat(5)}::{foo.name.repeat(n)}");
        Expression fooName = template.findExpression(e -> e.toOriginalString().equals("foo.name"));
        Expression fooNameRepeat5 = template.findExpression(e -> e.toOriginalString().equals("foo.name.repeat(5)"));
        Expression fooNameRepeatN = template.findExpression(e -> e.toOriginalString().equals("foo.name.repeat(n)"));
        PartImpl fooNamePart = (PartImpl) fooName.getParts().get(1);
        PartImpl fooNameRepeat5Part = (PartImpl) fooNameRepeat5.getParts().get(2);
        PartImpl fooNameRepeatNPart = (PartImpl) fooNameRepeatN.getParts().get(2);
        assertNull(fooNamePart.cachedResolver);
        assertNull(fooNameRepeat5Part.cachedResolver);
        assertNull(fooNameRepeatNPart.cachedResolver);
        assertEquals("box::boxboxboxboxbox::box", template.data("foo", new Foo("box"), "n", 1).render());
        assertEquals("box::boxboxboxboxbox::boxbox", template.data("foo", new Foo("box"), "n", 2).render());
        assertNotNull(fooNamePart.cachedResolver);
        assertNotNull(fooNameRepeat5Part.cachedResolver);
        assertNotNull(fooNameRepeatNPart.cachedResolver);
        assertTrue(fooNamePart.cachedResolver instanceof ReflectionValueResolver.AccessorResolver);
        assertTrue(fooNameRepeat5Part.cachedResolver instanceof ReflectionValueResolver.AccessorResolver);
        assertTrue(fooNameRepeatNPart.cachedResolver instanceof ReflectionValueResolver.CandidateResolver);
    }

    enum Status {
        ACTIVE,
        INACTIVE
    }

    @Test
    public void testSecurity() throws NoSuchMethodException, ClassNotFoundException {
        Engine engine = Engine.builder()
                .addDefaults()
                .addSectionHelper(new EvalSectionHelper.Factory())
                .addValueResolver(new ReflectionValueResolver())
                .build();

        Enum<?> enumValue = Status.ACTIVE;

        // java.lang.Class is not a safe declaring class
        try {
            engine.parse(
                    "{this.declaringClass.classLoader"
                            + ".loadClass('java.lang.Runtime')"
                            + ".getMethod('getRuntime').invoke(null)"
                            + ".exec('id').inputReader().readLine()}")
                    .instance().data(enumValue).render();
            fail();
        } catch (TemplateException expected) {
            assertTrue(expected.getMessage().startsWith(
                    "Rendering error: Property \"classLoader\" not found on the base object \"java.lang.Class\""),
                    expected.getMessage());
        }

        // Any ClassLoader is not a safe declaring class
        try {
            engine.parse(
                    "{this.loadClass('javax.naming.InitialContext')"
                            + ".newInstance().lookup('ldap://boom')}")
                    .instance().data(ReflectionResolverTest.class.getClassLoader()).render();
            fail();
        } catch (TemplateException expected) {
            assertTrue(expected.getMessage().startsWith(
                    "Rendering error: Method \"loadClass('javax.naming.InitialContext')\" not found on the base object"),
                    expected.getMessage());
        }

        // java.lang.Class is not a safe declaring class - {#eval}
        try {
            engine.parse(
                    "{#eval myTemp /}")
                    .instance()
                    .data("enumValue", enumValue)
                    .data("myTemp", "{enumValue.declaringClass.classLoader"
                            + ".loadClass('java.lang.Runtime')"
                            + ".getMethod('getRuntime').invoke(null)"
                            + ".exec('id').inputReader().readLine()}")
                    .render();
            fail();
        } catch (TemplateException expected) {
            assertTrue(expected.getMessage().startsWith(
                    "Rendering error: Property \"classLoader\" not found on the base object \"java.lang.Class\""),
                    expected.getMessage());
        }

        // java.lang.reflect.Method is not a safe declaring class
        try {
            Method method = this.getClass().getClassLoader().loadClass("java.lang.Runtime").getMethod("getRuntime");
            engine.parse(
                    "{this.invoke(null).exec('id').inputReader().readLine()}")
                    .instance().data(method).render();
            fail();
        } catch (TemplateException expected) {
            assertTrue(expected.getMessage().startsWith(
                    "Rendering error: Method \"invoke(null)\" not found on the base object \"java.lang.reflect.Method\""),
                    expected.getMessage());
        }
    }

    public static class Foo {

        public final String name;

        public static final String BAR = "bar";

        public Foo(String name) {
            this.name = name;
        }

        public int computeLength(String val) {
            return val.length();
        }

        public int computeLength(Double val) {
            return val.intValue();
        }

        public String compute(String val, int... counts) {
            StringBuilder builder = new StringBuilder();
            IntStream.of(counts).forEach(i -> builder.append(val).append(":"));
            return builder.toString();
        }

        public static String bar() {
            return "BAR";
        }

    }

}
