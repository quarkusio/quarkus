package io.quarkus.lambda.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.lambda.runtime.LambdaServlet;
import io.quarkus.lambda.runtime.LambdaTemplate;
import io.quarkus.undertow.ServletBuildItem;

public final class LambdaProcessor {
    private static final DotName REQUEST_HANDLER = DotName.createSimple(RequestHandler.class.getName());

    @BuildStep
    List<LambdaBuildItem> discover(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClasses) {
        List<LambdaBuildItem> ret = new ArrayList<>();

        for (ClassInfo info : combinedIndexBuildItem.getIndex().getAllKnownImplementors(REQUEST_HANDLER)) {
            final DotName name = info.name();

            final String lambda = name.toString();
            final String mapping = name.local();
            ret.add(new LambdaBuildItem(lambda, mapping));

            ClassInfo current = info;
            boolean done = false;
            while (current != null && !done) {
                for (MethodInfo method : current.methods()) {
                    if (method.name().equals("handleRequest")
                            && method.parameters().size() == 2
                            && !method.parameters().get(0).name().equals(DotName.createSimple(Object.class.getName()))) {
                        reflectiveClasses.produce(new ReflectiveHierarchyBuildItem(method.parameters().get(0)));
                        done = true;
                        break;
                    }
                }
                current = combinedIndexBuildItem.getIndex().getClassByName(current.superName());
            }
        }
        return ret;
    }

    @BuildStep
    List<AdditionalBeanBuildItem> beans(List<LambdaBuildItem> lambdas) {
        List<AdditionalBeanBuildItem> ret = new ArrayList<>();
        for (LambdaBuildItem i : lambdas) {
            ret.add(new AdditionalBeanBuildItem(false, i.getClassName()));
        }
        return ret;
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void servlets(List<LambdaBuildItem> lambdas,
            BuildProducer<ServletBuildItem> servletProducer,
            BeanContainerBuildItem beanContainerBuildItem,
            LambdaTemplate template,
            RecorderContext context) {

        for (LambdaBuildItem info : lambdas) {
            servletProducer.produce(ServletBuildItem.builder(info.getClassName(), LambdaServlet.class.getName())
                    .setLoadOnStartup(1)
                    .setInstanceFactory(template.lambdaServletInstanceFactory(
                            (Class<? extends RequestHandler>) context.classProxy(info.getClassName()),
                            beanContainerBuildItem.getValue()))
                    .addMapping("/" + info.getPath())
                    .build());
        }
    }
}
