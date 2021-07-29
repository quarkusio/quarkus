package io.quarkus.runtime;

import java.util.Collections;
import java.util.List;

import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.runtime.util.ExceptionUtil;

public class TemplateHtmlBuilder {

    private static final String SCRIPT_STACKTRACE_MANIPULATION = "<script>\n" +
            "	function toggleStackTraceOrder() {\n" +
            "		var stElement = document.getElementById('stacktrace');\n" +
            "		var current = stElement.getAttribute('data-current-setting');\n" +
            "		if (current == 'original-stacktrace') {\n" +
            "			var reverseOrder = document.getElementById('reversed-stacktrace');\n" +
            "			stElement.innerHTML = reverseOrder.innerHTML;\n" +
            "			stElement.setAttribute('data-current-setting', 'reversed-stacktrace');\n" +
            "		} else {\n" +
            "			var originalOrder = document.getElementById('original-stacktrace');\n" +
            "			stElement.innerHTML = originalOrder.innerHTML;\n" +
            "			stElement.setAttribute('data-current-setting', 'original-stacktrace');\n" +
            "		}\n" +
            "		return;\n" +
            "	}\n" +
            "	function showDefaultStackTraceOrder() {\n" +
            "		var reverseOrder = document.getElementById('reversed-stacktrace');\n" +
            "		var stElement = document.getElementById('stacktrace');\n" +
            "       if (reverseOrder == null || stElement == null) {\n" +
            "           return;\n" +
            "       }\n" +
            "		// default to reverse ordered stacktrace\n" +
            "		stElement.innerHTML = reverseOrder.innerHTML;\n" +
            "		stElement.setAttribute('data-current-setting', 'reversed-stacktrace');\n" +
            "		return;\n" +
            "	}\n" +
            "</script>\n";

    private static final String HTML_TEMPLATE_START = "" +
            "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <title>%1$s%2$s</title>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <style>%3$s</style>\n" +
            SCRIPT_STACKTRACE_MANIPULATION +
            "</head>\n" +
            "<body  onload=\"showDefaultStackTraceOrder()\">\n";

    private static final String HTML_TEMPLATE_END = "</div></body>\n" +
            "</html>\n";

    private static final String HEADER_TEMPLATE = "<header>\n" +
            "    <h1 class=\"container\">%1$s</h1>\n" +
            "    <div class=\"exception-message\">\n" +
            "        <h2 class=\"container\">%2$s</h2>\n" +
            "    </div>\n" +
            "</header>\n" +
            "<div class=\"container content\">\n";

    private static final String RESOURCES_START = "<div class=\"intro\">%1$s</div><div class=\"resources\">";

    private static final String ANCHOR_TEMPLATE = "<a href=\"/%1$s\">/%2$s</a>";

    private static final String DESCRIPTION_TEMPLATE = "%1$s — %2$s";

    private static final String RESOURCE_TEMPLATE = "<h3>%1$s</h3>\n";

    private static final String LIST_START = "<ul>\n";

    private static final String METHOD_START = "<li> %1$s <strong>%2$s</strong>\n"
            + "    <ul>\n";

    private static final String METHOD_IO = "<li>%1$s: %2$s</li>\n";

    private static final String LIST_ITEM = "<li>%s</li>\n";

    private static final String METHOD_END = "    </ul>\n"
            + "</li>";

    private static final String LIST_END = "</ul>\n";

    private static final String RESOURCES_END = "</div>";

    private static final String STACKTRACE_DISPLAY_DIV = "<div id=\"stacktrace\"></div>";

    private static final String ERROR_STACK = "    <div id=\"original-stacktrace\" class=\"trace hidden\">\n" +
            "<p><em><a href=\"\" onClick=\"toggleStackTraceOrder(); return false;\">Click Here</a> " +
            "to see the stacktrace in reversed  order (root-cause first)</em></p>" +
            "        <pre>%1$s</pre>\n" +
            "    </div>\n";

    private static final String ERROR_STACK_REVERSED = "    <div id=\"reversed-stacktrace\" class=\"trace hidden\">\n" +
            "<p><em>The stacktrace below has been reversed to show the root cause first. " +
            "<a href=\"\" onClick=\"toggleStackTraceOrder(); return false;\">Click Here</a> " +
            "to see the original stacktrace</em></p>" +
            "        <pre>%1$s</pre>\n" +
            "    </div>\n";

