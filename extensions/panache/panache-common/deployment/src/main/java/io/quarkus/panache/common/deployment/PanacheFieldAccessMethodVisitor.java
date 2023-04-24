package io.quarkus.panache.common.deployment;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.gizmo.Gizmo;

public class PanacheFieldAccessMethodVisitor extends MethodVisitor {

    private final String methodName;
    private final String methodOwnerClassName;
    private final String methodDescriptor;
    private final MetamodelInfo modelInfo;

    public PanacheFieldAccessMethodVisitor(MethodVisitor methodVisitor, String methodOwner,
            String methodName, String methodDescriptor,
            MetamodelInfo modelInfo) {
        super(Gizmo.ASM_API_VERSION, methodVisitor);
        this.methodOwnerClassName = methodOwner.replace('/', '.');
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.modelInfo = modelInfo;
    }

    @Override
    public void visitFieldInsn(int opcode, String fieldOwner, String fieldName, String descriptor) {
        String fieldOwnerClassName = fieldOwner.replace('/', '.');
        if ( // we only care about non-static access
        !(opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD)
                // if we're in the constructor, do not replace field accesses to this type and its supertypes
                // otherwise we risk running setters that depend on initialisation
                || (this.methodName.equals("<init>")
                        && targetIsInHierarchy(methodOwnerClassName, fieldOwnerClassName))) {
            // In those cases, don't do anything.
            super.visitFieldInsn(opcode, fieldOwner, fieldName, descriptor);
            return;
        }

        EntityField entityField = getEntityField(fieldOwnerClassName, fieldName);
        if (entityField == null) {
            // Not an entity field: don't do anything.
            super.visitFieldInsn(opcode, fieldOwner, fieldName, descriptor);
            return;
        }

        String javaBeanMethodName;
        String librarySpecificMethodName;
        String methodDescriptor;
        if (opcode == Opcodes.GETFIELD) {
            javaBeanMethodName = entityField.getGetterName();
            librarySpecificMethodName = entityField.librarySpecificGetterName;
            methodDescriptor = "()" + descriptor;
        } else {
            javaBeanMethodName = entityField.getSetterName();
            librarySpecificMethodName = entityField.librarySpecificSetterName;
            methodDescriptor = "(" + descriptor + ")V";
        }

        // For public fields, we'll replace field access with (potentially generated) JavaBean accessors.
        // For non-public fields, we won't be using JavaBean accessors,
        // because we might not generate any and pre-existing ones might have unwanted side effects.
        // Instead, we will replace the field access with a call to internal field access methods, if any,
        // e.g. generated methods like $$_hibernate_read_myProperty()
        boolean useJavaBeanAccessor = EntityField.Visibility.PUBLIC.equals(entityField.visibility);
        if (!useJavaBeanAccessor && librarySpecificMethodName == null) {
            // Field is non-public, so we won't be using JavaBean accessors,
            // and there are no library-specific accessors:
            // don't do anything.
            super.visitFieldInsn(opcode, fieldOwner, fieldName, descriptor);
            return;
        }
        String methodName = useJavaBeanAccessor ? javaBeanMethodName : librarySpecificMethodName;

        if (fieldOwnerClassName.equals(this.methodOwnerClassName)
                && javaBeanMethodName.equals(this.methodName)
                && methodDescriptor.equals(this.methodDescriptor)) {
            // The current method accessing the entity field is the corresponding getter/setter.
            // We don't perform substitution at all.
            super.visitFieldInsn(opcode, fieldOwner, fieldName, descriptor);
        } else {
            // The current method accessing the entity field is *not* the corresponding getter/setter.
            // We found a relevant field access: replace it with a call to the getter/setter.
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, fieldOwner, methodName, methodDescriptor, false);
        }
    }

    /**
     * Make sure that the target class is in the (superclass, since interfaces have no fields)
     * hierarchy of the current class
     */
    private boolean targetIsInHierarchy(String currentClass, String targetClass) {
        if (currentClass.equals(targetClass))
            return true;
        EntityModel entityModel = modelInfo.getEntityModel(currentClass);
        if (entityModel == null)
            return false;
        if (entityModel.superClassName != null)
            return targetIsInHierarchy(entityModel.superClassName, targetClass);
        return false;
    }

    /**
     * Checks that the given field belongs to an entity (any entity)
     */
    EntityField getEntityField(String className, String fieldName) {
        EntityModel entityModel = modelInfo.getEntityModel(className);
        if (entityModel == null)
            return null;
        EntityField field = entityModel.fields.get(fieldName);
        if (field != null)
            return field;
        if (entityModel.superClassName != null)
            return getEntityField(entityModel.superClassName, fieldName);
        return null;
    }
}
