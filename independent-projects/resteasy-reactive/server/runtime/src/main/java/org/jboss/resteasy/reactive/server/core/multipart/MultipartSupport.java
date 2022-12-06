package org.jboss.resteasy.reactive.server.core.multipart;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.common.util.Encode;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.ServerSerialisers;
import org.jboss.resteasy.reactive.server.core.multipart.FormData.FormValue;
import org.jboss.resteasy.reactive.server.handlers.RequestDeserializeHandler;
import org.jboss.resteasy.reactive.server.multipart.MultipartPartReadingException;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;

/**
 * This class isn't used directly, it is however used by generated code meant to deal with multipart forms.
 */
@SuppressWarnings("unused")
public final class MultipartSupport {

    private static final Logger log = Logger.getLogger(RequestDeserializeHandler.class);

    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    private MultipartSupport() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object convertFormAttribute(String value, Class type, Type genericType, MediaType mediaType,
            ResteasyReactiveRequestContext context, String attributeName) {
        if (value == null) {
            FormData formData = context.getFormData();
            if (formData != null) {
                Collection<FormValue> fileUploadsForName = formData.get(attributeName);
                if (fileUploadsForName != null) {
                    for (FormData.FormValue fileUpload : fileUploadsForName) {
                        if (fileUpload.isFileItem()) {
                            log.warn("Attribute '" + attributeName
                                    + "' of the multipart request is a file and therefore its value is not set. To obtain the contents of the file, use type '"
                                    + FileUpload.class + "' as the field type.");
                            break;
                        }
                    }
                }
            }
            return null;
        }

        ServerSerialisers serialisers = context.getDeployment().getSerialisers();
        List<MessageBodyReader<?>> readers = serialisers.findReaders(null, type, mediaType, RuntimeType.SERVER);
        if (readers.isEmpty()) {
            throw new NotSupportedException();
        }

        for (MessageBodyReader<?> reader : readers) {
            if (reader instanceof ServerMessageBodyReader) {
                ServerMessageBodyReader<?> serverMessageBodyReader = (ServerMessageBodyReader<?>) reader;
                if (serverMessageBodyReader.isReadable(type, genericType, context.getTarget().getLazyMethod(), mediaType)) {
                    // this should always be an empty stream as multipart doesn't set the request body
                    InputStream originalInputStream = context.getInputStream();
                    try {
                        // we need to set a fake input stream in order to trick the readers into thinking they are reading from the body
                        context.setInputStream(formAttributeValueToInputStream(value));
                        return serverMessageBodyReader.readFrom(type, genericType, mediaType, context);
                    } catch (IOException e) {
                        log.error("Unable to convert value provided for attribute '" + attributeName
                                + "' of the multipart request into type '" + type.getName() + "'", e);
                        throw new MultipartPartReadingException(e);
                    } finally {
                        context.setInputStream(originalInputStream);
                    }

                }
            } else {
                // TODO: should we be passing in the annotations?
                if (reader.isReadable(type, genericType, EMPTY_ANNOTATIONS, mediaType)) {
                    try {
                        return reader.readFrom((Class) type, genericType, EMPTY_ANNOTATIONS, mediaType,
                                context.getHttpHeaders().getRequestHeaders(),
                                formAttributeValueToInputStream(value));
                    } catch (IOException e) {
                        log.error("Unable to convert value provided for attribute '" + attributeName
                                + "' of the multipart request into type '" + type.getName() + "'", e);
                        throw new MultipartPartReadingException(e);
                    }
                }
            }
        }
        throw new NotSupportedException("Media type '" + mediaType + "' in multipart request is not supported");
    }

    private static FormData.FormValue getFirstValue(String formName, ResteasyReactiveRequestContext context) {
        Deque<FormValue> values = getValues(formName, context);
        if (values != null && !values.isEmpty()) {
            return values.getFirst();
        }
        return null;
    }

    private static Deque<FormData.FormValue> getValues(String formName, ResteasyReactiveRequestContext context) {
        FormData form = context.getFormData();
        if (form != null) {
            return form.get(formName);
        }
        return null;
    }

    public static String getString(String formName, ResteasyReactiveRequestContext context) {
        return getString(formName, context, false);
    }

    public static String getString(String formName, ResteasyReactiveRequestContext context, boolean encoded) {
        FormData.FormValue value = getFirstValue(formName, context);
        if (value == null) {
            return null;
        }
        // NOTE: we're not encoding it in case of file items, because multipart doesn't even use urlencoding,
        // this is only for the TCK and regular form params
        if (value.isFileItem()) {
            try {
                return Files.readString(value.getFileItem().getFile(), Charset.defaultCharset());
            } catch (IOException e) {
                throw new MultipartPartReadingException(e);
            }
        } else {
            if (encoded)
                return Encode.encodeQueryParam(value.getValue());
            return value.getValue();
        }
    }

    public static List<String> getStrings(String formName, ResteasyReactiveRequestContext context) {
        return getStrings(formName, context, false);
    }

    public static List<String> getStrings(String formName, ResteasyReactiveRequestContext context, boolean encoded) {
        Deque<FormData.FormValue> values = getValues(formName, context);
        if (values == null) {
            return Collections.emptyList();
        }
        List<String> ret = new ArrayList<String>();
        // NOTE: we're not encoding it in case of file items, because multipart doesn't even use urlencoding,
        // this is only for the TCK and regular form params
        for (FormValue value : values) {
            if (value.isFileItem()) {
                try {
                    ret.add(Files.readString(value.getFileItem().getFile(), Charset.defaultCharset()));
                } catch (IOException e) {
                    throw new MultipartPartReadingException(e);
                }
            } else {
                if (encoded) {
                    ret.add(Encode.encodeQueryParam(value.getValue()));
                } else {
                    ret.add(value.getValue());
                }
            }
        }
        return ret;
    }

