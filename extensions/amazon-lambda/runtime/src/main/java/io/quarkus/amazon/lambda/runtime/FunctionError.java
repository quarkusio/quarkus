package io.quarkus.amazon.lambda.runtime;

public class FunctionError {

    private String errorType;
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
