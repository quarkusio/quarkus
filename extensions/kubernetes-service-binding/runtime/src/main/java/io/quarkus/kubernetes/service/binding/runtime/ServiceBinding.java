package io.quarkus.kubernetes.service.binding.runtime;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jboss.logging.Logger;

/**
 * Contains all the necessary information about a service binding
 */
public final class ServiceBinding {

    private static final Logger log = Logger.getLogger(ServiceBinding.class);

    private static final String PROVIDER = "provider";
    private static final String TYPE = "type";

    private final String name;
    private final String provider;
    private final Map<String, String> properties;
    private final String type;

    public ServiceBinding(Path bindingDirectory) {
        this(bindingDirectory.getFileName().toString(), getFilenameToContentMap(bindingDirectory), bindingDirectory);
    }

    // visible for testing
    ServiceBinding(String name, Map<String, String> filenameToContentMap, Path bindingDirectory) {
        Map<String, String> properties = new HashMap<>();
        String type = null;
        String provider = null;
        for (Map.Entry<String, String> entry : filenameToContentMap.entrySet()) {
            if (TYPE.equals(entry.getKey())) {
                type = entry.getValue();
            } else if (PROVIDER.equals(entry.getKey())) {
                provider = entry.getValue();
            } else {
                properties.put(entry.getKey(), entry.getValue());
            }
        }

        if (type == null) {
            throw new IllegalArgumentException("Directory '" + bindingDirectory
                    + "' does not represent a valid Service ServiceBinding directory as it does not specify a type");
        }

        this.name = name;
        this.type = type;
        this.provider = provider;
        this.properties = Collections.unmodifiableMap(properties);
    }

    private static Map<String, String> getFilenameToContentMap(Path directory) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            log.warn("File '" + directory + "' is not a proper service binding directory so it will skipped");
            return Collections.emptyMap();
        }

        File[] files = directory.toFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                try {
                    return !Files.isHidden(f.toPath()) && !Files.isDirectory(f.toPath());
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to determine if file '" + f + "' is a regular file", e);
                }
            }
        });

        Map<String, String> result = new HashMap<>();
        if (files != null) {
            for (File f : files) {
                try {
                    result.put(f.toPath().getFileName().toString(),
                            new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim());
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to read file '" + f + "'", e);
                }
            }
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getType() {
        return type;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public String toString() {
        return "ServiceBinding{" +
                "name='" + name + '\'' +
                ", provider='" + provider + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public static List<ServiceBinding> matchingByType(String type, List<ServiceBinding> all) {
        Objects.requireNonNull(type, "Type must not be null");
        List<ServiceBinding> result = new ArrayList<>();
        for (ServiceBinding binding : all) {
            if (type.equals(binding.getType())) {
                result.add(binding);
            }
        }
        return result;
    }

    public static Optional<ServiceBinding> singleMatchingByType(String type, List<ServiceBinding> all) {
        List<ServiceBinding> allMatching = matchingByType(type, all);
        if (allMatching.isEmpty()) {
            return Optional.empty();
        }
        ServiceBinding first = allMatching.get(0);
        if (allMatching.size() > 1) {
            log.warn("More than one ServiceBinding matches type '" + type + "', but only " + first + " will be used");
        }
        return Optional.of(first);
    }
}
