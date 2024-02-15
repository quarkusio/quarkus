package io.quarkus.resteasy.reactive.server.test.security;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

import java.lang.reflect.Modifier;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.processor.transformation.AnnotationsTransformer;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.resteasy.reactive.server.spi.AnnotationsTransformerBuildItem;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class DenyAllJaxRsTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(PermitAllResource.class, UnsecuredResource.class,
                            TestIdentityProvider.class, UnsecuredResourceInterface.class,
                            TestIdentityController.class, SpecialResource.class,
                            UnsecuredSubResource.class, HelloResource.class, UnsecuredParentResource.class)
                    .addAsResource(new StringAsset("quarkus.security.jaxrs.deny-unannotated-endpoints = true\n"),
                            "application.properties"))
            .addBuildChainCustomizer(builder -> {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        // Here we add an AnnotationsTransformer in order to make sure that the security layer
                        // uses the proper set of transformers
                        context.produce(
                                new AnnotationsTransformerBuildItem(
                                        AnnotationsTransformer.builder().appliesTo(AnnotationTarget.Kind.METHOD)
                                                .transform(transformerContext -> {
                                                    // This transformer auto-adds @GET and @Path if missing, thus emulating Renarde
                                                    MethodInfo methodInfo = transformerContext.getTarget().asMethod();
                                                    ClassInfo declaringClass = methodInfo.declaringClass();
                                                    if (declaringClass.name().toString().equals(SpecialResource.class.getName())
                                                            && !methodInfo.isConstructor()
                                                            && !Modifier.isStatic(methodInfo.flags())) {
                                                        if (methodInfo.declaredAnnotation(GET.class.getName()) == null) {
                                                            // auto-add it
                                                            transformerContext.transform().add(GET.class).done();
                                                        }
                                                        if (methodInfo.declaredAnnotation(Path.class.getName()) == null) {
                                                            // auto-add it
                                                            transformerContext.transform().add(Path.class,
                                                                    AnnotationValue.createStringValue("value",
                                                                            methodInfo.name()))
                                                                    .done();
                                                        }
                                                    }
                                                })));
                    }
                }).produces(AnnotationsTransformerBuildItem.class).build();
            });

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void shouldDenyUnannotated() {
        String path = "/unsecured/defaultSecurity";
        assertStatus(path, 403, 401);
    }

    @Test
    public void shouldDenyUnannotatedOnParentClass() {
        String path = "/unsecured/defaultSecurityParent";
        assertStatus(path, 403, 401);
    }

    @Test
    public void shouldDenyUnannotatedOnInterface() {
        String path = "/unsecured/defaultSecurityInterface";
        assertStatus(path, 403, 401);
    }

    @Test
    public void shouldDenyUnannotatedNonBlocking() {
        String path = "/unsecured/defaultSecurityNonBlocking";
        assertStatus(path, 403, 401);
    }

    @Test
    public void shouldPermitPermitAllMethodNonBlocking() {
        String path = "/permitAll/defaultSecurityNonBlocking";
        assertStatus(path, 200, 200);
    }

    @Test
    public void shouldDenyDenyAllMethod() {
        String path = "/unsecured/denyAll";
        assertStatus(path, 403, 401);
    }

    @Test
    public void shouldPermitPermitAllMethod() {
        assertStatus("/unsecured/permitAll", 200, 200);
    }

    @Test
    public void shouldDenySubResource() {
        String path = "/unsecured/sub/subMethod";
        assertStatus(path, 403, 401);
    }

    @Test
    public void shouldAllowPermitAllSubResource() {
        String path = "/unsecured/permitAllSub/subMethod";
        assertStatus(path, 200, 200);
    }

    @Test
    public void shouldAllowPermitAllClass() {
        String path = "/permitAll/sub/subMethod";
        assertStatus(path, 200, 200);
    }

    @Test
    public void testServerExceptionMapper() {
        given()
                .get("/hello")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("unauthorizedExceptionMapper"));
    }

    @Test
    public void shouldDenyUnannotatedWithAnnotationTransformer() {
        String path = "/special/explicit";
        assertStatus(path, 403, 401);
        path = "/special/implicit";
        assertStatus(path, 403, 401);
    }

    private void assertStatus(String path, int status, int anonStatus) {
        given().auth().preemptive()
                .basic("admin", "admin").get(path)
                .then()
                .statusCode(status);
        given().auth().preemptive()
                .basic("user", "user").get(path)
                .then()
                .statusCode(status);
        when().get(path)
                .then()
                .statusCode(anonStatus);

    }

    @Path("/special")
    public static class SpecialResource {
        @GET
        public String explicit() {
            return "explicit";
        }

        public String implicit() {
            return "implicit";
        }
    }
}
