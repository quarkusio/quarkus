package io.quarkus.jdbc.mariadb.runtime.graal;

import org.mariadb.jdbc.plugin.authentication.standard.SendPamAuthPacket;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The SendPamAuthPacket class is not supported in native mode.
 */
@Delete
@TargetClass(SendPamAuthPacket.class)
public final class SendPamAuthPacket_Substitutions {

}
