package io.quarkus.lucene.runtime;

import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeImpl;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.apache.lucene.util.AttributeFactory$DefaultAttributeFactory")
public final class DefaultAttributeFactoryReplacement {

    @Substitute
    public AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass) {
        return AttributeCreator.create(attClass);
    }
}
