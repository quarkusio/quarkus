package io.quarkus.reactive.pg.client.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.kubernetes.service.binding.runtime.DatasourceServiceBindingConfigSourceFactory;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter;

public class PostgreSQLServiceBindingConverter implements ServiceBindingConverter {

    @Override
    public Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings) {
        return ServiceBinding.singleMatchingByType("postgresql", serviceBindings)
                .map(new DatasourceServiceBindingConfigSourceFactory.Reactive());
    }
}
