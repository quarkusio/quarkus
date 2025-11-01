package io.quarkus.arc.processor;

import static io.quarkus.gizmo2.Reflection2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.invoke.Invoker;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.PrimitiveType;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.gizmo2.StringBuilderGen;
import org.jboss.logging.Logger;

import io.quarkus.arc.impl.CreationalContextImpl;
import io.quarkus.arc.impl.InvokerCleanupTasks;
import io.quarkus.arc.processor.BuiltinBean.GeneratorContext;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.StaticFieldVar;
import io.quarkus.gizmo2.Var;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ClassMethodDesc;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.smallrye.common.annotation.SuppressForbidden;

public class InvokerGenerator extends AbstractGenerator {
    private static final Logger LOGGER = Logger.getLogger(InvokerGenerator.class);

    private final Predicate<DotName> applicationClassPredicate;
    private final IndexView beanArchiveIndex;
    private final BeanDeployment beanDeployment;
    private final AnnotationLiteralProcessor annotationLiterals;
    private final ReflectionRegistration reflectionRegistration;
    private final Predicate<DotName> injectionPointAnnotationsPredicate;

    private final Assignability assignability;

    InvokerGenerator(boolean generateSources, Predicate<DotName> applicationClassPredicate, BeanDeployment deployment,
            AnnotationLiteralProcessor annotationLiterals, ReflectionRegistration reflectionRegistration,
            Predicate<DotName> injectionPointAnnotationsPredicate) {
        super(generateSources);
        this.applicationClassPredicate = applicationClassPredicate;
        this.beanArchiveIndex = deployment.getBeanArchiveIndex();
        this.beanDeployment = deployment;
        this.annotationLiterals = annotationLiterals;
        this.reflectionRegistration = reflectionRegistration;
        this.injectionPointAnnotationsPredicate = injectionPointAnnotationsPredicate;

        this.assignability = new Assignability(deployment.getBeanArchiveIndex());
    }

    Collection<Resource> generate(InvokerInfo invoker) {
        Function<String, Resource.SpecialType> specialTypeFunction = className -> {
            if (className.equals(invoker.className)
                    || className.equals(invoker.wrapperClassName)
                    || className.equals(invoker.lazyClassName)) {
                return Resource.SpecialType.INVOKER;
            }
            return null;
        };

        ResourceClassOutput classOutput = new ResourceClassOutput(
                applicationClassPredicate.test(invoker.targetBeanClass.name()), specialTypeFunction, generateSources);

        io.quarkus.gizmo2.Gizmo gizmo = gizmo(classOutput);

        createInvokerLazyClass(gizmo, invoker);
        createInvokerWrapperClass(gizmo, invoker);
        createInvokerClass(gizmo, invoker);

        return classOutput.getResources();
    }

    // ---

    private void createInvokerLazyClass(io.quarkus.gizmo2.Gizmo gizmo, InvokerInfo invoker) {
        if (!invoker.usesLookup) {
            return;
        }

        gizmo.class_(invoker.lazyClassName, cc -> {
            cc.implements_(Invoker.class);

            cc.defaultConstructor();

            cc.method("invoke", mc -> {
                mc.returning(Object.class);
                ParamVar instance = mc.parameter("instance", Object.class);
                ParamVar arguments = mc.parameter("arguments", Object[].class);
                mc.body(bc -> {
                    ClassDesc invokerClass = invoker.wrapperClassName != null
                            ? ClassDesc.of(invoker.wrapperClassName)
                            : ClassDesc.of(invoker.className);

                    Expr delegate = bc.invokeStatic(ClassMethodDesc.of(invokerClass, "get", Invoker.class));
                    Expr result = bc.invokeInterface(MethodDesc.of(Invoker.class, "invoke", Object.class,
                            Object.class, Object[].class), delegate, instance, arguments);
                    bc.return_(result);
                });
            });
        });
    }

    // ---

    private void createInvokerWrapperClass(io.quarkus.gizmo2.Gizmo gizmo, InvokerInfo invoker) {
        if (invoker.wrapperClassName == null) {
            return;
        }

        gizmo.class_(invoker.wrapperClassName, cc -> {
            cc.implements_(Invoker.class);

            FieldDesc delegate = cc.field("delegate", fc -> {
                fc.private_();
                fc.final_();
                fc.setType(Invoker.class);
            });

            ConstructorDesc ctor = cc.constructor(mc -> {
                mc.body(bc -> {
                    bc.invokeSpecial(ConstructorDesc.of(Object.class), cc.this_());
                    bc.set(cc.this_().field(delegate), bc.invokeStatic(
                            ClassMethodDesc.of(ClassDesc.of(invoker.className), "get", Invoker.class)));
                    bc.return_();
                });
            });

            cc.method("invoke", mc -> {
                mc.returning(Object.class);
                ParamVar instance = mc.parameter("instance", Object.class);
                ParamVar arguments = mc.parameter("arguments", Object[].class);
                mc.body(bc -> {
                    MethodInfo wrappingMethod = findWrapper(invoker);
                    Expr result = bc.invokeStatic(methodDescOf(wrappingMethod), instance, arguments,
                            cc.this_().field(delegate));
                    if (wrappingMethod.returnType().kind() == Type.Kind.VOID) {
                        result = Const.ofNull(Object.class);
                    }
                    bc.return_(result);
                });
            });

            generateStaticGetMethod(cc, ctor);
        });
    }

