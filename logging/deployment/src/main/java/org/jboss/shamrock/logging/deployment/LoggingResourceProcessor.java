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

import java.nio.charset.StandardCharsets;

import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.logging.runtime.LogSetupTemplate;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
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

        final ClassWriter cv = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cv.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, GENERATED_CONFIGURATOR, null, "java/lang/Object", new String[] { "org/jboss/logmanager/EmbeddedConfigurator" });
        cv.visitSource("LoggingResourceProcessor.java", null);

        // simple ctor
        final MethodVisitor ctor = new DebugMethodVisitor(cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null));
        ctor.visitVarInsn(Opcodes.ALOAD, 0);
        ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        ctor.visitInsn(Opcodes.RETURN);
        ctor.visitMaxs(0, 0);
        ctor.visitEnd();

        final BuildConfig.ConfigNode rootLoggerNode = loggingNode.get("root");
        final BuildConfig.ConfigNode rootMinLevelNode = rootLoggerNode.get("min-level");
        final String rootMinLevel = rootMinLevelNode.isNull() ? "INFO" : rootMinLevelNode.asString();
        final BuildConfig.ConfigNode rootLevelNode = rootLoggerNode.get("level");
        final String rootLevel = rootLevelNode.isNull() ? rootMinLevel :  rootLevelNode.asString();

        int handlers = 0;

        final BuildConfig.ConfigNode consoleNode = loggingNode.get("console");
        final BuildConfig.ConfigNode consoleEnableNode = consoleNode.get("enable");
        final boolean consoleEnable = consoleEnableNode.isNull() || consoleEnableNode.asBoolean().booleanValue();
        if (consoleEnable) handlers ++;
        final BuildConfig.ConfigNode consoleFormatNode = consoleNode.get("format");
        final String consoleFormat = consoleFormatNode.isNull() ? "%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c{1.}] (%t) %s%e%n" : consoleFormatNode.asString();
        final BuildConfig.ConfigNode consoleLevelNode = consoleNode.get("level");
        final String consoleLevel = consoleLevelNode.isNull() ? "INFO" : consoleLevelNode.asString();

        final BuildConfig.ConfigNode fileNode = loggingNode.get("file");
        final BuildConfig.ConfigNode fileEnableNode = fileNode.get("enable");
        final boolean fileEnable = fileEnableNode.isNull() || fileEnableNode.asBoolean().booleanValue();
        if (fileEnable) handlers ++;
        final BuildConfig.ConfigNode fileFormatNode = fileNode.get("format");
        final String fileFormat = fileFormatNode.isNull() ? "%d{yyyy-MM-dd HH:mm:ss,SSS} %h %N[%i] %-5p [%c] (%t) %s%e%n" : fileFormatNode.asString();
        final BuildConfig.ConfigNode fileLevelNode = fileNode.get("level");
        final String fileLevel = fileLevelNode.isNull() ? "ALL" : fileLevelNode.asString();
        final BuildConfig.ConfigNode filePathNode = fileNode.get("path");
        final String filePath = filePathNode.isNull() ? "server.log" : filePathNode.asString();

        final MethodVisitor minimumLevelOf = new DebugMethodVisitor(cv.visitMethod(Opcodes.ACC_PUBLIC, "getMinimumLevelOf", "(Ljava/lang/String;)Ljava/util/logging/Level;", null, null));
        minimumLevelOf.visitParameter("name", 0);
        minimumLevelOf.visitCode();
        final MethodVisitor levelOf = new DebugMethodVisitor(cv.visitMethod(Opcodes.ACC_PUBLIC, "getLevelOf", "(Ljava/lang/String;)Ljava/util/logging/Level;", null, null));
        levelOf.visitParameter("name", 0);
        levelOf.visitCode();

        final MethodVisitor handlersOf = new DebugMethodVisitor(cv.visitMethod(Opcodes.ACC_PUBLIC, "getHandlersOf", "(Ljava/lang/String;)[Ljava/util/logging/Handler;", null, null));
        handlersOf.visitParameter("name", 0);
        handlersOf.visitCode();

        // in image build phase, do a special console handler config

        // S: -
        Label runTime = new Label();
        handlersOf.visitMethodInsn(Opcodes.INVOKESTATIC, "org/graalvm/nativeimage/ImageInfo", "inImageBuildtimeCode", "()Z", false);
        // S: bool
        handlersOf.visitJumpInsn(Opcodes.IFEQ, runTime); // true/1 = build time, false/0 = run time

        // build time
        // S: -
        handlersOf.visitVarInsn(Opcodes.ALOAD, 1);
        // S: name
        handlersOf.visitLdcInsn("");
        // S: name ""
        handlersOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        // S: match
        Label noMatch = new Label();
        handlersOf.visitJumpInsn(Opcodes.IFEQ, noMatch); // actually if equal to false/0

        // S: -
        iconst(handlersOf, 1);
        // S: 1
        handlersOf.visitTypeInsn(Opcodes.ANEWARRAY, "java/util/logging/Handler");
        // S: array
        handlersOf.visitInsn(Opcodes.DUP);
        // S: array array
        iconst(handlersOf, 0);
        // S: array array 0
        handlersOf.visitTypeInsn(Opcodes.NEW, "org/jboss/logmanager/formatters/PatternFormatter");
        // S: array array 0 fmt
        handlersOf.visitInsn(Opcodes.DUP);
        // S: array array 0 fmt fmt
        handlersOf.visitLdcInsn("%d{HH:mm:ss,SSS} %-5p [%c{1.}] %s%e%n");
        // S: array array 0 fmt fmt str
        handlersOf.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/jboss/logmanager/formatters/PatternFormatter", "<init>", "(Ljava/lang/String;)V", false);
        // S: array array 0 fmt
        handlersOf.visitTypeInsn(Opcodes.NEW, "org/jboss/logmanager/handlers/ConsoleHandler");
        // S: array array 0 fmt handler
        handlersOf.visitInsn(Opcodes.DUP_X1);
        // S: array array 0 handler fmt handler
        handlersOf.visitInsn(Opcodes.SWAP);
        // S: array array 0 handler handler fmt
        handlersOf.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/jboss/logmanager/handlers/ConsoleHandler", "<init>", "(Ljava/util/logging/Formatter;)V", false);
        // S: array array 0 handler
        handlersOf.visitInsn(Opcodes.DUP);
        // S: array array 0 handler handler
        levelInsn(handlersOf, consoleLevel);
        // S: array array 0 handler handler level
        handlersOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/Handler", "setLevel", "(Ljava/util/logging/Level;)V", false);
        // S: array array 0 handler
        handlersOf.visitInsn(Opcodes.AASTORE);
        // S: array
        handlersOf.visitInsn(Opcodes.ARETURN);
        // S: -
        handlersOf.visitLabel(noMatch);
        // S: -
        handlersOf.visitVarInsn(Opcodes.ALOAD, 0);
        // S: this
        handlersOf.visitVarInsn(Opcodes.ALOAD, 1);
        // S: this name
        handlersOf.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/jboss/logmanager/EmbeddedConfigurator", "getHandlersOf", "(Ljava/lang/String;)[Ljava/util/logging/Handler;", true);
        // S: array
        handlersOf.visitInsn(Opcodes.ARETURN);
        // S: -

        // run time
        handlersOf.visitLabel(runTime);
        // S: -
        handlersOf.visitVarInsn(Opcodes.ALOAD, 1);
        // S: name
        handlersOf.visitLdcInsn("");
        // S: name ""
        handlersOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        // S: match
        noMatch = new Label();
        handlersOf.visitJumpInsn(Opcodes.IFEQ, noMatch); // actually if equal to false/0
        // S: -

        // in image or run time

        // S: -
        iconst(handlersOf, handlers);
        // S: cnt
        handlersOf.visitTypeInsn(Opcodes.ANEWARRAY, "java/util/logging/Handler");
        // S: array
        int index = 0;
        int consoleIndex = -1;
        if (consoleEnable) {
            // S: array
            handlersOf.visitInsn(Opcodes.DUP);
            // S: array array
            handlersOf.visitTypeInsn(Opcodes.NEW, "org/jboss/logmanager/handlers/ConsoleHandler");
            // S: array array handler
            handlersOf.visitInsn(Opcodes.DUP);
            // S: array array handler handler
            handlersOf.visitTypeInsn(Opcodes.NEW, "org/jboss/logmanager/formatters/PatternFormatter");
            // S: array array handler handler fmt
            handlersOf.visitInsn(Opcodes.DUP);
            // S: array array handler handler fmt fmt
            handlersOf.visitLdcInsn(consoleFormat);
            // S: array array handler handler fmt fmt fmtStr
            handlersOf.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/jboss/logmanager/formatters/PatternFormatter", "<init>", "(Ljava/lang/String;)V", false);
            // S: array array handler handler fmt
            handlersOf.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/jboss/logmanager/handlers/ConsoleHandler", "<init>", "(Ljava/util/logging/Formatter;)V", false);
            // S: array array handler
            handlersOf.visitInsn(Opcodes.DUP);
            // S: array array handler handler
            levelInsn(handlersOf, consoleLevel);
            // S: array array handler handler level
            handlersOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/Handler", "setLevel", "(Ljava/util/logging/Level;)V", false);
            // S: array array handler
            iconst(handlersOf, consoleIndex = index++);
            // S: array array handler idx
            handlersOf.visitInsn(Opcodes.SWAP);
            // S: array array idx handler
            handlersOf.visitInsn(Opcodes.AASTORE);
            // S: array
        }
        if (fileEnable) {
            // S: array
            handlersOf.visitInsn(Opcodes.DUP);
            // S: array array
            handlersOf.visitTypeInsn(Opcodes.NEW, "org/jboss/logmanager/handlers/FileHandler");
            // S: array array handler
            handlersOf.visitInsn(Opcodes.DUP);
            // S: array array handler handler
            handlersOf.visitTypeInsn(Opcodes.NEW, "org/jboss/logmanager/formatters/PatternFormatter");
            // S: array array handler handler fmt
            handlersOf.visitInsn(Opcodes.DUP);
            // S: array array handler handler fmt fmt
            handlersOf.visitLdcInsn(fileFormat);
            // S: array array handler handler fmt fmt fmtStr
            handlersOf.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/jboss/logmanager/formatters/PatternFormatter", "<init>", "(Ljava/lang/String;)V", false);
            // S: array array handler handler fmt
            handlersOf.visitTypeInsn(Opcodes.NEW, "java/io/File");
            // S: array array handler handler fmt file
            handlersOf.visitInsn(Opcodes.DUP);
            // S: array array handler handler fmt file file
            handlersOf.visitLdcInsn(filePath);
            // S: array array handler handler fmt file file fileStr
            handlersOf.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/File", "<init>", "(Ljava/lang/String;)V", false);
            // S: array array handler handler fmt file
            iconst(handlersOf, 1);
            // S: array array handler handler fmt file autoflush
            handlersOf.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/jboss/logmanager/handlers/FileHandler", "<init>", "(Ljava/util/logging/Formatter;Ljava/io/File;Z)V", false);
            // S: array array handler
            handlersOf.visitInsn(Opcodes.DUP);
            // S: array array handler handler
            levelInsn(handlersOf, fileLevel);
            // S: array array handler handler level
            handlersOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/Handler", "setLevel", "(Ljava/util/logging/Level;)V", false);
            // S: array array handler
            // special case: if the console handler is configured, use it as an error manager
            if (consoleEnable) {
                // S: array array handler
                handlersOf.visitInsn(Opcodes.DUP2);
                // S: array array handler array handler
                handlersOf.visitInsn(Opcodes.SWAP);
                // S: array array handler handler array
                iconst(handlersOf, consoleIndex);
                // S: array array handler handler array consoleIdx
                handlersOf.visitInsn(Opcodes.AALOAD);
                // S: array array handler handler console
                handlersOf.visitTypeInsn(Opcodes.CHECKCAST, "org/jboss/logmanager/handlers/ConsoleHandler");
                // S: array array handler handler console'
                handlersOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "org/jboss/logmanager/handlers/ConsoleHandler", "getLocalErrorManager", "()Ljava/util/logging/ErrorManager;", false);
                // S: array array handler handler manager
                handlersOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/logging/Handler", "setErrorManager", "(Ljava/util/logging/ErrorManager;)V", false);
            }
            // S: array array handler
            //noinspection UnusedAssignment
            iconst(handlersOf, index++);
            // S: array array handler idx
            handlersOf.visitInsn(Opcodes.SWAP);
            // S: array array idx handler
            handlersOf.visitInsn(Opcodes.AASTORE);
            // S: array
        }
        // S: array
        handlersOf.visitInsn(Opcodes.ARETURN);

        // no-match path
        handlersOf.visitLabel(noMatch);
        // S: -
        handlersOf.visitVarInsn(Opcodes.ALOAD, 0);
        // S: this
        handlersOf.visitVarInsn(Opcodes.ALOAD, 1);
        // S: this name
        handlersOf.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/jboss/logmanager/EmbeddedConfigurator", "getHandlersOf", "(Ljava/lang/String;)[Ljava/util/logging/Handler;", true);
        // S: array
        handlersOf.visitInsn(Opcodes.ARETURN);
        // S: -
        handlersOf.visitMaxs(0, 0);
        handlersOf.visitEnd();

        // method: levelOf
        // configure the root logger
        // S: -
        levelOf.visitVarInsn(Opcodes.ALOAD, 1);
        // S: name
        levelOf.visitLdcInsn("");
        // S: name ""
        levelOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        // S: bool
        noMatch = new Label();
        levelOf.visitJumpInsn(Opcodes.IFEQ, noMatch); // if result == to 0 (false)
        // S: -
        levelInsn(levelOf, rootLevel);
        // S: level
        levelOf.visitInsn(Opcodes.ARETURN);
        // S: -
        levelOf.visitLabel(noMatch);

        // method: minimumLevelOf
        // S: -
        minimumLevelOf.visitVarInsn(Opcodes.ALOAD, 1);
        // S: name
        minimumLevelOf.visitLdcInsn("");
        // S: name ""
        minimumLevelOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        // S: bool
        noMatch = new Label();
        minimumLevelOf.visitJumpInsn(Opcodes.IFEQ, noMatch); // if result == to 0 (false)
        // S: -
        levelInsn(minimumLevelOf, rootMinLevel);
        // S: level
        minimumLevelOf.visitInsn(Opcodes.ARETURN);
        // S: -
        minimumLevelOf.visitLabel(noMatch);
        // S: -

        final BuildConfig.ConfigNode categoryNode = loggingNode.get("category");
        for (String category : categoryNode.getChildKeys()) {
            // configure category
            final BuildConfig.ConfigNode baseNode = categoryNode.get(category);
            final BuildConfig.ConfigNode minLevelNode = baseNode.get("min-level");
            final String minLevel = minLevelNode.isNull() ? "inherit" : minLevelNode.asString();
            final BuildConfig.ConfigNode levelNode = baseNode.get("level");
            final String level = levelNode.isNull() ? "inherit" : levelNode.asString();
            // level
            final boolean inheritLevel = level.equals("inherit");
            // S: -
            levelOf.visitVarInsn(Opcodes.ALOAD, 1); // name
            // S: name
            levelOf.visitLdcInsn(category);
            // S: name categoryName
            levelOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
            noMatch = new Label();
            // S: bool
            levelOf.visitJumpInsn(Opcodes.IFEQ, noMatch); // if result == to 0 (false)
            // use default level, if one is specified
            if (! inheritLevel) {
                // S: -
                levelInsn(levelOf, level);
            } else {
                // S: -
                levelOf.visitInsn(Opcodes.ACONST_NULL);
            }
            // S: level
            levelOf.visitInsn(Opcodes.ARETURN);
            // S: -
            levelOf.visitLabel(noMatch);
            // min level
            if (! minLevel.equals("inherit")) {
                minimumLevelOf.visitVarInsn(Opcodes.ALOAD, 1); // name
                minimumLevelOf.visitLdcInsn(category);
                minimumLevelOf.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                noMatch = new Label();
                minimumLevelOf.visitJumpInsn(Opcodes.IFEQ, noMatch); // if result == to 0 (false)
                levelInsn(minimumLevelOf, minLevel);
                minimumLevelOf.visitInsn(Opcodes.ARETURN);
                minimumLevelOf.visitLabel(noMatch);
            }
        }

        // epilogues

        levelOf.visitInsn(Opcodes.ACONST_NULL);
        levelOf.visitInsn(Opcodes.ARETURN);
        levelOf.visitMaxs(0, 0);
        levelOf.visitEnd();
        minimumLevelOf.visitInsn(Opcodes.ACONST_NULL);
        minimumLevelOf.visitInsn(Opcodes.ARETURN);
        minimumLevelOf.visitMaxs(0, 0);
        minimumLevelOf.visitEnd();
        cv.visitEnd();

        final String niceName = GENERATED_CONFIGURATOR.replace('/', '.');
        processorContext.addGeneratedClass(false, niceName, cv.toByteArray());

        processorContext.createResource("META-INF/services/org.jboss.logmanager.EmbeddedConfigurator", niceName.getBytes(StandardCharsets.UTF_8));

        // now inject the system property setter

        final LogSetupTemplate proxy;
        try (BytecodeRecorder recorder = processorContext.addStaticInitTask(- 1000)) {
            proxy = recorder.getRecordingProxy(LogSetupTemplate.class);
            proxy.initializeLogManager();
        }

        processorContext.addNativeImageSystemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
    }

    private static void levelInsn(MethodVisitor mv, String levelName) {
        final String ownerClass;
        switch (levelName) {
            case "FATAL":
            case "ERROR":
            case "WARN":
            case "INFO":
            case "DEBUG":
            case "TRACE":
                ownerClass = "org/jboss/logmanager/Level";
                break;
            case "OFF":
            case "SEVERE":
            case "WARNING":
            // case "INFO":
            case "CONFIG":
            case "FINE":
            case "FINER":
            case "FINEST":
            case "ALL":
                ownerClass = "java/util/logging/Level";
                break;
            default:
                mv.visitLdcInsn(levelName);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/logging/Level", "parse", "(Ljava/lang/String;)Ljava/util/logging/Level;", false);
                return;
        }
        mv.visitFieldInsn(Opcodes.GETSTATIC, ownerClass, levelName, "L" + ownerClass + ";");
    }

    private static void iconst(MethodVisitor mv, int val) {
        switch (val) {
            case 0: mv.visitInsn(Opcodes.ICONST_0); break;
            case 1: mv.visitInsn(Opcodes.ICONST_1); break;
            case 2: mv.visitInsn(Opcodes.ICONST_2); break;
            case 3: mv.visitInsn(Opcodes.ICONST_3); break;
            case 4: mv.visitInsn(Opcodes.ICONST_4); break;
            case 5: mv.visitInsn(Opcodes.ICONST_5); break;
            default: mv.visitLdcInsn(Integer.valueOf(val)); break;
        }
    }

    public int getPriority() {
        return 1;
    }
}
