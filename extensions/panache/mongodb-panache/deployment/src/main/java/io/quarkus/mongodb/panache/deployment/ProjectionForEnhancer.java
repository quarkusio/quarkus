package io.quarkus.mongodb.panache.deployment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import io.quarkus.gizmo.Gizmo;

public class ProjectionForEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {
    private static final String BSONPROPERTY_BINARY_NAME = "org/bson/codecs/pojo/annotations/BsonProperty";
    private static final String BSONPROPERTY_SIGNATURE = "L" + BSONPROPERTY_BINARY_NAME + ";";

    private Map<String, String> propertyMapping;

    public ProjectionForEnhancer(Map<String, String> propertyMapping) {
        this.propertyMapping = propertyMapping;
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor classVisitor) {
        return new BsonPropertyClassVisitor(classVisitor, propertyMapping);
    }

    static class BsonPropertyClassVisitor extends ClassVisitor {
        Map<String, String> propertyMapping;

        BsonPropertyClassVisitor(ClassVisitor outputClassVisitor, Map<String, String> propertyMapping) {
            super(Gizmo.ASM_API_VERSION, outputClassVisitor);
            this.propertyMapping = propertyMapping;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            FieldVisitor superVisitor = super.visitField(access, name, descriptor, signature, value);
            if (this.propertyMapping.containsKey(name)) {
                return new FieldVisitor(Gizmo.ASM_API_VERSION, superVisitor) {
                    private Set<String> descriptors = new HashSet<>();

                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        descriptors.add(descriptor);
                        return super.visitAnnotation(descriptor, visible);
                    }

                    @Override
                    public void visitEnd() {
                        if (!descriptors.contains(BSONPROPERTY_SIGNATURE)) {
                            AnnotationVisitor visitor = super.visitAnnotation(BSONPROPERTY_SIGNATURE, true);
                            visitor.visit("value", propertyMapping.get(name));
                            visitor.visitEnd();
                        }
                        super.visitEnd();
                    }
                };
            }
            return superVisitor;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor superVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (this.propertyMapping.containsKey(name)) {
                return new MethodVisitor(Gizmo.ASM_API_VERSION, superVisitor) {
                    private Set<String> descriptors = new HashSet<>();

                    @Override
                    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                        descriptors.add(descriptor);
                        return super.visitAnnotation(descriptor, visible);
                    }

                    @Override
                    public void visitEnd() {
                        if (!descriptors.contains(BSONPROPERTY_SIGNATURE)) {
                            AnnotationVisitor visitor = super.visitAnnotation(BSONPROPERTY_SIGNATURE, true);
                            visitor.visit("value", propertyMapping.get(name));
                            visitor.visitEnd();
                        }
                        super.visitEnd();
                    }
                };
            }
            return superVisitor;
        }
    }
}
