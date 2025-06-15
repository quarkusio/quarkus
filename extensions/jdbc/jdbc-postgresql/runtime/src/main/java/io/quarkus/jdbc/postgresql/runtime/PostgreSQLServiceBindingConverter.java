package io.quarkus.jdbc.postgresql.runtime;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.kubernetes.service.binding.runtime.DatasourceServiceBindingConfigSourceFactory;
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
        return ServiceBinding.singleMatchingByType(BINDING_TYPE, serviceBindings)
                .map(new PostgreSQLDatasourceServiceBindingConfigSourceFactory());
    }

    private static class PostgreSQLDatasourceServiceBindingConfigSourceFactory
            extends DatasourceServiceBindingConfigSourceFactory.Jdbc {

        @Override
        protected String formatUrl(String urlFormat, String type, String host, String database, String portPart) {
            String result = super.formatUrl(urlFormat, type, host, database, portPart);

            Map<String, String> sbProps = serviceBinding.getProperties();

            // process ssl params
            // https://www.postgresql.org/docs/14/libpq-connect.html
            StringBuilder sslparam = new StringBuilder();
            String sslmode = sbProps.getOrDefault(SSL_MODE, "");
            String sslRootCert = sbProps.getOrDefault(SSL_ROOT_CERT, "");
            if (!"".equals(sslmode)) {
                sslparam.append(SSL_MODE).append("=").append(sslmode);
            }
            if (!"".equals(sslRootCert)) {
                if (!"".equals(sslmode)) {
                    sslparam.append("&");
                }
                sslparam.append(SSL_ROOT_CERT).append("=").append(serviceBinding.getBindingDirectory())
                        .append(FileSystems.getDefault().getSeparator()).append(sslRootCert);
            }

            // cockroachdb cloud uses options parameter to pass in the cluster routing-id
            // https://www.cockroachlabs.com/docs/v21.2/connection-parameters#additional-connection-parameters
            String options = sbProps.getOrDefault(OPTIONS, "");
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
                combinedOptions = combinedOptions.length() > 0
                        ? OPTIONS + "=" + encode(combinedOptions).replace("+", "%20")
                        : "";
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("failed to encode options params" + options, e);
            }

            if (sslparam.length() > 0 && !combinedOptions.equals("")) {
                combinedOptions = sslparam + "&" + combinedOptions;
            } else if (sslparam.length() > 0) {
                combinedOptions = sslparam.toString();
            }

            if (!"".equals(combinedOptions)) {
                // append sslmode and options to the URL
                result += "?" + combinedOptions;
            }

            return result;
        }

        private String encode(String str) throws UnsupportedEncodingException {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.toString());
        }
    }
}