    private MethodInfo findWrapper(InvokerInfo invoker) {
        InvocationTransformer wrapper = invoker.invocationWrapper;
        ClassInfo clazz = beanArchiveIndex.getClassByName(wrapper.clazz);
        List<MethodInfo> methods = new ArrayList<>();
        for (MethodInfo method : clazz.methods()) {
            if (Modifier.isStatic(method.flags()) && wrapper.method.equals(method.name())) {
                methods.add(method);
            }
        }

        List<MethodInfo> matching = new ArrayList<>();
        List<MethodInfo> notMatching = new ArrayList<>();
        for (MethodInfo method : methods) {
            if (method.parametersCount() == 3
                    && method.parameterType(1).kind() == Type.Kind.ARRAY
                    && method.parameterType(1).asArrayType().deepDimensions() == 1
                    && method.parameterType(1).asArrayType().elementType().name().equals(DotName.OBJECT_NAME)
                    && method.parameterType(2).name().equals(DotName.createSimple(Invoker.class))) {

                Type targetInstanceType = method.parameterType(0);
                boolean targetInstanceOk = isAnyType(targetInstanceType)
                        || assignability.isSupertype(targetInstanceType, ClassType.create(invoker.targetBeanClass.name()));

                boolean isInvokerRaw = method.parameterType(2).kind() == Type.Kind.CLASS;
                boolean isInvokerParameterized = method.parameterType(2).kind() == Type.Kind.PARAMETERIZED_TYPE
                        && method.parameterType(2).asParameterizedType().arguments().size() == 2;
                boolean invokerTargetInstanceOk = isInvokerRaw
                        || isInvokerParameterized
                                && targetInstanceType.equals(method.parameterType(2).asParameterizedType().arguments().get(0));

                if (targetInstanceOk && invokerTargetInstanceOk) {
                    matching.add(method);
                } else {
                    notMatching.add(method);
                }
            } else {
                notMatching.add(method);
            }
        }

        if (matching.size() == 1) {
            return matching.get(0);
        }

        if (matching.isEmpty()) {
            String expectation = ""
                    + "\tmatching methods must be `static` and take 3 parameters (instance, argument array, invoker)\n"
                    + "\tthe 1st parameter must be a supertype of " + invoker.targetBeanClass.name() + ", possibly Object\n"
                    + "\tthe 2nd parameter must be Object[]\n"
                    + "\tthe 3rd parameter must be Invoker<type of 1st parameter, some type>";
            if (notMatching.isEmpty()) {
                throw new IllegalArgumentException(""
                        + "Error creating invoker for method " + invoker + ":\n"
                        + "\tno matching method found for " + wrapper + "\n"
                        + expectation);
            } else {
                throw new IllegalArgumentException(""
                        + "Error creating invoker for method " + invoker + ":\n"
                        + "\tno matching method found for " + wrapper + "\n"
                        + "\tfound methods that do not match:\n"
                        + notMatching.stream().map(it -> "\t- " + it).collect(Collectors.joining("\n")) + "\n"
                        + expectation);
            }
        } else {
            throw new IllegalArgumentException(""
                    + "Error creating invoker for method " + invoker + ":\n"
                    + "\ttoo many matching methods for " + wrapper + ":\n"
                    + matching.stream().map(it -> "\t- " + it).collect(Collectors.joining("\n")));
        }
    }

    // ---

