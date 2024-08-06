package io.quarkus.annotation.processor.util;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic.Kind;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.quarkus.annotation.processor.documentation.config.model.Extension;
import io.quarkus.annotation.processor.documentation.config.model.Extension.NameSource;

public final class ExtensionUtil {

    private static final String ARTIFACT_DEPLOYMENT_SUFFIX = "-deployment";
    private static final String ARTIFACT_COMMON_SUFFIX = "-common";
    private static final String ARTIFACT_INTERNAL_SUFFIX = "-internal";
    private static final String NAME_QUARKUS_PREFIX = "Quarkus - ";
    private static final String NAME_RUNTIME_SUFFIX = " - Runtime";
    private static final String NAME_DEPLOYMENT_SUFFIX = " - Deployment";
    private static final String NAME_COMMON_SUFFIX = " - Common";
    private static final String NAME_INTERNAL_SUFFIX = " - Internal";

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
    public Extension getExtension() {
        Optional<Path> pom = filerUtil.getPomPath();

        if (pom.isEmpty()) {
            return Extension.createNotDetected();
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

        return getExtensionFromPom(pom.get(), doc);
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
            processingEnv.getMessager().printMessage(Kind.WARNING, "Unable to determine artifact coordinates from: " + pom);
            return Extension.createNotDetected();
        }

        boolean commonOrInternal = false;

        if (artifactId.endsWith(ARTIFACT_DEPLOYMENT_SUFFIX)) {
            artifactId = artifactId.substring(0, artifactId.length() - ARTIFACT_DEPLOYMENT_SUFFIX.length());
        }
        if (artifactId.endsWith(ARTIFACT_COMMON_SUFFIX)) {
            artifactId = artifactId.substring(0, artifactId.length() - ARTIFACT_COMMON_SUFFIX.length());
            commonOrInternal = true;
        }
        if (artifactId.endsWith(ARTIFACT_INTERNAL_SUFFIX)) {
            artifactId = artifactId.substring(0, artifactId.length() - ARTIFACT_INTERNAL_SUFFIX.length());
            commonOrInternal = true;
        }

        NameSource nameSource;
        Optional<String> nameFromExtensionMetadata = getExtensionNameFromExtensionMetadata();
        if (nameFromExtensionMetadata.isPresent()) {
            name = nameFromExtensionMetadata.get();
            nameSource = commonOrInternal ? NameSource.EXTENSION_METADATA_COMMON_INTERNAL : NameSource.EXTENSION_METADATA;
        } else if (name != null) {
            nameSource = commonOrInternal ? NameSource.POM_XML_COMMON_INTERNAL : NameSource.POM_XML;
        } else {
            nameSource = NameSource.NONE;
        }

        if (name != null) {
            if (name.startsWith(NAME_QUARKUS_PREFIX)) {
                name = name.substring(NAME_QUARKUS_PREFIX.length()).trim();
            }
            if (name.endsWith(NAME_DEPLOYMENT_SUFFIX)) {
                name = name.substring(0, name.length() - NAME_DEPLOYMENT_SUFFIX.length());
            } else if (name.endsWith(NAME_RUNTIME_SUFFIX)) {
                name = name.substring(0, name.length() - NAME_RUNTIME_SUFFIX.length());
            } else if (name.endsWith(NAME_COMMON_SUFFIX)) {
                name = name.substring(0, name.length() - NAME_COMMON_SUFFIX.length());
            } else if (name.endsWith(NAME_INTERNAL_SUFFIX)) {
                name = name.substring(0, name.length() - NAME_INTERNAL_SUFFIX.length());
            }
        }

        return new Extension(groupId, artifactId, name, nameSource, true);
    }

    private Optional<String> getExtensionNameFromExtensionMetadata() {
        Optional<Map<String, Object>> extensionMetadata = filerUtil.getExtensionMetadata();

        if (extensionMetadata.isEmpty()) {
            return Optional.empty();
        }

        String extensionName = (String) extensionMetadata.get().get("name");
        if (extensionName == null || extensionName.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(extensionName.trim());
    }
}
