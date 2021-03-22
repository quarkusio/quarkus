package io.quarkus.runtime.graal;

import java.io.FileDescriptor;
import java.nio.MappedByteBuffer;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK15OrEarlier;

@TargetClass(value = MappedByteBuffer.class, onlyWith = JDK15OrEarlier.class)
final class MappedByteBufferReplacement {

    @Substitute
    private void force0(FileDescriptor fd, long address, long length) {

    }
}
