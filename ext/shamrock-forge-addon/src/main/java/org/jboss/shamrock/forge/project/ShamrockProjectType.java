package org.jboss.shamrock.forge.project;

import org.jboss.forge.addon.projects.AbstractProjectType;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.shamrock.forge.ShamrockFacet;
import org.jboss.shamrock.forge.commands.SetupShamrockCommand;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class ShamrockProjectType extends AbstractProjectType {

    @Override
    public Iterable<Class<? extends ProjectFacet>> getRequiredFacets() {
        return Collections.singleton(ShamrockFacet.class);
    }

    @Override
    public NavigationResult next(UINavigationContext context) {
        NavigationResultBuilder builder = NavigationResultBuilder.create();
        builder.add(
                Metadata.forCommand(SetupShamrockCommand.class).name("Shamrock: Setup").description("Setup Shamrock")
                        .category(Categories.create("shamrock")),
                Arrays.asList(SetupShamrockCommand.class));
        return builder.build();
    }

    @Override
    public String getType() {
        return "Shamrock";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public String toString() {
        return "shamrock";
    }

}
