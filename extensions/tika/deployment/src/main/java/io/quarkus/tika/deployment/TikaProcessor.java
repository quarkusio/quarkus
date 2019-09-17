package io.quarkus.tika.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.tika.detect.Detector;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.parser.Parser;
import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.substrate.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ServiceProviderBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.tika.runtime.TikaConfiguration;
import io.quarkus.tika.runtime.TikaRecorder;

public class TikaProcessor {
    private static final Set<String> NOT_NATIVE_READY_PARSERS = Arrays.stream(new String[] {
            "org.apache.tika.parser.mat.MatParser",
            "org.apache.tika.parser.journal.GrobidRESTParser",
            "org.apache.tika.parser.journal.JournalParser",
            "org.apache.tika.parser.jdbc.SQLite3Parser",
            "org.apache.tika.parser.mail.RFC822Parser",
            "org.apache.tika.parser.pkg.CompressorParser",
            "org.apache.tika.parser.geo.topic.GeoParser"
    }).collect(Collectors.toSet());

    private static final Map<String, String> PARSER_ABBREVIATIONS = Arrays.stream(new String[][] {
            { "pdf", "org.apache.tika.parser.pdf.PDFParser" },
    }).collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));

    private TikaConfiguration config;

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void initializeTikaParser(BeanContainerBuildItem beanContainer, TikaRecorder recorder) throws Exception {
        recorder.initTikaParser(beanContainer.getValue(), config, getSupportedParserNames(config.parsers));
    }

    @BuildStep(providesCapabilities = "io.quarkus.tika")
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.TIKA);
    }

    @BuildStep
    void setupJni(BuildProducer<JniBuildItem> jniProducer) {
        jniProducer.produce(new JniBuildItem());
    }

    @BuildStep
    public void registerRuntimeInitializedClasses(BuildProducer<RuntimeInitializedClassBuildItem> resource) {
        //org.apache.tika.parser.pdf.PDFParser (https://issues.apache.org/jira/browse/PDFBOX-4548)
        resource.produce(new RuntimeInitializedClassBuildItem("org.apache.pdfbox.pdmodel.font.PDType1Font"));
    }

    @BuildStep
    public void registerTikaCoreResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @BuildStep
    public void registerTikaParsersResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/tika/parser/pdf/PDFParser.properties"));
    }

    @BuildStep
    public void registerPdfBoxResources(BuildProducer<SubstrateResourceBuildItem> resource) throws Exception {
        resource.produce(new SubstrateResourceBuildItem("org/apache/pdfbox/resources/glyphlist/additional.txt"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/pdfbox/resources/glyphlist/glyphlist.txt"));
        resource.produce(new SubstrateResourceBuildItem("org/apache/pdfbox/resources/glyphlist/zapfdingbats.txt"));
    }

    @BuildStep
    public void registerTikaProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider) throws Exception {
        serviceProvider.produce(
                new ServiceProviderBuildItem(Parser.class.getName(), getSupportedParserNames(config.parsers)));
        serviceProvider.produce(
                new ServiceProviderBuildItem(Detector.class.getName(), getProviderNames(Detector.class.getName())));
        serviceProvider.produce(
                new ServiceProviderBuildItem(EncodingDetector.class.getName(),
                        getProviderNames(EncodingDetector.class.getName())));
    }

    static List<String> getProviderNames(String serviceProviderName) throws Exception {
        return new ArrayList<>(ServiceUtil.classNamesNamedIn(TikaProcessor.class.getClassLoader(),
                "META-INF/services/" + serviceProviderName));
    }

    static List<String> getSupportedParserNames(Optional<String> requiredParsers) throws Exception {
        Predicate<String> pred = p -> !NOT_NATIVE_READY_PARSERS.contains(p);
        List<String> providerNames = getProviderNames(Parser.class.getName());
        if (!requiredParsers.isPresent()) {
            return providerNames.stream().filter(pred).collect(Collectors.toList());
        } else {
            List<String> abbreviations = Arrays.stream(requiredParsers.get().split(",")).map(s -> s.trim())
                    .collect(Collectors.toList());
            Set<String> requiredParsersFullNames = abbreviations.stream()
                    .map(p -> getParserNameFromConfig(p)).collect(Collectors.toSet());

            return providerNames.stream().filter(pred).filter(p -> requiredParsersFullNames.contains(p))
                    .collect(Collectors.toList());
        }
    }

    private static String getParserNameFromConfig(String abbreviation) {
        if (PARSER_ABBREVIATIONS.containsKey(abbreviation)) {
            return PARSER_ABBREVIATIONS.get(abbreviation);
        }
        try {
            return ConfigProvider.getConfig().getValue(abbreviation, String.class);
        } catch (NoSuchElementException ex) {
            throw new IllegalStateException("The custom abbreviation " + abbreviation
                    + " can not be resolved to a parser class name");
        }
    }
}
