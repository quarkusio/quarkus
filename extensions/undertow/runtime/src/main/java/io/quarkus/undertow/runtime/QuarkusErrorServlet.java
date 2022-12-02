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
            String escapedDetails = escapeJsonString(details);
            String escapedStack = escapeJsonString(stack);
            StringBuilder jsonPayload = new StringBuilder("{\"details\":\"").append(escapedDetails)
                    .append("\",\"stack\":\"").append(escapedStack).append("\"}");
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

        return stringWriter.toString().trim();
    }

    private static String generateHeaderMessage(final Throwable exception, String uuid) {
        return String.format("Error id %s, %s: %s", uuid, exception.getClass().getName(),
                extractFirstLine(exception.getMessage()));
    }

    private static String extractFirstLine(final String message) {
        if (null == message) {
            return "";
        }

        String[] lines = message.split("\\r?\\n");
        return lines[0].trim();
    }

    private static String escapeJsonString(final String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }
}
