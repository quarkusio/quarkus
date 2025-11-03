package io.quarkus.arc.processor;

import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.annotation.Annotation;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.gizmo2.StringBuilderGen;
import org.jboss.logging.Logger;

import io.quarkus.arc.AbstractAnnotationLiteral;
import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.arc.processor.AnnotationLiteralProcessor.AnnotationLiteralClassInfo;
import io.quarkus.arc.processor.AnnotationLiteralProcessor.CacheKey;
import io.quarkus.arc.processor.ResourceOutput.Resource;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.FieldVar;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;

/**
 * This is an internal companion of {@link AnnotationLiteralProcessor} that handles generating
 * annotation literal classes. See {@link #generate(ComputingCache, Set) generate()} for more info.
 */
public class AnnotationLiteralGenerator extends AbstractGenerator {
    private static final Logger LOGGER = Logger.getLogger(AnnotationLiteralGenerator.class);

    AnnotationLiteralGenerator(boolean generateSources) {
        super(generateSources);
    }

    /**
     * Creator of an {@link AnnotationLiteralProcessor} must call this method at an appropriate point
     * in time and write the result to an appropriate output. If not, the bytecode sequences generated
     * using the {@code AnnotationLiteralProcessor} will refer to non-existing classes.
     *
     * @param existingClasses names of classes that already exist and should not be generated again
     * @return the generated classes, never {@code null}
     */
    Collection<Resource> generate(ComputingCache<CacheKey, AnnotationLiteralClassInfo> cache,
            Set<String> existingClasses) {
        List<ResourceOutput.Resource> resources = new ArrayList<>();
        cache.forEachExistingValue(literal -> {
            ResourceClassOutput classOutput = new ResourceClassOutput(literal.isApplicationClass, generateSources);
            createAnnotationLiteralClass(classOutput, literal, existingClasses);
            resources.addAll(classOutput.getResources());
        });
        return resources;
    }

    /**
     * Creator of an {@link AnnotationLiteralProcessor} must call this method at an appropriate point
     * in time and write the result to an appropriate output. If not, the bytecode sequences generated
     * using the {@code AnnotationLiteralProcessor} will refer to non-existing classes.
     *
     * @param existingClasses names of classes that already exist and should not be generated again
     * @return the generated classes, never {@code null}
     */
    Collection<Future<Collection<Resource>>> generate(ComputingCache<CacheKey, AnnotationLiteralClassInfo> cache,
            Set<String> existingClasses, ExecutorService executor) {
        List<Future<Collection<Resource>>> futures = new ArrayList<>();
        cache.forEachExistingValue(literal -> {
            futures.add(executor.submit(new Callable<Collection<Resource>>() {
                @Override
                public Collection<Resource> call() throws Exception {
                    ResourceClassOutput classOutput = new ResourceClassOutput(literal.isApplicationClass, generateSources);
                    createAnnotationLiteralClass(classOutput, literal, existingClasses);
                    return classOutput.getResources();
                }
            }));
        });
        return futures;
    }

    /**
     * Based on given {@code literal} data, generates an annotation literal class into the given {@code classOutput}.
     * Does nothing if {@code existingClasses} indicates that the class to be generated already exists.
     * <p>
     * The generated annotation literal class is supposed to have a constructor that accepts values
     * of all annotation members.
     *
     * @param classOutput the output to which the class is written
     * @param literal data about the annotation literal class to be generated
     * @param existingClasses set of existing classes that shouldn't be generated again
     */
    private void createAnnotationLiteralClass(ClassOutput classOutput, AnnotationLiteralClassInfo literal,
            Set<String> existingClasses) {

        String generatedName = literal.generatedClassName.replace('.', '/');
        if (existingClasses.contains(generatedName)) {
            return;
        }

        Gizmo gizmo = gizmo(classOutput);
        gizmo.class_(literal.generatedClassName, cc -> {
            cc.extends_(AbstractAnnotationLiteral.class);
            cc.implements_(classDescOf(literal.annotationClass));

            List<MethodInfo> annotationMembers = literal.annotationMembers();
            int annotationMembersCount = annotationMembers.size();
            boolean memberless = annotationMembers.isEmpty();

            FieldDesc[] fields = annotationMembers.stream()
                    .map(it -> cc.field(it.name(), fc -> {
                        fc.private_();
                        fc.final_();
                        fc.setType(classDescOf(it.returnType()));
                    }))
                    .toArray(FieldDesc[]::new);

            ConstructorDesc ctor = cc.constructor(mc -> {
                if (memberless) {
                    mc.private_();
                } else {
                    mc.public_();
                }

                ParamVar[] params = annotationMembers.stream()
                        .map(it -> mc.parameter(it.name(), classDescOf(it.returnType())))
                        .toArray(ParamVar[]::new);
                mc.body(bc -> {
                    bc.invokeSpecial(ConstructorDesc.of(AbstractAnnotationLiteral.class), cc.this_());
                    for (int i = 0; i < annotationMembersCount; i++) {
                        bc.set(cc.this_().field(fields[i]), params[i]);
                    }
                    bc.return_();
                });
            });

            cc.method("annotationType", mc -> {
                mc.public_();
                mc.returning(Class.class);
                mc.body(bc -> {
                    bc.return_(Const.of(classDescOf(literal.annotationClass)));
                });
            });

            for (int i = 0; i < annotationMembersCount; i++) {
                MethodInfo annotationMember = annotationMembers.get(i);
                FieldDesc field = fields[i];
                cc.method(annotationMember.name(), mc -> {
                    mc.public_();
                    mc.returning(classDescOf(annotationMember.returnType()));
                    mc.body(bc -> {
                        bc.return_(cc.this_().field(field));
                    });
                });
            }

            if (memberless) {
                cc.staticField("INSTANCE", fc -> {
                    fc.public_();
                    fc.final_();
                    fc.setType(cc.type());
                    fc.setInitializer(bc -> {
                        bc.yield(bc.new_(ctor));
                    });
                });
            } else {
                generateStaticFieldsWithDefaultValues(cc, annotationMembers);
            }

            generateEquals(cc, literal);
            generateHashCode(cc, literal);
            generateToString(cc, literal);
        });

        LOGGER.debugf("Annotation literal class generated: %s", literal.generatedClassName);
    }

