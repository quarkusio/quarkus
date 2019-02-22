/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.resteasy.common.runtime.graal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.w3c.dom.Document;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Manipulating {@link Document}s in REST services is very unlikely to be needed
 * and this provider contributes a significant amount of code to the native
 * image due to its dependency to Xerces and Xalan.
 * <p>
 * Let's remove it for now and see if people complain about it. If so, we
 * will need a more advanced strategy to disable/enable it.
 */
@TargetClass(className = "org.jboss.resteasy.plugins.providers.DocumentProvider")
final class DeleteDocumentProvider {

    @Substitute
    public DeleteDocumentProvider(final @Context ResteasyConfiguration config) {
    }

    @Substitute
    public boolean isReadable(Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return false;
    }

    @Substitute
    public Document readFrom(Class<Document> clazz, Type type, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> headers,
            InputStream input) throws IOException, WebApplicationException {
        return null;
    }

    @Substitute
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType) {
        return false;
    }

    @Substitute
    public void writeTo(Document document, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType,
            MultivaluedMap<String, Object> headers,
            OutputStream output) throws IOException, WebApplicationException {

    }
}
