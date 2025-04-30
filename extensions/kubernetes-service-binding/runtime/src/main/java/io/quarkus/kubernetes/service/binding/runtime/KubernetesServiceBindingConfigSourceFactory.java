package io.quarkus.kubernetes.service.binding.runtime;

import static java.util.Collections.emptyList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.logging.Logger;

import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigSourceFactory;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class KubernetesServiceBindingConfigSourceFactory implements ConfigSourceFactory {

    private static final Logger log = Logger.getLogger(KubernetesServiceBindingConfigSourceFactory.class);

    @Override
    public Iterable<ConfigSource> getConfigSources(final ConfigSourceContext context) {
        SmallRyeConfig config = new SmallRyeConfigBuilder()
                .withSources(new ConfigSourceContext.ConfigSourceContextConfigSource(context))
                .withMapping(KubernetesServiceBindingConfig.class)
                .withMappingIgnore("quarkus.**")
                .build();

        KubernetesServiceBindingConfig kubernetesServiceBindingConfig = config
                .getConfigMapping(KubernetesServiceBindingConfig.class);

        if (!kubernetesServiceBindingConfig.enabled()) {
            log.debug(
                    "No attempt will be made to bind configuration based on Kubernetes ServiceBinding because the feature was not enabled.");
            return emptyList();
        }
        if (kubernetesServiceBindingConfig.root().isEmpty()) {
            log.debug(
                    "No attempt will be made to bind configuration based on Kubernetes Service Binding because the binding root was not specified.");
            return emptyList();
        }

        List<ServiceBinding> serviceBindings = getServiceBindings(kubernetesServiceBindingConfig.root().get());
        if (serviceBindings.isEmpty()) {
            return emptyList();
        }

        return getConfigSources(serviceBindings, determineConverters());
    }

    Iterable<ConfigSource> getConfigSources(List<ServiceBinding> serviceBindings,
            List<ServiceBindingConverter> serviceBindingConverters) {
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
                rawConfigSourceProperties.put("quarkus.service-binding." + serviceBinding.getName() + "." + entry.getKey(),
                        entry.getValue());
            }
            result.add(new ServiceBindingConfigSource("service-binding-" + serviceBinding.getName() + "-raw",
                    rawConfigSourceProperties));
        }
        return result;
    }

    static List<ServiceBinding> getServiceBindings(String bindingRoot) {
        Path p = Paths.get(bindingRoot);
        if (!Files.exists(p)) {
            log.warn("Service Binding root '" + p.toAbsolutePath() + "' does not exist");
            return emptyList();
        }
        if (!Files.isDirectory(p)) {
            throw new IllegalArgumentException("Service Binding root '" + p + "' is not a directory");
        }

        File[] files = p.toFile().listFiles();
        if (files == null) {
            log.warn("Service Binding root '" + p.toAbsolutePath() + "' does not contain any sub-directories");
            return emptyList();
        } else {
            log.debug("Found " + files.length + " potential Service Binding directories");
            List<ServiceBinding> serviceBindings = new ArrayList<>(files.length);
            for (File f : files) {
                ServiceBinding sb = new ServiceBinding(f.toPath());
                serviceBindings.add(sb);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Directory '%s' contains %d %s and will be used as Service Binding %s",
                            f.toPath().toAbsolutePath(), sb.getProperties().size(),
                            sb.getProperties().size() == 1 ? "property" : "properties", sb));
                }
            }
            serviceBindings.sort(new Comparator<>() {
                @Override
                public int compare(ServiceBinding o1, ServiceBinding o2) {
                    if (!o1.getName().equals(o2.getName())) {
                        return o1.getName().compareTo(o2.getName());
                    }
                    return o1.getProvider().compareTo(o2.getProvider());
                }
            });
            return serviceBindings;
        }
    }

    static List<ServiceBindingConverter> determineConverters() {
        List<ServiceBindingConverter> result = new ArrayList<>();
        ServiceLoader<ServiceBindingConverter> loader = ServiceLoader.load(ServiceBindingConverter.class,
                Thread.currentThread().getContextClassLoader());
        for (ServiceBindingConverter c : loader) {
            result.add(c);
        }
        return result;
    }
}
