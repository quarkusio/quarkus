package io.quarkus.test.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.mockito.Mockito;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.impl.HierarchyDiscovery;
import io.quarkus.arc.processor.Annotations;
import io.quarkus.arc.processor.BeanResolver;
import io.quarkus.arc.processor.Types;

class MockBeanConfiguratorImpl<T> implements MockBeanConfigurator<T> {

    final QuarkusComponentTestExtensionBuilder builder;
    final Class<?> beanClass;
    Set<Type> types;
    Set<Annotation> qualifiers;
    Class<? extends Annotation> scope;
    boolean alternative = false;
    Integer priority;
    String name;
    boolean defaultBean = false;

    Function<SyntheticCreationalContext<T>, T> create;

    Set<org.jboss.jandex.Type> jandexTypes;
    Set<AnnotationInstance> jandexQualifiers;

    public MockBeanConfiguratorImpl(QuarkusComponentTestExtensionBuilder builder, Class<?> beanClass) {
        this.builder = builder;
        this.beanClass = beanClass;
        this.types = new HierarchyDiscovery(beanClass).getTypeClosure();

        if (beanClass.isAnnotationPresent(Singleton.class)) {
            this.scope = Singleton.class;
        } else if (beanClass.isAnnotationPresent(ApplicationScoped.class)) {
            this.scope = ApplicationScoped.class;
        } else if (beanClass.isAnnotationPresent(RequestScoped.class)) {
            this.scope = RequestScoped.class;
        } else {
            this.scope = Dependent.class;
        }
        this.qualifiers = new HashSet<>();
        for (Annotation annotation : beanClass.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Qualifier.class)) {
                this.qualifiers.add(annotation);
            }
        }
        if (this.qualifiers.isEmpty()) {
            this.qualifiers.add(Default.Literal.INSTANCE);
        }

        if (beanClass.isAnnotationPresent(Alternative.class)) {
            this.alternative = true;
        }
        Named named = beanClass.getAnnotation(Named.class);
        if (named != null) {
            String val = named.value();
            if (!val.isBlank()) {
                this.name = val;
            } else {
                StringBuilder defaultName = new StringBuilder();
                defaultName.append(beanClass.getSimpleName());
                // URLMatcher becomes uRLMatcher
                defaultName.setCharAt(0, Character.toLowerCase(defaultName.charAt(0)));
                this.name = defaultName.toString();
            }
        }
        Priority priority = beanClass.getAnnotation(Priority.class);
        if (priority != null) {
            this.priority = priority.value();
        }
        if (beanClass.isAnnotationPresent(DefaultBean.class)) {
            this.defaultBean = true;
        }
    }

    @Override
    public MockBeanConfigurator<T> types(Class<?>... types) {
        this.types = Set.of(types);
        return this;
    }

    @Override
    public MockBeanConfigurator<T> types(Type types) {
        this.types = Set.of(types);
        return this;
    }

    @Override
    public MockBeanConfigurator<T> qualifiers(Annotation... qualifiers) {
        this.qualifiers = Set.of(qualifiers);
        return this;
    }

    @Override
    public MockBeanConfigurator<T> scope(Class<? extends Annotation> scope) {
        this.scope = scope;
        return this;
    }

    @Override
    public MockBeanConfigurator<T> name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public MockBeanConfigurator<T> alternative(boolean alternative) {
        this.alternative = alternative;
        return this;
    }

    @Override
    public MockBeanConfigurator<T> priority(int priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public MockBeanConfigurator<T> defaultBean(boolean defaultBean) {
        this.defaultBean = defaultBean;
        return this;
    }

    @Override
    public QuarkusComponentTestExtensionBuilder create(Function<SyntheticCreationalContext<T>, T> create) {
        this.create = create;
        return register();
    }

    @Override
    public QuarkusComponentTestExtensionBuilder createMockitoMock() {
        this.create = c -> QuarkusComponentTestExtension.cast(Mockito.mock(beanClass));
        return register();
    }

    @Override
    public QuarkusComponentTestExtensionBuilder createMockitoMock(Consumer<T> mockInitializer) {
        this.create = c -> {
            T mock = QuarkusComponentTestExtension.cast(Mockito.mock(beanClass));
            mockInitializer.accept(mock);
            return mock;
        };
        return register();
    }

    public QuarkusComponentTestExtensionBuilder register() {
        builder.registerMockBean(this);
        return builder;
    }

    boolean matches(BeanResolver beanResolver, org.jboss.jandex.Type requiredType, Set<AnnotationInstance> qualifiers) {
        return matchesType(requiredType, beanResolver) && hasQualifiers(qualifiers, beanResolver);
    }

    boolean matchesType(org.jboss.jandex.Type requiredType, BeanResolver beanResolver) {
        for (org.jboss.jandex.Type beanType : jandexTypes()) {
            if (beanResolver.matches(requiredType, beanType)) {
                return true;
            }
        }
        return false;
    }

    boolean hasQualifiers(Set<AnnotationInstance> requiredQualifiers, BeanResolver beanResolver) {
        for (AnnotationInstance qualifier : requiredQualifiers) {
            if (!beanResolver.hasQualifier(jandexQualifiers(), qualifier)) {
                return false;
            }
        }
        return true;
    }

    Set<org.jboss.jandex.Type> jandexTypes() {
        if (jandexTypes == null) {
            jandexTypes = new HashSet<>();
            for (Type type : types) {
                jandexTypes.add(Types.jandexType(type));
            }
        }
        return jandexTypes;
    }

    Set<AnnotationInstance> jandexQualifiers() {
        if (jandexQualifiers == null) {
            jandexQualifiers = new HashSet<>();
            for (Annotation qualifier : qualifiers) {
                jandexQualifiers.add(Annotations.jandexAnnotation(qualifier));
            }
        }
        return jandexQualifiers;
    }

}
