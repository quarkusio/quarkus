@AnyMetaDef(name = "childrenAnyMetaDef", idType = "long", metaType = "string", metaValues = {
        @MetaValue(targetEntity = ChildEntity1.class, value = "child1"),
        @MetaValue(targetEntity = ChildEntity2.class, value = "child2")
})
package io.quarkus.hibernate.orm.packages;

import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.MetaValue;