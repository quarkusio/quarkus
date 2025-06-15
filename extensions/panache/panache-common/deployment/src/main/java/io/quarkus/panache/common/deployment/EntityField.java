package io.quarkus.panache.common.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.deployment.bean.JavaBeanUtil;

public class EntityField {

    public final String name;
    public final String descriptor;
    public final Visibility visibility;
    public final String librarySpecificGetterName;
    public final String librarySpecificSetterName;

    public String signature;
    public final Set<EntityFieldAnnotation> annotations = new HashSet<>(2);

    public EntityField(String name, String descriptor, Visibility visibility) {
        this(name, descriptor, visibility, null, null);
    }

    public EntityField(String name, String descriptor, Visibility visibility, String librarySpecificGetterName,
            String librarySpecificSetterName) {
        this.name = name;
        this.descriptor = descriptor;
        this.visibility = visibility;
        this.librarySpecificGetterName = librarySpecificGetterName;
        this.librarySpecificSetterName = librarySpecificSetterName;
    }

    public String getGetterName() {
        return JavaBeanUtil.getGetterName(name, descriptor);
    }

    public String getSetterName() {
        return JavaBeanUtil.getSetterName(name);
    }

    public boolean hasAnnotation(String descriptor) {
        for (EntityFieldAnnotation fieldAnnotation : annotations) {
            if (fieldAnnotation.descriptor.equals(descriptor)) {
                return true;
            }
        }
        return false;
    }

    public enum Visibility {
        PUBLIC(Opcodes.ACC_PUBLIC),
        PROTECTED(Opcodes.ACC_PROTECTED),
        PACKAGE_PRIVATE(0),
        PRIVATE(Opcodes.ACC_PRIVATE);

        public final int accessOpCode;

        Visibility(int accessOpCode) {
            this.accessOpCode = accessOpCode;
        }

        public static Visibility get(int flags) {
            if (Modifier.isPublic(flags)) {
                return PUBLIC;
            } else if (Modifier.isPrivate(flags)) {
                return PRIVATE;
            } else if (Modifier.isProtected(flags)) {
                return PROTECTED;
            } else {
                return PACKAGE_PRIVATE;
            }
        }
    }

    public static class EntityFieldAnnotation {
        public final String descriptor;
        public final Map<String, Object> attributes = new HashMap<>(2);
        public final List<EntityFieldAnnotation> nestedAnnotations = new ArrayList<>(2);

        public EntityFieldAnnotation(String desc) {
            this.descriptor = desc;
        }

        public void writeToVisitor(MethodVisitor mv) {
            AnnotationVisitor av = mv.visitAnnotation(descriptor, true);
            for (Entry<String, Object> e : attributes.entrySet()) {
                av.visit(e.getKey(), e.getValue());
            }
            if (!nestedAnnotations.isEmpty()) {
                // Always visit nested annotations as an array because this covers JAX-B annotations
                AnnotationVisitor nestedVisitor = av.visitArray("value");
                for (EntityFieldAnnotation nestedAnno : nestedAnnotations) {
                    AnnotationVisitor arrayAnnoVisitor = nestedVisitor.visitAnnotation(null, nestedAnno.descriptor);
                    for (Entry<String, Object> e : nestedAnno.attributes.entrySet()) {
                        arrayAnnoVisitor.visit(e.getKey(), e.getValue());
                    }
                    arrayAnnoVisitor.visitEnd();
                }
                nestedVisitor.visitEnd();
            }
            av.visitEnd();
        }
    }

}
