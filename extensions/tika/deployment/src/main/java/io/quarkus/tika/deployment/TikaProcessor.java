package io.quarkus.tika.deployment;

import java.util.Set;

import org.apache.tika.detect.Detector;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.parser.Parser;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.tika.runtime.jaxrs.TikaMessageBodyReader;

public class TikaProcessor {

    @BuildStep
    void produceTikaProvider(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(TikaMessageBodyReader.class.getName()));
    }

    @BuildStep
    public void produceTikaCoreResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @BuildStep
    public void produceTikaParsersParsers(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + Parser.class.getName()));
        Set<String> availableParsers = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + Parser.class.getName());
        for (String parser : availableParsers) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, parser));
        }
        unremovableBeans.produce(new UnremovableBeanBuildItem(
                b -> availableParsers.contains(b.getBeanClass().toString())));

    }

    @BuildStep
    public void produceTikaParsersDetectors(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + Detector.class.getName()));
        Set<String> availableDetectors = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + Detector.class.getName());
        for (String detector : availableDetectors) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, detector));
        }
        unremovableBeans.produce(new UnremovableBeanBuildItem(
                b -> availableDetectors.contains(b.getBeanClass().toString())));

    }

    @BuildStep
    public void produceTikaParsersEncodingDetectors(BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("META-INF/services/" + EncodingDetector.class.getName()));
        Set<String> availableDetectors = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/" + EncodingDetector.class.getName());
        for (String detector : availableDetectors) {
            reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, detector));
        }
        unremovableBeans.produce(new UnremovableBeanBuildItem(
                b -> availableDetectors.contains(b.getBeanClass().toString())));

    }

}
