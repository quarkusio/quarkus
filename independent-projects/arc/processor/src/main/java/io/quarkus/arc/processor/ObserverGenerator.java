package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.Annotations.uniqueAnnotations;
import static io.quarkus.arc.processor.ClientProxyGenerator.MOCK_FIELD;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.event.Reception;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.spi.EventContext;
import jakarta.enterprise.inject.spi.ObserverMethod;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.InjectableObserverMethod;
import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.Mockable;
import io.quarkus.arc.processor.BeanProcessor.PrivateMembersCollector;
import io.quarkus.arc.processor.BuiltinBean.GeneratorContext;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.arc.processor.ResourceOutput.Resource.SpecialType;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;

/**
 *
 * @author Martin Kouba
 */
public class ObserverGenerator extends AbstractGenerator {

    static final String OBSERVER_SUFFIX = "_Observer";
    static final String OBSERVED_TYPE = "observedType";
    static final String QUALIFIERS = "qualifiers";
    static final String DECLARING_PROVIDER_SUPPLIER = "declaringProviderSupplier";

    private final Map<ObserverInfo, String> observerToGeneratedName;

    private final AnnotationLiteralProcessor annotationLiterals;
    private final Predicate<DotName> applicationClassPredicate;
    private final PrivateMembersCollector privateMembers;
    private final Set<String> existingClasses;
    private final Predicate<DotName> injectionPointAnnotationsPredicate;
    private final boolean mockable;
    private final ConcurrentMap<String, ObserverInfo> generatedClasses;

    public ObserverGenerator(AnnotationLiteralProcessor annotationLiterals, Predicate<DotName> applicationClassPredicate,
            PrivateMembersCollector privateMembers, boolean generateSources, ReflectionRegistration reflectionRegistration,
            Set<String> existingClasses, Map<ObserverInfo, String> observerToGeneratedName,
            Predicate<DotName> injectionPointAnnotationsPredicate, boolean mockable) {
        super(generateSources, reflectionRegistration);
        this.annotationLiterals = annotationLiterals;
        this.applicationClassPredicate = applicationClassPredicate;
        this.privateMembers = privateMembers;
        this.existingClasses = existingClasses;
        this.observerToGeneratedName = observerToGeneratedName;
        this.injectionPointAnnotationsPredicate = injectionPointAnnotationsPredicate;
        this.mockable = mockable;
        this.generatedClasses = new ConcurrentHashMap<>();
    }

    /**
     * Precompute the generated name for the given observer so that the {@link ComponentsProviderGenerator}
     * can be executed before all observers metadata are generated.
     */
    void precomputeGeneratedName(ObserverInfo observer) {
        // The name of the generated class differs:
        // "org.acme.Foo_Observer_fooMethod_hash" for normal observer where hash represents the signature of the observer method
        // "org.acme.Registrar_Observer_Synthetic_hash" for synthetic observer where hash represents the basic attrs of the observer
        String classBase;
        if (observer.isSynthetic()) {
            classBase = observer.getBeanClass().withoutPackagePrefix();
        } else {
            classBase = observer.getObserverMethod().declaringClass().name().withoutPackagePrefix();
        }

        StringBuilder baseNameBuilder = new StringBuilder();
        baseNameBuilder.append(classBase).append(OBSERVER_SUFFIX).append(UNDERSCORE);
        if (observer.isSynthetic()) {
            baseNameBuilder.append(SYNTHETIC_SUFFIX);
        } else {
            baseNameBuilder.append(observer.getObserverMethod().name());
        }
        baseNameBuilder.append(UNDERSCORE).append(observer.getIdentifier());
        String baseName = baseNameBuilder.toString();

        // No suffix added at the end of generated name because it's already
        // included in a baseName, e.g. Foo_Observer_fooMethod_hash

        String targetPackage;
        if (observer.isSynthetic()) {
            targetPackage = DotNames.packagePrefix(observer.getBeanClass());
        } else {
            targetPackage = DotNames.packagePrefix(observer.getObserverMethod().declaringClass().name());
        }
        String generatedName = generatedNameFromTarget(targetPackage, baseName, "");
        this.observerToGeneratedName.put(observer, generatedName);
    }

