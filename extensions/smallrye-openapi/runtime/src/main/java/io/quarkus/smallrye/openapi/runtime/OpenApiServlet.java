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

package io.quarkus.smallrye.openapi.runtime;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;

/**
 * @author Ken Finnigan
 */
@WebServlet
public class OpenApiServlet extends HttpServlet {

    private static final String ALLOWED_METHODS = "GET, HEAD, OPTIONS";

    private static final String QUERY_PARAM_FORMAT = "format";

    protected static final String GENERATED_DOC_BASE = "quarkus-generated-openapi-doc.";
    public static final String BASE_NAME = "META-INF/resources/" + GENERATED_DOC_BASE;

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        addCorsResponseHeaders(resp);
        resp.addHeader("Allow", ALLOWED_METHODS);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String accept = req.getHeader("Accept");
        String formatParam = req.getParameter(QUERY_PARAM_FORMAT);

        // Default content type is YAML
        OpenApiSerializer.Format format = OpenApiSerializer.Format.YAML;

        // Check Accept, then query parameter "format" for JSON; else use YAML.
        if ((accept != null && accept.contains(OpenApiSerializer.Format.JSON.getMimeType())) ||
                (OpenApiSerializer.Format.JSON.getMimeType().equalsIgnoreCase(formatParam))) {
            format = OpenApiSerializer.Format.JSON;
        }

        addCorsResponseHeaders(resp);
        resp.setHeader("Content-Type", format.getMimeType());
        resp.setCharacterEncoding("UTF-8");
        req.getRequestDispatcher(GENERATED_DOC_BASE + format).include(req, resp);
    }

    private static void addCorsResponseHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Max-Age", "86400");
    }
}
