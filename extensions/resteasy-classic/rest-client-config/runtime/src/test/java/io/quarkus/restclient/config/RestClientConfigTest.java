package io.quarkus.restclient.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;

import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.junit.jupiter.api.Test;

import io.quarkus.restclient.config.RestClientsConfig.RestClientConfig;
import io.quarkus.restclient.config.key.SharedOneConfigKeyRestClient;
import io.quarkus.restclient.config.key.SharedThreeConfigKeyRestClient;
import io.quarkus.restclient.config.key.SharedTwoConfigKeyRestClient;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigBuilderCustomizer;

class RestClientConfigTest {
    @Test
    void restClientConfigClass() {
        RegisteredRestClient registeredRestClient = new RegisteredRestClient(FullNameRestClient.class, null);
        // application.properties in test/resources
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(registeredRestClient);
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);
        assertEquals(1, restClientsConfig.clients().size());
        assertTrue(restClientsConfig.clients().containsKey(FullNameRestClient.class.getName()));
        verifyConfig(restClientsConfig.getClient(FullNameRestClient.class));
    }

    @Test
    void restClientConfigKey() {
        RegisteredRestClient registeredRestClient = new RegisteredRestClient(ConfigKeyRestClient.class, "key");
        // application.properties in test/resources
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(registeredRestClient);
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);
        assertEquals(1, restClientsConfig.clients().size());
        assertTrue(restClientsConfig.clients().containsKey(ConfigKeyRestClient.class.getName()));
        verifyConfig(restClientsConfig.getClient(ConfigKeyRestClient.class));
    }

    @Test
    void restClientConfigKeyMatchName() {
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(new RegisteredRestClient(ConfigKeyRestClient.class,
                                        ConfigKeyRestClient.class.getName()));
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();
        assertNotNull(config);

        config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(new RegisteredRestClient(ConfigKeyRestClient.class,
                                        ConfigKeyRestClient.class.getSimpleName()));
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();
        assertNotNull(config);
    }

    @Test
    void restClientMicroProfile() {
        RegisteredRestClient registeredRestClient = new RegisteredRestClient(MPRestClient.class, null);
        // application.properties in test/resources
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(registeredRestClient);
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);
        assertEquals(1, restClientsConfig.clients().size());
        assertTrue(restClientsConfig.clients().containsKey(MPRestClient.class.getName()));

        RestClientConfig clientConfig = restClientsConfig.getClient(MPRestClient.class);
        assertTrue(clientConfig.url().isPresent());
        assertThat(clientConfig.url().get()).isEqualTo("http://localhost:8080");
        assertTrue(clientConfig.uri().isPresent());
        assertThat(clientConfig.uri().get()).isEqualTo("http://localhost:8081");
        assertTrue(clientConfig.providers().isPresent());
        assertThat(clientConfig.providers().get()).isEqualTo("io.quarkus.restclient.configuration.MyResponseFilter");
        assertTrue(clientConfig.connectTimeout().isPresent());
        assertThat(clientConfig.connectTimeout().get()).isEqualTo(5000);
        assertTrue(clientConfig.readTimeout().isPresent());
        assertThat(clientConfig.readTimeout().get()).isEqualTo(6000);
        assertTrue(clientConfig.followRedirects().isPresent());
        assertThat(clientConfig.followRedirects().get()).isEqualTo(true);
        assertTrue(clientConfig.proxyAddress().isPresent());
        assertTrue(clientConfig.queryParamStyle().isPresent());
        assertThat(clientConfig.queryParamStyle().get()).isEqualTo(QueryParamStyle.COMMA_SEPARATED);
    }

    @Test
    void restClientMicroProfileConfigKey() {
        RegisteredRestClient registeredRestClient = new RegisteredRestClient(MPConfigKeyRestClient.class, "mp.key");
        // application.properties in test/resources
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(registeredRestClient);
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);
        assertEquals(1, restClientsConfig.clients().size());
        assertTrue(restClientsConfig.clients().containsKey(MPConfigKeyRestClient.class.getName()));

        RestClientConfig clientConfig = restClientsConfig.getClient(MPConfigKeyRestClient.class);
        assertTrue(clientConfig.url().isPresent());
        assertThat(clientConfig.url().get()).isEqualTo("http://localhost:8080");
        assertTrue(clientConfig.uri().isPresent());
        assertThat(clientConfig.uri().get()).isEqualTo("http://localhost:8081");
        assertTrue(clientConfig.providers().isPresent());
        assertThat(clientConfig.providers().get()).isEqualTo("io.quarkus.restclient.configuration.MyResponseFilter");
        assertTrue(clientConfig.connectTimeout().isPresent());
        assertThat(clientConfig.connectTimeout().get()).isEqualTo(5000);
        assertTrue(clientConfig.readTimeout().isPresent());
        assertThat(clientConfig.readTimeout().get()).isEqualTo(6000);
        assertTrue(clientConfig.followRedirects().isPresent());
        assertThat(clientConfig.followRedirects().get()).isEqualTo(true);
        assertTrue(clientConfig.proxyAddress().isPresent());
        assertTrue(clientConfig.queryParamStyle().isPresent());
        assertThat(clientConfig.queryParamStyle().get()).isEqualTo(QueryParamStyle.COMMA_SEPARATED);
    }

    @Test
    void restClientMicroProfileConfigKeyMatchName() {
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(new RegisteredRestClient(MPConfigKeyRestClient.class,
                                        MPConfigKeyRestClient.class.getName()));
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();
        assertNotNull(config);

        config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(new RegisteredRestClient(MPConfigKeyRestClient.class,
                                        MPConfigKeyRestClient.class.getSimpleName()));
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();
        assertNotNull(config);

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);
        assertEquals(1, restClientsConfig.clients().size());
        assertTrue(restClientsConfig.clients().containsKey(MPConfigKeyRestClient.class.getName()));

        RestClientConfig restClientConfig = restClientsConfig.getClient(MPConfigKeyRestClient.class);
        assertTrue(restClientConfig.uri().isPresent());
        assertThat(restClientConfig.uri().get()).isEqualTo("http://localhost:8082");
    }

    @Test
    void restClientDuplicateSimpleName() {
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(
                                        new RegisteredRestClient(
                                                io.quarkus.restclient.config.simple.one.SimpleNameRestClient.class, "one"),
                                        new RegisteredRestClient(
                                                io.quarkus.restclient.config.simple.two.SimpleNameRestClient.class, "two"));
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();
        assertNotNull(config);

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);
        assertEquals(2, restClientsConfig.clients().size());
        assertThat(restClientsConfig.getClient(io.quarkus.restclient.config.simple.one.SimpleNameRestClient.class).uri().get())
                .isEqualTo("http://localhost:8081");
        assertThat(restClientsConfig.getClient(io.quarkus.restclient.config.simple.two.SimpleNameRestClient.class).uri().get())
                .isEqualTo("http://localhost:8082");
    }

    @Test
    void restClientSharedConfigKey() {
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(
                                        new RegisteredRestClient(SharedOneConfigKeyRestClient.class, "shared"),
                                        new RegisteredRestClient(SharedTwoConfigKeyRestClient.class, "shared"),
                                        new RegisteredRestClient(SharedThreeConfigKeyRestClient.class, "shared"));
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();
        assertNotNull(config);

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);
        assertEquals(3, restClientsConfig.clients().size());

        RestClientConfig restClientConfigOne = restClientsConfig.getClient(SharedOneConfigKeyRestClient.class);
        assertThat(restClientConfigOne.uri().get()).isEqualTo("http://localhost:8081");
        assertEquals(2, restClientConfigOne.headers().size());
        assertEquals("one", restClientConfigOne.headers().get("two"));
        assertEquals("two", restClientConfigOne.headers().get("one"));

        RestClientConfig restClientConfigTwo = restClientsConfig
                .getClient(SharedTwoConfigKeyRestClient.class);
        assertThat(restClientConfigTwo.uri().get()).isEqualTo("http://localhost:8082");
        assertEquals(2, restClientConfigTwo.headers().size());
        assertEquals("one", restClientConfigTwo.headers().get("two"));
        assertEquals("two", restClientConfigTwo.headers().get("one"));

        RestClientConfig restClientConfigThree = restClientsConfig
                .getClient(SharedThreeConfigKeyRestClient.class);
        assertThat(restClientConfigThree.uri().get()).isEqualTo("http://localhost:8083");
        assertEquals(2, restClientConfigThree.headers().size());
        assertEquals("one", restClientConfigThree.headers().get("two"));
        assertEquals("two", restClientConfigThree.headers().get("one"));
    }

    @Test
    void buildTimeConfig() {
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsBuildTimeConfig.class)
                .build();
        assertNotNull(config);

        RestClientsBuildTimeConfig buildTimeConfig = config.getConfigMapping(RestClientsBuildTimeConfig.class)
                .get(List.of(new RegisteredRestClient(ConfigKeyRestClient.class, "key")));

        assertFalse(buildTimeConfig.clients().get(ConfigKeyRestClient.class.getName()).removesTrailingSlash());
    }

    @Test
    void defaultPackage() {
        RegisteredRestClient registeredRestClient = new RegisteredRestClient("FullNameRestClient", "FullNameRestClient", null);
        // application.properties in test/resources
        SmallRyeConfig config = ConfigUtils.emptyConfigBuilder()
                .withMapping(RestClientsConfig.class)
                .withCustomizers(new SmallRyeConfigBuilderCustomizer() {
                    @Override
                    public void configBuilder(final SmallRyeConfigBuilder builder) {
                        new AbstractRestClientConfigBuilder() {
                            @Override
                            public List<RegisteredRestClient> getRestClients() {
                                return List.of(registeredRestClient);
                            }
                        }.configBuilder(builder);
                    }
                })
                .build();

        RestClientsConfig restClientsConfig = config.getConfigMapping(RestClientsConfig.class);
        assertEquals(1, restClientsConfig.clients().size());
    }

    private void verifyConfig(RestClientConfig config) {
        assertTrue(config.url().isPresent());
        assertThat(config.url().get()).isEqualTo("http://localhost:8080");
        assertTrue(config.uri().isPresent());
        assertThat(config.uri().get()).isEqualTo("http://localhost:8081");
        assertTrue(config.providers().isPresent());
        assertThat(config.providers().get()).isEqualTo("io.quarkus.restclient.configuration.MyResponseFilter");
        assertTrue(config.connectTimeout().isPresent());
        assertThat(config.connectTimeout().get()).isEqualTo(5000);
        assertTrue(config.readTimeout().isPresent());
        assertThat(config.readTimeout().get()).isEqualTo(6000);
        assertTrue(config.followRedirects().isPresent());
        assertThat(config.followRedirects().get()).isEqualTo(true);
        assertTrue(config.proxyAddress().isPresent());
        assertTrue(config.queryParamStyle().isPresent());
        assertThat(config.queryParamStyle().get()).isEqualTo(QueryParamStyle.COMMA_SEPARATED);
        assertTrue(config.hostnameVerifier().isPresent());
        assertThat(config.hostnameVerifier().get()).isEqualTo("io.quarkus.restclient.configuration.MyHostnameVerifier");
        assertTrue(config.connectionTTL().isPresent());
        assertThat(config.connectionTTL().getAsInt()).isEqualTo(30000);
        assertTrue(config.connectionPoolSize().isPresent());
        assertThat(config.connectionPoolSize().getAsInt()).isEqualTo(10);
        assertTrue(config.maxChunkSize().isPresent());
        assertThat(config.maxChunkSize().get().asBigInteger()).isEqualTo(BigInteger.valueOf(1024));
    }
}
