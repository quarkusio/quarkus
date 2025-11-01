package io.quarkus.arc.processor;

import static java.lang.constant.ConstantDescs.CD_Object;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstanceEquivalenceProxy;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.gizmo2.StringBuilderGen;

import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.InterceptionProxySubclass;
import io.quarkus.arc.impl.InterceptedMethodMetadata;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.SubclassGenerator.IntegerHolder;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

public class InterceptionProxyGenerator extends AbstractGenerator {
    private static final String INTERCEPTION_SUBCLASS = "_InterceptionSubclass";

    private final Predicate<DotName> applicationClassPredicate;
    private final IndexView beanArchiveIndex;
    private final AnnotationLiteralProcessor annotationLiterals;

    InterceptionProxyGenerator(boolean generateSources, Predicate<DotName> applicationClassPredicate,
            BeanDeployment deployment, AnnotationLiteralProcessor annotationLiterals,
            ReflectionRegistration reflectionRegistration) {
        super(generateSources, reflectionRegistration);
        this.applicationClassPredicate = applicationClassPredicate;
        this.beanArchiveIndex = deployment.getBeanArchiveIndex();
        this.annotationLiterals = annotationLiterals;
    }

    Collection<Resource> generate(BeanInfo bean, Consumer<BytecodeTransformer> bytecodeTransformerConsumer,
            boolean transformUnproxyableClasses) {
        if (bean.getInterceptionProxy() == null) {
            return Collections.emptyList();
        }

        Function<String, Resource.SpecialType> specialTypeFunction = className -> {
            if (className.endsWith(INTERCEPTION_SUBCLASS)) {
                return Resource.SpecialType.SUBCLASS;
            }
            return null;
        };
        ResourceClassOutput classOutput = new ResourceClassOutput(applicationClassPredicate.test(bean.getBeanClass()),
                specialTypeFunction, generateSources);

        Gizmo gizmo = gizmo(classOutput);

        createInterceptionProxyProvider(gizmo, bean);
        createInterceptionProxy(gizmo, bean);
        createInterceptionSubclass(gizmo, bean.getInterceptionProxy(), bytecodeTransformerConsumer,
                transformUnproxyableClasses);

        return classOutput.getResources();
    }

    // ---

    static String interceptionProxyProviderName(BeanInfo bean) {
        return bean.getBeanClass().toString() + "_InterceptionProxyProvider_" + bean.getIdentifier();
    }

    private static String interceptionProxyName(BeanInfo bean) {
        return bean.getBeanClass().toString() + "_InterceptionProxy_" + bean.getIdentifier();
    }

    private static String interceptionSubclassName(InterceptionProxyInfo interceptionProxy) {
        return interceptionProxy.getTargetClass() + INTERCEPTION_SUBCLASS;
    }

    private void createInterceptionProxyProvider(Gizmo gizmo, BeanInfo bean) {
        gizmo.class_(interceptionProxyProviderName(bean), cc -> {
            cc.implements_(Supplier.class);
            cc.implements_(InjectableReferenceProvider.class);

            cc.defaultConstructor();

            // Supplier
            cc.method("get", mc -> {
                mc.public_();
                mc.returning(Object.class);
                mc.body(bc -> bc.return_(cc.this_()));
            });

            // InjectableReferenceProvider
            cc.method("get", mc -> {
                mc.public_();
                mc.returning(Object.class);
                ParamVar creationalContext = mc.parameter("creationalContext", CreationalContext.class);
                mc.body(bc -> {
                    ConstructorDesc ctor = ConstructorDesc.of(ClassDesc.of(interceptionProxyName(bean)),
                            ClassDesc.of(CreationalContext.class.getName()));
                    bc.return_(bc.new_(ctor, creationalContext));
                });
            });
        });
    }

