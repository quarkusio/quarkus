package io.quarkus.pulsar.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;
import com.scurrilous.circe.checksum.IntHash;
import com.scurrilous.circe.checksum.Java8IntHash;

@TargetClass(className = "com.scurrilous.circe.checksum.Crc32cIntChecksum")
final class Target_com_scurrilous_circe_checksum_Crc32cIntChecksum {

    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    @Alias
    private static IntHash CRC32C_HASH = new Java8IntHash();

}
