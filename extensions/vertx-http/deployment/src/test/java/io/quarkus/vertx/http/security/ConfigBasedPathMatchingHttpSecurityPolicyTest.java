package io.quarkus.vertx.http.security;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigBasedPathMatchingHttpSecurityPolicyTest extends PathMatchingHttpSecurityPolicyTest {

    private static final String APP_PROPS = """
            quarkus.http.auth.permission.authenticated.paths=/
            quarkus.http.auth.permission.authenticated.policy=authenticated
            quarkus.http.auth.permission.public.paths=/api*
            quarkus.http.auth.permission.public.policy=permit
            quarkus.http.auth.permission.foo.paths=/api/foo/bar
            quarkus.http.auth.permission.foo.policy=authenticated
            quarkus.http.auth.permission.unsecured.paths=/api/public
            quarkus.http.auth.permission.unsecured.policy=permit
            quarkus.http.auth.permission.inner-wildcard.paths=/api/*/bar
            quarkus.http.auth.permission.inner-wildcard.policy=authenticated
            quarkus.http.auth.permission.inner-wildcard2.paths=/api/next/*/prev
            quarkus.http.auth.permission.inner-wildcard2.policy=authenticated
            quarkus.http.auth.permission.inner-wildcard3.paths=/api/one/*/three/*
            quarkus.http.auth.permission.inner-wildcard3.policy=authenticated
            quarkus.http.auth.permission.inner-wildcard4.paths=/api/one/*/*/five
            quarkus.http.auth.permission.inner-wildcard4.policy=authenticated
            quarkus.http.auth.permission.inner-wildcard5.paths=/api/one/*/jamaica/*
            quarkus.http.auth.permission.inner-wildcard5.policy=permit
            quarkus.http.auth.permission.inner-wildcard6.paths=/api/*/sadly/*/dont-know
            quarkus.http.auth.permission.inner-wildcard6.policy=deny
            quarkus.http.auth.permission.baz.paths=/api/baz
            quarkus.http.auth.permission.baz.policy=authenticated
            quarkus.http.auth.permission.static-resource.paths=/static-file.html
            quarkus.http.auth.permission.static-resource.policy=authenticated
            quarkus.http.auth.permission.fubar.paths=/api/fubar/baz*
            quarkus.http.auth.permission.fubar.policy=authenticated
            quarkus.http.auth.permission.management.paths=/q/*
            quarkus.http.auth.permission.management.policy=authenticated
            quarkus.http.auth.policy.shared1.roles.root=admin,user
            quarkus.http.auth.permission.shared1.paths=/secured/*
            quarkus.http.auth.permission.shared1.policy=shared1
            quarkus.http.auth.permission.shared1.shared=true
            quarkus.http.auth.policy.unshared1.roles-allowed=user
            quarkus.http.auth.permission.unshared1.paths=/secured/user/*
            quarkus.http.auth.permission.unshared1.policy=unshared1
            quarkus.http.auth.policy.unshared2.roles-allowed=admin
            quarkus.http.auth.permission.unshared2.paths=/secured/admin/*
            quarkus.http.auth.permission.unshared2.policy=unshared2
            quarkus.http.auth.permission.shared2.paths=/*
            quarkus.http.auth.permission.shared2.shared=true
            quarkus.http.auth.permission.shared2.policy=custom
            quarkus.http.auth.roles-mapping.root1=admin,user
            quarkus.http.auth.roles-mapping.admin1=admin
            quarkus.http.auth.roles-mapping.public1=public2
            """;

    @RegisterExtension
    static QuarkusUnitTest test = createQuarkusUnitTest(APP_PROPS);

}
