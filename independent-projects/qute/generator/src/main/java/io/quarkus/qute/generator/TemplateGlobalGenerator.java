package io.quarkus.qute.generator;

import static io.quarkus.qute.generator.ValueResolverGenerator.generatedNameFromTarget;
import static io.quarkus.qute.generator.ValueResolverGenerator.packageName;
import static io.quarkus.qute.generator.ValueResolverGenerator.simpleName;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.qute.TemplateInstance;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type.Kind;

/**
 * Generates {@link TemplateInstance.Initializer}s for {@link TemplateGlobal} annotations.
 */
public class TemplateGlobalGenerator {

    public static final DotName TEMPLATE_GLOBAL = DotName.createSimple(TemplateGlobal.class.getName());
    public static final String NAME = "name";

    public static final String SUFFIX = "_Globals";

    private final Set<String> generatedTypes;
    private final ClassOutput classOutput;

    public TemplateGlobalGenerator(ClassOutput classOutput) {
        this.generatedTypes = new HashSet<>();
        this.classOutput = classOutput;
    }

    public void generate(ClassInfo declaringClass, Map<String, AnnotationTarget> targets) {

        String baseName;
        if (declaringClass.enclosingClass() != null) {
            baseName = simpleName(declaringClass.enclosingClass()) + ValueResolverGenerator.NESTED_SEPARATOR
                    + simpleName(declaringClass);
        } else {
            baseName = simpleName(declaringClass);
        }
        String targetPackage = packageName(declaringClass.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, SUFFIX);
        generatedTypes.add(generatedName.replace('/', '.'));

        ClassCreator initializer = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(TemplateInstance.Initializer.class).build();

        MethodCreator accept = initializer.getMethodCreator("accept", void.class, Object.class)
                .setModifiers(ACC_PUBLIC);

        for (Entry<String, AnnotationTarget> entry : targets.entrySet()) {
            ResultHandle name = accept.load(entry.getKey());
            ResultHandle global;
            switch (entry.getValue().kind()) {
                case FIELD:
                    FieldInfo field = entry.getValue().asField();
                    validate(field);
                    global = accept.readStaticField(FieldDescriptor.of(field));
                    break;
                case METHOD:
                    MethodInfo method = entry.getValue().asMethod();
                    validate(method);
                    global = accept.invokeStaticMethod(MethodDescriptor.of(method));
                    break;
                default:
                    throw new IllegalStateException("Unsupported target: " + entry.getValue());
            }
            accept.invokeInterfaceMethod(Descriptors.TEMPLATE_INSTANCE_DATA, accept.getMethodParam(0), name, global);
        }
        accept.returnValue(null);
        initializer.close();
    }

    public Set<String> getGeneratedTypes() {
        return generatedTypes;
    }

    public static void validate(MethodInfo method) {
        if (!Modifier.isStatic(method.flags())) {
            throw new IllegalStateException(
                    "Global variable method declared on " + method.declaringClass().name() + " must be static: "
                            + method);
        }
        if (method.returnType().kind() == Kind.VOID) {
            throw new IllegalStateException("Global variable method declared on " + method.declaringClass().name()
                    + " must not return void: " + method);
        }
        if (!method.parameters().isEmpty()) {
            throw new IllegalStateException("Global variable method declared on " + method.declaringClass().name()
                    + " must not accept any parameter: " + method);
        }
        if (Modifier.isPrivate(method.flags())) {
            throw new IllegalStateException("Global variable method declared on " + method.declaringClass().name()
                    + " must not be private: " + method);
        }
    }

    public static void validate(FieldInfo field) {
        if (!Modifier.isStatic(field.flags())) {
            throw new IllegalStateException(
                    "Global variable field declared on " + field.declaringClass().name() + "  must be static: " + field);
        }
        if (Modifier.isPrivate(field.flags())) {
            throw new IllegalStateException("Global variable field declared on " + field.declaringClass().name()
                    + " must not be private: " + field);
        }
    }
}
