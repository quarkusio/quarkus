package io.quarkus.jdbc.postgresql.runtime;

import static io.quarkus.kubernetes.service.binding.runtime.JdbcDatasourceUtil.QUARKUS_DATASOURCE_JDBC_URL;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.kubernetes.service.binding.runtime.JdbcDatasourceUtil;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter;

public class PostgreSQLServiceBindingConverter implements ServiceBindingConverter {

    public static final String BINDING_TYPE = "postgresql";
    public static final String SSL_MODE = "sslmode";
    public static final String SSL_ROOT_CERT = "sslrootcert";
    public static final String OPTIONS = "options";

    @Override
    public Optional<ServiceBindingConfigSource> convert(List<ServiceBinding> serviceBindings) {

        Optional<ServiceBinding> matchingByType = ServiceBinding.singleMatchingByType(BINDING_TYPE, serviceBindings);
        if (!matchingByType.isPresent()) {
            return Optional.empty();
        }
        ServiceBinding binding = matchingByType.get();

        Map<String, String> properties = JdbcDatasourceUtil.getServiceBindingProperties(binding, BINDING_TYPE);
        //process ssl params
        //https://www.postgresql.org/docs/14/libpq-connect.html
        StringBuilder sslparam = new StringBuilder();
        String sslmode = binding.getProperties().getOrDefault(SSL_MODE, "");
        String sslRootCert = binding.getProperties().getOrDefault(SSL_ROOT_CERT, "");
        if (!"".equals(sslmode)) {
            sslparam.append(SSL_MODE).append("=").append(sslmode);
        }
        if (!"".equals(sslRootCert)) {
            if (!"".equals(sslmode)) {
                sslparam.append("&");
            }
            sslparam.append(SSL_ROOT_CERT).append("=")
                    .append(binding.getBindingDirectory()).append(FileSystems.getDefault().getSeparator())
                    .append(sslRootCert);
        }

        //cockroachdb cloud uses options parameter to pass in the cluster routing-id
        //https://www.cockroachlabs.com/docs/v21.2/connection-parameters#additional-connection-parameters
        String options = binding.getProperties().getOrDefault(OPTIONS, "");
        String crdbOption = "";
        List<String> postgreOptions = new ArrayList<>();
        if (!options.equals("")) {
            String[] allOpts = options.split("&");
            for (String o : allOpts) {
                String[] keyval = o.split("=");
                if (keyval.length != 2 || keyval[0].length() == 0 || keyval[1].length() == 0) {
                    continue;
                }
                if (keyval[0].equals("--cluster")) {
                    crdbOption = keyval[0] + "=" + keyval[1];
                } else {
                    postgreOptions.add("-c " + keyval[0] + "=" + keyval[1]);
                }
            }
        }

        String combinedOptions = crdbOption;
        if (postgreOptions.size() > 0) {
            String otherOpts = String.join(" ", postgreOptions);
            if (!combinedOptions.equals("")) {
                combinedOptions = combinedOptions + " " + otherOpts;
            } else {
                combinedOptions = otherOpts;
            }
        }

        try {
            combinedOptions = combinedOptions.length() > 0 ? OPTIONS + "=" + encode(combinedOptions).replace("+", "%20") : "";
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("failed to encode options params" + options, e);
        }

        if (sslparam.length() > 0 && !combinedOptions.equals("")) {
            combinedOptions = sslparam + "&" + combinedOptions;
        } else if (sslparam.length() > 0) {
            combinedOptions = sslparam.toString();
        }

        if (!"".equals(combinedOptions)) {
            //append sslmode and options to the URL
            properties.put(QUARKUS_DATASOURCE_JDBC_URL,
                    properties.get(QUARKUS_DATASOURCE_JDBC_URL) + "?" + combinedOptions);
        }

        return Optional.of(new ServiceBindingConfigSource(BINDING_TYPE + "-k8s-service-binding-source", properties));
    }

    private String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
    }
}
