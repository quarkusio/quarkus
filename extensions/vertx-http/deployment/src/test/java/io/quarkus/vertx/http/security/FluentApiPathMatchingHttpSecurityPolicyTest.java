package io.quarkus.vertx.http.security;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class FluentApiPathMatchingHttpSecurityPolicyTest extends PathMatchingHttpSecurityPolicyTest {

    @RegisterExtension
    static QuarkusUnitTest test = createQuarkusUnitTest("", HttpPermissionsConfig.class);

    public static class HttpPermissionsConfig {

        void configure(@Observes HttpSecurity httpSecurity, CustomNamedPolicy customNamedPolicy) {

            httpSecurity.path("/api/one/*/jamaica/*", "/api/public", "/api*").permit();

            httpSecurity.path("/api/*/sadly/*/dont-know").authorization().deny();

            httpSecurity.path("/static-file.html", "/api/baz", "/", "/api/foo/bar", "/api/one/*/*/five", "/api/one/*/three/*",
                    "/api/next/*/prev", "/api/*/bar").authenticated();

            httpSecurity.path("/api/fubar/baz*").authenticated();

            httpSecurity.path("/q/*").authenticated();

            httpSecurity.path("/secured/*").shared().authorization()
                    .roles(Map.of("root", List.of("admin", "user")), "**");

            httpSecurity.path("/secured/user/*").roles("user");

            httpSecurity.path("/secured/admin/*").authorization().roles("admin");

            httpSecurity.path("/*").shared().policy(customNamedPolicy);

            httpSecurity.rolesMapping(Map.of(
                    "root1", List.of("admin", "user"),
                    "admin1", List.of("admin"),
                    "public1", List.of("public2")));
        }

    }
}
