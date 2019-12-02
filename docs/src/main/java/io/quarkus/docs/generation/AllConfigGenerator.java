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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import io.quarkus.annotation.processor.generate_doc.ConfigDocGeneratedOutput;
import io.quarkus.annotation.processor.generate_doc.ConfigDocItem;
import io.quarkus.annotation.processor.generate_doc.ConfigDocItemScanner;
import io.quarkus.annotation.processor.generate_doc.ConfigDocSection;
import io.quarkus.annotation.processor.generate_doc.ConfigDocWriter;
import io.quarkus.annotation.processor.generate_doc.DocGeneratorUtil;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.docs.generation.ExtensionJson.Extension;

public class AllConfigGenerator {
    public static void main(String[] args) throws AppModelResolverException, IOException {
        if (args.length != 2) {
            // exit 1 will break Maven
            throw new IllegalArgumentException("Usage: <version> <extension.json>");
        }
        String version = args[0];
        String extensionFile = args[1];

        // This is where we produce the entire list of extensions
        File jsonFile = new File(extensionFile);
        if (!jsonFile.exists()) {
            System.err.println("WARNING: could not generate all-config file because extensions list is missing: " + jsonFile);
            // exit 0 will break Maven
            return;
        }
        ObjectMapper mapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS)
                .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        MavenArtifactResolver resolver = MavenArtifactResolver.builder().build();

        // let's read it (and ignore the fields we don't need)
        ExtensionJson extensionJson = mapper.readValue(jsonFile, ExtensionJson.class);

        // now get all the listed extension jars via Maven
        List<ArtifactRequest> requests = new ArrayList<>(extensionJson.extensions.size());
        Map<String, Extension> extensionsByGav = new HashMap<>();
        Map<String, Extension> extensionsByConfigRoots = new HashMap<>();
        for (Extension extension : extensionJson.extensions) {
            ArtifactRequest request = new ArtifactRequest();
            Artifact artifact = new DefaultArtifact(extension.groupId, extension.artifactId, "jar", version);
            request.setArtifact(artifact);
            requests.add(request);
            // record the extension for this GAV
            extensionsByGav.put(extension.groupId + ":" + extension.artifactId, extension);
        }

        // examine all the extension jars 
        List<ArtifactRequest> deploymentRequests = new ArrayList<>(extensionJson.extensions.size());
        for (ArtifactResult result : resolver.resolve(requests)) {
            Artifact artifact = result.getArtifact();
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
                        String deploymentGav = (String) properties.get("deployment-artifact");
                        // if it has one, load it
                        if (deploymentGav != null) {
                            ArtifactRequest request = new ArtifactRequest();
                            Artifact deploymentArtifact = new DefaultArtifact(deploymentGav);
                            request.setArtifact(deploymentArtifact);
                            deploymentRequests.add(request);
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
            Artifact artifact = result.getArtifact();
            // which extension was this for?
            Extension extension = extensionsByGav.get(artifact.getGroupId() + ":" + artifact.getArtifactId());
            try (ZipFile zf = new ZipFile(artifact.getFile())) {
                // collect all its config roots
                collectConfigRoots(zf, extension, extensionsByConfigRoots);
            }
        }

        // load all the config items per config root
        ConfigDocItemScanner configDocItemScanner = new ConfigDocItemScanner();
        Map<String, List<ConfigDocItem>> docItemsByConfigRoots = configDocItemScanner
                .loadAllExtensionsConfigurationItems();
        Map<String, String> artifactIdsByName = new HashMap<>();
        ConfigDocWriter configDocWriter = new ConfigDocWriter();

        // build a list of sorted config items by extension
        List<ConfigDocItem> allItems = new ArrayList<>();
        SortedMap<String, List<ConfigDocItem>> sortedConfigItemsByExtension = new TreeMap<>();

        // Temporary fix for https://github.com/quarkusio/quarkus/issues/5214 until we figure out how to fix it
        Extension openApi = extensionsByGav.get("io.quarkus:quarkus-smallrye-openapi");
        if (openApi != null)
            extensionsByConfigRoots.put("io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig", openApi);

        // sort extensions by name, assign their config items based on their config roots
        for (Entry<String, Extension> entry : extensionsByConfigRoots.entrySet()) {
            List<ConfigDocItem> items = docItemsByConfigRoots.get(entry.getKey());
            if (items != null) {
                String extensionName = entry.getValue().name;
                if (extensionName == null) {
                    String extensionGav = entry.getValue().groupId + ":" + entry.getValue().artifactId;
                    // compute the docs file name for this extension
                    String docFileName = DocGeneratorUtil.computeExtensionDocFileName(entry.getKey());
                    // now approximate an extension file name based on it
                    extensionName = guessExtensionNameFromDocumentationFileName(docFileName);
                    System.err.println("WARNING: Extension name missing for " + extensionGav + " using guessed extension name: "
                            + extensionName);
                }
                artifactIdsByName.put(extensionName, entry.getValue().artifactId);
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
        for (Map.Entry<String, List<ConfigDocItem>> entry : sortedConfigItemsByExtension.entrySet()) {
            final List<ConfigDocItem> configDocItems = entry.getValue();
            // sort the items
            DocGeneratorUtil.sort(configDocItems);
            // insert a header
            ConfigDocSection header = new ConfigDocSection();
            header.setSectionDetailsTitle(entry.getKey());
            header.setAnchorPrefix(artifactIdsByName.get(entry.getKey()));
            header.setName(artifactIdsByName.get(entry.getKey()));
            allItems.add(new ConfigDocItem(header, null));
            // add all the configs for this extension
            allItems.addAll(configDocItems);
        }

        // write our docs
        ConfigDocGeneratedOutput allConfigGeneratedOutput = new ConfigDocGeneratedOutput("all-config.adoc", true, allItems,
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
