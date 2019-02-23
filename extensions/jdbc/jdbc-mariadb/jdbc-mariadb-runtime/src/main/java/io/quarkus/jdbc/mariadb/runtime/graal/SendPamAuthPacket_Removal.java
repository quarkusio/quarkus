package io.quarkus.jdbc.mariadb.runtime.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.mariadb.jdbc.internal.com.send.SendPamAuthPacket")
@Delete
public final class SendPamAuthPacket_Removal {
}
