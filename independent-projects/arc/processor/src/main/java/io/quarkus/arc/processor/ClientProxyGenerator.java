package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static java.lang.constant.ConstantDescs.CD_Object;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.copyTypeParameters;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.genericTypeOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.genericTypeOfClass;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.genericTypeOfThrows;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.impl.Mockable;
import io.quarkus.arc.processor.Methods.MethodKey;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.GenericType;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.InterfaceMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 *
 * @author Martin Kouba
 */
public class ClientProxyGenerator extends AbstractGenerator {

    static final String CLIENT_PROXY_SUFFIX = "_ClientProxy";

    static final String DELEGATE_METHOD_NAME = "arc$delegate";
    static final String SET_MOCK_METHOD_NAME = "arc$setMock";
    static final String CLEAR_MOCK_METHOD_NAME = "arc$clearMock";
    static final String GET_CONTEXTUAL_INSTANCE_METHOD_NAME = "arc_contextualInstance";
    static final String GET_BEAN = "arc_bean";
    static final String BEAN_FIELD = "bean";
    static final String MOCK_FIELD = "mock";
    static final String CONTEXT_FIELD = "context";

    private final Predicate<DotName> applicationClassPredicate;
    private final boolean mockable;
    private final Set<String> existingClasses;
    // We optimize the access to the delegate if a single context is registered for a given scope
    private final Set<DotName> singleContextNormalScopes;

    public ClientProxyGenerator(Predicate<DotName> applicationClassPredicate, boolean generateSources, boolean mockable,
            ReflectionRegistration reflectionRegistration, Set<String> existingClasses,
            Set<DotName> singleContextNormalScopes) {
        super(generateSources, reflectionRegistration);
        this.applicationClassPredicate = applicationClassPredicate;
        this.mockable = mockable;
        this.existingClasses = existingClasses;
        this.singleContextNormalScopes = singleContextNormalScopes;
    }

    /**
     *
     * @param bean
     * @param beanClassName Fully qualified class name
     * @param bytecodeTransformerConsumer
     * @param transformUnproxyableClasses whether or not unproxyable classes should be transformed
     * @return a collection of resources
     */
    Collection<Resource> generate(BeanInfo bean, String beanClassName,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, boolean transformUnproxyableClasses) {

        // see `BeanGenerator` -- if this bean is unproxyable and that error is deferred to runtime,
        // we don't need to (and cannot, in fact) generate the client proxy class
        if (bean.getDeployment().hasRuntimeDeferredUnproxyableError(bean)) {
            return Collections.emptySet();
        }

        String baseName = getBeanBaseName(beanClassName);
        String targetPackage = bean.getClientProxyPackageName();
        String generatedName = generatedNameFromTarget(targetPackage, baseName, CLIENT_PROXY_SUFFIX);
        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(getApplicationClassTestName(bean))
                || bean.hasBoundDecoratorWhichIsApplicationClass(applicationClassPredicate);
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.CLIENT_PROXY : null, generateSources);

        Gizmo gizmo = gizmo(classOutput);

        createClientProxy(gizmo, bean, bytecodeTransformerConsumer, transformUnproxyableClasses,
                generatedName, targetPackage);

