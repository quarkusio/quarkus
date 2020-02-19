package io.quarkus.qute.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.Expression;
import io.quarkus.qute.ImmutableList;
import io.quarkus.qute.ValueResolver;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SimpleGeneratorTest {

    static Set<String> generatedTypes = new HashSet<>();

    @BeforeAll
    public static void init() throws IOException {
        TestClassOutput classOutput = new TestClassOutput();
        Index index = index(MyService.class, PublicMyService.class, BaseService.class, MyItem.class, String.class,
                CompletionStage.class,
                List.class);
        ValueResolverGenerator generator = new ValueResolverGenerator(index, classOutput, Collections.emptyMap());
        ClassInfo myServiceClazz = index.getClassByName(DotName.createSimple(MyService.class.getName()));
        generator.generate(myServiceClazz);
        generator.generate(index.getClassByName(DotName.createSimple(PublicMyService.class.getName())));
        generator.generate(index.getClassByName(DotName.createSimple(MyItem.class.getName())));
        generator.generate(index.getClassByName(DotName.createSimple(String.class.getName())));
        generator.generate(index.getClassByName(DotName.createSimple(List.class.getName())));
        generatedTypes.addAll(generator.getGeneratedTypes());

        ExtensionMethodGenerator extensionMethodGenerator = new ExtensionMethodGenerator(classOutput);
        MethodInfo extensionMethod = index.getClassByName(DotName.createSimple(MyService.class.getName())).method(
                "getDummy", Type.create(myServiceClazz.name(), Kind.CLASS), PrimitiveType.INT,
                Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS));
        extensionMethodGenerator.generate(extensionMethod, null);
        generatedTypes.addAll(extensionMethodGenerator.getGeneratedTypes());
    }

    @Test
    public void testGenerator() throws Exception {
        Class<?> clazz = SimpleGeneratorTest.class.getClassLoader()
                .loadClass("io.quarkus.qute.generator.MyService_ValueResolver");
        ValueResolver resolver = (ValueResolver) clazz.newInstance();
        assertEquals("Foo",
                resolver.resolve(new TestEvalContext(new MyService(), "getName", Collections.emptyList(), null))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS).toString());
        assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]",
                resolver.resolve(new TestEvalContext(new MyService(), "getList", ImmutableList.of("1", "foo"),
                        e -> "foo".equals(e.parts.get(0)) ? CompletableFuture.completedFuture("foo")
                                : CompletableFuture.completedFuture(Integer.valueOf(10))))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS).toString());
        assertEquals("oof",
                resolver.resolve(new TestEvalContext(new MyService(), "getTestName", Collections.emptyList(), null))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS).toString());
        assertEquals("Emma",
                resolver.resolve(new TestEvalContext(new MyService(), "getAnotherTestName", Collections.singletonList("Emma"),
                        v -> CompletableFuture.completedFuture(v.parts.get(0))))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS).toString());
        assertEquals("NOT_FOUND",
                resolver.resolve(new TestEvalContext(new MyService(), "surname", Collections.emptyList(), null))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS).toString());
    }

    @Test
    public void testWithEngine() throws Exception {
        try {
            newResolver("io.quarkus.qute.generator.BaseService_ValueResolver");
            fail();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException expected) {
        }

        EngineBuilder builder = Engine.builder().addDefaults();
        for (String generatedType : generatedTypes) {
            builder.addValueResolver(newResolver(generatedType));
        }
        Engine engine = builder.build();
        assertEquals(" FOO ", engine.parse("{#if isActive} {name.toUpperCase} {/if}").render(new MyService()));
        assertEquals("OK", engine.parse("{#if this.getList(5).size == 5}OK{/if}").render(new MyService()));
        assertEquals("Martin NOT_FOUND OK NOT_FOUND",
                engine.parse("{name} {surname} {isStatic ?: 'OK'} {base}").render(new PublicMyService()));
        assertEquals("foo NOT_FOUND", engine.parse("{id} {bar}").render(new MyItem()));
        try {
            engine.parse("{this.getList(5,5)}").render(new MyService());
            fail();
        } catch (IllegalStateException e) {
            assertClassCastException(e);
        }
        try {
            engine.parse("{service.getDummy(5,resultNotFound)}").data("service", new MyService()).render();
            fail();
        } catch (IllegalStateException e) {
            assertClassCastException(e);
        }
    }

    private void assertClassCastException(Exception e) {
        if (e.getCause() instanceof ExecutionException) {
            ExecutionException ex = (ExecutionException) e.getCause();
            if (ex.getCause() instanceof ClassCastException) {
                return;
            }
        }
        fail("Unexpected exception thrown: ", e);
    }

    private ValueResolver newResolver(String className)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = SimpleGeneratorTest.class.getClassLoader();
        }
        Class<?> clazz = cl.loadClass(className);
        return (ValueResolver) clazz.newInstance();
    }

    private static Index index(Class<?>... classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (InputStream stream = SimpleGeneratorTest.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }

    static class TestEvalContext implements EvalContext {

        private final Object base;
        private final String name;
        private final List<String> params;
        private Function<Expression, CompletionStage<Object>> evaluate;

        public TestEvalContext(Object base, String name, List<String> params,
                Function<Expression, CompletionStage<Object>> evaluate) {
            this.base = base;
            this.name = name;
            this.params = params;
            this.evaluate = evaluate;
        }

        @Override
        public Object getBase() {
            return base;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public List<String> getParams() {
            return params;
        }

        @Override
        public CompletionStage<Object> evaluate(String expression) {
            return evaluate(Expression.from(expression));
        }

        @Override
        public CompletionStage<Object> evaluate(Expression expression) {
            return evaluate.apply(expression);
        }

    }

}
