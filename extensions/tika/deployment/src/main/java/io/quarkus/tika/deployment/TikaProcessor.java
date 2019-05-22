package io.quarkus.tika.deployment;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.tika.detect.Detector;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.parser.Parser;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.resteasy.common.deployment.ResteasyJaxrsProviderBuildItem;
import io.quarkus.tika.runtime.jaxrs.TikaContentReader;
import io.quarkus.tika.runtime.jaxrs.TikaMetadataReader;

public class TikaProcessor {

    private static final Set<String> NATIVE_READY_PARSERS = Arrays.stream(new String[] {
            "org.apache.tika.parser.csv.TextAndCSVParser",
            "org.apache.tika.parser.txt.TXTParser",
            "org.apache.tika.parser.odf.OpenDocumentParser",
            "org.apache.tika.parser.pdf.PDFParser"
    }).collect(Collectors.toSet());

    @BuildStep
    public void produceRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
        //See https://issues.apache.org/jira/browse/PDFBOX-4548
        resource.produce(new RuntimeInitializedClassBuildItem("org.apache.pdfbox.pdmodel.font.PDType1Font"));
    }

    @BuildStep
    void produceTikaJaxrsProvider(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(TikaContentReader.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(TikaMetadataReader.class.getName()));
    }

    @BuildStep
    public void produceTikaCoreResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @BuildStep
    public void produceTikaParsersResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/parser/pdf/PDFParser.properties"));
    }

    @BuildStep
    public void producePdfBoxResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/pdfbox/resources/glyphlist/additional.txt"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/pdfbox/resources/glyphlist/glyphlist.txt"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/pdfbox/resources/glyphlist/zapfdingbats.txt"));
    }

    @BuildStep
    public void produceTikaParsersProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) throws Exception {
        produceTikaServiceProviders(serviceProvider,
                Parser.class.getName(),
                Detector.class.getName(),
                EncodingDetector.class.getName());
    }

    private void produceTikaServiceProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider,
            String... serviceProviderNames) throws Exception {
        for (String serviceProviderName : serviceProviderNames) {
            Set<String> availableProviderClasses = ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                    "META-INF/services/" + serviceProviderName);
            Predicate<String> pred = p -> !Parser.class.getName().equals(serviceProviderName)
                    || NATIVE_READY_PARSERS.contains(p);
            serviceProvider.produce(new ServiceProviderBuildItem(serviceProviderName,
                    availableProviderClasses.stream().filter(pred).collect(Collectors.toList())));
        }
    }

}