        return classOutput.getResources();
    }

    private void createClientProxy(Gizmo gizmo, BeanInfo bean, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses, String generatedName, String targetPackage) {

        Type providerType = bean.getProviderType();
        ClassInfo providerClass = getClassByName(bean.getDeployment().getBeanArchiveIndex(), providerType.name());
        ClassDesc providerClassDesc = classDescOf(providerClass);

        boolean isInterface = providerClass.isInterface();
        ClassDesc superClass = isInterface ? CD_Object : providerClassDesc;

        // Foo_ClientProxy extends Foo implements ClientProxy
        gizmo.class_(generatedName, cc -> {
            copyTypeParameters(providerClass, cc);

            GenericType.OfClass providerGenericType = genericTypeOfClass(providerType);
            if (!isInterface) {
                cc.extends_(providerGenericType);
            } else {
                cc.implements_(providerGenericType);
            }
            cc.implements_(ClientProxy.class);
            if (mockable) {
                cc.implements_(Mockable.class);
            }

            FieldDesc beanField = cc.field(BEAN_FIELD, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(InjectableBean.class);
            });

            FieldDesc mockField;
            if (mockable) {
                mockField = cc.field(MOCK_FIELD, fc -> {
                    fc.private_();
                    fc.volatile_();
                    fc.setType(providerClassDesc);
                });
            } else {
                mockField = null;
            }

            FieldDesc contextField;
            if (BuiltinScope.APPLICATION.is(bean.getScope())
                    || singleContextNormalScopes.contains(bean.getScope().getDotName())) {
                // It is safe to store the context instance on the proxy
                contextField = cc.field(CONTEXT_FIELD, fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(InjectableContext.class);
                });
            } else {
                contextField = null;
            }

            cc.constructor(mc -> {
                ParamVar id = mc.parameter("id", String.class);
                mc.body(bc -> {
                    bc.invokeSpecial(ConstructorDesc.of(superClass), cc.this_());

                    LocalVar arc = bc.localVar("arc", bc.invokeStatic(MethodDescs.ARC_REQUIRE_CONTAINER));
                    LocalVar beanVar = bc.localVar("bean", bc.invokeInterface(MethodDescs.ARC_CONTAINER_BEAN, arc, id));
                    bc.set(cc.this_().field(beanField), beanVar);

                    if (contextField != null) {
                        // At this point we can be sure there's only one context implementation available
                        Expr scope = bc.invokeInterface(MethodDescs.INJECTABLE_BEAN_GET_SCOPE, beanVar);
                        Expr contexts = bc.invokeInterface(MethodDescs.ARC_CONTAINER_GET_CONTEXTS, arc, scope);
                        bc.set(cc.this_().field(contextField), bc.withList(contexts).get(0));
                    }

                    bc.return_();
                });
            });

            MethodDesc delegateMethod = cc.method(DELEGATE_METHOD_NAME, mc -> {
                mc.private_();
                mc.returning(providerClassDesc);
                mc.body(b0 -> {
                    if (mockable) {
                        // if mockable and mocked just return the mock
                        b0.ifNotNull(cc.this_().field(mockField), b1 -> {
                            b1.return_(cc.this_().field(mockField));
                        });
                    }

                    Expr ret;
                    if (BuiltinScope.APPLICATION.is(bean.getScope())) {
                        // Application context is stored in a field and is always active
                        ret = b0.invokeStatic(MethodDescs.CLIENT_PROXIES_GET_APP_SCOPED_DELEGATE,
                                cc.this_().field(contextField), cc.this_().field(beanField));
                    } else if (singleContextNormalScopes.contains(bean.getScope().getDotName())) {
                        ret = b0.invokeStatic(MethodDescs.CLIENT_PROXIES_GET_SINGLE_CONTEXT_DELEGATE,
                                cc.this_().field(contextField), cc.this_().field(beanField));
                    } else {
                        ret = b0.invokeStatic(MethodDescs.CLIENT_PROXIES_GET_DELEGATE, cc.this_().field(beanField));
                    }
                    b0.return_(ret);
                });
            });

            cc.method(GET_CONTEXTUAL_INSTANCE_METHOD_NAME, mc -> {
                mc.public_();
                mc.returning(Object.class);
                mc.body(bc -> {
                    bc.return_(bc.invokeVirtual(delegateMethod, cc.this_()));
                });
            });

            cc.method(GET_BEAN, mc -> {
                mc.public_();
                mc.returning(InjectableBean.class);
                mc.body(bc -> {
                    bc.return_(cc.this_().field(beanField));
                });
            });

            if (mockable) {
                cc.method(CLEAR_MOCK_METHOD_NAME, mc -> {
                    mc.body(bc -> {
                        bc.set(cc.this_().field(mockField), Const.ofNull(mockField.type()));
                        bc.return_();
                    });
                });

                cc.method(SET_MOCK_METHOD_NAME, mc -> {
                    ParamVar mock = mc.parameter("mock", Object.class);
                    mc.body(bc -> {
                        bc.set(cc.this_().field(mockField), mock);
                        bc.return_();
                    });
                });
            }

            for (MethodInfo method : getDelegatingMethods(bean, bytecodeTransformerConsumer, transformUnproxyableClasses)) {
                MethodDesc originalMethodDesc = methodDescOf(method);
                cc.method(method.name(), mc -> {
                    copyTypeParameters(method, mc);

                    mc.returning(genericTypeOf(method.returnType()));
                    List<ParamVar> params = new ArrayList<>();
                    for (MethodParameterInfo param : method.parameters()) {
                        params.add(mc.parameter(param.nameOrDefault(), genericTypeOf(param.type())));
                    }
                    for (Type exception : method.exceptions()) {
                        mc.throws_(genericTypeOfThrows(exception));
                    }

                    mc.body(bc -> {
                        if (!superClass.equals(CD_Object)) {
                            // Skip delegation if proxy is not constructed yet
                            // This is not necessary for producers of interfaces, because interfaces cannot have constructors
                            // Similarly, this is not necessary for producers of `Object`, because its constructor does nothing
                            // if(!this.bean == null) return super.foo()
                            bc.ifNull(cc.this_().field(beanField), b1 -> {
                                if (method.isAbstract()) {
                                    b1.throw_(IllegalStateException.class, "Cannot invoke abstract method");
                                } else {
                                    MethodDesc superDesc = ClassMethodDesc.of(superClass, originalMethodDesc.name(),
                                            originalMethodDesc.type());
                                    b1.return_(b1.invokeSpecial(superDesc, cc.this_(), params));
                                }
                            });
                        }

                        Supplier<Expr> delegate = () -> bc.invokeVirtual(delegateMethod, cc.this_());

                        Expr ret;
                        // Note that we don't have to check for default interface methods if this is an interface,
                        // as it just works, and the reflection case cannot be true since it's not possible to have
                        // non-public default interface methods.
                        if (Methods.isObjectToString(method)) {
                            // Always use invokevirtual and the original descriptor for java.lang.Object#toString()
                            ret = bc.invokeVirtual(originalMethodDesc, delegate.get(), params);
                        } else if (isInterface) {
                            // make sure we invoke the method upon the provider type, i.e. don't use the original method descriptor
                            MethodDesc virtualMethod = InterfaceMethodDesc.of(providerClassDesc,
                                    originalMethodDesc.name(), originalMethodDesc.type());
                            ret = bc.invokeInterface(virtualMethod, delegate.get(), params);
                        } else if (isReflectionFallbackNeeded(method, targetPackage)) {
                            // Reflection fallback
                            reflectionRegistration.registerMethod(method);
                            Expr paramTypes = bc.newArray(Class.class, method.parameterTypes()
                                    .stream()
                                    .map(paramType -> Const.of(classDescOf(paramType)))
                                    .toList());
                            ret = bc.invokeStatic(MethodDescs.REFLECTIONS_INVOKE_METHOD,
                                    Const.of(classDescOf(method.declaringClass())), Const.of(method.name()),
                                    paramTypes, delegate.get(), bc.newArray(Object.class, params));
                            if (method.returnType().kind() == Type.Kind.VOID) {
                                ret = Const.ofVoid();
                            }
                        } else {
                            // make sure we do not use the original method descriptor as it could point to
                            // a default interface method containing class: make sure we invoke it on the provider type.
                            MethodDesc virtualMethod = ClassMethodDesc.of(providerClassDesc,
                                    originalMethodDesc.name(), originalMethodDesc.type());
                            ret = bc.invokeVirtual(virtualMethod, delegate.get(), params);
                        }
                        bc.return_(ret);
                    });
                });
            }
        });
    }

    Collection<MethodInfo> getDelegatingMethods(BeanInfo bean, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses) {
        Map<Methods.MethodKey, MethodInfo> methods = new HashMap<>();
        IndexView index = bean.getDeployment().getBeanArchiveIndex();

        if (bean.isClassBean()) {
            Map<String, Set<MethodKey>> methodsFromWhichToRemoveFinal = new HashMap<>();
            ClassInfo classInfo = bean.getTarget().get().asClass();
            addDelegatesAndTransformIfNecessary(bytecodeTransformerConsumer, transformUnproxyableClasses, methods, index,
                    methodsFromWhichToRemoveFinal, classInfo);
        } else if (bean.isProducerMethod()) {
            Map<String, Set<MethodKey>> methodsFromWhichToRemoveFinal = new HashMap<>();
            MethodInfo producerMethod = bean.getTarget().get().asMethod();
            ClassInfo returnTypeClass = getClassByName(index, producerMethod.returnType());
            addDelegatesAndTransformIfNecessary(bytecodeTransformerConsumer, transformUnproxyableClasses, methods, index,
                    methodsFromWhichToRemoveFinal, returnTypeClass);
        } else if (bean.isProducerField()) {
            Map<String, Set<MethodKey>> methodsFromWhichToRemoveFinal = new HashMap<>();
            FieldInfo producerField = bean.getTarget().get().asField();
            ClassInfo fieldClass = getClassByName(index, producerField.type());
            addDelegatesAndTransformIfNecessary(bytecodeTransformerConsumer, transformUnproxyableClasses, methods, index,
                    methodsFromWhichToRemoveFinal, fieldClass);
        } else if (bean.isSynthetic()) {
            Methods.addDelegatingMethods(index, bean.getImplClazz(), methods, null,
                    transformUnproxyableClasses);
        }

        return methods.values();
    }

    private void addDelegatesAndTransformIfNecessary(Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses,
            Map<Methods.MethodKey, MethodInfo> methods, IndexView index,
            Map<String, Set<MethodKey>> methodsFromWhichToRemoveFinal,
            ClassInfo fieldClass) {
        Methods.addDelegatingMethods(index, fieldClass, methods, methodsFromWhichToRemoveFinal,
                transformUnproxyableClasses);
        if (!methodsFromWhichToRemoveFinal.isEmpty()) {
            for (Map.Entry<String, Set<MethodKey>> entry : methodsFromWhichToRemoveFinal.entrySet()) {
                String className = entry.getKey();
                bytecodeTransformerConsumer.accept(new BytecodeTransformer(className,
                        new Methods.RemoveFinalFromMethod(entry.getValue())));
            }
        }
    }

    private DotName getApplicationClassTestName(BeanInfo bean) {
        DotName testedName;
        // For producers we need to test the produced type
        if (bean.isProducerField()) {
            testedName = bean.getTarget().get().asField().type().name();
        } else if (bean.isProducerMethod()) {
            testedName = bean.getTarget().get().asMethod().returnType().name();
        } else {
            testedName = bean.getBeanClass();
        }
        return testedName;
    }

}
