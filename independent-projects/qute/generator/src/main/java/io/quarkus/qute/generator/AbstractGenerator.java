package io.quarkus.qute.generator;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;

public abstract class AbstractGenerator {

    protected final Set<String> generatedTypes;
    protected final IndexView index;
    protected final Gizmo gizmo;

    protected AbstractGenerator(IndexView index, ClassOutput classOutput) {
        this.generatedTypes = new HashSet<>();
        this.index = index;
        this.gizmo = Gizmo.create(classOutput)
                .withDebugInfo(false)
                .withParameters(false);
    }

    public Set<String> getGeneratedTypes() {
        return generatedTypes;
    }

    protected void completeBoolean(BlockCreator bc, Expr result) {
        bc.ifElse(result, whenTrue -> {
            whenTrue.return_(Expr.staticField(Descriptors.RESULTS_TRUE));
        }, whenFalse -> {
            whenFalse.return_(Expr.staticField(Descriptors.RESULTS_FALSE));
        });
    }

    protected boolean isEnum(Type returnType) {
        if (returnType.kind() != org.jboss.jandex.Type.Kind.CLASS) {
            return false;
        }
        ClassInfo maybeEnum = index.getClassByName(returnType.name());
        return maybeEnum != null && maybeEnum.isEnum();
    }

    protected boolean hasCompletionStage(Type type) {
        return !skipMemberType(type) && hasCompletionStageInTypeClosure(index.getClassByName(type.name()), index);
    }

    protected boolean hasCompletionStageInTypeClosure(ClassInfo classInfo,
            IndexView index) {
        return hasClassInTypeClosure(classInfo, DotNames.COMPLETION_STAGE, index);
    }

    protected boolean hasClassInTypeClosure(ClassInfo classInfo, DotName className,
            IndexView index) {

        if (classInfo == null) {
            // TODO cannot perform analysis
            return false;
        }
        if (classInfo.name().equals(className)) {
            return true;
        }
        // Interfaces
        for (Type interfaceType : classInfo.interfaceTypes()) {
            ClassInfo interfaceClassInfo = index.getClassByName(interfaceType.name());
            if (interfaceClassInfo != null && hasCompletionStageInTypeClosure(interfaceClassInfo, index)) {
                return true;
            }
        }
        // Superclass
        if (classInfo.superClassType() != null) {
            ClassInfo superClassInfo = index.getClassByName(classInfo.superName());
            if (superClassInfo != null && hasClassInTypeClosure(superClassInfo, className, index)) {
                return true;
            }
        }
        return false;
    }

    protected void processReturnVal(BlockCreator bc, Type type, Expr val, ClassCreator classCreator) {
        if (hasCompletionStage(type)) {
            bc.return_(val);
        } else {
            // Try to use some shared CompletedStage constants
            if (type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE
                    && type.asPrimitiveType().primitive() == Primitive.BOOLEAN) {
                completeBoolean(bc, val);
            } else if (type.name().equals(DotNames.BOOLEAN)) {
                LocalVar boolVal = bc.localVar("bv", val);
                bc.ifNull(boolVal, isNull -> {
                    isNull.return_(isNull.getStaticField(Descriptors.RESULTS_NULL));
                });
                completeBoolean(bc, bc.invokeVirtual(Descriptors.BOOLEAN_VALUE, boolVal));
            } else {
                bc.return_(bc.invokeStatic(Descriptors.COMPLETED_STAGE_OF, val));
            }
        }
    }

    protected boolean skipMemberType(Type type) {
        switch (type.kind()) {
            case VOID:
            case PRIMITIVE:
            case ARRAY:
            case TYPE_VARIABLE:
            case UNRESOLVED_TYPE_VARIABLE:
            case TYPE_VARIABLE_REFERENCE:
            case WILDCARD_TYPE:
                return true;
            default:
                return false;
        }
    }

}
