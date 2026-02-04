package io.quarkus.deployment.steps;

import static io.quarkus.gizmo2.desc.Descs.MD_Class;
import static io.quarkus.gizmo2.desc.Descs.MD_Collection;
import static io.quarkus.gizmo2.desc.Descs.MD_Thread;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeSystemProperties;

import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsafeAccessedFieldBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.LocalesBuildTimeConfig;
import io.quarkus.runtime.graal.GraalVM;

public class NativeImageFeatureStep {

    public static final String GRAAL_FEATURE = "io.quarkus.runner.Feature";

    private static final MethodDesc BUILD_TIME_INITIALIZATION = MethodDesc.of(RuntimeClassInitialization.class,
            "initializeAtBuildTime", void.class, String[].class);
    private static final MethodDesc REGISTER_RUNTIME_SYSTEM_PROPERTIES = MethodDesc.of(RuntimeSystemProperties.class,
            "register", void.class, String.class, String.class);
    private static final MethodDesc GRAALVM_VERSION_GET_CURRENT = MethodDesc.of(GraalVM.Version.class, "getCurrent",
            GraalVM.Version.class);
    private static final MethodDesc GRAALVM_VERSION_COMPARE_TO = MethodDesc.of(GraalVM.Version.class, "compareTo", int.class,
            int[].class);
    private static final MethodDesc INITIALIZE_CLASSES_AT_RUN_TIME = MethodDesc.of(RuntimeClassInitialization.class,
            "initializeAtRunTime", void.class, Class[].class);
    private static final MethodDesc INITIALIZE_PACKAGES_AT_RUN_TIME = MethodDesc.of(RuntimeClassInitialization.class,
            "initializeAtRunTime", void.class, String[].class);
    private static final MethodDesc PRINT_STACK_TRACE = MethodDesc.of(Throwable.class, "printStackTrace", void.class);
    private static final MethodDesc GET_DECLARED_FIELD = MethodDesc.of(Class.class, "getDeclaredField", Field.class,
            String.class);
    private static final MethodDesc REGISTER_AS_UNSAFE_ACCESSED = MethodDesc.of(Feature.BeforeAnalysisAccess.class,
            "registerAsUnsafeAccessed", void.class, Field.class);
    private static final MethodDesc GET_CONTEXT_CLASS_LOADER = MethodDesc.of(Thread.class, "getContextClassLoader",
            ClassLoader.class);

    @BuildStep
    void addExportsToNativeImage(BuildProducer<JPMSExportBuildItem> features) {
        // required in order to access org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport
        // prior to 23.1 the class was provided by org.graalvm.sdk module and with 23.1 onwards, it's provided by org.graalvm.nativeimage instead
        features.produce(new JPMSExportBuildItem("org.graalvm.sdk", "org.graalvm.nativeimage.impl", null,
                GraalVM.Version.VERSION_23_1_0));
        features.produce(new JPMSExportBuildItem("org.graalvm.nativeimage", "org.graalvm.nativeimage.impl",
                GraalVM.Version.VERSION_23_1_0));
    }

