package io.quarkus.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import io.quarkus.dev.config.CurrentConfig;
import io.quarkus.runtime.logging.DecorateStackUtil;
import io.quarkus.runtime.util.ExceptionUtil;

public class TemplateHtmlBuilder {
    private static String CSS = null;

    private static final String SCRIPT_STACKTRACE_MANIPULATION = """
            <script>
                function toggleStackTraceOrder() {
                    var stElement = document.getElementById('stacktrace');
                    var current = stElement.getAttribute('data-current-setting');
                    if (current == 'original-stacktrace') {
                        var reverseOrder = document.getElementById('reversed-stacktrace');
                        stElement.innerHTML = reverseOrder.innerHTML;
                        stElement.setAttribute('data-current-setting', 'reversed-stacktrace');
                    } else {
                        var originalOrder = document.getElementById('original-stacktrace');
                        stElement.innerHTML = originalOrder.innerHTML;
                        stElement.setAttribute('data-current-setting', 'original-stacktrace');
                    }
                    return;
                }

                function showDefaultStackTraceOrder() {
                    var reverseOrder = document.getElementById('reversed-stacktrace');
                    var stacktrace = document.getElementById('stacktrace');

                    if (reverseOrder == null || stacktrace == null) {
                        return;
                    }
                    stacktrace.innerHTML = reverseOrder.innerHTML;
                    stacktrace.setAttribute('data-current-setting', 'reversed-stacktrace');

                    initStacktraceStyles(stacktrace);

                    return;
                }

                function copyStacktrace() {
                    const stacktrace = document.getElementById("stacktrace");
                    const content = stacktrace.getElementsByTagName("code")[0];
                    navigator.clipboard.writeText(content.textContent);
                }

                function initStacktraceStyles() {
                    document.querySelector("#stacktrace code > pre").setAttribute("data-viewmode", "wrap");
                    stacktrace.querySelector("#stacktrace code").classList.add("wrap-code");
                }

                function changeViewMode() {
                    const code = document.querySelector("div#stacktrace code");
                    const pre = code.querySelector("pre");
                    const currentViewMode = pre.getAttribute("data-viewmode");
                    if (currentViewMode === "scroll") {
                        code.classList.remove("scroll-code");
                        pre.classList.add("wrap-code");
                        pre.setAttribute("data-viewmode", "wrap");
                    } else {
                        pre.classList.remove("wrap-code");
                        code.classList.add("scroll-code");
                        pre.setAttribute("data-viewmode", "scroll");
                    }
                }
            </script>
            """;

    private static final String UTILITIES = """
                    <div id="utilities-container">
                        <p class="clipboard tooltip" onclick="changeViewMode();">
                            <span class="tooltip-text">Change view mode</span>
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><!--!Font Awesome Free 6.6.0 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free Copyright 2024 Fonticons, Inc.--><path fill="#555555" d="M0 96C0 78.3 14.3 64 32 64l384 0c17.7 0 32 14.3 32 32s-14.3 32-32 32L32 128C14.3 128 0 113.7 0 96zM64 256c0-17.7 14.3-32 32-32l384 0c17.7 0 32 14.3 32 32s-14.3 32-32 32L96 288c-17.7 0-32-14.3-32-32zM448 416c0 17.7-14.3 32-32 32L32 448c-17.7 0-32-14.3-32-32s14.3-32 32-32l384 0c17.7 0 32 14.3 32 32z"/></svg>
                        </p>
                        <p class="clipboard tooltip" onclick="copyStacktrace();">
                            <span class="tooltip-text">Copy stacktrace to clipboard</span>
                            <svg xmlns="http://www.w3.org/2000/svg"
                                viewBox="0 0 384 512"><!--!Font Awesome Free 6.6.0 by @fontawesome - https://fontawesome.com License - https://fontawesome.com/license/free Copyright 2024 Fonticons, Inc.-->
                                <path fill="#555555"
                                    d="M192 0c-41.8 0-77.4 26.7-90.5 64L64 64C28.7 64 0 92.7 0 128L0 448c0 35.3 28.7 64 64 64l256 0c35.3 0 64-28.7 64-64l0-320c0-35.3-28.7-64-64-64l-37.5 0C269.4 26.7 233.8 0 192 0zm0 64a32 32 0 1 1 0 64 32 32 0 1 1 0-64zM112 192l160 0c8.8 0 16 7.2 16 16s-7.2 16-16 16l-160 0c-8.8 0-16-7.2-16-16s7.2-16 16-16z" />
                            </svg>
                        </p>
                    </div>
            """;