    private void createInvokerClass(io.quarkus.gizmo2.Gizmo gizmo, InvokerInfo invoker) {
        CodeGenInfo info = preprocess(invoker);

        gizmo.class_(invoker.className, cc -> {
            cc.implements_(Invoker.class);

            FieldDesc instanceSupplier;
            FieldDesc[] argumentSuppliers = new FieldDesc[invoker.method.parametersCount()];
            if (info.instanceLookup != null) {
                instanceSupplier = cc.field("instance", fc -> {
                    fc.private_();
                    fc.final_();
                    fc.setType(Supplier.class);
                });
            } else {
                instanceSupplier = null;
            }
            for (int i = 0; i < info.argumentLookups.length; i++) {
                if (info.argumentLookups[i] != null) {
                    argumentSuppliers[i] = cc.field("arg" + i, fc -> {
                        fc.private_();
                        fc.final_();
                        fc.setType(Supplier.class);
                    });
                }
            }

            ConstructorDesc ctor = cc.constructor(mc -> {
                mc.body(bc -> {
                    bc.invokeSpecial(ConstructorDesc.of(Object.class), cc.this_());

                    if (info.usesLookup) {
                        LocalVar arc = bc.localVar("arc", bc.invokeStatic(MethodDescs.ARC_REQUIRE_CONTAINER));
                        if (instanceSupplier != null) {
                            Expr instanceBean = bc.invokeInterface(MethodDescs.ARC_CONTAINER_BEAN, arc,
                                    Const.of(info.instanceLookup.getUserBean().getIdentifier()));
                            bc.set(cc.this_().field(instanceSupplier), instanceBean);
                        }
                        for (int i = 0; i < argumentSuppliers.length; i++) {
                            if (argumentSuppliers[i] != null) {
                                ResolvedBean resolved = info.argumentLookups[i];
                                if (resolved.isUserBean()) {
                                    Expr argumentBean = bc.invokeInterface(MethodDescs.ARC_CONTAINER_BEAN, arc,
                                            Const.of(resolved.getUserBean().getIdentifier()));
                                    bc.set(cc.this_().field(argumentSuppliers[i]), argumentBean);
                                } else {
                                    BuiltinBean builtinBean = resolved.getBuiltinBean();
                                    InjectionPointInfo injectionPoint = invoker.getInjectionPointForArgument(i);
                                    builtinBean.getGenerator().generate(new GeneratorContext(beanDeployment,
                                            invoker, injectionPoint, cc, bc, argumentSuppliers[i], annotationLiterals,
                                            reflectionRegistration, injectionPointAnnotationsPredicate, null));
                                }
                            }
                        }
                    }

                    bc.return_();
                });
            });

            cc.method("invoke", mc -> {
                mc.returning(Object.class);
                ParamVar instanceParam = mc.parameter("instance", Object.class);
                ParamVar argumentsParam = mc.parameter("arguments", Object[].class);
                mc.body(b0 -> {
                    LocalVar cleanupTasks = info.usesCleanupTasks
                            ? b0.localVar("cleanupTasks", b0.new_(InvokerCleanupTasks.class))
                            : null;

                    LocalVar rootCC = info.usesLookup
                            ? b0.localVar("cc", b0.new_(CreationalContextImpl.class, Const.ofNull(Contextual.class)))
                            : null;

                    Var instance = null;
                    if (!Modifier.isStatic(invoker.method.flags())) {
                        instance = info.instanceLookup != null
                                ? b0.localVar("instance", generateLookup(cc, b0, instanceSupplier, rootCC))
                                : instanceParam;
                        if (info.instanceTransformer != null) {
                            instance = b0.localVar("transformedInstance",
                                    generateTransformerCall(b0, info.instanceTransformer, instance, cleanupTasks));
                        }
                    }
                    Var instanceFinal = instance;

                    Var[] arguments = new Var[invoker.method.parametersCount()];
                    for (int i = 0; i < invoker.method.parametersCount(); i++) {
                        Type parameterType = invoker.method.parameterType(i);
                        arguments[i] = info.argumentLookups[i] != null
                                ? b0.localVar("arg" + i, generateLookup(cc, b0, argumentSuppliers[i], rootCC))
                                : b0.localVar("arg" + i, argumentsParam.elem(i));
                        if (info.argumentTransformers[i] != null) {
                            arguments[i] = b0.localVar("transformedArg" + i,
                                    generateTransformerCall(b0, info.argumentTransformers[i], arguments[i], cleanupTasks));
                            if (info.argumentTransformers[i].method.returnType().kind() == Type.Kind.PRIMITIVE) {
                                arguments[i] = b0.localVar("boxedArg" + i, b0.box(arguments[i]));
                            }
                        }
                        if (parameterType.kind() == Type.Kind.PRIMITIVE) {
                            LocalVar primitiveArg = b0.localVar("primitiveArg" + i,
                                    Const.ofDefault(classDescOf(parameterType)));
                            int finalI = i;
                            b0.ifElse(b0.isNotNull(arguments[i]), b1 -> {
                                unboxAndWiden(b1, arguments[finalI], primitiveArg,
                                        invoker.method.parameterType(finalI).asPrimitiveType(), finalI);
                            }, b1 -> {
                                b1.throw_(NullPointerException.class, "Argument " + finalI + " is null, "
                                        + invoker.method.parameterType(finalI) + " expected");
                            });
                            arguments[i] = primitiveArg;
                        }
                    }

                    if (info.usesLookup && invoker.method.parametersCount() > 0) {
                        // the specification requires that the arguments array has at least as many elements
                        // as the target method has parameters, even if some of them (or all of them)
                        // are looked up
                        //
                        // we check that by simply reading the last parameter's position in the arguments array, when:
                        // 1. some lookups are configured (otherwise the check would be duplicate)
                        // 2. the target method has parameters (otherwise the check would be meaningless)
                        b0.get(argumentsParam.elem(invoker.method.parametersCount() - 1));
                    }

                    b0.try_(tc -> {
                        tc.body(b1 -> {
                            Expr result;
                            MethodDesc methodDesc = methodDescOf(invoker.method);
                            if (Modifier.isStatic(invoker.method.flags())) {
                                result = b1.invokeStatic(methodDesc, arguments);
                            } else if (invoker.method.declaringClass().isInterface()) {
                                result = b1.invokeInterface(methodDesc, instanceFinal, arguments);
                            } else {
                                result = b1.invokeVirtual(methodDesc, instanceFinal, arguments);
                            }
                            boolean isVoid = invoker.method.returnType().kind() == Type.Kind.VOID;
                            LocalVar resultVar = b1.localVar("result", isVoid ? Const.ofNull(Object.class) : result);

                            if (cleanupTasks != null) {
                                b1.invokeVirtual(MethodDesc.of(InvokerCleanupTasks.class, "finish", void.class), cleanupTasks);
                            }

                            if (rootCC != null) {
                                b1.set(resultVar, generateCCRelease(b1, invoker, rootCC, resultVar));
                            }

                            if (info.returnValueTransformer != null) {
                                b1.set(resultVar, generateTransformerCall(b1, info.returnValueTransformer, resultVar,
                                        cleanupTasks));
                            }

                            b1.return_(resultVar);
                        });
                        tc.catch_(Throwable.class, "e", (b1, e) -> {
                            if (cleanupTasks != null) {
                                b1.invokeVirtual(MethodDesc.of(InvokerCleanupTasks.class, "finish", void.class), cleanupTasks);
                            }

                            if (rootCC != null) {
                                // return value is always `null` here, so we can ignore it
                                generateCCRelease(b1, invoker, rootCC, null);
                            }

                            if (invoker.exceptionTransformer != null) {
                                b1.return_(generateTransformerCall(b1, info.exceptionTransformer, e, null));
                            } else {
                                b1.throw_(e);
                            }
                        });
                    });
                });
            });

            generateStaticGetMethod(cc, ctor);
        });
    }

