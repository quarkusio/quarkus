package io.quarkus.oidc.deployment;

import static io.quarkus.oidc.common.runtime.OidcConstants.BACK_CHANNEL_LOGOUT_SID_CLAIM;
import static io.quarkus.oidc.common.runtime.OidcConstants.BACK_CHANNEL_LOGOUT_TOKEN;
import static io.quarkus.oidc.common.runtime.OidcConstants.FRONT_CHANNEL_LOGOUT_SID_PARAM;

import java.util.List;

import org.eclipse.microprofile.jwt.Claims;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OperationImpl;
import io.smallrye.openapi.api.models.PathItemImpl;
import io.smallrye.openapi.api.models.PathsImpl;
import io.smallrye.openapi.api.models.media.ContentImpl;
import io.smallrye.openapi.api.models.media.MediaTypeImpl;
import io.smallrye.openapi.api.models.parameters.ParameterImpl;
import io.smallrye.openapi.api.models.parameters.RequestBodyImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;
import io.smallrye.openapi.api.models.responses.APIResponsesImpl;

/**
 * Create OpenAPI entries (if configured) for OIDC application endpoints.
 */
public class OidcOpenAPIFilter implements OASFilter {
    private static final List<String> OIDC_TAG = List.of("OpenID Connect application endpoint");
    private final String logoutPath;
    private final String backchannelPath;
    private final String frontchannelPath;

    public OidcOpenAPIFilter(String logoutPath, String backchannelPath, String frontchannelPath) {
        this.logoutPath = logoutPath;
        this.backchannelPath = backchannelPath;
        this.frontchannelPath = frontchannelPath;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(new ComponentsImpl());
        }

        if (openAPI.getPaths() == null) {
            openAPI.setPaths(new PathsImpl());
        }
        Paths paths = openAPI.getPaths();

        // Logout
        paths.addPathItem(logoutPath, createLogoutPathItem());

        // Back-Channel Logout
        paths.addPathItem(backchannelPath, createBackchannelPathItem());

        // Front-Channel Logout
        paths.addPathItem(frontchannelPath, createFrontchannelPathItem());
    }

    private PathItem createLogoutPathItem() {
        PathItem pathItem = new PathItemImpl();
        pathItem.setSummary("OpenID Connect - RP-Initiated Logout");
        pathItem.setDescription("See https://openid.net/specs/openid-connect-rpinitiated-1_0.html");
        pathItem.setGET(createLogoutOperation());
        return pathItem;
    }

    private PathItem createBackchannelPathItem() {
        PathItem pathItem = new PathItemImpl();
        pathItem.setSummary("OpenID Connect - Back-Channel Logout Endpoint");
        pathItem.setDescription("See https://openid.net/specs/openid-connect-backchannel-1_0.html");
        pathItem.setPOST(createBackchannelOperation());
        return pathItem;
    }

    private PathItem createFrontchannelPathItem() {
        PathItem pathItem = new PathItemImpl();
        pathItem.setSummary("OpenID Connect - Front-Channel Logout Endpoint");
        pathItem.setDescription("See https://openid.net/specs/openid-connect-frontchannel-1_0.html");
        pathItem.setGET(createFrontchannelOperation());
        return pathItem;
    }

    private Operation createLogoutOperation() {
        Operation operation = new OperationImpl();
        operation.setDescription(
                "The application is able to initiate the logout through this endpoint in conformance with the OpenID Connect RP-Initiated Logout specification.");
        operation.setOperationId("oidc_logout");
        operation.setTags(OIDC_TAG);
        operation.setSummary("The logout endpoint of this application");
        operation.setResponses(createRedirectAPIResponses());
        return operation;
    }

    private Operation createBackchannelOperation() {
        Operation operation = new OperationImpl();
        // formData contains logout_token passed as parameter
        operation.requestBody(createFormDataRequestBody());
        operation.setDescription("See https://openid.net/specs/openid-connect-backchannel-1_0.html");
        operation.setOperationId("oidc_backchannel_logout");
        operation.setTags(OIDC_TAG);
        operation.setSummary("The Back-Channel Logout endpoint at the application");
        // param description comes from specs
        Parameter sidQueryParam = new ParameterImpl().name(BACK_CHANNEL_LOGOUT_SID_CLAIM).description(
                "Session ID - String identifier for a Session. This represents a Session of a User Agent or device for a logged-in End-User at an RP.");
        Parameter logoutTokenParam = new ParameterImpl().style(Parameter.Style.FORM).name(BACK_CHANNEL_LOGOUT_TOKEN);
        operation.setParameters(List.of(sidQueryParam, logoutTokenParam));
        operation.setResponses(createBackchannelAPIResponses());
        return operation;
    }

    private RequestBody createFormDataRequestBody() {
        return new RequestBodyImpl()
                .content(new ContentImpl().addMediaType("application/x-www-form-urlencoded", new MediaTypeImpl()));
    }

    private Operation createFrontchannelOperation() {
        Operation operation = new OperationImpl();
        operation.setDescription("See https://openid.net/specs/openid-connect-frontchannel-1_0.html");
        operation.setOperationId("oidc_frontchannel_logout");
        operation.setTags(OIDC_TAG);
        operation.setSummary("The Front-Channel Logout endpoint at the application");
        // param description comes partially from specs
        Parameter issQueryParam = new ParameterImpl().name(Claims.iss.name()).description(
                "Issuer Identifier for the OP issuing the front-channel logout request. Frontchannel issuer parameter must match the ID token issuer.");
        Parameter sidQueryParam = new ParameterImpl().name(FRONT_CHANNEL_LOGOUT_SID_PARAM).description(
                "Identifier for the Session. Frontchannel session id parameter must match the ID token session id.");
        operation.setParameters(List.of(issQueryParam, sidQueryParam));
        operation.setResponses(createRedirectAPIResponses());
        return operation;
    }

    private APIResponses createRedirectAPIResponses() {
        APIResponses responses = new APIResponsesImpl();
        responses.addAPIResponse("302", createAPIResponse("Found"));
        responses.addAPIResponse("401", createAPIResponse("Unauthorized"));
        responses.addAPIResponse("500", createAPIResponse("Internal Server Error"));
        return responses;
    }

    private APIResponses createBackchannelAPIResponses() {
        APIResponses responses = new APIResponsesImpl();
        responses.addAPIResponse("200", createAPIResponse("OK"));
        responses.addAPIResponse("400", createAPIResponse("Bad Request"));
        responses.addAPIResponse("401", createAPIResponse("Unauthorized"));
        responses.addAPIResponse("500", createAPIResponse("Internal Server Error"));
        return responses;
    }

    private APIResponse createAPIResponse(String description) {
        APIResponse response = new APIResponseImpl();
        response.setDescription(description);
        return response;
    }

}
