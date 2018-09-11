package org.jboss.shamrock.runtime.graal.logback;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "ch.qos.logback.classic.spi.ThrowableProxy")
public final class ThrowableProxy {

	@Substitute
	public void calculatePackagingData() {
	}

}
