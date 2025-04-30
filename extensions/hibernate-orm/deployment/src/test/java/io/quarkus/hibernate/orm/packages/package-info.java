@NamedQuery(name = "test", query = "from ParentEntity")
@FilterDef(name = "filter", defaultCondition = "true")
package io.quarkus.hibernate.orm.packages;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.NamedQuery;