    private Expr generateLookup(ClassCreator cc, BlockCreator bc, FieldDesc supplierField, LocalVar rootCC) {
        LocalVar injectableReferenceProvider = bc.localVar("injectableReferenceProvider",
                bc.invokeInterface(MethodDescs.SUPPLIER_GET, cc.this_().field(supplierField)));
        LocalVar creationalContext = bc.localVar("creationalContext",
                bc.invokeStatic(MethodDescs.CREATIONAL_CTX_CHILD_CONTEXTUAL, injectableReferenceProvider, rootCC));
        return bc.invokeInterface(MethodDescs.INJECTABLE_REF_PROVIDER_GET,
                injectableReferenceProvider, creationalContext);
    }

    private Expr generateTransformerCall(BlockCreator bc, TransformerMethod transformerMethod, Var value,
            LocalVar cleanupTasks) {
        assert transformerMethod != null;
        MethodDesc methodDesc = methodDescOf(transformerMethod.method);

        if (Modifier.isStatic(transformerMethod.method.flags())) {
            if (transformerMethod.usesCleanupTasks()) {
                return bc.invokeStatic(methodDesc, value, cleanupTasks);
            } else {
                return bc.invokeStatic(methodDesc, value);
            }
        } else {
            if (transformerMethod.method.declaringClass().isInterface()) {
                return bc.invokeInterface(methodDesc, value);
            } else {
                return bc.invokeVirtual(methodDesc, value);
            }
        }
    }

    private Expr generateCCRelease(BlockCreator bc, InvokerInfo invoker, LocalVar rootCC, LocalVar returnValue) {
        assert rootCC != null;

        // `returnValue` is `null` when the target method has thrown an exception;
        // we're going to rethrow it, so we need to release the `CreationalContext` immediately
        if (returnValue == null || !invoker.isAsynchronous()) {
            bc.invokeInterface(MethodDescs.CREATIONAL_CTX_RELEASE, rootCC);
            return returnValue;
        } else {
            ClassDesc asyncType = classDescOf(invoker.method.returnType());
            return bc.invokeStatic(ClassMethodDesc.of(classDescOf(InvokerCleanupTasks.class), "deferRelease",
                    MethodTypeDesc.of(asyncType, classDescOf(CreationalContext.class), asyncType)),
                    rootCC, returnValue);
        }
    }

