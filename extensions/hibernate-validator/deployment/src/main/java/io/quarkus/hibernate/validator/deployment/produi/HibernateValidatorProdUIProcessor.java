package io.quarkus.hibernate.validator.deployment.produi;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.hibernate.validator.runtime.produi.HibernateValidatorProdUIRecorder;
import io.quarkus.hibernate.validator.runtime.produi.HibernateValidatorProdUIService;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;

/**
 * Contributes a read-only Prod UI page showing the Bean Validation constraint
 * metadata (validated classes and their class-level and per-property
 * constraints). There is no Dev UI data page to reuse - the Hibernate Validator
 * Dev UI card only links to library documentation - so a bespoke read-only
 * component + service is provided. The service reads the constraint metadata from
 * the always-present {@code Validator} bean; the set of validated classes is
 * discovered at build time and seeded into the service at runtime init.
 */
public class HibernateValidatorProdUIProcessor {

    // Produced from a zero-input build step: the provider bean registration in the
    // produi extension must not (transitively) depend on Arc/deployment items, or a
    // build-step cycle results. The page step below keeps the validated-class gating.
    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(HibernateValidatorProdUIService.class);
    }

    @BuildStep
    void createProdUIPage(ValidatedClassNamesBuildItem validatedClasses,
            BuildProducer<ProdUIPageBuildItem> prodUIProducer) {
        if (validatedClasses.getClassNames().isEmpty()) {
            return;
        }

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Constraints")
                .icon("font-awesome-solid:shield-halved")
                .componentLink("pwc-hibernate-validator.js"));
        prodUIProducer.produce(page);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void initializeProdUIService(ValidatedClassNamesBuildItem validatedClasses,
            HibernateValidatorProdUIRecorder recorder) {
        if (validatedClasses.getClassNames().isEmpty()) {
            return;
        }
        List<String> classNames = new ArrayList<>(validatedClasses.getClassNames());
        recorder.initializeProdUIService(classNames);
    }
}
