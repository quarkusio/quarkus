package org.hibernate.protean.substitutions;

import java.util.Map;
import javax.management.ObjectName;

import org.hibernate.jmx.spi.JmxService;
import org.hibernate.service.Service;
import org.hibernate.service.spi.Manageable;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.hibernate.jmx.internal.JmxServiceImpl")
@Substitute
public final class JmxServiceImpl implements JmxService {

	@Substitute
	public JmxServiceImpl(Map configValues) {
		//ignored but needs to exist
	}

	@Override
	public void registerService(Manageable service, Class<? extends Service> serviceRole) {
		//no-op
	}

	@Override
	public void registerMBean(ObjectName objectName, Object mBean) {
		//no-op
	}
}
