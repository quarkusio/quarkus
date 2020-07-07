package io.quarkus.jberet.runtime;

import java.io.Serializable;
import java.util.Arrays;

import javax.batch.operations.BatchRuntimeException;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbException;

import org.jberet._private.BatchLogger;
import org.jberet._private.BatchMessages;
import org.jberet.runtime.SerializableData;

import com.oracle.svm.core.SubstrateUtil;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

/**
 * Used to replace Java Serialization not supported in SVM by Jsonb Serialization.
 *
 * This is just a way to make JBeret work in native mode. Since we only support the in memory repository for now, this
 * should be fine. If we add support to a persistent repository, then the data written by the JVM version and the SVM
 * version will not be compatible.
 */
public class JBeretSubstitutions {
    static final Jsonb jsonb = JsonbBuilder.newBuilder().build();

    @TargetClass(SerializableData.class)
    static final class Target_SerializableData {
        @Alias
        private byte[] serialized;
        @Alias
        private Serializable raw;

        @Alias
        @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
        Target_SerializableData(final byte[] serialized, final Serializable raw) {

        }

        @Substitute
        public static SerializableData of(final Serializable data) {
            if (data instanceof SerializableData) {
                return (SerializableData) data;
            }
            if (data instanceof byte[]) {
                return SubstrateUtil.cast(new Target_SerializableData((byte[]) data, null), SerializableData.class);
            }
            if (data == null) {
                return SubstrateUtil.cast(new Target_SerializableData(null, null), SerializableData.class);
            }

            Class<?> c = data.getClass();
            if (c.isArray()) {
                c = c.getComponentType();
            }

            if (requiresSerialization(c)) {
                try {
                    return SubstrateUtil.cast(new Target_SerializableData(jsonb.toJson(data).getBytes(), data),
                            SerializableData.class);
                } catch (JsonbException e) {
                    if (data instanceof Throwable) {
                        //if failed to serialize step exception data, try to preserve original
                        //step exception message and stack trace
                        final Throwable exceptionData = (Throwable) data;
                        BatchLogger.LOGGER.failedToSerializeException(e, exceptionData);
                        final BatchRuntimeException replacementException = new BatchRuntimeException(
                                exceptionData.getMessage());
                        replacementException.setStackTrace(exceptionData.getStackTrace());
                        try {
                            return SubstrateUtil.cast(new Target_SerializableData(jsonb.toJson(data).getBytes(), data),
                                    SerializableData.class);
                        } catch (final JsonbException e1) {
                            throw BatchMessages.MESSAGES.failedToSerialize(e1, replacementException);
                        }
                    }
                    throw BatchMessages.MESSAGES.failedToSerialize(e, data);
                }
            }
            return SubstrateUtil.cast(new Target_SerializableData(null, data), SerializableData.class);
        }

        @Substitute
        public Serializable deserialize() {
            if (serialized != null) {
                try {
                    return jsonb.fromJson(new String(serialized), raw.getClass());
                } catch (JsonbException e) {
                    throw BatchMessages.MESSAGES.failedToDeserialize(e, Arrays.toString(serialized));
                }
            }

            if (raw != null) {
                return raw;
            }

            return null;
        }

        @Substitute
        byte[] getSerialized() throws RuntimeException {
            if (serialized != null) {
                return serialized;
            }
            try {
                return jsonb.toJson(raw).getBytes();
            } catch (final JsonbException e) {
                throw BatchMessages.MESSAGES.failedToSerialize(e, raw);
            }
        }

        @Alias
        private static boolean requiresSerialization(final Class<?> c) {
            return false;
        }
    }
}