    static String defaultValueStaticFieldName(MethodInfo annotationMember) {
        return annotationMember.name() + "_default_value";
    }

    private static boolean returnsClassOrClassArray(MethodInfo annotationMember) {
        boolean returnsClass = DotNames.CLASS.equals(annotationMember.returnType().name());
        boolean returnsClassArray = annotationMember.returnType().kind() == Type.Kind.ARRAY
                && DotNames.CLASS.equals(annotationMember.returnType().asArrayType().componentType().name());
        return returnsClass || returnsClassArray;
    }

    /**
     * Generates {@code public static final} fields for all the annotation members
     * that provide a default value and are of a class or class array type.
     * Also generates a static initializer that assigns the default value of those
     * annotation members to the generated fields.
     *
     * @param cc the class to which the fields and the static initializer should be added
     * @param annotationMembers the full set of annotation members of the annotation type
     */
    private static void generateStaticFieldsWithDefaultValues(ClassCreator cc, List<MethodInfo> annotationMembers) {
        List<MethodInfo> defaultOfClassType = new ArrayList<>();
        for (MethodInfo annotationMember : annotationMembers) {
            if (annotationMember.defaultValue() != null && returnsClassOrClassArray(annotationMember)) {
                defaultOfClassType.add(annotationMember);
            }
        }

        if (defaultOfClassType.isEmpty()) {
            return;
        }

        for (MethodInfo annotationMember : defaultOfClassType) {
            AnnotationValue defaultValue = annotationMember.defaultValue();

            cc.staticField(defaultValueStaticFieldName(annotationMember), fc -> {
                fc.public_();
                fc.final_();
                fc.setType(classDescOf(annotationMember.returnType()));
                fc.setInitializer(bc -> {
                    if (defaultValue.kind() == AnnotationValue.Kind.ARRAY) {
                        Type[] classes = defaultValue.asClassArray();
                        LocalVar array = bc.localVar("array", bc.newEmptyArray(Class.class, Const.of(classes.length)));
                        for (int i = 0; i < classes.length; i++) {
                            bc.set(array.elem(i), Const.of(classDescOf(classes[i])));
                        }
                        bc.yield(array);
                    } else {
                        bc.yield(Const.of(classDescOf(defaultValue.asClass())));
                    }
                });
            });
        }
    }

    // ---
    // note that `java.lang.annotation.Annotation` specifies exactly how `equals` and `hashCode` should work
    // (and there's also a recommendation for `toString`)

    private static final MethodDesc FLOAT_TO_INT_BITS = MethodDesc.of(Float.class, "floatToIntBits", int.class, float.class);
    private static final MethodDesc DOUBLE_TO_LONG_BITS = MethodDesc.of(Double.class, "doubleToLongBits", long.class,
            double.class);