    @BuildStep
    void generateFeature(BuildProducer<GeneratedNativeImageClassBuildItem> nativeImageClass,
            List<RuntimeInitializedClassBuildItem> runtimeInitializedClassBuildItems,
            List<RuntimeInitializedPackageBuildItem> runtimeInitializedPackageBuildItems,
            List<RuntimeReinitializedClassBuildItem> runtimeReinitializedClassBuildItems,
            List<UnsafeAccessedFieldBuildItem> unsafeAccessedFields,
            NativeConfig nativeConfig,
            LocalesBuildTimeConfig localesBuildTimeConfig) {

        Gizmo g = Gizmo.create(new GeneratedClassGizmo2Adaptor(
                item -> nativeImageClass
                        .produce(new GeneratedNativeImageClassBuildItem(item.binaryName(), item.getClassData())),
                item -> {
                },
                false));

        g.class_(GRAAL_FEATURE, cc -> {
            cc.implements_(Feature.class);

            cc.defaultConstructor();

            cc.method("getDescription", mc -> {
                mc.returning(String.class);
                mc.body(b0 -> b0.return_(Const.of("Auto-generated class by Quarkus from the existing extensions")));
            });

            cc.method("duringSetup", mc -> {
                mc.parameter("access", Feature.DuringSetupAccess.class);
                mc.body(b0 -> {
                    b0.invokeStatic(BUILD_TIME_INITIALIZATION, b0.newArray(String.class, Const.of("")));
                    b0.return_();
                });
            });

            cc.method("beforeAnalysis", mc -> {
                ParamVar access = mc.parameter("access", Feature.BeforeAnalysisAccess.class);
                MethodDesc classForName3 = MethodDesc.of(Class.class, "forName", Class.class, String.class, boolean.class,
                        ClassLoader.class);
                mc.body(b0 -> {
                    b0.try_(t1 -> {
                        t1.body(b2 -> {
                            LocalVar cl = b2.localVar("cl", b2.invokeVirtual(MD_Class.getClassLoader, Const.of(cc.type())));
                            if (localesBuildTimeConfig.defaultLocale().isPresent()) {
                                Locale defaultLocale = localesBuildTimeConfig.defaultLocale().get();
                                b2.invokeStatic(REGISTER_RUNTIME_SYSTEM_PROPERTIES, Const.of("user.language"),
                                        Const.of(defaultLocale.getLanguage()));
                                b2.invokeStatic(REGISTER_RUNTIME_SYSTEM_PROPERTIES, Const.of("user.country"),
                                        Const.of(defaultLocale.getCountry()));
                            } else {
                                LocalVar graalVMVersion = b2.localVar("graalVMVersion",
                                        b2.invokeStatic(GRAALVM_VERSION_GET_CURRENT));
                                /* GraalVM >= 24.2 */
                                b2.if_(b2.gt(
                                        b2.invokeVirtual(GRAALVM_VERSION_COMPARE_TO,
                                                graalVMVersion,
                                                b2.newArray(int.class, Const.of(24), Const.of(2))),
                                        0), t3 -> {
                                            t3.invokeStatic(REGISTER_RUNTIME_SYSTEM_PROPERTIES, Const.of("user.language"),
                                                    Const.of("en"));
                                            t3.invokeStatic(REGISTER_RUNTIME_SYSTEM_PROPERTIES, Const.of("user.country"),
                                                    Const.of("US"));
                                        });
                            }
                            if (!runtimeInitializedClassBuildItems.isEmpty()
                                    || !runtimeReinitializedClassBuildItems.isEmpty()) {
                                b2.block(b3 -> {
                                    // use an arraylist so we don't have null entries
                                    LocalVar classes = b3.localVar("classes", b3.new_(ArrayList.class,
                                            Const.of(runtimeInitializedClassBuildItems.size()
                                                    + runtimeReinitializedClassBuildItems.size())));
                                    Stream.concat(
                                            runtimeInitializedClassBuildItems.stream()
                                                    .map(RuntimeInitializedClassBuildItem::getClassName),
                                            runtimeReinitializedClassBuildItems.stream()
                                                    .map(RuntimeReinitializedClassBuildItem::getClassName))
                                            .forEach(name -> b3.try_(t4 -> {
                                                t4.body(b5 -> b5.invokeInterface(MD_Collection.add, classes,
                                                        b5.invokeStatic(classForName3, Const.of(name), Const.of(false), cl)));
                                                t4.catch_(Throwable.class, "t",
                                                        (b5, t) -> b5.invokeVirtual(PRINT_STACK_TRACE, t));
                                            }));
                                    b3.invokeStatic(INITIALIZE_CLASSES_AT_RUN_TIME,
                                            b3.cast(
                                                    b3.invokeVirtual(
                                                            MethodDesc.of(ArrayList.class, "toArray", Object[].class,
                                                                    Object[].class),
                                                            classes,
                                                            b3.newEmptyArray(Class.class,
                                                                    b3.invokeInterface(MD_Collection.size, classes))),
                                                    Class[].class));
                                });
                            }
                            if (!runtimeInitializedPackageBuildItems.isEmpty()) {
                                b2.block(b3 -> {
                                    b3.invokeStatic(INITIALIZE_PACKAGES_AT_RUN_TIME, b3.newArray(String.class,
                                            runtimeInitializedPackageBuildItems.stream()
                                                    .map(RuntimeInitializedPackageBuildItem::getPackageName)
                                                    .map(Const::of)
                                                    .toArray(Expr[]::new)));
                                });
                            }
                            // Ensure registration of fields being accessed through unsafe is done last to ensure that the class
                            // initialization configuration is done first.  Registering the fields before configuring class initialization
                            // may results in classes being marked for runtime initialization even if not explicitly requested.
                            if (!unsafeAccessedFields.isEmpty()) {
                                for (UnsafeAccessedFieldBuildItem unsafeAccessedField : unsafeAccessedFields) {
                                    b2.try_(t3 -> {
                                        t3.body(b4 -> {
                                            LocalVar declaringClass = b4.localVar("declaringClass",
                                                    b4.invokeStatic(classForName3,
                                                            Const.of(unsafeAccessedField.getDeclaringClass()), Const.of(false),
                                                            cl));
                                            LocalVar declaredField = b4.localVar("declaredField",
                                                    b4.invokeVirtual(GET_DECLARED_FIELD, declaringClass,
                                                            Const.of(unsafeAccessedField.getFieldName())));
                                            b4.invokeInterface(REGISTER_AS_UNSAFE_ACCESSED, access, declaredField);
                                        });
                                        t3.catch_(Throwable.class, "t", (b4, t) -> b4.invokeVirtual(PRINT_STACK_TRACE, t));
                                    });
                                }
                            }
                        });
                        t1.catch_(Throwable.class, "t", (b2, t) -> b2.invokeVirtual(PRINT_STACK_TRACE, t));
                    });
                    // todo: do not use TCCL here (it's incorrect)
                    Expr tccl = b0.invokeVirtual(GET_CONTEXT_CLASS_LOADER, b0.invokeStatic(MD_Thread.currentThread));
                    b0.invokeStatic(classForName3, Const.of("io.quarkus.runner.ApplicationImpl"), Const.of(false), tccl);
                    b0.return_();
                });
            });
        });
    }

}