    private void generateStaticGetMethod(ClassCreator cc, ConstructorDesc ctor) {
        StaticFieldVar instance = cc.staticField("INSTANCE", fc -> {
            fc.private_();
            fc.final_();
            fc.setType(AtomicReference.class);
            fc.setInitializer(bc -> {
                bc.yield(bc.new_(AtomicReference.class));
            });
        });

        MethodDesc atomicReferenceGet = MethodDesc.of(AtomicReference.class, "get", Object.class);
        MethodDesc atomicReferenceCAS = MethodDesc.of(AtomicReference.class, "compareAndSet", boolean.class,
                Object.class, Object.class);

        cc.staticMethod("get", mc -> {
            mc.returning(Invoker.class);
            mc.body(b0 -> {
                LocalVar result = b0.localVar("result", b0.invokeVirtual(atomicReferenceGet, instance));
                b0.ifNull(result, b1 -> {
                    b1.invokeVirtual(atomicReferenceCAS, instance, Const.ofNull(Invoker.class), b1.new_(ctor));
                    b1.set(result, b1.invokeVirtual(atomicReferenceGet, instance));
                });
                b0.return_(result);
            });
        });
    }

    private static void unboxAndWiden(BlockCreator b0, Var value, Var target, PrimitiveType targetType, int argumentNumber) {
        LocalVar ok = b0.localVar("ok" + argumentNumber, Const.of(false));

        // unboxing conversion
        b0.ifInstanceOf(value, classDescOf(PrimitiveType.box(targetType)), (b1, cast) -> {
            b1.set(target, cast);
            b1.set(ok, Const.of(true));
        });
        for (ClassType possibleType : WIDENING_CONVERSIONS_TO.get(targetType.primitive())) {
            // unboxing + widening conversion
            b0.ifInstanceOf(value, classDescOf(possibleType), (b1, cast) -> {
                b1.set(target, cast);
                b1.set(ok, Const.of(true));
            });
        }
        b0.ifNot(ok, b1 -> {
            b1.throw_(ClassCastException.class, StringBuilderGen.ofNew(b1)
                    .append("No method invocation conversion to ")
                    .append(targetType.name().toString())
                    .append(" exists for argument ")
                    .append("" + argumentNumber)
                    .append(": ")
                    .append(b1.withObject(value).getClass_())
                    .append(", value ")
                    .append(value)
                    .toString_());
        });
    }

    private static final Map<Primitive, Set<ClassType>> WIDENING_CONVERSIONS_TO = Map.of(
            Primitive.BOOLEAN, Set.of(),
            Primitive.BYTE, Set.of(),
            Primitive.SHORT, Set.of(ClassType.BYTE_CLASS),
            Primitive.INT, Set.of(ClassType.BYTE_CLASS, ClassType.SHORT_CLASS, ClassType.CHARACTER_CLASS),
            Primitive.LONG, Set.of(ClassType.BYTE_CLASS, ClassType.SHORT_CLASS, ClassType.INTEGER_CLASS,
                    ClassType.CHARACTER_CLASS),
            Primitive.FLOAT, Set.of(ClassType.BYTE_CLASS, ClassType.SHORT_CLASS, ClassType.INTEGER_CLASS,
                    ClassType.LONG_CLASS, ClassType.CHARACTER_CLASS),
            Primitive.DOUBLE, Set.of(ClassType.BYTE_CLASS, ClassType.SHORT_CLASS, ClassType.INTEGER_CLASS,
                    ClassType.LONG_CLASS, ClassType.FLOAT_CLASS, ClassType.CHARACTER_CLASS),
            Primitive.CHAR, Set.of());

    private record CodeGenInfo(
            ResolvedBean instanceLookup,
            ResolvedBean[] argumentLookups,
            TransformerMethod instanceTransformer,
            TransformerMethod[] argumentTransformers,
            TransformerMethod returnValueTransformer,
            TransformerMethod exceptionTransformer,
            boolean usesLookup,
            boolean usesCleanupTasks) {
    }

