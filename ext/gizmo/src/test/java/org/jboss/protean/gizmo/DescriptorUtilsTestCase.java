package org.jboss.protean.gizmo;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

public class DescriptorUtilsTestCase {

	@Test
	public void testReplace() {
		assertEquals("java/lang/String", DescriptorUtils.replace(String.class.getName(), '.', '/'));
		assertEquals("java/util/Map", DescriptorUtils.replace(Map.class.getName(), '.', '/'));
	}

}
