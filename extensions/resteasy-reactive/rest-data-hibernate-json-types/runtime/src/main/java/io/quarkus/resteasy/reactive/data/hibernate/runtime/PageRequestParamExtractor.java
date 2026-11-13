package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import jakarta.data.page.PageRequest;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts a {@link PageRequest} from HTTP query parameters.
 * <ul>
 * <li>{@code page} — 1-based page number (default: {@value #DEFAULT_PAGE_NUMBER})</li>
 * <li>{@code size} — maximum number of results per page (default: {@value #DEFAULT_PAGE_SIZE})</li>
 * <li>{@code requestTotal} — whether to request the total number of elements (default: {@code true})</li>
 * </ul>
 */
public class PageRequestParamExtractor implements ParameterExtractor {

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;

    public final String parameterName;

    public PageRequestParamExtractor(String parameterName) {
        this.parameterName = parameterName;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        long page = DEFAULT_PAGE_NUMBER;
        int size = DEFAULT_PAGE_SIZE;
        boolean requestTotal = true;

        String pageStr = (String) context.getQueryParameter(parameterName, true, false);
        if (pageStr != null) {
            page = Long.parseLong(pageStr);
        }

        // Support both prefixed and unprefixed auxiliary parameters for backward compatibility
        // Prefixed: "mainPage_size", "mainPage_requestTotal" when parameterName is "mainPage"
        // Unprefixed: "size", "requestTotal" (for default "page" parameter name)
        String sizeParam = parameterName.equals("page") ? "size" : parameterName + "_size";
        String requestTotalParam = parameterName.equals("page") ? "requestTotal" : parameterName + "_requestTotal";

        String sizeStr = (String) context.getQueryParameter(sizeParam, true, false);
        // Fallback to unprefixed parameter if prefixed one is not found (for backward compatibility)
        if (sizeStr == null && !parameterName.equals("page")) {
            sizeStr = (String) context.getQueryParameter("size", true, false);
        }
        if (sizeStr != null) {
            size = Integer.parseInt(sizeStr);
        }

        String requestTotalStr = (String) context.getQueryParameter(requestTotalParam, true, false);
        // Fallback to unprefixed parameter if prefixed one is not found (for backward compatibility)
        if (requestTotalStr == null && !parameterName.equals("page")) {
            requestTotalStr = (String) context.getQueryParameter("requestTotal", true, false);
        }
        if (requestTotalStr != null) {
            requestTotal = Boolean.parseBoolean(requestTotalStr);
        }

        return PageRequest.ofPage(page, size, requestTotal);
    }
}
