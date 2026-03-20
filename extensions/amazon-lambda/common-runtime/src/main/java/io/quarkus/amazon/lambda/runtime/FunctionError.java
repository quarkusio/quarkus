package io.quarkus.amazon.lambda.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FunctionError {

    @JsonProperty("errorType")
    private String errorType;
    @JsonProperty("errorMessage")
    private String errorMessage;

    public FunctionError(String errorType, String errorMessage) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    public FunctionError() {
    }

    public String getErrorType() {
        return errorType;
    }

    public FunctionError setErrorType(String errorType) {
        this.errorType = errorType;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public FunctionError setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }
}
