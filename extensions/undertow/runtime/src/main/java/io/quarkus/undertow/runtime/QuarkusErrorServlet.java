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

public class QuarkusErrorServlet extends HttpServlet {

    public static final String SHOW_STACK = "show-stack";

    private static final String HTML_TEMPLATE = "" +
            "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <title>Internal Server Error</title>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <style>%2$s</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "" +
            "<header>\n" +
            "    <h1 class=\"container\">Internal Server Error</h1>\n" +
            "    <div class=\"exception-message\">\n" +
            "        <h2 class=\"container\">%1$s</h2>\n" +
            "    </div>\n" +
            "</header>\n" +
            "" +
            "<div class=\"container\">\n" +
            "    <div class=\"trace\">\n" +
            "        <pre>%3$s</pre>\n" +
            "    </div>\n" +
            "</div>\n" +
            "" +
            "</body>\n" +
            "</html>\n";

    private static final String ERROR_CSS = "\n" +
            "html, body {\n" +
            "    margin: 0;\n" +
            "    padding: 0;\n" +
            "    font-family: Helvetica, Arial, sans-serif;\n" +
            "    font-size: 14px;\n" +
            "    line-height: 1.4;\n" +
            "}\n" +
            "\n" +
            "html {\n" +
            "    overflow-y: scroll;\n" +
            "}\n" +
            "\n" +
            "body {\n" +
            "    background: #f9f9f9;\n" +
            "}\n" +
            "\n" +
            ".container {\n" +
            "    width: 80%;\n" +
            "    margin: 0 auto;\n" +
            "}\n" +
            "\n" +
            "header {\n" +
            "    background: #ad1c1c;\n" +
            "}\n" +
            "\n" +
            ".exception-message {\n" +
            "    background: #be2828;\n" +
            "}\n" +
            "\n" +
            "h1, h2 {\n" +
            "    margin: 0;\n" +
            "    padding: 0;\n" +
            "    font-weight: normal;\n" +
            "}\n" +
            "\n" +
            "h1 {\n" +
            "    font-size: 22px;\n" +
            "    color: #fff;\n" +
            "    padding: 10px 0;\n" +
            "}\n" +
            "\n" +
            "h2 {\n" +
            "    font-size: 18px;\n" +
            "    color: rgba(255, 255, 255, 0.85);\n" +
            "    padding: 20px 0;\n" +
            "}\n" +
            "\n" +
            ".trace {\n" +
            "    background: #fff;\n" +
            "    padding: 15px;\n" +
            "    margin: 15px auto;\n" +
            "    overflow-y: scroll;\n" +
            "    border: 1px solid #ececec;\n" +
            "}\n" +
            "\n" +
            "pre {\n" +
            "    white-space: pre;\n" +
            "    font-family: Consolas, Monaco, Menlo, \"Ubuntu Mono\", \"Liberation Mono\", monospace;\n" +
            "    font-size: 12px;\n" +
            "    line-height: 1.5;\n" +
            "}\n";

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
        if (Boolean.parseBoolean(getInitParameter(SHOW_STACK)) && exception != null) {
            details = generateHeaderMessage(exception, uuid == null ? null : uuid.toString());
            stack = generateStackTrace(exception);

        } else if (uuid != null) {
            details += "Error id " + uuid;
        }

        resp.setContentType("text/html");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(String.format(HTML_TEMPLATE, details, ERROR_CSS, stack));
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
