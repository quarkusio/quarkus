/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.undertow.deployment.devmode;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * Generates an error page with a stack trace
 *
 * @author Stuart Douglas
 */
public class ReplacementDebugPage {

    private static final String HTML_TEMPLATE = "" +
            "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <title>Error restarting Quarkus: %1$s</title>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <style>%3$s</style>\n" +
            "</head>\n" +
            "<body>\n" +
            "" +
            "<header>\n" +
            "    <h1 class=\"container\">Error restarting Quarkus</h1>\n" +
            "    <div class=\"exception-message\">\n" +
            "        <h2 class=\"container\">%1$s</h2>\n" +
            "    </div>\n" +
            "</header>\n" +
            "" +
            "<div class=\"container\">\n" +
            "    <div class=\"trace\">\n" +
            "        <pre>%2$s</pre>\n" +
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

    public static void handleRequest(HttpServerExchange exchange, final Throwable exception) {
        String bodyText = generateHtml(exception);

        exchange.setStatusCode(500);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8");
        exchange.getResponseSender().send(bodyText);
    }

    private static String generateHtml(final Throwable exception) {
        String headerMessage = generateHeaderMessage(exception);
        String stackTrace = generateStackTrace(exception);

        return String.format(HTML_TEMPLATE, headerMessage, stackTrace, ERROR_CSS);
    }

    private static String generateStackTrace(final Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));

        return escapeHtml(stringWriter.toString().trim());
    }

    private static String generateHeaderMessage(final Throwable exception) {
        return escapeHtml(String.format("%s: %s", exception.getClass().getName(), exception.getMessage()));
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
