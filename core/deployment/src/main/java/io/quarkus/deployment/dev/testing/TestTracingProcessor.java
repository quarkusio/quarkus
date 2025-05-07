package io.quarkus.deployment.dev.testing;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.aesh.command.Command;
import org.aesh.command.CommandDefinition;
import org.aesh.command.CommandException;
import org.aesh.command.CommandResult;
import org.aesh.command.GroupCommandDefinition;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.option.Argument;
import org.aesh.command.option.Arguments;
import org.aesh.command.option.Option;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Tag;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.IsTest;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ConsoleCommandBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.console.QuarkusCommand;
import io.quarkus.deployment.console.SetCompleter;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.dev.testing.ContinuousTestingSharedStateManager;
import io.quarkus.dev.testing.KnownTags;
import io.quarkus.dev.testing.TracingHandler;
import io.quarkus.gizmo.Gizmo;

/**
 * processor that instruments test and application classes to trace the code path that is in use during a test run.
 * <p>
 * This allows for fine-grained running of tests when a file changes.
 */
public class TestTracingProcessor {

    @BuildStep(onlyIfNot = IsNormal.class)
    LogCleanupFilterBuildItem handle() {
        return new LogCleanupFilterBuildItem("org.junit.platform.launcher.core.EngineDiscoveryOrchestrator", "0 containers");
    }

    static volatile boolean testingSetup;