    /**
     *
     * @param observer
     * @return a collection of resources
     */
    Collection<Resource> generate(ObserverInfo observer) {
        String generatedName = observerToGeneratedName.get(observer);

        ObserverInfo generatedObserver = generatedClasses.putIfAbsent(generatedName, observer);
        if (generatedObserver != null) {
            if (observer.isSynthetic()) {
                throw new IllegalStateException("A synthetic observer with the generated class name " + generatedName
                        + " already exists for " + generatedObserver);
            } else {
                // Inherited observer methods share the same generated class
                return Collections.emptyList();
            }
        }

        if (existingClasses.contains(generatedName)) {
            return Collections.emptyList();
        }

        boolean isApplicationClass = applicationClassPredicate.test(observer.getBeanClass())
                || observer.isForceApplicationClass();
        ResourceClassOutput classOutput = new ResourceClassOutput(isApplicationClass,
                name -> name.equals(generatedName) ? SpecialType.OBSERVER : null, generateSources);

        Gizmo gizmo = gizmo(classOutput);

        createObserver(gizmo, observer, generatedName, isApplicationClass);

        return classOutput.getResources();
    }

    private void createObserver(Gizmo gizmo, ObserverInfo observer, String generatedName, boolean isApplicationClass) {
        // Foo_Observer_fooMethod_hash implements ObserverMethod<T>
        gizmo.class_(generatedName, cc -> {
            cc.implements_(InjectableObserverMethod.class);
            if (mockable) {
                // Observers declared on mocked beans can be disabled during tests
                cc.implements_(Mockable.class);
            }

            FieldDesc observedType = cc.field(OBSERVED_TYPE, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Type.class);
            });
            FieldDesc observedQualifiers = null;
            if (!observer.getQualifiers().isEmpty()) {
                observedQualifiers = cc.field(QUALIFIERS, fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Set.class);
                });
            }
            FieldDesc mock = null;
            if (mockable) {
                mock = cc.field(MOCK_FIELD, fc -> {
                    fc.private_();
                    fc.volatile_();
                    fc.setType(boolean.class);
                });
            }
            // Declaring bean provider
            FieldDesc declaringProvider = cc.field(DECLARING_PROVIDER_SUPPLIER, fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Supplier.class);
            });
            // Injection points
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderField = new HashMap<>();
            if (!observer.isSynthetic()) {
                int providerIdx = 1;
                for (InjectionPointInfo injectionPoint : observer.getInjection().injectionPoints) {
                    if (injectionPoint.getRequiredType().name().equals(DotNames.EVENT_METADATA)) {
                        // We do not need a provider for event metadata
                        continue;
                    }
                    FieldDesc desc = cc.field("observerProviderSupplier" + providerIdx++, fc -> {
                        fc.private_();
                        fc.final_();
                        fc.setType(Supplier.class);
                    });
                    injectionPointToProviderField.put(injectionPoint, desc);
                }
            }

            generateConstructor(cc, observer, observedType, observedQualifiers, mock, declaringProvider,
                    injectionPointToProviderField, annotationLiterals, reflectionRegistration);

            generateGetObservedType(cc, observedType);
            if (observedQualifiers != null) {
                generateGetObservedQualifiers(cc, observedQualifiers);
            }
            generateGetBeanClass(cc, observer.getBeanClass());
            if (!observer.getTransactionPhase().equals(TransactionPhase.IN_PROGRESS)) {
                generateGetTransactionPhase(cc, observer);
            }
            if (observer.getReception() == Reception.IF_EXISTS) {
                generateGetReceptionIfExists(cc);
            }
            if (observer.getPriority() != ObserverMethod.DEFAULT_PRIORITY) {
                generateGetPriority(cc, observer);
            }
            if (observer.isAsync()) {
                generateIsAsync(cc);
            }
            generateGetDeclaringBeanIdentifier(cc, observer.getDeclaringBean());
            if (mock != null) {
                generateMockMethods(cc, mock);
            }
            generateToString(cc, observer);

            generateNotify(cc, observer, injectionPointToProviderField, reflectionRegistration, isApplicationClass,
                    declaringProvider, mock);
        });
    }

    private void generateConstructor(ClassCreator cc, ObserverInfo observer,
            FieldDesc observedTypeField, FieldDesc observedQualifiersField, FieldDesc mockField,
            FieldDesc declaringProviderField, Map<InjectionPointInfo, FieldDesc> injectionPointToProviderField,
            AnnotationLiteralProcessor annotationLiterals, ReflectionRegistration reflectionRegistration) {

        cc.constructor(mc -> {
            ParamVar declaringProviderSupplier;
            Map<InjectionPointInfo, ParamVar> injectionPointSuppliers = new HashMap<>();
            if (!observer.isSynthetic()) {
                declaringProviderSupplier = mc.parameter(DECLARING_PROVIDER_SUPPLIER, Supplier.class);

                for (InjectionPointInfo injectionPoint : observer.getInjection().injectionPoints) {
                    if (BuiltinBean.resolve(injectionPoint) == null) {
                        FieldDesc field = injectionPointToProviderField.get(injectionPoint);
                        assert field != null;
                        injectionPointSuppliers.put(injectionPoint, mc.parameter(field.name(), Supplier.class));
                    }
                }
            } else {
                declaringProviderSupplier = null;
            }

            mc.body(bc -> {
                // super()
                bc.invokeSpecial(MethodDescs.OBJECT_CONSTRUCTOR, cc.this_());

                if (observer.isSynthetic()) {
                    SyntheticComponentsUtil.addParamsFieldAndInit(cc, bc, observer.getParams(), annotationLiterals,
                            observer.getBeanDeployment().getBeanArchiveIndex());
                } else {
                    bc.set(cc.this_().field(declaringProviderField), declaringProviderSupplier);

                    for (InjectionPointInfo injectionPoint : observer.getInjection().injectionPoints) {
                        ParamVar injectionPointParam = injectionPointSuppliers.get(injectionPoint);
                        if (injectionPointParam == null) {
                            assert injectionPoint.getResolvedBean() == null;
                            BuiltinBean builtinBean = BuiltinBean.resolve(injectionPoint);
                            assert builtinBean != null;
                            FieldDesc providerField = injectionPointToProviderField.get(injectionPoint);
                            builtinBean.getGenerator().generate(new GeneratorContext(
                                    observer.getDeclaringBean().getDeployment(), observer, injectionPoint, cc, bc,
                                    providerField, annotationLiterals, reflectionRegistration,
                                    injectionPointAnnotationsPredicate, declaringProviderSupplier));
                        } else {
                            Expr supplier;
                            if (injectionPoint.isCurrentInjectionPointWrapperNeeded()) {
                                // Wrap the constructor arg in a Supplier so we can pass it to CurrentInjectionPointProvider ctor.
                                Expr delegateSupplier = bc.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR,
                                        injectionPointParam);
                                Expr wrap = BeanGenerator.wrapCurrentInjectionPoint(observer.getDeclaringBean(),
                                        bc, injectionPoint, Const.ofNull(Object.class), delegateSupplier, null,
                                        annotationLiterals, reflectionRegistration, injectionPointAnnotationsPredicate);
                                supplier = bc.new_(MethodDescs.FIXED_VALUE_SUPPLIER_CONSTRUCTOR, wrap);
                            } else {
                                supplier = injectionPointParam;
                            }
                            bc.set(cc.this_().field(injectionPointToProviderField.get(injectionPoint)), supplier);
                        }
                    }
                }

                bc.set(cc.this_().field(observedTypeField), RuntimeTypeCreator.of(bc).create(observer.getObservedType()));

                if (observedQualifiersField != null) {
                    Expr set = bc.setOf(uniqueAnnotations(observer.getQualifiers()), qualifier -> {
                        BuiltinQualifier builtin = BuiltinQualifier.of(qualifier);
                        if (builtin != null) {
                            return builtin.getLiteralInstance();
                        } else {
                            ClassInfo qualifierClass = observer.getBeanDeployment().getQualifier(qualifier.name());
                            return annotationLiterals.create(bc, qualifierClass, qualifier);
                        }
                    });
                    bc.set(cc.this_().field(observedQualifiersField), set);
                }

                if (mockField != null) {
                    bc.set(cc.this_().field(mockField), Const.of(false));
                }

                bc.return_();
            });
        });
    }

    private void generateGetObservedType(ClassCreator cc, FieldDesc observedTypeField) {
        cc.method("getObservedType", mc -> {
            mc.returning(Type.class);
            mc.body(bc -> {
                bc.return_(cc.this_().field(observedTypeField));
            });
        });
    }

    private void generateGetObservedQualifiers(ClassCreator cc, FieldDesc observedQualifiersField) {
        cc.method("getObservedQualifiers", mc -> {
            mc.returning(Set.class);
            mc.body(bc -> {
                bc.return_(cc.this_().field(observedQualifiersField));
            });
        });
    }

    private void generateGetBeanClass(ClassCreator cc, DotName beanClass) {
        cc.method("getBeanClass", mc -> {
            mc.returning(Class.class);
            mc.body(bc -> {
                bc.return_(Const.of(classDescOf(beanClass)));
            });
        });
    }

    private void generateGetTransactionPhase(ClassCreator cc, ObserverInfo observer) {
        cc.method("getTransactionPhase", mc -> {
            mc.returning(TransactionPhase.class);
            mc.body(bc -> {
                bc.return_(Const.of(observer.getTransactionPhase()));
            });
        });
    }

    private void generateGetReceptionIfExists(ClassCreator cc) {
        cc.method("getReception", mc -> {
            mc.returning(Reception.class);
            mc.body(bc -> {
                bc.return_(Const.of(Reception.IF_EXISTS));
            });
        });
    }

    private void generateGetPriority(ClassCreator cc, ObserverInfo observer) {
        cc.method("getPriority", mc -> {
            mc.returning(int.class);
            mc.body(bc -> {
                bc.return_(Const.of(observer.getPriority()));
            });
        });
    }

    private void generateIsAsync(ClassCreator cc) {
        cc.method("isAsync", mc -> {
            mc.returning(boolean.class);
            mc.body(bc -> {
                bc.return_(Const.of(true));
            });
        });
    }

    private void generateGetDeclaringBeanIdentifier(ClassCreator cc, BeanInfo declaringBean) {
        cc.method("getDeclaringBeanIdentifier", mc -> {
            mc.returning(String.class);
            mc.body(bc -> {
                bc.return_(declaringBean != null ? Const.of(declaringBean.getIdentifier()) : Const.ofNull(String.class));
            });
        });
    }

    private void generateMockMethods(ClassCreator cc, FieldDesc mockField) {
        cc.method(ClientProxyGenerator.CLEAR_MOCK_METHOD_NAME, mc -> {
            mc.returning(void.class);
            mc.body(bc -> {
                bc.set(cc.this_().field(mockField), Const.of(false));
                bc.return_();
            });
        });

        cc.method(ClientProxyGenerator.SET_MOCK_METHOD_NAME, mc -> {
            mc.returning(void.class);
            ParamVar mock = mc.parameter("ignored", Object.class);
            mc.body(bc -> {
                bc.set(cc.this_().field(mockField), Const.of(true));
                bc.return_();
            });
        });
    }

    private void generateToString(ClassCreator cc, ObserverInfo observer) {
        cc.method("toString", mc -> {
            mc.returning(String.class);
            mc.body(bc -> {
                StringBuilder result = new StringBuilder();
                if (observer.isSynthetic()) {
                    result.append("Synthetic observer [");
                    if (observer.getUserId() != null) {
                        result.append("id=").append(observer.getUserId());
                    } else {
                        result.append("beanClass=").append(observer.getBeanClass());
                    }
                    result.append("]");
                } else {
                    result.append("Observer [method=")
                            .append(observer.getObserverMethod().declaringClass().name())
                            .append("#")
                            .append(observer.getObserverMethod().name())
                            .append("(")
                            .append(observer.getObserverMethod().parameterTypes().stream().map(Object::toString)
                                    .collect(Collectors.joining(", ")))
                            .append(")]");

                }
                bc.return_(Const.of(result.toString()));
            });
        });
    }

    private void generateNotify(ClassCreator cc, ObserverInfo observer,
            Map<InjectionPointInfo, FieldDesc> injectionPointToProviderField,
            ReflectionRegistration reflectionRegistration, boolean isApplicationClass,
            FieldDesc declaringProviderField,
            FieldDesc mockField) {

        cc.method("notify", mc -> {
            mc.returning(void.class);
            ParamVar eventContext = mc.parameter("eventContext", EventContext.class);
            mc.body(b0 -> {
                if (mockField != null) {
                    // if mockable and mocked then just return from the method
                    b0.if_(cc.this_().field(mockField), BlockCreator::return_);
                }

                if (observer.isSynthetic()) {
                    // synthetic observers generate the `notify` method themselves
                    observer.getNotify().accept(new ObserverConfigurator.NotifyGeneration() {
                        @Override
                        public ClassCreator observerClass() {
                            return cc;
                        }

                        @Override
                        public ParamVar eventContext() {
                            return eventContext;
                        }

                        @Override
                        public BlockCreator notifyMethod() {
                            return b0;
                        }
                    });
                    return;
                }

                boolean isStatic = Modifier.isStatic(observer.getObserverMethod().flags());
                // It is safe to skip creating/releasing `CreationalContext` for observers that don't inject anything
                boolean hasDependencies = !observer.getInjection().injectionPoints.isEmpty();

                // declaring bean
                LocalVar declaringProvider;
                // declaring bean instance, may be `null`
                LocalVar declaringProviderInstance;
                // this CC is used if the declaring bean itself is `@Dependent`
                LocalVar declaringProviderCtx;

                // this CC is used for `@Dependent` instances injected into method parameters
                LocalVar ctx = hasDependencies
                        ? b0.localVar("ctx", b0.new_(ConstructorDesc.of(CreationalContextImpl.class, Contextual.class),
                                Const.ofNull(Contextual.class)))
                        : null;

                // for `static` observers we don't need to obtain the contextual instance of the bean which declares the observer
                if (!isStatic) {
                    declaringProvider = b0.localVar("declaringProvider", b0.cast(b0.invokeInterface(MethodDescs.SUPPLIER_GET,
                            cc.this_().field(declaringProviderField)), Contextual.class));

                    if (observer.getReception() == Reception.IF_EXISTS
                            && !BuiltinScope.DEPENDENT.is(observer.getDeclaringBean().getScope())) {
                        LocalVar context = b0.localVar("context", b0.invokeInterface(
                                MethodDescs.ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                                b0.invokeStatic(MethodDescs.ARC_REQUIRE_CONTAINER),
                                Const.of(classDescOf(observer.getDeclaringBean().getScope().getDotName()))));
                        // context not active, notification is noop
                        b0.ifNull(context, BlockCreator::return_);
                        declaringProviderInstance = b0.localVar("declaringProviderInstance",
                                b0.invokeInterface(MethodDescs.CONTEXT_GET_IF_PRESENT, context, declaringProvider));
                        // bean instance does not exist in the context, notification is noop
                        b0.ifNull(declaringProviderInstance, BlockCreator::return_);
                        declaringProviderCtx = null;
                    } else if (BuiltinScope.DEPENDENT.is(observer.getDeclaringBean().getScope())) {
                        // always create a new dependent instance
                        declaringProviderCtx = b0.localVar("declaringProviderCtx",
                                b0.new_(ConstructorDesc.of(CreationalContextImpl.class, Contextual.class), declaringProvider));
                        declaringProviderInstance = b0.localVar("declaringProviderInstance",
                                b0.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET,
                                        declaringProvider, declaringProviderCtx));
                    } else {
                        // obtain contextual instance for non-`@Dependent` beans
                        LocalVar context = b0.localVar("context", b0.invokeInterface(
                                MethodDescs.ARC_CONTAINER_GET_ACTIVE_CONTEXT,
                                b0.invokeStatic(MethodDescs.ARC_REQUIRE_CONTAINER),
                                Const.of(classDescOf(observer.getDeclaringBean().getScope().getDotName()))));
                        // context not active, notification is noop
                        b0.ifNull(context, BlockCreator::return_);
                        // obtain the contextual instance, if exists
                        declaringProviderInstance = b0.localVar("declaringProviderInstance",
                                b0.invokeInterface(MethodDescs.CONTEXT_GET_IF_PRESENT, context, declaringProvider));
                        b0.ifNull(declaringProviderInstance, b1 -> {
                            // contextual instance doesn't exist yet, create one
                            b1.set(declaringProviderInstance, b1.invokeInterface(MethodDescs.CONTEXT_GET,
                                    context, declaringProvider,
                                    b1.new_(ConstructorDesc.of(CreationalContextImpl.class, Contextual.class),
                                            declaringProvider)));
                        });
                        declaringProviderCtx = null;
                    }
                } else {
                    declaringProvider = null;
                    declaringProviderInstance = null;
                    declaringProviderCtx = null;
                }

                // Collect all method arguments
                LocalVar[] args = new LocalVar[observer.getObserverMethod().parametersCount()];
                Iterator<InjectionPointInfo> injectionPointsIterator = observer.getInjection().injectionPoints.iterator();
                for (int i = 0; i < observer.getObserverMethod().parametersCount(); i++) {
                    if (i == observer.getEventParameter().position()) {
                        Expr eventObject = b0.invokeInterface(MethodDescs.EVENT_CONTEXT_GET_EVENT, eventContext);
                        args[i] = b0.localVar("arg" + i, eventObject);
                    } else if (i == observer.getEventMetadataParameterPosition()) {
                        args[i] = b0.localVar("arg" + i,
                                b0.invokeInterface(MethodDescs.EVENT_CONTEXT_GET_METADATA, eventContext));
                    } else {
                        InjectionPointInfo injectionPoint = injectionPointsIterator.next();
                        Expr provider = b0.invokeInterface(MethodDescs.SUPPLIER_GET,
                                cc.this_().field(injectionPointToProviderField.get(injectionPoint)));
                        Expr childCtx = b0.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD, ctx);
                        Expr dependency = b0.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET, provider, childCtx);
                        LocalVar arg = b0.localVar("arg" + i, dependency);
                        BeanGenerator.checkPrimitiveInjection(b0, injectionPoint, arg);
                        args[i] = arg;
                    }
                }

                if (Modifier.isPrivate(observer.getObserverMethod().flags())) {
                    // Reflection fallback
                    privateMembers.add(isApplicationClass, String.format("Observer method %s#%s()",
                            observer.getObserverMethod().declaringClass().name(), observer.getObserverMethod().name()));
                    reflectionRegistration.registerMethod(observer.getObserverMethod());
                    LocalVar paramsArray = b0.localVar("paramTypes", b0.newEmptyArray(Class.class, args.length));
                    LocalVar argsArray = b0.localVar("args", b0.newEmptyArray(Object.class, args.length));
                    for (int i = 0; i < args.length; i++) {
                        b0.set(paramsArray.elem(i), Const.of(classDescOf(observer.getObserverMethod().parameterType(i))));
                        b0.set(argsArray.elem(i), args[i]);
                    }
                    b0.invokeStatic(MethodDescs.REFLECTIONS_INVOKE_METHOD,
                            Const.of(classDescOf(observer.getObserverMethod().declaringClass())),
                            Const.of(observer.getObserverMethod().name()),
                            paramsArray, declaringProviderInstance, argsArray);
                } else {
                    if (isStatic) {
                        b0.invokeStatic(methodDescOf(observer.getObserverMethod()), args);
                    } else {
                        b0.invokeVirtual(methodDescOf(observer.getObserverMethod()), declaringProviderInstance, args);
                    }
                }

                // Destroy @Dependent instances injected into method parameters of an observer method
                if (hasDependencies) {
                    b0.invokeInterface(MethodDescs.CREATIONAL_CTX_RELEASE, ctx);
                }

                // if non-`static` and the declaring bean is `@Dependent`, we must destroy the instance afterwards
                if (!isStatic && BuiltinScope.DEPENDENT.is(observer.getDeclaringBean().getScope())) {
                    b0.invokeInterface(MethodDescs.INJECTABLE_BEAN_DESTROY, declaringProvider,
                            declaringProviderInstance, declaringProviderCtx);
                }

                b0.return_();
            });
        });
    }
}