    private static final String HTML_TEMPLATE_START_NO_STACK = "" +
            "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <title>%1$s%2$s</title>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "</head>";

    private static final String HTML_TEMPLATE_START = "" +
            "<!doctype html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <title>%1$s%2$s</title>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "    <style>%3$s</style>\n" +
            SCRIPT_STACKTRACE_MANIPULATION +
            "</head>\n" +
            "<body  onload=\"showDefaultStackTraceOrder();\">\n" +
            "<div class=\"header\">\n" +
            "   <svg id=\"quarkus-logo-svg\" data-name=\"Quarkus Logo\" xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 1280 195\">\n"
            +
            "       <defs>\n" +
            "           <style>\n" +
            "             .cls-1 {\n" +
            "                   fill: #fff;\n" +
            "               }\n" +
            "               .cls-2 {\n" +
            "                   fill: #4695eb;\n" +
            "               }\n" +
            "               .cls-3 {\n" +
            "                   fill: #ff004a;\n" +
            "               }\n" +
            "           </style>\n" +
            "       </defs>\n" +
            "       <title>quarkus_logo_horizontal_rgb_1280px_reverse</title>\n" +
            "       <path class=\"cls-1\"\n" +
            "           d=\"M404,89.39q0,25.9-10.49,43.07a52.31,52.31,0,0,1-29.61,23.24l32.3,33.39H373.29l-26.45-30.44-5.11.19q-30,0-46.27-18.22T279.17,89.2q0-32.93,16.34-51.05T341.92,20q29.24,0,45.66,18.45T404,89.39Zm-108.14,0q0,27.39,11.74,41.55t34.11,14.15q22.56,0,34.07-14.11t11.51-41.59q0-27.21-11.47-41.27T341.92,34.05q-22.56,0-34.3,14.16T295.88,89.39Z\"/>\n"
            +
            "       <path class=\"cls-1\"\n" +
            "           d=\"M559.56,22.15V109.4q0,23.05-14,36.25T507,158.84q-24.51,0-37.92-13.29T455.7,109V22.15h15.78v88q0,16.88,9.28,25.91t27.29,9q17.16,0,26.45-9.08T543.78,110V22.15Z\"/>\n"
            +
            "       <path class=\"cls-1\"\n" +
            "           d=\"M703.6,157,686.7,114.1H632.31L615.6,157h-16L653.29,21.6h13.27L719.93,157Zm-21.82-57L666,58.21q-3.07-7.92-6.32-19.46a172.55,172.55,0,0,1-5.85,19.46l-16,41.78Z\"/>\n"
            +
            "       <path class=\"cls-1\"\n" +
            "           d=\"M777.19,100.92V157H761.4V22.15h37.23q25,0,36.89,9.5t11.93,28.59q0,26.74-27.29,36.16L857,157H838.35l-32.86-56.07Zm0-13.47h21.62q16.71,0,24.51-6.6t7.79-19.78q0-13.37-7.93-19.27T797.7,35.89H777.19Z\"/>\n"
            +
            "       <path class=\"cls-1\"\n" +
            "           d=\"M999.75,157H981.18L931.71,91.6l-14.2,12.54V157H901.73V22.15h15.78V89l61.54-66.87h18.66L943.13,80.72Z\"/>\n"
            +
            "       <path class=\"cls-1\"\n" +
            "           d=\"M1143.69,22.15V109.4q0,23.05-14,36.25t-38.52,13.19q-24.5,0-37.92-13.29T1039.82,109V22.15h15.78v88q0,16.88,9.28,25.91t27.29,9q17.17,0,26.46-9.08t9.28-26.06V22.15Z\"/>\n"
            +
            "       <path class=\"cls-1\"\n" +
            "           d=\"M1279,121.11q0,17.8-13,27.76t-35.28,10q-24.13,0-37.13-6.18V137.53a97.82,97.82,0,0,0,18.2,5.53,95.67,95.67,0,0,0,19.49,2q15.78,0,23.76-6t8-16.55q0-7-2.83-11.48t-9.46-8.26q-6.65-3.78-20.19-8.58-18.94-6.72-27.06-16t-8.12-24.07q0-15.58,11.79-24.81t31.18-9.22a92.55,92.55,0,0,1,37.22,7.37l-4.91,13.65q-16.82-7-32.68-7-12.53,0-19.58,5.35t-7.06,14.85q0,7,2.6,11.48t8.77,8.21q6.18,3.73,18.89,8.26,21.36,7.56,29.38,16.23T1279,121.11Z\"/>\n"
            +
            "       <polygon class=\"cls-2\" points=\"126.05 34.6 96.61 51.59 126.05 68.59 126.05 34.6\"/>\n" +
            "       <polygon class=\"cls-3\" points=\"67.17 34.6 67.17 68.59 96.61 51.59 67.17 34.6\"/>\n" +
            "       <polygon class=\"cls-1\" points=\"126.05 68.59 96.61 51.59 67.17 68.59 96.61 85.59 126.05 68.59\"/>\n" +
            "       <polygon class=\"cls-2\" points=\"36.13 88.36 65.57 105.36 65.57 71.37 36.13 88.36\"/>\n" +
            "       <polygon class=\"cls-3\" points=\"65.57 139.35 95.01 122.36 65.57 105.36 65.57 139.35\"/>\n" +
            "       <polygon class=\"cls-1\" points=\"65.57 71.37 65.57 105.36 95.01 122.36 95.01 88.36 65.57 71.37\"/>\n" +
            "       <polygon class=\"cls-2\" points=\"127.65 139.35 127.65 105.36 98.21 122.36 127.65 139.35\"/>\n" +
            "       <polygon class=\"cls-3\" points=\"157.09 88.36 127.65 71.37 127.65 105.36 157.09 88.36\"/>\n" +
            "       <polygon class=\"cls-1\" points=\"98.21 122.36 127.65 105.36 127.65 71.37 98.21 88.36 98.21 122.36\"/>\n" +
            "       <path class=\"cls-2\"\n" +
            "           d=\"M160.5,1H32.72A31.81,31.81,0,0,0,1,32.72V160.5a31.81,31.81,0,0,0,31.72,31.72h87.51L96.61,134.85,79.48,171.07H32.72A10.71,10.71,0,0,1,22.15,160.5V32.72A10.71,10.71,0,0,1,32.72,22.15H160.5a10.71,10.71,0,0,1,10.57,10.57V160.5a10.71,10.71,0,0,1-10.57,10.57H132.77l8.71,21.15h19a31.81,31.81,0,0,0,31.72-31.72V32.72A31.81,31.81,0,0,0,160.5,1Z\"/>\n"
            +
            "   </svg>\n" +
            "</div> ";