    @BuildStep
    TestListenerBuildItem sharedStateListener() {
        return new TestListenerBuildItem(new ContinuousTestingSharedStateListener());
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    @Produce(LogHandlerBuildItem.class)
    @Produce(TestSetupBuildItem.class)
    @Produce(ServiceStartBuildItem.class)
    void startTesting(TestConfig config, LiveReloadBuildItem liveReloadBuildItem,
            LaunchModeBuildItem launchModeBuildItem, List<TestListenerBuildItem> testListenerBuildItems) {
        if (TestSupport.instance().isEmpty() || config.continuousTesting() == TestConfig.Mode.DISABLED
                || config.flatClassPath()) {
            return;
        }
        DevModeType devModeType = launchModeBuildItem.getDevModeType().orElse(null);
        if (devModeType == null || !devModeType.isContinuousTestingSupported()) {
            return;
        }
        if (testingSetup) {
            return;
        }
        testingSetup = true;
        TestSupport testSupport = TestSupport.instance().get();
        for (TestListenerBuildItem i : testListenerBuildItems) {
            testSupport.addListener(i.listener);
        }
        testSupport.setConfig(config);
        testSupport.setTags(config.includeTags().orElse(Collections.emptyList()),
                config.excludeTags().orElse(Collections.emptyList()));
        testSupport.setPatterns(config.includePattern().orElse(null),
                config.excludePattern().orElse(null));
        String specificSelection = System.getProperty("quarkus-internal.test.specific-selection");
        if (specificSelection != null) {
            testSupport.setSpecificSelection(specificSelection);
        }
        testSupport.setEngines(config.includeEngines().orElse(Collections.emptyList()),
                config.excludeEngines().orElse(Collections.emptyList()));
        testSupport.setConfiguredDisplayTestOutput(config.displayTestOutput());
        testSupport.setTestType(config.type());
        if (!liveReloadBuildItem.isLiveReload()) {
            if (config.continuousTesting() == TestConfig.Mode.ENABLED) {
                testSupport.start();
            } else if (config.continuousTesting() == TestConfig.Mode.PAUSED) {
                testSupport.stop();
            }
        }
        QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
        ((QuarkusClassLoader) cl.parent()).addCloseTask(ContinuousTestingSharedStateManager::reset);
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
                transformerProducer.produce(new BytecodeTransformerBuildItem.Builder()
                        .setClassToTransform(theClassName)
                        .setVisitorFunction(
                                new BiFunction<String, ClassVisitor, ClassVisitor>() {
                                    @Override
                                    public ClassVisitor apply(String s, ClassVisitor classVisitor) {
                                        return new TracingClassVisitor(classVisitor, theClassName);
                                    }
                                })
                        .setCacheable(true)
                        .setContinueOnFailure(true)
                        .build());
            }
        }

    }

    @BuildStep(onlyIf = IsTest.class)
    public ServiceStartBuildItem searchForTags(CombinedIndexBuildItem combinedIndexBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        if (!launchModeBuildItem.isAuxiliaryApplication()) {
            return null;
        }

        Set<String> ret = new HashSet<>();
        for (AnnotationInstance clazz : combinedIndexBuildItem.getIndex()
                .getAnnotations(DotName.createSimple(Tag.class.getName()))) {
            ret.add(clazz.value().asString());
        }
        KnownTags.setKnownTags(ret);
        return null;
    }

    public boolean isAppClass(String className) {
        return QuarkusClassLoader.isApplicationClass(className);
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

    @BuildStep
    ConsoleCommandBuildItem testConsoleCommand(CombinedIndexBuildItem indexBuildItem) {
        return new ConsoleCommandBuildItem(new TestCommand());
    }

    @GroupCommandDefinition(name = "test", description = "Test Commands", groupCommands = { TagsCommand.class,
            PatternCommand.class }, generateHelp = true)
    public static class TestCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @GroupCommandDefinition(name = "tags", description = "Tag Commands", groupCommands = { IncludeTagsCommand.class,
            ExcludeTagsCommand.class }, generateHelp = true)
    public static class TagsCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "include", description = "Sets the current included tags, this supports JUnit tag expressions.")
    public static class IncludeTagsCommand extends TestSelectionCommand {

        @Arguments(completer = TagCompleter.class)
        private List<String> tags;

        @Override
        protected String configValue() {
            if (tags == null) {
                return "";
            } else {
                return String.join(",", tags);
            }
        }

        @Override
        protected String configKey() {
            return "quarkus.test.include-tags";
        }

        @Override
        protected void configure(TestSupport testSupport) {
            testSupport.setTags(tags == null ? List.of() : tags, testSupport.excludeTags);
        }
    }

    @CommandDefinition(name = "exclude", description = "Sets the current excluded tags, this supports JUnit tag expressions.")
    public static class ExcludeTagsCommand extends TestSelectionCommand {

        @Arguments(completer = TagCompleter.class)
        private List<String> tags;

        @Override
        protected String configValue() {
            if (tags == null) {
                return "";
            } else {
                return String.join(",", tags);
            }
        }

        @Override
        protected String configKey() {
            return "quarkus.test.exclude-tags";
        }

        @Override
        protected void configure(TestSupport testSupport) {
            testSupport.setTags(testSupport.includeTags, tags == null ? List.of() : tags);
        }
    }

    @GroupCommandDefinition(name = "pattern", description = "Include/Exclude pattern Commands", groupCommands = {
            IncludePatternCommand.class,
            ExcludePatternCommand.class }, generateHelp = true)
    public static class PatternCommand implements Command {

        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            return CommandResult.SUCCESS;
        }
    }

    @CommandDefinition(name = "include", description = "Sets the current include pattern")
    public static class IncludePatternCommand extends TestSelectionCommand {

        @Argument
        private String pattern;

        @Override
        protected String configValue() {
            return Objects.requireNonNullElse(pattern, "");
        }

        @Override
        protected String configKey() {
            return "quarkus.test.include-pattern";
        }

        @Override
        protected void configure(TestSupport testSupport) {
            testSupport.setPatterns(pattern, testSupport.exclude != null ? testSupport.exclude.pattern() : null);
        }
    }

    @CommandDefinition(name = "exclude", description = "Sets the current exclude pattern")
    public static class ExcludePatternCommand extends TestSelectionCommand {

        @Argument(completer = TagCompleter.class)
        private String pattern;

        @Override
        protected String configValue() {
            return Objects.requireNonNullElse(pattern, "");
        }

        @Override
        protected String configKey() {
            return "quarkus.test.exclude-pattern";
        }

        @Override
        protected void configure(TestSupport testSupport) {
            testSupport.setPatterns(testSupport.include != null ? testSupport.include.pattern() : null, pattern);
        }
    }

    static abstract class TestSelectionCommand extends QuarkusCommand {

        @Option(shortName = 'p', hasValue = false)
        protected boolean persistent;

        @Option(shortName = 'r', hasValue = false)
        protected boolean run;

        protected abstract String configValue();

        protected abstract String configKey();

        @Override
        public final CommandResult doExecute(CommandInvocation commandInvocation)
                throws CommandException, InterruptedException {
            TestSupport testSupport = TestSupport.instance().get();
            configure(testSupport);
            if (persistent) {
                CurrentConfig.EDITOR.accept(Map.of(configKey(), configValue()));
            }
            if (run) {
                if (!testSupport.isStarted()) {
                    testSupport.start();
                }
                testSupport.runAllTests();
            }
            return CommandResult.SUCCESS;
        }

        protected abstract void configure(TestSupport testSupport);
    }

    public static class TagCompleter extends SetCompleter {

        @Override
        protected Set<String> allOptions(String soFar) {
            return KnownTags.getKnownTags();
        }
    }

}
