package org.acme.anotherExample.extension.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import org.acme.libb.LibB;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.arc.processor.DotNames;





class QuarkusAnotherExampleProcessor {

	private static final String FEATURE = "another-example";

	@BuildStep
	FeatureBuildItem feature() {
		return new FeatureBuildItem(FEATURE);
	}

	@BuildStep
	void addLibABean(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
		additionalBeans.produce(new AdditionalBeanBuildItem.Builder()
				.addBeanClasses(LibB.class)
				.setUnremovable()
				.setDefaultScope(DotNames.APPLICATION_SCOPED)
				.build());
	}

}
