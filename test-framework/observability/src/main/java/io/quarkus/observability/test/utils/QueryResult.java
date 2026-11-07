package io.quarkus.observability.test.utils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryResult {
    public String status;
    public Data data;

    // getters and setters

    @Override
    public String toString() {
        return "QueryResult{" +
                "status='" + status + '\'' +
                ", data=" + data +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        public String resultType;
        public List<ResultItem> result;

        // getters and setters

        @Override
        public String toString() {
            return "Data{" +
                    "resultType='" + resultType + '\'' +
                    ", result=" + result +
                    '}';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultItem {
        public Metric metric;
        public List<String> value;

        // getters and setters

        @Override
        public String toString() {
            return "ResultItem{" +
                    "metric=" + metric +
                    ", value=" + value +
                    '}';
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Metric {
        @JsonProperty("__name__")
        public String name;
        public String job;
        public String test;
        @JsonAnySetter
        public Map<String, String> labels = new LinkedHashMap<>();

        // getters and setters

        @Override
        public String toString() {
            return "Metric{" +
                    "name='" + name + '\'' +
                    ", job='" + job + '\'' +
                    ", test='" + test + '\'' +
                    ", labels=" + labels +
                    '}';
        }
    }
}
