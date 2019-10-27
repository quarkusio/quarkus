package io.quarkus.undertow.runtime;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.quarkus.runtime.TemplateHtmlBuilder;

public class QuarkusErrorServlet extends HttpServlet {

    public static final String SHOW_STACK = "show-stack";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String details = "";
        String stack = "";
        Object uuid = req.getAttribute(QuarkusExceptionHandler.ERROR_ID);
        Throwable exception = (Throwable) req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String errorMessage = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        if (errorMessage != null) {
            details = errorMessage;
        }
        final boolean showStack = Boolean.parseBoolean(getInitParameter(SHOW_STACK));
        if (showStack && exception != null) {
            details = generateHeaderMessage(exception, uuid == null ? null : uuid.toString());
            stack = generateStackTrace(exception);

        } else if (uuid != null) {
            details += "Error id " + uuid;
        }

        String accept = req.getHeader("Accept");
        if (accept != null && accept.contains("application/json")) {
            resp.setContentType("application/json");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String escapedStack = stack.replace(System.lineSeparator(), "\\n").replace("\"", "\\\"");
            StringBuilder jsonPayload = new StringBuilder("{\"details\":\"").append(details).append("\",\"stack\":\"")
                    .append(escapedStack).append("\"}");
            resp.getWriter().write(jsonPayload.toString());
        } else {
            //We default to HTML representation
            resp.setContentType("text/html");
            resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
            final TemplateHtmlBuilder htmlBuilder = new TemplateHtmlBuilder("Internal Server Error", details, details);
            if (showStack && exception != null) {
                htmlBuilder.stack(exception);
            }
            resp.getWriter().write(htmlBuilder.toString());
        }
    }

    private static String generateStackTrace(final Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));

        return escapeHtml(stringWriter.toString().trim());
    }

    private static String generateHeaderMessage(final Throwable exception, String uuid) {
        return escapeHtml(String.format("Error handling %s, %s: %s", uuid, exception.getClass().getName(),
                extractFirstLine(exception.getMessage())));
    }

    private static String extractFirstLine(final String message) {
        if (null == message) {
            return "";
        }

        String[] lines = message.split("\\r?\\n");
        return lines[0].trim();
    }

    private static String escapeHtml(final String bodyText) {
        if (bodyText == null) {
            return "null";
        }

        return bodyText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
