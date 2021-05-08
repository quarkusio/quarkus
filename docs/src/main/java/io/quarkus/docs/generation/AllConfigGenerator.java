package io.quarkus.docs.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;

import io.quarkus.annotation.processor.generate_doc.ConfigDocGeneratedOutput;
import io.quarkus.annotation.processor.generate_doc.ConfigDocItem;
import io.quarkus.annotation.processor.generate_doc.ConfigDocItemScanner;
import io.quarkus.annotation.processor.generate_doc.ConfigDocSection;
import io.quarkus.annotation.processor.generate_doc.ConfigDocWriter;
import io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;

public class AllConfigGenerator {

    public static Artifact toAetherArtifact(ArtifactCoords coords) {
        return new DefaultArtifact(coords.getGroupId(), coords.getArtifactId(), coords.getClassifier(), coords.getType(),
                coords.getVersion());
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            // exit 1 will break Maven
            throw new IllegalArgumentException("Usage: <extension-catalog.json>");
        }
        String jsonCatalog = args[0];

        // This is where we produce the entire list of extensions
        File jsonFile = new File(jsonCatalog);
        if (!jsonFile.exists()) {
            throw new RuntimeException(
                    "Could not generate all-config file because extension catalog file is missing: " + jsonFile);
        }
        MavenArtifactResolver resolver = MavenArtifactResolver.builder().setWorkspaceDiscovery(false).build();

        final JsonExtensionCatalog extensionJson = JsonCatalogMapperHelper.deserialize(jsonFile.toPath(),
                JsonExtensionCatalog.class);

        // now get all the listed extension jars via Maven
        List<ArtifactRequest> requests = new ArrayList<>(extensionJson.getExtensions().size());
        Map<String, Extension> extensionsByGav = new HashMap<>();
        Map<String, Extension> extensionsByConfigRoots = new HashMap<>();
        for (Extension extension : extensionJson.getExtensions()) {
            final Artifact artifact = toAetherArtifact(extension.getArtifact());
            requests.add(new ArtifactRequest().setArtifact(artifact));
            // record the extension for this GAV
            extensionsByGav.put(artifact.getGroupId() + ":" + artifact.getArtifactId(), extension);
        }

