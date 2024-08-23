package io.quarkus.info;

import java.time.OffsetDateTime;

public interface GitInfo {

    String branch();

    String latestCommitId();

    OffsetDateTime commitTime();
}
