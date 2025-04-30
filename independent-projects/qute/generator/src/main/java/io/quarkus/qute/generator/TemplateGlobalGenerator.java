package io.quarkus.qute.generator;

import static io.quarkus.qute.generator.ValueResolverGenerator.generatedNameFromTarget;
import static io.quarkus.qute.generator.ValueResolverGenerator.packageName;
import static io.quarkus.qute.generator.ValueResolverGenerator.simpleName;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type.Kind;

import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.FunctionCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.Switch.StringSwitch;
import io.quarkus.qute.EvalContext;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.qute.TemplateGlobalProvider;
import io.quarkus.qute.TemplateInstance;

/**
 * Generates {@link TemplateInstance.Initializer}s for {@link TemplateGlobal} annotations.
 */
public class TemplateGlobalGenerator extends AbstractGenerator {

    public static final DotName TEMPLATE_GLOBAL = DotName.createSimple(TemplateGlobal.class.getName());
    public static final String NAME = "name";

    public static final String SUFFIX = "_Globals";

    private final String namespace;
    private int priority;

    public TemplateGlobalGenerator(ClassOutput classOutput, String namespace, int initialPriority, IndexView index) {
        super(index, classOutput);
        this.namespace = namespace;
        this.priority = initialPriority;
    }

    public String generate(ClassInfo declaringClass, Map<String, AnnotationTarget> targets) {

        String baseName;
        if (declaringClass.enclosingClass() != null) {
            baseName = simpleName(declaringClass.enclosingClass()) + ValueResolverGenerator.NESTED_SEPARATOR
                    + simpleName(declaringClass);
        } else {
            baseName = simpleName(declaringClass);
        }
        String targetPackage = packageName(declaringClass.name());
        String generatedName = generatedNameFromTarget(targetPackage, baseName, SUFFIX);
        String generatedClassName = generatedName.replace('/', '.');
        generatedTypes.add(generatedClassName);

        ClassCreator provider = ClassCreator.builder().classOutput(classOutput).className(generatedName)
                .interfaces(TemplateGlobalProvider.class).build();

        // TemplateInstance.Initializer#accept()
        MethodCreator accept = provider.getMethodCreator("accept", void.class, Object.class)
                .setModifiers(ACC_PUBLIC);

        for (Entry<String, AnnotationTarget> entry : targets.entrySet()) {
            ResultHandle name = accept.load(entry.getKey());
            FunctionCreator fun = accept.createFunction(Function.class);
            BytecodeCreator funBytecode = fun.getBytecode();
            ResultHandle global;
            switch (entry.getValue().kind()) {
                case FIELD:
                    FieldInfo field = entry.getValue().asField();
                    validate(field);
                    global = funBytecode.readStaticField(FieldDescriptor.of(field));
                    break;
                case METHOD:
                    MethodInfo method = entry.getValue().asMethod();
                    validate(method);
                    global = funBytecode.invokeStaticMethod(MethodDescriptor.of(method));
                    break;
                default:
                    throw new IllegalStateException("Unsupported target: " + entry.getValue());
            }
            funBytecode.returnValue(global);
            // Global variables are computed lazily
            accept.invokeInterfaceMethod(Descriptors.TEMPLATE_INSTANCE_COMPUTED_DATA, accept.getMethodParam(0), name,
                    fun.getInstance());
        }
        accept.returnValue(null);

        // NamespaceResolver#getNamespace()
        MethodCreator getNamespace = provider.getMethodCreator("getNamespace", String.class);
        getNamespace.returnValue(getNamespace.load(namespace));

        // WithPriority#getPriority()
        MethodCreator getPriority = provider.getMethodCreator("getPriority", int.class);
        // Namespace resolvers for the same namespace may not share the same priority
        // So we increase the initial priority for each provider
        getPriority.returnValue(getPriority.load(priority++));

        // Resolver#resolve()
        MethodCreator resolve = provider.getMethodCreator("resolve", CompletionStage.class, EvalContext.class)
                .setModifiers(ACC_PUBLIC);
        ResultHandle evalContext = resolve.getMethodParam(0);
        ResultHandle name = resolve.invokeInterfaceMethod(Descriptors.GET_NAME, evalContext);
        StringSwitch nameSwitch = resolve.stringSwitch(name);
        for (Entry<String, AnnotationTarget> e : targets.entrySet()) {
            Consumer<BytecodeCreator> readGlobal = new Consumer<BytecodeCreator>() {
                @Override
                public void accept(BytecodeCreator bc) {
                    switch (e.getValue().kind()) {
                        case FIELD:
                            FieldInfo field = e.getValue().asField();
                            processReturnVal(bc, field.type(), bc.readStaticField(FieldDescriptor.of(field)), provider);
                            break;
                        case METHOD:
                            MethodInfo method = e.getValue().asMethod();
                            processReturnVal(bc, method.returnType(), bc.invokeStaticMethod(MethodDescriptor.of(method)),
                                    provider);
                            break;
                        default:
                            throw new IllegalStateException("Unsupported target: " + e.getValue());
                    }

                }
            };
            nameSwitch.caseOf(e.getKey(), readGlobal);
        }
        resolve.returnValue(resolve.invokeStaticMethod(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));

        provider.close();
        return generatedClassName;
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
        if (!method.parameterTypes().isEmpty()) {
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
