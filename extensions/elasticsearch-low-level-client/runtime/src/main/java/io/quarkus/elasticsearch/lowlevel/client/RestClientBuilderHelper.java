package io.quarkus.elasticsearch.lowlevel.client;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

public final class RestClientBuilderHelper {
    private RestClientBuilderHelper() {
        // avoid instanciation
    }

    public static RestClientBuilder createRestClientBuilder(ElasticsearchConfig config) {
        List<HttpHost> hosts = config.hosts.stream().map(s -> new HttpHost(s.substring(0, s.indexOf(":")),
                Integer.parseInt(s.substring(s.indexOf(":") + 1)), config.protocol)).collect(Collectors.toList());
        RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]));

        builder.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
            @Override
            public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder requestConfigBuilder) {
                return requestConfigBuilder
                        .setConnectTimeout((int) config.connectionTimeout.toMillis())
                        .setSocketTimeout((int) config.socketTimeout.toMillis());
            }
        });

        builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                if (config.username.isPresent()) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(AuthScope.ANY,
                            new UsernamePasswordCredentials(config.username.get(), config.password.get()));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                }

                if (config.ioThreadCounts.isPresent()) {
                    IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                            .setIoThreadCount(config.ioThreadCounts.get())
                            .build();
                    httpClientBuilder.setDefaultIOReactorConfig(ioReactorConfig);
                }

                return httpClientBuilder;
            }
        });

        return builder;
    }
}
