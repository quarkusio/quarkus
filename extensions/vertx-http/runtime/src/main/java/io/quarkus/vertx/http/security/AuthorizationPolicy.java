package io.quarkus.vertx.http.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;

/**
 * Secures endpoint classes and methods with {@link HttpSecurityPolicy}.
 * Policies selected by this annotation will run right after all path-matching policies.
 * Consider following example of the {@link HttpSecurityPolicy}:
 *
 * <pre>
 * {@code
 * import io.quarkus.security.identity.SecurityIdentity;
 * import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
 * import io.smallrye.mutiny.Uni;
 * import io.vertx.ext.web.RoutingContext;
 *
 * public class ExampleAuthorizationPolicy implements HttpSecurityPolicy {
 *
 *     &#64;Override
 *     public Uni<CheckResult> checkPermission(RoutingContext request, Uni<SecurityIdentity> identity,
 *             AuthorizationRequestContext requestContext) {
 *         return isRequestValid(request) ? CheckResult.permit() : CheckResult.deny();
 *     }
 *
 *     private static boolean isRequestValid(RoutingContext event) {
 *         // perform your authorization check
 *         // for example, you can validate headers
 *         var authorizationHeader = event.request().getHeader("Authorization");
 *         // or query params
 *         var crudAction = event.queryParam("action").getFirst();
 *         // replace with your business logic
 *         return authorizationHeader != null && "retrieve".equals(crudAction);
 *     }
 *
 *     @Override
 *     public String name() {
 *         return "example-policy";
 *     }
 * }
 * }
 * </pre>
 *
 * This policy can be bound to Jakarta REST resource in following fashion:
 *
 * <pre>
 * {@code
 * import io.quarkus.vertx.http.security.AuthorizationPolicy;
 * import jakarta.ws.rs.GET;
 * import jakarta.ws.rs.Path;
 *
 * &#64;AuthorizationPolicy(name = "example-policy")
 * &#64;Path("example")
 * public class ExampleResource {
 *
 *     @GET
 *     public String sayHello() {
 *         return "hello";
 *     }
 *
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Inherited
public @interface AuthorizationPolicy {

    /**
     * Specifies name of the {@link HttpSecurityPolicy} that should be applied on the annotation target.
     *
     * @return {@link HttpSecurityPolicy#name()}
     */
    String name();

}