    private static final String HTML_TEMPLATE_END = "</div></body>\n" +
            "</html>\n";

    private static final String HEADER_TEMPLATE = "<div class=\"banner\">\n" +
            "                            <div class=\"callout\">%1$s</div>\n" +
            "                        </div>"
            + ""
            + ""
            + "<header>\n" +
            "    <div class=\"exception-message\">\n" +
            "        <h2 class=\"container\">%2$s</h2>\n" +
            "        <div class=\"actions\">%3$s</div>\n" +
            "    </div>\n" +
            "</header>\n" +
            "<div class=\"container content\">\n";

    private static final String HEADER_TEMPLATE_NO_STACK = "<h1>%1$s</h1>\n" +
            "%2$s \n" +
            "<div class=\"container content\">\n";

    private static final String RESOURCES_START = "<div class=\"intro\">%1$s</div><div class=\"resources\">";

    private static final String ANCHOR_TEMPLATE_ABSOLUTE = "<a href=\"%1$s\">%2$s</a>";

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

    private static final String BRSTI = "___begin_relative_stack_trace_item___";
    private static final String ERSTI = "___end_relative_stack_trace_item___";

    private static final String OPEN_IDE_LINK = "<div class='rel-stacktrace-item' onclick=\"event.preventDefault(); fetch('/q/open-in-ide/%s/%s/%d');\">";

    private static final String ORIGINAL_STACK_TRACE = "    <div id=\"original-stacktrace\" class=\"trace hidden\">\n" +
            "<h3>The stacktrace below is the original. " +
            "<a href=\"\" onClick=\"toggleStackTraceOrder(); return false;\">See the stacktrace in reversed order</a> (root-cause first)</h3>"
            +
            "        <code class=\"stacktrace\"><pre>%1$s</pre></code>\n" +
            "    </div>\n";