    private void createInterceptionProxy(Gizmo gizmo, BeanInfo bean) {
        gizmo.class_(interceptionProxyName(bean), cc -> {
            cc.implements_(InterceptionProxy.class);

            FieldDesc ccField = cc.field("creationalContext", fc -> {
                fc.private_();
                fc.final_();
                fc.setType(CreationalContext.class);
            });

            cc.constructor(mc -> {
                mc.public_();
                ParamVar ccParam = mc.parameter("creationalContext", CreationalContext.class);
                mc.body(bc -> {
                    bc.invokeSpecial(ConstructorDesc.of(Object.class), cc.this_());
                    bc.set(cc.this_().field(ccField), ccParam);
                    bc.return_();
                });
            });

            cc.method("create", mc -> {
                mc.public_();
                mc.returning(Object.class);
                ParamVar delegate = mc.parameter("delegate", Object.class);
                mc.body(b0 -> {
                    InterceptionProxyInfo interceptionProxy = bean.getInterceptionProxy();
                    b0.ifInstanceOf(delegate, classDescOf(interceptionProxy.getTargetClass()), (b1, ignored) -> {
                        ConstructorDesc ctor = ConstructorDesc.of(ClassDesc.of(interceptionSubclassName(interceptionProxy)),
                                CreationalContext.class, Object.class);
                        b1.return_(b1.new_(ctor, cc.this_().field(ccField), delegate));
                    });

                    Expr err = StringBuilderGen.ofNew(b0)
                            .append("InterceptionProxy for ")
                            .append(bean.toString())
                            .append(" got unknown delegate: ")
                            .append(delegate)
                            .toString_();
                    b0.throw_(IllegalArgumentException.class, err);
                });
            });
        });
    }

