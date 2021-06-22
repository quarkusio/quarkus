package io.quarkus.deployment.dev.testing;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.jboss.jandex.ClassInfo;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.dev.console.ConsoleHelper;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.testing.TracingHandler;
import io.quarkus.gizmo.Gizmo;

/**
 * processor that instruments test and application classes to trace the code path that is in use during a test run.
 * <p>
 * This allows for fine grained running of tests when a file changes.
 */
public class TestTracingProcessor {

    private static boolean consoleInstalled = false;

    @BuildStep(onlyIfNot = IsNormal.class)
    LogCleanupFilterBuildItem handle() {
        return new LogCleanupFilterBuildItem("org.junit.platform.launcher.core.EngineDiscoveryOrchestrator", "0 containers");
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Produce(TestSetupBuildItem.class)
    void setupConsole(TestConfig config, BuildProducer<TestListenerBuildItem> testListenerBuildItemBuildProducer,
            LaunchModeBuildItem launchModeBuildItem) {
        if (!TestSupport.instance().isPresent() || config.continuousTesting == TestConfig.Mode.DISABLED
                || config.flatClassPath) {
            return;
        }
        if (consoleInstalled) {
            return;
        }
        consoleInstalled = true;
        if (config.console) {
            ConsoleHelper.installConsole(config);
            TestConsoleHandler consoleHandler = new TestConsoleHandler(launchModeBuildItem.getDevModeType().get());
            consoleHandler.install();
            testListenerBuildItemBuildProducer.produce(new TestListenerBuildItem(consoleHandler));
        }
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    @Produce(LogHandlerBuildItem.class)
    @Produce(TestSetupBuildItem.class)
    @Produce(ServiceStartBuildItem.class)
    void startTesting(TestConfig config, LiveReloadBuildItem liveReloadBuildItem,
            LaunchModeBuildItem launchModeBuildItem, List<TestListenerBuildItem> testListenerBuildItems) {
        if (!TestSupport.instance().isPresent() || config.continuousTesting == TestConfig.Mode.DISABLED
                || config.flatClassPath) {
            return;
        }
        DevModeType devModeType = launchModeBuildItem.getDevModeType().orElse(null);
        if (devModeType == null || !devModeType.isContinuousTestingSupported()) {
            return;
        }
        TestSupport testSupport = TestSupport.instance().get();
        for (TestListenerBuildItem i : testListenerBuildItems) {
            testSupport.addListener(i.listener);
        }
        testSupport.setTags(config.includeTags.orElse(Collections.emptyList()),
                config.excludeTags.orElse(Collections.emptyList()));
        testSupport.setPatterns(config.includePattern.orElse(null),
                config.excludePattern.orElse(null));
        testSupport.setConfiguredDisplayTestOutput(config.displayTestOutput);
        testSupport.setTestType(config.type);
        if (!liveReloadBuildItem.isLiveReload()) {
            if (config.continuousTesting == TestConfig.Mode.ENABLED) {
                testSupport.start();
            } else if (config.continuousTesting == TestConfig.Mode.PAUSED) {
                testSupport.stop();
            }
        }

        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
        ((QuarkusClassLoader) cl.parent()).addCloseTask(new Runnable() {
            @Override
            public void run() {
                testSupport.stop();
            }
        });
    }

    @BuildStep(onlyIf = IsTest.class)
    public void instrumentTestClasses(CombinedIndexBuildItem combinedIndexBuildItem,
            LaunchModeBuildItem launchModeBuildItem,
            BuildProducer<BytecodeTransformerBuildItem> transformerProducer) {
        if (!launchModeBuildItem.isAuxiliaryApplication()) {
            return;
        }
        for (ClassInfo clazz : combinedIndexBuildItem.getIndex().getKnownClasses()) {
            String theClassName = clazz.name().toString();
            if (isAppClass(theClassName)) {
                transformerProducer.produce(new BytecodeTransformerBuildItem(false, theClassName,
                        new BiFunction<String, ClassVisitor, ClassVisitor>() {
                            @Override
                            public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                                return new TracingClassVisitor(classVisitor, theClassName);
                            }
                        }, true));
            }
        }

    }

    public boolean isAppClass(String theClassName) {
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread()
                .getContextClassLoader();
        //if the class file is present in this (and not the parent) CL then it is an application class
        List<ClassPathElement> res = cl
                .getElementsWithResource(theClassName.replace(".", "/") + ".class", true);
        return !res.isEmpty();
    }

    public static class TracingClassVisitor extends ClassVisitor {
        private final String theClassName;

        public TracingClassVisitor(ClassVisitor classVisitor, String theClassName) {
            super(Gizmo.ASM_API_VERSION, classVisitor);
            this.theClassName = theClassName;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (name.equals("<init>") || name.equals("<clinit>")) {
                return mv;
            }
            return new MethodVisitor(Gizmo.ASM_API_VERSION, mv) {
                @Override
                public void visitCode() {
                    super.visitCode();
                    visitLdcInsn(theClassName);
                    visitMethodInsn(Opcodes.INVOKESTATIC,
                            TracingHandler.class.getName().replace(".", "/"), "trace",
                            "(Ljava/lang/String;)V", false);
                }
            };
        }
    }
}
