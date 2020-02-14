package io.quarkus.tika.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.*;
import io.quarkus.deployment.util.ReflectUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.tika.TikaParseException;
import io.quarkus.tika.runtime.TikaConfiguration;
import io.quarkus.tika.runtime.TikaParserProducer;
import io.quarkus.tika.runtime.TikaRecorder;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.xslf.usermodel.XSLFTheme;
import org.apache.poi.xwpf.usermodel.XWPFSettings;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.tika.detect.Detector;
import org.apache.tika.detect.EncodingDetector;
import org.apache.tika.parser.Parser;
import org.apache.xerces.parsers.XIncludeAwareParserConfiguration;
import org.apache.xerces.xni.parser.XMLParserConfiguration;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.impl.ThemeDocumentImpl;
import org.openxmlformats.schemas.presentationml.x2006.main.STPlaceholderType;
import org.openxmlformats.schemas.presentationml.x2006.main.impl.PresentationDocumentImpl;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CalcChainDocumentImpl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.impl.DocumentDocumentImpl;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerFactory;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    public static final String PDF_PARSER_NAME = "pdf";
    public static final String OOXML_PARSER_NAME = "ooxml";

    private static final Map<String, String> PARSER_ABBREVIATIONS = Arrays.stream(new String[][] {
            { "pdf", "org.apache.tika.parser.pdf.PDFParser" },
            { "odf", "org.apache.tika.parser.odf.OpenDocumentParser" },
            { OOXML_PARSER_NAME, "org.apache.tika.parser.microsoft.ooxml.OOXMLParser" },
    }).collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return AdditionalBeanBuildItem.unremovableOf(TikaParserProducer.class);
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
    public void registerTikaCoreResources(BuildProducer<NativeImageResourceBuildItem> resource) {
        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/mime/tika-mimetypes.xml"));
        resource.produce(new NativeImageResourceBuildItem("org/apache/tika/parser/external/tika-external-parsers.xml"));
    }

    @BuildStep
    public void registerPdfParser(TikaConfiguration configuration,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer) {
        if (doNotIncludeParser(configuration, PDF_PARSER_NAME)) {
            return;
        }

        //org.apache.tika.parser.pdf.PDFParser (https://issues.apache.org/jira/browse/PDFBOX-4548)
        runtimeProducer.produce(new RuntimeInitializedClassBuildItem("org.apache.pdfbox.pdmodel.font.PDType1Font"));

        nativeImageResourceProducer
                .produce(new NativeImageResourceBuildItem("org/apache/tika/parser/pdf/PDFParser.properties"));

        nativeImageResourceProducer
                .produce(new NativeImageResourceBuildItem("org/apache/pdfbox/resources/glyphlist/additional.txt"));
        nativeImageResourceProducer
                .produce(new NativeImageResourceBuildItem("org/apache/pdfbox/resources/glyphlist/glyphlist.txt"));
        nativeImageResourceProducer
                .produce(new NativeImageResourceBuildItem("org/apache/pdfbox/resources/glyphlist/zapfdingbats.txt"));
    }

    @BuildStep
    public void registerOOXMLParser(TikaConfiguration configuration,
            BuildProducer<ReflectiveClassBuildItem> resource,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundle,
            BuildProducer<NativeImageResourceDirectoryBuildItem> resourceDirectory,
            BuildProducer<NativeImageResourceBuildItem> resourceBuildItem) throws Exception {
        //https://github.com/quarkusio/quarkus/issues/6549

        if (doNotIncludeParser(configuration, OOXML_PARSER_NAME)) {
            return;
        }

        Map<String, Map<String, String>> fileFormatSupport = configuration.parserFileFormatSupport;

        boolean docxSupport = getOOXMLFileFormatSupport(fileFormatSupport, "docx", true);
        boolean pptxSupport = getOOXMLFileFormatSupport(fileFormatSupport, "pptx", false);
        boolean xlsxSupport = getOOXMLFileFormatSupport(fileFormatSupport, "xlsx", false);

        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xerces.impl.dv.dtd.DTDDVFactoryImpl"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xerces.impl.msg.XMLMessageFormatter"));

        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.poi.POIXMLTextExtractor"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.poi.openxml4j.opc.ZipPackagePart"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.poi.openxml4j.opc.PackagePart"));

        if (docxSupport) {
            resource.produce(new ReflectiveClassBuildItem(true, true, true, XWPFSettings.class.getName()));
            resource.produce(new ReflectiveClassBuildItem(true, true, true, XWPFStyles.class.getName()));
        }

        if (pptxSupport) {
            resource.produce(new ReflectiveClassBuildItem(true, true, true, STPlaceholderType.Enum.class.getName()));
            ReflectUtil.getAllClassesFromPackage(XSLFTheme.class.getPackage().getName(), POIXMLDocumentPart.class)
                    .forEach(aClass -> resource.produce(
                            new ReflectiveClassBuildItem(true, true, true, aClass)));
        }

        resource.produce(new ReflectiveClassBuildItem(true, true, true, POIXMLDocumentPart.class.getName()));

        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xmlbeans.impl.values.XmlComplexContentImpl"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xmlbeans.impl.schema.SchemaTypeLoaderImpl"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xmlbeans.impl.schema.SchemaTypeImpl"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xmlbeans.impl.schema.SchemaTypeSystemImpl"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xmlbeans.impl.store.Cursor"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xmlbeans.impl.store.Xobj"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xmlbeans.impl.store.Xobj.AttrXobj"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xmlbeans.impl.store.Xobj.ElementXobj"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xmlbeans.impl.store.Xobj.DocumentXobj"));
        resource.produce(new ReflectiveClassBuildItem(true, true, "org.apache.xmlbeans.impl.store.Locale"));

        resource.produce(new ReflectiveClassBuildItem(true, true,
                "schemaorg_apache_xmlbeans.system.sD023D6490046BA0250A839A9AD24C443.TypeSystemHolder"));

        if (pptxSupport) {
            ReflectUtil.getAllClassesFromPackage(ThemeDocumentImpl.class.getPackage().getName(), XmlObject.class)
                    .forEach(aClass -> resource.produce(
                            new ReflectiveClassBuildItem(true, true, true, aClass)));
            ReflectUtil.getAllClassesFromPackage(
                    PresentationDocumentImpl.class.getPackage().getName(),
                    XmlObject.class)
                    .forEach(aClass -> resource.produce(
                            new ReflectiveClassBuildItem(true, true, true, aClass)));
        }

        if (xlsxSupport) {
            ReflectUtil.getAllClassesFromPackage(CalcChainDocumentImpl.class.getPackage().getName(), XmlObject.class)
                    .forEach(aClass -> resource.produce(
                            new ReflectiveClassBuildItem(true, true, true, aClass)));
        }

        if (docxSupport) {
            ReflectUtil.getAllClassesFromPackage(DocumentDocumentImpl.class.getPackage().getName(), XmlObject.class)
                    .forEach(aClass -> resource.produce(
                            new ReflectiveClassBuildItem(true, true, true, aClass)));
        }

        serviceProvider.produce(
                new ServiceProviderBuildItem(XMLParserConfiguration.class.getName(),
                        Arrays.asList(XIncludeAwareParserConfiguration.class.getName())));
        serviceProvider.produce(
                new ServiceProviderBuildItem(SAXParserFactory.class.getName(),
                        getProviderNames(SAXParserFactory.class.getName())));
        serviceProvider.produce(
                new ServiceProviderBuildItem(TransformerFactory.class.getName(),
                        getProviderNames(TransformerFactory.class.getName())));

        resourceBundle.produce(new NativeImageResourceBundleBuildItem("org.apache.xerces.impl.msg.SAXMessages"));

        resourceDirectory.produce(new NativeImageResourceDirectoryBuildItem(
                "schemaorg_apache_xmlbeans/system/sD023D6490046BA0250A839A9AD24C443"));
        resourceBuildItem.produce(new NativeImageResourceBuildItem("org/apache/xalan/res/XSLTInfo.properties"));
        resourceBuildItem.produce(new NativeImageResourceBuildItem("org/apache/xalan/internal/res/XSLTInfo.properties"));
    }

    private Boolean getOOXMLFileFormatSupport(Map<String, Map<String, String>> fileFormatSupport, String fileFormat,
            boolean defaultValue) {
        return Optional.ofNullable(fileFormatSupport.get(OOXML_PARSER_NAME))
                .filter(map -> map.containsKey(fileFormat))
                .map(map -> map.get(fileFormat))
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    private boolean doNotIncludeParser(TikaConfiguration configuration, String parserName) {
        return configuration.parsers.isPresent()
                && !getParserAbbreviations(configuration.parsers).contains(parserName);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void initializeTikaParser(BeanContainerBuildItem beanContainer, TikaRecorder recorder,
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            TikaConfiguration configuration)
            throws Exception {
        Map<String, List<TikaParserParameter>> parsers = getSupportedParserConfig(configuration.tikaConfigPath,
                configuration.parsers,
                configuration.parserOptions, configuration.parser);
        String tikaXmlConfiguration = generateTikaXmlConfiguration(parsers);

        serviceProvider.produce(new ServiceProviderBuildItem(Parser.class.getName(), new ArrayList<>(parsers.keySet())));
        serviceProvider
                .produce(new ServiceProviderBuildItem(Detector.class.getName(), getProviderNames(Detector.class.getName())));
        serviceProvider.produce(new ServiceProviderBuildItem(EncodingDetector.class.getName(),
                getProviderNames(EncodingDetector.class.getName())));

        recorder.initTikaParser(beanContainer.getValue(), configuration, tikaXmlConfiguration);
    }

    private static List<String> getProviderNames(String serviceProviderName) throws Exception {
        return new ArrayList<>(ServiceUtil.classNamesNamedIn(TikaProcessor.class.getClassLoader(),
                "META-INF/services/" + serviceProviderName));
    }

    public static Map<String, List<TikaParserParameter>> getSupportedParserConfig(Optional<String> tikaConfigPath,
            Optional<String> requiredParsers,
            Map<String, Map<String, String>> parserParamMaps,
            Map<String, String> parserAbbreviations) throws Exception {
        Predicate<String> pred = p -> !NOT_NATIVE_READY_PARSERS.contains(p);
        List<String> providerNames = getProviderNames(Parser.class.getName());
        if (tikaConfigPath.isPresent() || !requiredParsers.isPresent()) {
            return providerNames.stream().filter(pred).collect(Collectors.toMap(Function.identity(),
                    p -> Collections.<TikaParserParameter> emptyList()));
        } else {
            List<String> abbreviations = getParserAbbreviations(requiredParsers);
            Map<String, String> fullNamesAndAbbreviations = abbreviations.stream()
                    .collect(Collectors.toMap(p -> getParserNameFromConfig(p, parserAbbreviations), Function.identity()));
            return providerNames.stream().filter(pred).filter(p -> fullNamesAndAbbreviations.containsKey(p))
                    .collect(Collectors.toMap(Function.identity(),
                            p -> getParserConfig(p, parserParamMaps.get(fullNamesAndAbbreviations.get(p)))));
        }
    }

    private static List<String> getParserAbbreviations(Optional<String> requiredParsers) {
        return Arrays.stream(requiredParsers.get().split(",")).map(s -> s.trim())
                .collect(Collectors.toList());
    }

    private static String generateTikaXmlConfiguration(Map<String, List<TikaParserParameter>> parserConfig) {
        StringBuilder tikaXmlConfigurationBuilder = new StringBuilder();
        tikaXmlConfigurationBuilder.append("<properties>");
        tikaXmlConfigurationBuilder.append("<parsers>");
        for (Entry<String, List<TikaParserParameter>> parserEntry : parserConfig.entrySet()) {
            tikaXmlConfigurationBuilder.append("<parser class=\"").append(parserEntry.getKey()).append("\">");
            if (!parserEntry.getValue().isEmpty()) {
                appendParserParameters(tikaXmlConfigurationBuilder, parserEntry.getValue());
            }
            tikaXmlConfigurationBuilder.append("</parser>");
        }
        tikaXmlConfigurationBuilder.append("</parsers>");
        tikaXmlConfigurationBuilder.append("</properties>");
        return tikaXmlConfigurationBuilder.toString();
    }

    private static void appendParserParameters(StringBuilder tikaXmlConfigurationBuilder,
            List<TikaParserParameter> parserParams) {
        tikaXmlConfigurationBuilder.append("<params>");
        for (TikaParserParameter parserParam : parserParams) {
            tikaXmlConfigurationBuilder.append("<param name=\"").append(parserParam.getName());
            tikaXmlConfigurationBuilder.append("\" type=\"").append(parserParam.getType()).append("\">");
            tikaXmlConfigurationBuilder.append(parserParam.getValue());
            tikaXmlConfigurationBuilder.append("</param>");
        }
        tikaXmlConfigurationBuilder.append("</params>");
    }

    private static List<TikaParserParameter> getParserConfig(String parserName, Map<String, String> parserParamMap) {
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
    public static String unhyphenate(String paramName) {
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

    public static class TikaParserParameter {
        private String name;
        private String value;
        private String type;

        public TikaParserParameter(String name, String value, String type) {
            this.name = name;
            this.value = value;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }
}