    private void createInterceptionSubclass(Gizmo gizmo, InterceptionProxyInfo interceptionProxy,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, boolean transformUnproxyableClasses) {
        BeanInfo pseudoBean = interceptionProxy.getPseudoBean();
        ClassDesc pseudoBeanClass = classDescOf(pseudoBean.getImplClazz());
        boolean implementingInterface = pseudoBean.getImplClazz().isInterface();

        CodeGenInfo info = preprocess(pseudoBean);

        ClassDesc superClass = implementingInterface ? CD_Object : pseudoBeanClass;

        gizmo.class_(interceptionSubclassName(interceptionProxy), cc -> {
            cc.extends_(superClass);
            if (implementingInterface) {
                cc.implements_(pseudoBeanClass);
            }
            cc.implements_(InterceptionProxySubclass.class);

            FieldDesc delegateField = cc.field("delegate", fc -> {
                fc.private_();
                fc.final_();
                fc.setType(pseudoBeanClass);
            });

            FieldDesc constructedField = cc.field(SubclassGenerator.FIELD_NAME_CONSTRUCTED, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(boolean.class);
            });

            Map<List<InterceptorInfo>, String> interceptorChainKeys = new HashMap<>();
            Map<Set<AnnotationInstanceEquivalenceProxy>, String> bindingKeys = new HashMap<>();
            Map<MethodDesc, MethodDesc> forwardingMethods = new HashMap<>();

            for (InterceptedMethod interceptedMethod : info.interceptedMethods()) {
                MethodInfo method = interceptedMethod.method();
                MethodDesc forwardDesc = SubclassGenerator.createForwardingMethod(cc, pseudoBeanClass, method,
                        implementingInterface);
                forwardingMethods.put(methodDescOf(method), forwardDesc);
            }

            cc.constructor(mc -> {
                mc.public_();
                ParamVar ccParam = mc.parameter("creationalContext", CreationalContext.class);
                ParamVar delegateParam = mc.parameter("delegate", Object.class);
                mc.body(bc -> {
                    bc.invokeSpecial(ConstructorDesc.of(superClass), cc.this_());

                    bc.set(cc.this_().field(delegateField), bc.cast(delegateParam, pseudoBeanClass));

                    LocalVar arc = bc.localVar("arc", bc.invokeStatic(MethodDescs.ARC_REQUIRE_CONTAINER));

                    Map<String, LocalVar> interceptorBeanToLocalVar = new HashMap<>();
                    Map<String, LocalVar> interceptorInstanceToLocalVar = new HashMap<>();
                    for (int i = 0; i < info.boundInterceptors().size(); i++) {
                        InterceptorInfo interceptorInfo = info.boundInterceptors().get(i);
                        String id = interceptorInfo.getIdentifier();

                        LocalVar interceptorBean = bc.localVar("interceptorBean_" + i, bc.invokeInterface(
                                MethodDescs.ARC_CONTAINER_BEAN, arc, Const.of(id)));
                        interceptorBeanToLocalVar.put(id, interceptorBean);

                        Expr ccChild = bc.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD, ccParam);
                        LocalVar interceptorInstance = bc.localVar("interceptorInstance_" + i, bc.invokeInterface(
                                MethodDescs.INJECTABLE_REF_PROVIDER_GET, interceptorBean, ccChild));
                        interceptorInstanceToLocalVar.put(id, interceptorInstance);
                    }

                    LocalVar interceptorChainMap = bc.localVar("interceptorChainMap", bc.new_(HashMap.class));
                    LocalVar bindingsMap = bc.localVar("bindingsMap", bc.new_(HashMap.class));

                    // Shared interceptor bindings literals
                    IntegerHolder chainIdx = new IntegerHolder();
                    IntegerHolder bindingIdx = new IntegerHolder();
                    Map<AnnotationInstanceEquivalenceProxy, Expr> bindingsLiterals = new HashMap<>();
                    var bindingsFun = SubclassGenerator.createBindingsFun(bindingIdx, bc, bindingsMap, bindingsLiterals,
                            pseudoBean, annotationLiterals);
                    var interceptorChainKeysFun = SubclassGenerator.createInterceptorChainKeysFun(chainIdx, bc,
                            interceptorChainMap, interceptorInstanceToLocalVar, interceptorBeanToLocalVar);

                    for (InterceptedMethod interceptedMethod : info.interceptedMethods()) {
                        BeanInfo.InterceptionInfo interception = interceptedMethod.interception();
                        // Each intercepted method has a corresponding InterceptedMethodMetadata field
                        cc.field("arc$" + interceptedMethod.index, fc -> {
                            fc.private_();
                            fc.setType(InterceptedMethodMetadata.class);
                        });
                        interceptorChainKeys.computeIfAbsent(interception.interceptors, interceptorChainKeysFun);
                        bindingKeys.computeIfAbsent(interception.bindingsEquivalenceProxies(), bindingsFun);
                    }

                    // Split initialization of InterceptedMethodMetadata into multiple methods
                    for (MethodGroup group : info.methodGroups()) {
                        MethodDesc desc = ClassMethodDesc.of(cc.type(), "arc$initMetadata" + group.id(),
                                void.class, Map.class, Map.class);

                        bc.invokeVirtual(desc, cc.this_(), interceptorChainMap, bindingsMap);
                    }

                    bc.set(cc.this_().field(constructedField), Const.of(true));
                    bc.return_();
                });
            });

            for (MethodGroup group : info.methodGroups()) {
                createInitMetadataMethod(cc, pseudoBeanClass, implementingInterface, constructedField, delegateField, group,
                        forwardingMethods, interceptorChainKeys, bindingKeys);
            }

            cc.method("arc_delegate", mc -> {
                mc.public_();
                mc.returning(Object.class);
                mc.body(bc -> bc.return_(cc.this_().field(delegateField)));
            });

            // forward non-intercepted methods to the delegate unconditionally
            Collection<MethodInfo> methodsToForward = collectMethodsToForward(pseudoBean,
                    bytecodeTransformerConsumer, transformUnproxyableClasses);
            for (MethodInfo method : methodsToForward) {
                MethodDesc methodDesc = methodDescOf(method);
                cc.method(methodDesc, mc -> {
                    mc.public_();
                    List<ParamVar> params = new ArrayList<>(method.parametersCount());
                    for (MethodParameterInfo param : method.parameters()) {
                        params.add(mc.parameter(param.nameOrDefault()));
                    }
                    mc.body(b0 -> {
                        if (!superClass.equals(CD_Object)) {
                            // Skip delegation if proxy is not constructed yet
                            // This is not necessary when intercepting interfaces, because interfaces cannot have constructors
                            // Similarly, this is not necessary when intercepting `Object`, because its constructor does nothing
                            b0.ifNot(cc.this_().field(constructedField), b1 -> {
                                if (method.isAbstract()) {
                                    b1.throw_(IllegalStateException.class, "Cannot invoke abstract method");
                                } else {
                                    MethodDesc superMethod = ClassMethodDesc.of(pseudoBeanClass, methodDesc.name(),
                                            methodDesc.type());
                                    b1.return_(b1.invokeSpecial(superMethod, cc.this_(), params));
                                }
                            });

                        }

                        b0.return_(method.declaringClass().isInterface()
                                ? b0.invokeInterface(methodDesc, cc.this_().field(delegateField), params)
                                : b0.invokeVirtual(methodDesc, cc.this_().field(delegateField), params));
                    });
                });
            }
        });
    }

    private void createInitMetadataMethod(ClassCreator cc, ClassDesc pseudoBeanClass, boolean isInterface,
            FieldDesc constructedField, FieldDesc delegateField, MethodGroup group,
            Map<MethodDesc, MethodDesc> forwardingMethods, Map<List<InterceptorInfo>, String> interceptorChainKeys,
            Map<Set<AnnotationInstanceEquivalenceProxy>, String> bindingKeys) {

        cc.method("arc$initMetadata" + group.id(), mc -> {
            mc.private_();
            mc.returning(void.class);
            ParamVar interceptorChainMapParam = mc.parameter("interceptorChainMap", Map.class);
            ParamVar bindingsMapParam = mc.parameter("bindingsMap", Map.class);
            mc.body(bc -> {
                // to avoid repeatedly looking for the exact same thing in the maps
                Map<String, LocalVar> chains = new HashMap<>();
                Map<String, LocalVar> bindings = new HashMap<>();

                for (InterceptedMethod interceptedMethod : group.interceptedMethods()) {
                    MethodInfo method = interceptedMethod.method();
                    MethodDesc methodDesc = methodDescOf(method);
                    BeanInfo.InterceptionInfo interception = interceptedMethod.interception();
                    List<Type> parameters = method.parameterTypes();

                    // 1. Interceptor chain
                    String interceptorChainKey = interceptorChainKeys.get(interception.interceptors);
                    LocalVar chainArg = chains.computeIfAbsent(interceptorChainKey, ignored -> {
                        return bc.localVar("interceptorChain", bc.withMap(interceptorChainMapParam)
                                .get(Const.of(interceptorChainKey)));
                    });

                    // 2. Method method = Reflections.findMethod(org.jboss.weld.arc.test.interceptors.SimpleBean.class,"foo",java.lang.String.class)
                    Expr[] args = new Expr[3];
                    args[0] = Const.of(pseudoBeanClass);
                    args[1] = Const.of(method.name());
                    if (!parameters.isEmpty()) {
                        LocalVar paramTypes = bc.localVar("paramTypes",
                                bc.newEmptyArray(Class.class, parameters.size()));
                        for (int i = 0; i < parameters.size(); i++) {
                            bc.set(paramTypes.elem(i), Const.of(classDescOf(parameters.get(i))));
                        }
                        args[2] = paramTypes;
                    } else {
                        args[2] = bc.getStaticField(FieldDescs.ANNOTATION_LITERALS_EMPTY_CLASS_ARRAY);
                    }
                    Expr methodArg = bc.invokeStatic(MethodDescs.REFLECTIONS_FIND_METHOD, args);

                    // 3. Interceptor bindings
                    String bindingKey = bindingKeys.get(interception.bindingsEquivalenceProxies());
                    LocalVar bindingsArg = bindings.computeIfAbsent(bindingKey, ignored -> {
                        return bc.localVar("bindings", bc.withMap(bindingsMapParam)
                                .get(Const.of(bindingKey)));
                    });

                    // Instantiate the forwarding function
                    // BiFunction<Object, InvocationContext, Object> forward = (target, ctx) -> target.foo$$superforward((java.lang.String)ctx.getParameters()[0])
                    Expr forwardFunArg = bc.lambda(BiFunction.class, lc -> {
                        ParamVar target = lc.parameter("target", 0);
                        ParamVar ctx = lc.parameter("ctx", 1);
                        lc.body(lbc -> {
                            Expr[] superArgs;
                            if (parameters.isEmpty()) {
                                superArgs = new Expr[0];
                            } else {
                                Expr ctxArgs = lbc.localVar("args", lbc.invokeInterface(
                                        MethodDesc.of(InvocationContext.class, "getParameters", Object[].class), ctx));
                                superArgs = new Expr[parameters.size()];
                                for (int i = 0; i < parameters.size(); i++) {
                                    superArgs[i] = ctxArgs.elem(i);
                                }
                            }
                            Expr superResult = method.declaringClass().isInterface()
                                    ? lbc.invokeInterface(methodDesc, target, superArgs)
                                    : lbc.invokeVirtual(methodDesc, target, superArgs);
                            lbc.return_(superResult);
                        });
                    });

                    // Now create metadata for the given intercepted method
                    Expr methodMetadata = bc.new_(MethodDescs.INTERCEPTED_METHOD_METADATA_CONSTRUCTOR,
                            chainArg, methodArg, bindingsArg, forwardFunArg);

                    FieldDesc metadataField = FieldDesc.of(cc.type(), "arc$" + interceptedMethod.index,
                            InterceptedMethodMetadata.class);

                    bc.set(cc.this_().field(metadataField), methodMetadata);

                    // Needed when running on native image
                    reflectionRegistration.registerMethod(method);

                    // Finally create the intercepted method
                    MethodDesc forwardDescriptor = forwardingMethods.get(methodDesc);
                    SubclassGenerator.createInterceptedMethod(method, cc, metadataField, constructedField,
                            forwardDescriptor, () -> cc.this_().field(delegateField));
                }

                bc.return_();
            });
        });
    }

    // uses the same algorithm as `ClientProxyGenerator`
    private Collection<MethodInfo> collectMethodsToForward(BeanInfo pseudoBean,
            Consumer<BytecodeTransformer> bytecodeTransformerConsumer, boolean transformUnproxyableClasses) {
        ClassInfo pseudoBeanClass = pseudoBean.getImplClazz();

        Map<Methods.MethodKey, MethodInfo> methods = new HashMap<>();
        Map<String, Set<Methods.MethodKey>> methodsFromWhichToRemoveFinal = new HashMap<>();

        Methods.addDelegatingMethods(beanArchiveIndex, pseudoBeanClass, methods, methodsFromWhichToRemoveFinal,
                transformUnproxyableClasses);

        if (!methodsFromWhichToRemoveFinal.isEmpty()) {
            for (Map.Entry<String, Set<Methods.MethodKey>> entry : methodsFromWhichToRemoveFinal.entrySet()) {
                String className = entry.getKey();
                bytecodeTransformerConsumer.accept(new BytecodeTransformer(className,
                        new Methods.RemoveFinalFromMethod(entry.getValue())));
            }
        }

        for (MethodInfo interceptedMethod : pseudoBean.getInterceptedMethods().keySet()) {
            // these methods are intercepted, so they don't need to (and in fact _must not_) forward directly
            methods.remove(new Methods.MethodKey(interceptedMethod));
        }

        return methods.values();
    }

    record InterceptedMethod(int index, MethodInfo method, BeanInfo.InterceptionInfo interception) {
    }

    record MethodGroup(int id, List<InterceptedMethod> interceptedMethods) {
    }

    record CodeGenInfo(
            List<InterceptorInfo> boundInterceptors,
            List<InterceptedMethod> interceptedMethods,
            List<MethodGroup> methodGroups) {
    }

    private CodeGenInfo preprocess(BeanInfo pseudoBean) {
        List<InterceptorInfo> boundInterceptors = pseudoBean.getBoundInterceptors();

        IntegerHolder methodIdx = new IntegerHolder();
        List<InterceptedMethod> interceptedMethods = new ArrayList<>();
        pseudoBean.getInterceptedMethods().forEach((method, interception) -> {
            interceptedMethods.add(new InterceptedMethod(methodIdx.i, method, interception));
            methodIdx.i++;
        });

        List<MethodGroup> methodGroups = Grouping.of(interceptedMethods, 30, MethodGroup::new);

        return new CodeGenInfo(List.copyOf(boundInterceptors), List.copyOf(interceptedMethods), List.copyOf(methodGroups));
    }
}
