package io.quarkus.arc.processor;

import io.quarkus.arc.AlternativePriority;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.VetoedProducer;
import io.quarkus.arc.impl.ComputingCache;
import java.io.Serializable;
import java.lang.annotation.Repeatable;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.decorator.Decorator;
import javax.decorator.Delegate;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Intercepted;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.TransientReference;
import javax.enterprise.inject.Typed;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.EventMetadata;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.interceptor.AroundConstruct;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

public final class DotNames {

    private static final ComputingCache<String, DotName> NAMES = new ComputingCache<>(DotNames::create);

    public static final DotName OBJECT = create(Object.class);
    public static final DotName OBSERVES = create(Observes.class);
    public static final DotName OBSERVES_ASYNC = create(ObservesAsync.class);
    public static final DotName PRODUCES = create(Produces.class);
    public static final DotName DISPOSES = create(Disposes.class);
    public static final DotName QUALIFIER = create(Qualifier.class);
    public static final DotName REPEATABLE = create(Repeatable.class);
    public static final DotName NONBINDING = create(Nonbinding.class);
    public static final DotName INJECT = create(Inject.class);
    public static final DotName POST_CONSTRUCT = create(PostConstruct.class);
    public static final DotName PRE_DESTROY = create(PreDestroy.class);
    public static final DotName INSTANCE = create(Instance.class);
    public static final DotName INJECTABLE_INSTANCE = create(InjectableInstance.class);
    public static final DotName PROVIDER = create(Provider.class);
    public static final DotName INJECTION_POINT = create(InjectionPoint.class);
    public static final DotName INTERCEPTOR = create(Interceptor.class);
    public static final DotName INTERCEPTOR_BINDING = create(InterceptorBinding.class);
    public static final DotName INTERCEPTED = create(Intercepted.class);
    public static final DotName AROUND_INVOKE = create(AroundInvoke.class);
    public static final DotName AROUND_CONSTRUCT = create(AroundConstruct.class);
    public static final DotName PRIORITY = create(Priority.class);
    public static final DotName DEFAULT = create(Default.class);
    public static final DotName ANY = create(Any.class);
    public static final DotName BEAN = create(Bean.class);
    public static final DotName INJECTABLE_BEAN = create(InjectableBean.class);
    public static final DotName BEAN_MANAGER = create(BeanManager.class);
    public static final DotName EVENT = create(Event.class);
    public static final DotName EVENT_METADATA = create(EventMetadata.class);
    public static final DotName ALTERNATIVE = create(Alternative.class);
    public static final DotName ALTERNATIVE_PRIORITY = create(AlternativePriority.class);
    public static final DotName DEFAULT_BEAN = create(DefaultBean.class);
    public static final DotName SINGLETON = create(Singleton.class);
    public static final DotName APPLICATION_SCOPED = create(ApplicationScoped.class);
    public static final DotName STEREOTYPE = create(Stereotype.class);
    public static final DotName TYPED = create(Typed.class);
    public static final DotName VETOED = create(Vetoed.class);
    public static final DotName CLASS = create(Class.class);
    public static final DotName ENUM = create(Enum.class);
    public static final DotName EXTENSION = create(Extension.class);
    public static final DotName OPTIONAL = create(Optional.class);
    public static final DotName OPTIONAL_INT = create(OptionalInt.class);
    public static final DotName OPTIONAL_LONG = create(OptionalLong.class);
    public static final DotName OPTIONAL_DOUBLE = create(OptionalDouble.class);
    public static final DotName NAMED = create(Named.class);
    public static final DotName ACTIVATE_REQUEST_CONTEXT = create(ActivateRequestContext.class);
    public static final DotName TRANSACTION_PHASE = create(TransactionPhase.class);
    public static final DotName INITIALIZED = create(Initialized.class);
    public static final DotName TRANSIENT_REFERENCE = create(TransientReference.class);
    public static final DotName INVOCATION_CONTEXT = create(InvocationContext.class);
    public static final DotName ARC_INVOCATION_CONTEXT = create(ArcInvocationContext.class);
    public static final DotName DECORATOR = create(Decorator.class);
    public static final DotName DELEGATE = create(Delegate.class);
    public static final DotName SERIALIZABLE = create(Serializable.class);
    public static final DotName UNREMOVABLE = create(Unremovable.class);
    public static final DotName VETOED_PRODUCER = create(VetoedProducer.class);

    public static final DotName BOOLEAN = create(Boolean.class);
    public static final DotName BYTE = create(Byte.class);
    public static final DotName CHARACTER = create(Character.class);
    public static final DotName DOUBLE = create(Double.class);
    public static final DotName FLOAT = create(Float.class);
    public static final DotName INTEGER = create(Integer.class);
    public static final DotName LONG = create(Long.class);
    public static final DotName SHORT = create(Short.class);
    public static final DotName STRING = create(String.class);

    public static final DotName DEPRECATED = create(Deprecated.class);

    private DotNames() {
    }

    /**
     * Note that this method does not attempt to detect a nested class because the computing cache is shared with the
     * {@link #create(String)} variant and so the results would be inconsistent. Therefore, this method should only be used for
     * top-level classes.
     *
     * @param clazz
     * @return the computed dot name
     */
    static DotName create(Class<?> clazz) {
        return create(clazz.getName());
    }

    /**
     * Note that the dollar sign is a valid character for class names so we cannot detect a nested class here. Therefore, this
     * method returns a dot name for which {@link DotName#local()} returns {@code Foo$Bar} for the parameter
     * "com.foo.Foo$Bar".
     *
     * @param name
     * @return the computed dot name
     */
    static DotName create(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            return DotName.createComponentized(null, name);
        }
        String prefix = name.substring(0, lastDot);
        DotName prefixName = NAMES.getValue(prefix);
        String local = name.substring(lastDot + 1);
        return DotName.createComponentized(prefixName, local);
    }

    /**
     * @param clazz
     * @return the simple name for the given top-level or nested class
     */
    public static String simpleName(ClassInfo clazz) {
        switch (clazz.nestingType()) {
            case TOP_LEVEL:
                return simpleName(clazz.name());
            case INNER:
                // Nested class
                // com.foo.Foo$Bar -> Bar
                return clazz.simpleName();
            default:
                throw new IllegalStateException("Unsupported nesting type: " + clazz);
        }
    }

    /**
     * @param dotName
     * @see #simpleName(String)
     */
    public static String simpleName(DotName dotName) {
        return simpleName(dotName.toString());
    }

    /**
     * Note that dollar sign is a valid character for class names so we cannot detect a nested class here. Therefore, this
     * method returns "Foo$Bar" for the parameter "com.foo.Foo$Bar". Use {@link #simpleName(ClassInfo)} when you need to
     * distinguish
     * the nested classes.
     *
     * @param name
     * @return the simple name
     */
    public static String simpleName(String name) {
        return name.contains(".") ? name.substring(name.lastIndexOf(".") + 1, name.length()) : name;
    }

    public static String packageName(DotName dotName) {
        String name = dotName.toString();
        int index = name.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        return name.substring(0, index);
    }

    /**
     * Returns a package name with a trailing '/'. If the class is in the default package then this returns
     * the empty string.
     * <p>
     * This method should be used to determine the package to generate classes in to ensure the default package is handled
     * correctly.
     */
    public static String internalPackageNameWithTrailingSlash(DotName dotName) {
        String name = dotName.toString();
        int index = name.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        return name.substring(0, index).replace('.', '/') + '/';
    }

}
