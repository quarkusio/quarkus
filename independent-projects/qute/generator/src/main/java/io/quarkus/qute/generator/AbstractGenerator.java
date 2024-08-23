package io.quarkus.qute.generator;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.PrimitiveType.Primitive;
import org.jboss.jandex.Type;

import io.quarkus.gizmo.BranchResult;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.IfThenElse;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.qute.CompletedStage;

public abstract class AbstractGenerator {

    protected final Set<String> generatedTypes;
    protected final IndexView index;
    protected final ClassOutput classOutput;

    protected AbstractGenerator(IndexView index, ClassOutput classOutput) {
        this.generatedTypes = new HashSet<>();
        this.index = index;
        this.classOutput = classOutput;
    }

    public Set<String> getGeneratedTypes() {
        return generatedTypes;
    }

    protected void completeBoolean(BytecodeCreator bc, ResultHandle result) {
        BranchResult isTrue = bc.ifTrue(result);
        BytecodeCreator trueBranch = isTrue.trueBranch();
        trueBranch.returnValue(trueBranch.readStaticField(Descriptors.RESULTS_TRUE));
        BytecodeCreator falseBranch = isTrue.falseBranch();
        falseBranch.returnValue(falseBranch.readStaticField(Descriptors.RESULTS_FALSE));
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

    protected void processReturnVal(BytecodeCreator bc, Type type, ResultHandle ret, ClassCreator classCreator) {
        if (hasCompletionStage(type)) {
            bc.returnValue(ret);
        } else {
            // Try to use some shared CompletedStage constants
            if (type.kind() == org.jboss.jandex.Type.Kind.PRIMITIVE
                    && type.asPrimitiveType().primitive() == Primitive.BOOLEAN) {
                completeBoolean(bc, ret);
            } else if (type.name().equals(DotNames.BOOLEAN)) {
                BytecodeCreator isNull = bc.ifNull(ret).trueBranch();
                isNull.returnValue(isNull.readStaticField(Descriptors.RESULTS_NULL));
                completeBoolean(bc, bc.invokeVirtualMethod(Descriptors.BOOLEAN_VALUE, ret));
            } else if (isEnum(type)) {
                BytecodeCreator isNull = bc.ifNull(ret).trueBranch();
                isNull.returnValue(isNull.readStaticField(Descriptors.RESULTS_NULL));
                completeEnum(index.getClassByName(type.name()), classCreator, ret, bc);
            } else {
                bc.returnValue(bc.invokeStaticMethod(Descriptors.COMPLETED_STAGE_OF, ret));
            }
        }
    }

    protected boolean completeEnum(ClassInfo enumClass, ClassCreator valueResolver, ResultHandle result, BytecodeCreator bc) {
        IfThenElse ifThenElse = null;
        for (FieldInfo enumConstant : enumClass.enumConstants()) {
            String name = enumClass.name().toString().replace(".", "_") + "$$"
                    + enumConstant.name();
            FieldDescriptor enumConstantField = FieldDescriptor.of(enumClass.name().toString(),
                    enumConstant.name(), enumClass.name().toString());

            // Additional methods and fields are generated for enums that are part of the index
            // We don't care about visibility and atomicity here
            // private CompletedStage org_acme_MyEnum$$CONSTANT;
            FieldDescriptor csField = valueResolver
                    .getFieldCreator(name, CompletedStage.class).setModifiers(ACC_PRIVATE)
                    .getFieldDescriptor();
            // private CompletedStage org_acme_MyEnum$$CONSTANT() {
            //    if (org_acme_MyEnum$$CONSTANT == null) {
            //        org_acme_MyEnum$$CONSTANT = CompletedStage.of(MyEnum.CONSTANT);
            //    }
            //    return org_acme_MyEnum$$CONSTANT;
            // }
            MethodCreator enumConstantMethod = valueResolver.getMethodCreator(name,
                    CompletedStage.class).setModifiers(ACC_PRIVATE);
            BytecodeCreator isNull = enumConstantMethod.ifNull(enumConstantMethod
                    .readInstanceField(csField, enumConstantMethod.getThis()))
                    .trueBranch();
            ResultHandle val = isNull.readStaticField(enumConstantField);
            isNull.writeInstanceField(csField, enumConstantMethod.getThis(),
                    isNull.invokeStaticMethod(Descriptors.COMPLETED_STAGE_OF, val));
            enumConstantMethod.returnValue(enumConstantMethod
                    .readInstanceField(csField, enumConstantMethod.getThis()));

            // Unfortunately, we can't use the BytecodeCreator#enumSwitch() here because the enum class is not loaded
            // if(val.equals(MyEnum.CONSTANT))
            //    return org_acme_MyEnum$$CONSTANT();
            BytecodeCreator match;
            if (ifThenElse == null) {
                ifThenElse = bc.ifThenElse(
                        Gizmo.equals(bc, bc.readStaticField(enumConstantField), result));
                match = ifThenElse.then();
            } else {
                match = ifThenElse.elseIf(
                        b -> Gizmo.equals(b, b.readStaticField(enumConstantField), result));
            }
            match.returnValue(match.invokeVirtualMethod(
                    enumConstantMethod.getMethodDescriptor(), match.getThis()));
        }
        return true;
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
