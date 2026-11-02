package io.quarkus.deployment.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.util.IoUtil;

/**
 * Rewrites the fixtures, loads the rewritten classes in a class loader of their own and checks they behave like
 * the originals, lambda by lambda.
 */
public class LambdaMaterializerTest {

    /** A class with every kind of lambda and method reference the rewrite handles. */
    public static class Fixture {
        private int count;
        final int field = 100;

        public static Supplier<String> constant() {
            return () -> "constant";
        }

        public Function<Integer, Integer> addTo(int base) {
            return x -> x + base + field;
        }

        public static Function<String, Integer> parse() {
            return Integer::parseInt;
        }

        public static ToIntFunction<String> length() {
            return String::length;
        }

        public static Supplier<List<String>> list() {
            return ArrayList::new;
        }

        public static Runnable bound(StringBuilder sb) {
            return sb::reverse;
        }

        public static Comparator<String> comparing() {
            return (a, b) -> a.compareTo(b);
        }

        public static IntBinaryOperator sum() {
            return Integer::sum;
        }

        public static ToLongFunction<Integer> widen() {
            return Integer::intValue;
        }

        public Runnable priv() {
            return this::privateMethod;
        }

        /** Erases its parameter to Number, wider than the Integer receiver of the reference below. */
        public interface BoundedFunction<T extends Number> {
            int apply(T value);
        }

        public static BoundedFunction<Integer> narrowing() {
            return Integer::intValue;
        }

        private void privateMethod() {
            count++;
        }

        public static List<Object> exercise() {
            Fixture fixture = new Fixture();
            List<Object> results = new ArrayList<>();
            results.add(constant().get());
            results.add(fixture.addTo(5).apply(7));
            results.add(parse().apply("42"));
            results.add(length().applyAsInt("hello"));
            results.add(list().get().getClass().getName());
            StringBuilder sb = new StringBuilder("abc");
            bound(sb).run();
            results.add(sb.toString());
            results.add(comparing().compare("a", "b"));
            results.add(sum().applyAsInt(2, 3));
            results.add(widen().applyAsLong(9));
            Runnable priv = fixture.priv();
            priv.run();
            priv.run();
            results.add(fixture.count);
            results.add(narrowing().apply(17));
            results.add(constant() == constant());
            results.add(FixtureInterface.STATIC.get());
            results.add(new FixtureImpl().name().get());
            return results;
        }
    }

    /** An interface host: a lambda in the static initializer and one capturing {@code this} in a default method. */
    public interface FixtureInterface {
        Supplier<String> STATIC = () -> "static";

        String value();

        default Supplier<String> name() {
            return () -> "name:" + value();
        }
    }

    public static class FixtureImpl implements FixtureInterface {
        @Override
        public String value() {
            return "impl";
        }
    }

    @Test
    public void rewrittenLambdasBehaveLikeTheOriginals() throws Exception {
        List<Object> expected = Fixture.exercise();

        Map<String, byte[]> classes = new HashMap<>();
        int sites = 0;
        for (Class<?> host : List.of(Fixture.class, FixtureInterface.class)) {
            byte[] original = IoUtil.readClassAsBytes(getClass().getClassLoader(), host.getName());
            assertTrue(countMetafactorySites(original) > 0, host.getName() + " has no lambda to rewrite");
            Map<String, byte[]> generated = new HashMap<>();
            LambdaMaterializer.Result result = LambdaMaterializer.materialize(original,
                    (name, bytes) -> generated.put(name.replace(".class", "").replace('/', '.'), bytes));
            assertEquals(countMetafactorySites(original), result.callSites(),
                    "one call site rewritten per call site of " + host.getName());
            assertEquals(result.callSites(), generated.size(), "one generated class per call site");
            assertEquals(0, countMetafactorySites(result.hostClass()), "no call site left in " + host.getName());
            sites += result.callSites();
            classes.put(host.getName(), result.hostClass());
            classes.putAll(generated);
        }
        classes.put(FixtureImpl.class.getName(), IoUtil.readClassAsBytes(getClass().getClassLoader(),
                FixtureImpl.class.getName()));
        assertEquals(13, sites);

        RewrittenClassLoader loader = new RewrittenClassLoader(getClass().getClassLoader(), classes);
        Class<?> rewritten = loader.loadClass(Fixture.class.getName());
        assertNotSame(Fixture.class, rewritten);
        Method exercise = rewritten.getMethod("exercise");
        @SuppressWarnings("unchecked")
        List<Object> actual = (List<Object>) exercise.invoke(null);

        assertEquals(expected, actual);
        for (String name : classes.keySet()) {
            if (name.contains("$QuarkusLambda$")) {
                Class<?> generated = loader.loadClass(name);
                assertFalse(generated.isHidden());
                assertEquals(1, generated.getInterfaces().length, name + " implements the functional interface");
            }
        }
    }

    @Test
    public void classesWithoutLambdasAreLeftAlone() throws IOException {
        byte[] original = IoUtil.readClassAsBytes(getClass().getClassLoader(), FixtureImpl.class.getName());
        LambdaMaterializer.Result result = LambdaMaterializer.materialize(original,
                (name, bytes) -> fail("nothing should be generated for " + name));
        assertEquals(0, result.callSites());
        assertSame(original, result.hostClass());
    }

    private static int countMetafactorySites(byte[] classBytes) {
        int[] count = { 0 };
        new ClassReader(classBytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bsm, Object... args) {
                        if (bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
                            count[0]++;
                        }
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);
        return count[0];
    }

    /** Loads the rewritten classes itself, child first, and everything else from the parent. */
    private static final class RewrittenClassLoader extends ClassLoader {
        private final Map<String, byte[]> classes;

        RewrittenClassLoader(ClassLoader parent, Map<String, byte[]> classes) {
            super(parent);
            this.classes = classes;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    byte[] bytes = classes.get(name);
                    loaded = bytes != null ? defineClass(name, bytes, 0, bytes.length) : super.loadClass(name, false);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }
    }
}
