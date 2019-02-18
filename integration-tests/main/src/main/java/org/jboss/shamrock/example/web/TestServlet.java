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

package org.jboss.shamrock.example.web;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@WebServlet(name = "MyServlet", urlPatterns = "/testservlet", initParams = {@WebInitParam(name = "message", value = "A message")})
public class TestServlet extends HttpServlet {

    @Inject
    @ConfigProperty(name = "web-message")
    String configMessage;

    @Inject
    HttpServletResponse injectedResponse;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        injectedResponse.getWriter().write(configMessage);
    }
}
