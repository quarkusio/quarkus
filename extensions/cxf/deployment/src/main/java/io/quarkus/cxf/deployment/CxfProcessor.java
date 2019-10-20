package io.quarkus.cxf.deployment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.cxf.runtime.AbstractCxfWebServiceProducer;
import io.quarkus.cxf.runtime.CXFQuarkusServlet;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletInitParamBuildItem;

/**
 * Processor that finds JAX-RS classes in the deployment
 */
public class CxfProcessor {

    private static final String JAX_WS_CXF_SERVLET = "org.apache.cxf.transport.servlet.CXFNonSpringServlet";

    private static final DotName WEBSERVICE_ANNOTATION = DotName.createSimple("javax.jws.WebService");
    private static final DotName ADAPTER_ANNOTATION = DotName
            .createSimple("javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter");

    /**
     * JAX-RS configuration.
     */
    CxfConfig cxfConfig;

    //    @BuildStep
    //    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
    //            BuildProducer<SubstrateResourceBundleBuildItem> resourceBundle,
    //            BuildProducer<ServiceProviderBuildItem> serviceProvider) {
    //        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
    //                ProviderImpl.class.getName()));
    //        
    //        serviceProvider.produce(new ServiceProviderBuildItem(Provider.class.getName(),
    //                ProviderImpl.class.getName()));
    //    }

    @BuildStep
    public void build(
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            BuildProducer<SubstrateProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeClasses,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<CxfServerConfigBuildItem> cxfServerConfig,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) throws Exception {
        IndexView index = combinedIndexBuildItem.getIndex();

        for (AnnotationInstance annotation : index.getAnnotations(WEBSERVICE_ANNOTATION)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(true, true, annotation.target().asClass().name().toString()));
            }
        }

        for (AnnotationInstance annotation : index.getAnnotations(ADAPTER_ANNOTATION)) {
            if (annotation.target().kind() == AnnotationTarget.Kind.CLASS) {
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(true, true, annotation.target().asClass().name().toString()));
            }
            AnnotationValue value = annotation.value();
            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, true, value.asClass().name().toString()));
        }

        Map<String, String> cxfInitParameters = new HashMap<>();

        cxfServerConfig.produce(new CxfServerConfigBuildItem(cxfConfig.path, cxfInitParameters));
    }

    @BuildStep
    public void configToBuildItem(BuildProducer<WebServiceBuildItem> webServiceBuildItems) {
        for (Entry<String, String> webServicesByPath : cxfConfig.webServicesPaths.entrySet()) {
            webServiceBuildItems.produce(new WebServiceBuildItem(webServicesByPath.getValue()));
        }
    }

    @BuildStep
    public void build(
            Optional<CxfServerConfigBuildItem> cxfServerConfig,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ServletBuildItem> servlet,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ServletInitParamBuildItem> servletInitParameters) throws Exception {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.CXF));

        if (cxfServerConfig.isPresent()) {
            String path = cxfServerConfig.get().getPath();

            String mappingPath = getMappingPath(path);
            servlet.produce(ServletBuildItem.builder(JAX_WS_CXF_SERVLET, CXFQuarkusServlet.class.getName())
                    .setLoadOnStartup(1).addMapping(mappingPath).setAsyncSupported(true).build());
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, CXFQuarkusServlet.class.getName()));

            for (Entry<String, String> initParameter : cxfServerConfig.get().getInitParameters().entrySet()) {
                servletInitParameters.produce(new ServletInitParamBuildItem(initParameter.getKey(), initParameter.getValue()));
            }

            for (Entry<String, String> webServicesByPath : cxfConfig.webServicesPaths.entrySet()) {
                CXFQuarkusServlet.publish(webServicesByPath.getKey(), webServicesByPath.getValue());
            }
        }
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
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            List<WebServiceBuildItem> webServices) throws Exception {

        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedBean.produce(new GeneratedBeanBuildItem(name, data));
            }
        };

        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className("CxfWebServiceProducer")
                .superClass(AbstractCxfWebServiceProducer.class)
                .build();
        classCreator.addAnnotation(ApplicationScoped.class);

        for (WebServiceBuildItem webService : webServices) {
            unremovableBeans.produce(new UnremovableBeanBuildItem(
                    new UnremovableBeanBuildItem.BeanClassNameExclusion(webService.getWebServiceClass())));

            MethodCreator namedWebServiceMethodCreator = classCreator.getMethodCreator(
                    "createWebService_" + HashUtil.sha1(webService.getWebServiceClass()),
                    Object.class);
            namedWebServiceMethodCreator.addAnnotation(ApplicationScoped.class);
            namedWebServiceMethodCreator.addAnnotation(Produces.class);
            namedWebServiceMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                    new AnnotationValue[] { AnnotationValue.createStringValue("value", webService.getWebServiceClass()) }));

            ResultHandle namedDataSourceNameRH = namedWebServiceMethodCreator.load(webService.getWebServiceClass());

            namedWebServiceMethodCreator.returnValue(
                    namedWebServiceMethodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractCxfWebServiceProducer.class, "createWebService",
                                    Object.class, String.class),
                            namedWebServiceMethodCreator.getThis(),
                            namedDataSourceNameRH));
        }

        classCreator.close();

    }

}
