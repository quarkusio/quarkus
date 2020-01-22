package io.quarkus.tika.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.tika.detect.Detector;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.parser.Parser;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.JniBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.tika.TikaParseException;
import io.quarkus.tika.runtime.TikaConfiguration;
import io.quarkus.tika.runtime.TikaParserParameter;
import io.quarkus.tika.runtime.TikaParserProducer;
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
            { "odf", "org.apache.tika.parser.odf.OpenDocumentParser" }
    }).collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));

    private TikaConfiguration config;

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.builder().addBeanClasses(TikaParserProducer.class).build();
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    TikaParsersConfigBuildItem initializeTikaParser(BeanContainerBuildItem beanContainer, TikaRecorder recorder)
            throws Exception {
        Map<String, List<TikaParserParameter>> parsersConfig = getSupportedParserConfig(config.tikaConfigPath, config.parsers,
                config.parserOptions, config.parser);
        recorder.initTikaParser(beanContainer.getValue(), config, parsersConfig);
        return new TikaParsersConfigBuildItem(parsersConfig);
    }

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.TIKA);
    }

    @BuildStep
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
    public void registerTikaCoreResources(BuildProducer<NativeImageResourceBuildItem> resource) throws Exception {
        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @BuildStep
    public void registerTikaParsersResources(BuildProducer<NativeImageResourceBuildItem> resource) throws Exception {
        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/parser/pdf/PDFParser.properties"));
    }

    @BuildStep
    public void registerPdfBoxResources(BuildProducer<NativeImageResourceBuildItem> resource) throws Exception {
        resource.produce(new NativeImageResourceBuildItem("org/apache/pdfbox/resources/glyphlist/additional.txt"));
        resource.produce(new NativeImageResourceBuildItem("org/apache/pdfbox/resources/glyphlist/glyphlist.txt"));
        resource.produce(new NativeImageResourceBuildItem("org/apache/pdfbox/resources/glyphlist/zapfdingbats.txt"));
    }

    @BuildStep
    public void registerTikaProviders(BuildProducer<ServiceProviderBuildItem> serviceProvider,
            TikaParsersConfigBuildItem parserConfigItem) throws Exception {
        serviceProvider.produce(
                new ServiceProviderBuildItem(Parser.class.getName(),
                        new ArrayList<>(parserConfigItem.getConfiguration().keySet())));
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

    static Map<String, List<TikaParserParameter>> getSupportedParserConfig(Optional<String> tikaConfigPath,
            Optional<String> requiredParsers,
            Map<String, Map<String, String>> parserParamMaps,
            Map<String, String> parserAbbreviations) throws Exception {
        Predicate<String> pred = p -> !NOT_NATIVE_READY_PARSERS.contains(p);
        List<String> providerNames = getProviderNames(Parser.class.getName());
        if (tikaConfigPath.isPresent() || !requiredParsers.isPresent()) {
            return providerNames.stream().filter(pred).collect(Collectors.toMap(Function.identity(),
                    p -> Collections.<TikaParserParameter> emptyList()));
        } else {
            List<String> abbreviations = Arrays.stream(requiredParsers.get().split(",")).map(s -> s.trim())
                    .collect(Collectors.toList());
            Map<String, String> fullNamesAndAbbreviations = abbreviations.stream()
                    .collect(Collectors.toMap(p -> getParserNameFromConfig(p, parserAbbreviations), Function.identity()));

            return providerNames.stream().filter(pred).filter(p -> fullNamesAndAbbreviations.containsKey(p))
                    .collect(Collectors.toMap(Function.identity(),
                            p -> getParserConfig(p, parserParamMaps.get(fullNamesAndAbbreviations.get(p)))));
        }
    }

    static List<TikaParserParameter> getParserConfig(String parserName, Map<String, String> parserParamMap) {
        List<TikaParserParameter> parserParams = new LinkedList<>();
        if (parserParamMap != null) {
            for (Map.Entry<String, String> entry : parserParamMap.entrySet()) {
                String paramName = unhyphenate(entry.getKey());
                String paramType = getParserParamType(parserName, paramName);
                parserParams.add(new TikaParserParameter(paramName, entry.getValue(), paramType));
            }
        }
        return parserParams;
    }

    private static String getParserNameFromConfig(String abbreviation, Map<String, String> parserAbbreviations) {
        if (PARSER_ABBREVIATIONS.containsKey(abbreviation)) {
            return PARSER_ABBREVIATIONS.get(abbreviation);
        }

        if (parserAbbreviations.containsKey(abbreviation)) {
            return parserAbbreviations.get(abbreviation);
        }

        throw new IllegalStateException("The custom abbreviation `" + abbreviation
                + "` can not be resolved to a parser class name, please set a "
                + "quarkus.tika.parser-name." + abbreviation + " property");
    }

    // Convert a property name such as "sort-by-position" to "sortByPosition"   
    private static String unhyphenate(String paramName) {
        StringBuilder sb = new StringBuilder();
        String[] words = paramName.split("-");
        for (int i = 0; i < words.length; i++) {
            sb.append(i > 0 ? capitalize(words[i]) : words[i]);
        }
        return sb.toString();
    }

    private static String capitalize(String paramName) {
        char[] chars = paramName.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    // TODO: Remove the reflection code below once TikaConfig becomes capable
    // of loading the parameters without the type attribute: TIKA-2944

    private static Class<?> loadParserClass(String parserName) {
        try {
            return TikaProcessor.class.getClassLoader().loadClass(parserName);
        } catch (Throwable t) {
            final String errorMessage = "Parser " + parserName + " can not be loaded";
            throw new TikaParseException(errorMessage);
        }
    }

    private static String getParserParamType(String parserName, String paramName) {
        try {
            Class<?> parserClass = loadParserClass(parserName);
            String paramType = parserClass.getMethod("get" + capitalize(paramName), new Class[] {}).getReturnType()
                    .getSimpleName().toLowerCase();
            if (paramType.equals(boolean.class.getSimpleName())) {
                // TikaConfig Param class does not recognize 'boolean', only 'bool'
                // This whole reflection code is temporary anyway
                paramType = "bool";
            }
            return paramType;
        } catch (Throwable t) {
            final String errorMessage = "Parser " + parserName + " has no " + paramName + " property";
            throw new TikaParseException(errorMessage);
        }
    }
}
