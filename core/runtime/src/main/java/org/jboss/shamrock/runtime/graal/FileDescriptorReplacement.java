package org.jboss.shamrock.runtime.graal;

import java.io.FileDescriptor;
import java.io.SyncFailedException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * TODO: this is broken, it needs to be fixed on Graal
 */
@TargetClass(FileDescriptor.class)
final class FileDescriptorReplacement {

    @Substitute
    public void sync() throws SyncFailedException {

    }

}
