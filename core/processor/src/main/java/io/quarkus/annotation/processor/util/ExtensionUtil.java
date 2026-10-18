package io.quarkus.annotation.processor.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic.Kind;
import javax.tools.StandardLocation;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.quarkus.annotation.processor.Options;
import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.Extension.NameSource;
import io.quarkus.annotation.processor.documentation.config.model.ExtensionModule;
import io.quarkus.annotation.processor.documentation.config.model.ExtensionModule.ExtensionModuleType;

public final class ExtensionUtil {

    private static final String RUNTIME_MARKER_FILE = "META-INF/quarkus-extension.properties";

    private static final String ARTIFACT_DEPLOYMENT_SUFFIX = "-deployment";
    private static final String NAME_QUARKUS_PREFIX = "Quarkus - ";
    private static final String NAME_RUNTIME_SUFFIX = " - Runtime";
    private static final String NAME_DEPLOYMENT_SUFFIX = " - Deployment";

    private final ProcessingEnvironment processingEnv;
    private final FilerUtil filerUtil;

    ExtensionUtil(ProcessingEnvironment processingEnv, FilerUtil filerUtil) {
        this.processingEnv = processingEnv;
        this.filerUtil = filerUtil;
    }

    /**
     * This is not exactly pretty but it's actually not easy to get the artifact id of the current artifact.
     * One option would be to pass it through the annotation processor but it's not exactly ideal.
     */
    public ExtensionModule getExtensionModule() {
        Optional<Path> pom = filerUtil.getPomPath();

        if (pom.isEmpty()) {
            return ExtensionModule.createNotDetected();
        }

        Document doc;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(pom.get().toFile());
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse pom file: " + pom, e);
        }

        return getExtensionModuleFromPom(pom.get(), doc);
    }

    private ExtensionModule getExtensionModuleFromPom(Path pom, Document doc) {
        String parentGroupId = null;
        String artifactId = null;
        String groupId = null;
        String name = null;
        String guideUrl = null;

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
            processingEnv.getMessager().printMessage(Kind.WARNING, "Unable to determine artifact coordinates from: " + pom);
            return ExtensionModule.createNotDetected();
        }

        ExtensionModuleType moduleType = detectExtensionModuleType(artifactId);

        String extensionArtifactId;
        if (moduleType == ExtensionModuleType.DEPLOYMENT) {
            extensionArtifactId = artifactId.substring(0, artifactId.length() - ARTIFACT_DEPLOYMENT_SUFFIX.length());
        } else {
            extensionArtifactId = artifactId;
        }

        NameSource extensionNameSource;
        Optional<ExtensionMetadata> extensionMetadata = getExtensionMetadata();
        if (extensionMetadata.isPresent()) {
            name = extensionMetadata.get().name();
            extensionNameSource = NameSource.EXTENSION_METADATA;
            guideUrl = extensionMetadata.get().guideUrl();
        } else if (name != null) {
            extensionNameSource = NameSource.POM_XML;
        } else {
            extensionNameSource = NameSource.NONE;
        }

        String extensionName = name;
        if (extensionName != null) {
            if (extensionName.startsWith(NAME_QUARKUS_PREFIX)) {
                extensionName = extensionName.substring(NAME_QUARKUS_PREFIX.length()).trim();
            }
            if (moduleType == ExtensionModuleType.DEPLOYMENT && extensionName.endsWith(NAME_DEPLOYMENT_SUFFIX)) {
                extensionName = extensionName.substring(0, extensionName.length() - NAME_DEPLOYMENT_SUFFIX.length());
            }
            if (moduleType == ExtensionModuleType.RUNTIME && extensionName.endsWith(NAME_RUNTIME_SUFFIX)) {
                extensionName = extensionName.substring(0, extensionName.length() - NAME_RUNTIME_SUFFIX.length());
            }
        }

        return ExtensionModule.of(groupId, artifactId, moduleType,
                Extension.of(groupId, extensionArtifactId, extensionName, extensionNameSource, guideUrl,
                        Boolean.parseBoolean(
                                processingEnv.getOptions().getOrDefault(Options.SPLIT_ON_CONFIG_ROOT_DESCRIPTION, "false"))));
    }

    private Optional<ExtensionMetadata> getExtensionMetadata() {
        Optional<Map<String, Object>> extensionMetadata = filerUtil.getExtensionMetadata();

        if (extensionMetadata.isEmpty()) {
            return Optional.empty();
        }

        String extensionName = (String) extensionMetadata.get().get("name");
        if (extensionName == null || extensionName.isBlank()) {
            // we at least want the extension name set there
            return Optional.empty();
        }

        extensionName = extensionName.trim();

        Map<String, Object> metadata = (Map<String, Object>) extensionMetadata.get().get("metadata");
        String guideUrl = null;
        if (metadata != null) {
            guideUrl = (String) metadata.get("guide");
            if (guideUrl == null || guideUrl.isBlank()) {
                guideUrl = null;
            } else {
                guideUrl = guideUrl.trim();
            }
        }

        return Optional.of(new ExtensionMetadata(extensionName, guideUrl));
    }

    private record ExtensionMetadata(String name, String guideUrl) {
    }

    private ExtensionModuleType detectExtensionModuleType(String artifactId) {
        try {
            Path runtimeMarkerFile = Paths
                    .get(processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", RUNTIME_MARKER_FILE).toUri());
            if (Files.exists(runtimeMarkerFile)) {
                return ExtensionModuleType.RUNTIME;
            }
        } catch (IOException e) {
            // ignore, the file doesn't exist
        }

        if (artifactId.endsWith(ARTIFACT_DEPLOYMENT_SUFFIX)) {
            return ExtensionModuleType.DEPLOYMENT;
        }

        return ExtensionModuleType.UNKNOWN;
    }
}
