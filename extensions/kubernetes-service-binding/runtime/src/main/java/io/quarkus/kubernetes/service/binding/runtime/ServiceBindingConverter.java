package io.quarkus.kubernetes.service.binding.runtime;

import java.util.List;
import java.util.Optional;

public interface ServiceBindingConverter {

    Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings);
}
