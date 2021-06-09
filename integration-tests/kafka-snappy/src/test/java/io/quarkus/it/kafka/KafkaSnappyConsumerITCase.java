package io.quarkus.it.kafka;

import static io.quarkus.test.junit.DisableIfBuiltWithGraalVMOlderThan.GraalVMVersion.GRAALVM_21_0;

import io.quarkus.test.junit.DisableIfBuiltWithGraalVMOlderThan;
import io.quarkus.test.junit.NativeImageTest;

@NativeImageTest
@DisableIfBuiltWithGraalVMOlderThan(GRAALVM_21_0)
public class KafkaSnappyConsumerITCase extends KafkaSnappyConsumerTest {

}
