package io.quarkus.infinispan.client.substitutions;

import org.infinispan.commons.dataconversion.DefaultTranscoder;
import org.infinispan.commons.dataconversion.GenericJbossMarshallerEncoder;
import org.infinispan.commons.marshall.jboss.AbstractJBossMarshaller;
import org.infinispan.commons.marshall.jboss.GenericJBossMarshaller;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This class removes jboss marshalling based classes in client, which we don't support with substrate
 * 
 * @author William Burns
 */

final class DeleteJBossBasedClasses {
}

@TargetClass(GenericJBossMarshaller.class)
@Delete
final class DeleteGenericJBossMarshaller {
}

@TargetClass(AbstractJBossMarshaller.class)
@Delete
final class DeleteAbstractJBossMarshaller {
}

@TargetClass(GenericJbossMarshallerEncoder.class)
@Delete
final class DeleteGenericJbossMarshallerEncoder {
}

@TargetClass(DefaultTranscoder.class)
@Delete
final class DeleteGenericDefaultTranscoder {
}
