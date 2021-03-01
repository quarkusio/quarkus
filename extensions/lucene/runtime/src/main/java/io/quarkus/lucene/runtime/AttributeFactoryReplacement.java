package io.quarkus.lucene.runtime;

import java.lang.reflect.UndeclaredThrowableException;

import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeFactory.StaticImplementationAttributeFactory;
import org.apache.lucene.util.AttributeImpl;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(AttributeFactory.class)
public final class AttributeFactoryReplacement {

    @Substitute
    public static <A extends AttributeImpl> AttributeFactory getStaticImplementation(AttributeFactory delegate,
            Class<A> clazz) {
        return new StaticImplementationAttributeFactory<A>(delegate, clazz) {
            @Override
            protected A createInstance() {
                try {
                    return (A) AttributeCreator.create(clazz);
                } catch (Error | RuntimeException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new UndeclaredThrowableException(e);
                }
            }
        };
    }
}
