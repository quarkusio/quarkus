package io.quarkus.deployment.dev;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.deployment.builditem.ConsoleFormatterBannerBuildItem;
import io.quarkus.deployment.dev.testing.TestHandler;
import io.quarkus.deployment.dev.testing.TestSetupBuildItem;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.steps.ClassTransformingBuildStep;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.runner.bootstrap.AugmentActionImpl;
import io.quarkus.runtime.Quarkus;

/**
 * The main entry point of quarkus:test
 */
public class IsolatedTestModeMain extends IsolatedDevModeMain {

    private volatile DevModeContext context;

    private final List<HotReplacementSetup> hotReplacementSetups = new ArrayList<>();
    static volatile Throwable deploymentProblem;
    private static volatile CuratedApplication curatedApplication;
    private static volatile AugmentAction augmentAction;

    private RuntimeUpdatesProcessor setupRuntimeCompilation(DevModeContext context, Path applicationRoot)
            throws Exception {
        System.setProperty("quarkus.test.display-test-output", "true");
        if (!context.getAllModules().isEmpty()) {
            ServiceLoader<CompilationProvider> serviceLoader = ServiceLoader.load(CompilationProvider.class);
            List<CompilationProvider> compilationProviders = new ArrayList<>();
            for (CompilationProvider provider : serviceLoader) {
                compilationProviders.add(provider);
                context.getAllModules().forEach(moduleInfo -> moduleInfo.addSourcePaths(provider.handledSourcePaths()));
            }
            QuarkusCompiler compiler;
            try {
                compiler = new QuarkusCompiler(curatedApplication, compilationProviders, context);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create compiler", e);
            }
            TestSupport testSupport = new TestSupport(curatedApplication, compilationProviders, context);
            RuntimeUpdatesProcessor processor = new RuntimeUpdatesProcessor(applicationRoot, context, compiler,
                    DevModeType.TEST_ONLY, this::regenerateApplication,
                    new BiConsumer<DevModeContext.ModuleInfo, String>() {
                        @Override
                        public void accept(DevModeContext.ModuleInfo moduleInfo, String s) {
                        }
                    }, new BiFunction<String, byte[], byte[]>() {
                        @Override
                        public byte[] apply(String s, byte[] bytes) {
                            return ClassTransformingBuildStep.transform(s, bytes);
                        }
                    }, testSupport);

            for (HotReplacementSetup service : ServiceLoader.load(HotReplacementSetup.class,
                    curatedApplication.getBaseRuntimeClassLoader())) {
                hotReplacementSetups.add(service);
                service.setupHotDeployment(processor);
                processor.addHotReplacementSetup(service);
            }
            return processor;
        }
        return null;
    }

    void regenerateApplication(Set<String> ignore, ClassScanResult ignore2) {
    }

    public void close() {
        try {
            try {
                RuntimeUpdatesProcessor.INSTANCE.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (HotReplacementSetup i : hotReplacementSetups) {
                i.close();
            }
        } finally {
            curatedApplication.close();
        }

    }

    //the main entry point, but loaded inside the augmentation class loader
    @Override
    public void accept(CuratedApplication o, Map<String, Object> params) {
        Timing.staticInitStarted(o.getBaseRuntimeClassLoader(), false);
        try {
            curatedApplication = o;
            Object potentialContext = params.get(DevModeContext.class.getName());
            if (potentialContext instanceof DevModeContext) {
                context = (DevModeContext) potentialContext;
            } else {
                //this was from the external class loader
                //we need to copy it into this one
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ObjectOutputStream oo = new ObjectOutputStream(out);
                oo.writeObject(potentialContext);
                context = (DevModeContext) new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
            }
            augmentAction = new AugmentActionImpl(curatedApplication);
            RuntimeUpdatesProcessor.INSTANCE = setupRuntimeCompilation(context, (Path) params.get(APP_ROOT));

            if (RuntimeUpdatesProcessor.INSTANCE != null) {
                RuntimeUpdatesProcessor.INSTANCE.checkForFileChange();
                RuntimeUpdatesProcessor.INSTANCE.checkForChangedClasses(true);
            }
            try {
                augmentAction.performCustomBuild(TestHandler.class.getName(), null, TestSetupBuildItem.class.getName(),
                        LoggingSetupBuildItem.class.getName(), ConsoleFormatterBannerBuildItem.class.getName());
            } catch (Throwable t) {
                //logging may not have been started, this is more reliable
                System.err.println("Failed to start quarkus test mode");
                t.printStackTrace();
                System.exit(1);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (DevModeMain.class) {
                        try {
                            close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, "Quarkus Shutdown Thread"));
            Quarkus.waitForExit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
