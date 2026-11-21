package io.quarkus.resteasy.reactive.jsonb.runtime.mappers;

import jakarta.json.stream.JsonLocation;
import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import io.quarkus.runtime.LaunchMode;

/**
 * Maps {@link JsonParsingException} thrown by the JSON-B message body reader for malformed
 * input to a {@code 400 Bad Request} response.
 * <p>
 * In dev and test mode, the parsing failure details (the failure message, line and column)
 * are added to the response entity; in production the response body stays empty so that
 * internals are not leaked to clients.
 */
@SuppressWarnings("unused")
public class BuiltinJsonParsingExceptionMapper implements ExceptionMapper<JsonParsingException> {

    @Override
    public Response toResponse(JsonParsingException exception) {
        var responseBuilder = Response.status(Response.Status.BAD_REQUEST);
        if (LaunchMode.current().isDevOrTest()) {
            var errorBuilder = new JsonParsingError.Builder();
            if (exception.getMessage() != null) {
                errorBuilder.setMessage(exception.getMessage());
            }
            JsonLocation location = exception.getLocation();
            if (location != null) {
                errorBuilder.setLine(location.getLineNumber())
                        .setColumn(location.getColumnNumber())
                        .setOffset(location.getStreamOffset());
            }
            responseBuilder.entity(errorBuilder.build());
        }
        return responseBuilder.build();
    }

    public static class JsonParsingError {
        private final String message;
        private final Long line;
        private final Long column;
        private final Long offset;

        private JsonParsingError(Builder builder) {
            this.message = builder.message;
            this.line = builder.line;
            this.column = builder.column;
            this.offset = builder.offset;
        }

        public String getMessage() {
            return message;
        }

        public Long getLine() {
            return line;
        }

        public Long getColumn() {
            return column;
        }

        public Long getOffset() {
            return offset;
        }

        // will be used when the client only accepts text
        @Override
        public String toString() {
            return "JsonParsingError{" +
                    "message='" + message + '\'' +
                    ", line=" + line +
                    ", column=" + column +
                    ", offset=" + offset +
                    '}';
        }

        static class Builder {
            private String message;
            private Long line;
            private Long column;
            private Long offset;

            public Builder setMessage(String message) {
                this.message = message;
                return this;
            }

            public Builder setLine(Long line) {
                this.line = line;
                return this;
            }

            public Builder setColumn(Long column) {
                this.column = column;
                return this;
            }

            public Builder setOffset(Long offset) {
                this.offset = offset;
                return this;
            }

            public JsonParsingError build() {
                return new JsonParsingError(this);
            }
        }
    }
}
