package io.quarkus.devui.runtime.jsonrpc;

import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.VERSION;

public final class JsonRpcResponse {

    // Public for serialization
    public final int id;
    public final Result result;
    public final Error error;

    public JsonRpcResponse(int id, Result result) {
        this.id = id;
        this.result = result;
        this.error = null;
    }

    public JsonRpcResponse(int id, Error error) {
        this.id = id;
        this.result = null;
        this.error = error;
    }

    public String getJsonrpc() {
        return VERSION;
    }

    @Override
    public String toString() {
        return "JsonRpcResponse{" +
                "id=" + id +
                ", result=" + result +
                ", error=" + error +
                '}';
    }

    public static final class Result {
        public final String messageType;
        public final Object object;

        public Result(String messageType, Object object) {
            this.messageType = messageType;
            this.object = object;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "messageType='" + messageType + '\'' +
                    ", object=" + object +
                    '}';
        }
    }

    public static final class Error {
        public final int code;
        public final String message;

        public Error(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public String toString() {
            return "Error{" +
                    "code=" + code +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
