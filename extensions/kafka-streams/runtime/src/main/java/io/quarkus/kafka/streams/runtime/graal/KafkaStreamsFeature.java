package io.quarkus.kafka.streams.runtime.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.rocksdb.util.Environment;

public class KafkaStreamsFeature implements Feature {

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        String libraryFileName = Environment.getJniLibraryFileName("rocksdb");
        RuntimeResourceAccess.addResource(Environment.class.getModule(), libraryFileName);
    }

}