    private CodeGenInfo preprocess(InvokerInfo invoker) {
        boolean usesLookup = false;
        ResolvedBean instanceLookup = null;
        if (invoker.instanceLookup) {
            instanceLookup = ResolvedBean.of(invoker.targetBean);
            usesLookup = true;
        }

        ResolvedBean[] argumentLookups = new ResolvedBean[invoker.argumentLookups.length];
        for (int i = 0; i < invoker.argumentLookups.length; i++) {
            if (invoker.argumentLookups[i]) {
                InjectionPointInfo injectionPoint = invoker.getInjectionPointForArgument(i);
                if (injectionPoint != null) {
                    argumentLookups[i] = ResolvedBean.of(injectionPoint);
                } else {
                    throw new IllegalStateException("No injection point for argument " + i + " of " + invoker);
                }
                usesLookup = true;
            }
        }

        boolean usesCleanupTasks = false;
        TransformerMethod instanceTransformer = null;
        if (invoker.instanceTransformer != null) {
            Type instanceType = ClassType.create(invoker.targetBeanClass.name());
            instanceTransformer = findCandidates(invoker.instanceTransformer, instanceType, invoker).resolve();
            usesCleanupTasks |= instanceTransformer.usesCleanupTasks();
        }

        TransformerMethod[] argumentTransfomers = new TransformerMethod[invoker.argumentTransformers.length];
        for (int i = 0; i < invoker.argumentTransformers.length; i++) {
            if (invoker.argumentTransformers[i] != null) {
                Type parameterType = invoker.method.parameterType(i);
                argumentTransfomers[i] = findCandidates(invoker.argumentTransformers[i], parameterType, invoker).resolve();
                usesCleanupTasks |= argumentTransfomers[i].usesCleanupTasks();
            }
        }

        TransformerMethod returnValueTransformer = null;
        if (invoker.returnValueTransformer != null) {
            Type returnType = invoker.method.returnType();
            returnValueTransformer = findCandidates(invoker.returnValueTransformer, returnType, invoker).resolve();
            usesCleanupTasks |= returnValueTransformer.usesCleanupTasks();
        }

        TransformerMethod exceptionTransformer = null;
        if (invoker.exceptionTransformer != null) {
            ClassType throwableType = ClassType.create(Throwable.class);
            exceptionTransformer = findCandidates(invoker.exceptionTransformer, throwableType, invoker).resolve();
            usesCleanupTasks |= exceptionTransformer.usesCleanupTasks();
        }

        return new CodeGenInfo(instanceLookup, argumentLookups, instanceTransformer, argumentTransfomers,
                returnValueTransformer, exceptionTransformer, usesLookup, usesCleanupTasks);
    }

    private TransformerMethodCandidates findCandidates(InvocationTransformer transformer, Type expectedType,
            InvokerInfo invoker) {
        assert transformer.kind != InvocationTransformerKind.WRAPPER;

        ClassInfo clazz = beanArchiveIndex.getClassByName(transformer.clazz);

        // static methods only from the given class
        // instance methods also from superclasses and superinterfaces

        // first, set up the worklist so that it contains the given class and all its superclasses
        // next, as each class from the queue is processed, add its interfaces to the queue
        // this is so that superclasses are processed before interfaces
        Deque<ClassInfo> worklist = new ArrayDeque<>();
        while (clazz != null) {
            worklist.addLast(clazz);
            clazz = clazz.superName() == null ? null : beanArchiveIndex.getClassByName(clazz.superName());
        }

        boolean originalClass = true;
        Set<Methods.MethodKey> seenMethods = new HashSet<>();
        while (!worklist.isEmpty()) {
            ClassInfo current = worklist.removeFirst();

            for (MethodInfo method : current.methods()) {
                if (!transformer.method.equals(method.name())) {
                    continue;
                }

                Methods.MethodKey key = new Methods.MethodKey(method);

                if (Modifier.isStatic(method.flags()) && originalClass) {
                    seenMethods.add(key);
                } else {
                    if (!Methods.isOverriden(key, seenMethods)) {
                        seenMethods.add(key);
                    }
                }
            }

            for (DotName iface : current.interfaceNames()) {
                worklist.addLast(beanArchiveIndex.getClassByName(iface));
            }

            originalClass = false;
        }

        List<TransformerMethod> matching = new ArrayList<>();
        List<TransformerMethod> notMatching = new ArrayList<>();
        for (Methods.MethodKey seenMethod : seenMethods) {
            TransformerMethod candidate = new TransformerMethod(seenMethod.method, assignability);
            if (candidate.matches(transformer, expectedType)) {
                matching.add(candidate);
            } else {
                notMatching.add(candidate);
            }
        }
        return new TransformerMethodCandidates(transformer, expectedType, matching, notMatching, invoker);
    }

    static class ResolvedBean {
        // exactly one of these is non-null
        private final BeanInfo userBean;
        private final BuiltinBean builtinBean;

        static ResolvedBean of(BeanInfo userBean) {
            return new ResolvedBean(userBean, null);
        }

        static ResolvedBean of(InjectionPointInfo injectionPoint) {
            BeanInfo userBean = injectionPoint.getResolvedBean();
            if (userBean != null) {
                return new ResolvedBean(userBean, null);
            }
            BuiltinBean builtinBean = BuiltinBean.resolve(injectionPoint);
            if (builtinBean != null) {
                return new ResolvedBean(null, builtinBean);
            }
            throw new IllegalStateException("Injection point not resolved: " + injectionPoint);
        }

