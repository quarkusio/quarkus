package org.jboss.shamrock.forge.commands;

import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import javax.inject.Inject;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public abstract class AbstractShamrockCommand extends AbstractProjectCommand {

    @Inject
    protected FacetFactory factory;

    @Inject
    protected ProjectFactory projectFactory;

    public abstract String name();

    public String description() {
        return "";
    }

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(this.getClass())
            .name(name())
            .description(description())
            .category(Categories.create(category()));
    }

    public String category() {
        return "shamrock";
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }
}
