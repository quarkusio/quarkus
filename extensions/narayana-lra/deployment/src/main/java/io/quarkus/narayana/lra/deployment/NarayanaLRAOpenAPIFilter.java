package io.quarkus.narayana.lra.deployment;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;

public class NarayanaLRAOpenAPIFilter implements OASFilter {

    final static String LRA_PROXY_PATH = "/lraproxy";
    final static String LRA_PARTICIAPANT_PROXY_PATH = "/lra-participant-proxy";
    final static String LRA_STATUS_SCHEMA = "LRAStatus";

    boolean openapiIncluded;

    public NarayanaLRAOpenAPIFilter(boolean openapiIncluded) {
        this.openapiIncluded = openapiIncluded;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {

        Paths paths = openAPI.getPaths();

        if (!openapiIncluded) {

            Set<String> lraProxyPaths = new HashSet<>();

            for (String path : paths.getPathItems().keySet()) {
                if (path.startsWith(LRA_PROXY_PATH) || path.startsWith(LRA_PARTICIAPANT_PROXY_PATH)) {
                    lraProxyPaths.add(path);
                }
            }

            // remove LRA proxy paths from OpenAPI
            for (String path : lraProxyPaths) {
                paths.removePathItem(path);
            }

            // remove LRA schema from OpenAPI
            openAPI.getComponents().removeSchema(LRA_STATUS_SCHEMA);
        }

    }

}
