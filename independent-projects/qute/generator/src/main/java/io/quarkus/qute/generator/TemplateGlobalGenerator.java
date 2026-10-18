package io.quarkus.qute.generator;

import static io.quarkus.qute.generator.ValueResolverGenerator.generatedNameFromTarget;
import static io.quarkus.qute.generator.ValueResolverGenerator.packageName;
import static io.quarkus.qute.generator.ValueResolverGenerator.simpleName;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.fieldDescOf;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.methodDescOf;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type.Kind;

import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
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

        gizmo.class_(generatedClassName, cc -> {
            cc.implements_(TemplateGlobalProvider.class);
            cc.defaultConstructor();

            cc.method("accept", mc -> {
                mc.returning(void.class);
                ParamVar templateInstance = mc.parameter("input", Object.class);

                mc.body(bc -> {
                    for (Entry<String, AnnotationTarget> entry : targets.entrySet()) {
                        var name = Const.of(entry.getKey());
                        Expr fun = bc.lambda(Function.class, lc -> {
                            @SuppressWarnings("unused")
                            ParamVar key = lc.parameter("k", 0);
                            lc.body(lbc -> {
                                Expr global;
                                switch (entry.getValue().kind()) {
                                    case FIELD:
                                        FieldInfo field = entry.getValue().asField();
                                        validate(field);
                                        global = Expr.staticField(fieldDescOf(field));
                                        break;
                                    case METHOD:
                                        MethodInfo method = entry.getValue().asMethod();
                                        validate(method);
                                        global = lbc.invokeStatic(methodDescOf(method));
                                        break;
                                    default:
                                        throw new IllegalStateException("Unsupported target: " + entry.getValue());
                                }
                                lbc.return_(global);
                            });
                        });
                        // Global variables are computed lazily
                        bc.invokeInterface(Descriptors.TEMPLATE_INSTANCE_COMPUTED_DATA, templateInstance, name, fun);
                    }
                    bc.return_();
                });
            });

            // NamespaceResolver#getNamespace()
            cc.method("getNamespace", mc -> {
                mc.returning(String.class);
                mc.body(bc -> bc.return_(Const.of(namespace)));
            });

            // WithPriority#getPriority()
            cc.method("getPriority", mc -> {
                mc.returning(int.class);
                // Namespace resolvers for the same namespace may not share the same priority
                // So we increase the initial priority for each provider
                mc.body(bc -> bc.return_(Const.of(priority++)));
            });

            // Resolver#resolve()
            cc.method("resolve", mc -> {
                mc.returning(CompletionStage.class);
                ParamVar evalContext = mc.parameter("evalContext", EvalContext.class);

                mc.body(bc -> {
                    Expr name = bc.invokeInterface(Descriptors.GET_NAME, evalContext);
                    bc.switch_(name, sc -> {
                        for (Entry<String, AnnotationTarget> e : targets.entrySet()) {
                            sc.caseOf(Const.of(e.getKey()), cbc -> {
                                switch (e.getValue().kind()) {
                                    case FIELD:
                                        FieldInfo field = e.getValue().asField();
                                        LocalVar val = cbc.localVar("val", cbc.getStaticField(fieldDescOf(field)));
                                        processReturnVal(cbc, field.type(), val, cc);
                                        break;
                                    case METHOD:
                                        MethodInfo method = e.getValue().asMethod();
                                        LocalVar val2 = cbc.localVar("val", cbc.invokeStatic(methodDescOf(method)));
                                        processReturnVal(cbc, method.returnType(), val2, cc);
                                        break;
                                    default:
                                        throw new IllegalStateException("Unsupported target: " + e.getValue());
                                }
                            });
                        }
                        sc.default_(dbc -> {
                        });
                    });
                    bc.return_(bc.invokeStatic(Descriptors.RESULTS_NOT_FOUND_EC, evalContext));
                });
            });
        });
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
