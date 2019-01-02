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

package org.jboss.shamrock.metrics.runtime;

import java.io.IOException;
import java.util.Collections;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.smallrye.metrics.MetricsRequestHandler;

/**
 * Created by bob on 7/30/18.
 */
@WebServlet
public class MetricsServlet extends HttpServlet {

    @Inject
    MetricsRequestHandler metricsHandler;

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestPath = request.getRequestURI();
        String method = request.getMethod();
        Stream<String> acceptHeaders = Collections.list(request.getHeaders("Accept")).stream();

        metricsHandler.handleRequest(requestPath, method, acceptHeaders, (status, message, headers) -> {
            headers.forEach(response::addHeader);
            response.setStatus(status);
            response.getWriter().write(message);
        });
    }
}
