package io.quarkus.info;

import java.time.OffsetDateTime;

public interface BuildInfo {

    String group();

    String artifact();

    String version();

    OffsetDateTime time();

    String quarkusVersion();
}
