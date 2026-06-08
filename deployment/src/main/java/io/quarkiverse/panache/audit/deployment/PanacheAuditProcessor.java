package io.quarkiverse.panache.audit.deployment;

import io.quarkiverse.panache.audit.runtime.CreatedByGenerator;
import io.quarkiverse.panache.audit.runtime.DefaultAuditUserProvider;
import io.quarkiverse.panache.audit.runtime.LastModifiedByGenerator;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class PanacheAuditProcessor {

    private static final String FEATURE = "panache-audit";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerAuditBeans() {
        return AdditionalBeanBuildItem.unremovableOf(DefaultAuditUserProvider.class);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> producer) {
        producer.produce(ReflectiveClassBuildItem.builder(
                CreatedByGenerator.class.getName(),
                LastModifiedByGenerator.class.getName())
                .constructors(true)
                .methods(true)
                .fields(true)
                .build());
    }
}
