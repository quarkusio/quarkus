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

package io.quarkus.example;

import static java.lang.String.format;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.quarkus.extest.runtime.FinalFieldReflectionObject;

/**
 * Final field reflection functionality test
 */
@WebServlet(name = "FinalFieldReflectionTestEndpoint", urlPatterns = "/core/reflection/final")
public class FinalFieldReflectionTestEndpoint extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        reflectiveSetterInvoke(resp);
        resp.getWriter().write("OK");
    }

    private void reflectiveSetterInvoke(HttpServletResponse resp) throws IOException {
        try {
            FinalFieldReflectionObject nominalInstance = new FinalFieldReflectionObject();
            Field field = nominalInstance.getClass().getDeclaredField("value");
            field.setAccessible(true);
            field.set(nominalInstance, "OK");

            Method getValue = nominalInstance.getClass().getMethod("getValue");
            Object value = getValue.invoke(nominalInstance);
            if (!"OK".equals(value)) {
                final PrintWriter writer = resp.getWriter();
                writer.write(format("field incorrectly set, expecting 'OK', got '%s'", value));
                writer.append("\n\t");
            }
        } catch (Exception e) {
            reportException(e, resp);
        }
    }

    private void reportException(final Exception e, final HttpServletResponse resp) throws IOException {
        reportException(null, e, resp);
    }

    private void reportException(String errorMessage, final Exception e, final HttpServletResponse resp) throws IOException {
        final PrintWriter writer = resp.getWriter();
        if (errorMessage != null) {
            writer.write(errorMessage);
            writer.write(" ");
        }
        writer.write(e.toString());
        writer.append("\n\t");
        e.printStackTrace(writer);
        writer.append("\n\t");
    }

}
