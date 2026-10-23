package io.quarkus.it.jackson;

import java.lang.StackWalker.Option;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonPropertyOrder({ "creatorValue", "fieldValue", "setterValue", "creatorInvocationStackTrace",
        "setterInvocationStackTrace", "serializationInvocationStackTrace" })
public class MethodHandlePojo {

    private final String creatorValue;
    private final List<String> creatorInvocationStackTrace;

    @JsonProperty
    private String fieldValue;

    private String setterValue;
    private List<String> setterInvocationStackTrace;
    private final AtomicReference<List<String>> serializationInvocationStackTrace = new AtomicReference<>();

    private MethodHandlePojo(String creatorValue, List<String> creatorInvocationStackTrace) {
        this.creatorValue = creatorValue;
        this.creatorInvocationStackTrace = creatorInvocationStackTrace;
    }

    @JsonCreator
    public static MethodHandlePojo create(@JsonProperty("creatorValue") String creatorValue) {
        return new MethodHandlePojo(creatorValue, invocationStackTrace());
    }

    public String getCreatorValue() {
        serializationInvocationStackTrace.compareAndSet(null, invocationStackTrace());
        return creatorValue;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public String getSetterValue() {
        return setterValue;
    }

    public void setSetterValue(String setterValue) {
        setterInvocationStackTrace = invocationStackTrace();
        this.setterValue = setterValue;
    }

    @JsonProperty(access = Access.READ_ONLY)
    public List<String> getCreatorInvocationStackTrace() {
        return creatorInvocationStackTrace;
    }

    @JsonProperty(access = Access.READ_ONLY)
    public List<String> getSetterInvocationStackTrace() {
        return setterInvocationStackTrace;
    }

    @JsonProperty(access = Access.READ_ONLY)
    public List<String> getSerializationInvocationStackTrace() {
        return serializationInvocationStackTrace.get();
    }

    private static List<String> invocationStackTrace() {
        return StackWalker.getInstance(Option.SHOW_REFLECT_FRAMES)
                .walk(frames -> frames.limit(20)
                        .map(frame -> frame.getClassName() + "." + frame.getMethodName())
                        .collect(Collectors.toList()));
    }
}
