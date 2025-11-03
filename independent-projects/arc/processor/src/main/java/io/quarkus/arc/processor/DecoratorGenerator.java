package io.quarkus.arc.processor;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.constructorDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;

import io.quarkus.arc.InjectableDecorator;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

public class DecoratorGenerator extends BeanGenerator {

    protected static final String FIELD_NAME_DECORATED_TYPES = "decoratedTypes";
    protected static final String FIELD_NAME_DELEGATE_TYPE = "delegateType";
    static final String ABSTRACT_IMPL_SUFFIX = "_Impl";

    public DecoratorGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            PrivateMembersCollector privateMembers, boolean generateSources, ReflectionRegistration reflectionRegistration,
            Set<String> existingClasses, Map<BeanInfo, String> beanToGeneratedName,
            Predicate<DotName> injectionPointAnnotationsPredicate) {
        super(annotationLiterals, applicationClassPredicate, privateMembers, generateSources, reflectionRegistration,
                existingClasses, beanToGeneratedName, injectionPointAnnotationsPredicate, Collections.emptyList());
    }

    /**
     * Precompute the generated name for the given decorator so that the {@link ComponentsProviderGenerator} can be executed
     * before all decorators metadata are generated.
     *
     * @param decorator
     */
    void precomputeGeneratedName(DecoratorInfo decorator) {
        ClassInfo decoratorClass = decorator.getTarget().get().asClass();
        String baseName = decoratorClass.name().withoutPackagePrefix();
        String targetPackage = DotNames.packagePrefix(decorator.getProviderType().name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        beanToGeneratedName.put(decorator, generatedName);
        beanToGeneratedBaseName.put(decorator, baseName);
    }

    /**
     *
     * @param decorator bean
     * @return a collection of resources
     */
    Collection<Resource> generate(DecoratorInfo decorator) {
        String baseName = beanToGeneratedBaseName.get(decorator);
        String generatedName = beanToGeneratedName.get(decorator);
        String targetPackage = DotNames.packagePrefix(decorator.getProviderType().name());

        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(decorator.getBeanClass())
                || decorator.isForceApplicationClass();
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.DECORATOR_BEAN : null, generateSources);

        Gizmo gizmo = gizmo(classOutput);

        generateDecorator(gizmo, decorator, generatedName, baseName, targetPackage, isApplicationClass);

        return classOutput.getResources();
    }

    private void generateDecorator(Gizmo gizmo, DecoratorInfo decorator, String generatedName, String baseName,
            String targetPackage, boolean isApplicationClass) {
        ClassInfo decoratorClass = decorator.getTarget().get().asClass();
        gizmo.class_(generatedName, cc -> {
            cc.implements_(InjectableDecorator.class);
            cc.implements_(Supplier.class);

            Type providerType = decorator.getProviderType();
            if (decorator.isAbstract()) {
                String generatedImplName = generateAbstractDecoratorImplementation(gizmo, baseName, targetPackage,
                        decorator, decoratorClass);
                providerType = ClassType.create(generatedImplName);
            }

            FieldDesc beanTypesField = cc.field(FIELD_NAME_BEAN_TYPES, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Set.class);
            });
            FieldDesc decoratedTypesField = cc.field(FIELD_NAME_DECORATED_TYPES, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Set.class);
            });
            FieldDesc delegateTypeField = cc.field(FIELD_NAME_DELEGATE_TYPE, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(java.lang.reflect.Type.class);
            });
            FieldDesc delegateQualifiersField;
            if (!decorator.getDelegateInjectionPoint().hasDefaultedQualifier()) {
                delegateQualifiersField = cc.field(FIELD_NAME_QUALIFIERS, fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Set.class);
                });
            } else {
                delegateQualifiersField = null;
            }
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderField = new HashMap<>();
            generateProviderFields(decorator, cc, injectionPointToProviderField, Map.of(), Map.of());

            generateConstructor(decorator, cc, beanTypesField, injectionPointToProviderField, delegateQualifiersField,
                    decoratedTypesField, delegateTypeField);

            generateCreate(cc, decorator, new ProviderType(providerType), baseName, injectionPointToProviderField,
                    null, null, targetPackage, isApplicationClass);
            if (decorator.hasDestroyLogic()) {
                generateDestroy(cc, decorator, Map.of(), isApplicationClass, baseName, targetPackage);
            }

            generateSupplierGet(cc);
            generateInjectableReferenceProviderGet(decorator, cc, baseName);
            generateGetIdentifier(cc, decorator);
            generateGetTypes(beanTypesField, cc);
            // always `@Dependent` -- no need to `generateGetScope()`
            // always default qualifiers -- no need to `generateGetQualifiers()`
            // never an alternative -- no need to `generateIsAlternative()`
            generateGetPriority(cc, decorator);
            // never any stereotypes -- no need to `generateGetStereotypes()`
            generateGetBeanClass(cc, decorator);
            // never named -- no need to `generateGetName()`
            // never default bean -- no need to `generateIsDefaultBean()`
            // `InjectableDecorator.getKind()` always returns `Kind.DECORATOR` -- no need to `generateGetKind()`
            // never suppressed -- no need to `generateIsSuppressed()`
            generateGetInjectionPoints(cc, decorator);
            generateEquals(cc, decorator);
            generateHashCode(cc, decorator);
            generateToString(cc);

            generateGetDecoratedTypes(cc, decoratedTypesField);
            generateGetDelegateType(cc, delegateTypeField);
            generateGetDelegateQualifiers(cc, delegateQualifiersField);
        });
    }

    private void generateConstructor(DecoratorInfo decorator, ClassCreator cc, FieldDesc beanTypesField,
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderField, FieldDesc delegateQualifiersField,
            FieldDesc decoratedTypesField, FieldDesc delegateTypeField) {
        super.generateConstructor(cc, decorator, beanTypesField, null, null, null, injectionPointToProviderField,
                Map.of(), Map.of(), bc -> {
                    if (delegateQualifiersField != null) {
                        LocalVar delegateQualifiers = bc.localVar("delegateQualifiers", bc.new_(HashSet.class));
                        for (AnnotationInstance delegateQualifier : decorator.getDelegateQualifiers()) {
                            ClassInfo delegateQualifierClass = decorator.getDeployment()
                                    .getQualifier(delegateQualifier.name());
                            bc.withSet(delegateQualifiers).add(
                                    annotationLiterals.create(bc, delegateQualifierClass, delegateQualifier));
                        }
                        bc.set(cc.this_().field(delegateQualifiersField), delegateQualifiers);

                    }

                    LocalVar tccl = bc.localVar("tccl", bc.invokeVirtual(MethodDescs.THREAD_GET_TCCL, bc.currentThread()));
                    RuntimeTypeCreator rttc = RuntimeTypeCreator.of(bc).withTCCL(tccl);

                    LocalVar decoratedTypes = bc.localVar("decoratedTypes", bc.new_(HashSet.class));
                    for (Type decoratedType : decorator.getDecoratedTypes()) {
                        try {
                            bc.withSet(decoratedTypes).add(rttc.create(decoratedType));
                        } catch (IllegalArgumentException e) {
                            throw new IllegalStateException("Unable to construct type for " + decorator
                                    + ": " + e.getMessage());
                        }
                    }
                    bc.set(cc.this_().field(decoratedTypesField), decoratedTypes);

                    LocalVar delegateType;
                    try {
                        delegateType = rttc.create(decorator.getDelegateType());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Unable to construct type for " + decorator
                                + ": " + e.getMessage());
                    }
                    bc.set(cc.this_().field(delegateTypeField), delegateType);
                });
    }

    static boolean isAbstractDecoratorImpl(BeanInfo bean, String providerTypeName) {
        return bean.isDecorator() && ((DecoratorInfo) bean).isAbstract() && providerTypeName.endsWith(ABSTRACT_IMPL_SUFFIX);
    }

    private String generateAbstractDecoratorImplementation(Gizmo gizmo, String baseName, String targetPackage,
            DecoratorInfo decorator, ClassInfo decoratorClass) {

        IndexView index = decorator.getDeployment().getBeanArchiveIndex();

        // MyDecorator_Impl
        String generatedImplName = generatedNameFromTarget(targetPackage, baseName, ABSTRACT_IMPL_SUFFIX);

        gizmo.class_(generatedImplName, cc -> {
            cc.extends_(classDescOf(decoratorClass));

            FieldDesc delegateField = cc.field("impl$delegate", fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Object.class);
            });

            MethodInfo decoratorConstructor = decoratorClass.firstMethod(Methods.INIT);
            cc.constructor(mc -> {
                List<ParamVar> superParams = new ArrayList<>();
                int paramIdx = 0;
                for (MethodParameterInfo parameter : decoratorConstructor.parameters()) {
                    String paramName = parameter.name();
                    Type paramType = parameter.type();
                    superParams.add(mc.parameter(paramName != null ? paramName : "param" + paramIdx, classDescOf(paramType)));
                    paramIdx++;
                }
                ParamVar creationalContext = mc.parameter("creationalContext", CreationalContext.class);
                mc.body(bc -> {
                    bc.invokeSpecial(constructorDescOf(decoratorConstructor), cc.this_(), superParams);
                    bc.set(cc.this_().field(delegateField),
                            bc.invokeStatic(MethodDescs.DECORATOR_DELEGATE_PROVIDER_GET, creationalContext));
                    bc.return_();
                });
            });

            // Find non-decorated methods from all decorated types
            Set<MethodDesc> abstractMethods = new HashSet<>();
            Map<MethodDesc, MethodDesc> bridgeMethods = new HashMap<>();
            for (Type decoratedType : decorator.getDecoratedTypes()) {
                ClassInfo decoratedTypeClass = index.getClassByName(decoratedType.name());
                if (decoratedTypeClass == null) {
                    throw new IllegalStateException("Decorated type not found in the bean archive index: " + decoratedType);
                }

                // A decorated type can declare type parameters
                // For example Converter<String> should result in a T -> String mapping
                List<TypeVariable> typeParameters = decoratedTypeClass.typeParameters();
                Map<String, Type> resolvedTypeParameters = Types.resolveDecoratedTypeParams(decoratedTypeClass, decorator);

                for (MethodInfo method : decoratedTypeClass.methods()) {
                    if (Methods.skipForDelegateSubclass(method)) {
                        continue;
                    }
                    MethodDesc methodDesc = methodDescOf(method);

                    // Create a resolved descriptor variant if a param contains a type variable
                    // E.g. ping(T) -> ping(String)
                    MethodDesc resolvedMethodDesc;
                    if (!typeParameters.isEmpty() && (Methods.containsTypeVariableParameter(method)
                            || Types.containsTypeVariable(method.returnType()))) {
                        List<Type> paramTypes = Types.getResolvedParameters(decoratedTypeClass, resolvedTypeParameters,
                                method, index);
                        Type returnType = Types.resolveTypeParam(method.returnType(), resolvedTypeParameters, index);
                        ClassDesc[] paramTypesArray = new ClassDesc[paramTypes.size()];
                        for (int i = 0; i < paramTypesArray.length; i++) {
                            paramTypesArray[i] = classDescOf(paramTypes.get(i));
                        }
                        resolvedMethodDesc = ClassMethodDesc.of(classDescOf(method.declaringClass()),
                                method.name(), MethodTypeDesc.of(classDescOf(returnType), paramTypesArray));
                    } else {
                        resolvedMethodDesc = null;
                    }

                    boolean include = true;
                    for (MethodInfo decoratorMethod : decoratorClass.methods()) {
                        if (decoratorMethod.isConstructor() || decoratorMethod.isStaticInitializer()) {
                            // we cannot build a `MethodDesc` for constructors and static initializers
                            // (`methodDescOf()` below would throw) and they cannot be decorated anyway
                            //
                            // note that we cannot skip all methods for which `Methods.skipForDelegateSubclass(method)`
                            // returns `true`, because that would immediately skip bridge methods, which we rely on!
                            continue;
                        }
                        if (Methods.descriptorMatches(methodDescOf(decoratorMethod), methodDesc)) {
                            include = false;
                            break;
                        }
                    }

                    if (include) {
                        abstractMethods.add(methodDesc);
                        // Bridge method is needed
                        if (resolvedMethodDesc != null) {
                            bridgeMethods.put(resolvedMethodDesc, methodDesc);
                        }
                    }
                }
            }

            for (MethodDesc abstractMethod : abstractMethods) {
                cc.method(abstractMethod, mc -> {
                    List<ParamVar> params = new ArrayList<>(abstractMethod.parameterCount());
                    for (int i = 0; i < abstractMethod.parameterCount(); i++) {
                        params.add(mc.parameter("param" + i, i));
                    }
                    mc.body(bc -> {
                        bc.return_(bc.invokeInterface(abstractMethod, cc.this_().field(delegateField), params));
                    });
                });
            }

            for (Map.Entry<MethodDesc, MethodDesc> entry : bridgeMethods.entrySet()) {
                MethodDesc bridgeMethod = entry.getKey();
                MethodDesc targetMethod = entry.getValue();
                cc.method(bridgeMethod, mc -> {
                    List<ParamVar> params = new ArrayList<>(bridgeMethod.parameterCount());
                    for (int i = 0; i < bridgeMethod.parameterCount(); i++) {
                        params.add(mc.parameter("param" + i, i));
                    }
                    mc.body(bc -> {
                        Expr result = bc.invokeVirtual(ClassMethodDesc.of(cc.type(), targetMethod.name(),
                                MethodTypeDesc.of(targetMethod.returnType(),
                                        targetMethod.parameterTypes().toArray(ClassDesc[]::new))),
                                cc.this_(), params);
                        bc.return_(bc.cast(result, bridgeMethod.returnType()));
                    });
                });
            }
        });

        return generatedImplName;
    }

    /**
     * @see InjectableDecorator#getDecoratedTypes()
     */
    protected void generateGetDecoratedTypes(io.quarkus.gizmo2.creator.ClassCreator cc, FieldDesc decoratedTypes) {
        cc.method("getDecoratedTypes", mc -> {
            mc.returning(Set.class);
            mc.body(bc -> {
                bc.return_(cc.this_().field(decoratedTypes));
            });
        });
    }

    /**
     * @see InjectableDecorator#getDelegateType()
     */
    protected void generateGetDelegateType(io.quarkus.gizmo2.creator.ClassCreator cc, FieldDesc delegateType) {
        cc.method("getDelegateType", mc -> {
            mc.returning(java.lang.reflect.Type.class);
            mc.body(bc -> {
                bc.return_(cc.this_().field(delegateType));
            });
        });
    }

    /**
     * @see InjectableDecorator#getDelegateQualifiers()
     */
    protected void generateGetDelegateQualifiers(io.quarkus.gizmo2.creator.ClassCreator cc, FieldDesc qualifiersField) {
        if (qualifiersField != null) {
            cc.method("getDelegateQualifiers", mc -> {
                mc.returning(Set.class);
                mc.body(bc -> {
                    bc.return_(cc.this_().field(qualifiersField));
                });
            });
        }
    }
}
