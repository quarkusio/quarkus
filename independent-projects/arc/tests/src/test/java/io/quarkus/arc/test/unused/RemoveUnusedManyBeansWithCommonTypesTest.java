package io.quarkus.arc.test.unused;

import jakarta.enterprise.context.Dependent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;

public class RemoveUnusedManyBeansWithCommonTypesTest extends RemoveUnusedComponentsTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyBean1.class, MyBean2.class, MyBean3.class, MyBean4.class, MyBean5.class, MyBean6.class,
                    MyBean7.class, MyBean8.class, MyBean9.class, MyBean10.class, MyBean11.class, MyBean12.class,
                    MyBean13.class, MyBean14.class, MyBean15.class, MyBean16.class)
            .removeUnusedBeans(true)
            .build();

    @Test
    public void test() {
        assertNotPresent(MyBean1.class);
        assertNotPresent(MyBean2.class);
        assertNotPresent(MyBean3.class);
        assertNotPresent(MyBean4.class);
        assertNotPresent(MyBean5.class);
        assertNotPresent(MyBean6.class);
        assertNotPresent(MyBean7.class);
        assertNotPresent(MyBean8.class);
        assertNotPresent(MyBean9.class);
        assertNotPresent(MyBean10.class);
        assertNotPresent(MyBean11.class);
        assertNotPresent(MyBean12.class);
        assertNotPresent(MyBean13.class);
        assertNotPresent(MyBean14.class);
        assertNotPresent(MyBean15.class);
        assertNotPresent(MyBean16.class);
    }

    interface MyGenericType1<T> {
    }

    interface MyGenericType2<T> {
    }

    @Dependent
    static class MyBean1 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean2 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean3 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean4 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean5 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean6 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean7 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean8 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean9 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean10 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean11 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean12 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean13 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean14 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean15 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }

    @Dependent
    static class MyBean16 implements MyGenericType1<String>, MyGenericType2<Integer> {
    }
}
