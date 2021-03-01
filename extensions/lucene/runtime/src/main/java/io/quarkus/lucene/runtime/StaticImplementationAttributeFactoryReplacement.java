package io.quarkus.lucene.runtime;

import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeFactory.StaticImplementationAttributeFactory;
import org.apache.lucene.util.AttributeImpl;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(StaticImplementationAttributeFactory.class)
public final class StaticImplementationAttributeFactoryReplacement {

    @Substitute
    public AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass) {
        return AttributeCreator.create(attClass);
    }

}
