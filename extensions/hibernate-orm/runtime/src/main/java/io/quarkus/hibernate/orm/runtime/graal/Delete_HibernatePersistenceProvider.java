package io.quarkus.hibernate.orm.runtime.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.hibernate.jpa.HibernatePersistenceProvider")
@Delete
public final class Delete_HibernatePersistenceProvider {
}
