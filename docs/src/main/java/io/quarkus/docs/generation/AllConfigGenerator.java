package io.quarkus.docs.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.annotation.processor.generate_doc.ConfigDocItem;
import io.quarkus.annotation.processor.generate_doc.ConfigDocItemScanner;
import io.quarkus.annotation.processor.generate_doc.ConfigDocSection;
import io.quarkus.annotation.processor.generate_doc.ConfigDocWriter;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.docs.generation.ExtensionJson.Extension;

public class AllConfigGenerator {
    public static void main(String[] args)
            throws AppModelResolverException, JsonParseException, JsonMappingException, IOException {
        MavenArtifactResolver resolver = MavenArtifactResolver.builder().build();
        // This is where we produce the entire list of extensions
        String jsonPath = "devtools/core-extensions-json/target/extensions.json";
        ObjectMapper mapper = new ObjectMapper();

        // let's read it (and ignore the fields we don't need)
        ExtensionJson extensionJson = mapper.readValue(new File(jsonPath), ExtensionJson.class);

        // now get all the listed extension jars via Maven
        List<ArtifactRequest> requests = new ArrayList<>(extensionJson.extensions.size());
        Map<String, Extension> extensionsByGav = new HashMap<>();
        Map<String, Extension> extensionsByConfigRoots = new HashMap<>();
        for (Extension extension : extensionJson.extensions) {
            ArtifactRequest request = new ArtifactRequest();
            Artifact artifact = new DefaultArtifact(extension.groupId, extension.artifactId, "jar", "999-SNAPSHOT");
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
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)))) {
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
        ConfigDocWriter configDocWriter = new ConfigDocWriter();

        // build a list of sorted config items by extension
        List<ConfigDocItem> allItems = new ArrayList<>();
        SortedMap<String, List<ConfigDocItem>> sortedConfigItemsByExtension = new TreeMap<>();

        // sort extensions by name, assign their config items based on their config roots
        for (Entry<String, Extension> entry : extensionsByConfigRoots.entrySet()) {
            List<ConfigDocItem> items = docItemsByConfigRoots.get(entry.getKey());
            if (items != null) {
                List<ConfigDocItem> configItems = sortedConfigItemsByExtension.computeIfAbsent(entry.getValue().getConfigName(),
                        k -> new ArrayList<>());
                configItems.addAll(items);
            }
        }

        // now we have all the config items sorted by extension, let's build the entire list
        for (Map.Entry<String, List<ConfigDocItem>> entry : sortedConfigItemsByExtension.entrySet()) {
            final List<ConfigDocItem> configDocItems = entry.getValue();
            // sort the items
            ConfigDocWriter.sort(configDocItems);
            // insert a header
            ConfigDocSection header = new ConfigDocSection();
            header.setSectionDetailsTitle(entry.getKey());
            allItems.add(new ConfigDocItem(header, null));
            // add all the configs for this extension
            allItems.addAll(configDocItems);
        }

        // write our docs
        configDocWriter.writeAllExtensionConfigDocumentation(allItems);
    }

    private static void collectConfigRoots(ZipFile zf, Extension extension, Map<String, Extension> extensionsByConfigRoots)
            throws IOException {
        ZipEntry entry = zf.getEntry("META-INF/quarkus-config-roots.list");
        if (entry != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(zf.getInputStream(entry)))) {
                reader.lines().map(String::trim).filter(str -> !str.isEmpty())
                        .forEach(klass -> extensionsByConfigRoots.put(klass, extension));
            }
        }
    }
}
