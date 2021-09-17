package io.quarkus.resteasy.reactive.server.deployment;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

import org.jboss.jandex.DotName;
import org.jboss.resteasy.reactive.ResponseHeader;
import org.jboss.resteasy.reactive.Status;
import org.jboss.resteasy.reactive.multipart.FileUpload;

final class DotNames {

    static final String POPULATE_METHOD_NAME = "populate";
    static final DotName OBJECT_NAME = DotName.createSimple(Object.class.getName());
    static final DotName STRING_NAME = DotName.createSimple(String.class.getName());
    static final DotName BYTE_NAME = DotName.createSimple(byte.class.getName());
    static final DotName INPUT_STREAM_NAME = DotName.createSimple(InputStream.class.getName());
    static final DotName INPUT_STREAM_READER_NAME = DotName.createSimple(InputStreamReader.class.getName());
    static final DotName FIELD_UPLOAD_NAME = DotName.createSimple(FileUpload.class.getName());
    static final DotName PATH_NAME = DotName.createSimple(Path.class.getName());
    static final DotName FILE_NAME = DotName.createSimple(File.class.getName());
    static final DotName RESPONSE_HEADER_ANNOTATION = DotName.createSimple(ResponseHeader.class.getName());
    static final DotName RESPONSE_STATUS_ANNOTATION = DotName.createSimple(Status.class.getName());

    private DotNames() {
    }
}
