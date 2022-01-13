package io.quarkus.jdbc.postgresql.runtime;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.kubernetes.service.binding.runtime.ServiceBinding;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConfigSource;
import io.quarkus.kubernetes.service.binding.runtime.ServiceBindingConverter;

public class PostgreSqlServiceBindingConverter implements ServiceBindingConverter {
    public static final String QUARKUS_DATASOURCE_JDBC_URL = "quarkus.datasource.jdbc.url";
    public static final String BINDING_TYPE = "postgresql";
    private static final Logger log = Logger.getLogger(PostgreSqlServiceBindingConverter.class);
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

        Optional<Map<String, String>> properties = buildBindingProperties(matchingByType, BINDING_TYPE);
        if (!properties.isPresent()) {
            return Optional.empty();
        }
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
            properties.get().put(QUARKUS_DATASOURCE_JDBC_URL,
                    properties.get().get(QUARKUS_DATASOURCE_JDBC_URL) + "?" + combinedOptions);
        }

        return Optional.of(new ServiceBindingConfigSource(BINDING_TYPE + "-k8s-service-binding-source", properties.get()));
    }

    private String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
    }

    private Optional<Map<String, String>> buildBindingProperties(Optional<ServiceBinding> matchingByType, String urlType) {
        if (!matchingByType.isPresent()) {
            return Optional.empty();
        }

        Map<String, String> properties = new HashMap<>();
        ServiceBinding binding = matchingByType.get();

        String username = binding.getProperties().get("username");
        if (username != null) {
            properties.put("quarkus.datasource.username", username);
        } else {
            log.debug("Property 'username' was not found");
        }
        String password = binding.getProperties().get("password");
        if (password != null) {
            properties.put("quarkus.datasource.password", password);
        } else {
            log.debug("Property 'password' was not found");
        }
        String host = binding.getProperties().get("host");
        String port = binding.getProperties().get("port");
        String database = binding.getProperties().get("database");
        if ((host != null) && (database != null)) {
            String portPart = "";
            if (port != null) {
                portPart = ":" + port;
            }
            properties.put(QUARKUS_DATASOURCE_JDBC_URL, String.format("jdbc:%s://%s%s/%s", urlType, host, portPart, database));
        } else {
            log.debug("One or more of 'host' or 'database' properties were not found");
        }

        return Optional.of(properties);
    }
}
