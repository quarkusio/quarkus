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

package io.quarkus.undertow.devmode;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * generates a error page with a stack trace
 *
 * @author Stuart Douglas
 */
public class ReplacementDebugPage {

    public static final String ERROR_CSS = "<style>\n" +
            "body {\n" +
            "    font-family: \"Lucida Grande\", \"Lucida Sans Unicode\", \"Trebuchet MS\", Helvetica, Arial, Verdana, sans-serif;\n"
            +
            "    margin: 5px;\n" +
            "}\n" +
            "\n" +
            ".header {\n" +
            "    background-image: linear-gradient(bottom, rgb(153,151,153) 8%, rgb(199,199,199) 54%);\n" +
            "    background-image: -o-linear-gradient(bottom, rgb(153,151,153) 8%, rgb(199,199,199) 54%);\n" +
            "    background-image: -moz-linear-gradient(bottom, rgb(153,151,153) 8%, rgb(199,199,199) 54%);\n" +
            "    background-image: -webkit-linear-gradient(bottom, rgb(153,151,153) 8%, rgb(199,199,199) 54%);\n" +
            "    background-image: -ms-linear-gradient(bottom, rgb(153,151,153) 8%, rgb(199,199,199) 54%);\n" +
            "    \n" +
            "    background-image: -webkit-gradient(\n" +
            "        linear,\n" +
            "        left bottom,\n" +
            "        left top,\n" +
            "        color-stop(0.08, rgb(153,151,153)),\n" +
            "        color-stop(0.54, rgb(199,199,199))\n" +
            "    );\n" +
            "    color: black;\n" +
            "    padding: 2px;\n" +
            "    font-weight: normal;\n" +
            "    border: solid 1px;\n" +
            "    font-size: 170%;\n" +
            "    text-align: left;\n" +
            "    vertical-align: middle; \n" +
            "    height: 32px; \n" +
            "    margin-bottom: 10px;\n" +
            "}\n" +
            ".error-div {\n" +
            "    display: inline-block;\n" +
            "    width: 32px;\n" +
            "    height: 32px;\n" +
            "    background: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAABmJLR0QAAAAAAAD5Q7t/AAAACXBIWXMAAABIAAAASABGyWs+AAAACXZwQWcAAAAgAAAAIACH+pydAAAGGElEQVRYw8WXW2wcVxnHf+fM7NXrdXbdtZ3ipGqCEzvQKmlFoUggwkvvGBoaqVVV5Y0nkCoQPPCAhMQbQlzUh/IWHoh6QUVKZVLxUCGogKZRW9uJkyg0iVzZ2dhre9c7u7M7c87hYWc2M7t2bBASI306O/85+v7/7zLn24H/8yV2u/EcFNvwVNK2X0DKw1qpYaX1gCWlY0m5arRe8JQ6I2DmaVj/nwk4CxO2Zf1aw9dKo6N67+eOZHN7x0gViiSHBmlXa7hrazjLt1i+eKmxUi5bwpg/e8a8PA3X/msBr0MyJ+WvgFOffehY8v7HH5OW78HqGjgOtNrge2AnIJWAgQEYLqBsm3/NvKM/mZ1rYczpRa2/9x3w/iMBM1BCiHfuGRmZevClF9NJ5cP1m9B0+/aa3t/ZDNw3TtuSfPy737trK6sXtTGPPwOruxLwNhSEEHMHpqZGDz93wubKFdio3ZXUGNOHs2cIDh/g6pt/9G8uXLnlGPP5k1Dt5bN60m6lhXj34OTkxKET37SZm4d6oz/KgFRH7yOrBozbwlTWGf7ql6RZrWTcytrxaTj9Ro9OGb3JS/mz0nDxgYnnvmWbuUsdJ6FTY2JmImShqZA83N90UbOXOXDiG4nCcPFoTsqfbluCP8DenJQLx1/+7pBcXILaZl9qo2svFmalb48xkM9hxkf5+29+u9HSenIayn0ZyEr5i0NHpnKy7aGrtW6UehsLozRBRlRQEh3BwzLpWh3h++yfPJRLSvnzvhK8DkPamOl7n3nCUjcXtyXuK0X0WViaAFc9QtSNJe594uu2MubZGcjHBGTgqeFiQQml0K12nDgSpd4iyi4uRMci6Q8xJSWq7SGEZCg/qD14LCYgIcTzoxMHc6qygYk40aETKTFSdh2aANOWhbEsjJSYCK6C/V0LcH+9xvCB/TlbiBdCAXbQMJPZ/fvQjUbXEULAa6/1deyuh0dwNU+e7DSzEOhmi8xn9iI+ujgVE6ChlBwpYcqVjuIgA3IXBMYYtNbd30IIpJQI0ZEa+gPQnk9ieA8GSrESaGNyiWIBrVQntUHqdrq01iiluo0WilBK4fs+WutOr4S9YAyJoRzamMGYAClEve046ETyTr13EKCU6kZ+N4GNCxfwKpVOryQSuOUVhBCbsRIIuN1cWRkaSCfRrdaOTncijgltNPDn5xHZLIn7x3GNh4CVmADgcv3a9YmBY0cxm06nAQH31KlOao3BL5fxbtzAOE73hAt7JXb6RU7PGO44WK5Do1ZHw6WYAN+YM+X5S8dLXziWizVNvY5aWcFfWuq8Idsctb1Yr8DwmRwrUr1yfVMbcyYmwIWZ2mbdai9+gvfBAsaAbrUwrrtllLuJPibIGGQ2hUlIHLdlA+diTXgSqkKIt5b/OevL+8bwqlWU63anW3jyhaa4c9T2Hs0hrsL5EOCJQ/sov7/gS8Mb07AZEwDgav2D5aVy3eSziEL+Dllk0PSRcmcMqx6BUeGykINsksrttboPP4w2aVfACVhGiFevvft+I/XwJCad3DF6tc2MiD4zmRSZhyZY/OusI4V4JTqKYwIA6lr/2Gm6s5/+7UMv/egDmEyqL7VbRR/LViR6sikGvniYpffm2m6r/VFd65/0vqZ9R/tbsMeG+WKpMDL+lWMJ58Jl/NVqvOG26PAoDmCXhhg4epDl9+a9zUp1uQkPbvWfcMvZchbuEXAuk0kf2X/84YxpuDQv3kA5zW1fw9CswSyZyXFEJsmnf/m46bntuTY8+SxUtuLadri9CokxeEUK8WJhtJgaeeSIxG3RurWGv+Gg3RbaU5CwkOkU1lCWxFgBmbS4ff6qX7297rXh9Gn4/llobMcjtsFywACQ+zZMTsOP8vBIOpNS+bFiJj2cxx7MYg+k8ZwmrWqD9lqNjVvrTc9tWRtw/k345dtwFagDTmTdUUA2EDAYXffB2JPw6FH4ch7GUpCzIemD50J9A1Y+hH/8Cc4vdTq9Tud9D9dNOt+MajclyNL53xZmIhusmcBSQJLOd4UJnHqACzTppDy0kHizl/yuPRB5nggIM0A6IE4GuA34EQFtoBUQuwGm7kbwb+eaEEXmuV5dAAAAJXRFWHRjcmVhdGUtZGF0ZQAyMDA5LTExLTEwVDE5OjM4OjI0LTA3OjAwdDKp4gAAACV0RVh0ZGF0ZTpjcmVhdGUAMjAxMC0wMi0yMFQyMzoyNjoyNC0wNzowMC7DUNYAAAAldEVYdGRhdGU6bW9kaWZ5ADIwMTAtMDEtMTFUMDg6NTc6MzUtMDc6MDCruapPAAAAMnRFWHRMaWNlbnNlAGh0dHA6Ly9lbi53aWtpcGVkaWEub3JnL3dpa2kvUHVibGljX2RvbWFpbj/96s8AAAAldEVYdG1vZGlmeS1kYXRlADIwMDktMTEtMTBUMTk6Mzg6MjQtMDc6MDArg9/WAAAAGXRFWHRTb3VyY2UAVGFuZ28gSWNvbiBMaWJyYXJ5VM/tggAAADp0RVh0U291cmNlX1VSTABodHRwOi8vdGFuZ28uZnJlZWRlc2t0b3Aub3JnL1RhbmdvX0ljb25fTGlicmFyebzIrdYAAAAASUVORK5CYII=') left center no-repeat;\n"
            +
            "}\n" +
            ".error-text-div {\n" +
            "    display: inline-block;\n" +
            "    vertical-align: top;\n" +
            "    height: 32px;\n" +
            "}\n" +
            ".label {\n" +
            "    font-weight:bold;\n" +
            "    display: inline-block;\n" +
            "}\n" +
            ".value {\n" +
            "    display: inline-block;\n" +
            "    margin-left: 5px;\n" +
            "}\n" +
            "pre {\n" +
            "    font-size: 110%;\n" +
            "    margin-left: 1.5em;\n" +
            "    white-space: pre-wrap;\n" +
            "    white-space: -moz-pre-wrap;\n" +
            "    white-space: -pre-wrap;\n" +
            "    white-space: -o-pre-wrap;\n" +
            "    word-wrap: break-word;\n" +
            "}\n" +
            "</style>";

    public static void handleRequest(HttpServerExchange exchange, final Throwable exception) throws IOException {
        StringBuilder sb = new StringBuilder();
        //todo: make this good
        sb.append("<html><head><title>ERROR</title>");
        sb.append(ERROR_CSS);
        sb.append(
                "</head><body><div class=\"header\"><div class=\"error-div\"></div><div class=\"error-text-div\">Error Restarting Quarkus</div></div>");
        writeLabel(sb, "Stack Trace", "");

        sb.append("<br/><textarea readonly style=\"width:90%; height:80%\">");
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        sb.append(escapeBodyText(stringWriter.toString()));
        sb.append("</textarea></body></html>");
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8");
        exchange.getResponseSender().send(sb.toString());
    }

    private static void writeLabel(StringBuilder sb, String label, String value) {
        sb.append("<div class=\"label\">");
        sb.append(escapeBodyText(label));
        sb.append(":</div><div class=\"value\">");
        sb.append(escapeBodyText(value));
        sb.append("</div><br/>");
    }

    public static String escapeBodyText(final String bodyText) {
        if (bodyText == null) {
            return "null";
        }
        return bodyText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
