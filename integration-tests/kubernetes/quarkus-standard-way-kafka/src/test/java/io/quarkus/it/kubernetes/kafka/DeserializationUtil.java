package io.quarkus.it.kubernetes.kafka;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.fabric8.kubernetes.api.model.HasMetadata;

// TODO: as this is a copy of the class in the 'quarkus-integration-test-kubernetes-standard',
//  maybe we should create a new kubernetes test module to contain this class?
final class DeserializationUtil {

    private static final String DOCUMENT_DELIMITER = "---";
    static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    private DeserializationUtil() {
    }

    /**
     * Takes a YAML file as input and output a list of Kubernetes resources
     * present in the file
     * The list is sorted alphabetically based on the resource Kind
     */
    public static List<HasMetadata> deserializeAsList(Path yamlFilePath) throws IOException {
        String[] parts = splitDocument(Files.readAllLines(yamlFilePath, StandardCharsets.UTF_8).toArray(new String[0]));
        List<HasMetadata> items = new ArrayList<>();
        for (String part : parts) {
            if (part.trim().isEmpty()) {
                continue;
            }
            items.add(MAPPER.readValue(part, HasMetadata.class));
        }
        items.sort(Comparator.comparing(HasMetadata::getKind));
        return items;
    }

    static String[] splitDocument(String[] lines) {
        List<String> documents = new ArrayList<>();
        int nLine = 0;
        StringBuilder builder = new StringBuilder();

        while (nLine < lines.length) {
            if (lines[nLine].length() < DOCUMENT_DELIMITER.length()
                    || !lines[nLine].substring(0, DOCUMENT_DELIMITER.length()).equals(DOCUMENT_DELIMITER)) {
                builder.append(lines[nLine]).append(System.lineSeparator());
            } else {
                documents.add(builder.toString());
                builder.setLength(0);
            }

            nLine++;
        }

        if (!builder.toString().isEmpty())
            documents.add(builder.toString());
        return documents.toArray(new String[0]);
    }
}
