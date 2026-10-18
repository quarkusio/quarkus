package io.quarkus.resteasy.reactive.jackson.deployment.test.generated;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class FormatDateTimestampBean {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Date timestamp;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
