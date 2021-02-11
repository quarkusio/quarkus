package io.quarkus.kubernetes.service.binding.runtime;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

public class KubernetesServiceBindingConfigSourceProvider implements ConfigSourceProvider {

    private final List<ServiceBinding> serviceBindings;
    private final List<ServiceBindingConverter> serviceBindingConverters;

    public KubernetesServiceBindingConfigSourceProvider(String bindingRoot) {
        this(bindingRoot, determineConverters());
    }

    //visible for testing
    KubernetesServiceBindingConfigSourceProvider(String bindingRoot, List<ServiceBindingConverter> serviceBindingConverters) {
        this.serviceBindingConverters = serviceBindingConverters;
        Path p = Paths.get(bindingRoot);
        if (!Files.exists(p)) {
            serviceBindings = Collections.emptyList();
            return;
        }
        if (!Files.isDirectory(p)) {
            throw new IllegalArgumentException("Service Binding root '" + p + "' is not a directory");
        }

        File[] files = p.toFile().listFiles();
        if (files == null) {
            serviceBindings = Collections.emptyList();
        } else {
            serviceBindings = new ArrayList<>(files.length);
            for (File f : files) {
                serviceBindings.add(new ServiceBinding(f.toPath()));
            }
            serviceBindings.sort(new Comparator<ServiceBinding>() {
                @Override
                public int compare(ServiceBinding o1, ServiceBinding o2) {
                    if (!o1.getName().equals(o2.getName())) {
                        return o1.getName().compareTo(o2.getName());
                    }
                    return o1.getProvider().compareTo(o2.getProvider());
                }
            });
        }
    }

    private static List<ServiceBindingConverter> determineConverters() {
        List<ServiceBindingConverter> result = new ArrayList<>();
        ServiceLoader<ServiceBindingConverter> loader = ServiceLoader.load(ServiceBindingConverter.class,
                Thread.currentThread().getContextClassLoader());
        for (ServiceBindingConverter c : loader) {
            result.add(c);
        }
        return result;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader) {
        if (serviceBindings.isEmpty()) {
            return Collections.emptyList();
        }

        List<ConfigSource> result = new ArrayList<>();
        for (ServiceBindingConverter converter : serviceBindingConverters) {
            Optional<ServiceBindingConfigSource> optional = converter.convert(serviceBindings);
            if (optional.isPresent()) {
                result.add(optional.get());
            }
        }
        for (ServiceBinding serviceBinding : serviceBindings) {
            Map<String, String> serviceBindingProperties = serviceBinding.getProperties();
            Map<String, String> rawConfigSourceProperties = new HashMap<>();
            for (Map.Entry<String, String> entry : serviceBindingProperties.entrySet()) {
                rawConfigSourceProperties.put("quarkus." + serviceBinding.getName() + "." + entry.getKey(), entry.getValue());
            }
            result.add(new ServiceBindingConfigSource("service-binding-" + serviceBinding.getName() + "-raw",
                    rawConfigSourceProperties));
        }
        return result;
    }
}