    private static final String ERROR_CSS = "\n" +
            "html, body {\n" +
            "    margin: 0;\n" +
            "    padding: 0;\n" +
            "    font-family: 'Open Sans', Helvetica, Arial, sans-serif;\n" +
            "    font-size: 100%;\n" +
            "    font-weight: 100;\n" +
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
            ".container {\n" +
            "    width: 80%;\n" +
            "    margin: 0 auto;\n" +
            "}\n" +
            ".content {\n" +
            "    padding: 1em 0 1em 0;\n" +
            "}\n" +
            "\n" +
            "header, .component-name {\n" +
            "    background-color: #ad1c1c;\n" +
            "}\n" +
            "\n" +
            "ul {\n" +
            "    line-height: 1.5rem;\n" +
            "    margin: 0.25em 0 0.25em 0;\n" +
            "}\n" +
            "\n" +
            ".exception-message {\n" +
            "    background: #be2828;\n" +
            "}\n" +
            "\n" +
            "h1, h2 {\n" +
            "    margin: 0;\n" +
            "    padding: 0;\n" +
            "}\n" +
            "\n" +
            "h1 {\n" +
            "    font-size: 2rem;\n" +
            "    color: #fff;\n" +
            "    line-height: 3.75rem;\n" +
            "    font-weight: 700;\n" +
            "    padding: 0.4rem 0rem 0.4rem 0rem;\n" +
            "}\n" +
            "\n" +
            "h2 {\n" +
            "    font-size: 1.2rem;\n" +
            "    color: rgba(255, 255, 255, 0.85);\n" +
            "    line-height: 2.5rem;\n" +
            "    font-weight: 400;\n" +
            "    padding: 0.4rem 0rem 0.4rem 0rem;\n" +
            "}\n" +
            "\n" +
            ".intro {" +
            "    font-size: 1.2rem;\n" +
            "    font-weight: 400;\n" +
            "    margin: 0.25em 0 1em 0;\n" +
            "}\n" +
            "h3 {\n" +
            "    font-size: 1.2rem;\n" +
            "    line-height: 2.5rem;\n" +
            "    font-weight: 400;\n" +
            "    color: #555;\n" +
            "    margin: 0.25em 0 0.25em 0;\n" +
            "}\n" +
            "\n" +
            ".trace, .resources {\n" +
            "    background: #fff;\n" +
            "    padding: 15px;\n" +
            "    margin: 15px auto;\n" +
            "    border: 1px solid #ececec;\n" +
            "}\n" +
            ".trace {\n" +
            "    overflow-y: scroll;\n" +
            "}\n" +
            ".hidden {\n" +
            "   display: none;\n" +
            "}\n" +
            "\n" +
            "pre {\n" +
            "    white-space: pre;\n" +
            "    font-family: Consolas, Monaco, Menlo, \"Ubuntu Mono\", \"Liberation Mono\", monospace;\n" +
            "    font-size: 12px;\n" +
            "    line-height: 1.5;\n" +
            "    color: #555;\n" +
            "}\n";

    private static final String CONFIG_EDITOR_HEAD = "<h3>The following incorrect config values were detected:</h3>" +
            "<form method=\"post\" enctype=\"application/x-www-form-urlencoded\"  action=\"/io.quarkus.vertx-http.devmode.config.fix\">"
            +
            "<input type=\"hidden\" name=\"redirect\" value=\"%s\"/>\n" +
            "<table class=\"table table-striped\" cellspacing=\"20\">\n" +
            "    <thead class=\"thead-dark\">\n" +
            "    <tr>\n" +
            "        <th scope=\"col\">Config Key</th>\n" +
            "        <th scope=\"col\">Value</th>\n" +
            "    </tr>\n" +
            "    </thead>\n" +
            "    <tbody>\n";

    private static final String CONFIG_EDITOR_ROW = "    <tr style=\"padding:12px\">\n" +
            "            <td>\n" +
            "                %s\n" +
            "            </td>\n" +
            "            <td>\n" +
            "                <input type=\"text\" name=\"key.%s\" value=\"%s\"/>\n" +
            "            </td>\n" +
            "    </tr>\n";

    private static final String CONFIG_EDITOR_TAIL = "    </tbody>\n" +
            "</table>" +
            "<input type=\"submit\" value=\"Update\" >" +
            "</form>";
    private StringBuilder result;

    public TemplateHtmlBuilder(String title, String subTitle, String details) {
        this(title, subTitle, details, null, Collections.emptyList());
    }

