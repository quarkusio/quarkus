package io.quarkus.undertow.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Instance;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.annotations.StaticInitSafe;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.config.common.MapBackedConfigSource;

public class WebFilterConfigInjectionWarningsTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addAsServiceProvider("org.eclipse.microprofile.config.spi.ConfigSource",
                    StaticConfigSource.class.getName(), RuntimeConfigSource.class.getName()))
            .setLogRecordPredicate(record -> record.getLevel().intValue() >= Level.WARNING.intValue())
            .assertLogRecords(logRecords -> {
                assertEquals(1, logRecords.size());
                Set<String> messages = logRecords.stream().map(LogRecord::getMessage).collect(Collectors.toSet());
                assertTrue(messages.contains(
                        "Directly injecting a org.eclipse.microprofile.config.inject.ConfigProperty into a jakarta.servlet" +
                                ".annotation.WebFilter may lead to unexpected results. To ensure proper results, please change the "
                                +
                                "type of the field to jakarta.enterprise.inject.Instance<java.lang.String>. Offending field is "
                                +
                                "'configProperty' of class 'io.quarkus.undertow.test.config" +
                                ".WebFilterConfigInjectionWarningsTest$ConfigFilter'"));
            });

    @Test
    public void configWarnings() {
        RestAssured.when().get("/config").then().body(Matchers.is("static runtime"));
    }

    @WebFilter(filterName = "ConfigFilter", servletNames = "config")
    public static class ConfigFilter implements Filter {
        @ConfigProperty(name = "configProperty", defaultValue = "configProperty")
        String configProperty;
        @ConfigProperty(name = "configProperty", defaultValue = "configProperty")
        Instance<String> configPropertyInstance;

        @Override
        public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                final FilterChain filterChain) throws IOException, ServletException {
            filterChain.doFilter(servletRequest, servletResponse);
            servletResponse.getWriter().write(configProperty + " " + configPropertyInstance.get());
        }
    }

    @WebServlet(name = "config", urlPatterns = "/config")
    public static class ConfigServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().write("");
        }
    }

    @StaticInitSafe
    public static class StaticConfigSource extends MapBackedConfigSource {
        public StaticConfigSource() {
            super("static", Map.of("configProperty", "static"), 100);
        }
    }

    public static class RuntimeConfigSource extends MapBackedConfigSource {
        public RuntimeConfigSource() {
            super("runtime", Map.of("configProperty", "runtime"), 200);
        }
    }
}
