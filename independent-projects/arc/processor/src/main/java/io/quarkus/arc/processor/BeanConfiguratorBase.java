package io.quarkus.arc.processor;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.enterprise.context.NormalScope;
import javax.enterprise.context.spi.CreationalContext;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

/**
 * This construct is not thread-safe.
 */
public abstract class BeanConfiguratorBase<B extends BeanConfiguratorBase<B, T>, T> implements Consumer<AnnotationInstance> {

    protected final DotName implClazz;
    protected final Set<Type> types;
    protected final Set<AnnotationInstance> qualifiers;
    protected ScopeInfo scope;
    protected Integer alternativePriority;
    protected String name;
    protected Consumer<MethodCreator> creatorConsumer;
    protected Consumer<MethodCreator> destroyerConsumer;
    protected boolean defaultBean;
    protected boolean removable;
    protected final Map<String, Object> params;
    protected Type providerType;
    protected boolean forceApplicationClass;

    protected BeanConfiguratorBase(DotName implClazz) {
        this.implClazz = implClazz;
        this.types = new HashSet<>();
        this.qualifiers = new HashSet<>();
        this.scope = BuiltinScope.DEPENDENT.getInfo();
        this.removable = true;
        this.params = new HashMap<>();
    }

    protected abstract B self();

    /**
     * Read metadata from another configurator base.
     * 
     * @param base
     * @return self
     */
    public B read(BeanConfiguratorBase<?, ?> base) {
        types.clear();
        types.addAll(base.types);
        qualifiers.clear();
        qualifiers.addAll(base.qualifiers);
        forceApplicationClass = base.forceApplicationClass;
        scope(base.scope);
        if (base.alternativePriority != null) {
            alternativePriority(base.alternativePriority);
        }
        name(base.name);
        creator(base.creatorConsumer);
        destroyer(base.destroyerConsumer);
        if (base.defaultBean) {
            defaultBean();
        }
        removable = base.removable;
        params.clear();
        params.putAll(base.params);
        providerType(base.providerType);
        return self();
    }

    public B types(Class<?>... types) {
        for (Class<?> type : types) {
            addType(type);
        }
        return self();
    }

    public B types(Type... types) {
        Collections.addAll(this.types, types);
        return self();
    }

    public B addType(DotName className) {
        this.types.add(Type.create(className, Kind.CLASS));
        return self();
    }

    public B addType(Type type) {
        this.types.add(type);
        return self();
    }

    public B addType(Class<?> type) {
        return addType(DotName.createSimple(type.getName()));
    }

    public B addQualifier(Class<? extends Annotation> annotationClass) {
        return addQualifier(DotName.createSimple(annotationClass.getName()));
    }

    public B addQualifier(DotName annotationName) {
        return addQualifier(AnnotationInstance.create(annotationName, null, new AnnotationValue[] {}));
    }

    public B addQualifier(AnnotationInstance qualifier) {
        this.qualifiers.add(qualifier);
        return self();
    }

    public QualifierConfigurator<B> addQualifier() {
        return new QualifierConfigurator<B>(cast(this));
    }

    public B qualifiers(AnnotationInstance... qualifiers) {
        Collections.addAll(this.qualifiers, qualifiers);
        return self();
    }

    public B scope(ScopeInfo scope) {
        this.scope = scope;
        return self();
    }

    public B scope(Class<? extends Annotation> scope) {
        DotName scopeName = DotName.createSimple(scope.getName());
        BuiltinScope builtinScope = BuiltinScope.from(scopeName);
        if (builtinScope != null) {
            this.scope = builtinScope.getInfo();
        } else {
            this.scope = new ScopeInfo(scopeName, scope.isAnnotationPresent(NormalScope.class),
                    scope.isAnnotationPresent(Inherited.class));
        }
        return self();
    }

    public B name(String name) {
        this.name = name;
        return self();
    }

