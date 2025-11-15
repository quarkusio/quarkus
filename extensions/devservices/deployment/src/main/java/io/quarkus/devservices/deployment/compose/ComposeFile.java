package io.quarkus.devservices.deployment.compose;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Representation of a docker-compose file, with partial parsing for validation and extraction of a minimal set of
 * data.
 */
public class ComposeFile {

    private static final Logger log = Logger.getLogger(ComposeFile.class);

    private final Map<String, Object> composeFileContent;

    private final String composeFileName;

    private final Map<String, ComposeServiceDefinition> serviceNameToDefinition = new HashMap<>();

    public ComposeFile(File composeFile) {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> composeFileContentVal;
        try (FileInputStream fileInputStream = new FileInputStream(composeFile)) {
            composeFileContentVal = yaml.load(fileInputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse YAML file from " + composeFile.getAbsolutePath(), e);
        }
        this.composeFileName = composeFile.getAbsolutePath();
        if (composeFileContentVal == null) { // this happens when the file is empty
            log.warnv("File {0} is empty", composeFileName);
            composeFileContent = Collections.emptyMap();
        } else {
            composeFileContent = composeFileContentVal;
        }
        parseAndValidate();
    }

    public String getProjectName() {
        return (String) composeFileContent.get("name");
    }

    private void parseAndValidate() {
        final Map<String, ?> servicesMap;
        if (composeFileContent.containsKey("version")) {
            log.warn("The 'version' property is deprecated in docker-compose files. It will be ignored.");
        }

        final Object servicesElement = composeFileContent.get("services");
        if (servicesElement == null) {
            log.debugv("Compose file {0} has an unknown format: 'services' is not defined", composeFileName);
            servicesMap = composeFileContent;
        } else if (!(servicesElement instanceof Map)) {
            log.debugv("Compose file {0} has an unknown format: 'services' is not Map", composeFileName);
            return;
        } else {
            servicesMap = (Map<String, ?>) servicesElement;
        }

        for (Map.Entry<String, ?> entry : servicesMap.entrySet()) {
            String serviceName = entry.getKey();
            Object serviceDefinition = entry.getValue();
            if (!(serviceDefinition instanceof Map)) {
                log.debugv("Compose file {0} has an unknown format: service '{0}' is not Map, it will be ignored.",
                        composeFileName, serviceName);
                continue;
            }

            final Map<String, ?> serviceDefinitionMap = (Map) serviceDefinition;
            serviceNameToDefinition.put(serviceName, new ComposeServiceDefinition(serviceName, serviceDefinitionMap));
        }
    }

    public Map<String, ComposeServiceDefinition> getServiceDefinitions() {
        return serviceNameToDefinition;
    }

}
