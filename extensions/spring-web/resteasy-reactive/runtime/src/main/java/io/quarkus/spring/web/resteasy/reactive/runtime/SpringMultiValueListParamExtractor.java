package io.quarkus.spring.web.resteasy.reactive.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.parameters.ParameterExtractor;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class SpringMultiValueListParamExtractor implements ParameterExtractor {

    /**
     * Returns a List containing all query parameters from the request, splitting values by commas if necessary.
     *
     * <p>
     * Spring MVC maps comma-delimited request parameters into a {@code List<String>}.
     * To maintain compatibility, this method follows the same approach:
     * If a query parameter contains multiple values, separated by commas, it adds those multiple values; otherwise, it is added
     * directly.
     * </p>
     *
     * <p>
     * Example:
     *
     * <pre>
     * ?tags=java,quarkus&amp;category=framework
     * </pre>
     *
     * would produce:
     *
     * <pre>
     * ["java", "quarkus", "framework"]
     * </pre>
     * </p>
     *
     * @param context The ResteasyReactiveRequestContext containing the HTTP request.
     * @return An immutable list of all extracted query parameters.
     */
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        Pattern commaPattern = Pattern.compile(",");

        List<String> allQueryParams = new ArrayList<>();
        for (String paramName : context.serverRequest().queryParamNames()) {
            List<String> values = context.serverRequest().getAllQueryParams(paramName);
            if (values != null) {
                for (String value : values) {
                    allQueryParams.addAll(Arrays.asList(commaPattern.split(value)));
                }
            }
        }

        return List.copyOf(allQueryParams);
    }

}
