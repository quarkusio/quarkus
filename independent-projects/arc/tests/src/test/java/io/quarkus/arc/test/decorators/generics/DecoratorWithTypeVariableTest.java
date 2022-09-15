package io.quarkus.arc.test.decorators.generics;

import io.quarkus.arc.Arc;
import io.quarkus.arc.Priority;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DecoratorWithTypeVariableTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyParameterizedType.class, MyInterface.class, MyDecorator.class,
            MyDelegateBean.class, Contract.class);

    @Test
    public void testDecoration() {
        // Firstly verify that decorator was invoked and works, i.e. that build time resolution worked
        MyDelegateBean bean = Arc.container().instance(MyDelegateBean.class).get();
        Assertions.assertEquals(MyDecorator.class.getSimpleName(),
                bean.doSomething(new MyParameterizedType<>("test", new Contract())));

        // Secondly, assert that this decorator can be resolved at runtime via BM
        List<jakarta.enterprise.inject.spi.Decorator<?>> decoratorsFound = Arc.container().beanManager()
                .resolveDecorators(Set.of(new TypeLiteral<MyInterface<String, Contract>>() {
                }.getType()), Any.Literal.INSTANCE);
        Assertions.assertTrue(decoratorsFound.size() == 1);
    }

    public static class MyParameterizedType<K, V> {

        final K key;

        final V value;

        public MyParameterizedType(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "key=" + key.toString() + ", value=" + value.toString();
        }

    }

    public interface MyInterface<K, V> {

        String doSomething(MyParameterizedType<K, V> myParameterizedType);

    }

    @Decorator
    @Priority(1)
    public static class MyDecorator<K, V> implements MyInterface<K, V> {

        @Inject
        @Delegate
        @Any
        MyInterface<K, V> delegate;

        @Override
        public String doSomething(MyParameterizedType<K, V> myParameterizedType) {
            delegate.doSomething(myParameterizedType);
            // return something else to verify decoration
            return MyDecorator.class.getSimpleName();
        }

    }

    @ApplicationScoped
    @Unremovable
    public static class MyDelegateBean implements MyInterface<String, Contract> {

        @Override
        public String doSomething(MyParameterizedType<String, Contract> myConcreteType) {
            return MyDelegateBean.class.getSimpleName();
        }

    }

    public static class Contract {

    }
}
