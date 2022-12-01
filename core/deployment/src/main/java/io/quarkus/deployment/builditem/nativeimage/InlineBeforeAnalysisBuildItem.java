package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * If present, will force the addition of the {@code -H:+InlineBeforeAnalysis} flag during native image build
 */
public final class InlineBeforeAnalysisBuildItem extends MultiBuildItem {
}