    private static final String ERROR_STACK_REVERSED = "    <div id=\"reversed-stacktrace\" class=\"trace hidden\">\n" +
            "<h3>The stacktrace below has been reversed to show the root cause first. " +
            "<a href=\"\" onClick=\"toggleStackTraceOrder(); return false;\">See the original stacktrace</a></h3>" +
            "        <code class=\"stacktrace\"><pre>%1$s</pre></code>\n" +
            "    </div>\n";

    private static final String DECORATE_DIV = "<pre class='decorate'>%s</pre>";
    private static final String CONFIG_EDITOR_HEAD = "<h3>The following incorrect config values were detected:</h3>" +
            "<form class=\"updateConfigForm\" method=\"post\" enctype=\"application/x-www-form-urlencoded\"  action=\"/io.quarkus.vertx-http.devmode.config.fix\">"
            + "<input type=\"hidden\" name=\"redirect\" value=\"%s\"/>\n";

    private static final String CONFIG_EDITOR_ROW = "<code class=\"configKey\">%s\n</code>\n" +
            "                <input class=\"configValue\" type=\"text\" name=\"key.%s\" value=\"%s\"/>\n";

    private static final String CONFIG_UPDATE_BUTTON = "<input class=\"cta-button\" type=\"submit\" value=\"Update\" >";

    private static final String CONFIG_EDITOR_TAIL = "    </tbody>\n" +
            "</table>" +
            CONFIG_UPDATE_BUTTON +
            "</form>";
    private StringBuilder result;

    private String baseUrl;

    public TemplateHtmlBuilder(String title, String subTitle, String details) {
        this(true, null, title, subTitle, details, Collections.emptyList(), null, Collections.emptyList());
    }

    public TemplateHtmlBuilder(boolean showStack, String title, String subTitle, String details,
            List<ErrorPageAction> actions) {
        this(showStack, null, title, subTitle, details, actions, null, Collections.emptyList());
    }

    public TemplateHtmlBuilder(String title, String subTitle, String details,
            List<ErrorPageAction> actions) {
        this(true, null, title, subTitle, details, actions, null, Collections.emptyList());
    }

    public TemplateHtmlBuilder(String baseUrl, String title, String subTitle, String details,
            List<ErrorPageAction> actions) {
        this(true, baseUrl, title, subTitle, details, actions, null, Collections.emptyList());
    }

    public TemplateHtmlBuilder(String title, String subTitle, String details, List<ErrorPageAction> actions,
            String redirect,
            List<CurrentConfig> config) {
        this(true, null, title, subTitle, details, actions, null, Collections.emptyList());
    }

    public TemplateHtmlBuilder(boolean showStack, String baseUrl, String title, String subTitle, String details,
            List<ErrorPageAction> actions,
            String redirect,
            List<CurrentConfig> config) {
        this.baseUrl = baseUrl;
        StringBuilder actionLinks = new StringBuilder();

        if (showStack) {
            loadCssFile();
            for (ErrorPageAction epa : actions) {
                actionLinks.append(buildLink(epa.name(), epa.url()));
            }

            result = new StringBuilder(String.format(HTML_TEMPLATE_START, escapeHtml(title),
                    subTitle == null || subTitle.isEmpty() ? "" : " - " + escapeHtml(subTitle), CSS));
            result.append(String.format(HEADER_TEMPLATE, escapeHtml(title), escapeHtml(details), actionLinks.toString()));
        } else {
            result = new StringBuilder(String.format(HTML_TEMPLATE_START_NO_STACK, escapeHtml(title),
                    subTitle == null || subTitle.isEmpty() ? "" : " - " + escapeHtml(subTitle), CSS));
            result.append(
                    String.format(HEADER_TEMPLATE_NO_STACK, escapeHtml(title), escapeHtml(details), actionLinks.toString()));
        }

        if (!config.isEmpty()) {
            result.append(String.format(CONFIG_EDITOR_HEAD, redirect));
            for (CurrentConfig i : config) {
                result.append(String.format(CONFIG_EDITOR_ROW, escapeHtml(i.getPropertyName()), escapeHtml(i.getPropertyName()),
                        escapeHtml(i.getCurrentValue())));
            }
            result.append(CONFIG_EDITOR_TAIL);
        }
    }

    public TemplateHtmlBuilder decorate(final Throwable throwable, String srcMainJava, List<String> knowClasses) {
        String decoratedString = DecorateStackUtil.getDecoratedString(throwable, srcMainJava, knowClasses);
        if (decoratedString != null) {
            result.append(String.format(DECORATE_DIV, decoratedString));
        }

        return this;
    }

