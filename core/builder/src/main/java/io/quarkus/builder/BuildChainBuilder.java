package io.quarkus.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.qlue.ChainBuilder;

/**
 * A build chain builder.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildChainBuilder {

    private final ChainBuilder chainBuilder;
    private final List<BuildProvider> providers = new ArrayList<>();

    BuildChainBuilder(final ChainBuilder chainBuilder) {
        this.chainBuilder = chainBuilder;
    }

    /**
     * Add a build step to the chain. The configuration in the build step builder at the time that the chain is built is
     * the configuration that will apply to the build step in the final chain. Any subsequent changes will be ignored.
     * <p>
     * A given build step is included in the chain when one or more of the following criteria are met:
     * <ul>
     * <li>It includes a pre-produce step for a item which is produced by one or more build steps that is included in the
     * chain</li>
     * <li>It includes a produce step for a item which is consumed by a build step that is included in the chain or is a final
     * item</li>
     * <li>It includes a consume step for a item which is produced by a build step that is included in the chain or is an
     * initial item</li>
     * <li>It includes a destroy step for a item which is produced by a build step that is included in the chain or is an
     * initial item</li>
     * </ul>
     * In addition, the declaration of producers and consumers can cause corresponding consumers and producers to be
     * included if they exist.
     *
     * @param buildStep the build step instance
     * @return the builder for the build step
     */
    public BuildStepBuilder addBuildStep(BuildStep buildStep) {
        return new BuildStepBuilder(this, chainBuilder.addRawStep(sc -> buildStep.execute(new BuildContext(sc))));
    }

    /**
     * This deprecated method is no longer supported.
     *
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    public BuildStepBuilder addBuildStep() {
        throw Assert.unsupported();
    }

    /**
     * Declare an initial item that will be provided to build steps in the chain. Note that if this method is called
     * for a simple item, no build steps will be allowed to produce that item.
     *
     * @param type the item type (must not be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if the item type is {@code null}
     */
    public BuildChainBuilder addInitial(Class<? extends BuildItem> type) {
        Assert.checkNotNullParam("type", type);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            chainBuilder.addInitial(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class));
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            chainBuilder.addInitial(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class));
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            chainBuilder.addInitial(LegacyEmptyItem.class, type.asSubclass(EmptyBuildItem.class));
        }
        return this;
    }

    public BuildChainBuilder loadProviders(ClassLoader classLoader) throws ChainBuildException {
        final ServiceLoader<BuildProvider> serviceLoader = ServiceLoader.load(BuildProvider.class, classLoader);
        for (final BuildProvider provider : serviceLoader) {
            provider.installInto(this);
        }
        return this;
    }

    /**
     * Declare a final item that will be consumable after the build step chain completes. This may be any item
     * that is produced in the chain.
     *
     * @param type the item type (must not be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if the item type is {@code null}
     */
    public BuildChainBuilder addFinal(Class<? extends BuildItem> type) {
        Assert.checkNotNullParam("type", type);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            chainBuilder.addFinal(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class));
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            chainBuilder.addFinal(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class));
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            chainBuilder.addFinal(LegacyEmptyItem.class, type.asSubclass(EmptyBuildItem.class));
        }
        return this;
    }

    /**
     * Sets the ClassLoader for the build. Every build step will be run with this as the TCCL.
     *
     * @param classLoader The ClassLoader
     */
    public void setClassLoader(ClassLoader classLoader) {
        chainBuilder.setClassLoader(classLoader);
    }

    /**
     * Get the underlying chain builder.
     *
     * @return the underlying chain builder
     */
    public ChainBuilder getChainBuilder() {
        return chainBuilder;
    }

    /**
     * Build the build step chain from the current builder configuration.
     *
     * @return the constructed build chain
     * @throws ChainBuildException if the chain could not be built
     */
    public BuildChain build() throws ChainBuildException {
        try {
            return new BuildChain(chainBuilder.build(), providers);
        } catch (io.quarkus.qlue.ChainBuildException e) {
            throw new ChainBuildException(e);
        }
    }
}
