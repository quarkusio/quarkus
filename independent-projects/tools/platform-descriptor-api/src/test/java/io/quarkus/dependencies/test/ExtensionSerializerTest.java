package io.quarkus.dependencies.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.dependencies.Extension;

class ExtensionSerializerTest {

	@Test
	void testLegacyLabelsMapToKeywords() {
		Extension e = new Extension();

		String[] input = new String[] { "what", "not", "to", "do"};

		e.setLabels(input);

		@SuppressWarnings("unchecked")
        List<String> list = (List<String>) e.getMetadata().get("keywords");

		assertNotNull(list);
		assertArrayEquals(input, list.toArray(new String[0]));
		assertArrayEquals(input,e.getLabels());
	}

}
