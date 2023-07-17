package io.quarkus.it.jackson;

import java.sql.Timestamp;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class SqlTimestampPojo {

    public Timestamp timestamp;

    public SqlTimestampPojo() {
    }

    public SqlTimestampPojo(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

}