    private static void generateEquals(ClassCreator cc, AnnotationLiteralClassInfo literal) {
        cc.method("equals", mc -> {
            mc.public_();
            mc.returning(boolean.class);
            ParamVar other = mc.parameter("other", Object.class);
            mc.body(bc -> {
                bc.if_(bc.eq(cc.this_(), other), BlockCreator::returnTrue);

                if (literal.annotationMembers().isEmpty()) {
                    // special case for memberless annotations
                    //
                    // a lot of people apparently use the construct `new AnnotationLiteral<MyAnnotation>() {}`
                    // to create an annotation literal for a memberless annotation, which is wrong, because
                    // the result doesn't implement the annotation interface
                    //
                    // yet, we handle that case here by doing what `AnnotationLiteral` does: instead of
                    // checking that the other object is an instance of the same annotation interface,
                    // as specified by the `Annotation.equals()` contract, we check that it implements
                    // the `Annotation` interface and have the same `annotationType()`
                    bc.ifNotInstanceOf(other, ClassDesc.of(Annotation.class.getName()), BlockCreator::returnFalse);
                    Expr thisAnnType = Const.of(classDescOf(literal.annotationClass));
                    Expr thatAnnType = bc.invokeInterface(MethodDesc.of(Annotation.class, "annotationType", Class.class),
                            other);
                    bc.return_(bc.objEquals(thisAnnType, thatAnnType));
                    return;
                }

                bc.ifNotInstanceOf(other, classDescOf(literal.annotationClass), BlockCreator::returnFalse);
                LocalVar that = bc.localVar("that", bc.cast(other, classDescOf(literal.annotationClass)));

                for (MethodInfo annotationMember : literal.annotationMembers()) {
                    ClassDesc type = classDescOf(annotationMember.returnType());

                    // for `this` object, can read directly from the field, that's what the method also does
                    FieldDesc field = FieldDesc.of(cc.type(), annotationMember.name(), type);
                    Supplier<Expr> thisValue = () -> cc.this_().field(field);

                    // for the other object, must invoke the method
                    Supplier<Expr> thatValue = () -> bc.invokeInterface(methodDescOf(annotationMember), that);

                    // type of the field (in this class) is the same as return type of the method (in both classes)
                    Expr cmp;
                    if (type.equals(ConstantDescs.CD_float)) {
                        Expr thisIntBits = bc.invokeStatic(FLOAT_TO_INT_BITS, thisValue.get());
                        Expr thatIntBits = bc.invokeStatic(FLOAT_TO_INT_BITS, thatValue.get());
                        cmp = bc.eq(thisIntBits, thatIntBits);
                    } else if (type.equals(ConstantDescs.CD_double)) {
                        Expr thisLongBits = bc.invokeStatic(DOUBLE_TO_LONG_BITS, thisValue.get());
                        Expr thatLongBits = bc.invokeStatic(DOUBLE_TO_LONG_BITS, thatValue.get());
                        cmp = bc.eq(thisLongBits, thatLongBits);
                    } else if (type.isArray()) {
                        cmp = bc.arrayEquals(thisValue.get(), thatValue.get());
                    } else if (type.isClassOrInterface()) {
                        // annotation members are never `null`
                        cmp = bc.withObject(thisValue.get()).equals_(thatValue.get());
                    } else {
                        cmp = bc.eq(thisValue.get(), thatValue.get());
                    }
                    bc.ifNot(cmp, BlockCreator::returnFalse);
                }
                bc.returnTrue();
            });
        });
    }

    private static void generateHashCode(ClassCreator cc, AnnotationLiteralClassInfo literal) {
        cc.method("hashCode", mc -> {
            mc.public_();
            mc.returning(int.class);
            mc.body(bc -> {
                if (literal.annotationMembers().isEmpty()) {
                    bc.return_(0);
                    return;
                }

                LocalVar result = bc.localVar("result", Const.of(0));
                for (MethodInfo annotationMember : literal.annotationMembers()) {
                    Expr memberNameHash = bc.mul(Const.of(127), bc.withObject(Const.of(annotationMember.name())).hashCode_());

                    ClassDesc type = classDescOf(annotationMember.returnType());
                    FieldDesc field = FieldDesc.of(cc.type(), annotationMember.name(), type);
                    FieldVar value = cc.this_().field(field);
                    Expr memberValueHash;
                    if (type.isArray()) {
                        memberValueHash = bc.arrayHashCode(value);
                    } else if (type.isClassOrInterface()) {
                        // annotation members are never `null`
                        memberValueHash = bc.withObject(value).hashCode_();
                    } else {
                        memberValueHash = bc.objHashCode(value);
                    }

                    Expr xor = bc.xor(memberNameHash, memberValueHash);
                    bc.addAssign(result, xor);
                }
                bc.return_(result);
            });
        });
    }

    // CDI's `AnnotationLiteral` has special cases for `String` and `Class` values
    // and wraps arrays into "{...}" instead of "[...]", but that's not necessary
    private static void generateToString(ClassCreator cc, AnnotationLiteralClassInfo literal) {
        cc.method("toString", mc -> {
            mc.public_();
            mc.returning(String.class);
            mc.body(bc -> {
                if (literal.annotationMembers().isEmpty()) {
                    // short-circuit for memberless annotations
                    bc.return_("@" + literal.annotationClass.name() + "()");
                    return;
                }

                StringBuilderGen str = StringBuilderGen.ofNew(bc);
                str.append("@" + literal.annotationClass.name() + '(');

                boolean first = true;
                for (MethodInfo annotationMember : literal.annotationMembers()) {
                    if (first) {
                        str.append(annotationMember.name() + "=");
                    } else {
                        str.append(", " + annotationMember.name() + "=");
                    }

                    ClassDesc type = classDescOf(annotationMember.returnType());
                    FieldDesc field = FieldDesc.of(cc.type(), annotationMember.name(), type);
                    FieldVar value = cc.this_().field(field);
                    str.append(type.isArray() ? bc.arrayToString(value) : value);

                    first = false;
                }

                str.append(')');
                bc.return_(str.toString_());
            });
        });
    }
}
