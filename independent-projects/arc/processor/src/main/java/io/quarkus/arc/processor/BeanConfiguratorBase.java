package io.quarkus.arc.processor;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;

/**
 * This construct is not thread-safe.
 */
public abstract class BeanConfiguratorBase<THIS extends BeanConfiguratorBase<THIS, T>, T> extends ConfiguratorBase<THIS>
        implements Consumer<AnnotationInstance> {

    protected final DotName implClazz;
    protected final Set<Type> types;
    protected final Set<AnnotationInstance> qualifiers;
    protected ScopeInfo scope;
    protected Boolean alternative;
    protected final List<StereotypeInfo> stereotypes;
    protected String name;
    protected Consumer<MethodCreator> creatorConsumer;
    protected Consumer<MethodCreator> destroyerConsumer;
    protected boolean defaultBean;
    protected boolean removable;
    protected Type providerType;
    protected boolean forceApplicationClass;
    protected String targetPackageName;
    protected Integer priority;

    protected BeanConfiguratorBase(DotName implClazz) {
        this.implClazz = implClazz;
        this.types = new HashSet<>();
        this.qualifiers = new HashSet<>();
        this.stereotypes = new ArrayList<>();
        this.removable = true;
    }

    /**
     * Read metadata from another configurator base.
     *
     * @param base
     * @return self
     */
    public THIS read(BeanConfiguratorBase<?, ?> base) {
        super.read(base);
        types.clear();
        types.addAll(base.types);
        qualifiers.clear();
        qualifiers.addAll(base.qualifiers);
        forceApplicationClass = base.forceApplicationClass;
        targetPackageName = base.targetPackageName;
        scope(base.scope);
        alternative = base.alternative;
        priority = base.priority;
        stereotypes.clear();
        stereotypes.addAll(base.stereotypes);
        name(base.name);
        creator(base.creatorConsumer);
        destroyer(base.destroyerConsumer);
        if (base.defaultBean) {
            defaultBean();
        }
        removable = base.removable;
        providerType(base.providerType);
        return self();
    }

    public THIS types(Class<?>... types) {
        for (Class<?> type : types) {
            addType(type);
        }
        return self();
    }

    public THIS types(Type... types) {
        Collections.addAll(this.types, types);
        return self();
    }

    public THIS addType(DotName className) {
        this.types.add(Type.create(className, Kind.CLASS));
        return self();
    }

    public THIS addType(Type type) {
        this.types.add(type);
        return self();
    }

    public THIS addType(Class<?> type) {
        return addType(DotName.createSimple(type.getName()));
    }

    public THIS addQualifier(Class<? extends Annotation> annotationClass) {
        return addQualifier(DotName.createSimple(annotationClass.getName()));
    }

    public THIS addQualifier(DotName annotationName) {
        return addQualifier(AnnotationInstance.create(annotationName, null, new AnnotationValue[] {}));
    }

    public THIS addQualifier(AnnotationInstance qualifier) {
        this.qualifiers.add(qualifier);
        return self();
    }

    public QualifierConfigurator<THIS> addQualifier() {
        return new QualifierConfigurator<>(cast(this));
    }

    public THIS qualifiers(AnnotationInstance... qualifiers) {
        Collections.addAll(this.qualifiers, qualifiers);
        return self();
    }

    public THIS scope(ScopeInfo scope) {
        this.scope = scope;
        return self();
    }

    public THIS scope(Class<? extends Annotation> scope) {
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

    public THIS name(String name) {
        this.name = name;
        return self();
    }

    /**
     * Unlike for the {@link #name(String)} method a new {@link jakarta.inject.Named} qualifier with the specified value is
     * added
     * to the configured bean.
     *
     * @param name
     * @return self
     */
    public THIS named(String name) {
        return name(name).addQualifier().annotation(DotNames.NAMED).addValue("value", name).done();
    }

    public THIS defaultBean() {
        this.defaultBean = true;
        return self();
    }

    public THIS unremovable() {
        this.removable = false;
        return self();
    }

    /**
     * Forces the bean to be considered an 'application class', so it will be defined in the runtime
     * ClassLoader and re-created on each redeployment.
     *
     * @return self
     */
    public THIS forceApplicationClass() {
        this.forceApplicationClass = true;
        return self();
    }

    public THIS targetPackageName(String name) {
        this.targetPackageName = name;
        return self();
    }

    public THIS alternativePriority(int value) {
        this.alternative = true;
        this.priority = value;
        return self();
    }

    public THIS priority(int value) {
        this.priority = value;
        return self();
    }

    public THIS addStereotype(StereotypeInfo stereotype) {
        this.stereotypes.add(stereotype);
        return self();
    }

    public THIS stereotypes(StereotypeInfo... stereotypes) {
        Collections.addAll(this.stereotypes, stereotypes);
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
    public THIS providerType(Type providerType) {
        this.providerType = providerType;
        return self();
    }

    public <U extends T> THIS creator(Class<? extends BeanCreator<U>> creatorClazz) {
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

    public THIS creator(Consumer<MethodCreator> methodCreatorConsumer) {
        this.creatorConsumer = methodCreatorConsumer;
        return cast(this);
    }

    public <U extends T> THIS destroyer(Class<? extends BeanDestroyer<U>> destroyerClazz) {
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

    public THIS destroyer(Consumer<MethodCreator> methodCreatorConsumer) {
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
