package io.quarkus.smallrye.metrics.runtime;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.smallrye.metrics.MetricsRequestHandler;

@WebServlet
public class SmallRyeMetricsServlet extends HttpServlet {

    @Inject
    MetricsRequestHandler metricsHandler;

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        Stream<String> acceptHeaders = Collections.list(request.getHeaders("Accept")).stream();

        metricsHandler.handleRequest(requestPath, method, acceptHeaders, (status, message, headers) -> {
            headers.forEach(response::addHeader);
            response.setStatus(status);

            // FIXME: this is a workaround for issue #3673, normally just response.getWriter().write(message) should do
            final int stepSize = 5000;
            if (message.length() < stepSize) {
                response.getWriter().write(message);
            } else {
                // split a string longer than 8192 characters into smaller chunks
                // and feed them one by one to the writer
                // and call flush() in between
                for (int i = 0; i < (message.length() / stepSize) + 1; i++) {
                    int start = stepSize * i;
                    final int chars = Math.min(stepSize, message.length() - (stepSize * i));
                    response.getWriter()
                            .write(message.substring(start, start + chars));
                    response.getWriter().flush();
                }
            }
        });
    }
}
