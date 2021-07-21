package org.acme;

import java.util.Collections;
import java.util.Map;

import org.acme.common.CommonBean;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class TestResource implements QuarkusTestResourceLifecycleManager {

	@Override
	public Map<String, String> start() {
		final CommonBean bean = new CommonBean();
		System.out.println("TestResource start " + bean.getClass().getName());
		return Collections.emptyMap();
	}

	@Override
	public void stop() {
	}
}
