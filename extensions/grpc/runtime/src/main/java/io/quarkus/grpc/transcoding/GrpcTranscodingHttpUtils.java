package io.quarkus.grpc.transcoding;

import java.util.HashMap;
import java.util.Map;

/**
 * The `GrpcTranscodingHttpUtils` class provides utility functions for path handling
 * and parameter extraction during the gRPC message transcoding process. Its key
 * functions include:
 * <p>
 * Checking if a request path matches a given gRPC path template.
 * Extracting path parameters from both gRPC path templates and concrete HTTP paths.
 */
public class GrpcTranscodingHttpUtils {

    /**
     * Determines if a given HTTP request path conforms to a specified gRPC path template.
     *
     * @param requestPath The actual HTTP request path to be checked.
     * @param pathTemplate The gRPC path template defining the expected structure.
     * @return `true` if the paths match, `false` otherwise.
     */
    public static boolean isPathMatch(String requestPath, String pathTemplate) {
        int pathIndex = 0;
        int templateIndex = 0;

        while (pathIndex < requestPath.length() && templateIndex < pathTemplate.length()) {
            int pathEnd = requestPath.indexOf('/', pathIndex);
            int templateEnd = pathTemplate.indexOf('/', templateIndex);

            // Extract the current segment from both paths
            String requestPart = pathEnd == -1 ? requestPath.substring(pathIndex) : requestPath.substring(pathIndex, pathEnd);
            String templatePart = templateEnd == -1 ? pathTemplate.substring(templateIndex)
                    : pathTemplate.substring(templateIndex, templateEnd);

            // Check if the template part is a variable segment
            if (templatePart.startsWith("{") && templatePart.endsWith("}")) {
                if (requestPart.isEmpty()) {
                    return false;
                }
                // Skip to the end of the next segment
                pathIndex = pathEnd != -1 ? pathEnd + 1 : requestPath.length();
                templateIndex = templateEnd != -1 ? templateEnd + 1 : pathTemplate.length();
            } else {
                if (!requestPart.equals(templatePart)) {
                    return false;
                }

                // Skip to the end of the next segment
                pathIndex = pathEnd != -1 ? pathEnd + 1 : requestPath.length();
                templateIndex = templateEnd != -1 ? templateEnd + 1 : pathTemplate.length();
            }
        }

        // Ensure both paths have been fully consumed
        return pathIndex == requestPath.length() && templateIndex == pathTemplate.length();
    }

    /**
     * Extracts path parameters from a gRPC path template and an associated HTTP path.
     *
     * @param pathTemplate The gRPC path template defining the parameter structure.
     * @param httpPath The actual HTTP path from which to extract the parameter values.
     * @return A `Map` containing the extracted parameter names and their corresponding values.
     */
    public static Map<String, String> extractPathParams(String pathTemplate, String httpPath) {
        Map<String, String> extractedParams = new HashMap<>();

        String[] pathParts = httpPath.split("/");
        String[] templateParts = pathTemplate.split("/");

        for (int i = 0; i < pathParts.length; i++) {
            String pathPart = pathParts[i];
            String templatePart = templateParts[i];

            if (templatePart.startsWith("{") && templatePart.endsWith("}")) {
                String paramName = templatePart.substring(1, templatePart.length() - 1);
                extractedParams.put(paramName, pathPart);
            }
        }

        return extractedParams;
    }
}