        private ResolvedBean(BeanInfo userBean, BuiltinBean builtinBean) {
            this.userBean = userBean;
            this.builtinBean = builtinBean;
        }

        boolean isUserBean() {
            return userBean != null;
        }

        boolean isBuiltinBean() {
            return builtinBean != null;
        }

        BeanInfo getUserBean() {
            assert isUserBean();
            return userBean;
        }

        BuiltinBean getBuiltinBean() {
            assert isBuiltinBean();
            return builtinBean;
        }

        boolean requiresDestruction() {
            return userBean != null && BuiltinScope.DEPENDENT.is(userBean.getScope());
        }
    }

    static class TransformerMethodCandidates {
        // most of the fields here are only used for providing a good error message
        final InvocationTransformer transformer;
        final Type expectedType;
        final List<TransformerMethod> matching;
        final List<TransformerMethod> notMatching;
        final InvokerInfo invoker;

        TransformerMethodCandidates(InvocationTransformer transformer, Type expectedType,
                List<TransformerMethod> matching, List<TransformerMethod> notMatching,
                InvokerInfo invoker) {
            this.transformer = transformer;
            this.expectedType = expectedType;
            this.matching = matching;
            this.notMatching = notMatching;
            this.invoker = invoker;
        }

        @SuppressForbidden(reason = "Using Type.toString() to build an informative message")
        TransformerMethod resolve() {
            if (matching.size() == 1) {
                return matching.get(0);
            }

            if (matching.isEmpty()) {
                String expectedType = this.expectedType.toString();
                String expectation = "";
                if (transformer.isInputTransformer()) {
                    expectation = "\n"
                            + "\tmatching `static` methods must take 1 or 2 parameters and return " + expectedType
                            + " (or subtype)\n"
                            + "\t(if the `static` method takes 2 parameters, the 2nd must be `Consumer<Runnable>`)\n"
                            + "\tmatching instance methods must take no parameter and return " + expectedType + " (or subtype)";
                } else if (transformer.isOutputTransformer()) {
                    expectation = "\n"
                            + "\tmatching `static` method must take 1 parameter of type " + expectedType + " (or supertype)\n"
                            + "\tmatching instance methods must be declared on " + expectedType
                            + " (or supertype) and take no parameter";
                }

                if (notMatching.isEmpty()) {
                    throw new IllegalArgumentException(""
                            + "Error creating invoker for method " + invoker + ":\n"
                            + "\tno matching method found for " + transformer
                            + expectation);
                } else {
                    throw new IllegalArgumentException(""
                            + "Error creating invoker for method " + invoker + ":\n"
                            + "\tno matching method found for " + transformer + "\n"
                            + "\tfound methods that do not match:\n"
                            + notMatching.stream().map(it -> "\t- " + it).collect(Collectors.joining("\n"))
                            + expectation);
                }
            } else {
                throw new IllegalArgumentException(""
                        + "Error creating invoker for method " + invoker + ":\n"
                        + "\ttoo many matching methods for " + transformer + ":\n"
                        + matching.stream().map(it -> "\t- " + it).collect(Collectors.joining("\n")));
            }
        }
    }

    static class TransformerMethod {
        final MethodInfo method;
        private final Assignability assignability;

        TransformerMethod(MethodInfo method, Assignability assignability) {
            this.method = method;
            this.assignability = assignability;
        }

        boolean matches(InvocationTransformer transformer, Type expectedType) {
            if (transformer.isInputTransformer()) {
                // for input transformer (target instance, argument):
                // - we can't check what comes into the transformer
                // - we can check what comes out of the transformer, because that's what the invokable method consumes
                //   (and the transformer must produce a subtype)

                boolean returnTypeOk = isAnyType(method.returnType()) || isSubtype(method.returnType(), expectedType);
                if (Modifier.isStatic(method.flags())) {
                    return method.parametersCount() == 1 && returnTypeOk
                            || method.parametersCount() == 2 && returnTypeOk && isConsumerOfRunnable(method.parameterType(1));
                } else {
                    return method.parametersCount() == 0 && returnTypeOk;
                }
            } else if (transformer.isOutputTransformer()) {
                // for output transformer (return value, exception):
                // - we can check what comes into the transformer, because that's what the invokable method produces
                //   (and the transformer must consume a supertype)
                // - we can't check what comes out of the transformer

                if (Modifier.isStatic(method.flags())) {
                    return method.parametersCount() == 1
                            && (isAnyType(method.parameterType(0)) || isSupertype(method.parameterType(0), expectedType));
                } else {
                    return method.parametersCount() == 0
                            && isSupertype(ClassType.create(method.declaringClass().name()), expectedType);
                }
            } else {
                throw new IllegalArgumentException(transformer.toString());
            }
        }

