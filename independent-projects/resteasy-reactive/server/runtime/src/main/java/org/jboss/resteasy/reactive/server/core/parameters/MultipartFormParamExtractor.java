package org.jboss.resteasy.reactive.server.core.parameters;

import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.multipart.MultipartSupport;

public class MultipartFormParamExtractor implements ParameterExtractor {

    private final String name;
    private final String mimeType;
    private final Type type;
    private final boolean single;
    private final java.lang.reflect.Type genericType;
    private final Class<Object> typeClass;
    // Note that this is only used for the String type, due to the TCK requiring it
    private final boolean encoded;
    private final boolean allowEmpty;

    public enum Type {
        FileUpload,
        File,
        Path,
        PartType,
        String,
        ByteArray,
        InputStream;
    }

    public MultipartFormParamExtractor(String name, boolean single, Type type, Class<Object> typeClass,
            java.lang.reflect.Type genericType, String mimeType, boolean encoded, boolean allowEmpty) {
        this.name = name;
        this.single = single;
        this.type = type;
        this.mimeType = mimeType;
        this.typeClass = typeClass;
        this.genericType = genericType;
        this.encoded = encoded;
        this.allowEmpty = allowEmpty;
    }

    @Override
    public Object extractParameter(ResteasyReactiveRequestContext context) {
        switch (type) {
            case String:
                if (single) {
                    return MultipartSupport.getString(name, context, encoded);
                } else {
                    return MultipartSupport.getStrings(name, context, encoded);
                }
            case ByteArray:
                if (single) {
                    return MultipartSupport.getByteArray(name, context);
                } else {
                    return MultipartSupport.getByteArrays(name, context);
                }
            case InputStream:
                if (single) {
                    return MultipartSupport.getInputStream(name, context);
                } else {
                    return MultipartSupport.getInputStreams(name, context);
                }
            case PartType:
                if (single) {
                    return MultipartSupport.getConvertedFormAttribute(name, typeClass, genericType, MediaType.valueOf(mimeType),
                            context);
                } else {
                    return MultipartSupport.getConvertedFormAttributes(name, typeClass, genericType,
                            MediaType.valueOf(mimeType),
                            context);
                }
            case FileUpload:
                // special case
                if (name.equals(FileUpload.ALL))
                    return MultipartSupport.getFileUploads(context);
                return single ? MultipartSupport.getFileUpload(name, context)
                        : MultipartSupport.getFileUploads(name, context);
            case File:
                if (single) {
                    FileUpload upload = MultipartSupport.getFileUpload(name, context);
                    return upload != null ? upload.uploadedFile().toFile() : null;
                } else {
                    return MultipartSupport.getJavaIOFileUploads(name, context);
                }
            case Path:
                if (single) {
                    FileUpload upload = MultipartSupport.getFileUpload(name, context);
                    return upload != null ? upload.uploadedFile() : null;
                } else {
                    return MultipartSupport.getJavaPathFileUploads(name, context);
                }
            default:
                throw new RuntimeException("Unknown multipart parameter type: " + type + " for parameter " + name);
        }
    }
}
