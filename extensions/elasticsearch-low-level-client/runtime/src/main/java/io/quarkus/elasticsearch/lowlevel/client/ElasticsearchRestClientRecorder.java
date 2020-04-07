package io.quarkus.elasticsearch.lowlevel.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ElasticsearchRestClientRecorder {

    public void configureRestClient(BeanContainer beanContainer, ElasticsearchConfig config, ShutdownContext shutdownContext) {
        List<HttpHost> hosts = config.hosts.stream().map(s -> new HttpHost(s.substring(0, s.indexOf(":")),
                Integer.valueOf(s.substring(s.indexOf(":") + 1)), config.protocol)).collect(Collectors.toList());
        RestClientBuilder builder = RestClient.builder(hosts.toArray(new HttpHost[0]));

        builder.setRequestConfigCallback(new RestClientBuilder.RequestConfigCallback() {
            @Override
            public RequestConfig.Builder customizeRequestConfig(
                    RequestConfig.Builder requestConfigBuilder) {
                return requestConfigBuilder
                        .setConnectTimeout((int) config.connectionTimeout.toMillis());
            }
        });

        if (config.username.isPresent()) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(config.username.get(), config.password.get()));
            builder.setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                @Override
                public HttpAsyncClientBuilder customizeHttpClient(
                        HttpAsyncClientBuilder httpClientBuilder) {
                    return httpClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider);
                }
            });
        }

        //FIXME sniffer if discovery is enabled ?

        RestClient client = builder.build();
        shutdownContext.addShutdownTask(() -> {
            try {
                client.close();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        });

        ElasticsearchRestClientProducer restClientProducer = beanContainer.instance(ElasticsearchRestClientProducer.class);
        restClientProducer.initialize(client);
    }
}
