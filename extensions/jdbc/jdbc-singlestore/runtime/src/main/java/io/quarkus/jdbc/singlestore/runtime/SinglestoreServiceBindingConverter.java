package io.quarkus.jdbc.singlestore.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.kubernetes.service.binding.runtime.DatasourceServiceBindingConfigSourceFactory;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter;

public class SinglestoreServiceBindingConverter implements ServiceBindingConverter {

    @Override
    public Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings) {
        return ServiceBinding.singleMatchingByType("mysql", serviceBindings)
                .map(new SinglestoreDatasourceServiceBindingConfigSourceFactory());
    }

    private static class SinglestoreDatasourceServiceBindingConfigSourceFactory
            extends DatasourceServiceBindingConfigSourceFactory.Jdbc {
        @Override
        protected String formatUrl(String urlFormat, String type, String host, String database, String portPart) {
            return super.formatUrl(urlFormat, "singlestore", host, database, portPart);
        }
    }
}
