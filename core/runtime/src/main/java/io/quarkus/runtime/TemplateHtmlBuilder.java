/**
 * 
 */
package io.quarkus.runtime;

/**
 * 
 */
public class TemplateHtmlBuilder {

    private static final String HTML_TEMPLATE_START = "" +
            "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <title>Internal Server Error</title>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <style>%1$s</style>\n" +
            "</head>\n" +
            "<body><div class=\"content\">\n";

    private static final String HTML_TEMPLATE_END = "</div></body>\n" +
            "</html>\n";

    private static final String ERROR_DETAILS = "<header>\n" +
            "    <h1 class=\"container\">Internal Server Error</h1>\n" +
            "    <div class=\"exception-message\">\n" +
            "        <h2 class=\"container\">%1$s</h2>\n" +
            "    </div>\n" +
            "</header>\n";

    private static final String HEADER_TEMPLATE = "<div class=\"component-name\"><h1>%1$s</h1><h2>%2$s</h2></div>";

    private static final String RESOURCE_TEMPLATE = "<h2>%1$s</h2>\n"
            + "<ul>";

    private static final String METHOD_START = "<li> %1$s <strong>%2$s</strong>\n"
            + "    <ul>\n";

    private static final String METHOD_IO = "<li>%1$s: %2$s</li>\n";

    private static final String METHOD_END = "    </ul>\n"
            + "</li>";

    private static final String LIST_END = "</ul>\n";

    private static final String ERROR_STACK = "<div class=\"container\">\n" +
            "    <div class=\"trace\">\n" +
            "        <pre>%1$s</pre>\n" +
            "    </div>\n" +
            "</div>\n";

    private static final String ERROR_CSS = "\n" +
            "html, body {\n" +
            "    margin: 0;\n" +
            "    padding: 0;\n" +
            "    font-family: 'Open Sans', Arial, sans-serif;\n" +
            "    font-size: 100%;\n" +
            "    font-weight: 100;\n" +
            "    line-height: 1.4;\n" +
            "    background-color:#000c16;\n" +
            "}\n" +
            "\n" +
            "div.content {\n" +
            "    padding:0 13rem;\n" +
            "}\n" +
            "\n" +
            "html {\n" +
            "    overflow-y: scroll;\n" +
            "}\n" +
            "\n" +
            "body {\n" +
            "    background: #000c16;\n" +
            "}\n" +
            "\n" +
            ".container {\n" +
            "    width: 80%;\n" +
            "    margin: 0 auto;\n" +
            "}\n" +
            "\n" +
            "header, .component-name {\n" +
            "    background-color: #004153;\n" +
            "}\n" +
            "\n" +
            ".component-name h1 {\n" +
            "    margin: 1rem;\n" +
            "}\n" +
            "\n" +
            ".component-name h2 {\n" +
            "    margin: 0.75rem;\n" +
            "}\n" +
            "\n" +
            "ul {\n" +
            "    line-height: 2rem;\n" +
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
            "    font-size: 3rem;\n" +
            "    color: #fff;\n" +
            "    line-height: 3.75rem;\n" +
            "    font-weight: 700;\n" +
            "}\n" +
            "\n" +
            "h2 {\n" +
            "    font-size: 2rem;\n" +
            "    color: rgba(255, 255, 255, 0.85);\n" +
            "    line-height: 2.5rem;\n" +
            "    font-weight: 400;\n" +
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
            "}\n" +
            "\n" +
            "* {\n" +
            "    font-family:'Open Sans', Arial, sans-serif;\n" +
            "    color:#fff;\n" +
            "}\n";

    private StringBuilder result = new StringBuilder(String.format(HTML_TEMPLATE_START, ERROR_CSS));

    public TemplateHtmlBuilder error(String details) {
        result.append(String.format(ERROR_DETAILS, details));
        return this;
    }

    public TemplateHtmlBuilder stack(String stack) {
        result.append(String.format(ERROR_STACK, stack));
        return this;
    }

    public TemplateHtmlBuilder header(String title, String subTitle) {
        result.append(String.format(HEADER_TEMPLATE, title, subTitle));
        return this;
    }

    public TemplateHtmlBuilder resourcePath(String title) {
        result.append(String.format(RESOURCE_TEMPLATE, title));
        return this;
    }

    public TemplateHtmlBuilder method(String method, String fullPath) {
        result.append(String.format(METHOD_START, method, fullPath));
        return this;
    }

    public TemplateHtmlBuilder consumes(String consumes) {
        result.append(String.format(METHOD_IO, "Consumes", consumes));
        return this;
    }

    public TemplateHtmlBuilder produces(String produces) {
        result.append(String.format(METHOD_IO, "Produces", produces));
        return this;
    }

    public TemplateHtmlBuilder methodEnd() {
        result.append(METHOD_END);
        return this;
    }

    public TemplateHtmlBuilder resourceEnd() {
        result.append(LIST_END);
        return this;
    }

    public String toString() {
        return result.append(HTML_TEMPLATE_END).toString();
    }

}