    public TemplateHtmlBuilder stack(final Throwable throwable) {
        return stack(throwable, List.of());
    }

    public TemplateHtmlBuilder stack(final Throwable throwable, List<String> knowClasses) {
        if (knowClasses != null && throwable != null) {
            StackTraceElement[] originalStackTrace = Arrays.copyOf(throwable.getStackTrace(), throwable.getStackTrace().length);
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            String className = "";
            String type = "java"; //default
            int lineNumber = 0;
            if (!knowClasses.isEmpty()) {

                for (int i = 0; i < stackTrace.length; ++i) {
                    var elem = stackTrace[i];
                    if (knowClasses.contains(elem.getClassName())) {
                        className = elem.getClassName();
                        String filename = elem.getFileName();
                        if (filename != null) {
                            int dotindex = filename.lastIndexOf(".");
                            type = elem.getFileName().substring(dotindex + 1);
                        }
                        lineNumber = elem.getLineNumber();

                        stackTrace[i] = new StackTraceElement(elem.getClassLoaderName(), elem.getModuleName(),
                                elem.getModuleVersion(),
                                BRSTI + elem.getClassName()
                                        + ERSTI,
                                elem.getMethodName(), elem.getFileName(), elem.getLineNumber());
                    }
                }
            }
            throwable.setStackTrace(stackTrace);

            String original = escapeHtml(ExceptionUtil.generateStackTrace(throwable));
            String rootFirst = escapeHtml(ExceptionUtil.rootCauseFirstStackTrace(throwable));
            if (original.contains(BRSTI)) {
                original = original.replace(BRSTI,
                        String.format(OPEN_IDE_LINK, className, type, lineNumber));
                original = original.replace(ERSTI, "</div>");
                rootFirst = rootFirst.replace(BRSTI,
                        String.format(OPEN_IDE_LINK, className, type, lineNumber));
                rootFirst = rootFirst.replace(ERSTI, "</div>");
            }

            result.append(UTILITIES);
            result.append(String.format(ORIGINAL_STACK_TRACE, original));
            result.append(String.format(ERROR_STACK_REVERSED, rootFirst));
            result.append(STACKTRACE_DISPLAY_DIV);

            throwable.setStackTrace(originalStackTrace);
        }
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
        result.append(String.format(RESOURCE_TEMPLATE, "No resources discovered"));
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
        return resourcePath(title, false, true, null);
    }

    private TemplateHtmlBuilder resourcePath(String title, boolean withListStart, boolean withAnchor, String description) {
        String content;
        if (withAnchor) {
            String text = title;
            if (title.startsWith("/")) {
                title = title.substring(1);
            }

            if (!title.startsWith("http") && baseUrl != null) {
                title = baseUrl + title;
            }
            if (title.startsWith("http")) {
                int firstSlashIndex = title.indexOf("/", title.indexOf("//") + 2);
                text = title.substring(firstSlashIndex);
            }

            content = String.format(ANCHOR_TEMPLATE_ABSOLUTE, title, escapeHtml(text));
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
        fullPath = escapeHtml(fullPath);
        if (method.equalsIgnoreCase("GET")) {
            if (baseUrl != null) {
                fullPath = "<a href='" + baseUrl + fullPath.substring(1) + "' target='_blank'>" + fullPath + "</a>";
            } else {
                fullPath = "<a href='" + fullPath + "' target='_blank'>" + fullPath + "</a>";
            }
        }
        result.append(String.format(METHOD_START, escapeHtml(method), fullPath));
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
        //return the value that follows the same pattern

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

    public void loadCssFile() {
        if (CSS == null) {
            ClassLoader classLoader = getClass().getClassLoader();
            String cssFilePath = "META-INF/template-html-builder.css";
            try (InputStream inputStream = classLoader.getResourceAsStream(cssFilePath)) {
                if (inputStream != null) {
                    // Use a Scanner to read the contents of the CSS file
                    try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                        StringBuilder stringBuilder = new StringBuilder();
                        while (scanner.hasNextLine()) {
                            stringBuilder.append(scanner.nextLine()).append("\n");
                        }
                        CSS = stringBuilder.toString();
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private String buildLink(String name, String url) {
        return "<a href=" + url + ">" + name + "</a>";
    }
}
