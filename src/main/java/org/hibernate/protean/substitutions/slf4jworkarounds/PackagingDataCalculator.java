package org.hibernate.protean.substitutions.slf4jworkarounds;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "ch.qos.logback.classic.spi.PackagingDataCalculator")
public final class PackagingDataCalculator {

	@Substitute
	String getCodeLocation(Class type) {
		//hard code this to ensure some tricky code is not included
		return "na";
	}

}
