package io.quarkus.qute.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.TestEvalContext;
import io.quarkus.qute.ValueResolver;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
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
        ClassInfo myServiceClazz = index.getClassByName(DotName.createSimple(MyService.class.getName()));
        ValueResolverGenerator generator = ValueResolverGenerator.builder().setIndex(index).setClassOutput(classOutput)
                .addClass(myServiceClazz)
                .addClass(index.getClassByName(DotName.createSimple(PublicMyService.class.getName())))
                .addClass(index.getClassByName(DotName.createSimple(MyItem.class.getName())))
                .addClass(index.getClassByName(DotName.createSimple(String.class.getName())))
                .addClass(index.getClassByName(DotName.createSimple(List.class.getName())))
                .build();

        generator.generate();
        generatedTypes.addAll(generator.getGeneratedTypes());

        ExtensionMethodGenerator extensionMethodGenerator = new ExtensionMethodGenerator(index, classOutput);
        MethodInfo extensionMethod = index.getClassByName(DotName.createSimple(MyService.class.getName())).method(
                "getDummy", Type.create(myServiceClazz.name(), Kind.CLASS), PrimitiveType.INT,
                Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS));
        extensionMethodGenerator.generate(extensionMethod, null, null, null);
        extensionMethod = index.getClassByName(DotName.createSimple(MyService.class.getName())).method(
                "getDummy", Type.create(myServiceClazz.name(), Kind.CLASS), PrimitiveType.INT,
                PrimitiveType.LONG);
        extensionMethodGenerator.generate(extensionMethod, null, null, null);
        extensionMethod = index.getClassByName(DotName.createSimple(MyService.class.getName())).method(
                "getDummyVarargs", Type.create(myServiceClazz.name(), Kind.CLASS), PrimitiveType.INT,
                Type.create(DotName.createSimple("[L" + String.class.getName() + ";"), Kind.ARRAY));
        extensionMethodGenerator.generate(extensionMethod, null, null, null);
        generatedTypes.addAll(extensionMethodGenerator.getGeneratedTypes());
    }

    @Test
    public void testGenerator() throws Exception {
        Class<?> clazz = SimpleGeneratorTest.class.getClassLoader()
                .loadClass("io.quarkus.qute.generator.MyService_ValueResolver");
        ValueResolver resolver = (ValueResolver) clazz.getDeclaredConstructor().newInstance();
        assertEquals("Foo",
                resolver.resolve(new TestEvalContext(new MyService(), "getName", null))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS).toString());
        assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]",
                resolver.resolve(new TestEvalContext(new MyService(), "getList",
                        e -> "foo".equals(e.getParts().get(0).getName()) ? CompletableFuture.completedFuture("foo")
                                : CompletableFuture.completedFuture(Integer.valueOf(10)),
                        "1", "foo"))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS).toString());
        assertEquals("oof",
                resolver.resolve(new TestEvalContext(new MyService(), "getTestName", null))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS).toString());
        assertEquals("Emma",
                resolver.resolve(new TestEvalContext(new MyService(), "getAnotherTestName",
                        v -> CompletableFuture.completedFuture(v.getParts().get(0).getName()), "Emma"))
                        .toCompletableFuture().get(1, TimeUnit.SECONDS).toString());
        assertEquals("NOT_FOUND",
                resolver.resolve(new TestEvalContext(new MyService(), "surname", null))
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
        assertEquals(" FOO ", engine.parse("{#if active} {name.toUpperCase} {/if}").render(new MyService()));
        assertEquals(" FOO ", engine.parse("{#if !hasItems} {name.toUpperCase} {/if}").render(new MyService()));
        assertEquals(" FOO ", engine.parse("{#if !items} {name.toUpperCase} {/if}").render(new MyService()));
        assertEquals("OK", engine.parse("{#if this.getList(5).size == 5}OK{/if}").render(new MyService()));
        assertEquals("2", engine.parse("{this.getListVarargs('foo','bar').size}").render(new MyService()));
        assertEquals("NOT_FOUND", engine.parse("{this.getAnotherTestName(1).or('NOT_FOUND')}").render(new MyService()));
        assertEquals("Martin NOT_FOUND OK NOT_FOUND",
                engine.parse("{name} {surname.or('NOT_FOUND')} {isStatic ?: 'OK'} {base.or('NOT_FOUND')}")
                        .render(new PublicMyService()));
        assertEquals("foo NOT_FOUND", engine.parse("{id} {bar.or('NOT_FOUND')}").render(new MyItem()));
        // Param types don't match - NOT_FOUND
        assertEquals("NOT_FOUND", engine.parse("{this.getList(5,5).or('NOT_FOUND')}").render(new MyService()));
        // Test multiple extension methods with the same number of parameters
        assertEquals("1", engine.parse("{service.getDummy(5,2l).size}").data("service", new MyService()).render());
        // No extension method matches the param types
        assertEquals("NOT_FOUND",
                engine.parse("{service.getDummy(5,resultNotFound.or(false)).or('NOT_FOUND')}").data("service", new MyService())
                        .render());
        // Extension method with varargs
        assertEquals("alphabravo",
                engine.parse("{#each service.getDummyVarargs(5,'alpha','bravo')}{it}{/}").data("service", new MyService())
                        .render());
        assertEquals("5",
                engine.parse("{#each service.getDummyVarargs(5)}{it}{/}").data("service", new MyService())
                        .render());
    }

    private ValueResolver newResolver(String className)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = SimpleGeneratorTest.class.getClassLoader();
        }
        Class<?> clazz = cl.loadClass(className);
        return (ValueResolver) clazz.getDeclaredConstructor().newInstance();
    }

    static Index index(Class<?>... classes) throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> clazz : classes) {
            try (InputStream stream = SimpleGeneratorTest.class.getClassLoader()
                    .getResourceAsStream(clazz.getName().replace('.', '/') + ".class")) {
                indexer.index(stream);
            }
        }
        return indexer.complete();
    }

}
