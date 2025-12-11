package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.fieldDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.IllegalProductException;
import jakarta.enterprise.inject.UnproxyableResolutionException;
import jakarta.enterprise.inject.literal.InjectLiteral;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.Type;
import org.jboss.jandex.gizmo2.StringBuilderGen;
import org.jboss.logging.Logger;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableDecorator;
import io.quarkus.arc.InjectableInterceptor;
import io.quarkus.arc.InjectableReferenceProvider;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.CurrentInjectionPointProvider;
import io.quarkus.arc.impl.DecoratorDelegateProvider;
import io.quarkus.arc.impl.InitializedInterceptor;
import io.quarkus.arc.impl.SyntheticCreationalContextImpl;
import io.quarkus.arc.impl.SyntheticCreationalContextImpl.TypeAndQualifiers;
import io.quarkus.arc.impl.UncaughtExceptions;
import io.quarkus.arc.processor.BeanInfo.InterceptionInfo;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.BuiltinBean.GeneratorContext;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.FieldVar;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.Reflection2Gizmo;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.creator.ModifierFlag;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 *
 * @author Martin Kouba
 */
public class BeanGenerator extends AbstractGenerator {

    static final String BEAN_SUFFIX = "_Bean";
    static final String PRODUCER_METHOD_SUFFIX = "_ProducerMethod";
    static final String PRODUCER_FIELD_SUFFIX = "_ProducerField";

    protected static final String FIELD_NAME_DECLARING_PROVIDER_SUPPLIER = "declaringProviderSupplier";
    protected static final String FIELD_NAME_BEAN_TYPES = "types";
    protected static final String FIELD_NAME_QUALIFIERS = "qualifiers";
    protected static final String FIELD_NAME_STEREOTYPES = "stereotypes";
    protected static final String FIELD_NAME_PROXY = "proxy";

    protected final AnnotationLiteralProcessor annotationLiterals;
    protected final Predicate<DotName> applicationClassPredicate;
    protected final PrivateMembersCollector privateMembers;
    protected final Set<String> existingClasses;
    protected final Map<BeanInfo, String> beanToGeneratedName;
    protected final Map<BeanInfo, String> beanToGeneratedBaseName;
    protected final Predicate<DotName> injectionPointAnnotationsPredicate;
    protected final List<Function<BeanInfo, Consumer<BlockCreator>>> suppressConditionGenerators;