        // if `matches()` returns `false`, there's no point in calling this method
        boolean usesCleanupTasks() {
            return Modifier.isStatic(method.flags())
                    && method.parametersCount() == 2
                    && isConsumerOfRunnable(method.parameterType(1));
        }

        private boolean isConsumerOfRunnable(Type type) {
            return type.kind() == Type.Kind.PARAMETERIZED_TYPE
                    && type.name().equals(DotName.createSimple(Consumer.class))
                    && type.asParameterizedType().arguments().size() == 1
                    && type.asParameterizedType().arguments().get(0).kind() == Type.Kind.CLASS
                    && type.asParameterizedType().arguments().get(0).name().equals(DotName.createSimple(Runnable.class));
        }

        private boolean isSubtype(Type a, Type b) {
            return assignability.isSubtype(a, b);
        }

        private boolean isSupertype(Type a, Type b) {
            return assignability.isSupertype(a, b);
        }

        @Override
        public String toString() {
            return method.toString() + " declared on " + method.declaringClass();
        }
    }

    // ---

    static boolean isAnyType(Type t) {
        if (ClassType.OBJECT_TYPE.equals(t)) {
            return true;
        }
        if (t.kind() == Type.Kind.TYPE_VARIABLE) {
            TypeVariable typeVar = t.asTypeVariable();
            return typeVar.bounds().isEmpty() || typeVar.hasImplicitObjectBound() || isAnyType(typeVar.bounds().get(0));
        }
        return false;
    }

    // this is mostly a prototype, doesn't follow any specification
    static class Assignability {
        private final AssignabilityCheck assignabilityCheck;

        Assignability(IndexView index) {
            this.assignabilityCheck = new AssignabilityCheck(index, null);
        }

        boolean isSubtype(Type a, Type b) {
            Objects.requireNonNull(a);
            Objects.requireNonNull(b);

            switch (a.kind()) {
                case VOID:
                    return b.kind() == Type.Kind.VOID
                            || b.kind() == Type.Kind.CLASS && b.asClassType().name().equals(DotName.createSimple(Void.class));
                case PRIMITIVE:
                    return b.kind() == Type.Kind.PRIMITIVE
                            && a.asPrimitiveType().primitive() == b.asPrimitiveType().primitive();
                case ARRAY:
                    return b.kind() == Type.Kind.ARRAY
                            && a.asArrayType().deepDimensions() == b.asArrayType().deepDimensions()
                            && isSubtype(a.asArrayType().elementType(), b.asArrayType().elementType());
                case CLASS:
                    if (b.kind() == Type.Kind.VOID) {
                        return a.asClassType().name().equals(DotName.createSimple(Void.class));
                    } else if (b.kind() == Type.Kind.CLASS) {
                        return isClassSubtype(a.asClassType(), b.asClassType());
                    } else if (b.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        return isClassSubtype(a.asClassType(), ClassType.create(b.name()));
                    } else if (b.kind() == Type.Kind.TYPE_VARIABLE) {
                        Type firstBound = b.asTypeVariable().bounds().isEmpty()
                                ? ClassType.OBJECT_TYPE
                                : b.asTypeVariable().bounds().get(0);
                        return isSubtype(a, firstBound);
                    } else {
                        return false;
                    }
                case PARAMETERIZED_TYPE:
                    if (b.kind() == Type.Kind.CLASS) {
                        return isClassSubtype(ClassType.create(a.name()), b.asClassType());
                    } else if (b.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        return isClassSubtype(ClassType.create(a.name()), ClassType.create(b.name()));
                    } else if (b.kind() == Type.Kind.TYPE_VARIABLE) {
                        Type firstBound = b.asTypeVariable().bounds().isEmpty()
                                ? ClassType.OBJECT_TYPE
                                : b.asTypeVariable().bounds().get(0);
                        return isSubtype(a, firstBound);
                    } else {
                        return false;
                    }
                case TYPE_VARIABLE:
                    if (b.kind() == Type.Kind.CLASS || b.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                        Type firstBound = a.asTypeVariable().bounds().isEmpty()
                                ? ClassType.OBJECT_TYPE
                                : a.asTypeVariable().bounds().get(0);
                        return isSubtype(firstBound, b);
                    } else {
                        return false;
                    }
                default:
                    throw new IllegalArgumentException("Cannot determine assignability between " + a + " and " + b);
            }
        }

        boolean isSupertype(Type a, Type b) {
            return isSubtype(b, a);
        }

        private boolean isClassSubtype(ClassType a, ClassType b) {
            return assignabilityCheck.isAssignableFrom(b, a);
        }
    }
}
