package io.quarkus.cxf.deployment;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.cxf.runtime.CXFQuarkusServlet;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.undertow.deployment.ServletBuildItem;

/**
 * Processor that finds CXF web service classes in the deployment
 */
public class CxfProcessor {

    private static final String JAX_WS_CXF_SERVLET = "org.apache.cxf.transport.servlet.CXFNonSpringServlet;";

    private static final DotName WEBSERVICE_ANNOTATION = DotName.createSimple("javax.jws.WebService");

    /**
     * JAX-RS configuration.
     */
    CxfConfig cxfConfig;

    @BuildStep
    public void build(
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ServletBuildItem> servlet,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {
        IndexView index = combinedIndexBuildItem.getIndex();

        for (AnnotationInstance annotation : index.getAnnotations(WEBSERVICE_ANNOTATION)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(true, true, annotation.target().asClass().name().toString()));
            }
        }

        feature.produce(new FeatureBuildItem(FeatureBuildItem.CXF));

        String mappingPath = getMappingPath(cxfConfig.path);
        servlet.produce(ServletBuildItem.builder(JAX_WS_CXF_SERVLET, CXFQuarkusServlet.class.getName())
                .setLoadOnStartup(1).addMapping(mappingPath).setAsyncSupported(true).build());
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, CXFQuarkusServlet.class.getName()));

        for (Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.endpoints.entrySet()) {
            CXFQuarkusServlet.publish(webServicesByPath.getKey(), webServicesByPath.getValue().implementor);
        }

    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        return Arrays.asList(
                new RuntimeInitializedClassBuildItem("com.sun.xml.fastinfoset.stax.StAXDocumentParser"),
                new RuntimeInitializedClassBuildItem("com.sun.xml.fastinfoset.stax.StAXDocumentSerializer"));
    }

    private String getMappingPath(String path) {
        String mappingPath;
        if (path.endsWith("/")) {
            mappingPath = path + "*";
        } else {
            mappingPath = path + "/*";
        }
        return mappingPath;
    }

    @BuildStep
    public void createBeans(
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans) throws Exception {
        for (Entry<String, CxfEndpointConfig> webServicesByPath : cxfConfig.endpoints.entrySet()) {
            ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
            String webServiceName = webServicesByPath.getValue().implementor;
            String producerClassName = webServiceName + "Producer";
            ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                    .className(producerClassName)
                    .build();
            classCreator.addAnnotation(ApplicationScoped.class);

            unremovableBeans.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion(producerClassName)));

            MethodCreator namedWebServiceMethodCreator = classCreator.getMethodCreator(
                    "createWebService_" + HashUtil.sha1(webServiceName),
                    webServiceName);
            namedWebServiceMethodCreator.addAnnotation(ApplicationScoped.class);
            namedWebServiceMethodCreator.addAnnotation(Unremovable.class);
            namedWebServiceMethodCreator.addAnnotation(Produces.class);
            namedWebServiceMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                    new AnnotationValue[] { AnnotationValue.createStringValue("value", webServiceName) }));

            ResultHandle namedWebService = namedWebServiceMethodCreator
                    .newInstance(MethodDescriptor.ofConstructor(webServiceName));

            namedWebServiceMethodCreator.returnValue(namedWebService);
            classCreator.close();
        }

    }

}
