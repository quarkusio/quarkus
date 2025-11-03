package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.KotlinUtils.isKotlinMethod;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstanceEquivalenceProxy;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.ArcUndeclaredThrowableException;
import io.quarkus.arc.InjectableDecorator;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.Subclass;
import io.quarkus.arc.impl.InterceptedMethodMetadata;
import io.quarkus.arc.processor.BeanInfo.DecorationInfo;
import io.quarkus.arc.processor.BeanInfo.DecoratorMethod;
import io.quarkus.arc.processor.BeanInfo.InterceptionInfo;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.Methods.MethodKey;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.FieldVar;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.InstanceFieldVar;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.InterfaceMethodDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 * A subclass is generated for any intercepted/decorated bean.
 */
public class SubclassGenerator extends AbstractGenerator {

    private static final DotName JAVA_LANG_THROWABLE = DotNames.create(Throwable.class);
    private static final DotName JAVA_LANG_EXCEPTION = DotNames.create(Exception.class);
    private static final DotName JAVA_LANG_RUNTIME_EXCEPTION = DotNames.create(RuntimeException.class);

    static final String SUBCLASS_SUFFIX = "_Subclass";
    static final String MARK_CONSTRUCTED_METHOD_NAME = "arc$markConstructed";
    static final String DESTROY_METHOD_NAME = "arc$destroy";

    protected static final String FIELD_NAME_PREDESTROYS = "arc$preDestroys";
    protected static final String FIELD_NAME_CONSTRUCTED = "arc$constructed";

    private final Predicate<DotName> applicationClassPredicate;
    private final Set<String> existingClasses;
    private final PrivateMembersCollector privateMembers;
    private final AnnotationLiteralProcessor annotationLiterals;

    static String generatedName(DotName providerTypeName, String baseName) {
        return generatedNameFromTarget(DotNames.packagePrefix(providerTypeName), baseName, SUBCLASS_SUFFIX);
    }

    SubclassGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            boolean generateSources, ReflectionRegistration reflectionRegistration,
            Set<String> existingClasses, PrivateMembersCollector privateMembers) {
        super(generateSources, reflectionRegistration);
        this.applicationClassPredicate = applicationClassPredicate;
        this.annotationLiterals = annotationLiterals;
        this.existingClasses = existingClasses;
        this.privateMembers = privateMembers;
    }

    Collection<Resource> generate(BeanInfo bean, String beanClassName) {
        Type providerType = bean.getProviderType();
        String baseName = getBeanBaseName(beanClassName);
        String generatedName = generatedName(providerType.name(), baseName);
        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(bean.getBeanClass())
                || bean.hasBoundDecoratorWhichIsApplicationClass(applicationClassPredicate);
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.SUBCLASS : null,
                generateSources);

        Gizmo gizmo = gizmo(classOutput);

        createSubclass(gizmo, bean, generatedName, providerType);

        return classOutput.getResources();
    }

    private void createSubclass(Gizmo gizmo, BeanInfo bean, String generatedName, Type providerType) {
        CodeGenInfo codeGenInfo = preprocess(bean);
        InterceptionInfo preDestroyInterception = bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY);

        // Foo_Subclass extends Foo implements Subclass
        gizmo.class_(generatedName, cc -> {
            cc.extends_(classDescOf(providerType));
            cc.implements_(Subclass.class);

            for (InterceptedDecoratedMethod interceptedDecoratedMethod : codeGenInfo.interceptedDecoratedMethods) {
                if (interceptedDecoratedMethod.interception() != null) {
                    // Each intercepted method has a corresponding InterceptedMethodMetadata field
                    cc.field("arc$" + interceptedDecoratedMethod.index, fc -> {
                        fc.private_();
                        fc.setType(InterceptedMethodMetadata.class);
                    });
                }
            }

            FieldDesc aroundInvokesField;
            if (bean.hasAroundInvokes()) {
                aroundInvokesField = cc.field("aroundInvokes", fc -> {
                    fc.private_();
                    fc.setType(List.class);
                });
            } else {
                aroundInvokesField = null;
            }

            FieldDesc preDestroys;
            if (!preDestroyInterception.isEmpty()) {
                // private final List<InvocationContextImpl.InterceptorInvocation> preDestroys
                preDestroys = cc.field(FIELD_NAME_PREDESTROYS, fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(ArrayList.class);
                });
            } else {
                preDestroys = null;
            }

            // `volatile` is perhaps not best, this field is monotonic (once `true`, it never becomes `false` again),
            // so maybe making the `markConstructed` method `synchronized` would be enough (?)
            FieldDesc constructedField = cc.field(FIELD_NAME_CONSTRUCTED, fc -> {
                fc.private_();
                fc.volatile_();
                fc.setType(boolean.class);
            });

            // Initialize maps of shared interceptor chains and interceptor bindings
            Map<List<InterceptorInfo>, String> interceptorChainKeys = new HashMap<>();
            Map<Set<AnnotationInstanceEquivalenceProxy>, String> bindingKeys = new HashMap<>();
            Map<MethodDesc, MethodDesc> forwardingMethods = new HashMap<>();

            for (InterceptedDecoratedMethod interceptedDecoratedMethod : codeGenInfo.interceptedDecoratedMethods()) {
                MethodInfo method = interceptedDecoratedMethod.method();
                MethodDesc forwardDesc = createForwardingMethod(cc, classDescOf(providerType), method, false);
                forwardingMethods.put(methodDescOf(method), forwardDesc);
            }

            cc.constructor(mc -> {
                Optional<Injection> constructorInjection = bean.getConstructorInjection();
                List<ClassDesc> ipTypes = new ArrayList<>();
                List<ParamVar> ipParams = new ArrayList<>();
                if (constructorInjection.isPresent()) {
                    int idx = 0;
                    for (InjectionPointInfo injectionPoint : constructorInjection.get().injectionPoints) {
                        ClassDesc ipType = classDescOf(injectionPoint.getType());
                        ipTypes.add(ipType);
                        ipParams.add(mc.parameter("ip" + idx, ipType));
                        idx++;
                    }
                }
                ParamVar ccParam = mc.parameter("creationalContext", CreationalContext.class);
                List<ParamVar> interceptorParams = new ArrayList<>();
                for (int i = 0; i < codeGenInfo.boundInterceptors().size(); i++) {
                    interceptorParams.add(mc.parameter("interceptor" + i, InjectableInterceptor.class));
                }
                List<ParamVar> decoratorParams = new ArrayList<>();
                for (int i = 0; i < codeGenInfo.boundDecorators().size(); i++) {
                    decoratorParams.add(mc.parameter("decorator" + i, InjectableDecorator.class));
                }

                mc.body(bc -> {
                    // super(fooProvider)
                    bc.invokeSpecial(ConstructorDesc.of(classDescOf(providerType), ipTypes), cc.this_(), ipParams);

                    // First instantiate all interceptor instances, so that they can be shared
                    Map<String, ParamVar> interceptorBeanToParamVar = new HashMap<>();
                    Map<String, LocalVar> interceptorInstanceToLocalVar = new HashMap<>();
                    for (int i = 0; i < codeGenInfo.boundInterceptors().size(); i++) {
                        InterceptorInfo interceptorInfo = codeGenInfo.boundInterceptors().get(i);
                        String id = interceptorInfo.getIdentifier();

                        ParamVar interceptorBean = interceptorParams.get(i);
                        interceptorBeanToParamVar.put(id, interceptorBean);

                        // create instance of each interceptor -> InjectableInterceptor.get()
                        Expr ccChild = bc.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD, ccParam);
                        LocalVar interceptorInstance = bc.localVar("interceptorInstance_" + id, bc.invokeInterface(
                                MethodDescs.INJECTABLE_REF_PROVIDER_GET, interceptorBean, ccChild));
                        interceptorInstanceToLocalVar.put(id, interceptorInstance);
                    }

                    // If a decorator is associated:
                    // 1. Generate the delegate subclass
                    // 2. Instantiate the decorator instance
                    // 3. Create and set the corresponding field
                    if (!codeGenInfo.boundDecorators().isEmpty()) {
                        Map<String, LocalVar> decoratorToLocalVar = new HashMap<>();
                        for (int j = 0; j < codeGenInfo.boundDecorators().size(); j++) {
                            processDecorator(gizmo, cc, codeGenInfo.boundDecorators().get(j), bean, providerType, bc,
                                    decoratorParams.get(j), decoratorToLocalVar, ccParam, forwardingMethods);
                        }
                    }

                    // PreDestroy interceptors
                    if (preDestroys != null) {
                        LocalVar list = bc.localVar("preDestroysList", bc.new_(ArrayList.class));
                        for (InterceptorInfo interceptor : preDestroyInterception.interceptors) {
                            LocalVar interceptorInstance = interceptorInstanceToLocalVar.get(interceptor.getIdentifier());
                            bc.withList(list).add(bc.invokeStatic(MethodDescs.INTERCEPTOR_INVOCATION_PRE_DESTROY,
                                    interceptorBeanToParamVar.get(interceptor.getIdentifier()), interceptorInstance));
                        }
                        bc.set(cc.this_().field(preDestroys), list);
                    }

                    LocalVar interceptorChainMap = bc.localVar("interceptorChainMap", bc.new_(HashMap.class));
                    LocalVar bindingsMap = bc.localVar("bindingsMap", bc.new_(HashMap.class));

                    // Shared interceptor bindings literals
                    IntegerHolder chainIdx = new IntegerHolder();
                    IntegerHolder bindingIdx = new IntegerHolder();
                    Map<AnnotationInstanceEquivalenceProxy, Expr> bindingsLiterals = new HashMap<>();
                    var bindingsFun = SubclassGenerator.createBindingsFun(bindingIdx, bc, bindingsMap, bindingsLiterals,
                            bean, annotationLiterals);
                    var interceptorChainKeysFun = SubclassGenerator.createInterceptorChainKeysFun(chainIdx, bc,
                            interceptorChainMap, interceptorInstanceToLocalVar, interceptorBeanToParamVar);

                    for (InterceptedDecoratedMethod interceptedDecoratedMethod : codeGenInfo.interceptedDecoratedMethods) {
                        InterceptionInfo interception = interceptedDecoratedMethod.interception();
                        if (interception != null) {
                            interceptorChainKeys.computeIfAbsent(interception.interceptors, interceptorChainKeysFun);
                            bindingKeys.computeIfAbsent(interception.bindingsEquivalenceProxies(), bindingsFun);
                        }
                    }

                    // Initialize the "aroundInvokes" field if necessary
                    if (bean.hasAroundInvokes()) {
                        LocalVar aroundInvokes = bc.localVar("aroundInvokes", bc.new_(ArrayList.class));
                        for (MethodInfo method : bean.getAroundInvokes()) {
                            // BiFunction<Object,InvocationContext,Object>
                            Expr lambda = bc.lambda(BiFunction.class, lc -> {
                                ParamVar target = lc.parameter("target", 0);
                                ParamVar ctx = lc.parameter("ctx", 1);
                                lc.body(lbc -> {
                                    boolean isApplicationClass = applicationClassPredicate.test(bean.getBeanClass());
                                    // Check if interceptor method uses InvocationContext or ArcInvocationContext
                                    Class<?> invocationContextClass;
                                    if (method.parameterType(0).name().equals(DotNames.INVOCATION_CONTEXT)) {
                                        invocationContextClass = InvocationContext.class;
                                    } else {
                                        invocationContextClass = ArcInvocationContext.class;
                                    }
                                    if (Modifier.isPrivate(method.flags())) {
                                        // Use reflection fallback
                                        privateMembers.add(isApplicationClass, String.format("Interceptor method %s#%s()",
                                                method.declaringClass().name(), method.name()));
                                        reflectionRegistration.registerMethod(method);
                                        Expr paramTypes = lbc.newArray(Class.class, Const.of(invocationContextClass));
                                        Expr argValues = lbc.newArray(Object.class, ctx);
                                        lbc.return_(lbc.invokeStatic(MethodDescs.REFLECTIONS_INVOKE_METHOD,
                                                Const.of(classDescOf(method.declaringClass())), Const.of(method.name()),
                                                paramTypes, target, argValues));
                                    } else {
                                        lbc.return_(lbc.invokeVirtual(methodDescOf(method),
                                                lbc.cast(target, classDescOf(method.declaringClass())), ctx));
                                    }
                                });
                            });
                            bc.withList(aroundInvokes).add(lambda);
                        }
                        bc.set(cc.this_().field(aroundInvokesField), aroundInvokes);
                    }

                    // Split initialization of InterceptedMethodMetadata into multiple methods
                    for (MethodGroup group : codeGenInfo.methodGroups()) {
                        MethodDesc desc = ClassMethodDesc.of(cc.type(), "arc$initMetadata" + group.id(),
                                void.class, Map.class, Map.class);

                        bc.invokeVirtual(desc, cc.this_(), interceptorChainMap, bindingsMap);
                    }

                    bc.return_();
                });
            });

            for (MethodGroup group : codeGenInfo.methodGroups()) {
                generateInitMetadata(cc, bean, providerType, aroundInvokesField, constructedField, group,
                        forwardingMethods, interceptorChainKeys, bindingKeys);
            }

            cc.method(MARK_CONSTRUCTED_METHOD_NAME, mc -> {
                mc.body(bc -> {
                    bc.set(cc.this_().field(constructedField), Const.of(true));
                    bc.return_();
                });
            });

            if (preDestroys != null) {
                cc.method(DESTROY_METHOD_NAME, mc -> {
                    ParamVar forward = mc.parameter("forward", Runnable.class);
                    mc.body(b0 -> {
                        b0.try_(tc -> {
                            tc.body(b1 -> {
                                Expr bindings = b1.setOf(preDestroyInterception.bindings
                                        .stream()
                                        .map(binding -> {
                                            ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.name());
                                            return annotationLiterals.create(b1, bindingClass, binding);
                                        })
                                        .toList());

                                Expr invocationContext = b1.invokeStatic(MethodDescs.INVOCATION_CONTEXTS_PRE_DESTROY,
                                        cc.this_(), cc.this_().field(preDestroys), bindings, forward);
                                b1.invokeInterface(MethodDescs.INVOCATION_CONTEXT_PROCEED, invocationContext);
                                b1.return_();
                            });
                            tc.catch_(Exception.class, "e", (b1, e) -> {
                                b1.throw_(b1.new_(RuntimeException.class, Const.of("Error destroying subclass"), e));
                            });
                        });
                    });
                });
            }
        });
    }

    private void generateInitMetadata(ClassCreator cc, BeanInfo bean, Type providerType,
            FieldDesc aroundInvokesField, FieldDesc constructedField, MethodGroup group,
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

                for (InterceptedDecoratedMethod interceptedDecoratedMethod : group.interceptedDecoratedMethods()) {
                    MethodInfo method = interceptedDecoratedMethod.method();
                    MethodDesc methodDesc = methodDescOf(method);
                    InterceptionInfo interception = interceptedDecoratedMethod.interception();
                    DecorationInfo decoration = interceptedDecoratedMethod.decoration();
                    MethodDesc forwardDesc = forwardingMethods.get(methodDesc);
                    List<Type> parameters = method.parameterTypes();

                    if (interception != null) {
                        // 1. Interceptor chain
                        String interceptorChainKey = interceptorChainKeys.get(interception.interceptors);
                        LocalVar chainArg = chains.computeIfAbsent(interceptorChainKey, ignored -> {
                            return bc.localVar("interceptorChain", bc.withMap(interceptorChainMapParam)
                                    .get(Const.of(interceptorChainKey)));
                        });

                        // 2. Method method = Reflections.findMethod(org.jboss.weld.arc.test.interceptors.SimpleBean.class,"foo",java.lang.String.class)
                        Expr[] args = new Expr[3];
                        args[0] = Const.of(classDescOf(providerType));
                        args[1] = Const.of(method.name());
                        if (!parameters.isEmpty()) {
                            LocalVar paramTypes = bc.localVar("paramTypes",
                                    bc.newEmptyArray(Class.class, parameters.size()));
                            for (int i = 0; i < parameters.size(); i++) {
                                bc.set(paramTypes.elem(i), Const.of(classDescOf(parameters.get(i))));
                            }
                            args[2] = paramTypes;
                        } else {
                            args[2] = Expr.staticField(FieldDescs.ANNOTATION_LITERALS_EMPTY_CLASS_ARRAY);
                        }
                        Expr methodArg = bc.invokeStatic(MethodDescs.REFLECTIONS_FIND_METHOD, args);

                        // 3. Interceptor bindings
                        String bindingKey = bindingKeys.get(interception.bindingsEquivalenceProxies());
                        LocalVar bindingsArg = bindings.computeIfAbsent(bindingKey, ignored -> {
                            return bc.localVar("bindings", bc.withMap(bindingsMapParam)
                                    .get(Const.of(bindingKey)));
                        });

                        DecoratorMethod decoratorMethod = decoration != null ? decoration.firstDecoratorMethod() : null;
                        FieldVar decorator;
                        if (decoratorMethod != null) {
                            decorator = cc.this_().field(FieldDesc.of(cc.type(),
                                    decoratorMethod.decorator.getIdentifier(), Object.class));
                        } else {
                            decorator = null;
                        }

                        // Instantiate the forwarding function
                        // BiFunction<Object, InvocationContext, Object> forward = (target, ctx) -> target.foo$$superforward((java.lang.String)ctx.getParameters()[0])
                        LocalVar forwardFun = bc.localVar("forwardFun", bc.lambda(BiFunction.class, lc -> {
                            Var capturedDecorator = decorator != null ? lc.capture(decorator) : null;
                            ParamVar target = lc.parameter("target", 0);
                            ParamVar ctx = lc.parameter("ctx", 1);
                            lc.body(lbc -> {
                                MethodDesc desc;
                                Expr instance;
                                if (decoratorMethod == null) {
                                    desc = forwardDesc;
                                    instance = target;
                                } else {
                                    // If a decorator is bound then invoke the method upon the decorator instance instead of the generated forwarding method
                                    // We need to use the decorator method in order to support not visible or generic decorators
                                    desc = methodDescOf(decoratorMethod.method);
                                    instance = capturedDecorator;
                                }

                                Expr[] superArgs;
                                if (parameters.isEmpty()) {
                                    superArgs = new Expr[0];
                                } else {
                                    Expr ctxArgs = lbc.localVar("args", lbc.invokeInterface(
                                            MethodDescs.INVOCATION_CONTEXT_GET_PARAMETERS, ctx));
                                    superArgs = new Expr[parameters.size()];
                                    for (int i = 0; i < parameters.size(); i++) {
                                        superArgs[i] = ctxArgs.elem(i);
                                    }
                                }

                                Expr superResult = decoratorMethod == null
                                        ? lbc.invokeVirtual(desc, instance, superArgs)
                                        : lbc.invokeInterface(desc, instance, superArgs);
                                lbc.return_(superResult.isVoid() ? Const.ofNull(Object.class) : superResult);
                            });
                        }));

                        if (bean.hasAroundInvokes()) {
                            LocalVar finalForwardFun = forwardFun;
                            // Wrap the forwarding function with a function that calls around invoke methods declared in a hierarchy of the target class first
                            forwardFun = bc.localVar("forwardFun2", bc.lambda(BiFunction.class, lc -> {
                                Var capturedAroundInvokes = lc.capture(cc.this_().field(aroundInvokesField));
                                Var capturedForwardFun = lc.capture(finalForwardFun);
                                ParamVar target = lc.parameter("target", 0); // unused
                                ParamVar ctx = lc.parameter("ctx", 1);
                                lc.body(lbc -> {
                                    lbc.return_(lbc.invokeStatic(MethodDescs.INVOCATION_CONTEXTS_PERFORM_TARGET_AROUND_INVOKE,
                                            ctx, capturedAroundInvokes, capturedForwardFun));
                                });
                            }));
                        }

                        // Now create metadata for the given intercepted method
                        Expr methodMetadata = bc.new_(MethodDescs.INTERCEPTED_METHOD_METADATA_CONSTRUCTOR,
                                chainArg, methodArg, bindingsArg, forwardFun);

                        FieldDesc metadataField = FieldDesc.of(cc.type(), "arc$" + interceptedDecoratedMethod.index,
                                InterceptedMethodMetadata.class);

                        bc.set(cc.this_().field(metadataField), methodMetadata);

                        // Needed when running on native image
                        reflectionRegistration.registerMethod(method);

                        // Finally create the intercepted method
                        MethodDesc forwardDescriptor = forwardingMethods.get(methodDesc);
                        createInterceptedMethod(method, cc, metadataField, constructedField, forwardDescriptor, cc::this_);
                    } else {
                        // Only decorators are applied
                        cc.method(methodDesc, dmc -> {
                            List<ParamVar> params = new ArrayList<>(method.parametersCount());
                            for (MethodParameterInfo param : method.parameters()) {
                                params.add(dmc.parameter(param.nameOrDefault()));
                            }
                            dmc.body(db0 -> {
                                // Delegate to super class if not constructed yet
                                db0.ifNot(cc.this_().field(constructedField), db1 -> {
                                    if (method.isAbstract()) {
                                        db1.throw_(IllegalStateException.class, "Cannot invoke abstract method");
                                    } else {
                                        db1.return_(db1.invokeVirtual(forwardDesc, cc.this_(), params));
                                    }
                                });

                                DecoratorMethod decoratorMethod = decoration.firstDecoratorMethod();
                                DecoratorInfo firstDecorator = decoratorMethod.decorator;
                                FieldVar decoratorInstance = cc.this_().field(FieldDesc.of(cc.type(),
                                        firstDecorator.getIdentifier(), Object.class));
                                // We need to use the decorator method in order to support not visible or generic decorators
                                MethodDesc decoratorMethodDesc = methodDescOf(decoratorMethod.method);
                                db0.return_(db0.cast(db0.invokeInterface(decoratorMethodDesc, decoratorInstance, params),
                                        methodDesc.returnType()));
                            });
                        });
                    }
                }

                bc.return_();
            });
        });
    }

    static Function<Set<AnnotationInstanceEquivalenceProxy>, String> createBindingsFun(IntegerHolder bindingIdx,
            BlockCreator bc, Expr bindingsMap, Map<AnnotationInstanceEquivalenceProxy, Expr> bindingsLiterals,
            BeanInfo bean, AnnotationLiteralProcessor annotationLiterals) {
        Function<AnnotationInstanceEquivalenceProxy, Expr> bindingsLiteralFun = binding -> {
            // Create annotation literal if needed
            ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.get().name());
            return bc.localVar("literal", annotationLiterals.create(bc, bindingClass, binding.get()));
        };

        return bindings -> {
            String key = "b" + bindingIdx.i++;
            Expr value;
            if (bindings.size() == 1) {
                value = bc.invokeStatic(MethodDescs.COLLECTIONS_SINGLETON,
                        bindingsLiterals.computeIfAbsent(bindings.iterator().next(), bindingsLiteralFun));
            } else {
                LocalVar bindingsArray = bc.localVar("bindings", bc.newEmptyArray(Object.class, bindings.size()));
                int bindingsIndex = 0;
                for (AnnotationInstanceEquivalenceProxy binding : bindings) {
                    bc.set(bindingsArray.elem(bindingsIndex), bindingsLiterals.computeIfAbsent(binding, bindingsLiteralFun));
                    bindingsIndex++;
                }
                value = bc.invokeStatic(MethodDescs.SETS_OF, bindingsArray);
            }
            bc.withMap(bindingsMap).put(Const.of(key), value);
            return key;
        };
    }

    static Function<List<InterceptorInfo>, String> createInterceptorChainKeysFun(IntegerHolder chainIdx,
            BlockCreator bc, Expr interceptorChainMap, Map<String, LocalVar> interceptorInstanceToLocalVar,
            Map<String, ? extends Var> interceptorBeanToVar) {
        return interceptors -> {
            String key = "i" + chainIdx.i++;
            if (interceptors.size() == 1) {
                // List<InvocationContextImpl.InterceptorInvocation> chain = Collections.singletonList(...);
                InterceptorInfo interceptor = interceptors.get(0);
                LocalVar interceptorInstance = interceptorInstanceToLocalVar.get(interceptor.getIdentifier());
                Expr interceptionInvocation = bc.invokeStatic(MethodDescs.INTERCEPTOR_INVOCATION_AROUND_INVOKE,
                        interceptorBeanToVar.get(interceptor.getIdentifier()), interceptorInstance);
                bc.withMap(interceptorChainMap).put(Const.of(key), bc.listOf(interceptionInvocation));
            } else {
                // List<InvocationContextImpl.InterceptorInvocation> chain = new ArrayList<>();
                LocalVar chain = bc.localVar("chain", bc.new_(ConstructorDesc.of(ArrayList.class)));
                for (InterceptorInfo interceptor : interceptors) {
                    // chain.add(InvocationContextImpl.InterceptorInvocation.aroundInvoke(p3,interceptorInstanceMap.get(InjectableInterceptor.getIdentifier())))
                    LocalVar interceptorInstance = interceptorInstanceToLocalVar.get(interceptor.getIdentifier());
                    Expr interceptionInvocation = bc.invokeStatic(MethodDescs.INTERCEPTOR_INVOCATION_AROUND_INVOKE,
                            interceptorBeanToVar.get(interceptor.getIdentifier()), interceptorInstance);
                    bc.withList(chain).add(interceptionInvocation);
                }
                bc.withMap(interceptorChainMap).put(Const.of(key), chain);
            }
            return key;
        };
    }

    private void processDecorator(Gizmo gizmo, ClassCreator subclass,
            DecoratorInfo decorator, BeanInfo bean, Type providerType,
            BlockCreator subclassCtor,
            ParamVar decoratorParam, Map<String, LocalVar> decoratorToLocalVar,
            ParamVar ccParam, Map<MethodDesc, MethodDesc> forwardingMethods) {

        // First generate the delegate subclass
        // An instance of this subclass is injected in the delegate injection point of a decorator instance
        ClassInfo decoratorClass = decorator.getTarget().get().asClass();
        String baseName;
        if (decoratorClass.enclosingClass() != null) {
            baseName = decoratorClass.enclosingClass().withoutPackagePrefix() + UNDERSCORE
                    + decoratorClass.name().withoutPackagePrefix();
        } else {
            baseName = decoratorClass.name().withoutPackagePrefix();
        }
        // Name: AlphaDecorator_FooBeanId_Delegate_Subclass
        String generatedName = generatedName(providerType.name(),
                baseName + UNDERSCORE + bean.getIdentifier() + UNDERSCORE + "Delegate");

        Set<MethodInfo> decoratedMethods = bean.getDecoratedMethods(decorator);
        Set<MethodDesc> decoratedMethodDescriptors = new HashSet<>(decoratedMethods.size());
        for (MethodInfo m : decoratedMethods) {
            decoratedMethodDescriptors.add(methodDescOf(m));
        }

        Map<MethodDesc, DecoratorMethod> nextDecorators = bean.getNextDecorators(decorator);
        List<DecoratorInfo> decoratorParameters = new ArrayList<>();
        for (DecoratorMethod decoratorMethod : nextDecorators.values()) {
            decoratorParameters.add(decoratorMethod.decorator);
        }
        Collections.sort(decoratorParameters);

        List<ClassDesc> delegateSubclassCtorParams = new ArrayList<>();

        ClassDesc delegateSubclass = gizmo.class_(generatedName, cc -> {
            ClassInfo delegateTypeClass = decorator.getDelegateTypeClass();
            boolean delegateTypeIsInterface = delegateTypeClass.isInterface();
            // The subclass implements/extends the delegate type
            if (delegateTypeIsInterface) {
                cc.implements_(classDescOf(delegateTypeClass));
            } else {
                cc.extends_(classDescOf(delegateTypeClass));
            }

            // Holds a reference to the subclass of the decorated bean
            FieldDesc subclassField = cc.field("subclass", fc -> {
                fc.private_();
                fc.final_();
                fc.setType(subclass.type());
            });

            List<ClassDesc> nextDecoratorTypes = new ArrayList<>();
            Map<DecoratorInfo, FieldDesc> nextDecoratorToField = new HashMap<>();
            for (DecoratorInfo nextDecorator : decoratorParameters) {
                FieldDesc desc = cc.field(nextDecorator.getIdentifier(), fc -> {
                    fc.private_();
                    fc.final_();
                    // this can be always of type `Object`, because decorated types are always interfaces
                    // and their methods are always invoked via `invokeinterface` (see elsewhere in this class)
                    // and the JVM verifier doesn't care about the receiver type of interface method calls
                    // (see e.g. https://wiki.openjdk.org/display/HotSpot/InterfaceCalls)
                    fc.setType(Object.class);
                });
                nextDecoratorTypes.add(desc.type());
                nextDecoratorToField.put(nextDecorator, desc);
            }

            // Constructor
            cc.constructor(mc -> {
                ParamVar subclassParam = mc.parameter("subclass", subclass.type());
                delegateSubclassCtorParams.add(subclass.type());
                List<ParamVar> nextDecoratorParams = new ArrayList<>();
                for (int i = 0; i < nextDecoratorTypes.size(); i++) {
                    nextDecoratorParams.add(mc.parameter("nextDecorator" + i, nextDecoratorTypes.get(i)));
                    delegateSubclassCtorParams.add(nextDecoratorTypes.get(i));
                }

                mc.body(bc -> {
                    if (delegateTypeIsInterface) {
                        bc.invokeSpecial(MethodDescs.OBJECT_CONSTRUCTOR, cc.this_());
                    } else {
                        bc.invokeSpecial(ConstructorDesc.of(classDescOf(delegateTypeClass)), cc.this_());
                    }

                    // Set fields
                    bc.set(cc.this_().field(subclassField), subclassParam);
                    for (int i = 0; i < decoratorParameters.size(); i++) {
                        DecoratorInfo nextDecorator = decoratorParameters.get(i);
                        bc.set(cc.this_().field(nextDecoratorToField.get(nextDecorator)), nextDecoratorParams.get(i));
                    }

                    bc.return_();
                });
            });

            IndexView index = bean.getDeployment().getBeanArchiveIndex();

            // Identify the set of methods that should be delegated
            // Note that the delegate subclass must override ALL methods from the delegate type
            // This is not enough if the delegate type is parameterized
            Set<MethodKey> methods = new HashSet<>();
            Methods.addDelegateTypeMethods(index, delegateTypeClass, methods);

            // The delegate type can declare type parameters
            // For example @Delegate Converter<String> should result in a T -> String mapping
            List<TypeVariable> typeParameters = delegateTypeClass.typeParameters();
            Map<String, Type> resolvedTypeParameters;
            if (!typeParameters.isEmpty()) {
                resolvedTypeParameters = new HashMap<>();
                // The delegate type can be used to infer the parameter types
                Type delegateType = decorator.getDelegateType();
                if (delegateType.kind() == Kind.PARAMETERIZED_TYPE) {
                    List<Type> typeArguments = delegateType.asParameterizedType().arguments();
                    for (int i = 0; i < typeParameters.size(); i++) {
                        resolvedTypeParameters.put(typeParameters.get(i).identifier(), typeArguments.get(i));
                    }
                }
            } else {
                resolvedTypeParameters = Map.of();
            }

            for (MethodKey m : methods) {
                MethodInfo method = m.method;
                MethodDesc methodDescriptor = methodDescOf(method);
                cc.method(method.name(), mc -> {
                    mc.public_();
                    mc.returning(classDescOf(method.returnType()));
                    List<ParamVar> params = new ArrayList<>();
                    for (int i = 0; i < method.parametersCount(); i++) {
                        String paramName = method.parameterName(i);
                        if (paramName == null || paramName.isBlank()) {
                            paramName = "param" + i;
                        }
                        params.add(mc.parameter(paramName, classDescOf(method.parameterType(i))));
                    }
                    for (Type exception : method.exceptions()) {
                        mc.throws_(classDescOf(exception));
                    }

                    mc.body(bc -> {
                        // Create a resolved descriptor variant if a param contains a type variable
                        // E.g. ping(T) -> ping(String)
                        MethodDesc resolvedMethodDesc;
                        if (typeParameters.isEmpty() || (!Methods.containsTypeVariableParameter(method)
                                && !Types.containsTypeVariable(method.returnType()))) {
                            resolvedMethodDesc = null;
                        } else {
                            Type returnType = Types.resolveTypeParam(method.returnType(), resolvedTypeParameters, index);
                            List<Type> paramTypes = Types.getResolvedParameters(delegateTypeClass, resolvedTypeParameters,
                                    method, index);
                            ClassDesc[] paramTypesArray = new ClassDesc[paramTypes.size()];
                            for (int i = 0; i < paramTypesArray.length; i++) {
                                paramTypesArray[i] = classDescOf(paramTypes.get(i));
                            }
                            resolvedMethodDesc = ClassMethodDesc.of(classDescOf(method.declaringClass()),
                                    method.name(), MethodTypeDesc.of(classDescOf(returnType), paramTypesArray));
                        }

                        DecoratorMethod nextDecorator = null;
                        MethodDesc nextDecoratorDecorated = null;
                        for (Entry<MethodDesc, DecoratorMethod> e : nextDecorators.entrySet()) {
                            // Find the next decorator for the current delegate type method
                            if (Methods.descriptorMatches(e.getKey(), methodDescriptor)
                                    || (resolvedMethodDesc != null
                                            && Methods.descriptorMatches(e.getKey(), resolvedMethodDesc))
                                    || Methods.descriptorMatches(methodDescOf(e.getValue().method), methodDescriptor)) {
                                nextDecorator = e.getValue();
                                nextDecoratorDecorated = e.getKey();
                                break;
                            }
                        }

                        if (nextDecorator != null
                                && isDecorated(decoratedMethodDescriptors, methodDescriptor, resolvedMethodDesc,
                                        nextDecoratorDecorated)) {
                            // This method is decorated by this decorator and there is a next decorator in the chain
                            // Just delegate to the next decorator
                            FieldVar delegateTo = cc.this_().field(nextDecoratorToField.get(nextDecorator.decorator));
                            bc.return_(bc.invokeInterface(methodDescOf(nextDecorator.method), delegateTo, params));
                        } else {
                            // This method is not decorated or no next decorator was found in the chain
                            MethodDesc forwardingMethod = null;
                            MethodInfo decoratedMethod = bean.getDecoratedMethod(method, decorator);
                            MethodDesc decoratedMethodDesc = decoratedMethod != null ? methodDescOf(decoratedMethod) : null;
                            for (Entry<MethodDesc, MethodDesc> entry : forwardingMethods.entrySet()) {
                                if (Methods.descriptorMatches(entry.getKey(), methodDescriptor)
                                        || (resolvedMethodDesc != null // Also try to find the forwarding method for the resolved variant
                                                && Methods.descriptorMatches(entry.getKey(), resolvedMethodDesc))
                                        || (decoratedMethodDesc != null // Finally, try to match the decorated method
                                                && Methods.descriptorMatches(entry.getKey(), decoratedMethodDesc))) {
                                    forwardingMethod = entry.getValue();
                                    break;
                                }
                            }

                            InstanceFieldVar delegateTo = cc.this_().field(subclassField);
                            if (forwardingMethod != null) {
                                // Delegate to the subclass forwarding method
                                List<Expr> args = new ArrayList<>();
                                for (int i = 0; i < params.size(); i++) {
                                    args.add(bc.cast(params.get(i), forwardingMethod.parameterType(i)));
                                }
                                bc.return_(bc.invokeVirtual(forwardingMethod, delegateTo, args));
                            } else {
                                // No forwarding method exists
                                // Simply delegate to subclass
                                if (method.declaringClass().isInterface()) {
                                    bc.return_(bc.invokeInterface(methodDescriptor, delegateTo, params));
                                } else {
                                    bc.return_(bc.invokeVirtual(methodDescriptor, delegateTo, params));
                                }
                            }
                        }
                    });
                });
            }
        });

        // Now modify the subclass constructor

        LocalVar cc = subclassCtor.localVar("cc", subclassCtor.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD, ccParam));

        // Create new delegate subclass instance
        Expr[] params = new Expr[1 + decoratorParameters.size()];
        params[0] = subclass.this_();
        int paramIdx = 1;
        for (DecoratorInfo decoratorParameter : decoratorParameters) {
            LocalVar decoratorVar = decoratorToLocalVar.get(decoratorParameter.getIdentifier());
            if (decoratorVar == null) {
                throw new IllegalStateException("Decorator var must not be null");
            }
            params[paramIdx] = decoratorVar;
            paramIdx++;
        }
        Expr delegateSubclassInstance = subclassCtor.new_(ConstructorDesc.of(
                delegateSubclass, delegateSubclassCtorParams), params);

        // Set the DecoratorDelegateProvider to satisfy the delegate IP
        LocalVar prev = subclassCtor.localVar("prev", subclassCtor.invokeStatic(
                MethodDescs.DECORATOR_DELEGATE_PROVIDER_SET, cc, delegateSubclassInstance));

        // Create the decorator instance
        LocalVar decoratorInstance = subclassCtor.localVar("decoratorInstance",
                subclassCtor.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, decoratorParam, cc));

        // And unset the delegate IP afterwards
        subclassCtor.invokeStatic(MethodDescs.DECORATOR_DELEGATE_PROVIDER_SET, cc, prev);

        decoratorToLocalVar.put(decorator.getIdentifier(), decoratorInstance);

        // Store the decorator instance in a field
        FieldDesc decoratorField = subclass.field(decorator.getIdentifier(), fc -> {
            fc.private_();
            fc.final_();
            fc.setType(Object.class);
        });

        subclassCtor.set(subclass.this_().field(decoratorField), decoratorInstance);
    }

    private boolean isDecorated(Set<MethodDesc> decoratedMethodDescriptors, MethodDesc original,
            MethodDesc resolved, MethodDesc nextDecoratorDecorated) {
        for (MethodDesc decorated : decoratedMethodDescriptors) {
            if (Methods.descriptorMatches(decorated, original)
                    || (resolved != null && Methods.descriptorMatches(decorated, resolved))
                    || Methods.descriptorMatches(decorated, nextDecoratorDecorated)) {
                return true;
            }
        }
        return false;
    }

    static MethodDesc createForwardingMethod(io.quarkus.gizmo2.creator.ClassCreator subclass, ClassDesc providerType,
            MethodInfo method, boolean implementingInterface) {
        return subclass.method(method.name() + "$$superforward", mc -> {
            mc.returning(classDescOf(method.returnType()));
            List<ParamVar> params = new ArrayList<>(method.parametersCount());
            for (MethodParameterInfo param : method.parameters()) {
                params.add(mc.parameter(param.nameOrDefault(), classDescOf(param.type())));
            }
            mc.body(bc -> {
                // `invokespecial` requires the descriptor to point to a method on a _direct_ supertype
                // if we're extending a class, we have to always create a `ClassMethodDesc`
                // if we're implementing an interface, we have to always create an `InterfaceMethodDesc`
                // in both cases, the direct supertype is `providerType`
                MethodDesc methodDesc = methodDescOf(method);
                MethodDesc superMethod = implementingInterface
                        ? InterfaceMethodDesc.of(providerType, methodDesc.name(), methodDesc.type())
                        : ClassMethodDesc.of(providerType, methodDesc.name(), methodDesc.type());
                Expr result = bc.invokeSpecial(superMethod, subclass.this_(), params);
                bc.return_(bc.cast(result, classDescOf(method.returnType())));
            });
        });
    }

    static void createInterceptedMethod(MethodInfo method, ClassCreator subclass,
            FieldDesc metadataField, FieldDesc constructedField, MethodDesc forwardMethod,
            Supplier<Expr> getTarget) {

        subclass.method(methodDescOf(method), mc -> {
            mc.public_();
            List<ParamVar> params = IntStream.range(0, method.parametersCount())
                    .mapToObj(i -> mc.parameter("param" + i, i))
                    .toList();
            for (Type exception : method.exceptions()) {
                mc.throws_(classDescOf(exception));
            }
            mc.body(b0 -> {
                // Delegate to super class if not constructed yet
                b0.ifNot(subclass.this_().field(constructedField), b1 -> {
                    if (Modifier.isAbstract(method.flags())) {
                        b1.throw_(IllegalStateException.class, "Cannot invoke abstract method");
                    } else {
                        b1.return_(b1.invokeVirtual(forwardMethod, subclass.this_(), params));
                    }
                });

                // Object[] args = new Object[] {p1}
                LocalVar args = b0.localVar("args",
                        method.parametersCount() > 0 ? b0.newArray(Object.class, params) : Const.ofNull(Object[].class));

                b0.try_(tc -> {
                    tc.body(b1 -> {
                        // InvocationContexts.performAroundInvoke(...)
                        FieldVar methodMetadata = subclass.this_().field(metadataField);
                        Expr result = b1.invokeStatic(MethodDescs.INVOCATION_CONTEXTS_PERFORM_AROUND_INVOKE,
                                getTarget.get(), args, methodMetadata);
                        if (method.returnType().kind() == Kind.VOID) {
                            result = Const.ofVoid();
                        }
                        b1.return_(result);
                    });

                    // catch exceptions declared on the original method
                    boolean addCatchRuntimeException = true;
                    boolean addCatchException = true;
                    for (Type declaredException : method.exceptions()) {
                        tc.catch_(classDescOf(declaredException), "e", BlockCreator::throw_);

                        DotName exName = declaredException.name();
                        if (JAVA_LANG_RUNTIME_EXCEPTION.equals(exName) || JAVA_LANG_THROWABLE.equals(exName)) {
                            addCatchRuntimeException = false;
                        }
                        if (JAVA_LANG_EXCEPTION.equals(exName) || JAVA_LANG_THROWABLE.equals(exName)) {
                            addCatchException = false;
                        }
                    }
                    // catch (RuntimeException e) if not already caught
                    if (addCatchRuntimeException) {
                        tc.catch_(RuntimeException.class, "e", BlockCreator::throw_);
                    }
                    // now catch the rest (Exception e) if not already caught
                    // this catch is _not_ included for Kotlin methods because Kotlin has no checked exceptions contract
                    if (addCatchException && !isKotlinMethod(method)) {
                        tc.catch_(Exception.class, "e", (b1, e) -> {
                            // and wrap them in ArcUndeclaredThrowableException
                            b1.throw_(b1.new_(
                                    ConstructorDesc.of(ArcUndeclaredThrowableException.class, String.class, Throwable.class),
                                    Const.of("Error invoking subclass method"), e));
                        });
                    }
                });
            });
        });
    }

    static class IntegerHolder {
        int i = 1;
    }

    // either `interception` or `decoration` may be `null` if the `method` isn't intercepted or decorated
    record InterceptedDecoratedMethod(int index, MethodInfo method, BeanInfo.InterceptionInfo interception,
            BeanInfo.DecorationInfo decoration) {
    }

    record MethodGroup(int id, List<InterceptedDecoratedMethod> interceptedDecoratedMethods) {
    }

    record CodeGenInfo(
            List<InterceptorInfo> boundInterceptors,
            List<DecoratorInfo> boundDecorators,
            List<InterceptedDecoratedMethod> interceptedDecoratedMethods,
            List<MethodGroup> methodGroups) {
    }

    private CodeGenInfo preprocess(BeanInfo bean) {
        List<InterceptorInfo> boundInterceptors = bean.getBoundInterceptors();

        List<DecoratorInfo> boundDecorators = bean.getBoundDecorators();

        List<InterceptedDecoratedMethod> interceptedDecoratedMethods = new ArrayList<>();
        List<MethodInfo> interestingMethods = bean.getInterceptedOrDecoratedMethods();
        for (int i = 0; i < interestingMethods.size(); i++) {
            MethodInfo method = interestingMethods.get(i);
            InterceptionInfo interception = bean.getInterceptedMethods().get(method);
            DecorationInfo decoration = bean.getDecoratedMethods().get(method);
            interceptedDecoratedMethods.add(new InterceptedDecoratedMethod(i, method, interception, decoration));
        }

        List<MethodGroup> methodGroups = Grouping.of(interceptedDecoratedMethods, 30, MethodGroup::new);

        return new CodeGenInfo(List.copyOf(boundInterceptors), List.copyOf(boundDecorators),
                List.copyOf(interceptedDecoratedMethods), List.copyOf(methodGroups));
    }
}
