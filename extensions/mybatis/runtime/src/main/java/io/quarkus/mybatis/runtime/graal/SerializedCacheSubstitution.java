package io.quarkus.mybatis.runtime.graal;

import java.io.Serializable;

import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.cache.decorators.SerializedCache;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(SerializedCache.class)
final public class SerializedCacheSubstitution {

    @Substitute
    private byte[] serialize(Serializable value) {
        throw new CacheException("ObjectOutputStream.writeObject is unsupported in Graal VM");
    }
}
