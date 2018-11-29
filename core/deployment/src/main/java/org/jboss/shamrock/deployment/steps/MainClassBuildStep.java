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

package org.jboss.shamrock.deployment.steps;

import static org.jboss.protean.gizmo.MethodDescriptor.ofConstructor;
import static org.jboss.protean.gizmo.MethodDescriptor.ofMethod;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.protean.gizmo.CatchBlockCreator;
import org.jboss.protean.gizmo.ClassCreator;
import org.jboss.protean.gizmo.FieldCreator;
import org.jboss.protean.gizmo.MethodCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;
import org.jboss.protean.gizmo.TryBlock;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.deployment.ClassOutput;
import org.jboss.shamrock.deployment.builditem.ClassOutputBuildItem;
import org.jboss.shamrock.deployment.builditem.MainBytecodeRecorderBuildItem;
import org.jboss.shamrock.deployment.builditem.MainClassBuildItem;
import org.jboss.shamrock.deployment.builditem.StaticBytecodeRecorderBuildItem;
import org.jboss.shamrock.deployment.builditem.SystemPropertyBuildItem;
import org.jboss.shamrock.runtime.Application;
import org.jboss.shamrock.runtime.StartupContext;
import org.jboss.shamrock.runtime.StartupTask;
import org.jboss.shamrock.runtime.Timing;

class MainClassBuildStep {

    private static final AtomicInteger COUNT = new AtomicInteger();
    private static final String APP_CLASS = "org.jboss.shamrock.runner.ApplicationImpl";
    private static final String MAIN_CLASS = "org.jboss.shamrock.runner.GeneratedMain";
    private static final String STARTUP_CONTEXT = "STARTUP_CONTEXT";

    @BuildStep
    MainClassBuildItem build(List<StaticBytecodeRecorderBuildItem> staticInitTasks,
                             List<MainBytecodeRecorderBuildItem> mainMethod,
                             List<SystemPropertyBuildItem> properties,
                             ClassOutputBuildItem classOutput) {

        // Application class
        ClassCreator file = new ClassCreator(ClassOutput.gizmoAdaptor(classOutput.getClassOutput(), true), APP_CLASS, null, Application.class.getName());

        // Application class: static init

        FieldCreator scField = file.getFieldCreator(STARTUP_CONTEXT, StartupContext.class);
        scField.setModifiers(Modifier.STATIC);

        MethodCreator mv = file.getMethodCreator("<clinit>", void.class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        //very first thing is to set system props (for build time)
        for (SystemPropertyBuildItem i : properties) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class), mv.load(i.getKey()), mv.load(i.getValue()));
        }

        mv.invokeStaticMethod(MethodDescriptor.ofMethod(Timing.class, "staticInitStarted", void.class));
        ResultHandle startupContext = mv.newInstance(ofConstructor(StartupContext.class));
        mv.writeStaticField(scField.getFieldDescriptor(), startupContext);
        TryBlock tryBlock = mv.tryBlock();
        for (StaticBytecodeRecorderBuildItem holder : staticInitTasks) {
            if (!holder.getBytecodeRecorder().isEmpty()) {
                String className = getClass().getName() + "$$Proxy" + COUNT.incrementAndGet();
                holder.getBytecodeRecorder().writeBytecode(classOutput.getClassOutput(), className);

                ResultHandle dup = tryBlock.newInstance(ofConstructor(className));
                tryBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup, startupContext);
            }
        }
        tryBlock.returnValue(null);

        CatchBlockCreator cb = tryBlock.addCatch(Throwable.class);
        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start shamrock", cb.getCaughtException());

        // Application class: start method

        mv = file.getMethodCreator("doStart", void.class, String[].class);
        mv.setModifiers(Modifier.PROTECTED | Modifier.FINAL);

        // very first thing is to set system props (for run time, which use substitutions for a different
        // storage from build-time)
        for (SystemPropertyBuildItem i : properties) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class), mv.load(i.getKey()), mv.load(i.getValue()));
        }

        mv.invokeStaticMethod(ofMethod(Timing.class, "mainStarted", void.class));
        startupContext = mv.readStaticField(scField.getFieldDescriptor());
        tryBlock = mv.tryBlock();
        for (MainBytecodeRecorderBuildItem holder : mainMethod) {
            if (!holder.getBytecodeRecorder().isEmpty()) {
                String className = getClass().getName() + "$$Proxy" + COUNT.incrementAndGet();
                holder.getBytecodeRecorder().writeBytecode(classOutput.getClassOutput(), className);
                ResultHandle dup = tryBlock.newInstance(ofConstructor(className));
                tryBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup, startupContext);
            }
        }
        tryBlock.invokeStaticMethod(ofMethod(Timing.class, "printStartupTime", void.class));

        cb = tryBlock.addCatch(Throwable.class);
        cb.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cb.getCaughtException());
        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start shamrock", cb.getCaughtException());
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

        file = new ClassCreator(ClassOutput.gizmoAdaptor(classOutput.getClassOutput(), true), MAIN_CLASS, null, Object.class.getName());

        mv = file.getMethodCreator("main", void.class, String[].class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        final ResultHandle appClassInstance = mv.newInstance(ofConstructor(APP_CLASS));
        // run the app
        mv.invokeVirtualMethod(ofMethod(Application.class, "run", void.class, String[].class), appClassInstance, mv.getMethodParam(0));

        mv.returnValue(null);

        file.close();
        return new MainClassBuildItem(MAIN_CLASS);
    }

}
