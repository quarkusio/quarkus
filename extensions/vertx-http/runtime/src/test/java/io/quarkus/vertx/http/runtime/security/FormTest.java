package io.quarkus.vertx.http.runtime.security;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.vertx.http.runtime.AccessLogConfig;
import io.quarkus.vertx.http.runtime.AuthRuntimeConfig;
import io.quarkus.vertx.http.runtime.BodyConfig;
import io.quarkus.vertx.http.runtime.FilterConfig;
import io.quarkus.vertx.http.runtime.FormAuthConfig;
import io.quarkus.vertx.http.runtime.HeaderConfig;
import io.quarkus.vertx.http.runtime.PolicyConfig;
import io.quarkus.vertx.http.runtime.PolicyMappingConfig;
import io.quarkus.vertx.http.runtime.ProxyConfig;
import io.quarkus.vertx.http.runtime.SameSiteCookieConfig;
import io.quarkus.vertx.http.runtime.ServerLimitsConfig;
import io.quarkus.vertx.http.runtime.ServerSslConfig;
import io.quarkus.vertx.http.runtime.StaticResourcesConfig;
import io.quarkus.vertx.http.runtime.TrafficShapingConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.WebsocketServerConfig;
import io.quarkus.vertx.http.runtime.cors.CORSConfig;
import io.quarkus.vertx.http.security.Form;

public class FormTest {

    @Test
    void testFormBuilder() {
        FormAuthConfig defaults = new FormAuthConfig() {

            @Override
            public Optional<String> loginPage() {
                return Optional.of("login123");
            }

            @Override
            public String usernameParameter() {
                return "username123";
            }

            @Override
            public String passwordParameter() {
                return "pwd123";
            }

            @Override
            public Optional<String> errorPage() {
                return Optional.of("error123");
            }

            @Override
            public Optional<String> landingPage() {
                return Optional.of("landing123");
            }

            @Override
            public boolean redirectAfterLogin() {
                return false;
            }

            @Override
            public String locationCookie() {
                return "location123";
            }

            @Override
            public Duration timeout() {
                return null;
            }

            @Override
            public Duration newCookieInterval() {
                return null;
            }

            @Override
            public String cookieName() {
                return "must-be-replaced";
            }

            @Override
            public Optional<String> cookiePath() {
                return Optional.of("must-be-replaced");
            }

            @Override
            public Optional<String> cookieDomain() {
                return Optional.of("must-be-replaced");
            }

            @Override
            public boolean httpOnlyCookie() {
                return false;
            }

            @Override
            public CookieSameSite cookieSameSite() {
                return CookieSameSite.NONE;
            }

            @Override
            public Optional<Duration> cookieMaxAge() {
                return Optional.empty();
            }

            @Override
            public String postLocation() {
                return "must-be-replaced";
            }
        };
        VertxHttpConfig httpConfig = new VertxHttpConfigImpl(defaults);
        Form form = new Form.Builder(httpConfig, true)
                .httpOnlyCookie()
                .timeout(Duration.ofSeconds(5))
                .newCookieInterval(Duration.ofSeconds(6))
                .cookieName("cookie-name123")
                .cookiePath("cookie-path123")
                .cookieDomain("cookie-domain123")
                .postLocation("post-location123")
                .cookieSameSite(FormAuthConfig.CookieSameSite.LAX)
                .build();
        Assertions.assertTrue(form.enabled());
        Assertions.assertEquals("login123", form.loginPage().orElseThrow());
        Assertions.assertEquals("username123", form.usernameParameter());
        Assertions.assertEquals("pwd123", form.passwordParameter());
        Assertions.assertEquals("error123", form.errorPage().orElseThrow());
        Assertions.assertEquals("landing123", form.landingPage().orElseThrow());
        Assertions.assertEquals("location123", form.locationCookie());
        Assertions.assertEquals(Duration.ofSeconds(5), form.timeout());
        Assertions.assertEquals(Duration.ofSeconds(6), form.newCookieInterval());
        Assertions.assertEquals("cookie-name123", form.cookieName());
        Assertions.assertEquals("cookie-path123", form.cookiePath().orElseThrow());
        Assertions.assertEquals("cookie-domain123", form.cookieDomain().orElseThrow());
        Assertions.assertTrue(form.httpOnlyCookie());
        Assertions.assertEquals(FormAuthConfig.CookieSameSite.LAX, form.cookieSameSite());
        Assertions.assertEquals("post-location123", form.postLocation());
    }

