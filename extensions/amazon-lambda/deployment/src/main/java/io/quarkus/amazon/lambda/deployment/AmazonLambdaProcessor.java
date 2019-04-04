package io.quarkus.amazon.lambda.deployment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import io.quarkus.amazon.lambda.runtime.AmazonLambdaServlet;
import io.quarkus.amazon.lambda.runtime.AmazonLambdaTemplate;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.undertow.deployment.ServletBuildItem;

@SuppressWarnings("unchecked")
public final class AmazonLambdaProcessor {
    private static final DotName REQUEST_HANDLER = DotName.createSimple(RequestHandler.class.getName());

    @BuildStep
    List<AmazonLambdaBuildItem> discover(CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveClasses) {
        List<AmazonLambdaBuildItem> ret = new ArrayList<>();

        for (ClassInfo info : combinedIndexBuildItem.getIndex().getAllKnownImplementors(REQUEST_HANDLER)) {
            final DotName name = info.name();

            final String lambda = name.toString();
            ret.add(new AmazonLambdaBuildItem(lambda));

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
    AdditionalBeanBuildItem beans(List<AmazonLambdaBuildItem> lambdas) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();
        for (AmazonLambdaBuildItem i : lambdas) {
            builder.addBeanClass(i.getClassName());
        }
        return builder.build();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    public void servlets(List<AmazonLambdaBuildItem> lambdas,
            BuildProducer<ServletBuildItem> servletProducer,
            BeanContainerBuildItem beanContainerBuildItem,
            AmazonLambdaTemplate template,
            RecorderContext context) throws IOException {

        for (AmazonLambdaBuildItem lambda : lambdas) {
            servletProducer.produce(ServletBuildItem.builder(lambda.getClassName(), AmazonLambdaServlet.class.getName())
                    .setLoadOnStartup(1)
                    .setInstanceFactory(template.lambdaServletInstanceFactory(
                            (Class<? extends RequestHandler>) context.classProxy(lambda.getClassName()),
                            beanContainerBuildItem.getValue()))
                    .addMapping("/__lambda")
                    .build());
        }
        final File bootstrap = new File("target/bundle/bootstrap");
        bootstrap.getParentFile().mkdirs();
        try (final InputStream stream = getClass().getResourceAsStream("/bootstrap");
                final FileOutputStream outputStream = new FileOutputStream(bootstrap)) {
            byte[] bytes = new byte[4096];
            int read;
            while ((read = stream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }
}
