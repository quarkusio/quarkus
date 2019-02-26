/*
 * Copyright 2018 Red Hat, Inc.
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

package io.quarkus.deployment.steps;

import static org.jboss.protean.gizmo.MethodDescriptor.ofConstructor;
import static org.jboss.protean.gizmo.MethodDescriptor.ofMethod;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.builder.Version;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.CatchBlockCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.FieldCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.protean.gizmo.TryBlock;

import io.quarkus.deployment.ClassOutput;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassNameBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderObjectLoaderBuildItem;
import io.quarkus.deployment.builditem.ClassOutputBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HttpServerBuildItem;
import io.quarkus.deployment.builditem.JavaLibraryPathAdditionalPathBuildItem;
import io.quarkus.deployment.builditem.MainBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.MainClassBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.SslTrustStoreSystemPropertyBuildItem;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.Application;
import io.quarkus.runtime.StartupContext;
import io.quarkus.runtime.StartupTask;
import io.quarkus.runtime.Timing;

class MainClassBuildStep {

    private static final String APP_CLASS = "io.quarkus.runner.ApplicationImpl";
    private static final String MAIN_CLASS = "io.quarkus.runner.GeneratedMain";
    private static final String STARTUP_CONTEXT = "STARTUP_CONTEXT";
    private static final String JAVA_LIBRARY_PATH = "java.library.path";
    private static final String JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";

    private static final AtomicInteger COUNT = new AtomicInteger();

    @BuildStep
    MainClassBuildItem build(List<StaticBytecodeRecorderBuildItem> staticInitTasks,
            List<ObjectSubstitutionBuildItem> substitutions,
            List<MainBytecodeRecorderBuildItem> mainMethod,
            List<SystemPropertyBuildItem> properties,
            List<JavaLibraryPathAdditionalPathBuildItem> javaLibraryPathAdditionalPaths,
            Optional<SslTrustStoreSystemPropertyBuildItem> sslTrustStoreSystemProperty,
            Optional<HttpServerBuildItem> httpServer,
            List<FeatureBuildItem> features,
            BuildProducer<ApplicationClassNameBuildItem> appClassNameProducer,
            List<BytecodeRecorderObjectLoaderBuildItem> loaders,
            ClassOutputBuildItem classOutput) {

        String appClassName = APP_CLASS + COUNT.incrementAndGet();
        appClassNameProducer.produce(new ApplicationClassNameBuildItem(appClassName));

        // Application class
        ClassCreator file = new ClassCreator(ClassOutput.gizmoAdaptor(classOutput.getClassOutput(), true), appClassName, null,
                Application.class.getName());

        // Application class: static init

        FieldCreator scField = file.getFieldCreator(STARTUP_CONTEXT, StartupContext.class);
        scField.setModifiers(Modifier.STATIC);

        MethodCreator mv = file.getMethodCreator("<clinit>", void.class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        //very first thing is to set system props (for build time)
        for (SystemPropertyBuildItem i : properties) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(i.getKey()), mv.load(i.getValue()));
        }

        mv.invokeStaticMethod(MethodDescriptor.ofMethod(Timing.class, "staticInitStarted", void.class));
        ResultHandle startupContext = mv.newInstance(ofConstructor(StartupContext.class));
        mv.writeStaticField(scField.getFieldDescriptor(), startupContext);
        TryBlock tryBlock = mv.tryBlock();
        for (StaticBytecodeRecorderBuildItem holder : staticInitTasks) {
            final BytecodeRecorderImpl recorder = holder.getBytecodeRecorder();
            if (!recorder.isEmpty()) {
                // Register substitutions in all recorders
                for (ObjectSubstitutionBuildItem sub : substitutions) {
                    ObjectSubstitutionBuildItem.Holder holder1 = sub.holder;
                    recorder.registerSubstitution(holder1.from, holder1.to, holder1.substitution);
                }
                for (BytecodeRecorderObjectLoaderBuildItem item : loaders) {
                    recorder.registerObjectLoader(item.getObjectLoader());
                }
                recorder.writeBytecode(classOutput.getClassOutput());

                ResultHandle dup = tryBlock.newInstance(ofConstructor(recorder.getClassName()));
                tryBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup,
                        startupContext);
            }
        }
        tryBlock.returnValue(null);

        CatchBlockCreator cb = tryBlock.addCatch(Throwable.class);
        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start quarkus", cb.getCaughtException());

        // Application class: start method

        mv = file.getMethodCreator("doStart", void.class, String[].class);
        mv.setModifiers(Modifier.PROTECTED | Modifier.FINAL);

        // very first thing is to set system props (for run time, which use substitutions for a different
        // storage from build-time)
        for (SystemPropertyBuildItem i : properties) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    mv.load(i.getKey()), mv.load(i.getValue()));
        }

        // Set the SSL system properties
        if (!javaLibraryPathAdditionalPaths.isEmpty()) {
            // FIXME: this is the code we should use but I couldn't get GraalVM to work with a java.library.path containing multiple paths.
            // We need to dig further but for now, we need this to work.
            // ResultHandle javaLibraryPath = mv.newInstance(ofConstructor(StringBuilder.class, String.class),
            //         mv.invokeStaticMethod(ofMethod(System.class, "getProperty", String.class, String.class), mv.load(JAVA_LIBRARY_PATH)));
            // for (JavaLibraryPathAdditionalPathBuildItem javaLibraryPathAdditionalPath : javaLibraryPathAdditionalPaths) {
            //     ResultHandle javaLibraryPathLength = mv.invokeVirtualMethod(ofMethod(StringBuilder.class, "length", int.class), javaLibraryPath);
            //     mv.ifNonZero(javaLibraryPathLength).trueBranch()
            //             .invokeVirtualMethod(ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class), javaLibraryPath, mv.load(File.pathSeparator));
            //     mv.invokeVirtualMethod(ofMethod(StringBuilder.class, "append", StringBuilder.class, String.class), javaLibraryPath,
            //             mv.load(javaLibraryPathAdditionalPath.getPath()));
            // }
            // mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class),
            //         mv.load(JAVA_LIBRARY_PATH), mv.invokeVirtualMethod(ofMethod(StringBuilder.class, "toString", String.class), javaLibraryPath));

            ResultHandle isJavaLibraryPathEmpty = mv.invokeVirtualMethod(ofMethod(String.class, "isEmpty", boolean.class),
                    mv.invokeStaticMethod(ofMethod(System.class, "getProperty", String.class, String.class),
                            mv.load(JAVA_LIBRARY_PATH)));

            BytecodeCreator inGraalVMCode = mv
                    .ifNonZero(mv.invokeStaticMethod(ofMethod(ImageInfo.class, "inImageRuntimeCode", boolean.class)))
                    .trueBranch();

            inGraalVMCode.ifNonZero(isJavaLibraryPathEmpty).trueBranch().invokeStaticMethod(
                    ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    inGraalVMCode.load(JAVA_LIBRARY_PATH),
                    inGraalVMCode.load(javaLibraryPathAdditionalPaths.iterator().next().getPath()));
        }

        if (sslTrustStoreSystemProperty.isPresent()) {
            ResultHandle alreadySetTrustStore = mv.invokeStaticMethod(
                    ofMethod(System.class, "getProperty", String.class, String.class),
                    mv.load(JAVAX_NET_SSL_TRUST_STORE));

            BytecodeCreator inGraalVMCode = mv
                    .ifNonZero(mv.invokeStaticMethod(ofMethod(ImageInfo.class, "inImageRuntimeCode", boolean.class)))
                    .trueBranch();

            inGraalVMCode.ifNull(alreadySetTrustStore).trueBranch().invokeStaticMethod(
                    ofMethod(System.class, "setProperty", String.class, String.class, String.class),
                    inGraalVMCode.load(JAVAX_NET_SSL_TRUST_STORE),
                    inGraalVMCode.load(sslTrustStoreSystemProperty.get().getPath()));
        }

        mv.invokeStaticMethod(ofMethod(Timing.class, "mainStarted", void.class));
        startupContext = mv.readStaticField(scField.getFieldDescriptor());

        tryBlock = mv.tryBlock();
        for (MainBytecodeRecorderBuildItem holder : mainMethod) {
            final BytecodeRecorderImpl recorder = holder.getBytecodeRecorder();
            if (!recorder.isEmpty()) {
                for (BytecodeRecorderObjectLoaderBuildItem item : loaders) {
                    recorder.registerObjectLoader(item.getObjectLoader());
                }
                recorder.writeBytecode(classOutput.getClassOutput());
                ResultHandle dup = tryBlock.newInstance(ofConstructor(recorder.getClassName()));
                tryBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup,
                        startupContext);
            }
        }

        // Startup log messages
        ResultHandle featuresHandle = tryBlock.load(features.stream()
                .map(f -> f.getInfo())
                .sorted()
                .collect(Collectors.joining(", ")));
        ResultHandle serverHandle = httpServer.isPresent() ? tryBlock.load("Listening on: http://" + httpServer.get()
                .getHost() + ":" + httpServer.get().getPort()) : tryBlock.load("");
        tryBlock.invokeStaticMethod(
                ofMethod(Timing.class, "printStartupTime", void.class, String.class, String.class, String.class),
                tryBlock.load(Version.getVersion()), featuresHandle, serverHandle);

        cb = tryBlock.addCatch(Throwable.class);
        cb.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cb.getCaughtException());
        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start quarkus", cb.getCaughtException());
        mv.returnValue(null);

        // Application class: stop method

        mv = file.getMethodCreator("doStop", void.class);
        mv.setModifiers(Modifier.PROTECTED | Modifier.FINAL);
        startupContext = mv.readStaticField(scField.getFieldDescriptor());
        mv.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        mv.returnValue(null);

        // Finish application class
        file.close();

        // Main class

        file = new ClassCreator(ClassOutput.gizmoAdaptor(classOutput.getClassOutput(), true), MAIN_CLASS, null,
                Object.class.getName());

        mv = file.getMethodCreator("main", void.class, String[].class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        final ResultHandle appClassInstance = mv.newInstance(ofConstructor(appClassName));
        // run the app
        mv.invokeVirtualMethod(ofMethod(Application.class, "run", void.class, String[].class), appClassInstance,
                mv.getMethodParam(0));

        mv.returnValue(null);

        file.close();
        return new MainClassBuildItem(MAIN_CLASS);
    }

}
