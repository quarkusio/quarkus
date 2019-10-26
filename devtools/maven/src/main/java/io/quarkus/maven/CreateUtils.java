package io.quarkus.maven;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.util.ZipUtils;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.QuarkusPlatformDescriptorLoaderContext;
import io.quarkus.platform.descriptor.loader.json.impl.QuarkusJsonPlatformDescriptorLoaderBootstrap;
import io.quarkus.platform.descriptor.loader.json.util.QuarkusJsonDescriptorUtils;
import io.quarkus.platform.tools.MessageWriter;
import io.quarkus.platform.tools.config.QuarkusPlatformConfig;
import io.quarkus.platform.tools.maven.MojoMessageWriter;

public final class CreateUtils {

    public static final String DEFAULT_PLATFORM_GROUP_ID = "io.quarkus";
    public static final String DEFAULT_PLATFORM_ARTIFACT_ID = "quarkus-bom-descriptor-json";

    private CreateUtils() {
        //Not to be constructed
    }

    public static String getDerivedPath(String className) {
        String[] resourceClassName = StringUtils.splitByCharacterTypeCamelCase(
                className.substring(className.lastIndexOf(".") + 1));
        return "/" + resourceClassName[0].toLowerCase();
    }

    public static void setupQuarkusJsonPlatformDescriptor(
            RepositorySystem repoSystem, RepositorySystemSession repoSession, List<RemoteRepository> repos,
            String platformGroupId, String platformArtifactId, String defaultPlatformVersion,
            final Log log)
            throws MojoExecutionException {

        final Artifact platformArtifact = new DefaultArtifact(platformGroupId, platformArtifactId, null, "json",
                defaultPlatformVersion);

        if (isOnClasspath(platformArtifact)) {
            loadDescriptorFromClasspath(log);
            return;
        }

        final MavenArtifactResolver mvn;
        try {
            mvn = MavenArtifactResolver.builder()
                    .setRepositorySystem(repoSystem)
                    .setRepositorySystemSession(repoSession)
                    .setRemoteRepositories(repos)
                    .build();
        } catch (AppModelResolverException e) {
            throw new MojoExecutionException("Failed to initialize artifact resolver", e);
        }

        doSetupQuarkusJsonPlatformDescriptor(mvn, platformArtifact, log);
    }

    public static void setupQuarkusJsonPlatformDescriptor(MavenArtifactResolver mvn, Artifact platformArtifact, final Log log)
            throws MojoExecutionException {

        if (isOnClasspath(platformArtifact)) {
            loadDescriptorFromClasspath(log);
            return;
        }

        doSetupQuarkusJsonPlatformDescriptor(mvn, platformArtifact, log);
    }

    private static void doSetupQuarkusJsonPlatformDescriptor(MavenArtifactResolver mvn, Artifact platformArtifact,
            final Log log)
            throws MojoExecutionException {
        if (platformArtifact.getVersion() == null || platformArtifact.getVersion().isEmpty()) {
            platformArtifact = platformArtifact.setVersion(resolvePluginInfo(CreateUtils.class).getVersion());
        }

        final QuarkusPlatformDescriptor platformDescr;
        try {
            platformDescr = QuarkusJsonDescriptorUtils.loadDescriptor(mvn, platformArtifact, new MojoMessageWriter(log));
        } catch (Throwable t) {
            throw new MojoExecutionException("Failed to resolve platform descriptor " + platformArtifact, t);
        }
        QuarkusPlatformConfig.defaultConfigBuilder()
                .setPlatformDescriptor(platformDescr)
                .build();
    }

    private static void loadDescriptorFromClasspath(Log log) {
        QuarkusPlatformConfig.defaultConfigBuilder()
                .setPlatformDescriptor(new QuarkusJsonPlatformDescriptorLoaderBootstrap()
                        .load(new QuarkusPlatformDescriptorLoaderContext() {
                            final MessageWriter msgWriter = new MojoMessageWriter(log);

                            @Override
                            public MessageWriter getMessageWriter() {
                                return msgWriter;
                            }
                        }))
                .build();
    }

    private static boolean isOnClasspath(Artifact platformArtifact) throws MojoExecutionException {
        return "quarkus-bom-descriptor-json".equals(platformArtifact.getArtifactId())
                && "io.quarkus".equals(platformArtifact.getGroupId())
                && (platformArtifact.getVersion() == null
                        || platformArtifact.getVersion().isEmpty()
                        || platformArtifact.getVersion()
                                .equals(CreateUtils.resolvePluginInfo(CreateUtils.class).getVersion()));
    }

    public static Plugin resolvePluginInfo(Class<?> cls) throws MojoExecutionException {
        try {
            final Path classOrigin = MojoUtils.getClassOrigin(cls);
            if (Files.isDirectory(classOrigin)) {
                return resolvePluginInfo(classOrigin);
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
