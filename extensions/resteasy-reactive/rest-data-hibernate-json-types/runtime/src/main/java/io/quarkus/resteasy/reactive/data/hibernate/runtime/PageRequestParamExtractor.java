package io.quarkus.resteasy.reactive.data.hibernate.runtime;

import jakarta.data.page.PageRequest;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

/**
 * Extracts a {@link PageRequest} from HTTP query parameters.
 * <p>
 * <b>Parameter naming:</b>
 * <ul>
 * <li><b>Default name:</b> When the parameter is not annotated with {@code @RestQuery},
 * or annotated without a value ({@code @RestQuery}), or with an empty value
 * ({@code @RestQuery("")}), the parameter name defaults to {@code "page"}.</li>
 * <li><b>Custom name:</b> When annotated with {@code @RestQuery("customName")},
 * uses {@code "customName"} as the parameter name.</li>
 * </ul>
 * <p>
 * <b>Query parameters:</b>
 * <ul>
 * <li>{@code <name>} — 1-based page number (default: {@value #DEFAULT_PAGE_NUMBER})</li>
 * <li>{@code <name>_size} or {@code size} — maximum number of results per page
 * (default: {@value #DEFAULT_PAGE_SIZE})</li>
 * <li>{@code <name>_requestTotal} or {@code requestTotal} — whether to request
 * the total number of elements (default: {@code true})</li>
 * </ul>
 * <p>
 * <b>Auxiliary parameters:</b>
 * <ul>
 * <li><b>Default name:</b> Uses unprefixed {@code size} and {@code requestTotal}</li>
 * <li><b>Custom name:</b> Uses prefixed {@code <name>_size} and {@code <name>_requestTotal}</li>
 * </ul>
 * This allows multiple {@code PageRequest} parameters with different names to coexist
 * without interfering with each other.
 * <p>
 * <b>Examples:</b>
 *
 * <pre>
 * // Default name:
 * {@code PageRequest page}  →  ?page=2&size=25&requestTotal=false
 *
 * // Custom name:
 * {@code @RestQuery("pageNum") PageRequest pageNum}
 *   →  ?pageNum=2&pageNum_size=25&pageNum_requestTotal=false
 *
 * // Multiple page requests:
 * {@code @RestQuery("mainPage") PageRequest mainPage, @RestQuery("secondPage") PageRequest secondPage}
 *   →  ?mainPage=2&mainPage_size=25&secondPage=5&secondPage_size=50
 * </pre>
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
        String pageStr = (String) context.getQueryParameter(parameterName, true, false);

        // Use prefixed auxiliary parameters for custom names, unprefixed for default "page"
        // Default: "size", "requestTotal" (for backward compatibility)
        // Custom: "mainPage_size", "mainPage_requestTotal" when parameterName is "mainPage"
        String sizeParam = parameterName.equals("page") ? "size" : parameterName + "_size";
        String requestTotalParam = parameterName.equals("page") ? "requestTotal" : parameterName + "_requestTotal";

        String sizeStr = (String) context.getQueryParameter(sizeParam, true, false);
        String requestTotalStr = (String) context.getQueryParameter(requestTotalParam, true, false);

        // For custom-named parameters, return null if no parameters are provided
        // For default "page" parameter, always return with defaults for backward compatibility
        if (!parameterName.equals("page") && pageStr == null && sizeStr == null && requestTotalStr == null) {
            return null;
        }

        long page = pageStr != null ? Long.parseLong(pageStr) : DEFAULT_PAGE_NUMBER;
        int size = sizeStr != null ? Integer.parseInt(sizeStr) : DEFAULT_PAGE_SIZE;
        boolean requestTotal = requestTotalStr != null ? Boolean.parseBoolean(requestTotalStr) : true;

        return PageRequest.ofPage(page, size, requestTotal);
    }
}
