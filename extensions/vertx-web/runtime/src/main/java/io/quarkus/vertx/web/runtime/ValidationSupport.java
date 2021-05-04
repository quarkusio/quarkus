package io.quarkus.vertx.web.runtime;

import java.util.Iterator;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path.Node;
import javax.validation.Validator;

import io.quarkus.arc.ArcContainer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ValidationSupport {

    public static final String APPLICATION_JSON = "application/json";
    public static final String ACCEPT_HEADER = "Accept";
    public static final String PROBLEM_TITLE = "title";
    public static final String PROBLEM_DETAIL = "details";
    public static final String PROBLEM_FIELD = "field";
    public static final String PROBLEM_MESSAGE = "message";
    public static final String PROBLEM_STATUS = "status";
    public static final String PROBLEM_VIOLATIONS = "violations";

    private ValidationSupport() {
        // Avoid direct instantiation
    }

    public static Validator getValidator(ArcContainer container) {
        return container.instance(Validator.class).get();
    }

    public static String mapViolationsToJson(Set<ConstraintViolation<?>> violations, HttpServerResponse response) {
        response.setStatusCode(500);
        JsonObject json = generateJsonResponse(violations, true);
        return json.encode();
    }

    /**
     * Generates a JSON response following https://opensource.zalando.com/problem/constraint-violation/
     * 
     * @param violations the violations
     * @return the json object
     */
    private static JsonObject generateJsonResponse(Set<ConstraintViolation<?>> violations, boolean violationInProducedItem) {
        JsonObject json = new JsonObject()
                .put(PROBLEM_TITLE, "Constraint Violation")
                .put(PROBLEM_DETAIL, "validation constraint violations");

        JsonArray array = new JsonArray();
        boolean isProduced = false;
        for (ConstraintViolation<?> cv : violations) {
            if (isReturnValueConstraintViolation(cv)) {
                isProduced = true;
            }
            JsonObject violation = new JsonObject();
            violation.put(PROBLEM_FIELD, cv.getPropertyPath().toString());
            violation.put(PROBLEM_MESSAGE, cv.getMessage());
            array.add(violation);
        }
        json.put(PROBLEM_STATUS, isProduced || violationInProducedItem ? 500 : 400);
        json.put(PROBLEM_VIOLATIONS, array);
        return json;
    }

    public static void handleViolationException(ConstraintViolationException ex, RoutingContext rc, boolean forceJsonEncoding) {
        String accept = rc.request().getHeader(ACCEPT_HEADER);
        if (forceJsonEncoding || accept != null && accept.contains(APPLICATION_JSON)) {
            rc.response().putHeader(RouteHandlers.CONTENT_TYPE, APPLICATION_JSON);
            JsonObject json = generateJsonResponse(ex.getConstraintViolations(), false);
            rc.response().setStatusCode(json.getInteger(PROBLEM_STATUS));
            rc.response().end(json.encode());
        } else {
            // Check status
            int status = 400;
            for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
                if (isReturnValueConstraintViolation(violation)) {
                    status = 500;
                    break;
                }
            }
            // If not JSON just fails.
            rc.fail(status, ex);
        }
    }

    static boolean isReturnValueConstraintViolation(ConstraintViolation<?> violation) {
        Iterator<Node> pathNodes = violation.getPropertyPath().iterator();
        // Returns true if there are at least two nodes and the first is METHOD and the second RETURN_VALUE
        return pathNodes.hasNext() && pathNodes.next().getKind() == ElementKind.METHOD && pathNodes.hasNext()
                && pathNodes.next().getKind() == ElementKind.RETURN_VALUE;
    }

}
