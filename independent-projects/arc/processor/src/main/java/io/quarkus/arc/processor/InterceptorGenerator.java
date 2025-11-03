package io.quarkus.arc.processor;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;

import java.lang.constant.ClassDesc;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InterceptorCreator.InterceptFunction;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.FieldVar;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.FieldDesc;

/**
 *
 * @author Martin Kouba
 */
public class InterceptorGenerator extends BeanGenerator {

    protected static final String FIELD_NAME_BINDINGS = "bindings";

    public InterceptorGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            PrivateMembersCollector privateMembers, boolean generateSources, ReflectionRegistration reflectionRegistration,
            Set<String> existingClasses, Map<BeanInfo, String> beanToGeneratedName,
            Predicate<DotName> injectionPointAnnotationsPredicate) {
        super(annotationLiterals, applicationClassPredicate, privateMembers, generateSources, reflectionRegistration,
                existingClasses, beanToGeneratedName, injectionPointAnnotationsPredicate, Collections.emptyList());
    }

    /**
     * Precompute the generated name for the given interceptor so that the {@link ComponentsProviderGenerator}
     * can be executed before all interceptors metadata are generated.
     *
     * @param interceptor
     */
    void precomputeGeneratedName(InterceptorInfo interceptor) {
        String baseName;
        String targetPackage;
        if (interceptor.isSynthetic()) {
            DotName creatorClassName = DotName.createSimple(interceptor.getCreatorClass());
            baseName = InterceptFunction.class.getSimpleName() + "_" + interceptor.getIdentifier();
            targetPackage = DotNames.packagePrefix(creatorClassName);
        } else {
            ClassInfo interceptorClass = interceptor.getTarget().get().asClass();
            baseName = interceptorClass.name().withoutPackagePrefix();
            targetPackage = DotNames.packagePrefix(interceptor.getProviderType().name());
        }
        beanToGeneratedBaseName.put(interceptor, baseName);
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        beanToGeneratedName.put(interceptor, generatedName);
    }

    /**
     *
     * @param interceptor bean
     * @return a collection of resources
     */
    Collection<Resource> generate(InterceptorInfo interceptor) {
        DotName targetPackageClassName = interceptor.isSynthetic()
                ? DotName.createSimple(interceptor.getCreatorClass())
                : interceptor.getProviderType().name();
        DotName isApplicationClassName = interceptor.isSynthetic()
                ? targetPackageClassName
                : interceptor.getBeanClass();

        String baseName = beanToGeneratedBaseName.get(interceptor);
        String targetPackage = DotNames.packagePrefix(targetPackageClassName);
        String generatedName = beanToGeneratedName.get(interceptor);

        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(isApplicationClassName)
                || interceptor.isForceApplicationClass();
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.INTERCEPTOR_BEAN : null, generateSources);

        Gizmo gizmo = gizmo(classOutput);

        generateInterceptor(gizmo, interceptor, generatedName, baseName, targetPackage, isApplicationClass);

        return classOutput.getResources();
    }

    private void generateInterceptor(Gizmo gizmo, InterceptorInfo interceptor, String generatedName, String baseName,
            String targetPackage, boolean isApplicationClass) {
        gizmo.class_(generatedName, cc -> {
            cc.implements_(InjectableInterceptor.class);
            cc.implements_(Supplier.class);

            FieldDesc beanTypesField = cc.field(FIELD_NAME_BEAN_TYPES, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Set.class);
            });
            FieldDesc bindingsField = cc.field(FIELD_NAME_BINDINGS, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Set.class);
            });
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderField = new HashMap<>();
            generateProviderFields(interceptor, cc, injectionPointToProviderField, Map.of(), Map.of());

            generateConstructor(cc, interceptor, beanTypesField, bindingsField, injectionPointToProviderField,
                    isApplicationClass, interceptor.isSynthetic() ? bc -> {
                        SyntheticComponentsUtil.addParamsFieldAndInit(cc, bc, interceptor.getParams(),
                                annotationLiterals, interceptor.getDeployment().getBeanArchiveIndex());
                    } : ignored -> {
                    });

            generateCreate(cc, interceptor, new ProviderType(interceptor.getProviderType()), baseName,
                    injectionPointToProviderField, Map.of(), Map.of(), targetPackage, isApplicationClass);

            generateSupplierGet(cc);
            generateInjectableReferenceProviderGet(interceptor, cc, baseName);
            generateGetIdentifier(cc, interceptor);
            generateGetTypes(beanTypesField, cc);
            // always `@Dependent` -- no need to `generateGetScope()`
            // always default qualifiers -- no need to `generateGetQualifiers()`
            // never an alternative -- no need to `generateIsAlternative()`
            generateGetPriority(cc, interceptor);
            // never any stereotypes -- no need to `generateGetStereotypes()`
            generateGetBeanClass(cc, interceptor);
            // never named -- no need to `generateGetName()`
            // never default bean -- no need to `generateIsDefaultBean()`
            // `InjectableInterceptor.getKind()` always returns `Kind.INTERCEPTOR` -- no need to `generateGetKind()`
            // never suppressed -- no need to `generateIsSuppressed()`
            generateGetInjectionPoints(cc, interceptor);
            generateEquals(cc, interceptor);
            generateHashCode(cc, interceptor);
            generateToString(cc);

            generateGetInterceptorBindings(cc, bindingsField);
            generateIntercepts(cc, interceptor);
            generateIntercept(cc, interceptor, isApplicationClass);
        });
    }

    private void generateConstructor(ClassCreator cc, InterceptorInfo interceptor, FieldDesc beanTypesField,
            FieldDesc bindingsField, Map<InjectionPointInfo, FieldDesc> injectionPointToProviderField,
            boolean isApplicationClass, Consumer<BlockCreator> additionalCode) {
        super.generateConstructor(cc, interceptor, beanTypesField, null, null, null, injectionPointToProviderField,
                Map.of(), Map.of(), bc -> {
                    LocalVar bindings = bc.localVar("bindings", bc.new_(HashSet.class));
                    for (AnnotationInstance binding : interceptor.getBindings()) {
                        ClassInfo bindingClass = interceptor.getDeployment().getInterceptorBinding(binding.name());
                        bc.withSet(bindings).add(annotationLiterals.create(bc, bindingClass, binding));
                    }
                    bc.set(cc.this_().field(bindingsField), bindings);

                    // Initialize a list of BiFunction for each interception type if multiple interceptor methods are declared in a hierarchy
                    ClassDesc interceptorClass = classDescOf(interceptor.getProviderType());
                    generateInterceptorMethodsField(cc, bc, InterceptionType.AROUND_INVOKE,
                            interceptor.getAroundInvokes(), interceptorClass, isApplicationClass);
                    generateInterceptorMethodsField(cc, bc, InterceptionType.AROUND_CONSTRUCT,
                            interceptor.getAroundConstructs(), interceptorClass, isApplicationClass);
                    generateInterceptorMethodsField(cc, bc, InterceptionType.POST_CONSTRUCT,
                            interceptor.getPostConstructs(), interceptorClass, isApplicationClass);
                    generateInterceptorMethodsField(cc, bc, InterceptionType.PRE_DESTROY,
                            interceptor.getPreDestroys(), interceptorClass, isApplicationClass);

                    additionalCode.accept(bc);
                });
    }

    private void generateInterceptorMethodsField(ClassCreator cc, BlockCreator bc, InterceptionType interceptionType,
            List<MethodInfo> methods, ClassDesc interceptorClass, boolean isApplicationClass) {
        if (methods.size() < 2) {
            // if there's just one interceptor method, we'll generate a more streamlined code, see `generateIntercept()`
            return;
        }

        FieldDesc fieldDesc = cc.field(interceptorMethodsField(interceptionType), fc -> {
            fc.private_();
            fc.final_();
            fc.setType(List.class);
        });

        LocalVar list = bc.localVar(fieldDesc.name(), bc.new_(ArrayList.class));
        for (MethodInfo method : methods) {
            Expr bifunc = bc.lambda(BiFunction.class, lc -> {
                ParamVar interceptor = lc.parameter("interceptor", 0);
                ParamVar invocationContext = lc.parameter("invocationContext", 1);
                lc.body(lbc -> {
                    Expr result = invokeInterceptorMethod(lbc, interceptorClass, method, interceptionType,
                            isApplicationClass, invocationContext, interceptor);
                    lbc.return_(interceptionType == InterceptionType.AROUND_INVOKE ? result : Const.ofNull(Object.class));
                });
            });
            bc.withList(list).add(bifunc);
        }
        bc.set(cc.this_().field(fieldDesc), list);
    }

    /**
     * @see InjectableBean#getBeanClass()
     */
    protected void generateGetBeanClass(ClassCreator cc, InterceptorInfo interceptor) {
        cc.method("getBeanClass", mc -> {
            mc.returning(Class.class);
            mc.body(bc -> {
                bc.return_(interceptor.isSynthetic()
                        ? Const.of(interceptor.getCreatorClass())
                        : Const.of(classDescOf(interceptor.getBeanClass())));
            });
        });
    }

    /**
     * @see InjectableInterceptor#getInterceptorBindings()
     */
    protected void generateGetInterceptorBindings(ClassCreator cc, FieldDesc bindingsField) {
        cc.method("getInterceptorBindings", mc -> {
            mc.returning(Set.class);
            mc.body(bc -> {
                bc.return_(cc.this_().field(bindingsField));
            });
        });
    }

    /**
     * @see InjectableInterceptor#intercepts(jakarta.enterprise.inject.spi.InterceptionType)
     */
    protected void generateIntercepts(ClassCreator cc, InterceptorInfo interceptor) {
        cc.method("intercepts", mc -> {
            mc.returning(boolean.class);
            ParamVar interceptionType = mc.parameter("interceptionType", InterceptionType.class);
            mc.body(bc -> {
                if (interceptor.isSynthetic()) {
                    FieldVar enumValue = Expr.staticField(FieldDesc.of(InterceptionType.class,
                            interceptor.getInterceptionType().name()));
                    bc.return_(bc.eq(enumValue, interceptionType));
                } else {
                    generateIntercepts(interceptor, InterceptionType.AROUND_INVOKE, bc, interceptionType);
                    generateIntercepts(interceptor, InterceptionType.POST_CONSTRUCT, bc, interceptionType);
                    generateIntercepts(interceptor, InterceptionType.PRE_DESTROY, bc, interceptionType);
                    generateIntercepts(interceptor, InterceptionType.AROUND_CONSTRUCT, bc, interceptionType);
                    bc.returnFalse();
                }
            });
        });
    }

    private void generateIntercepts(InterceptorInfo interceptor, InterceptionType interceptionType, BlockCreator bc,
            ParamVar interceptionTypeParam) {
        if (interceptor.intercepts(interceptionType)) {
            FieldVar enumValue = Expr.staticField(FieldDesc.of(InterceptionType.class, interceptionType.name()));
            bc.if_(bc.eq(enumValue, interceptionTypeParam), BlockCreator::returnTrue);
        }
    }

    /**
     * @see InjectableInterceptor#intercept(InterceptionType, Object, jakarta.interceptor.InvocationContext)
     */
    protected void generateIntercept(ClassCreator cc, InterceptorInfo interceptor, boolean isApplicationClass) {
        cc.method("intercept", mc -> {
            mc.returning(Object.class);
            ParamVar interceptionType = mc.parameter("interceptionType", InterceptionType.class);
            ParamVar interceptorInstance = mc.parameter("interceptorInstance", Object.class);
            ParamVar invocationContext = mc.parameter("invocationContext", InvocationContext.class);
            mc.body(b0 -> {
                if (interceptor.isSynthetic()) {
                    b0.if_(b0.eq(Const.of(interceptor.getInterceptionType()), interceptionType), b1 -> {
                        Expr interceptFunction = b1.cast(interceptorInstance, InterceptFunction.class);
                        b1.return_(b1.invokeInterface(MethodDescs.INTERCEPT_FUNCTION_INTERCEPT, interceptFunction,
                                invocationContext));
                    });
                } else {
                    ClassDesc interceptorClass = classDescOf(interceptor.getProviderType());
                    generateIntercept(cc, b0, interceptor.getAroundInvokes(), InterceptionType.AROUND_INVOKE,
                            interceptorClass, isApplicationClass, interceptionType, interceptorInstance, invocationContext);
                    generateIntercept(cc, b0, interceptor.getPostConstructs(), InterceptionType.POST_CONSTRUCT,
                            interceptorClass, isApplicationClass, interceptionType, interceptorInstance, invocationContext);
                    generateIntercept(cc, b0, interceptor.getPreDestroys(), InterceptionType.PRE_DESTROY,
                            interceptorClass, isApplicationClass, interceptionType, interceptorInstance, invocationContext);
                    generateIntercept(cc, b0, interceptor.getAroundConstructs(), InterceptionType.AROUND_CONSTRUCT,
                            interceptorClass, isApplicationClass, interceptionType, interceptorInstance, invocationContext);
                }
                b0.return_(Const.ofNull(Object.class));
            });
        });
    }

    private void generateIntercept(ClassCreator cc, BlockCreator b0, List<MethodInfo> interceptorMethods,
            InterceptionType interceptionType, ClassDesc interceptorClass, boolean isApplicationClass,
            ParamVar interceptionTypeParam, ParamVar interceptorInstanceParam, ParamVar invocationContextParam) {
        if (interceptorMethods.isEmpty()) {
            return;
        }
        b0.if_(b0.eq(Const.of(interceptionType), interceptionTypeParam), b1 -> {
            Expr result;
            if (interceptorMethods.size() == 1) {
                MethodInfo interceptorMethod = interceptorMethods.get(0);
                result = invokeInterceptorMethod(b1, interceptorClass, interceptorMethod,
                        interceptionType, isApplicationClass, invocationContextParam, interceptorInstanceParam);
            } else {
                // Multiple interceptor methods found in the hierarchy
                Expr list = cc.this_().field(FieldDesc.of(cc.type(), interceptorMethodsField(interceptionType), List.class));
                Expr params;
                if (interceptionType == InterceptionType.AROUND_INVOKE) {
                    params = b1.invokeInterface(MethodDescs.INVOCATION_CONTEXT_GET_PARAMETERS, invocationContextParam);
                } else {
                    params = Const.ofNull(Object[].class);
                }
                result = b1.invokeStatic(MethodDescs.INVOCATION_CONTEXTS_PERFORM_SUPERCLASS,
                        invocationContextParam, list, interceptorInstanceParam, params);

            }
            b1.return_(InterceptionType.AROUND_INVOKE == interceptionType ? result : Const.ofNull(Object.class));
        });
    }

    private String interceptorMethodsField(InterceptionType interceptionType) {
        return switch (interceptionType) {
            case AROUND_INVOKE -> "aroundInvokes";
            case AROUND_CONSTRUCT -> "aroundConstructs";
            case POST_CONSTRUCT -> "postConstructs";
            case PRE_DESTROY -> "preDestroys";
            default -> throw new IllegalArgumentException("Unsupported interception type: " + interceptionType);
        };
    }

    private Expr invokeInterceptorMethod(BlockCreator bc, ClassDesc interceptorClass, MethodInfo interceptorMethod,
            InterceptionType interceptionType, boolean isApplicationClass, ParamVar invocationContext,
            ParamVar interceptorInstance) {
        Class<?> resultType;
        if (InterceptionType.AROUND_INVOKE.equals(interceptionType)) {
            resultType = Object.class;
        } else {
            // @PostConstruct, @PreDestroy, @AroundConstruct
            resultType = interceptorMethod.returnType().kind().equals(Type.Kind.VOID) ? void.class : Object.class;
        }
        // Check if interceptor method uses InvocationContext or ArcInvocationContext
        Class<?> invocationContextClass;
        if (interceptorMethod.parameterType(0).name().equals(DotNames.INVOCATION_CONTEXT)) {
            invocationContextClass = InvocationContext.class;
        } else {
            invocationContextClass = ArcInvocationContext.class;
        }
        if (Modifier.isPrivate(interceptorMethod.flags())) {
            privateMembers.add(isApplicationClass, String.format("Interceptor method %s#%s()",
                    interceptorMethod.declaringClass().name(), interceptorMethod.name()));
            reflectionRegistration.registerMethod(interceptorMethod);
            Expr paramTypes = bc.newArray(Class.class, Const.of(invocationContextClass));
            Expr args = bc.newArray(Object.class, invocationContext);
            return bc.invokeStatic(MethodDescs.REFLECTIONS_INVOKE_METHOD,
                    Const.of(classDescOf(interceptorMethod.declaringClass())),
                    Const.of(interceptorMethod.name()), paramTypes, interceptorInstance, args);
        } else {
            return bc.invokeVirtual(ClassMethodDesc.of(interceptorClass, interceptorMethod.name(),
                    resultType, invocationContextClass), interceptorInstance, invocationContext);
        }
    }
}