    public TemplateHtmlBuilder(String title, String subTitle, String details, String redirect, List<CurrentConfig> config) {
        result = new StringBuilder(String.format(HTML_TEMPLATE_START, escapeHtml(title),
                subTitle == null || subTitle.isEmpty() ? "" : " - " + escapeHtml(subTitle), ERROR_CSS));
        result.append(String.format(HEADER_TEMPLATE, escapeHtml(title), escapeHtml(details)));
        if (!config.isEmpty()) {
            result.append(String.format(CONFIG_EDITOR_HEAD, redirect));
            for (CurrentConfig i : config) {
                result.append(String.format(CONFIG_EDITOR_ROW, escapeHtml(i.getPropertyName()), escapeHtml(i.getPropertyName()),
                        escapeHtml(i.getCurrentValue())));
            }
            result.append(CONFIG_EDITOR_TAIL);
        }
    }

    public TemplateHtmlBuilder stack(final Throwable throwable) {
        result.append(String.format(ERROR_STACK, escapeHtml(ExceptionUtil.generateStackTrace(throwable))));
        result.append(String.format(ERROR_STACK_REVERSED, escapeHtml(ExceptionUtil.rootCauseFirstStackTrace(throwable))));
        result.append(STACKTRACE_DISPLAY_DIV);
        return this;
    }

    public TemplateHtmlBuilder resourcesStart(String title) {
        result.append(String.format(RESOURCES_START, title));
        return this;
    }

    public TemplateHtmlBuilder resourcesEnd() {
        result.append(RESOURCES_END);
        return this;
    }

    public TemplateHtmlBuilder noResourcesFound() {
        result.append(String.format(RESOURCE_TEMPLATE, "No REST resources discovered"));
        return this;
    }

    public TemplateHtmlBuilder resourcePath(String title) {
        return resourcePath(title, true, false, null);
    }

    public TemplateHtmlBuilder staticResourcePath(String title) {
        return staticResourcePath(title, null);
    }

    public TemplateHtmlBuilder staticResourcePath(String title, String description) {
        return resourcePath(title, false, true, description);
    }

    public TemplateHtmlBuilder servletMapping(String title) {
        return resourcePath(title, false, false, null);
    }

    private TemplateHtmlBuilder resourcePath(String title, boolean withListStart, boolean withAnchor, String description) {
        String content;
        if (withAnchor) {
            if (title.startsWith("/")) {
                title = title.substring(1);
            }
            content = String.format(ANCHOR_TEMPLATE, title, escapeHtml(title));
        } else {
            content = escapeHtml(title);
        }
        if (description != null && !description.isEmpty()) {
            content = String.format(DESCRIPTION_TEMPLATE, content, description);
        }
        result.append(String.format(RESOURCE_TEMPLATE, content));
        if (withListStart) {
            result.append(LIST_START);
        }
        return this;
    }

    public TemplateHtmlBuilder method(String method, String fullPath) {
        result.append(String.format(METHOD_START, escapeHtml(method), escapeHtml(fullPath)));
        return this;
    }

    public TemplateHtmlBuilder consumes(String consumes) {
        result.append(String.format(METHOD_IO, "Consumes", escapeHtml(consumes)));
        return this;
    }

    public TemplateHtmlBuilder produces(String produces) {
        result.append(String.format(METHOD_IO, "Produces", escapeHtml(produces)));
        return this;
    }

    public TemplateHtmlBuilder listItem(String content) {
        result.append(String.format(LIST_ITEM, escapeHtml(content)));
        return this;
    }

    public TemplateHtmlBuilder methodEnd() {
        result.append(METHOD_END);
        return this;
    }

    public TemplateHtmlBuilder resourceStart() {
        result.append(LIST_START);
        return this;
    }

    public TemplateHtmlBuilder resourceEnd() {
        result.append(LIST_END);
        return this;
    }

    public TemplateHtmlBuilder append(String html) {
        result.append(html);
        return this;
    }

    @Override
    public String toString() {
        return result.append(HTML_TEMPLATE_END).toString();
    }

    private static String escapeHtml(final String bodyText) {
        if (bodyText == null) {
            return "";
        }

        return bodyText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    public static String adjustRoot(String httpRoot, String basePath) {
        //httpRoot can optionally end with a slash
        //also some templates want the returned path to start with a / and some don't
        //to make this work we check if the basePath starts with a / or not, and make sure we
        //the return value follows the same pattern

        if (httpRoot.equals("/")) {
            //leave it alone
            return basePath;
        }
        if (basePath.startsWith("/")) {
            if (!httpRoot.endsWith("/")) {
                return httpRoot + basePath;
            }
            return httpRoot.substring(0, httpRoot.length() - 1) + basePath;
        }
        if (httpRoot.endsWith("/")) {
            return httpRoot.substring(1) + basePath;
        }
        return httpRoot.substring(1) + "/" + basePath;
    }
}