        // examine all the extension jars
        List<ArtifactRequest> deploymentRequests = new ArrayList<>(extensionJson.getExtensions().size());
        for (ArtifactResult result : resolver.resolve(requests)) {
            final Artifact artifact = result.getArtifact();
            // which extension was this for?
            Extension extension = extensionsByGav.get(artifact.getGroupId() + ":" + artifact.getArtifactId());
            try (ZipFile zf = new ZipFile(artifact.getFile())) {
                // collect all its config roots
                collectConfigRoots(zf, extension, extensionsByConfigRoots);

                // see if it has a deployment artifact we need to load
                ZipEntry entry = zf.getEntry("META-INF/quarkus-extension.properties");
                if (entry != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(zf.getInputStream(entry), StandardCharsets.UTF_8))) {
                        Properties properties = new Properties();
                        properties.load(reader);
                        final String deploymentCoords = (String) properties.get("deployment-artifact");
                        // if it has one, load it
                        if (deploymentCoords != null) {
                            final Artifact deploymentArtifact = toAetherArtifact(ArtifactCoords.fromString(deploymentCoords));
                            deploymentRequests.add(new ArtifactRequest().setArtifact(deploymentArtifact));
                            // tie this artifact to its extension
                            extensionsByGav.put(deploymentArtifact.getGroupId() + ":" + deploymentArtifact.getArtifactId(),
                                    extension);
                        }
                    }
                }
            }
        }

        // now examine all the extension deployment jars
        for (ArtifactResult result : resolver.resolve(deploymentRequests)) {
            final Artifact artifact = result.getArtifact();
            // which extension was this for?
            Extension extension = extensionsByGav.get(artifact.getGroupId() + ":" + artifact.getArtifactId());
            try (ZipFile zf = new ZipFile(artifact.getFile())) {
                // collect all its config roots
                collectConfigRoots(zf, extension, extensionsByConfigRoots);
            }
        }

        // load all the config items per config root
        final ConfigDocItemScanner configDocItemScanner = new ConfigDocItemScanner();
        final Map<String, List<ConfigDocItem>> docItemsByConfigRoots = configDocItemScanner
                .loadAllExtensionsConfigurationItems();
        final Map<String, String> artifactIdsByName = new HashMap<>();
        ConfigDocWriter configDocWriter = new ConfigDocWriter();

        // Temporary fix for https://github.com/quarkusio/quarkus/issues/5214 until we figure out how to fix it
        Extension openApi = extensionsByGav.get("io.quarkus:quarkus-smallrye-openapi");
        if (openApi != null) {
            extensionsByConfigRoots.put("io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig", openApi);
        }

        // build a list of sorted config items by extension
        final SortedMap<String, List<ConfigDocItem>> sortedConfigItemsByExtension = new TreeMap<>();

        // sort extensions by name, assign their config items based on their config roots
        for (Entry<String, Extension> entry : extensionsByConfigRoots.entrySet()) {
            List<ConfigDocItem> items = docItemsByConfigRoots.get(entry.getKey());
            if (items != null) {
                String extensionName = entry.getValue().getName();
                if (extensionName == null) {
                    // compute the docs file name for this extension
                    String docFileName = DocGeneratorUtil.computeExtensionDocFileName(entry.getKey());
                    // now approximate an extension file name based on it
                    extensionName = guessExtensionNameFromDocumentationFileName(docFileName);
                    System.err.println("WARNING: Extension name missing for "
                            + (entry.getValue().getArtifact().getGroupId() + ":"
                                    + entry.getValue().getArtifact().getArtifactId())
                            + " using guessed extension name: " + extensionName);
                }
                artifactIdsByName.put(extensionName, entry.getValue().getArtifact().getArtifactId());
                List<ConfigDocItem> existingConfigDocItems = sortedConfigItemsByExtension.get(extensionName);
                if (existingConfigDocItems != null) {
                    DocGeneratorUtil.appendConfigItemsIntoExistingOnes(existingConfigDocItems, items);
                } else {
                    ArrayList<ConfigDocItem> configItems = new ArrayList<>();
                    sortedConfigItemsByExtension.put(extensionName, configItems);
                    configItems.addAll(items);
                }
            }
        }

        // now we have all the config items sorted by extension, let's build the entire list
        final List<ConfigDocItem> allItems = new ArrayList<>();
        for (Map.Entry<String, List<ConfigDocItem>> entry : sortedConfigItemsByExtension.entrySet()) {
            final List<ConfigDocItem> configDocItems = entry.getValue();
            // sort the items
            DocGeneratorUtil.sort(configDocItems);
            // insert a header
            ConfigDocSection header = new ConfigDocSection();
            header.setShowSection(true);
            header.setSectionDetailsTitle(entry.getKey());
            header.setAnchorPrefix(artifactIdsByName.get(entry.getKey()));
            header.setName(artifactIdsByName.get(entry.getKey()));
            allItems.add(new ConfigDocItem(header, null));
            // add all the configs for this extension
            allItems.addAll(configDocItems);
        }

        // write our docs
        ConfigDocGeneratedOutput allConfigGeneratedOutput = new ConfigDocGeneratedOutput("quarkus-all-config.adoc", true,
                allItems,
                false);
        configDocWriter.writeAllExtensionConfigDocumentation(allConfigGeneratedOutput);
    }

    private static String guessExtensionNameFromDocumentationFileName(String docFileName) {
        // sanitise
        if (docFileName.startsWith("quarkus-")) {
            docFileName = docFileName.substring(8);
        }

        if (docFileName.endsWith(".adoc")) {
            docFileName = docFileName.substring(0, docFileName.length() - 5);
        }

        if (docFileName.endsWith("-config")) {
            docFileName = docFileName.substring(0, docFileName.length() - 7);
        }

        if (docFileName.endsWith("-configuration")) {
            docFileName = docFileName.substring(0, docFileName.length() - 14);
        }

        docFileName = docFileName.replace('-', ' ');
        return capitalize(docFileName);
    }

    private static String capitalize(String title) {
        char[] chars = title.toCharArray();
        boolean capitalize = true;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isSpaceChar(c)) {
                capitalize = true;
                continue;
            }
            if (capitalize) {
                if (Character.isLetter(c))
                    chars[i] = Character.toUpperCase(c);
                capitalize = false;
            }
        }
        return new String(chars);
    }

    private static void collectConfigRoots(ZipFile zf, Extension extension, Map<String, Extension> extensionsByConfigRoots)
            throws IOException {
        ZipEntry entry = zf.getEntry("META-INF/quarkus-config-roots.list");
        if (entry != null) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(zf.getInputStream(entry), StandardCharsets.UTF_8))) {
                // make sure we turn $ into . because javadoc-scanned class names are dot-separated
                reader.lines().map(String::trim).filter(str -> !str.isEmpty()).map(str -> str.replace('$', '.'))
                        .forEach(klass -> extensionsByConfigRoots.put(klass, extension));
            }
        }
    }
}
