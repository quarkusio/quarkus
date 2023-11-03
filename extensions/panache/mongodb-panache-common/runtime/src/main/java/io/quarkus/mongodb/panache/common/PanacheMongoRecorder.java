package io.quarkus.mongodb.panache.common;

import java.util.Map;

import io.quarkus.mongodb.panache.common.runtime.MongoPropertyUtil;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheMongoRecorder {
    public void setReplacementCache(Map<String, Map<String, String>> replacementMap) {
        MongoPropertyUtil.setReplacementCache(replacementMap);
    }
}
