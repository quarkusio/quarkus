package io.quarkus.qute.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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

import io.quarkus.qute.Engine;
import io.quarkus.qute.EngineBuilder;
import io.quarkus.qute.NamespaceResolver;
import io.quarkus.qute.Resolver;
import io.quarkus.qute.TestEvalContext;
import io.quarkus.qute.ValueResolver;

public class SimpleGeneratorTest {

    static Set<String> generatedTypes = new HashSet<>();

    @BeforeAll
    public static void init() throws IOException {
        TestClassOutput classOutput = new TestClassOutput();
        Index index = index(MyService.class, PublicMyService.class, BaseService.class, MyItem.class, String.class,
                CompletionStage.class, List.class, MyEnum.class);
        ClassInfo myServiceClazz = index.getClassByName(DotName.createSimple(MyService.class.getName()));
        ValueResolverGenerator generator = ValueResolverGenerator.builder().setIndex(index).setClassOutput(classOutput)
                .addClass(myServiceClazz)
                .addClass(index.getClassByName(DotName.createSimple(PublicMyService.class.getName())))
                .addClass(index.getClassByName(DotName.createSimple(MyItem.class.getName())))
                .addClass(index.getClassByName(DotName.createSimple(String.class.getName())))
                .addClass(index.getClassByName(DotName.createSimple(List.class.getName())))
                .addClass(index.getClassByName(DotName.createSimple(MyEnum.class.getName())))
                .build();

        generator.generate();
        generatedTypes.addAll(generator.getGeneratedTypes());

        ExtensionMethodGenerator extensionMethodGenerator = new ExtensionMethodGenerator(index, classOutput);
        MethodInfo extensionMethod = index.getClassByName(DotName.createSimple(MyService.class.getName())).method(
                "getDummy", Type.create(myServiceClazz.name(), Kind.CLASS), PrimitiveType.INT,
                Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS));
        extensionMethodGenerator.generate(extensionMethod, null, List.of(), null, null);
        extensionMethod = index.getClassByName(DotName.createSimple(MyService.class.getName())).method(
                "getDummy", Type.create(myServiceClazz.name(), Kind.CLASS), PrimitiveType.INT,
                PrimitiveType.LONG);
        extensionMethodGenerator.generate(extensionMethod, null, List.of(), null, null);
        extensionMethod = index.getClassByName(DotName.createSimple(MyService.class.getName())).method(
                "getDummyVarargs", Type.create(myServiceClazz.name(), Kind.CLASS), PrimitiveType.INT,
                Type.create(DotName.createSimple("[L" + String.class.getName() + ";"), Kind.ARRAY));
        extensionMethodGenerator.generate(extensionMethod, null, List.of(), null, null);
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
            if (generatedType.contains(ValueResolverGenerator.NAMESPACE_SUFFIX)) {
                builder.addNamespaceResolver((NamespaceResolver) newResolver(generatedType));
            } else {
                builder.addValueResolver((ValueResolver) newResolver(generatedType));
            }
        }
        Engine engine = builder.build();
        assertEquals(" FOO ", engine.parse("{#if isActive} {name.toUpperCase} {/if}").render(new MyService()));
        assertEquals(" FOO ", engine.parse("{#if isActiveObject} {name.toUpperCase} {/if}").render(new MyService()));
        assertEquals("", engine.parse("{#if isActiveObjectNull} {name.toUpperCase} {/if}").render(new MyService()));
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
        assertEquals("BAR::", engine.parse("{myEnum}::{myEnumNull}").render(new MyService()));

        // Namespace resolvers
        assertEquals("OK", engine.parse("{#if enum is MyEnum:BAR}OK{/if}").data("enum", MyEnum.BAR).render());
        assertEquals("one", engine.parse("{MyEnum:valueOf('ONE').name}").render());
        assertEquals("10", engine.parse("{io_quarkus_qute_generator_MyService:getDummy(5)}").render());
    }

    @Test
    public void testArrays() {
        Engine engine = Engine.builder().addDefaults().build();
        assertEquals("1,2,3,4,5,6,7,8,9,10,", engine.parse("{#for i in 10}{i_count},{/for}").render());
        assertEquals("0,1,2,3,4,5,6,7,8,9,", engine.parse("{#for i in 10}{i_index},{/for}").render());
        assertEquals("odd,even,odd,even,odd,even,odd,even,odd,even,",
                engine.parse("{#for i in 10}{i_indexParity},{/for}").render());
        assertEquals("true,false,true,false,true,",
                engine.parse("{#for i in 5}{i_odd},{/for}").render());
        assertEquals("false,true,false,true,false,",
                engine.parse("{#for i in 5}{i_even},{/for}").render());
        { //these two are not documented in the guide (https://quarkus.io/guides/qute-reference)
            assertEquals("true,false,true,false,true,",
                    engine.parse("{#for i in 5}{i_isOdd},{/for}").render());
            assertEquals("false,true,false,true,false,",
                    engine.parse("{#for i in 5}{i_isEven},{/for}").render());
        }
        assertEquals("true,true,true,true,false,", engine.parse("{#for i in 5}{i_hasNext},{/for}").render());
        assertEquals("false,false,false,false,true,", engine.parse("{#for i in 5}{i_isLast},{/for}").render());
        assertEquals("true,false,false,false,false,", engine.parse("{#for i in 5}{i_isFirst},{/for}").render());
    }

    public static Resolver newResolver(String className)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = SimpleGeneratorTest.class.getClassLoader();
        }
        Class<?> clazz = cl.loadClass(className);
        return (Resolver) clazz.getDeclaredConstructor().newInstance();
    }

    public static Index index(Class<?>... classes) throws IOException {
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
