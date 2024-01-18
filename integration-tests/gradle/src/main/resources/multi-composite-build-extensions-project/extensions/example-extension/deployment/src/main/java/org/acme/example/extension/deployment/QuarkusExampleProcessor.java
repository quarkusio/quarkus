package org.acme.example.extension.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import org.acme.liba.LibA;
import org.jboss.jandex.DotName;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.arc.processor.DotNames;





class QuarkusExampleProcessor {

	private static final String FEATURE = "example";

	@BuildStep
	FeatureBuildItem feature() {
		return new FeatureBuildItem(FEATURE);
	}

	@BuildStep
	void addLibABean(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
		additionalBeans.produce(new AdditionalBeanBuildItem.Builder()
				.addBeanClasses(LibA.class)
				.setUnremovable()
				.setDefaultScope(DotNames.APPLICATION_SCOPED)
				.build());
	}

}
