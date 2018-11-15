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
import org.jboss.shamrock.runtime.StartupContext;
import org.jboss.shamrock.runtime.StartupTask;
import org.jboss.shamrock.runtime.Timing;

class MainClassBuildStep {

    private static final AtomicInteger COUNT = new AtomicInteger();
    private static final String MAIN_CLASS_INTERNAL = "org/jboss/shamrock/runner/GeneratedMain";
    private static final String MAIN_CLASS = MAIN_CLASS_INTERNAL.replace('/', '.');
    private static final String STARTUP_CONTEXT = "STARTUP_CONTEXT";

    @BuildStep
    MainClassBuildItem build(List<StaticBytecodeRecorderBuildItem> staticInitTasks,
                             List<MainBytecodeRecorderBuildItem> mainMethod,
                             List<SystemPropertyBuildItem> properties,
                             ClassOutputBuildItem classOutput) {

        ClassCreator file = new ClassCreator(ClassOutput.gizmoAdaptor(classOutput.getClassOutput(), true), MAIN_CLASS, null, Object.class.getName());

        FieldCreator scField = file.getFieldCreator(STARTUP_CONTEXT, StartupContext.class);
        scField.setModifiers(Modifier.STATIC);

        MethodCreator mv = file.getMethodCreator("<clinit>", void.class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);

        //very first thing is to set system props
        for (SystemPropertyBuildItem i : properties) {
            mv.invokeStaticMethod(ofMethod(System.class, "setProperty", String.class, String.class, String.class), mv.load(i.getKey()), mv.load(i.getValue()));
        }

        mv.invokeStaticMethod(MethodDescriptor.ofMethod(Timing.class, "staticInitStarted", void.class));
        ResultHandle startupContext = mv.newInstance(ofConstructor(StartupContext.class));
        mv.writeStaticField(scField.getFieldDescriptor(), startupContext);
        TryBlock catchBlock = mv.tryBlock();
        for (StaticBytecodeRecorderBuildItem holder : staticInitTasks) {
            if (!holder.getBytecodeRecorder().isEmpty()) {
                String className = getClass().getName() + "$$Proxy" + COUNT.incrementAndGet();
                holder.getBytecodeRecorder().writeBytecode(classOutput.getClassOutput(), className);

                ResultHandle dup = catchBlock.newInstance(ofConstructor(className));
                catchBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup, startupContext);
            }
        }
        catchBlock.returnValue(null);

        CatchBlockCreator cb = catchBlock.addCatch(Throwable.class);
        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start shamrock", cb.getCaughtException());

        mv = file.getMethodCreator("main", void.class, String[].class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        mv.invokeStaticMethod(ofMethod(Timing.class, "mainStarted", void.class));
        startupContext = mv.readStaticField(scField.getFieldDescriptor());
        catchBlock = mv.tryBlock();
        for (MainBytecodeRecorderBuildItem holder : mainMethod) {
            if (!holder.getBytecodeRecorder().isEmpty()) {
                String className = getClass().getName() + "$$Proxy" + COUNT.incrementAndGet();
                holder.getBytecodeRecorder().writeBytecode(classOutput.getClassOutput(), className);
                ResultHandle dup = catchBlock.newInstance(ofConstructor(className));
                catchBlock.invokeInterfaceMethod(ofMethod(StartupTask.class, "deploy", void.class, StartupContext.class), dup, startupContext);
            }
        }
        catchBlock.invokeStaticMethod(ofMethod(Timing.class, "printStartupTime", void.class));
        mv.returnValue(null);

        cb = catchBlock.addCatch(Throwable.class);
        cb.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), cb.getCaughtException());
        cb.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        cb.throwException(RuntimeException.class, "Failed to start shamrock", cb.getCaughtException());

        mv = file.getMethodCreator("close", void.class);
        mv.setModifiers(Modifier.PUBLIC | Modifier.STATIC);
        startupContext = mv.readStaticField(scField.getFieldDescriptor());
        mv.invokeVirtualMethod(ofMethod(StartupContext.class, "close", void.class), startupContext);
        mv.returnValue(null);
        file.close();
        return new MainClassBuildItem(MAIN_CLASS);
    }

}
