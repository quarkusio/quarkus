package io.quarkus.resteasy.reactive.server.test.security;

import static io.quarkus.resteasy.reactive.server.test.security.AuthMechRequest.requestWithBasicAuthUser;
import static io.quarkus.resteasy.reactive.server.test.security.AuthMechRequest.requestWithFormAuth;
import static io.quarkus.vertx.http.runtime.security.HttpCredentialTransport.Type.AUTHORIZATION;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.logmanager.Level;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.arc.Arc;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.quarkus.vertx.http.runtime.security.annotation.BasicAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.FormAuthentication;
import io.quarkus.vertx.http.runtime.security.annotation.HttpAuthenticationMechanism;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class AnnotationBasedAuthMechanismSelectionTest {

    private static final List<AuthMechRequest> REQUESTS = List.of(
            new AuthMechRequest("annotated-http-permissions/no-roles-allowed-basic").basic().noRbacAnnotation(),
            new AuthMechRequest("unannotated-http-permissions/no-roles-allowed-basic").basic().noRbacAnnotation(),
            new AuthMechRequest("annotated-http-permissions/roles-allowed-annotation-basic-auth").basic(),
            new AuthMechRequest("unannotated-http-permissions/roles-allowed-annotation-basic-auth").basic(),
            new AuthMechRequest("annotated-http-permissions/unauthenticated-form").form().noRbacAnnotation(),
            new AuthMechRequest("unannotated-http-permissions/unauthenticated-form").form().noRbacAnnotation(),
            new AuthMechRequest("annotated-http-permissions/authenticated-form").form().authRequest(),
            new AuthMechRequest("unannotated-http-permissions/authenticated-form").form().authRequest(),
            new AuthMechRequest("unannotated-http-permissions/basic-class-level-interface").basic().noRbacAnnotation()
                    .pathAnnotationDeclaredOnInterface(),
            new AuthMechRequest("annotated-http-permissions/basic-class-level-interface").basic().noRbacAnnotation()
                    .pathAnnotationDeclaredOnInterface(),
            new AuthMechRequest("annotated-http-permissions/overridden-parent-class-endpoint").custom().noRbacAnnotation(),
            new AuthMechRequest("annotated-http-permissions/default-impl-custom-class-level-interface").custom()
                    .noRbacAnnotation(),
            new AuthMechRequest("unannotated-http-permissions/overridden-parent-class-endpoint").form().noRbacAnnotation(),
            new AuthMechRequest("unannotated-http-permissions/default-impl-custom-class-level-interface").basic()
                    .noRbacAnnotation().pathAnnotationDeclaredOnInterface(),
            new AuthMechRequest("annotated-http-permissions/default-form-method-level-interface").form().noRbacAnnotation()
                    .defaultAuthMech(),
            new AuthMechRequest("unannotated-http-permissions/default-form-method-level-interface").form().noRbacAnnotation()
                    .defaultAuthMech(),
            new AuthMechRequest("annotated-http-permissions/basic-method-level-interface").basic().noRbacAnnotation()
                    .defaultAuthMech(),
            new AuthMechRequest("unannotated-http-permissions/basic-method-level-interface").basic().noRbacAnnotation()
                    .defaultAuthMech(),
            new AuthMechRequest("annotated-http-permissions/custom-inherited").custom(),
            new AuthMechRequest("annotated-http-permissions/basic-inherited").basic().authRequest(),
            new AuthMechRequest("annotated-http-permissions/form-default").form().defaultAuthMech().authRequest(),
            new AuthMechRequest("annotated-http-permissions/custom").custom().noRbacAnnotation(),
            new AuthMechRequest("annotated-http-permissions/custom-roles-allowed").custom(),
            new AuthMechRequest("unannotated-http-permissions/deny-custom").custom().denyPolicy(),
            new AuthMechRequest("annotated-http-permissions/roles-allowed-jax-rs-policy").form());

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class,
                            CustomBasicAuthMechanism.class, AbstractHttpPermissionsResource.class,
                            AnnotatedHttpPermissionsResource.class, AbstractAnnotatedHttpPermissionsResource.class,
                            UnannotatedHttpPermissionsResource.class, HttpPermissionsResourceClassLevelInterface.class,
                            HttpPermissionsResourceMethodLevelInterface.class, AuthMechRequest.class,
                            TestTrustedIdentityProvider.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            quarkus.http.auth.proactive=false
                                            quarkus.http.auth.form.enabled=true
                                            quarkus.http.auth.form.login-page=
                                            quarkus.http.auth.form.error-page=
                                            quarkus.http.auth.form.landing-page=
                                            quarkus.http.auth.basic=true
                                            quarkus.http.auth.permission.roles1.paths=/annotated-http-permissions/roles-allowed,/unannotated-http-permissions/roles-allowed
                                            quarkus.http.auth.permission.roles1.policy=roles1
                                            quarkus.http.auth.permission.jax-rs.paths=/annotated-http-permissions/roles-allowed-jax-rs-policy
                                            quarkus.http.auth.permission.jax-rs.policy=roles1
                                            quarkus.http.auth.permission.jax-rs.applies-to=JAXRS
                                            quarkus.http.auth.policy.roles1.roles-allowed=admin
                                            quarkus.http.auth.permission.authenticated.auth-mechanism=basic
                                            quarkus.http.auth.permission.authenticated.paths=/annotated-http-permissions/authenticated,/unannotated-http-permissions/authenticated
                                            quarkus.http.auth.permission.authenticated.policy=authenticated
                                            quarkus.http.auth.permission.same-mechanism.paths=/annotated-http-permissions/same-mech
                                            quarkus.http.auth.permission.same-mechanism.policy=authenticated
                                            quarkus.http.auth.permission.same-mechanism.auth-mechanism=custom
                                            quarkus.http.auth.permission.diff-mechanism.paths=/annotated-http-permissions/diff-mech
                                            quarkus.http.auth.permission.diff-mechanism.policy=authenticated
                                            quarkus.http.auth.permission.diff-mechanism.auth-mechanism=basic
                                            quarkus.http.auth.permission.permit1.paths=/annotated-http-permissions/permit,/unannotated-http-permissions/permit
                                            quarkus.http.auth.permission.permit1.policy=permit
                                            quarkus.http.auth.permission.deny1.paths=/annotated-http-permissions/deny,/unannotated-http-permissions/deny
                                            quarkus.http.auth.permission.deny1.policy=deny
                                            """),
                            "application.properties"))
            // with lazy authentication when request is issued to the POST location, we trigger authentication
            // in past we experienced issue that response has already been sent because we called `next` on
            // the RoutingContext that was already ended, this ensures the issue is gone
            .setLogRecordPredicate(logRecord -> {
                if (Level.ERROR == logRecord.getLevel() && logRecord.getMessage() != null) {
                    String message = logRecord.getMessage().toLowerCase();
                    if (message.contains("request") && message.contains("failed")) {
                        Throwable thrown = logRecord.getThrown();
                        return thrown != null && thrown.getMessage() != null
                                && thrown.getMessage().toLowerCase().contains("response head already sent");
                    }
                }
                return false;
            })
            .assertLogRecords(logRecords -> Assertions.assertTrue(logRecords.isEmpty()));

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @MethodSource("authMechanismRequestsIdxs")
    @ParameterizedTest
    public void testAuthMechanismSelection(final int idx) {
        var in = REQUESTS.get(idx);
        in.requestSpecification.get()
                .get(in.path)
                .then()
                .statusCode(in.expectedStatus)
                .body(is(in.expectedBody))
                .header(in.expectedHeaderKey, in.expectedHeaderVal);
        if (in.authRequired && in.unauthorizedRequestSpec != null) {
            in.unauthorizedRequestSpec.get().get(in.path).then().statusCode(403).header(in.expectedHeaderKey,
                    in.expectedHeaderVal);
        }
        if (in.authRequired && in.unauthenticatedRequestSpec != null) {
            in.unauthenticatedRequestSpec.get().get(in.path).then().statusCode(401).header(in.expectedHeaderKey,
                    in.expectedHeaderVal);
        }
        if (in.requestUsingOtherAuthMech != null) {
            if (in.authRequired) {
                in.requestUsingOtherAuthMech.get().get(in.path).then().statusCode(401).header(in.expectedHeaderKey,
                        in.expectedHeaderVal);
            } else {
                // anonymous request - principal name is empty
                in.requestUsingOtherAuthMech.get().get(in.path).then().header(in.expectedHeaderKey,
                        in.expectedHeaderVal).statusCode(401);
            }
        }
    }

    @Test
    public void testHttpPolicyApplied() {
        given().get("/annotated-http-permissions/authenticated").then().statusCode(401);
        given().get("/unannotated-http-permissions/authenticated").then().statusCode(401);
        given().get("/annotated-http-permissions/deny").then().statusCode(401);
        given().get("/unannotated-http-permissions/deny").then().statusCode(401);
        // both basic and form auth mechanism can be used even though the resource is annotated with 'form'
        // because HTTP policies are applied before the mechanism is selected
        requestWithBasicAuthUser().get("/annotated-http-permissions/roles-allowed").then().statusCode(403);
        requestWithFormAuth("user").get("/unannotated-http-permissions/roles-allowed").then().statusCode(403);

        requestWithFormAuth("admin").get("/annotated-http-permissions/roles-allowed").then().statusCode(200);
        requestWithFormAuth("admin").get("/unannotated-http-permissions/roles-allowed").then().statusCode(200);
        requestWithFormAuth("user").get("/unannotated-http-permissions/authenticated").then().statusCode(401);

        // works because no authentication is performed by HTTP permissions policy 'permit', but for @Form is applied
        // @Authenticated by default
        given().get("/annotated-http-permissions/permit").then().statusCode(401);
        given().get("/unannotated-http-permissions/permit").then().statusCode(401);
    }

    @Test
    public void testBothHttpSecPolicyAndAnnotationApplied() {
        // here we test HTTP Security policy applied to all the paths that runs before annotation is matched
        // HTTP policy requires basic, but resource method inherits class-level `@Form` annotation
        requestWithBasicAuthUser().get("/annotated-http-permissions/authenticated").then().statusCode(401);
        requestWithFormAuth("user").get("/annotated-http-permissions/authenticated").then().statusCode(401);
        // send both form & basic credentials
        requestWithFormAuth("user").auth().preemptive().basic("admin", "admin").get("/annotated-http-permissions/authenticated")
                .then().statusCode(401);
    }

    @Test
    public void testAuthenticatedHttpPolicyUsingSameMechanism() {
        requestWithBasicAuthUser().get("/annotated-http-permissions/same-mech").then().statusCode(200);
    }

    @Test
    public void testAuthenticatedHttpPolicyUsingDiffMechanism() {
        // HTTP Security policy applied on all the paths (not just JAX-RS ones) tries to authenticate with a different
        // authentication mechanism than is selected with the annotation, therefore we deny request
        requestWithBasicAuthUser().get("/annotated-http-permissions/diff-mech").then().statusCode(401);
    }

    private static IntStream authMechanismRequestsIdxs() {
        return IntStream.range(0, REQUESTS.size());
    }

    @Path("unannotated-http-permissions")
    public static class UnannotatedHttpPermissionsResource extends AbstractHttpPermissionsResource {

        @HttpAuthenticationMechanism("custom")
        @DenyAll
        @Path("deny-custom")
        @GET
        public String denyCustomAuthMechanism() {
            // verifies custom auth mechanism is applied when authenticated requests comes in (by 403 and custom headers)
            return "ignored";
        }

        @Override
        public String defaultImplementedClassLevelInterfaceMethod() {
            // here we do not repeat Path annotation, therefore this interface auth mechanism is going to be used
            return super.defaultImplementedClassLevelInterfaceMethod();
        }

        @Override
        public String overriddenParentClassEndpoint() {
            // here we do not repeat Path annotation, therefore parent class auth mechanism is going to be used
            return super.overriddenParentClassEndpoint();
        }
    }

    public static class AbstractAnnotatedHttpPermissionsResource extends AbstractHttpPermissionsResource {

        @RolesAllowed("admin")
        @HttpAuthenticationMechanism("custom")
        @Path("custom-roles-allowed")
        @GET
        public String noPolicyCustomAuthMechRolesAllowed() {
            // verifies method-level annotation is used and for basic credentials, custom auth mechanism is applied
            return "custom-roles-allowed";
        }

        @HttpAuthenticationMechanism("custom")
        @Path("custom")
        @GET
        public String noPolicyCustomAuthMech() {
            // verifies method-level annotation is used and for basic credentials, custom auth mechanism is applied
            // even when no RBAC annotation is present
            return securityIdentity.getPrincipal().getName();
        }

        @Authenticated
        @Path("form-default")
        @GET
        public String formDefault() {
            // verifies when no @HttpAuthenticationMechanism is applied, default form authentication is used
            // also verifies @HttpAuthenticationMechanism on abstract class is not applied
            return "form-default";
        }

    }

    @HttpAuthenticationMechanism("custom") // verifies that
    @Path("annotated-http-permissions")
    public static class AnnotatedHttpPermissionsResource extends AbstractAnnotatedHttpPermissionsResource {

        @Authenticated
        @BasicAuthentication
        @Path("basic-inherited")
        @GET
        public String basicInherited() {
            // verifies method-level annotation has priority over inherited class-level annotation
            return "basic-inherited";
        }

        @RolesAllowed("admin")
        @Path("custom-inherited")
        @GET
        public String customInherited() {
            // verifies class-level annotation is applied, not inherited form authentication from abstract class
            return "custom-inherited";
        }

        @Path("default-impl-custom-class-level-interface")
        @GET
        @Override
        public String defaultImplementedClassLevelInterfaceMethod() {
            // here we repeated Path annotation, therefore this class http auth mechanism is going to be used
            return super.defaultImplementedClassLevelInterfaceMethod();
        }

        @Path("overridden-parent-class-endpoint")
        @GET
        @Override
        public String overriddenParentClassEndpoint() {
            // here we repeated Path annotation, therefore this class http auth mechanism is going to be used
            return super.overriddenParentClassEndpoint();
        }

        @GET
        @HttpAuthenticationMechanism("custom")
        @Path("same-mech")
        public String authPolicyIsUsingSameMechAsAnnotation() {
            // policy uses custom mechanism and annotation selects custom mechanism as well
            return "same-mech";
        }

        @GET
        @HttpAuthenticationMechanism("custom")
        @Path("diff-mech")
        public String authPolicyIsUsingDiffMechAsAnnotation() {
            // policy uses basic mechanism and annotation selects custom mechanism
            return "diff-mech";
        }
    }

    public interface HttpPermissionsResourceMethodLevelInterface {

        @Authenticated // by rules of CDI inheritance, this annotation is completely ignored
        @BasicAuthentication
        @Path("basic-method-level-interface")
        @GET
        default String basicMethodLevelInterface() {
            // verifies method-level annotation on default interface method is applied
            return Arc.container().instance(SecurityIdentity.class).get().getPrincipal().getName();
        }

        @Authenticated // by rules of CDI inheritance, this annotation is completely ignored
        @Path("default-form-method-level-interface")
        @GET
        default String defaultFormMethodLevelInterface() {
            // verifies no specific auth mechanism is enforced unless this method is implemented
            return Arc.container().instance(SecurityIdentity.class).get().getPrincipal().getName();
        }
    }

    @BasicAuthentication
    public interface HttpPermissionsResourceClassLevelInterface {

        @Path("basic-class-level-interface")
        @GET
        default String basicClassLevelInterface() {
            // verifies class-level annotation is applied on default interface method
            return Arc.container().instance(SecurityIdentity.class).get().getPrincipal().getName();
        }

        @Path("default-impl-custom-class-level-interface")
        @GET
        default String defaultImplementedClassLevelInterfaceMethod() {
            // this method will be implemented
            return Arc.container().instance(SecurityIdentity.class).get().getPrincipal().getName();
        }
    }

    @FormAuthentication
    public static abstract class AbstractHttpPermissionsResource
            implements HttpPermissionsResourceClassLevelInterface, HttpPermissionsResourceMethodLevelInterface {

        @Inject
        SecurityIdentity securityIdentity;

        @Path("permit")
        @GET
        public String permit() {
            return "permit";
        }

        @Path("deny")
        @GET
        public String deny() {
            return "deny";
        }

        @Path("roles-allowed")
        @GET
        public String rolesAllowed() {
            return "roles-allowed";
        }

        @Path("roles-allowed-jax-rs-policy")
        @GET
        public String rolesAllowedJaxRsPolicy() {
            return "roles-allowed-jax-rs-policy";
        }

        @Path("authenticated")
        @GET
        public String authenticated() {
            return "authenticated";
        }

        @Authenticated
        @Path("authenticated-form")
        @GET
        public String authenticatedNoPolicyFormAuthMech() {
            // verifies class-level annotation declared on this class is applied when RBAC annotation is present
            return "authenticated-form";
        }

        @Path("unauthenticated-form")
        @GET
        public String unauthenticatedNoPolicyFormAuthMech() {
            // verifies class-level annotation declared on this class is applied when no RBAC annotation is present
            return securityIdentity.getPrincipal().getName();
        }

        @RolesAllowed("admin")
        @BasicAuthentication
        @Path("roles-allowed-annotation-basic-auth")
        @GET
        public String rolesAllowedNoPolicyBasicAuthMech() {
            // verifies method-level annotation has priority over class-level annotation on same class
            return "roles-allowed-annotation-basic-auth";
        }

        @BasicAuthentication
        @Path("no-roles-allowed-basic")
        @GET
        public String noPolicyBasicAuthMech() {
            // verifies method-level annotation has priority over class-level even when no RBAC annotation is present
            return securityIdentity.getPrincipal().getName();
        }

        @RolesAllowed("admin")
        @Path("overridden-parent-class-endpoint")
        @GET
        public String overriddenParentClassEndpoint() {
            // verifies method-level annotation has priority over class-level even when no RBAC annotation is present
            return securityIdentity.getPrincipal().getName();
        }
    }

    @Singleton
    public static class CustomBasicAuthMechanism implements io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism {

        static final String CUSTOM_AUTH_HEADER_KEY = CustomBasicAuthMechanism.class.getName();

        private final BasicAuthenticationMechanism delegate;

        public CustomBasicAuthMechanism(BasicAuthenticationMechanism delegate) {
            this.delegate = delegate;
        }

        @Override
        public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
            context.response().putHeader(CUSTOM_AUTH_HEADER_KEY, "true");
            return delegate.authenticate(context, identityProviderManager);
        }

        @Override
        public Uni<ChallengeData> getChallenge(RoutingContext context) {
            return delegate.getChallenge(context);
        }

        @Override
        public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
            return delegate.getCredentialTypes();
        }

        @Override
        public Uni<Boolean> sendChallenge(RoutingContext context) {
            return delegate.sendChallenge(context);
        }

        @Override
        public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
            return Uni.createFrom().item(new HttpCredentialTransport(AUTHORIZATION, "custom"));
        }

        @Override
        public int getPriority() {
            return delegate.getPriority();
        }
    }
}