    /**
     * Unlike for the {@link #name(String)} method a new {@link javax.inject.Named} qualifier with the specified value is added
     * to the configured bean.
     * 
     * @param name
     * @return self
     */
    public B named(String name) {
        return name(name).addQualifier().annotation(DotNames.NAMED).addValue("value", name).done();
    }

    public B defaultBean() {
        this.defaultBean = true;
        return self();
    }

    public B unremovable() {
        this.removable = false;
        return self();
    }

    /**
     * Forces the bean to be considered an 'application class', so it will be defined in the runtime
     * ClassLoader and re-created on each redeployment.
     *
     * @return self
     */
    public B forceApplicationClass() {
        this.forceApplicationClass = true;
        return self();
    }

    public B alternativePriority(int priority) {
        this.alternativePriority = priority;
        return self();
    }

    public B param(String name, Class<?> value) {
        params.put(name, value);
        return self();
    }

    public B param(String name, int value) {
        params.put(name, value);
        return self();
    }

    public B param(String name, long value) {
        params.put(name, value);
        return self();
    }

    public B param(String name, double value) {
        params.put(name, value);
        return self();
    }

    public B param(String name, String value) {
        params.put(name, value);
        return self();
    }

    public B param(String name, boolean value) {
        params.put(name, value);
        return self();
    }

    /**
     * The provider type is the "real" type of the bean instance created via
     * {@link InjectableReferenceProvider#get(CreationalContext)}.
     * <p>
     * The container attempts to derive the provider type from the implementation class. However, in some cases it's better to
     * specify it manually.
     * 
     * @param providerType
     * @return self
     */
    public B providerType(Type providerType) {
        this.providerType = providerType;
        return self();
    }

    public <U extends T> B creator(Class<? extends BeanCreator<U>> creatorClazz) {
        return creator(mc -> {
            // return new FooBeanCreator().create(context, params)
            ResultHandle paramsHandle = mc.readInstanceField(
                    FieldDescriptor.of(mc.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                    mc.getThis());
            ResultHandle creatorHandle = mc.newInstance(MethodDescriptor.ofConstructor(creatorClazz));
            ResultHandle[] params = { mc.getMethodParam(0), paramsHandle };
            ResultHandle ret = mc.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(BeanCreator.class, "create", Object.class, CreationalContext.class, Map.class),
                    creatorHandle, params);
            mc.returnValue(ret);
        });
    }

    public <U extends T> B creator(Consumer<MethodCreator> methodCreatorConsumer) {
        this.creatorConsumer = methodCreatorConsumer;
        return cast(this);
    }

    public <U extends T> B destroyer(Class<? extends BeanDestroyer<U>> destroyerClazz) {
        return destroyer(mc -> {
            // new FooBeanDestroyer().destroy(instance, context, params)
            ResultHandle paramsHandle = mc.readInstanceField(
                    FieldDescriptor.of(mc.getMethodDescriptor().getDeclaringClass(), "params", Map.class),
                    mc.getThis());
            ResultHandle destoyerHandle = mc.newInstance(MethodDescriptor.ofConstructor(destroyerClazz));
            ResultHandle[] params = { mc.getMethodParam(0), mc.getMethodParam(1), paramsHandle };
            mc.invokeInterfaceMethod(
                    MethodDescriptor.ofMethod(BeanDestroyer.class, "destroy", void.class, Object.class, CreationalContext.class,
                            Map.class),
                    destoyerHandle, params);
            mc.returnValue(null);
        });
    }

    public <U extends T> B destroyer(Consumer<MethodCreator> methodCreatorConsumer) {
        this.destroyerConsumer = methodCreatorConsumer;
        return cast(this);
    }

    @SuppressWarnings("unchecked")
    protected static <T> T cast(Object obj) {
        return (T) obj;
    }

    @Override
    public void accept(AnnotationInstance qualifier) {
        addQualifier(qualifier);
    }

}