    public BeanGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            PrivateMembersCollector privateMembers, boolean generateSources, ReflectionRegistration reflectionRegistration,
            Set<String> existingClasses, Map<BeanInfo, String> beanToGeneratedName,
            Predicate<DotName> injectionPointAnnotationsPredicate,
            List<Function<BeanInfo, Consumer<BlockCreator>>> suppressConditionGenerators) {
        super(generateSources, reflectionRegistration);
        this.annotationLiterals = annotationLiterals;
        this.applicationClassPredicate = applicationClassPredicate;
        this.privateMembers = privateMembers;
        this.existingClasses = existingClasses;
        this.beanToGeneratedName = beanToGeneratedName;
        this.injectionPointAnnotationsPredicate = injectionPointAnnotationsPredicate;
        this.suppressConditionGenerators = suppressConditionGenerators;
        this.beanToGeneratedBaseName = new HashMap<>();
    }

    /**
     * Precompute the generated name for the given bean so that the {@link ComponentsProviderGenerator} can be executed
     * before all beans metadata are generated.
     *
     * @param bean
     */
    void precomputeGeneratedName(BeanInfo bean) {
        if (bean.isClassBean()) {
            generateClassBeanName(bean);
        } else if (bean.isProducerMethod()) {
            generateProducerMethodBeanName(bean);
        } else if (bean.isProducerField()) {
            generateProducerFieldBeanName(bean);
        } else if (bean.isSynthetic()) {
            generateSyntheticBeanName(bean);
        }
    }

    private void generateClassBeanName(BeanInfo bean) {
        ClassInfo beanClass = bean.getTarget().get().asClass();
        String baseName = beanClass.name().withoutPackagePrefix();
        this.beanToGeneratedBaseName.put(bean, baseName);
        String targetPackage = DotNames.packagePrefix(bean.getProviderType().name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        this.beanToGeneratedName.put(bean, generatedName);
    }

    private void generateProducerMethodBeanName(BeanInfo bean) {
        MethodInfo producerMethod = bean.getTarget().get().asMethod();
        ClassInfo declaringClass = producerMethod.declaringClass();
        String declaringClassBase = declaringClass.name().withoutPackagePrefix();

        StringBuilder sigBuilder = new StringBuilder();
        sigBuilder.append(producerMethod.name())
                .append(UNDERSCORE)
                .append(producerMethod.returnType().name());
        for (Type parameterType : producerMethod.parameterTypes()) {
            sigBuilder.append(parameterType.name());
        }

        String baseName = declaringClassBase + PRODUCER_METHOD_SUFFIX + UNDERSCORE + producerMethod.name() + UNDERSCORE
                + Hashes.sha1_base64(sigBuilder.toString());
        this.beanToGeneratedBaseName.put(bean, baseName);
        String targetPackage = DotNames.packagePrefix(declaringClass.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        this.beanToGeneratedName.put(bean, generatedName);
    }

    private void generateProducerFieldBeanName(BeanInfo bean) {
        FieldInfo producerField = bean.getTarget().get().asField();
        ClassInfo declaringClass = producerField.declaringClass();
        String declaringClassBase = declaringClass.name().withoutPackagePrefix();

        String baseName = declaringClassBase + PRODUCER_FIELD_SUFFIX + UNDERSCORE + producerField.name();
        this.beanToGeneratedBaseName.put(bean, baseName);
        String targetPackage = DotNames.packagePrefix(declaringClass.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX);
        this.beanToGeneratedName.put(bean, generatedName);
    }

    private void generateSyntheticBeanName(BeanInfo bean) {
        String baseName = bean.getImplClazz().name().withoutPackagePrefix() + UNDERSCORE + bean.getIdentifier()
                + UNDERSCORE + SYNTHETIC_SUFFIX;
        this.beanToGeneratedBaseName.put(bean, baseName);
        String targetPackage = bean.getTargetPackageName();
        this.beanToGeneratedName.put(bean, generatedNameFromTarget(targetPackage, baseName, BEAN_SUFFIX));
    }

    /**
     *
     * @param bean
     * @return a collection of resources
     */
    Collection<Resource> generate(BeanInfo bean) {
        ClassInfo clazz;
        if (bean.isClassBean()) {
            clazz = bean.getTarget().get().asClass();
        } else if (bean.isProducerMethod()) {
            clazz = bean.getTarget().get().asMethod().declaringClass();
        } else if (bean.isProducerField()) {
            clazz = bean.getTarget().get().asField().declaringClass();
        } else if (bean.isSynthetic()) {
            clazz = bean.getImplClazz();
        } else {
            throw new IllegalArgumentException("Unknown bean type: " + bean);
        }

        ProviderType providerType = new ProviderType(bean.getProviderType());
        String baseName = beanToGeneratedBaseName.get(bean);
        String targetPackage = bean.isSynthetic()
                ? bean.getTargetPackageName()
                : DotNames.packagePrefix(bean.isProducer() ? clazz.name() : providerType.name());
        String generatedName = beanToGeneratedName.get(bean);
        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(clazz.name())
                || bean.isForceApplicationClass()
                || bean.hasBoundDecoratorMatching(applicationClassPredicate);
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.BEAN : null, generateSources);

        Gizmo gizmo = gizmo(classOutput);

        generateBean(gizmo, bean, generatedName, baseName, targetPackage, isApplicationClass, providerType);

        return classOutput.getResources();
    }

    private void generateBean(Gizmo gizmo, BeanInfo bean, String generatedName, String baseName,
            String targetPackage, boolean isApplicationClass, ProviderType providerType) {
        gizmo.class_(generatedName, cc -> {
            cc.implements_(InjectableBean.class);
            cc.implements_(Supplier.class);

            FieldDesc beanTypesField = cc.field(FIELD_NAME_BEAN_TYPES, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Set.class);
            });
            FieldDesc qualifiersField = null;
            if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
                qualifiersField = cc.field(FIELD_NAME_QUALIFIERS, fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Set.class);
                });
            }
            FieldDesc stereotypesField = null;
            if (!bean.getStereotypes().isEmpty()) {
                stereotypesField = cc.field(FIELD_NAME_STEREOTYPES, fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Set.class);
                });
            }
            FieldDesc declaringProviderSupplierField = null;
            if (bean.isProducer()) {
                declaringProviderSupplierField = cc.field(FIELD_NAME_DECLARING_PROVIDER_SUPPLIER, fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Supplier.class);
                });
            }
            if (bean.getScope().isNormal()) {
                // For normal scopes a client proxy is generated too
                generateProxy(cc, bean, baseName);
            }
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderSupplierField = new HashMap<>();
            Map<InterceptorInfo, FieldDesc> interceptorToProviderSupplierField = new HashMap<>();
            Map<DecoratorInfo, FieldDesc> decoratorToProviderSupplierField = new HashMap<>();
            generateProviderFields(bean, cc, injectionPointToProviderSupplierField, interceptorToProviderSupplierField,
                    decoratorToProviderSupplierField);

            generateConstructor(cc, bean, beanTypesField, qualifiersField, stereotypesField,
                    declaringProviderSupplierField, injectionPointToProviderSupplierField,
                    interceptorToProviderSupplierField, decoratorToProviderSupplierField,
                    bean.isSynthetic() ? bc -> {
                        SyntheticComponentsUtil.addParamsFieldAndInit(cc, bc, bean.getParams(), annotationLiterals,
                                bean.getDeployment().getBeanArchiveIndex());
                    } : bc -> {
                    });

            generateCreate(cc, bean, providerType, baseName, injectionPointToProviderSupplierField,
                    interceptorToProviderSupplierField, decoratorToProviderSupplierField, targetPackage,
                    isApplicationClass);
            if (bean.hasDestroyLogic()) {
                generateDestroy(cc, bean, injectionPointToProviderSupplierField, isApplicationClass,
                        baseName, targetPackage);
            }
            generateSupplierGet(cc);
            generateInjectableReferenceProviderGet(bean, cc, baseName);
            generateGetIdentifier(cc, bean);
            generateGetTypes(beanTypesField, cc);
            if (!BuiltinScope.isDefault(bean.getScope())) {
                generateGetScope(cc, bean);
            }
            if (qualifiersField != null) {
                generateGetQualifiers(cc, qualifiersField);
            }
            generateIsAlternative(cc, bean);
            generateGetPriority(cc, bean);
            if (bean.isProducer()) {
                generateGetDeclaringBean(cc, declaringProviderSupplierField);
            }
            if (stereotypesField != null) {
                generateGetStereotypes(cc, stereotypesField);
            }
            generateGetBeanClass(cc, bean);
            if (bean.isProducer() || bean.isSynthetic()) {
                generateGetImplementationClass(cc, bean);
            }
            generateGetName(cc, bean);
            if (bean.isDefaultBean()) {
                generateIsDefaultBean(cc, bean);
            }
            generateGetKind(cc, bean);
            generateIsSuppressed(cc, bean);
            generateGetInjectionPoints(cc, bean);
            generateEquals(cc, bean);
            generateHashCode(cc, bean);
            generateToString(cc);
        });
    }

    private void generateProxy(ClassCreator cc, BeanInfo bean, String baseName) {
        if (bean.getDeployment().hasRuntimeDeferredUnproxyableError(bean)) {
            return;
        }

        ClassDesc proxyType = getClientProxyType(bean, baseName);

        FieldDesc proxyField = cc.field(FIELD_NAME_PROXY, fc -> {
            fc.private_();
            fc.volatile_();
            fc.setType(proxyType);
        });

        cc.method(FIELD_NAME_PROXY, mc -> {
            mc.private_();
            mc.returning(proxyType);
            mc.body(b0 -> {
                LocalVar proxy = b0.localVar("proxy", cc.this_().field(proxyField));
                // Create a new proxy instance, atomicity does not really matter here
                b0.ifNull(proxy, b1 -> {
                    b1.set(proxy, b1.new_(proxyType, Const.of(bean.getIdentifier())));
                    b1.set(cc.this_().field(proxyField), proxy);
                });
                b0.return_(proxy);
            });
        });
    }

    protected void generateProviderFields(BeanInfo bean, ClassCreator cc,
            Map<InjectionPointInfo, FieldDesc> injectionPointToProvider,
            Map<InterceptorInfo, FieldDesc> interceptorToProvider,
            Map<DecoratorInfo, FieldDesc> decoratorToProvider) {

        int providerIdx = 1;
        // Injection points
        for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
            FieldDesc field = cc.field("injectProviderSupplier" + providerIdx++, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Supplier.class);
            });
            injectionPointToProvider.put(injectionPoint, field);
        }
        if (bean.getDisposer() != null) {
            for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                FieldDesc field = cc.field("disposerProviderSupplier" + providerIdx++, fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Supplier.class);
                });
                injectionPointToProvider.put(injectionPoint, field);
            }
        }
        // Interceptors
        for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
            FieldDesc field = cc.field("interceptorProviderSupplier" + providerIdx++, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Supplier.class);
            });
            interceptorToProvider.put(interceptor, field);
        }
        // Decorators
        for (DecoratorInfo decorator : bean.getBoundDecorators()) {
            FieldDesc field = cc.field("decoratorProviderSupplier" + providerIdx++, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Supplier.class);
            });
            decoratorToProvider.put(decorator, field);
        }
    }

    protected void generateConstructor(ClassCreator cc, BeanInfo bean,
            FieldDesc beanTypesField,
            FieldDesc qualifiersField,
            FieldDesc stereotypesField,
            FieldDesc declaringProviderSupplierField,
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderField,
            Map<InterceptorInfo, FieldDesc> interceptorToProviderField,
            Map<DecoratorInfo, FieldDesc> decoratorToProviderSupplierField,
            Consumer<BlockCreator> additionalCode) {

        cc.constructor(mc -> {
            List<ParamVar> params = new ArrayList<>();
            if (bean.isProducer()) {
                params.add(mc.parameter("declaringBean", Reflection2Gizmo.classDescOf(Supplier.class)));
            }
            int ipIdx = 0;
            for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
                if (!injectionPoint.isDelegate() && BuiltinBean.resolve(injectionPoint) == null) {
                    params.add(mc.parameter("injectionPoint" + (ipIdx++), Reflection2Gizmo.classDescOf(Supplier.class)));
                }
            }
            if (bean.getDisposer() != null) {
                ipIdx = 0;
                for (InjectionPointInfo injectionPoint : bean.getDisposer().getInjection().injectionPoints) {
                    if (BuiltinBean.resolve(injectionPoint) == null) {
                        params.add(mc.parameter("disposerInjectionPoint" + (ipIdx++), Supplier.class));
                    }
                }
            }
            for (int i = 0; i < interceptorToProviderField.size(); i++) {
                params.add(mc.parameter("interceptorProvider" + i, Supplier.class));
            }
            for (int i = 0; i < decoratorToProviderSupplierField.size(); i++) {
                params.add(mc.parameter("decoratorProvider" + i, Supplier.class));
            }
            mc.body(bc -> {
                // Invoke super()
                bc.invokeSpecial(MethodDescs.OBJECT_CONSTRUCTOR, cc.this_());

                LocalVar tccl = bc.localVar("tccl", bc.invokeVirtual(MethodDescs.THREAD_GET_TCCL, bc.currentThread()));

                // Bean types
                RuntimeTypeCreator rttc = RuntimeTypeCreator.of(bc).withTCCL(tccl);
                Expr typesArray = bc.newArray(Object.class, bean.getTypes()
                        .stream()
                        .map(type -> {
                            try {
                                return rttc.create(type);
                            } catch (IllegalArgumentException e) {
                                throw new IllegalStateException("Unable to construct type for " + bean + ": " + e.getMessage());
                            }
                        })
                        .toList());
                bc.set(cc.this_().field(beanTypesField), bc.invokeStatic(MethodDescs.SETS_OF, typesArray));

                // Qualifiers
                if (!bean.getQualifiers().isEmpty() && !bean.hasDefaultQualifiers()) {
                    Expr qualifiersArray = bc.newArray(Object.class, bean.getQualifiers()
                            .stream()
                            .map(qualifier -> {
                                BuiltinQualifier builtinQualifier = BuiltinQualifier.of(qualifier);
                                if (builtinQualifier != null) {
                                    return builtinQualifier.getLiteralInstance();
                                } else {
                                    ClassInfo qualifierClass = bean.getDeployment().getQualifier(qualifier.name());
                                    return annotationLiterals.create(bc, qualifierClass, qualifier);
                                }
                            })
                            .toList());
                    bc.set(cc.this_().field(qualifiersField), bc.invokeStatic(MethodDescs.SETS_OF, qualifiersArray));
                }

                // Stereotypes
                if (!bean.getStereotypes().isEmpty()) {
                    Expr stereotypesArray = bc.newArray(Object.class, bean.getStereotypes()
                            .stream()
                            .map(stereotype -> Const.of(classDescOf(stereotype.getTarget())))
                            .toList());
                    bc.set(cc.this_().field(stereotypesField), bc.invokeStatic(MethodDescs.SETS_OF, stereotypesArray));
                }

                int paramIdx = 0;
                // Declaring bean provider
                if (bean.isProducer()) {
                    bc.set(cc.this_().field(declaringProviderSupplierField), params.get(paramIdx++));
                }
                // Injection points
                List<InjectionPointInfo> allInjectionPoints = new ArrayList<>(bean.getAllInjectionPoints());
                if (bean.getDisposer() != null) {
                    allInjectionPoints.addAll(bean.getDisposer().getInjection().injectionPoints);
                }
                for (InjectionPointInfo injectionPoint : allInjectionPoints) {
                    if (injectionPoint.isDelegate()) {
                        // this.delegateProvider = () -> new DecoratorDelegateProvider();
                        Expr delegateProvider = bc.new_(DecoratorDelegateProvider.class);
                        Expr delegateProviderSupplier = bc.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR,
                                delegateProvider);
                        bc.set(cc.this_().field(injectionPointToProviderField.get(injectionPoint)), delegateProviderSupplier);
                    } else if (injectionPoint.getResolvedBean() == null) {
                        BuiltinBean builtinBean = BuiltinBean.resolve(injectionPoint);
                        builtinBean.getGenerator().generate(new GeneratorContext(bean.getDeployment(), bean, injectionPoint,
                                cc, bc, injectionPointToProviderField.get(injectionPoint), annotationLiterals,
                                reflectionRegistration, injectionPointAnnotationsPredicate, null));
                    } else if (injectionPoint.isCurrentInjectionPointWrapperNeeded()) {
                        Expr wrap = wrapCurrentInjectionPoint(bean, bc, injectionPoint, cc.this_(),
                                params.get(paramIdx++), tccl, annotationLiterals, reflectionRegistration,
                                injectionPointAnnotationsPredicate);
                        Expr wrapSupplier = bc.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, wrap);
                        bc.set(cc.this_().field(injectionPointToProviderField.get(injectionPoint)), wrapSupplier);
                    } else {
                        bc.set(cc.this_().field(injectionPointToProviderField.get(injectionPoint)), params.get(paramIdx++));
                    }
                }
                // Interceptors
                for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                    bc.set(cc.this_().field(interceptorToProviderField.get(interceptor)), params.get(paramIdx++));
                }
                // Decorators
                for (DecoratorInfo decorator : bean.getBoundDecorators()) {
                    bc.set(cc.this_().field(decoratorToProviderSupplierField.get(decorator)), params.get(paramIdx++));
                }

                additionalCode.accept(bc);

                bc.return_();
            });
        });
    }

    protected void generateCreate(ClassCreator cc, BeanInfo bean, ProviderType providerType,
            String baseName, Map<InjectionPointInfo, FieldDesc> injectionPointToProviderSupplierField,
            Map<InterceptorInfo, FieldDesc> interceptorToProviderSupplierField,
            Map<DecoratorInfo, FieldDesc> decoratorToProviderSupplierField,
            String targetPackage, boolean isApplicationClass) {

        MethodDesc createDesc = cc.method("create", mc -> {
            mc.returning(classDescOf(bean.getProviderType()));
            ParamVar ccParam = mc.parameter("creationalContext", CreationalContext.class);
            mc.body(b0 -> {
                b0.try_(tc -> {
                    tc.body(b1 -> {
                        if (bean.isClassBean()) {
                            generateCreateForClassBean(cc, bean, providerType, baseName,
                                    injectionPointToProviderSupplierField, interceptorToProviderSupplierField,
                                    decoratorToProviderSupplierField, targetPackage, isApplicationClass, b1, ccParam);
                        } else if (bean.isProducerMethod()) {
                            generateCreateForProducerMethod(cc, bean, injectionPointToProviderSupplierField,
                                    isApplicationClass, b1, ccParam);
                        } else if (bean.isProducerField()) {
                            generateCreateForProducerField(cc, bean, isApplicationClass, b1);
                        } else if (bean.isSynthetic()) {
                            generateCreateForSyntheticBean(cc, bean, injectionPointToProviderSupplierField, b1, ccParam);
                        }
                    });
                    tc.catch_(Exception.class, "e", (b1, e) -> {
                        // `Reflections.newInstance()` throws `CreationException` on its own,
                        // but that's handled like all other `RuntimeException`s
                        // also ignore custom Throwables, they are virtually never used in practice
                        b1.ifInstanceOf(e, RuntimeException.class, BlockCreator::throw_);
                        b1.throw_(b1.new_(ConstructorDesc.of(CreationException.class, Throwable.class), e));
                    });
                });
            });
        });

        if (!ClassType.OBJECT_TYPE.equals(bean.getProviderType())) {
            // Bridge method needed
            cc.method("create", mc -> {
                mc.addFlag(ModifierFlag.BRIDGE);
                mc.returning(Object.class);
                ParamVar ccParam = mc.parameter("creationalContext", CreationalContext.class);
                mc.body(bc -> {
                    bc.return_(bc.invokeVirtual(createDesc, cc.this_(), ccParam));
                });
            });
        }
    }

    private void generateCreateForClassBean(ClassCreator cc, BeanInfo bean,
            ProviderType providerType, String baseName,
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderSupplierField,
            Map<InterceptorInfo, FieldDesc> interceptorToProviderSupplierField,
            Map<DecoratorInfo, FieldDesc> decoratorToProviderSupplierField,
            String targetPackage, boolean isApplicationClass, BlockCreator bc, ParamVar parentCC) {

        // Note that we must share the interceptors instances with the intercepted subclass, if present
        InterceptionInfo postConstructInterception = bean.getLifecycleInterceptors(InterceptionType.POST_CONSTRUCT);
        InterceptionInfo aroundConstructInterception = bean.getLifecycleInterceptors(InterceptionType.AROUND_CONSTRUCT);

        LocalVar aroundConstructs = null;
        LocalVar postConstructs = null;
        Map<InterceptorInfo, LocalVar> interceptorToWrap = new HashMap<>();

        if (bean.hasLifecycleInterceptors()) {
            // Wrap InjectableInterceptors using InitializedInterceptor
            Set<InterceptorInfo> wraps = new HashSet<>();
            wraps.addAll(aroundConstructInterception.interceptors);
            wraps.addAll(postConstructInterception.interceptors);

            // instances of around/post construct interceptors also need to be shared
            // build a map that links `InterceptorInfo` to `LocalVar` and reuse that when creating wrappers
            Map<InterceptorInfo, LocalVar> interceptorVars = new HashMap<>();

            for (InterceptorInfo interceptor : wraps) {
                Expr interceptorProviderSupplier = cc.this_().field(interceptorToProviderSupplierField.get(interceptor));
                LocalVar interceptorProvider = bc.localVar("interceptorProvider", bc.invokeInterface(
                        MethodDescs.SUPPLIER_GET, interceptorProviderSupplier));
                Expr childCC = bc.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD, parentCC);
                LocalVar interceptorInstance = bc.localVar("interceptorInstance",
                        bc.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, interceptorProvider, childCC));
                interceptorVars.put(interceptor, interceptorInstance);
                LocalVar wrap = bc.localVar("wrap",
                        bc.invokeStatic(MethodDesc.of(InitializedInterceptor.class, "of",
                                InitializedInterceptor.class, Object.class, InjectableInterceptor.class),
                                interceptorInstance, interceptorProvider));
                interceptorToWrap.put(interceptor, wrap);
            }

            if (!aroundConstructInterception.isEmpty()) {
                // aroundConstructs = new ArrayList<InterceptorInvocation>()
                aroundConstructs = bc.localVar("aroundConstructs", bc.new_(ArrayList.class));
                for (InterceptorInfo interceptor : aroundConstructInterception.interceptors) {
                    // aroundConstructs.add(InterceptorInvocation.aroundConstruct(interceptor,interceptor.get(CreationalContextImpl.child(ctx))))
                    Expr interceptorSupplier = cc.this_().field(interceptorToProviderSupplierField.get(interceptor));
                    Expr interceptorInstance = bc.invokeInterface(MethodDescs.SUPPLIER_GET, interceptorSupplier);
                    Expr interceptorInvocationHandle = bc.invokeStatic(MethodDescs.INTERCEPTOR_INVOCATION_AROUND_CONSTRUCT,
                            interceptorInstance, interceptorVars.get(interceptor));
                    bc.withList(aroundConstructs).add(interceptorInvocationHandle);
                }
            }

            if (!postConstructInterception.isEmpty()) {
                // postConstructs = new ArrayList<InterceptorInvocation>()
                postConstructs = bc.localVar("postConstructs", bc.new_(ArrayList.class));
                for (InterceptorInfo interceptor : postConstructInterception.interceptors) {
                    // postConstructs.add(InterceptorInvocation.postConstruct(interceptor,interceptor.get(CreationalContextImpl.child(ctx))))
                    FieldVar interceptorSupplier = cc.this_().field(interceptorToProviderSupplierField.get(interceptor));
                    Expr interceptorInstance = bc.invokeInterface(MethodDescs.SUPPLIER_GET, interceptorSupplier);
                    Expr interceptorInvocation = bc.invokeStatic(MethodDescs.INTERCEPTOR_INVOCATION_POST_CONSTRUCT,
                            interceptorInstance, interceptorVars.get(interceptor));
                    bc.withList(postConstructs).add(interceptorInvocation);
                }
            }
        }

        LocalVar instance;

        // AroundConstruct lifecycle callback interceptors
        if (!aroundConstructInterception.isEmpty()) {
            Optional<Injection> constructorInjection = bean.getConstructorInjection();
            LocalVar constructor;
            if (constructorInjection.isPresent()) {
                List<ClassDesc> paramTypes = new ArrayList<>();
                for (InjectionPointInfo injectionPoint : constructorInjection.get().injectionPoints) {
                    paramTypes.add(classDescOf(injectionPoint.getType()));
                }
                constructor = bc.localVar("constructor", bc.invokeStatic(MethodDescs.REFLECTIONS_FIND_CONSTRUCTOR,
                        Const.of(providerType.classDesc()),
                        bc.newArray(Class.class, paramTypes.stream().map(Const::of).toList())));
                reflectionRegistration.registerMethod(constructorInjection.get().target.asMethod());
            } else {
                // constructor = Reflections.findConstructor(Foo.class)
                constructor = bc.localVar("constructor", bc.invokeStatic(MethodDescs.REFLECTIONS_FIND_CONSTRUCTOR,
                        Const.of(providerType.classDesc()), bc.newEmptyArray(Class.class, 0)));
                MethodInfo noArgsConstructor = bean.getTarget().get().asClass().method(Methods.INIT);
                reflectionRegistration.registerMethod(noArgsConstructor);
            }

            List<TransientReference> transientReferences = new ArrayList<>();
            // List of injectable parameters
            List<Var> injectableCtorParams = new ArrayList<>();
            // list of all other parameters, such as injectable interceptors
            List<Var> allOtherCtorParams = new ArrayList<>();
            newProviders(bean, cc, bc, injectionPointToProviderSupplierField, interceptorToProviderSupplierField,
                    decoratorToProviderSupplierField, interceptorToWrap, transientReferences, injectableCtorParams,
                    allOtherCtorParams, parentCC);

            // Forwarding function
            // Function<Object[], Object> forward = (params) -> new SimpleBean_Subclass(params[0], ctx, lifecycleInterceptorProvider1)
            LocalVar func = bc.localVar("func", bc.lambda(Function.class, lc -> {
                List<Var> capturedAllOtherCtorParams = new ArrayList<>();
                for (int i = 0; i < allOtherCtorParams.size(); i++) {
                    Var param = allOtherCtorParams.get(i);
                    capturedAllOtherCtorParams.add(lc.capture(param.name() + i, param));
                }
                Var capturedParentCC = lc.capture(parentCC);
                List<TransientReference> capturedTransientReferences = transientReferences.stream()
                        .map(it -> new TransientReference(
                                lc.capture(it.provider), lc.capture(it.instance), lc.capture(it.creationalContext)))
                        .toList();
                ParamVar params = lc.parameter("params", 0);
                lc.body(lbc -> {
                    List<LocalVar> args = new ArrayList<>();
                    if (!injectableCtorParams.isEmpty()) {
                        // `injectableCtorParams` are passed to the first interceptor in the chain
                        // the `Function` generated here obtains the parameter array from `InvocationContext`
                        // these 2 arrays have the same shape (size and element types), but not necessarily the same content
                        LocalVar paramsArray = lbc.localVar("params", lbc.cast(params, Object[].class));
                        for (int i = 0; i < injectableCtorParams.size(); i++) {
                            args.add(lbc.localVar("arg" + i, paramsArray.elem(i)));
                        }
                    }
                    List<Var> providers = new ArrayList<>(args);
                    providers.addAll(capturedAllOtherCtorParams);
                    LocalVar result = lbc.localVar("result", newBeanInstance(bean, lbc, providerType, baseName,
                            providers, isApplicationClass, capturedParentCC));
                    // Destroy injected transient references
                    destroyTransientReferences(lbc, capturedTransientReferences);
                    lbc.return_(result);
                });
            }));

            // Interceptor bindings
            LocalVar bindingsArray = bc.localVar("bindings",
                    bc.newEmptyArray(Object.class, aroundConstructInterception.bindings.size()));
            int bindingsIndex = 0;
            for (AnnotationInstance binding : aroundConstructInterception.bindings) {
                // Create annotation literals first
                ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.name());
                bc.set(bindingsArray.elem(bindingsIndex++), annotationLiterals.create(bc, bindingClass, binding));
            }
            // ResultHandle of Object[] holding all constructor args
            LocalVar ctorArgsArray = bc.localVar("ctorArgs", bc.newArray(Object.class, injectableCtorParams));
            LocalVar invocationContext = bc.localVar("invocationContext", bc.invokeStatic(
                    MethodDescs.INVOCATION_CONTEXTS_AROUND_CONSTRUCT, constructor, ctorArgsArray, aroundConstructs,
                    func, bc.invokeStatic(MethodDescs.SETS_OF, bindingsArray)));
            instance = bc.localVar("instance", Const.ofNull(providerType.classDesc()));
            bc.try_(tc -> {
                tc.body(b1 -> {
                    // InvocationContextImpl.aroundConstruct(constructor,aroundConstructs,forward).proceed()
                    b1.invokeInterface(MethodDescs.INVOCATION_CONTEXT_PROCEED, invocationContext);
                    // instance = InvocationContext.getTarget()
                    b1.set(instance, b1.invokeInterface(MethodDescs.INVOCATION_CONTEXT_GET_TARGET,
                            invocationContext));
                });
                tc.catch_(Exception.class, "e", (b1, e) -> {
                    b1.ifInstanceOf(e, RuntimeException.class, BlockCreator::throw_);
                    b1.throw_(b1.new_(ConstructorDesc.of(RuntimeException.class, String.class, Throwable.class),
                            Const.of("Error invoking aroundConstructs"), e));
                });
            });
        } else {
            List<TransientReference> transientReferences = new ArrayList<>();
            List<Var> providers = new ArrayList<>();
            newProviders(bean, cc, bc, injectionPointToProviderSupplierField, interceptorToProviderSupplierField,
                    decoratorToProviderSupplierField, interceptorToWrap, transientReferences, providers, providers,
                    parentCC);
            instance = bc.localVar("instance",
                    newBeanInstance(bean, bc, providerType, baseName, providers, isApplicationClass, parentCC));
            // Destroy injected transient references
            destroyTransientReferences(bc, transientReferences);
        }

        // Perform field and initializer injections
        for (Injection injection : bean.getInjections()) {
            if (injection.isField()) {
                bc.try_(tc -> {
                    tc.body(b1 -> {
                        InjectionPointInfo injectionPoint = injection.injectionPoints.get(0);
                        FieldVar providerSupplier = cc.this_().field(injectionPointToProviderSupplierField.get(injectionPoint));
                        LocalVar provider = b1.localVar("provider",
                                b1.invokeInterface(MethodDescs.SUPPLIER_GET, providerSupplier));
                        Expr childCC = b1.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD_CONTEXTUAL, provider, parentCC);
                        LocalVar injectedReference = b1.localVar("injectedReference",
                                b1.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, provider, childCC));
                        checkPrimitiveInjection(b1, injectionPoint, injectedReference);

                        FieldInfo injectedField = injection.target.asField();
                        // only use reflection fallback if we are not performing transformation
                        if (isReflectionFallbackNeeded(injectedField, targetPackage, bean)) {
                            if (Modifier.isPrivate(injectedField.flags())) {
                                privateMembers.add(isApplicationClass, String.format("@Inject field %s#%s",
                                        injection.target.asField().declaringClass().name(), injection.target.asField().name()));
                            }
                            reflectionRegistration.registerField(injectedField);
                            b1.invokeStatic(MethodDescs.REFLECTIONS_WRITE_FIELD,
                                    Const.of(classDescOf(injectedField.declaringClass())),
                                    Const.of(injectedField.name()), instance, injectedReference);

                        } else {
                            // We cannot use `injectionPoint.getRequiredType()` because it might be a resolved
                            // parameterize type and we could get `NoSuchFieldError`
                            Type ipType = injectionPoint.getAnnotationTarget().asField().type();
                            b1.set(instance.field(FieldDesc.of(classDescOf(injectedField.declaringClass()),
                                    injectedField.name(), classDescOf(ipType))), injectedReference);
                        }
                    });
                    tc.catch_(RuntimeException.class, "e", (b1, e) -> {
                        b1.throw_(b1.new_(ConstructorDesc.of(RuntimeException.class, String.class, Throwable.class),
                                Const.of("Error injecting " + injection.target), e));
                    });
                });
            } else if (injection.isMethod() && !injection.isConstructor()) {
                List<TransientReference> transientReferences = new ArrayList<>();
                LocalVar[] injectedReferences = new LocalVar[injection.injectionPoints.size()];
                int paramIdx = 0;
                for (InjectionPointInfo injectionPoint : injection.injectionPoints) {
                    FieldVar providerSupplier = cc.this_().field(injectionPointToProviderSupplierField.get(injectionPoint));
                    LocalVar provider = bc.localVar("provider",
                            bc.invokeInterface(MethodDescs.SUPPLIER_GET, providerSupplier));
                    LocalVar childCC = bc.localVar("childCC",
                            bc.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD_CONTEXTUAL, provider, parentCC));
                    LocalVar injectedReference = bc.localVar("injectedReference",
                            bc.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, provider, childCC));
                    checkPrimitiveInjection(bc, injectionPoint, injectedReference);
                    injectedReferences[paramIdx++] = injectedReference;
                    // We need to destroy dependent beans for @TransientReference injection points
                    if (injectionPoint.isDependentTransientReference()) {
                        transientReferences.add(new TransientReference(provider, injectedReference, childCC));
                    }
                }

                MethodInfo initializerMethod = injection.target.asMethod();
                if (isReflectionFallbackNeeded(initializerMethod, targetPackage)) {
                    if (Modifier.isPrivate(initializerMethod.flags())) {
                        privateMembers.add(isApplicationClass, String.format("@Inject initializer %s#%s()",
                                initializerMethod.declaringClass().name(), initializerMethod.name()));
                    }
                    reflectionRegistration.registerMethod(initializerMethod);

                    Expr paramTypes = bc.newArray(Class.class, initializerMethod.parameterTypes()
                            .stream()
                            .map(paramType -> Const.of(classDescOf(paramType)))
                            .toList());
                    Expr args = bc.newArray(Object.class, injectedReferences);
                    bc.invokeStatic(MethodDescs.REFLECTIONS_INVOKE_METHOD,
                            Const.of(classDescOf(initializerMethod.declaringClass())),
                            Const.of(initializerMethod.name()), paramTypes, instance, args);

                } else {
                    bc.invokeVirtual(methodDescOf(initializerMethod), instance, injectedReferences);
                }

                // Destroy injected transient references
                destroyTransientReferences(bc, transientReferences);
            }
        }

        if (bean.isSubclassRequired()) {
            // marking the *_Subclass instance as constructed not when its constructor finishes,
            // but only after injection is complete, to satisfy the Interceptors specification:
            //
            // > With the exception of `@AroundConstruct` lifecycle callback interceptor methods,
            // > no interceptor methods are invoked until after dependency injection has been completed
            // > on both the interceptor instances and the target.
            //
            // and also the CDI specification:
            //
            // > Invocations of initializer methods by the container are not business method invocations.
            ClassDesc subclass = ClassDesc.of(SubclassGenerator.generatedName(bean.getProviderType().name(), baseName));
            bc.invokeVirtual(ClassMethodDesc.of(subclass, SubclassGenerator.MARK_CONSTRUCTED_METHOD_NAME, void.class),
                    bc.cast(instance, subclass));
        }

        class PostConstructGenerator {
            void generate(BlockCreator bc, Var instance) {
                if (!bean.isInterceptor()) {
                    List<MethodInfo> postConstructCallbacks = Beans.getCallbacks(bean.getTarget().get().asClass(),
                            DotNames.POST_CONSTRUCT, bean.getDeployment().getBeanArchiveIndex());

                    for (MethodInfo callback : postConstructCallbacks) {
                        if (isReflectionFallbackNeeded(callback, targetPackage)) {
                            if (Modifier.isPrivate(callback.flags())) {
                                privateMembers.add(isApplicationClass, String.format("@PostConstruct callback %s#%s()",
                                        callback.declaringClass().name(), callback.name()));
                            }
                            reflectionRegistration.registerMethod(callback);
                            bc.invokeStatic(MethodDescs.REFLECTIONS_INVOKE_METHOD,
                                    Const.of(classDescOf(callback.declaringClass())),
                                    Const.of(callback.name()),
                                    bc.newEmptyArray(Class.class, 0),
                                    instance,
                                    bc.newEmptyArray(Object.class, 0));
                        } else {
                            bc.invokeVirtual(methodDescOf(callback), instance);
                        }
                    }
                }
            }
        }

        // PostConstruct lifecycle callback interceptors
        if (postConstructInterception.isEmpty()) {
            // if there's no `@PostConstruct` interceptor, we'll generate code to invoke `@PostConstruct` callbacks
            // directly into the `doCreate` method:
            //
            // private MyBean doCreate(CreationalContext var1) {
            //     MyBean var2 = new MyBean();
            //     var2.myPostConstructCallback();
            //     return var2;
            // }
            new PostConstructGenerator().generate(bc, instance);
        } else {
            // if there _is_ some `@PostConstruct` interceptor, however, we'll reify the chain of `@PostConstruct`
            // callbacks into a `Runnable` that we pass into the interceptor chain to be called
            // by the last `proceed()` call:
            //
            // private MyBean doCreate(CreationalContext var1) {
            //     ...
            //     MyBean var7 = new MyBean();
            //     // this is a `Runnable` that calls `MyBean.myPostConstructCallback()`
            //     MyBean_Bean$$function$$1 var11 = new MyBean_Bean$$function$$1(var7);
            //     ...
            //     InvocationContext var12 = InvocationContexts.postConstruct(var7, (List)var5, var10, (Runnable)var11);
            //     var12.proceed();
            //     return var7;
            // }
            LocalVar runnable = bc.localVar("runnable", bc.lambda(Runnable.class, lc -> {
                Var capturedInstance = lc.capture("instance", instance);
                lc.body(lbc -> {
                    new PostConstructGenerator().generate(lbc, capturedInstance);
                    lbc.return_();
                });
            }));

            // Interceptor bindings
            LocalVar bindingsArray = bc.localVar("bindings",
                    bc.newEmptyArray(Object.class, postConstructInterception.bindings.size()));
            int bindingsIndex = 0;
            for (AnnotationInstance binding : postConstructInterception.bindings) {
                // Create annotation literals first
                ClassInfo bindingClass = bean.getDeployment().getInterceptorBinding(binding.name());
                bc.set(bindingsArray.elem(bindingsIndex++),
                        annotationLiterals.create(bc, bindingClass, binding));
            }

            // InvocationContextImpl.postConstruct(instance,postConstructs).proceed()
            LocalVar invocationContext = bc.localVar("invocationContext",
                    bc.invokeStatic(MethodDescs.INVOCATION_CONTEXTS_POST_CONSTRUCT, instance, postConstructs,
                            bc.invokeStatic(MethodDescs.SETS_OF, bindingsArray), runnable));
            bc.try_(tc -> {
                tc.body(b1 -> {
                    b1.invokeInterface(MethodDesc.of(InvocationContext.class, "proceed", Object.class),
                            invocationContext);
                });
                tc.catch_(Exception.class, "e", (b1, e) -> {
                    b1.ifInstanceOf(e, RuntimeException.class, BlockCreator::throw_);
                    // throw new RuntimeException(e)
                    b1.throw_(b1.new_(ConstructorDesc.of(RuntimeException.class, String.class, Throwable.class),
                            Const.of("Error invoking postConstructs"), e));
                });
            });
        }

        bc.return_(instance);
    }

    private Expr newBeanInstance(BeanInfo bean, BlockCreator bc, ProviderType providerType, String baseName,
            List<Var> providers, boolean isApplicationClass, Var parentCC) {
        Optional<Injection> constructorInjection = bean.getConstructorInjection();
        MethodInfo constructor = constructorInjection.isPresent() ? constructorInjection.get().target.asMethod() : null;
        List<InjectionPointInfo> injectionPoints = constructorInjection.isPresent()
                ? constructorInjection.get().injectionPoints
                : Collections.emptyList();

        if (bean.isSubclassRequired()) {
            // new SimpleBean_Subclass(foo,ctx,lifecycleInterceptorProvider1)
            List<ClassDesc> paramTypes = new ArrayList<>();
            List<Expr> args = new ArrayList<>();

            List<InterceptorInfo> interceptors = bean.getBoundInterceptors();
            List<DecoratorInfo> decorators = bean.getBoundDecorators();

            // 1. constructor injection points
            for (int i = 0; i < injectionPoints.size(); i++) {
                paramTypes.add(classDescOf(injectionPoints.get(i).getType()));
                args.add(providers.get(i));
            }
            // 2. ctx
            paramTypes.add(Reflection2Gizmo.classDescOf(CreationalContext.class));
            args.add(parentCC);
            // 3. interceptors (wrapped if needed)
            for (int i = 0; i < interceptors.size(); i++) {
                paramTypes.add(Reflection2Gizmo.classDescOf(InjectableInterceptor.class));
                args.add(providers.get(injectionPoints.size() + i));
            }
            // 4. decorators
            for (int i = 0; i < decorators.size(); i++) {
                paramTypes.add(Reflection2Gizmo.classDescOf(InjectableDecorator.class));
                args.add(providers.get(injectionPoints.size() + interceptors.size() + i));
            }

            ClassDesc subclass = ClassDesc.of(SubclassGenerator.generatedName(bean.getProviderType().name(), baseName));
            return bc.new_(ConstructorDesc.of(subclass, paramTypes), args);
        } else if (constructorInjection.isPresent()) {
            if (Modifier.isPrivate(constructor.flags())) {
                // we often remove `private` from the constructor (see `Beans.validateBean()`),
                // so we could avoid using reflection here
                privateMembers.add(isApplicationClass, String.format("Bean constructor %s on %s",
                        constructor, constructor.declaringClass().name()));
                reflectionRegistration.registerMethod(constructor);
                int params = providers.size();
                if (DecoratorGenerator.isAbstractDecoratorImpl(bean, providerType.className())) {
                    params++;
                }
                LocalVar paramTypes = bc.localVar("paramTypes", bc.newEmptyArray(Class.class, params));
                LocalVar args = bc.localVar("args", bc.newEmptyArray(Object.class, params));
                for (int i = 0; i < injectionPoints.size(); i++) {
                    bc.set(paramTypes.elem(i), Const.of(classDescOf(injectionPoints.get(i).getType())));
                    bc.set(args.elem(i), providers.get(i));
                }
                if (DecoratorGenerator.isAbstractDecoratorImpl(bean, providerType.className())) {
                    bc.set(paramTypes.elem(params - 1), Const.of(CreationalContext.class));
                    bc.set(args.elem(params - 1), parentCC);
                }
                ClassDesc declaringClass = classDescOf(constructor.declaringClass());
                return bc.cast(bc.invokeStatic(MethodDescs.REFLECTIONS_NEW_INSTANCE,
                        Const.of(declaringClass), paramTypes, args), declaringClass);
            } else {
                // new SimpleBean(foo)
                int params = injectionPoints.size();
                if (DecoratorGenerator.isAbstractDecoratorImpl(bean, providerType.className())) {
                    params++;
                }
                List<ClassDesc> paramTypes = new ArrayList<>(params);
                for (InjectionPointInfo injectionPoint : injectionPoints) {
                    paramTypes.add(classDescOf(injectionPoint.getType()));
                }
                Expr[] args = new Expr[params];
                for (int i = 0; i < injectionPoints.size(); i++) {
                    args[i] = providers.get(i);
                }
                if (DecoratorGenerator.isAbstractDecoratorImpl(bean, providerType.className())) {
                    paramTypes.add(Reflection2Gizmo.classDescOf(CreationalContext.class));
                    args[params - 1] = parentCC;
                }
                return bc.new_(ConstructorDesc.of(providerType.classDesc(), paramTypes), args);
            }
        } else {
            MethodInfo noArgsConstructor = bean.getTarget().get().asClass().method(Methods.INIT);
            if (Modifier.isPrivate(noArgsConstructor.flags())) {
                // we often remove `private` from the constructor (see `Beans.validateBean()`),
                // so we could avoid using reflection here
                privateMembers.add(isApplicationClass, String.format("Bean constructor %s on %s",
                        noArgsConstructor, noArgsConstructor.declaringClass().name()));
                reflectionRegistration.registerMethod(noArgsConstructor);
                Expr paramTypesArray;
                Expr argsArray;
                if (DecoratorGenerator.isAbstractDecoratorImpl(bean, providerType.className())) {
                    paramTypesArray = bc.newArray(Class.class, Const.of(CreationalContext.class));
                    argsArray = bc.newArray(Object.class, parentCC);
                } else {
                    paramTypesArray = bc.newArray(Class.class);
                    argsArray = bc.newArray(Object.class);
                }
                ClassDesc declaringClass = classDescOf(noArgsConstructor.declaringClass());
                return bc.cast(bc.invokeStatic(MethodDescs.REFLECTIONS_NEW_INSTANCE,
                        Const.of(declaringClass), paramTypesArray, argsArray), declaringClass);
            } else {
                if (DecoratorGenerator.isAbstractDecoratorImpl(bean, providerType.className())) {
                    // new SimpleDecorator_Impl(ctx)
                    return bc.new_(ConstructorDesc.of(providerType.classDesc(), CreationalContext.class), parentCC);
                } else {
                    // new SimpleBean()
                    return bc.new_(ConstructorDesc.of(providerType.classDesc()));
                }
            }
        }
    }

    private void newProviders(BeanInfo bean, ClassCreator cc, BlockCreator bc,
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderField,
            Map<InterceptorInfo, FieldDesc> interceptorToProviderField,
            Map<DecoratorInfo, FieldDesc> decoratorToProviderSupplierField,
            Map<InterceptorInfo, LocalVar> interceptorToWrap,
            List<TransientReference> transientReferences,
            List<Var> injectableParams,
            List<Var> allOtherParams,
            ParamVar parentCC) {

        Optional<Injection> constructorInjection = bean.getConstructorInjection();

        if (constructorInjection.isPresent()) {
            for (InjectionPointInfo injectionPoint : constructorInjection.get().injectionPoints) {
                Expr providerSupplier = cc.this_().field(injectionPointToProviderField.get(injectionPoint));
                LocalVar provider = bc.localVar("provider",
                        bc.invokeInterface(MethodDescs.SUPPLIER_GET, providerSupplier));
                LocalVar childCC = bc.localVar("childCC",
                        bc.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD_CONTEXTUAL, provider, parentCC));
                LocalVar injectedReference = bc.localVar("injectedReference",
                        bc.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, provider, childCC));
                checkPrimitiveInjection(bc, injectionPoint, injectedReference);
                injectableParams.add(injectedReference);
                if (injectionPoint.isDependentTransientReference()) {
                    transientReferences.add(new TransientReference(provider, injectedReference, childCC));
                }
            }
        }
        if (bean.isSubclassRequired()) {
            for (InterceptorInfo interceptor : bean.getBoundInterceptors()) {
                LocalVar wrapped = interceptorToWrap.get(interceptor);
                if (wrapped != null) {
                    allOtherParams.add(wrapped);
                } else {
                    FieldVar interceptorProviderSupplier = cc.this_().field(interceptorToProviderField.get(interceptor));
                    LocalVar interceptorProvider = bc.localVar("interceptorProvider",
                            bc.invokeInterface(MethodDescs.SUPPLIER_GET, interceptorProviderSupplier));
                    allOtherParams.add(interceptorProvider);
                }
            }
            for (DecoratorInfo decorator : bean.getBoundDecorators()) {
                FieldVar decoratorProviderSupplier = cc.this_().field(decoratorToProviderSupplierField.get(decorator));
                LocalVar decoratorProvider = bc.localVar("decoratorProvider",
                        bc.invokeInterface(MethodDescs.SUPPLIER_GET, decoratorProviderSupplier));
                allOtherParams.add(decoratorProvider);
            }
        }
    }

    private void generateCreateForProducerMethod(ClassCreator cc, BeanInfo bean,
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderSupplierField, boolean isApplicationClass,
            BlockCreator b0, ParamVar parentCC) {

        MethodInfo producerMethod = bean.getTarget().get().asMethod();
        boolean isStatic = Modifier.isStatic(producerMethod.flags());

        // instance = declaringProviderSupplier.get().get(new CreationalContextImpl<>()).produce()

        FieldDesc declaringProviderSupplierField = FieldDesc.of(cc.type(), FIELD_NAME_DECLARING_PROVIDER_SUPPLIER,
                Supplier.class);
        FieldVar declaringProviderSupplier = cc.this_().field(declaringProviderSupplierField);
        LocalVar declaringProvider = b0.localVar("declaringProvider",
                b0.invokeInterface(MethodDescs.SUPPLIER_GET, declaringProviderSupplier));
        LocalVar creationalContext = b0.localVar("creationalContext",
                b0.new_(ConstructorDesc.of(CreationalContextImpl.class, Contextual.class), cc.this_()));
        LocalVar declaringProviderInstance = b0.localVar("declaringProviderInstance",
                b0.blockExpr(classDescOf(producerMethod.declaringClass()), b1 -> {
                    if (isStatic) {
                        // for `static` producers, we don't need to instantiate the bean
                        // the `null` will only be used for reflective invocation in case the producer is `private`, which is OK
                        b1.yieldNull();
                        return;
                    }
                    Expr result = b1.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, declaringProvider,
                            creationalContext);
                    if (bean.getDeclaringBean().getScope().isNormal()) {
                        b1.yield(b1.invokeInterface(MethodDescs.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE, result));
                    } else {
                        b1.yield(result);
                    }
                }));

        List<InjectionPointInfo> injectionPoints = bean.getAllInjectionPoints();
        LocalVar[] injectedReferences = new LocalVar[injectionPoints.size()];
        List<TransientReference> transientReferences = new ArrayList<>();
        int idx = 0;
        for (InjectionPointInfo injectionPoint : injectionPoints) {
            FieldVar providerSupplier = cc.this_().field(injectionPointToProviderSupplierField.get(injectionPoint));
            LocalVar provider = b0.localVar("provider" + idx,
                    b0.invokeInterface(MethodDescs.SUPPLIER_GET, providerSupplier));
            LocalVar childCC = b0.localVar("childCC" + idx,
                    b0.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD_CONTEXTUAL, provider, parentCC));
            LocalVar instance = b0.localVar("instance" + idx,
                    b0.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, provider, childCC));
            checkPrimitiveInjection(b0, injectionPoint, instance);
            injectedReferences[idx] = instance;
            if (injectionPoint.isDependentTransientReference()) {
                transientReferences.add(new TransientReference(provider, instance, childCC));
            }
            idx++;
        }
        LocalVar instance = b0.localVar("instance", b0.blockExpr(classDescOf(bean.getProviderType()), b1 -> {
            Expr result;
            if (Modifier.isPrivate(producerMethod.flags())) {
                privateMembers.add(isApplicationClass, String.format("Producer method %s#%s()",
                        producerMethod.declaringClass().name(), producerMethod.name()));
                reflectionRegistration.registerMethod(producerMethod);
                LocalVar paramTypes = b1.localVar("paramTypes", b1.newEmptyArray(Class.class, injectedReferences.length));
                LocalVar args = b1.localVar("args", b1.newEmptyArray(Object.class, injectedReferences.length));
                for (int i = 0; i < injectedReferences.length; i++) {
                    b1.set(paramTypes.elem(i), Const.of(classDescOf(producerMethod.parameterType(i))));
                    b1.set(args.elem(i), injectedReferences[i]);
                }
                result = b1.invokeStatic(MethodDescs.REFLECTIONS_INVOKE_METHOD,
                        Const.of(classDescOf(producerMethod.declaringClass())), Const.of(producerMethod.name()),
                        paramTypes, declaringProviderInstance, args);
            } else {
                result = isStatic
                        ? b1.invokeStatic(methodDescOf(producerMethod), injectedReferences)
                        : b1.invokeVirtual(methodDescOf(producerMethod), declaringProviderInstance, injectedReferences);
            }
            b1.yield(result);
        }));

        if (bean.getScope().isNormal()) {
            b0.ifNull(instance, b1 -> {
                b1.throw_(IllegalProductException.class, "Normal scoped producer method may not return null: "
                        + bean.getDeclaringBean().getImplClazz().name() + "." + producerMethod.name() + "()");
            });
        }

        // If the declaring bean is `@Dependent` and the producer is not `static`, we must destroy the instance afterwards
        if (BuiltinScope.DEPENDENT.is(bean.getDeclaringBean().getScope()) && !isStatic) {
            b0.invokeInterface(MethodDescs.INJECTABLE_BEAN_DESTROY, declaringProvider, declaringProviderInstance,
                    creationalContext);
        }

        // Destroy injected transient references
        destroyTransientReferences(b0, transientReferences);

        b0.return_(instance);
    }

    private void generateCreateForProducerField(ClassCreator cc, BeanInfo bean,
            boolean isApplicationClass, BlockCreator b0) {

        FieldInfo producerField = bean.getTarget().get().asField();
        boolean isStatic = Modifier.isStatic(producerField.flags());

        // instance = declaringProviderSupplier.get().get(new CreationalContextImpl<>()).field

        FieldDesc declaringProviderSupplierField = FieldDesc.of(cc.type(), FIELD_NAME_DECLARING_PROVIDER_SUPPLIER,
                Supplier.class);
        FieldVar declaringProviderSupplier = cc.this_().field(declaringProviderSupplierField);
        LocalVar declaringProvider = b0.localVar("declaringProvider",
                b0.invokeInterface(MethodDescs.SUPPLIER_GET, declaringProviderSupplier));
        LocalVar creationalContext = b0.localVar("creationalContext",
                b0.new_(ConstructorDesc.of(CreationalContextImpl.class, Contextual.class), cc.this_()));
        LocalVar declaringProviderInstance = b0.localVar("declaringProviderInstance",
                b0.blockExpr(classDescOf(producerField.declaringClass()), b1 -> {
                    if (isStatic) {
                        // for `static` producers, we don't need to instantiate the bean
                        // the `null` will only be used for reflective access in case the producer is `private`, which is OK
                        b1.yieldNull();
                        return;
                    }
                    Expr result = b1.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, declaringProvider,
                            creationalContext);
                    if (bean.getDeclaringBean().getScope().isNormal()) {
                        b1.yield(b1.invokeInterface(MethodDescs.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE, result));
                    } else {
                        b1.yield(result);
                    }
                }));

        LocalVar instance = b0.localVar("instance", b0.blockExpr(classDescOf(bean.getProviderType()), b1 -> {
            Expr result;
            if (Modifier.isPrivate(producerField.flags())) {
                privateMembers.add(isApplicationClass,
                        String.format("Producer field %s#%s", producerField.declaringClass().name(), producerField.name()));
                reflectionRegistration.registerField(producerField);
                result = b1.invokeStatic(MethodDescs.REFLECTIONS_READ_FIELD,
                        Const.of(classDescOf(producerField.declaringClass())), Const.of(producerField.name()),
                        declaringProviderInstance);
            } else {
                result = Modifier.isStatic(producerField.flags())
                        ? Expr.staticField(fieldDescOf(producerField))
                        : declaringProviderInstance.field(fieldDescOf(producerField));
            }
            b1.yield(result);
        }));

        if (bean.getScope().isNormal()) {
            b0.ifNull(instance, b1 -> {
                b1.throw_(IllegalProductException.class, "Normal scoped producer field may not be null: "
                        + bean.getDeclaringBean().getImplClazz().name() + "." + bean.getTarget().get().asField().name());
            });
        }

        // If the declaring bean is `@Dependent` and the producer is not `static`, we must destroy the instance afterwards
        if (BuiltinScope.DEPENDENT.is(bean.getDeclaringBean().getScope()) && !isStatic) {
            b0.invokeInterface(MethodDescs.INJECTABLE_BEAN_DESTROY, declaringProvider, declaringProviderInstance,
                    creationalContext);
        }

        b0.return_(instance);
    }

    private void generateCreateForSyntheticBean(ClassCreator cc, BeanInfo bean,
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderSupplierField, BlockCreator b0, ParamVar parentCC) {

        MethodDesc createSyntheticDesc = cc.method("createSynthetic", mc -> {
            // returning `Object` because the creation logic would otherwise have to cast explicitly
            mc.returning(Object.class);
            ParamVar synthCC = mc.parameter("synthCC", SyntheticCreationalContext.class);
            mc.body(bc -> {
                bean.getCreatorConsumer().accept(new BeanConfiguratorBase.CreateGeneration() {
                    @Override
                    public ClassCreator beanClass() {
                        return cc;
                    }

                    @Override
                    public BlockCreator createMethod() {
                        return bc;
                    }

                    @Override
                    public Var syntheticCreationalContext() {
                        return synthCC;
                    }
                });
            });
        });

        Consumer<BeanConfiguratorBase.CheckActiveGeneration> checkActiveConsumer = bean.getCheckActiveConsumer();
        if (checkActiveConsumer != null) {
            MethodDesc checkActiveDesc = cc.method("checkActive", mc -> {
                mc.returning(ActiveResult.class);
                mc.body(bc -> {
                    checkActiveConsumer.accept(new BeanConfiguratorBase.CheckActiveGeneration() {
                        @Override
                        public ClassCreator beanClass() {
                            return cc;
                        }

                        @Override
                        public BlockCreator checkActiveMethod() {
                            return bc;
                        }
                    });
                });
            });

            List<InjectionPointInfo> matchingIPs = new ArrayList<>();
            for (InjectionPointInfo injectionPoint : bean.getDeployment().getInjectionPoints()) {
                if (!injectionPoint.isSynthetic() && bean.equals(injectionPoint.getResolvedBean())) {
                    matchingIPs.add(injectionPoint);
                }
            }

            LocalVar active = b0.localVar("active", b0.invokeVirtual(checkActiveDesc, cc.this_()));
            Expr activeBool = b0.invokeVirtual(MethodDescs.ACTIVE_RESULT_VALUE, active);
            b0.ifNot(activeBool, b1 -> {
                LocalVar msg = b1.localVar("msg", b1.new_(StringBuilder.class));
                StringBuilderGen msgBuilder = StringBuilderGen.of(msg, b1)
                        .append("Bean is not active: ")
                        .append(cc.this_())
                        .append("\nReason: ")
                        .append(b1.invokeVirtual(MethodDescs.ACTIVE_RESULT_REASON, active));
                LocalVar cause = b1.localVar("cause", b1.invokeVirtual(MethodDescs.ACTIVE_RESULT_CAUSE, active));
                b1.while_(b2 -> b2.yield(b2.isNotNull(cause)), b2 -> {
                    StringBuilderGen.of(msg, b2)
                            .append("\nCause: ")
                            .append(b2.invokeVirtual(MethodDescs.ACTIVE_RESULT_REASON, cause));
                    b2.set(cause, b2.invokeVirtual(MethodDescs.ACTIVE_RESULT_CAUSE, cause));
                });
                msgBuilder.append("\nTo avoid this exception while keeping the bean inactive:")
                        .append("\n\t- Configure all extensions consuming this bean as inactive as well, if they allow it,")
                        .append(" e.g. 'quarkus.someextension.active=false'")
                        .append("\n\t- Make sure that custom code only accesses this bean if it is active");
                if (!matchingIPs.isEmpty()) {
                    Expr implClassName = Const.of(bean.getImplClazz().name().toString());
                    msgBuilder.append("\n\t- Inject the bean with 'Instance<")
                            .append(implClassName)
                            .append(">' instead of '")
                            .append(implClassName)
                            .append("'\n");
                    msgBuilder.append("This bean is injected into:");
                    for (InjectionPointInfo matchingIP : matchingIPs) {
                        msgBuilder.append("\n\t- ").append(matchingIP.getTargetInfo());
                    }
                }
                b1.throw_(InactiveBeanException.class, b1.exprToString(msg));
            });
        }

        LocalVar injectedReferences;
        if (injectionPointToProviderSupplierField.isEmpty()) {
            injectedReferences = b0.localVar("injectedReferences", b0.mapOf());
        } else {
            // Initialize injected references
            injectedReferences = b0.localVar("injectedReferences", b0.new_(HashMap.class));
            LocalVar tccl = b0.localVar("tccl", b0.invokeVirtual(MethodDescs.THREAD_GET_TCCL, b0.currentThread()));
            IndexView index = bean.getDeployment().getBeanArchiveIndex();
            for (InjectionPointInfo injectionPoint : bean.getAllInjectionPoints()) {
                b0.try_(tc -> {
                    tc.body(b1 -> {
                        LocalVar requiredType;
                        try {
                            requiredType = RuntimeTypeCreator.of(b1).withTCCL(tccl).withIndex(index)
                                    .create(injectionPoint.getType());
                        } catch (IllegalArgumentException e) {
                            throw new IllegalStateException("Unable to construct type for " + injectionPoint.getType()
                                    + ": " + e.getMessage());
                        }
                        LocalVar requiredQualifiers;
                        if (injectionPoint.hasDefaultedQualifier()) {
                            requiredQualifiers = b1.localVar("qualifiers", Const.ofNull(Annotation[].class));
                        } else {
                            requiredQualifiers = b1.localVar("qualifiers",
                                    b1.newEmptyArray(Annotation.class, injectionPoint.getRequiredQualifiers().size()));
                            int idx = 0;
                            for (AnnotationInstance qualifier : injectionPoint.getRequiredQualifiers()) {
                                BuiltinQualifier builtinQualifier = BuiltinQualifier.of(qualifier);
                                if (builtinQualifier != null) {
                                    b1.set(requiredQualifiers.elem(idx++), builtinQualifier.getLiteralInstance());
                                } else {
                                    // Create the annotation literal first
                                    ClassInfo qualifierClass = bean.getDeployment().getQualifier(qualifier.name());
                                    b1.set(requiredQualifiers.elem(idx++),
                                            annotationLiterals.create(b1, qualifierClass, qualifier));
                                }
                            }
                        }
                        LocalVar typeAndQualifiers = b1.localVar("typeAndQualifiers",
                                b1.new_(TypeAndQualifiers.class, requiredType, requiredQualifiers));
                        FieldVar providerSupplier = cc.this_().field(
                                injectionPointToProviderSupplierField.get(injectionPoint));
                        LocalVar provider = b1.localVar("provider",
                                b1.invokeInterface(MethodDescs.SUPPLIER_GET, providerSupplier));
                        LocalVar childCC = b1.localVar("childCC",
                                b1.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD_CONTEXTUAL, provider, parentCC));
                        LocalVar injectedReference = b1.localVar("injectedReference",
                                b1.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, provider, childCC));
                        checkPrimitiveInjection(b1, injectionPoint, injectedReference);
                        b1.withMap(injectedReferences).put(typeAndQualifiers, injectedReference);
                    });
                    tc.catch_(RuntimeException.class, "e", (b1, e) -> {
                        b1.throw_(b1.new_(ConstructorDesc.of(RuntimeException.class, String.class, Throwable.class),
                                Const.of("Error injecting synthetic injection point of bean: " + bean.getIdentifier()), e));
                    });
                });
            }
        }

        FieldVar params = cc.this_().field(FieldDesc.of(cc.type(), "params", Map.class));
        Expr synthCC = b0.localVar("synthCC", b0.new_(
                ConstructorDesc.of(SyntheticCreationalContextImpl.class, CreationalContext.class, Map.class, Map.class),
                parentCC, params, injectedReferences));

        LocalVar result = b0.localVar("result", Const.ofNull(classDescOf(bean.getProviderType())));
        b0.try_(tc -> {
            tc.body(b1 -> {
                b1.set(result, b1.invokeVirtual(createSyntheticDesc, cc.this_(), synthCC));
            });
            tc.catch_(Exception.class, "e", (b1, e) -> {
                Expr msg = StringBuilderGen.ofNew(b1)
                        .append("Error creating synthetic bean [")
                        .append(bean.getIdentifier())
                        .append("]: ")
                        .append(e)
                        .toString_();
                b1.throw_(b1.new_(ConstructorDesc.of(CreationException.class, String.class, Throwable.class), msg, e));
            });
        });

        if (bean.getScope().isNormal()) {
            // Normal scoped synthetic beans should never return null
            b0.ifNull(result, b1 -> {
                Expr msg = StringBuilderGen.ofNew(b1)
                        .append("Null contextual instance was produced by a normal scoped synthetic bean: ")
                        .append(cc.this_())
                        .toString_();
                b1.throw_(CreationException.class, msg);
            });
        }

        b0.return_(result);
    }

    static void checkPrimitiveInjection(BlockCreator b0, InjectionPointInfo injectionPoint, LocalVar localVar) {
        if (injectionPoint.getType().kind() == Type.Kind.PRIMITIVE) {
            Type producerType = null;
            if (injectionPoint.getResolvedBean().isProducerField()) {
                producerType = injectionPoint.getResolvedBean().getTarget().get().asField().type();
            } else if (injectionPoint.getResolvedBean().isProducerMethod()) {
                producerType = injectionPoint.getResolvedBean().getTarget().get().asMethod().returnType();
            }

            if (PrimitiveType.isBox(producerType)) {
                b0.ifNull(localVar, b1 -> {
                    // this will be auto-boxed by Gizmo
                    b1.set(localVar, Const.ofDefault(classDescOf(injectionPoint.getType())));
                });
            }
        }
    }

    protected void generateDestroy(ClassCreator cc, BeanInfo bean,
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderField, boolean isApplicationClass, String baseName,
            String targetPackage) {

        MethodDesc destroyDesc = cc.method("destroy", mc -> {
            mc.returning(void.class);
            ParamVar providerParam = mc.parameter("provider", classDescOf(bean.getProviderType()));
            ParamVar ccParam = mc.parameter("creationalContext", CreationalContext.class);
            mc.body(b0 -> {
                b0.try_(tc -> {
                    tc.body(b1 -> {
                        if (bean.isClassBean()) {
                            if (!bean.isInterceptor()) {
                                // in case someone calls `Bean.destroy()` directly (i.e., they use the low-level CDI API),
                                // they may pass us a client proxy
                                LocalVar instance = b1.localVar("instance",
                                        b1.invokeStatic(MethodDescs.CLIENT_PROXY_UNWRAP, providerParam));

                                class PreDestroyGenerator {
                                    void generate(BlockCreator bc, Var instance) {
                                        // PreDestroy callbacks
                                        // possibly wrapped into Runnable so that PreDestroy interceptors can proceed() correctly
                                        List<MethodInfo> preDestroyCallbacks = Beans.getCallbacks(
                                                bean.getTarget().get().asClass(), DotNames.PRE_DESTROY,
                                                bean.getDeployment().getBeanArchiveIndex());
                                        for (MethodInfo callback : preDestroyCallbacks) {
                                            if (isReflectionFallbackNeeded(callback, targetPackage)) {
                                                if (Modifier.isPrivate(callback.flags())) {
                                                    privateMembers.add(isApplicationClass,
                                                            String.format("@PreDestroy callback %s#%s()",
                                                                    callback.declaringClass().name(), callback.name()));
                                                }
                                                reflectionRegistration.registerMethod(callback);
                                                bc.invokeStatic(MethodDescs.REFLECTIONS_INVOKE_METHOD,
                                                        Const.of(classDescOf(callback.declaringClass())),
                                                        Const.of(callback.name()),
                                                        bc.newEmptyArray(Class.class, 0),
                                                        instance,
                                                        bc.newEmptyArray(Object.class, 0));
                                            } else {
                                                // instance.superCoolDestroyCallback()
                                                bc.invokeVirtual(methodDescOf(callback), instance);
                                            }
                                        }
                                    }
                                }

                                if (bean.getLifecycleInterceptors(InterceptionType.PRE_DESTROY).isEmpty()) {
                                    // if there's no `@PreDestroy` interceptor, we'll generate code to invoke `@PreDestroy` callbacks
                                    // directly into the `doDestroy` method:
                                    //
                                    // private void doDestroy(MyBean var1, CreationalContext var2) {
                                    //     var1.myPreDestroyCallback();
                                    //     var2.release();
                                    // }
                                    new PreDestroyGenerator().generate(b1, instance);
                                } else {
                                    ClassDesc subclass = ClassDesc.of(SubclassGenerator.generatedName(
                                            bean.getProviderType().name(), baseName));

                                    // if there _is_ some `@PreDestroy` interceptor, however, we'll reify the chain of `@PreDestroy`
                                    // callbacks into a `Runnable` that we pass into the interceptor chain to be called
                                    // by the last `proceed()` call:
                                    //
                                    // private void doDestroy(MyBean var1, CreationalContext var2) {
                                    //     // this is a `Runnable` that calls `MyBean.myPreDestroyCallback()`
                                    //     MyBean_Bean$$function$$2 var3 = new MyBean_Bean$$function$$2(var1);
                                    //     ((MyBean_Subclass)var1).arc$destroy((Runnable)var3);
                                    //     var2.release();
                                    // }
                                    Expr runnable = b1.lambda(Runnable.class, lc -> {
                                        Var capturedInstance = lc.capture(instance);
                                        lc.body(lbc -> {
                                            new PreDestroyGenerator().generate(lbc, capturedInstance);
                                            lbc.return_();
                                        });
                                    });

                                    b1.invokeVirtual(ClassMethodDesc.of(subclass, SubclassGenerator.DESTROY_METHOD_NAME,
                                            void.class, Runnable.class), instance, runnable);
                                }
                            }

                            // ctx.release()
                            b1.invokeInterface(MethodDescs.CREATIONAL_CTX_RELEASE, ccParam);
                            b1.return_();
                        } else if (bean.getDisposer() != null) {
                            // Invoke the disposer method
                            // declaringProvider.get(new CreationalContextImpl<>()).dispose()
                            MethodInfo disposerMethod = bean.getDisposer().getDisposerMethod();
                            boolean isStatic = Modifier.isStatic(disposerMethod.flags());

                            Expr declaringProviderSupplier = cc.this_().field(
                                    FieldDesc.of(cc.type(), FIELD_NAME_DECLARING_PROVIDER_SUPPLIER, Supplier.class));
                            LocalVar declaringProvider = b1.localVar("declaringProvider",
                                    b1.invokeInterface(MethodDescs.SUPPLIER_GET, declaringProviderSupplier));
                            LocalVar parentCC = b1.localVar("parentCC",
                                    b1.new_(ConstructorDesc.of(CreationalContextImpl.class, Contextual.class),
                                            Const.ofNull(Contextual.class)));
                            // for static disposers, we don't need to obtain the value
                            // `null` will only be used for reflective invocation in case the disposer is `private`, which is OK
                            LocalVar declaringProviderInstance = b1.localVar("declaringProviderInstance",
                                    Const.ofNull(Object.class));
                            if (!isStatic) {
                                b1.set(declaringProviderInstance, b1.invokeInterface(
                                        MethodDescs.INJECTABLE_REF_PROVIDER_GET, declaringProvider, parentCC));
                                if (bean.getDeclaringBean().getScope().isNormal()) {
                                    // We need to unwrap the client proxy
                                    b1.set(declaringProviderInstance, b1.invokeInterface(
                                            MethodDescs.CLIENT_PROXY_GET_CONTEXTUAL_INSTANCE,
                                            declaringProviderInstance));
                                }
                            }

                            Var[] disposerArgs = new Var[disposerMethod.parametersCount()];
                            int disposedParamPosition = bean.getDisposer().getDisposedParameter().position();
                            Iterator<InjectionPointInfo> injectionPointsIterator = bean.getDisposer()
                                    .getInjection().injectionPoints.iterator();
                            for (int i = 0; i < disposerMethod.parametersCount(); i++) {
                                if (i == disposedParamPosition) {
                                    disposerArgs[i] = providerParam;
                                } else {
                                    InjectionPointInfo injectionPoint = injectionPointsIterator.next();
                                    Expr providerSupplier = cc.this_().field(injectionPointToProviderField.get(injectionPoint));
                                    Expr provider = b1.invokeInterface(MethodDescs.SUPPLIER_GET, providerSupplier);
                                    Expr childCC = b1.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD_CONTEXTUAL,
                                            declaringProvider, parentCC);
                                    LocalVar injection = b1.localVar("injection" + i,
                                            b1.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, provider, childCC));
                                    checkPrimitiveInjection(b1, injectionPoint, injection);
                                    disposerArgs[i] = injection;
                                }
                            }

                            if (Modifier.isPrivate(disposerMethod.flags())) {
                                privateMembers.add(isApplicationClass, String.format("Disposer %s#%s",
                                        disposerMethod.declaringClass().name(), disposerMethod.name()));
                                reflectionRegistration.registerMethod(disposerMethod);
                                LocalVar paramTypesArray = b1.localVar("paramTypes",
                                        b1.newEmptyArray(Class.class, disposerArgs.length));
                                LocalVar argsArray = b1.localVar("args",
                                        b1.newEmptyArray(Object.class, disposerArgs.length));
                                for (int i = 0; i < disposerArgs.length; i++) {
                                    b1.set(paramTypesArray.elem(i), Const.of(classDescOf(disposerMethod.parameterType(i))));
                                    b1.set(argsArray.elem(i), disposerArgs[i]);
                                }
                                b1.invokeStatic(MethodDescs.REFLECTIONS_INVOKE_METHOD,
                                        Const.of(classDescOf(disposerMethod.declaringClass())),
                                        Const.of(disposerMethod.name()), paramTypesArray,
                                        declaringProviderInstance, argsArray);
                            } else {
                                if (isStatic) {
                                    b1.invokeStatic(methodDescOf(disposerMethod), disposerArgs);
                                } else {
                                    b1.invokeVirtual(methodDescOf(disposerMethod), declaringProviderInstance, disposerArgs);
                                }
                            }

                            // Destroy @Dependent instances injected into method parameters of a disposer method
                            b1.invokeInterface(MethodDescs.CREATIONAL_CTX_RELEASE, parentCC);

                            // If the declaring bean is `@Dependent` and the disposer is not `static`, we must destroy the instance afterwards
                            if (BuiltinScope.DEPENDENT.is(bean.getDisposer().getDeclaringBean().getScope()) && !isStatic) {
                                b1.invokeInterface(MethodDescs.INJECTABLE_BEAN_DESTROY, declaringProvider,
                                        declaringProviderInstance, parentCC);
                            }
                            // ctx.release()
                            b1.invokeInterface(MethodDescs.CREATIONAL_CTX_RELEASE, ccParam);
                            b1.return_();
                        } else if (bean.isSynthetic()) {
                            bean.getDestroyerConsumer().accept(new BeanConfiguratorBase.DestroyGeneration() {
                                @Override
                                public ClassCreator beanClass() {
                                    return cc;
                                }

                                @Override
                                public BlockCreator destroyMethod() {
                                    return b1;
                                }

                                @Override
                                public Var destroyedInstance() {
                                    return providerParam;
                                }

                                @Override
                                public Var creationalContext() {
                                    return ccParam;
                                }
                            });
                        }
                    });
                    tc.catch_(Throwable.class, "e", (b1, e) -> {
                        Const error = Const.of("Error occurred while destroying instance of " + bean);
                        LocalVar logger = b1.localVar("logger",
                                Expr.staticField(FieldDesc.of(UncaughtExceptions.class, "LOGGER")));
                        Expr isDebugEnabled = b1.invokeVirtual(MethodDesc.of(Logger.class, "isDebugEnabled", boolean.class),
                                logger);
                        b1.ifElse(isDebugEnabled, b2 -> {
                            b2.invokeVirtual(MethodDesc.of(Logger.class, "error", void.class, Object.class, Throwable.class),
                                    logger, error, e);
                        }, b2 -> {
                            b2.invokeVirtual(MethodDesc.of(Logger.class, "error", void.class, Object.class), logger,
                                    StringBuilderGen.ofNew(b2).append(error).append(": ").append(e).toString_());
                        });
                    });
                });
                b0.return_();
            });
        });

        if (!ClassType.OBJECT_TYPE.equals(bean.getProviderType())) {
            // Bridge method needed
            cc.method("destroy", mc -> {
                mc.addFlag(ModifierFlag.BRIDGE);
                mc.returning(void.class);
                ParamVar provider = mc.parameter("provider", Object.class);
                ParamVar creationalContext = mc.parameter("creationalContext", CreationalContext.class);
                mc.body(bc -> {
                    bc.return_(bc.invokeVirtual(destroyDesc, cc.this_(), provider, creationalContext));
                });
            });
        }
    }

    protected void generateSupplierGet(ClassCreator cc) {
        cc.method("get", mc -> {
            mc.returning(Object.class);
            mc.body(bc -> {
                bc.return_(cc.this_());
            });
        });
    }

    protected void generateInjectableReferenceProviderGet(BeanInfo bean, ClassCreator cc, String baseName) {
        ClassDesc providerType = classDescOf(bean.getProviderType());

        MethodDesc getDesc = cc.method("get", mc -> {
            mc.returning(providerType);
            ParamVar creationalContextParam = mc.parameter("creationalContext", CreationalContext.class);
            mc.body(b0 -> {
                if (bean.getDeployment().hasRuntimeDeferredUnproxyableError(bean)) {
                    b0.throw_(UnproxyableResolutionException.class, "Bean not proxyable: " + bean);
                } else if (BuiltinScope.DEPENDENT.is(bean.getScope())) {
                    // @Dependent pseudo-scope
                    // Foo instance = create(ctx)
                    LocalVar instance = b0.localVar("instance", b0.invokeVirtual(ClassMethodDesc.of(cc.type(), "create",
                            MethodTypeDesc.of(providerType, Reflection2Gizmo.classDescOf(CreationalContext.class))),
                            cc.this_(), creationalContextParam));

                    // We can optimize if:
                    // 1) class bean - has no @PreDestroy interceptor and there is no @PreDestroy callback
                    // 2) producer - there is no disposal method
                    // 3) synthetic bean - has no destruction logic
                    if (!bean.hasDestroyLogic()) {
                        // If there is no dependency in the creational context we don't have to store the instance in the CreationalContext
                        Expr hasDependentInstances = b0.invokeVirtual(MethodDescs.CREATIONAL_CTX_HAS_DEPENDENT_INSTANCES,
                                b0.cast(creationalContextParam, CreationalContextImpl.class));
                        b0.ifNot(hasDependentInstances, b1 -> {
                            b1.return_(instance);
                        });
                    }

                    // CreationalContextImpl.addDependencyToParent(this,instance,ctx)
                    b0.invokeStatic(MethodDescs.CREATIONAL_CTX_ADD_DEP_TO_PARENT, cc.this_(), instance, creationalContextParam);
                    // return instance
                    b0.return_(instance);
                } else if (bean.getScope().isNormal()) {
                    // All normal scopes
                    // return proxy()
                    b0.return_(b0.invokeVirtual(ClassMethodDesc.of(cc.type(), FIELD_NAME_PROXY,
                            MethodTypeDesc.of(getClientProxyType(bean, baseName))), cc.this_()));
                } else {
                    // All pseudo scopes other than @Dependent (incl. @Singleton)
                    // return Arc.requireContainer().getActiveContext(getScope()).get(this, new CreationalContextImpl<>(this))
                    Expr context = b0.invokeInterface(MethodDescs.ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                            b0.invokeStatic(MethodDescs.ARC_REQUIRE_CONTAINER),
                            Const.of(classDescOf(bean.getScope().getDotName())));
                    Expr creationalContext = b0.new_(ConstructorDesc.of(CreationalContextImpl.class, Contextual.class),
                            cc.this_());
                    Expr instance = b0.invokeInterface(MethodDescs.CONTEXT_GET, context, cc.this_(), creationalContext);
                    b0.return_(instance);
                }
            });
        });

        if (!ClassType.OBJECT_TYPE.equals(bean.getProviderType())) {
            // Bridge method needed
            cc.method("get", mc -> {
                mc.addFlag(ModifierFlag.BRIDGE);
                mc.returning(Object.class);
                ParamVar creationalContext = mc.parameter("creationalContext", CreationalContext.class);
                mc.body(bc -> {
                    bc.return_(bc.invokeVirtual(getDesc, cc.this_(), creationalContext));
                });
            });
        }
    }

    /**
     * @see InjectableBean#getIdentifier()
     */
    protected void generateGetIdentifier(ClassCreator cc, BeanInfo bean) {
        cc.method("getIdentifier", mc -> {
            mc.returning(String.class);
            mc.body(bc -> {
                bc.return_(Const.of(bean.getIdentifier()));
            });
        });
    }

    /**
     * @see InjectableBean#getTypes()
     */
    protected void generateGetTypes(FieldDesc typesField, ClassCreator cc) {
        cc.method("getTypes", mc -> {
            mc.returning(Set.class);
            mc.body(bc -> {
                bc.return_(cc.this_().field(typesField));
            });
        });
    }

    /**
     * @see InjectableBean#getScope()
     */
    protected void generateGetScope(ClassCreator cc, BeanInfo bean) {
        cc.method("getScope", mc -> {
            mc.returning(Class.class);
            mc.body(bc -> {
                bc.return_(Const.of(classDescOf(bean.getScope().getDotName())));
            });
        });
    }

    /**
     * @see InjectableBean#getQualifiers()
     */
    protected void generateGetQualifiers(ClassCreator cc, FieldDesc qualifiersField) {
        cc.method("getQualifiers", mc -> {
            mc.returning(Set.class);
            mc.body(bc -> {
                bc.return_(cc.this_().field(qualifiersField));
            });
        });
    }

    /**
     * @see InjectableBean#isAlternative()
     */
    protected void generateIsAlternative(ClassCreator cc, BeanInfo bean) {
        if (bean.isAlternative()) {
            cc.method("isAlternative", mc -> {
                mc.returning(boolean.class);
                mc.body(BlockCreator::returnTrue);
            });
        }
    }

    /**
     * @see InjectableBean#getPriority()
     */
    protected void generateGetPriority(ClassCreator cc, BeanInfo bean) {
        if (bean.getPriority() != null) {
            cc.method("hasPriority", mc -> {
                mc.returning(boolean.class);
                mc.body(BlockCreator::returnTrue);
            });
            cc.method("getPriority", mc -> {
                mc.returning(int.class);
                mc.body(bc -> {
                    bc.return_(bean.getPriority());
                });
            });
        }
    }

    /**
     * @see InjectableBean#getDeclaringBean()
     */
    protected void generateGetDeclaringBean(ClassCreator cc,
            FieldDesc declaringProviderSupplierField) {
        cc.method("getDeclaringBean", mc -> {
            mc.returning(InjectableBean.class);
            mc.body(bc -> {
                Expr declaringProviderSupplier = cc.this_().field(declaringProviderSupplierField);
                bc.return_(bc.invokeInterface(MethodDescs.SUPPLIER_GET, declaringProviderSupplier));
            });
        });
    }

    /**
     * @see InjectableBean#getStereotypes()
     */
    protected void generateGetStereotypes(ClassCreator cc, FieldDesc stereotypesField) {
        cc.method("getStereotypes", mc -> {
            mc.returning(Set.class);
            mc.body(bc -> {
                bc.return_(cc.this_().field(stereotypesField));
            });
        });
    }

    /**
     * @see InjectableBean#getBeanClass()
     */
    protected void generateGetBeanClass(ClassCreator cc, BeanInfo bean) {
        cc.method("getBeanClass", mc -> {
            mc.returning(Class.class);
            mc.body(bc -> {
                bc.return_(Const.of(classDescOf(bean.getBeanClass())));
            });
        });
    }

    /**
     * @see InjectableBean#getImplementationClass()
     */
    protected void generateGetImplementationClass(ClassCreator cc, BeanInfo bean) {
        cc.method("getImplementationClass", mc -> {
            mc.returning(Class.class);
            mc.body(bc -> {
                bc.return_(bean.getImplClazz() != null
                        ? Const.of(classDescOf(bean.getImplClazz()))
                        : Const.ofNull(Class.class));
            });
        });
    }

    /**
     * @see InjectableBean#getName()
     */
    protected void generateGetName(ClassCreator cc, BeanInfo bean) {
        if (bean.getName() != null) {
            cc.method("getName", mc -> {
                mc.returning(String.class);
                mc.body(bc -> {
                    bc.return_(Const.of(bean.getName()));
                });
            });
        }
    }

    /**
     * @see InjectableBean#isDefaultBean()
     */
    protected void generateIsDefaultBean(ClassCreator cc, BeanInfo bean) {
        cc.method("isDefaultBean", mc -> {
            mc.returning(boolean.class);
            mc.body(bc -> {
                bc.return_(bean.isDefaultBean());
            });
        });
    }

    /**
     * @see InjectableBean#getKind()
     */
    protected void generateGetKind(ClassCreator cc, BeanInfo bean) {
        if (bean.isClassBean()) {
            // the `default` implementation of `InjectableBean.getKind()` returns `CLASS`, so that
            // we don't need to implement the method and make the class larger in the most common case
            return;
        }

        InjectableBean.Kind kind;
        if (bean.isProducerMethod()) {
            kind = InjectableBean.Kind.PRODUCER_METHOD;
        } else if (bean.isProducerField()) {
            kind = InjectableBean.Kind.PRODUCER_FIELD;
        } else if (bean.isSynthetic()) {
            kind = InjectableBean.Kind.SYNTHETIC;
        } else {
            // `getKind()` is implemented on `InjectableInterceptor`/`InjectableDecorator`/`BuiltinBean`,
            // which are the kinds of beans not covered by this `if` chain, so this branch should never be taken
            throw new IllegalArgumentException("Unknown bean kind: " + bean);
        }
        cc.method("getKind", mc -> {
            mc.returning(InjectableBean.Kind.class);
            mc.body(bc -> {
                bc.return_(Expr.staticField(FieldDesc.of(InjectableBean.Kind.class, kind.name())));
            });
        });
    }

    /**
     * @see InjectableBean#isSuppressed()
     */
    protected void generateIsSuppressed(ClassCreator cc, BeanInfo bean) {
        cc.method("isSuppressed", mc -> {
            mc.returning(boolean.class);
            mc.body(bc -> {
                for (Function<BeanInfo, Consumer<BlockCreator>> generator : suppressConditionGenerators) {
                    Consumer<BlockCreator> condition = generator.apply(bean);
                    if (condition != null) {
                        condition.accept(bc);
                    }
                }
                bc.returnFalse();
            });
        });
    }

    /**
     * @see InjectableBean#getInjectionPoints()
     */
    protected void generateGetInjectionPoints(ClassCreator cc, BeanInfo bean) {
        // this is practically never used at runtime, but it makes the `Bean` classes bigger;
        // let's only implement `getInjectionPoints()` in strict mode, to be able to pass the TCK
        if (!bean.getDeployment().strictCompatibility) {
            return;
        }

        List<InjectionPointInfo> injectionPoints = bean.getAllInjectionPoints();
        if (injectionPoints.isEmpty()) {
            // inherit the default implementation from `InjectableBean`
            return;
        }

        cc.method("getInjectionPoints", mc -> {
            mc.returning(Set.class);
            mc.body(bc -> {
                LocalVar tccl = bc.localVar("tccl", bc.invokeVirtual(MethodDescs.THREAD_GET_TCCL, bc.currentThread()));
                LocalVar result = bc.localVar("result", bc.new_(HashSet.class));
                for (InjectionPointInfo injectionPoint : injectionPoints) {
                    LocalVar type = RuntimeTypeCreator.of(bc).withTCCL(tccl).create(injectionPoint.getType());
                    Var qualifiers = collectInjectionPointQualifiers(bean.getDeployment(), bc, injectionPoint,
                            annotationLiterals);
                    Var annotations = collectInjectionPointAnnotations(bean.getDeployment(), bc, injectionPoint,
                            annotationLiterals, injectionPointAnnotationsPredicate);
                    Var member = getJavaMember(bc, injectionPoint, reflectionRegistration);

                    Expr ip = bc.new_(MethodDescs.INJECTION_POINT_IMPL_CONSTRUCTOR, type, type, qualifiers, cc.this_(),
                            annotations, member, Const.of(injectionPoint.getPosition()),
                            Const.of(injectionPoint.isTransient()));
                    bc.withSet(result).add(ip);
                }
                bc.return_(result);
            });
        });
    }

    protected void generateEquals(ClassCreator cc, BeanInfo bean) {
        cc.method("equals", mc -> {
            mc.returning(boolean.class);
            ParamVar other = mc.parameter("other", Object.class);
            mc.body(bc -> {
                // if (this == other) {
                //    return true;
                // }
                bc.if_(bc.eq(cc.this_(), other), BlockCreator::returnTrue);
                // if (other == null) {
                //    return false;
                // }
                bc.ifNull(other, BlockCreator::returnFalse);
                // if (!(other instanceof InjectableBean)) {
                //    return false;
                // }
                bc.ifNotInstanceOf(other, InjectableBean.class, BlockCreator::returnFalse);
                // return identifier.equals(((InjectableBean) other).getIdentifier());
                Expr otherBean = bc.cast(other, InjectableBean.class);
                Expr otherIdentifier = bc.invokeInterface(MethodDescs.GET_IDENTIFIER, otherBean);
                bc.return_(bc.exprEquals(Const.of(bean.getIdentifier()), otherIdentifier));
            });
        });
    }

    protected void generateHashCode(ClassCreator cc, BeanInfo bean) {
        cc.method("hashCode", mc -> {
            mc.returning(int.class);
            mc.body(bc -> {
                bc.return_(Const.of(bean.getIdentifier().hashCode()));
            });
        });
    }

    protected void generateToString(ClassCreator cc) {
        cc.method("toString", mc -> {
            mc.returning(String.class);
            mc.body(bc -> {
                bc.return_(bc.invokeStatic(MethodDescs.BEANS_TO_STRING, cc.this_()));
            });
        });
    }

    private ClassDesc getClientProxyType(BeanInfo bean, String baseName) {
        StringBuilder proxyTypeName = new StringBuilder();
        proxyTypeName.append(bean.getClientProxyPackageName());
        if (!proxyTypeName.isEmpty()) {
            proxyTypeName.append(".");
        }
        proxyTypeName.append(baseName);
        proxyTypeName.append(ClientProxyGenerator.CLIENT_PROXY_SUFFIX);
        return ClassDesc.of(proxyTypeName.toString());
    }

    static Expr wrapCurrentInjectionPoint(BeanInfo bean, BlockCreator constructor, InjectionPointInfo injectionPoint,
            Expr beanExpr, Expr delegateSupplier, LocalVar tccl, AnnotationLiteralProcessor annotationLiterals,
            ReflectionRegistration reflectionRegistration, Predicate<DotName> injectionPointAnnotationsPredicate) {
        Var requiredQualifiers = collectInjectionPointQualifiers(bean.getDeployment(),
                constructor, injectionPoint, annotationLiterals);
        Var annotations = collectInjectionPointAnnotations(bean.getDeployment(),
                constructor, injectionPoint, annotationLiterals, injectionPointAnnotationsPredicate);
        Var javaMember = getJavaMember(constructor, injectionPoint, reflectionRegistration);

        // TODO empty IP for synthetic injections

        RuntimeTypeCreator rttc = RuntimeTypeCreator.of(constructor);
        if (tccl != null) {
            rttc = rttc.withTCCL(tccl);
        }

        return constructor.new_(ConstructorDesc.of(CurrentInjectionPointProvider.class, InjectableBean.class,
                Supplier.class, java.lang.reflect.Type.class, Set.class, Set.class, Member.class, int.class, boolean.class),
                beanExpr, delegateSupplier, rttc.create(injectionPoint.getType()),
                requiredQualifiers, annotations, javaMember, Const.of(injectionPoint.getPosition()),
                Const.of(injectionPoint.isTransient()));
    }

    public static Var getJavaMember(BlockCreator bc, InjectionPointInfo injectionPoint,
            ReflectionRegistration reflectionRegistration) {
        Expr javaMember;
        if (injectionPoint.isSynthetic()) {
            javaMember = Const.ofNull(Member.class);
        } else if (injectionPoint.isField()) {
            FieldInfo field = injectionPoint.getAnnotationTarget().asField();
            reflectionRegistration.registerField(field);
            javaMember = bc.invokeStatic(MethodDescs.REFLECTIONS_FIND_FIELD,
                    Const.of(classDescOf(field.declaringClass())), Const.of(field.name()));
        } else {
            MethodInfo method = injectionPoint.getAnnotationTarget().asMethodParameter().method();
            reflectionRegistration.registerMethod(method);
            if (method.name().equals(Methods.INIT)) {
                // Reflections.findConstructor(org.foo.SimpleBean.class,java.lang.String.class)
                Expr clazz = Const.of(classDescOf(method.declaringClass()));
                Expr paramsArray = bc.newArray(Class.class, method.parameterTypes()
                        .stream()
                        .map(paramType -> Const.of(classDescOf(paramType)))
                        .toList());
                javaMember = bc.invokeStatic(MethodDescs.REFLECTIONS_FIND_CONSTRUCTOR, clazz, paramsArray);
            } else {
                // Reflections.findMethod(org.foo.SimpleBean.class,"foo",java.lang.String.class)
                Expr clazz = Const.of(classDescOf(method.declaringClass()));
                Expr name = Const.of(method.name());
                Expr paramsArray = bc.newArray(Class.class, method.parameterTypes()
                        .stream()
                        .map(paramType -> Const.of(classDescOf(paramType)))
                        .toList());
                javaMember = bc.invokeStatic(MethodDescs.REFLECTIONS_FIND_METHOD, clazz, name, paramsArray);
            }
        }
        return bc.localVar("member", javaMember);
    }

    public static Var collectInjectionPointAnnotations(BeanDeployment beanDeployment, BlockCreator bc,
            InjectionPointInfo injectionPoint, AnnotationLiteralProcessor annotationLiterals,
            Predicate<DotName> injectionPointAnnotationsPredicate) {
        if (injectionPoint.isSynthetic()) {
            return bc.localVar("annotations", bc.setOf());
        }
        Collection<AnnotationInstance> annotations;
        if (Kind.FIELD.equals(injectionPoint.getAnnotationTarget().kind())) {
            FieldInfo field = injectionPoint.getAnnotationTarget().asField();
            annotations = beanDeployment.getAnnotations(field);
        } else {
            MethodInfo method = injectionPoint.getAnnotationTarget().asMethodParameter().method();
            annotations = Annotations.getParameterAnnotations(beanDeployment, method, injectionPoint.getPosition());
        }
        if (annotations.isEmpty()) {
            return bc.localVar("annotations", bc.setOf());
        }

        LocalVar annotationsVar = bc.localVar("annotations", bc.new_(HashSet.class));
        for (AnnotationInstance annotation : annotations) {
            if (!injectionPointAnnotationsPredicate.test(annotation.name())) {
                continue;
            }
            Expr annotationExpr;
            if (DotNames.INJECT.equals(annotation.name())) {
                annotationExpr = Expr.staticField(FieldDesc.of(InjectLiteral.class, "INSTANCE"));
            } else {
                ClassInfo annotationClass = getClassByName(beanDeployment.getBeanArchiveIndex(), annotation.name());
                if (annotationClass == null) {
                    continue;
                }
                annotationExpr = annotationLiterals.create(bc, annotationClass, annotation);
            }
            bc.withSet(annotationsVar).add(annotationExpr);
        }
        return annotationsVar;
    }

    public static Var collectInjectionPointQualifiers(BeanDeployment beanDeployment, BlockCreator bc,
            InjectionPointInfo injectionPoint, AnnotationLiteralProcessor annotationLiterals) {
        return collectQualifiers(beanDeployment, bc, annotationLiterals,
                injectionPoint.hasDefaultedQualifier() ? Collections.emptySet() : injectionPoint.getRequiredQualifiers());
    }

    public static Var collectQualifiers(BeanDeployment beanDeployment, BlockCreator bc,
            AnnotationLiteralProcessor annotationLiterals, Set<AnnotationInstance> qualifiers) {
        if (qualifiers.isEmpty()) {
            return Expr.staticField(FieldDescs.QUALIFIERS_IP_QUALIFIERS);
        } else {
            LocalVar qualifiersVar = bc.localVar("qualifiers", bc.new_(HashSet.class));
            for (AnnotationInstance qualifier : qualifiers) {
                BuiltinQualifier builtinQualifier = BuiltinQualifier.of(qualifier);
                Expr qualifierExpr;
                if (builtinQualifier != null) {
                    qualifierExpr = builtinQualifier.getLiteralInstance();
                } else {
                    // Create annotation literal if needed
                    qualifierExpr = annotationLiterals.create(bc, beanDeployment.getQualifier(qualifier.name()), qualifier);
                }
                bc.withSet(qualifiersVar).add(qualifierExpr);
            }
            return qualifiersVar;
        }
    }

    static void destroyTransientReferences(BlockCreator bc, Iterable<TransientReference> transientReferences) {
        for (TransientReference transientReference : transientReferences) {
            bc.invokeStatic(MethodDescs.INJECTABLE_REFERENCE_PROVIDERS_DESTROY, transientReference.provider,
                    transientReference.instance, transientReference.creationalContext);
        }
    }

    record TransientReference(Var provider, Var instance, Var creationalContext) {
    }

    /**
     *
     * @see InjectableReferenceProvider
     */
    static final class ProviderType {

        private final Type type;
        private final ClassDesc desc;
        private final String className;

        public ProviderType(Type type) {
            this.type = type;
            this.desc = classDescOf(type);
            this.className = type.name().toString();
        }

        ClassDesc classDesc() {
            return desc;
        }

        DotName name() {
            return type.name();
        }

        /**
         *
         * @return the class name, e.g. {@code org.acme.Foo}
         */
        String className() {
            return className;
        }
    }
}
