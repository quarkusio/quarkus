/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.logging.deployment;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logmanager.EmbeddedConfigurator;
import org.jboss.logmanager.formatters.ColorPatternFormatter;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.ConsoleHandler;
import org.jboss.logmanager.handlers.FileHandler;
import org.jboss.protean.gizmo.BranchResult;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.ClassOutput;
import org.jboss.protean.gizmo.FieldDescriptor;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.logging.runtime.LogSetupTemplate;
import org.objectweb.asm.Opcodes;

/**
 */
public final class LoggingResourceProcessor implements ResourceProcessor {

    private static final String GENERATED_CONFIGURATOR = "org/jboss/logmanager/GeneratedConfigurator";

    public void process(final ArchiveContext archiveContext, final ProcessorContext processorContext) throws Exception {
        final BuildConfig config = archiveContext.getBuildConfig();
        final BuildConfig.ConfigNode loggingNode = config.getApplicationConfig().get("logging");
        final BuildConfig.ConfigNode enableNode = loggingNode.get("enable");
        if (! enableNode.isNull() && enableNode.asBoolean() == Boolean.FALSE) {
            // forget the whole thing
            return;
        }

        final String rootMinLevel;
        final String rootLevel;
        final boolean consoleEnable;
        final String consoleFormat;
        final String consoleLevel;
        final boolean consoleColor;
        final boolean fileEnable;
        final String fileFormat;
        final String fileLevel;
        final String filePath;
        final MethodCreator minimumLevelOf;
        final MethodCreator levelOf;
        final MethodCreator handlersOf;
        try (ClassCreator cc = new ClassCreator(new ProcessorClassOutput(processorContext), GENERATED_CONFIGURATOR, null, "java/lang/Object", "org/jboss/logmanager/EmbeddedConfigurator")) {
            // TODO set source file
            final MethodCreator ctor = cc.getMethodCreator("<init>", void.class);
            ctor.setModifiers(Opcodes.ACC_PUBLIC);
            ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
            ctor.returnValue(null);

            final BuildConfig.ConfigNode rootLoggerNode = loggingNode.get("root");
            final BuildConfig.ConfigNode rootMinLevelNode = rootLoggerNode.get("min-level");
            rootMinLevel = rootMinLevelNode.isNull() ? "INFO" : rootMinLevelNode.asString();
            final BuildConfig.ConfigNode rootLevelNode = rootLoggerNode.get("level");
            rootLevel = rootLevelNode.isNull() ? rootMinLevel : rootLevelNode.asString();

            final BuildConfig.ConfigNode consoleNode = loggingNode.get("console");
            final BuildConfig.ConfigNode consoleEnableNode = consoleNode.get("enable");
            consoleEnable = consoleEnableNode.isNull() || consoleEnableNode.asBoolean().booleanValue();
            final BuildConfig.ConfigNode consoleFormatNode = consoleNode.get("format");
            consoleFormat = consoleFormatNode.isNull() ? "%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{1.}] (%t) %s%e%n" : consoleFormatNode.asString();
            final BuildConfig.ConfigNode consoleLevelNode = consoleNode.get("level");
            consoleLevel = consoleLevelNode.isNull() ? "INFO" : consoleLevelNode.asString();
            final BuildConfig.ConfigNode consoleColorNode = consoleNode.get("color");
            consoleColor = consoleColorNode.isNull() || consoleColorNode.asBoolean().booleanValue();
            if (consoleColor) {
                processorContext.addRuntimeInitializedClasses("org.jboss.logmanager.formatters.TrueColorHolder");
            }

            final BuildConfig.ConfigNode fileNode = loggingNode.get("file");
            final BuildConfig.ConfigNode fileEnableNode = fileNode.get("enable");
            fileEnable = fileEnableNode.isNull() || fileEnableNode.asBoolean().booleanValue();
            final BuildConfig.ConfigNode fileFormatNode = fileNode.get("format");
            fileFormat = fileFormatNode.isNull() ? "%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c] (%t) %s%e%n" : fileFormatNode.asString();
            final BuildConfig.ConfigNode fileLevelNode = fileNode.get("level");
            fileLevel = fileLevelNode.isNull() ? "ALL" : fileLevelNode.asString();
            final BuildConfig.ConfigNode filePathNode = fileNode.get("path");
            filePath = filePathNode.isNull() ? "server.log" : filePathNode.asString();

            minimumLevelOf = cc.getMethodCreator("getMinimumLevelOf", Level.class, String.class);
            minimumLevelOf.setModifiers(Opcodes.ACC_PUBLIC);
            // TODO set minimumLevelOf parameter names

            levelOf = cc.getMethodCreator("getLevelOf", Level.class, String.class);
            levelOf.setModifiers(Opcodes.ACC_PUBLIC);
            // TODO set levelOf parameter names

            handlersOf = cc.getMethodCreator("getHandlersOf", Handler[].class, String.class);
            handlersOf.setModifiers(Opcodes.ACC_PUBLIC);
            // TODO set handlersOf parameter names

            // in image build phase, do a special console handler config

            final BytecodeCreator ifRootLogger = ifRootLogger(handlersOf, b ->
                b.readStaticField(
                    FieldDescriptor.of(
                        EmbeddedConfigurator.class.getName(),
                        "NO_HANDLERS",
                        "[Ljava/util/logging/Handler;"
                    )
                )
            );
            BranchResult buildOrRunBranchResult = ifRootLogger.ifNonZero(ifRootLogger.invokeStaticMethod(MethodDescriptor.ofMethod(ImageInfo.class, "inImageBuildtimeCode", boolean.class)));

            // run time
            BytecodeCreator branch = buildOrRunBranchResult.falseBranch();
            ResultHandle consoleErrorManager;
            ResultHandle console, file;
            if (consoleEnable) {
                ResultHandle formatter;
                final ResultHandle consoleFormatResult = branch.load(consoleFormat);
                if (consoleColor) {
                    // detect a console at run time
                    final ResultHandle consoleProbeResult = branch.invokeStaticMethod(MethodDescriptor.ofMethod(System.class, "console", Console.class));
                    final BranchResult consoleBranchResult = branch.ifNull(consoleProbeResult);
                    final ResultHandle noConsole = consoleBranchResult.falseBranch().newInstance(
                        MethodDescriptor.ofConstructor(PatternFormatter.class, String.class),
                        consoleFormatResult
                    );
                    final ResultHandle yesConsole = consoleBranchResult.trueBranch().newInstance(
                        MethodDescriptor.ofConstructor(ColorPatternFormatter.class, String.class),
                        consoleFormatResult
                    );
                    formatter = consoleBranchResult.mergeBranches(yesConsole, noConsole, Formatter.class);
                } else {
                    formatter = branch.newInstance(
                        MethodDescriptor.ofConstructor(PatternFormatter.class, String.class),
                        consoleFormatResult
                    );
                }
                console = branch.newInstance(
                    MethodDescriptor.ofConstructor(ConsoleHandler.class, Formatter.class),
                    formatter
                );
                branch.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(ConsoleHandler.class, "setLevel", void.class, Level.class),
                    console,
                    getLevelFor(branch, consoleLevel)
                );
                consoleErrorManager = branch.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(ConsoleHandler.class, "getLocalErrorManager", ErrorManager.class),
                    console
                );
                branch.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(ConsoleHandler.class, "setLevel", void.class, Level.class),
                    console,
                    getLevelFor(branch, consoleLevel)
                );
            } else {
                consoleErrorManager = null;
                console = null;
            }
            if (fileEnable) {
                ResultHandle formatter = branch.newInstance(
                    MethodDescriptor.ofConstructor(PatternFormatter.class, String.class),
                    branch.load(fileFormat)
                );
                file = branch.newInstance(
                    MethodDescriptor.ofConstructor(FileHandler.class, Formatter.class),
                    formatter
                );
                branch.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(FileHandler.class, "setLevel", void.class, Level.class),
                    file,
                    getLevelFor(branch, fileLevel)
                );
                branch.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(FileHandler.class, "setFile", void.class, File.class),
                    file,
                    branch.newInstance(MethodDescriptor.ofConstructor(File.class, String.class), branch.load(filePath))
                );
                if (consoleErrorManager != null) {
                    branch.invokeVirtualMethod(
                        MethodDescriptor.ofMethod(FileHandler.class, "setErrorManager", void.class, ErrorManager.class),
                        file,
                        consoleErrorManager
                    );
                }
            } else {
                file = null;
            }
            ResultHandle array;
            if (consoleEnable && fileEnable) {
                array = branch.newArray(Handler[].class, branch.load(2));
                branch.writeArrayValue(array, branch.load(0), console);
                branch.writeArrayValue(array, branch.load(1), file);
            } else if (consoleEnable) {
                array = branch.newArray(Handler[].class, branch.load(1));
                branch.writeArrayValue(array, branch.load(0), console);
            } else if (fileEnable) {
                array = branch.newArray(Handler[].class, branch.load(1));
                branch.writeArrayValue(array, branch.load(0), file);
            } else {
                array = branch.readStaticField(FieldDescriptor.of(EmbeddedConfigurator.class, "NO_HANDLERS", Handler[].class));
            }
            branch.returnValue(array);

            // build time
            branch = buildOrRunBranchResult.trueBranch();
            ResultHandle formatter = branch.newInstance(
                MethodDescriptor.ofConstructor(PatternFormatter.class, String.class),
                branch.load("%d{HH:mm:ss,SSS} %-5p [%c{1.}] %s%e%n") // fixed format at build time
            );
            console = branch.newInstance(
                MethodDescriptor.ofConstructor(ConsoleHandler.class, Formatter.class),
                formatter
            );
            branch.invokeVirtualMethod(
                MethodDescriptor.ofMethod(ConsoleHandler.class, "setLevel", void.class, Level.class),
                console,
                getLevelFor(branch, "ALL")
            );
            array = branch.newArray(
                Handler[].class,
                branch.load(1)
            );
            branch.writeArrayValue(
                array,
                branch.load(0),
                console
            );
            branch.returnValue(
                array
            );

            // levels do not have the option of being reconfigured at run time
            BytecodeCreator levelOfBc = ifNotRootLogger(levelOf, b -> getLevelFor(b, rootLevel));
            BytecodeCreator minLevelOfBc = ifNotRootLogger(minimumLevelOf, b -> getLevelFor(b, rootMinLevel));

            final BuildConfig.ConfigNode categoryNode = loggingNode.get("category");
            for (String category : categoryNode.getChildKeys()) {
                // configure category
                final BuildConfig.ConfigNode baseNode = categoryNode.get(category);
                final BuildConfig.ConfigNode minLevelNode = baseNode.get("min-level");
                final String minLevel = minLevelNode.isNull() ? "inherit" : minLevelNode.asString();
                final BuildConfig.ConfigNode levelNode = baseNode.get("level");
                final String level = levelNode.isNull() ? "inherit" : levelNode.asString();

                levelOfBc = ifNotLogger(levelOfBc, category, b -> getLevelFor(b, level));
                minLevelOfBc = ifNotLogger(minLevelOfBc, category, b -> getLevelFor(b, minLevel));
            }

            // epilogues

            levelOfBc.returnValue(levelOfBc.loadNull());
            minLevelOfBc.returnValue(levelOfBc.loadNull());
        }

        processorContext.createResource("META-INF/services/org.jboss.logmanager.EmbeddedConfigurator", GENERATED_CONFIGURATOR.replace('/', '.').getBytes(StandardCharsets.UTF_8));

        // now inject the system property setter

        final LogSetupTemplate proxy;
        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(- 1000)) {
            proxy = recorder.getRecordingProxy(LogSetupTemplate.class);
            proxy.initializeLogManager();
        }

        processorContext.addNativeImageSystemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    private BytecodeCreator ifRootLogger(BytecodeCreator orig, Function<BytecodeCreator, ResultHandle> returnIfNotRoot) {
        BranchResult branchResult = orig.ifNonZero(
            orig.invokeVirtualMethod(
                MethodDescriptor.ofMethod(String.class, "isEmpty", boolean.class),
                orig.getMethodParam(0) // name
            )
        );
        final BytecodeCreator falseBranch = branchResult.falseBranch();
        falseBranch.returnValue(returnIfNotRoot.apply(falseBranch));

        return branchResult.trueBranch();
    }

    private BytecodeCreator ifNotRootLogger(BytecodeCreator orig, Function<BytecodeCreator, ResultHandle> returnIfRoot) {
        BranchResult branchResult = orig.ifNonZero(
            orig.invokeVirtualMethod(
                MethodDescriptor.ofMethod(String.class, "isEmpty", boolean.class),
                orig.getMethodParam(0) // name
            )
        );
        final BytecodeCreator trueBranch = branchResult.trueBranch();
        trueBranch.returnValue(returnIfRoot.apply(trueBranch));

        return branchResult.falseBranch();
    }

    private BytecodeCreator ifNotLogger(BytecodeCreator orig, String category, Function<BytecodeCreator, ResultHandle> returnIfCategory) {
        BranchResult branchResult = orig.ifNonZero(
            orig.invokeVirtualMethod(
                MethodDescriptor.ofMethod(String.class, "equals", boolean.class, Object.class),
                orig.getMethodParam(0), // name
                orig.load(category)
            )
        );
        final BytecodeCreator trueBranch = branchResult.trueBranch();
        trueBranch.returnValue(returnIfCategory.apply(trueBranch));

        return branchResult.falseBranch();
    }

    private ResultHandle getLevelFor(final BytecodeCreator bc, final String levelName) {
        switch (levelName) {
            case "inherit":
                return bc.loadNull();
            case "FATAL":
            case "ERROR":
            case "WARN":
            case "INFO":
            case "DEBUG":
            case "TRACE":
                return bc.readStaticField(FieldDescriptor.of(org.jboss.logmanager.Level.class, levelName, org.jboss.logmanager.Level.class));
            case "OFF":
            case "SEVERE":
            case "WARNING":
            // case "INFO":
            case "CONFIG":
            case "FINE":
            case "FINER":
            case "FINEST":
            case "ALL":
                return bc.readStaticField(FieldDescriptor.of(Level.class, levelName, Level.class));
            default:
                return bc.invokeStaticMethod(
                    MethodDescriptor.ofMethod(Level.class, "parse", Level.class, String.class),
                    bc.load(levelName)
                );
        }
    }

    public int getPriority() {
        return 1;
    }

    static final class ProcessorClassOutput implements ClassOutput {
        private final ProcessorContext processorContext;

        public void write(final String name, final byte[] data) {
            try {
                processorContext.addGeneratedClass(false, name, data);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        ProcessorClassOutput(final ProcessorContext processorContext) {
            this.processorContext = processorContext;
        }
    }
}
