package io.quarkus.test.component.callbacks;

import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.DotName;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.component.QuarkusComponentTestCallbacks;
import io.quarkus.test.component.beans.MyOtherComponent;

public class MyTestCallbacks implements QuarkusComponentTestCallbacks {

    static volatile boolean afterStart = false;
    static volatile boolean afterStop = false;

    @Override
    public void beforeIndex(BeforeIndexContext beforeIndexContext) {
        if (isProcessed(beforeIndexContext)) {
            beforeIndexContext.addComponentClass(MyOtherComponent.class);
        }
    }

    @Override
    public void beforeBuild(BeforeBuildContext buildContext) {
        if (isProcessed(buildContext)) {
            buildContext.addAnnotationTransformation(AnnotationTransformation
                    .forClasses()
                    .whenClass(DotName.createSimple(MyOtherComponent.class))
                    .transform(t -> t.add(Unremovable.class)));
        }
    }

    @Override
    public void beforeStart(BeforeStartContext beforeStartContext) {
        if (isProcessed(beforeStartContext)) {
            beforeStartContext.setConfigProperty("foo", "RAB");
        }
    }

    @Override
    public void afterStart(AfterStartContext afterStartContext) {
        if (isProcessed(afterStartContext)) {
            afterStart = true;
        }
    }

    @Override
    public void afterStop(AfterStopContext afterStopContext) {
        if (isProcessed(afterStopContext)) {
            afterStop = true;
        }
    }

    boolean isProcessed(ComponentTestContext testContext) {
        return testContext.getTestClass().getSimpleName().equals("QuarkusComponentTestCallbacksTest");
    }

}
