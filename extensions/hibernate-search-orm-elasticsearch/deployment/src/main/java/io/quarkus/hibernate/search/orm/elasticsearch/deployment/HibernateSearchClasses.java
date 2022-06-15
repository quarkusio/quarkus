package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.jboss.jandex.DotName;

class HibernateSearchClasses {

    static final DotName INDEXED = DotName.createSimple(Indexed.class.getName());

}
