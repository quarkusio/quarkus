/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.quarkus.vertx.http.runtime.filters.accesslog;

import java.util.Collections;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.vertx.http.runtime.attribute.ExchangeAttribute;
import io.quarkus.vertx.http.runtime.attribute.ExchangeAttributeParser;
import io.quarkus.vertx.http.runtime.attribute.SubstituteEmptyWrapper;
import io.quarkus.vertx.http.runtime.filters.QuarkusRequestWrapper;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Access log handler. This handler will generate access log messages based on the provided format string,
 * and pass these messages into the provided {@link AccessLogReceiver}.
 * <p>
 * This handler can log any attribute that is provides via the {@link ExchangeAttribute}
 * mechanism. A general guide to the most common attribute is provided before, however this mechanism is extensible.
 * <p>
 * <p>
 * <p>
 * This factory produces token handlers for the following patterns
 * </p>
 * <ul>
 * <li><b>%a</b> - Remote IP address
 * <li><b>%A</b> - Local IP address
 * <li><b>%b</b> - Bytes sent, excluding HTTP headers, or '-' if no bytes
 * were sent
 * <li><b>%B</b> - Bytes sent, excluding HTTP headers
 * <li><b>%h</b> - Remote host name
 * <li><b>%H</b> - Request getProtocol
 * <li><b>%l</b> - Remote logical username from identd (always returns '-')
 * <li><b>%m</b> - Request method
 * <li><b>%p</b> - Local port
 * <li><b>%q</b> - Query string (excluding the '?' character)
 * <li><b>%r</b> - First line of the request
 * <li><b>%s</b> - HTTP status code of the response
 * <li><b>%t</b> - Date and time, in Common Log Format format
 * <li><b>%u</b> - Remote user that was authenticated
 * <li><b>%U</b> - Requested URL path
 * <li><b>%v</b> - Local server name
 * <li><b>%D</b> - Time taken to process the request, in millis
 * <li><b>%T</b> - Time taken to process the request, in seconds
 * <li><b>%I</b> - current Request thread name (can compare later with stacktraces)
 * </ul>
 * <p>
 * In addition, the caller can specify one of the following aliases for
 * commonly utilized patterns:
 * </p>
 * <ul>
 * <li><b>common</b> - <code>%h %l %u %t "%r" %s %b</code>
 * <li><b>combined</b> -
 * <code>%h %l %u %t "%r" %s %b "%{i,Referer}" "%{i,User-Agent}"</code>
 * </ul>
 * <p>
 * <p>
 * There is also support to write information from the cookie, incoming
 * header, or the session<br>
 * It is modeled after the apache syntax:
 * <ul>
 * <li><code>%{i,xxx}</code> for incoming headers
 * <li><code>%{o,xxx}</code> for outgoing response headers
 * <li><code>%{c,xxx}</code> for a specific cookie
 * <li><code>%{r,xxx}</code> xxx is an attribute in the ServletRequest
 * <li><code>%{s,xxx}</code> xxx is an attribute in the HttpSession
 * </ul>
 *
 * @author Stuart Douglas
 */
public class AccessLogHandler implements Handler<RoutingContext> {

    private final AccessLogReceiver accessLogReceiver;
    private final String formatString;
    private final ExchangeAttribute tokens;
    private final Pattern excludePattern;

    public AccessLogHandler(final AccessLogReceiver accessLogReceiver, final String formatString, ClassLoader classLoader,
            Optional<String> excludePattern) {
        this.accessLogReceiver = accessLogReceiver;
        this.formatString = handleCommonNames(formatString);
        this.tokens = new ExchangeAttributeParser(classLoader, Collections.singletonList(new SubstituteEmptyWrapper("-")))
                .parse(this.formatString);
        if (excludePattern.isPresent()) {
            this.excludePattern = Pattern.compile(excludePattern.get());
        } else {
            this.excludePattern = null;
        }
    }

    public AccessLogHandler(final AccessLogReceiver accessLogReceiver, String formatString, final ExchangeAttribute attribute) {
        this.accessLogReceiver = accessLogReceiver;
        this.formatString = handleCommonNames(formatString);
        this.tokens = attribute;
        this.excludePattern = null;
    }

    private static String handleCommonNames(String formatString) {
        switch (formatString) {
            case "common":
                return "%h %l %u %t \"%r\" %s %b";
            case "combined":
                return "%h %l %u %t \"%r\" %s %b \"%{i,Referer}\" \"%{i,User-Agent}\"";
            case "long":
                return new StringJoiner(System.lineSeparator(), System.lineSeparator(), "")
                        .add("%r")
                        .add("%{ALL_REQUEST_HEADERS}")
                        .toString();
            default:
                return formatString;
        }
    }

    @Override
    public void handle(RoutingContext rc) {
        if (excludePattern != null) {
            Matcher m = excludePattern.matcher(rc.request().path());
            if (m.matches()) {
                rc.next();
                return;
            }
        }
        QuarkusRequestWrapper.get(rc.request()).addRequestDoneHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                accessLogReceiver.logMessage(tokens.readAttribute(rc));
            }
        });
        rc.next();
    }

    @Override
    public String toString() {
        return "AccessLogHandler{" +
                "formatString='" + formatString + '\'' +
                '}';
    }

}
