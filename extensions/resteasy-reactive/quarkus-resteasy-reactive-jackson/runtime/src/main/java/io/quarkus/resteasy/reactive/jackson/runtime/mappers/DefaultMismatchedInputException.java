package io.quarkus.resteasy.reactive.jackson.runtime.mappers;

import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import io.quarkus.runtime.LaunchMode;

public class DefaultMismatchedInputException
        implements ExceptionMapper<com.fasterxml.jackson.databind.exc.MismatchedInputException> {

    @Override
    public Response toResponse(com.fasterxml.jackson.databind.exc.MismatchedInputException exception) {
        var responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        if (LaunchMode.current().isDevOrTest()) {
            List<JsonMappingException.Reference> path = exception.getPath();
            if (path != null && !path.isEmpty()) {
                var errorBuilder = new MismatchedJsonInputError.Builder((path.get(0)).getFrom().getClass().getSimpleName());
                StringBuilder attributeNameBuilder = new StringBuilder();

                for (JsonMappingException.Reference pathReference : path) {
                    if (pathReference.getFieldName() != null) {
                        if (attributeNameBuilder.length() > 0) {
                            attributeNameBuilder.append(".");
                        }

                        attributeNameBuilder.append(pathReference.getFieldName());
                    }

                    if (pathReference.getIndex() >= 0) {
                        attributeNameBuilder.append("[");
                        attributeNameBuilder.append(pathReference.getIndex());
                        attributeNameBuilder.append("]");
                    }
                }

                if (attributeNameBuilder.length() > 0) {
                    errorBuilder.setAttributeName(attributeNameBuilder.toString());
                }

                if (exception.getLocation() != null) {
                    errorBuilder.setLine(exception.getLocation().getLineNr());
                    errorBuilder.setColumn(exception.getLocation().getColumnNr());
                    if (exception instanceof InvalidFormatException) {
                        InvalidFormatException ife = (InvalidFormatException) exception;
                        if (ife.getValue() != null) {
                            errorBuilder.setValue(ife.getValue());
                        }
                    }
                }

                responseBuilder.entity(errorBuilder.build());
            }

        }
        return responseBuilder.build();
    }

    public static class MismatchedJsonInputError {
        private final String objectName;
        private final String attributeName;
        private final Integer line;
        private final Integer column;
        private final Object value;

        private MismatchedJsonInputError(Builder builder) {
            this.objectName = builder.objectName;
            this.attributeName = builder.attributeName;
            this.line = builder.line;
            this.column = builder.column;
            this.value = builder.value;
        }

        public String getObjectName() {
            return objectName;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public Integer getLine() {
            return line;
        }

        public Integer getColumn() {
            return column;
        }

        public Object getValue() {
            return value;
        }

        // will be used when the client only accepts text
        @Override
        public String toString() {
            return "MismatchedJsonInputError{" +
                    "objectName='" + objectName + '\'' +
                    ", attributeName='" + attributeName + '\'' +
                    ", line=" + line +
                    ", column=" + column +
                    ", value='" + value + '\'' +
                    '}';
        }

        static class Builder {
            private final String objectName;
            private String attributeName;
            private Integer line;
            private Integer column;
            private Object value;

            public Builder(String objectName) {
                this.objectName = objectName;
            }

            public Builder setAttributeName(String attributeName) {
                this.attributeName = attributeName;
                return this;
            }

            public Builder setLine(Integer line) {
                this.line = line;
                return this;
            }

            public Builder setColumn(Integer column) {
                this.column = column;
                return this;
            }

            public Builder setValue(Object value) {
                this.value = value;
                return this;
            }

            public MismatchedJsonInputError build() {
                return new MismatchedJsonInputError(this);
            }
        }
    }
}