    public static byte[] getByteArray(String formName, ResteasyReactiveRequestContext context) {
        FormData.FormValue value = getFirstValue(formName, context);
        if (value == null) {
            return null;
        }
        if (value.isFileItem()) {
            try {
                return Files.readAllBytes(value.getFileItem().getFile());
            } catch (IOException e) {
                throw new MultipartPartReadingException(e);
            }
        } else {
            return value.getValue().getBytes(Charset.defaultCharset());
        }
    }

    public static List<byte[]> getByteArrays(String formName, ResteasyReactiveRequestContext context) {
        Deque<FormData.FormValue> values = getValues(formName, context);
        if (values == null) {
            return Collections.emptyList();
        }
        List<byte[]> ret = new ArrayList<byte[]>();
        for (FormValue value : values) {
            if (value.isFileItem()) {
                try {
                    ret.add(Files.readAllBytes(value.getFileItem().getFile()));
                } catch (IOException e) {
                    throw new MultipartPartReadingException(e);
                }
            } else {
                ret.add(value.getValue().getBytes(Charset.defaultCharset()));
            }
        }
        return ret;
    }

    public static InputStream getInputStream(String formName, ResteasyReactiveRequestContext context) {
        FormData.FormValue value = getFirstValue(formName, context);
        if (value == null) {
            return null;
        }
        if (value.isFileItem()) {
            try {
                return new FileInputStream(value.getFileItem().getFile().toFile());
            } catch (IOException e) {
                throw new MultipartPartReadingException(e);
            }
        } else {
            return new ByteArrayInputStream(value.getValue().getBytes(Charset.defaultCharset()));
        }
    }

    public static List<InputStream> getInputStreams(String formName, ResteasyReactiveRequestContext context) {
        Deque<FormData.FormValue> values = getValues(formName, context);
        if (values == null) {
            return Collections.emptyList();
        }
        List<InputStream> ret = new ArrayList<InputStream>();
        for (FormValue value : values) {
            if (value.isFileItem()) {
                try {
                    ret.add(new FileInputStream(value.getFileItem().getFile().toFile()));
                } catch (IOException e) {
                    throw new MultipartPartReadingException(e);
                }
            } else {
                ret.add(new ByteArrayInputStream(value.getValue().getBytes(Charset.defaultCharset())));
            }
        }
        return ret;
    }

    public static DefaultFileUpload getSingleFileUpload(String formName, ResteasyReactiveRequestContext context) {
        List<DefaultFileUpload> uploads = getFileUploads(formName, context);
        if (uploads.size() > 1) {
            throw new BadRequestException("Found more than one files for attribute '" + formName + "'. Expected only one file");
        } else if (uploads.size() == 1) {
            return uploads.get(0);
        }
        return null;
    }

    public static List<Object> convertFormAttributes(List<String> params, Class<Object> typeClass, Type genericType,
            MediaType mimeType, ResteasyReactiveRequestContext context, String name) {
        List<Object> ret = new ArrayList<>(params.size());
        for (String param : params) {
            ret.add(MultipartSupport.convertFormAttribute(param, typeClass, genericType,
                    mimeType, context,
                    name));
        }
        return ret;
    }

    public static DefaultFileUpload getFileUpload(String formName, ResteasyReactiveRequestContext context) {
        List<DefaultFileUpload> uploads = getFileUploads(formName, context);
        if (!uploads.isEmpty()) {
            return uploads.get(0);
        }
        return null;
    }

    public static List<DefaultFileUpload> getFileUploads(String formName, ResteasyReactiveRequestContext context) {
        List<DefaultFileUpload> result = new ArrayList<>();
        FormData fileUploads = context.getFormData();
        if (fileUploads != null) {
            Collection<FormValue> fileUploadsForName = fileUploads.get(formName);
            if (fileUploadsForName != null) {
                for (FormData.FormValue fileUpload : fileUploadsForName) {
                    if (fileUpload.isFileItem()) {
                        result.add(new DefaultFileUpload(formName, fileUpload));
                    }
                }
            }
        }
        return result;
    }

    public static List<File> getJavaIOFileUploads(String formName, ResteasyReactiveRequestContext context) {
        List<File> result = new ArrayList<>();
        List<DefaultFileUpload> uploads = getFileUploads(formName, context);
        for (DefaultFileUpload upload : uploads) {
            result.add(upload.uploadedFile().toFile());
        }
        return result;
    }

    public static List<Path> getJavaPathFileUploads(String formName, ResteasyReactiveRequestContext context) {
        List<Path> result = new ArrayList<>();
        List<DefaultFileUpload> uploads = getFileUploads(formName, context);
        for (DefaultFileUpload upload : uploads) {
            result.add(upload.uploadedFile());
        }
        return result;
    }

    public static List<DefaultFileUpload> getFileUploads(ResteasyReactiveRequestContext context) {
        FormData formData = context.getFormData();
        if (formData == null) {
            return Collections.emptyList();
        }
        List<DefaultFileUpload> result = new ArrayList<>();
        for (String name : formData) {
            for (FormData.FormValue fileUpload : formData.get(name)) {
                if (fileUpload.isFileItem()) {
                    result.add(new DefaultFileUpload(name, fileUpload));
                }
            }
        }
        return result;
    }

    private static ByteArrayInputStream formAttributeValueToInputStream(String formAttributeValue) {
        return new ByteArrayInputStream(formAttributeValue.getBytes(StandardCharsets.UTF_8));
    }
}