    /**
     * Purpose of this {@link VertxHttpConfig} is to simply test
     * {@link AuthRuntimeConfig#form()} and {@link VertxHttpConfig#encryptionKey()}.
     * If you added a new property into the {@link VertxHttpConfig}, you only need to care if it belongs to one of these.
     * For other properties, just add a dummy method.
     */
    private record VertxHttpConfigImpl(FormAuthConfig formAuthConfig) implements VertxHttpConfig {

        public AuthRuntimeConfig auth() {
            return new AuthRuntimeConfig() {

                @Override
                public Map<String, PolicyMappingConfig> permissions() {
                    return Map.of();
                }

                @Override
                public Map<String, PolicyConfig> rolePolicy() {
                    return Map.of();
                }

                @Override
                public Map<String, List<String>> rolesMapping() {
                    return Map.of();
                }

                @Override
                public String certificateRoleAttribute() {
                    return "";
                }

                @Override
                public Optional<Path> certificateRoleProperties() {
                    return Optional.empty();
                }

                @Override
                public Optional<String> realm() {
                    return Optional.empty();
                }

                @Override
                public FormAuthConfig form() {
                    return formAuthConfig;
                }

                @Override
                public boolean inclusive() {
                    return false;
                }

                @Override
                public InclusiveMode inclusiveMode() {
                    return null;
                }
            };
        }

        public boolean corsEnabled() {
            return false;
        }

        public boolean oldCorsEnabled() {
            return false;
        }

        public int port() {
            return 0;
        }

        public int testPort() {
            return 0;
        }

        public String host() {
            return null;
        }

        @Override
        public Optional<String> testHost() {
            return Optional.empty();
        }

        public boolean hostEnabled() {
            return false;
        }

        public int sslPort() {
            return 0;
        }

        public int testSslPort() {
            return 0;
        }

        public Optional<Boolean> testSslEnabled() {
            return Optional.empty();
        }

        public Optional<InsecureRequests> insecureRequests() {
            return Optional.empty();
        }

        public boolean http2() {
            return false;
        }

        public boolean http2PushEnabled() {
            return false;
        }

        public CORSConfig cors() {
            return null;
        }

        public ServerSslConfig ssl() {
            return null;
        }

        public Optional<String> tlsConfigurationName() {
            return Optional.empty();
        }

        public StaticResourcesConfig staticResources() {
            return null;
        }

        public boolean handle100ContinueAutomatically() {
            return false;
        }

        public OptionalInt ioThreads() {
            return OptionalInt.empty();
        }

        public ServerLimitsConfig limits() {
            return null;
        }

        public Duration idleTimeout() {
            return null;
        }

        public Duration readTimeout() {
            return null;
        }

        public BodyConfig body() {
            return null;
        }

        public Optional<String> encryptionKey() {
            return Optional.empty();
        }

        public boolean soReusePort() {
            return false;
        }

        public boolean tcpQuickAck() {
            return false;
        }

        public boolean tcpCork() {
            return false;
        }

        public boolean tcpFastOpen() {
            return false;
        }

        public int acceptBacklog() {
            return 0;
        }

        public OptionalInt initialWindowSize() {
            return OptionalInt.empty();
        }

        public String domainSocket() {
            return null;
        }

        public boolean domainSocketEnabled() {
            return false;
        }

        public boolean recordRequestStartTime() {
            return false;
        }

        public AccessLogConfig accessLog() {
            return null;
        }

        public TrafficShapingConfig trafficShaping() {
            return null;
        }

        public Map<String, SameSiteCookieConfig> sameSiteCookie() {
            return null;
        }

        public Optional<PayloadHint> unhandledErrorContentTypeDefault() {
            return Optional.empty();
        }

        public Map<String, HeaderConfig> header() {
            return null;
        }

        public Map<String, FilterConfig> filter() {
            return null;
        }

        public ProxyConfig proxy() {
            return null;
        }

        public WebsocketServerConfig websocketServer() {
            return null;
        }
    }
}
