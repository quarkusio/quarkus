package org.hibernate.protean.substitutions;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.hibernate.dialect.Oracle8iDialect")
@Delete
public final class RemoveOracle {

	//The Oracle8iDialect presence gets SVM to initialize the org.hibernate.dialect.Oracle8iDialect,
	//which in turn pulls in the Oracle JDBC driver - and requires it to be on classpath.

}
