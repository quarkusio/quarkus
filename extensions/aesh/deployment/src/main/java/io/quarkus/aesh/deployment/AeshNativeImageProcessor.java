package io.quarkus.aesh.deployment;

import static io.quarkus.deployment.builditem.nativeimage.FfmType.ADDRESS;
import static io.quarkus.deployment.builditem.nativeimage.FfmType.INT;
import static io.quarkus.deployment.builditem.nativeimage.FfmType.LONG;

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
import io.quarkus.deployment.builditem.nativeimage.FfmDowncallBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

class AeshNativeImageProcessor {

    private static final Logger LOGGER = Logger.getLogger(AeshNativeImageProcessor.class);

    /**
     * Registers aesh service providers and resources for native images.
     * <p>
     * Aesh discovers {@code MetadataRegistry} implementations via a resource file
     * ({@code META-INF/aesh/registry}) before falling back to ServiceLoader.
     * <p>
     * Terminal providers ({@code TerminalProvider}) are discovered via ServiceLoader
     * to create the correct terminal connection (FFM-based PTY, exec-based PTY, etc.).
     * Without explicit registration, Quarkus's disabled auto-service-loader-registration
     * prevents terminal provider discovery, causing aesh-readline to fall back to
     * ExternalTerminal which uses buffered System.out and has no raw terminal mode.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProviders,
            BuildProducer<NativeImageResourceBuildItem> nativeResources) {
        serviceProviders.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                "org.aesh.command.metadata.CommandMetadataProvider"));
        serviceProviders.produce(ServiceProviderBuildItem.allProvidersFromClassPath(
                "org.aesh.terminal.provider.TerminalProvider"));
        nativeResources.produce(new NativeImageResourceBuildItem("META-INF/aesh/registry"));
    }

    /**
     * Registers FFM downcall signatures used by aesh-readline's {@code LibC} class
     * for POSIX terminal access (tcgetattr, tcsetattr, ioctl, read, poll, etc.).
     * <p>
     * GraalVM native image requires explicit registration of all FFM downcall
     * signatures. Without this, the FFM terminal provider fails at runtime with
     * {@code MissingForeignRegistrationError} and falls back to ExecPty.
     * <p>
     * Most LibC functions use {@code captureCallState} to capture {@code errno},
     * and {@code ioctl} additionally uses {@code firstVariadicArg}.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerFfmDowncalls(BuildProducer<FfmDowncallBuildItem> downcalls) {
        // int isatty(int fd) — no captureCallState
        downcalls.produce(FfmDowncallBuildItem.builder(INT, INT).build());
        // int open(const char *pathname, int flags)
        downcalls.produce(FfmDowncallBuildItem.builder(INT, ADDRESS, INT)
                .captureCallState().build());
        // int close(int fd)
        downcalls.produce(FfmDowncallBuildItem.builder(INT, INT)
                .captureCallState().build());
        // ssize_t read(int fd, void *buf, size_t count)
        downcalls.produce(FfmDowncallBuildItem.builder(LONG, INT, ADDRESS, LONG)
                .captureCallState().build());
        // int poll(struct pollfd *fds, nfds_t nfds, int timeout) — Linux: nfds_t=long
        downcalls.produce(FfmDowncallBuildItem.builder(INT, ADDRESS, LONG, INT)
                .captureCallState().build());
        // int poll(struct pollfd *fds, nfds_t nfds, int timeout) — macOS: nfds_t=int
        downcalls.produce(FfmDowncallBuildItem.builder(INT, ADDRESS, INT, INT)
                .captureCallState().build());
        // int tcgetattr(int fd, struct termios *termios_p)
        downcalls.produce(FfmDowncallBuildItem.builder(INT, INT, ADDRESS)
                .captureCallState().build());
        // int tcsetattr(int fd, int actions, const struct termios *termios_p)
        downcalls.produce(FfmDowncallBuildItem.builder(INT, INT, INT, ADDRESS)
                .captureCallState().build());
        // int ioctl(int fd, unsigned long request, void *arg) — variadic
        downcalls.produce(FfmDowncallBuildItem.builder(INT, INT, LONG, ADDRESS)
                .captureCallState().firstVariadicArg(2).build());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void reflectionConfiguration(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies,
            BuildProducer<NativeImageProxyDefinitionBuildItem> nativeImageProxies,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        IndexView index = combinedIndexBuildItem.getIndex();

        Collection<DotName> annotationsToAnalyze = Arrays.asList(
                DotName.createSimple("org.aesh.command.CommandDefinition"),
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
                    case CLASS -> foundClasses.add(target.asClass());
                    case FIELD -> foundClasses.add(target.asField().declaringClass());
                    case METHOD -> foundClasses.add(target.asMethod().declaringClass());
                    case METHOD_PARAMETER -> foundClasses.add(target.asMethodParameter().method().declaringClass());
                    default -> LOGGER.warnf("Unsupported type %s annotated with %s", target.kind().name(),
                            analyzedAnnotation);
                }

                // Register classes referenced in aesh annotations for reflection
                // (converter, completer, validator, activator, renderer, parser).
                // Use values() rather than valuesWithDefaults() because the aesh
                // annotation definitions are not in the Jandex index; default values
                // reference internal aesh classes that don't need reflection registration.
                List<AnnotationValue> values = ann.values();
                for (AnnotationValue value : values) {
                    switch (value.kind()) {
                        case CLASS -> typeAnnotationValues.add(value.asClass());
                        case ARRAY -> {
                            if (value.componentKind() == Kind.CLASS) {
                                Collections.addAll(typeAnnotationValues, value.asClassArray());
                            }
                        }
                        default -> {
                        }
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
        // InternalCommandMetadataRegistry is loaded by resource-file discovery via Class.forName().
        // The others are used as defaults or selected dynamically based on field types
        // (e.g. BooleanOptionCompleter for boolean fields, FileOptionCompleter for File fields).
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.aesh.command.internal.InternalCommandMetadataRegistry",
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

        // FfmTerminalProvider loads FfmPty via Class.forName() (multi-release JAR,
        // Java 22+). Register it for reflection so it can be instantiated in native.
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(
                "org.aesh.terminal.tty.impl.FfmPty")
                .methods().build());

        // FFM terminal classes use java.lang.foreign.Linker in static initializers
        // to create downcall handles for POSIX functions (tcgetattr, ioctl, etc.)
        // and Windows console functions (GetStdHandle, etc.). These must be
        // initialized at runtime, not captured at build time inside the container
        // which has no TTY and may produce stale FFM state.
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "org.aesh.terminal.tty.impl.LibC"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "org.aesh.terminal.tty.impl.PosixConstants"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "org.aesh.terminal.tty.impl.FfmPty"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "org.aesh.terminal.tty.impl.WinConsoleNative"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "org.aesh.terminal.tty.impl.WinSysTerminal"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "org.aesh.terminal.tty.impl.WinSysTerminal$Handles"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "org.aesh.terminal.tty.impl.AbstractWindowsTerminal"));
        runtimeInitializedClasses.produce(new RuntimeInitializedClassBuildItem(
                "org.aesh.terminal.tty.impl.AbstractWindowsTerminal$ConsoleOutput"));
    }

}
