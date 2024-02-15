package io.quarkus.annotation.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Completion;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jboss.jdeparser.JDeparser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.quarkus.annotation.processor.documentation.config.ConfigDocExtensionProcessor;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.util.Types;
import io.quarkus.annotation.processor.extension.ExtensionBuildProcessor;
import io.quarkus.annotation.processor.generate_doc.LegacyConfigDocExtensionProcessor;
import io.quarkus.annotation.processor.util.Config;
import io.quarkus.annotation.processor.util.Utils;

@SupportedOptions({ Options.LEGACY_CONFIG_ROOT, Options.GENERATE_DOC, Options.GENERATE_LEGACY_CONFIG_DOC })
public class ExtensionAnnotationProcessor extends AbstractProcessor {

    private static final String DEBUG = "debug-extension-annotation-processor";
    private static final String ARTIFACT_DEPLOYMENT_SUFFIX = "-deployment";
    private static final String NAME_RUNTIME_SUFFIX = " - Runtime";
    private static final String NAME_DEPLOYMENT_SUFFIX = " - DEPLOYMENT";

    private List<ExtensionProcessor> extensionProcessors;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        List<ExtensionProcessor> extensionProcessors = new ArrayList<>();
        extensionProcessors.add(new ExtensionBuildProcessor());

        boolean skipDocs = Boolean.getBoolean("skipDocs") || Boolean.getBoolean("quickly");
        boolean generateDoc = !skipDocs && !"false".equals(processingEnv.getOptions().get(Options.GENERATE_DOC));

        // for now, we generate the old config doc by default but we will change this behavior soon
        if (generateDoc) {
            extensionProcessors.add(new ConfigDocExtensionProcessor());

            if (!"false".equals(processingEnv.getOptions().get(Options.GENERATE_LEGACY_CONFIG_DOC))) {
                extensionProcessors.add(new LegacyConfigDocExtensionProcessor());
            }
        }

        this.extensionProcessors = Collections.unmodifiableList(extensionProcessors);

        Utils utils = new Utils(processingEnv);

        boolean useConfigMapping = !Boolean
                .parseBoolean(utils.processingEnv().getOptions().getOrDefault(Options.LEGACY_CONFIG_ROOT, "false"));
        boolean debug = Boolean.getBoolean(DEBUG);

        Extension extension = getExtension(processingEnv);
        Config config = new Config(extension, useConfigMapping, debug);

        if (!useConfigMapping) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Extension " + extension.artifactId()
                    + " config implementation is deprecated. Please migrate to use @ConfigMapping: https://quarkus.io/guides/writing-extensions#configuration");
        }

        for (ExtensionProcessor extensionProcessor : extensionProcessors) {
            extensionProcessor.init(config, utils);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Types.SUPPORTED_ANNOTATIONS_TYPES;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member,
            String userText) {
        return Collections.emptySet();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (ExtensionProcessor extensionProcessor : extensionProcessors) {
                extensionProcessor.process(annotations, roundEnv);
            }

            if (roundEnv.processingOver()) {
                for (ExtensionProcessor extensionProcessor : extensionProcessors) {
                    extensionProcessor.finalizeProcessing();
                }
            }
            return true;
        } finally {
            JDeparser.dropCaches();
        }
    }

    /**
     * This is not exactly pretty but it's actually not easy to get the artifact id of the current artifact.
     * One option would be to pass it through the annotation processor but it's not exactly ideal.
     */
    private Extension getExtension(ProcessingEnvironment processingEnv) {
        Path pom;

        try {
            pom = Paths.get(processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "dummy").toUri())
                    .getParent().getParent().getParent().resolve("pom.xml").toAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to determine path to pom.xml");
        }

        Document doc;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(pom.toFile());
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            throw new IllegalStateException("Unable parse pom file: " + pom, e);
        }

        return getExtensionFromPom(pom, doc);
    }

    private Extension getExtensionFromPom(Path pom, Document doc) {
        String parentGroupId = null;
        String artifactId = null;
        String groupId = null;
        String name = null;

        NodeList children = doc.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (groupId != null && artifactId != null && name != null) {
                break;
            }

            Node child = children.item(i);

            if ("parent".equals(child.getNodeName())) {
                NodeList parentChildren = child.getChildNodes();
                for (int j = 0; j < parentChildren.getLength(); j++) {
                    Node parentChild = parentChildren.item(j);
                    if ("groupId".equals(parentChild.getNodeName())) {
                        parentGroupId = parentChild.getTextContent() != null ? parentChild.getTextContent().trim() : null;
                        ;
                        break;
                    }
                }
                continue;
            }
            if ("groupId".equals(child.getNodeName())) {
                groupId = child.getTextContent() != null ? child.getTextContent().trim() : null;
                continue;
            }
            if ("artifactId".equals(child.getNodeName())) {
                artifactId = child.getTextContent() != null ? child.getTextContent().trim() : null;
                continue;
            }
            if ("name".equals(child.getNodeName())) {
                name = child.getTextContent() != null ? child.getTextContent().trim() : null;
                continue;
            }
        }

        if (groupId == null) {
            groupId = parentGroupId;
        }

        if (groupId == null || groupId.isBlank() || artifactId == null || artifactId.isBlank()) {
            throw new IllegalStateException("Unable to determine artifact coordinates from: " + pom);
        }

        if (artifactId.endsWith(ARTIFACT_DEPLOYMENT_SUFFIX)) {
            artifactId = artifactId.substring(0, artifactId.length() - ARTIFACT_DEPLOYMENT_SUFFIX.length());
        }

        if (name != null) {
            if (name.endsWith(NAME_DEPLOYMENT_SUFFIX)) {
                name = name.substring(0, name.length() - NAME_DEPLOYMENT_SUFFIX.length());
            } else if (name.endsWith(NAME_RUNTIME_SUFFIX)) {
                name = name.substring(0, name.length() - NAME_RUNTIME_SUFFIX.length());
            }
        }

        return new Extension(groupId, artifactId, name);
    }
}
