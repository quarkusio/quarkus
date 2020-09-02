package io.quarkus.maven;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import io.quarkus.platform.tools.maven.MojoMessageWriter;

public final class CreateUtils {

    public static final String DEFAULT_PLATFORM_BOM_GROUP_ID = "io.quarkus";
    public static final String QUARKUS_CORE_BOM_ARTIFACT_ID = "quarkus-bom";
    public static final String DEFAULT_PLATFORM_BOM_ARTIFACT_ID = QUARKUS_CORE_BOM_ARTIFACT_ID;

    public static final String GRADLE_WRAPPER_PATH = "gradle-wrapper";
    public static final String[] GRADLE_WRAPPER_FILES = new String[] {
            "gradlew",
            "gradlew.bat",
            "gradle/wrapper/gradle-wrapper.properties",
            "gradle/wrapper/gradle-wrapper.jar"
    };

    private CreateUtils() {
        //Not to be constructed
    }

    private static boolean isVersionRange(String versionStr) {
        if (versionStr == null || versionStr.isEmpty()) {
            return false;
        }
        char c = versionStr.charAt(0);
        if (c == '[' || c == '(') {
            return true;
        }
        c = versionStr.charAt(versionStr.length() - 1);
        if (c == ']' || c == ')') {
            return true;
        }
        return versionStr.indexOf(',') >= 0;
    }

    static QuarkusPlatformDescriptor setGlobalPlatformDescriptor(final String bomGroupId, final String bomArtifactId,
            final String bomVersion,
            MavenArtifactResolver mvn, Log log) throws MojoExecutionException {
        final QuarkusPlatformDescriptor platform = resolvePlatformDescriptor(bomGroupId, bomArtifactId, bomVersion, mvn, log);
        QuarkusPlatformConfig.defaultConfigBuilder().setPlatformDescriptor(platform).build();
        return platform;
    }

    static QuarkusPlatformDescriptor resolvePlatformDescriptor(final String bomGroupId, final String bomArtifactId,
            final String bomVersion, MavenArtifactResolver mvn, Log log) throws MojoExecutionException {
        final QuarkusJsonPlatformDescriptorResolver platformResolver = QuarkusJsonPlatformDescriptorResolver.newInstance()
                .setMessageWriter(new MojoMessageWriter(log))
                .setArtifactResolver(new BootstrapAppModelResolver(mvn));

        String groupId = StringUtils.defaultIfBlank(bomGroupId, null);
        String artifactId = StringUtils.defaultIfBlank(bomArtifactId, null);
        String version = StringUtils.defaultIfBlank(bomVersion, null);

        if (CreateUtils.QUARKUS_CORE_BOM_ARTIFACT_ID.equals(artifactId)
                && version == null) {
            version = resolvePluginInfo(CreateUtils.class).getVersion();
        }

        final QuarkusPlatformDescriptor platform;
        if (version == null) {
            if (artifactId == null && groupId == null) {
                platform = platformResolver.resolve();
            } else {
                platform = platformResolver.resolveLatestFromBom(groupId, artifactId, null);
            }
        } else if (isVersionRange(version)) {
            platform = platformResolver.resolveLatestFromBom(groupId, artifactId, version);
        } else {
            platform = platformResolver.resolveFromBom(groupId, artifactId, version);
        }
        return platform;
    }

    public static String getDerivedPath(String className) {
        String[] resourceClassName = StringUtils.splitByCharacterTypeCamelCase(
                className.substring(className.lastIndexOf(".") + 1));
        return "/" + resourceClassName[0].toLowerCase();
    }

    public static Plugin resolvePluginInfo(Class<?> cls) throws MojoExecutionException {
        try {
            final Path classOrigin = MojoUtils.getClassOrigin(cls);
            if (Files.isDirectory(classOrigin)) {
                return resolvePluginInfo(classOrigin.resolve("META-INF").resolve("maven").resolve("plugin.xml"));
            }
            try (FileSystem fs = ZipUtils.newFileSystem(classOrigin)) {
                return resolvePluginInfo(fs.getPath("META-INF", "maven", "plugin.xml"));
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to resolve maven plugin version containing " + cls, e);
        }
    }

    private static Plugin resolvePluginInfo(Path pluginXml) throws MojoExecutionException {
        if (!Files.exists(pluginXml)) {
            throw new MojoExecutionException("Failed to locate " + pluginXml);
        }
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            final DocumentBuilder db = dbf.newDocumentBuilder();
            try (InputStream is = Files.newInputStream(pluginXml)) {
                final Document doc = db.parse(is);
                final Node pluginNode = getElement(doc.getChildNodes(), "plugin");
                final Plugin plugin = new Plugin();
                plugin.setGroupId(getChildElementTextValue(pluginNode, "groupId"));
                plugin.setArtifactId(getChildElementTextValue(pluginNode, "artifactId"));
                plugin.setVersion(getChildElementTextValue(pluginNode, "version"));
                return plugin;
            }
        } catch (Throwable t) {
            throw new MojoExecutionException("Failed to parse " + pluginXml, t);
        }
    }

    private static String getChildElementTextValue(final Node parentNode, String childName) throws MojoExecutionException {
        final Node node = getElement(parentNode.getChildNodes(), childName);
        final String text = getText(node);
        if (text.isEmpty()) {
            throw new MojoExecutionException(
                    "The " + parentNode.getNodeName() + " element description is missing child " + childName);
        }
        return text;
    }

    private static Node getElement(NodeList nodeList, String name) throws MojoExecutionException {
        for (int i = 0; i < nodeList.getLength(); ++i) {
            final Node item = nodeList.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE
                    && (name.equals(item.getNodeName()) || name.equals(item.getLocalName()))) {
                return item;
            }
        }
        throw new MojoExecutionException("Failed to locate element " + name);
    }

    private static String getText(Node node) {
        if (!node.hasChildNodes()) {
            return "";
        }
        StringBuffer result = new StringBuffer();
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node subnode = list.item(i);
            if (subnode.getNodeType() == Node.TEXT_NODE) {
                result.append(subnode.getNodeValue());
            } else if (subnode.getNodeType() == Node.CDATA_SECTION_NODE) {
                result.append(subnode.getNodeValue());
            } else if (subnode.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
                // Recurse into the subtree for text
                // (and ignore comments)
                result.append(getText(subnode));
            }
        }
        return result.toString();
    }
}
