package io.quarkus.aesh.deployment;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.AnnotationValue.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

class AeshNativeImageProcessor {

    private static final Logger LOGGER = Logger.getLogger(AeshNativeImageProcessor.class);

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void reflectionConfiguration(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies,
            BuildProducer<NativeImageProxyDefinitionBuildItem> nativeImageProxies) {
        IndexView index = combinedIndexBuildItem.getIndex();

        Collection<DotName> annotationsToAnalyze = Arrays.asList(
                DotName.createSimple("org.aesh.command.CommandDefinition"),
                DotName.createSimple("org.aesh.command.GroupCommandDefinition"),
                DotName.createSimple("org.aesh.command.option.Option"),
                DotName.createSimple("org.aesh.command.option.OptionList"),
                DotName.createSimple("org.aesh.command.option.OptionGroup"),
                DotName.createSimple("org.aesh.command.option.Arguments"),
                DotName.createSimple("org.aesh.command.option.Argument"),
                DotName.createSimple("org.aesh.command.option.ParentCommand"));

        Set<ClassInfo> foundClasses = new HashSet<>();
        Set<Type> typeAnnotationValues = new HashSet<>();

        for (DotName analyzedAnnotation : annotationsToAnalyze) {
            for (AnnotationInstance ann : index.getAnnotations(analyzedAnnotation)) {
                AnnotationTarget target = ann.target();
                switch (target.kind()) {
                    case CLASS:
                        foundClasses.add(target.asClass());
                        break;
                    case FIELD:
                        foundClasses.add(target.asField().declaringClass());
                        break;
                    case METHOD:
                        foundClasses.add(target.asMethod().declaringClass());
                        break;
                    case METHOD_PARAMETER:
                        foundClasses.add(target.asMethodParameter().method().declaringClass());
                        break;
                    default:
                        LOGGER.warnf("Unsupported type %s annotated with %s", target.kind().name(),
                                analyzedAnnotation);
                        break;
                }

                // Register classes referenced in aesh annotations for reflection
                // (converter, completer, validator, activator, renderer, parser).
                // Use values() rather than valuesWithDefaults() because the aesh
                // annotation definitions are not in the Jandex index; default values
                // reference internal aesh classes that don't need reflection registration.
                List<AnnotationValue> values = ann.values();
                for (AnnotationValue value : values) {
                    if (value.kind() == Kind.CLASS) {
                        typeAnnotationValues.add(value.asClass());
                    } else if (value.kind() == Kind.ARRAY && value.componentKind() == Kind.CLASS) {
                        Collections.addAll(typeAnnotationValues, value.asClassArray());
                    }
                }
            }
        }

        // Register both declared methods and fields as they are accessed by aesh during initialization
        foundClasses.forEach(classInfo -> {
            if (Modifier.isInterface(classInfo.flags())) {
                nativeImageProxies
                        .produce(new NativeImageProxyDefinitionBuildItem(classInfo.name().toString()));
                reflectiveClasses.produce(ReflectiveClassBuildItem.builder(classInfo.name().toString())
                        .constructors(false).methods().fields().build());
            } else {
                reflectiveClasses.produce(ReflectiveClassBuildItem.builder(classInfo.name().toString())
                        .methods().fields().build());
            }
        });

        typeAnnotationValues.forEach(type -> reflectiveHierarchies.produce(ReflectiveHierarchyBuildItem
                .builder(type)
                .source(AeshNativeImageProcessor.class.getSimpleName())
                .ignoreFieldPredicate(fi -> true)
                .ignoreMethodPredicate(mi -> true)
                .build()));

        // Register aesh internal classes that are instantiated reflectively at runtime.
        // These are used as defaults or selected dynamically based on field types
        // (e.g. BooleanOptionCompleter for boolean fields, FileOptionCompleter for File fields).
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.aesh.command.impl.completer.BooleanOptionCompleter",
                "org.aesh.command.impl.completer.FileOptionCompleter",
                "org.aesh.command.impl.completer.NullOptionCompleter",
                "org.aesh.command.impl.converter.NullConverter",
                "org.aesh.command.impl.validator.NullValidator",
                "org.aesh.command.impl.validator.NullCommandValidator",
                "org.aesh.command.impl.activator.NullActivator",
                "org.aesh.command.impl.activator.NullCommandActivator",
                "org.aesh.command.impl.renderer.NullOptionRenderer",
                "org.aesh.command.impl.parser.AeshOptionParser",
                "org.aesh.AeshConsoleRunner$ExitCommand")
                .build());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    NativeImageResourceBuildItem terminalInfoCompResources() {
        // InfoCmp.readDefaultInfoCmp() loads terminal capability files via
        // Class.getResourceAsStream(). Include them in the native image so
        // remote terminal connections (SSH, WebSocket) can resolve terminal
        // capabilities for xterm, vt100, screen, etc.
        return new NativeImageResourceBuildItem(
                "org/aesh/terminal/utils/xterm_caps.src",
                "org/aesh/terminal/utils/xterm-256color_caps.src",
                "org/aesh/terminal/utils/screen-256color_caps.src",
                "org/aesh/terminal/utils/vt100_caps.src",
                "org/aesh/terminal/utils/ansi_caps.src",
                "org/aesh/terminal/utils/windows_caps.src");
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    RuntimeInitializedPackageBuildItem runtimeInitJlineNative() {
        // The aesh readline jar bundles jline-native classes that contain
        // platform-specific JNI wrappers (e.g. Kernel32 on Windows).
        // These fail to initialize at build time on other platforms.
        return new RuntimeInitializedPackageBuildItem("org.jline.nativ");
    }
}
