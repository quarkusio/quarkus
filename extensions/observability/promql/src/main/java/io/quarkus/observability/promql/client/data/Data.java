package io.quarkus.observability.promql.client.data;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "resultType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MatrixData.class, name = "matrix"),
        @JsonSubTypes.Type(value = ScalarData.class, name = "scalar"),
        @JsonSubTypes.Type(value = StringData.class, name = "string"),
        @JsonSubTypes.Type(value = VectorData.class, name = "vector")
})
public interface Data<T> {
    T result();
}