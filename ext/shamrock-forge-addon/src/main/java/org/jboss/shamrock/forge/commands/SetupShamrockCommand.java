package org.jboss.shamrock.forge.commands;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.shamrock.forge.ShamrockFacet;

import javax.inject.Inject;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class SetupShamrockCommand extends AbstractShamrockCommand {

    @Inject
    @WithAttributes(shortName = 'v', label = "Shamrock version", type = InputType.DROPDOWN)
    private UISelectOne<String> version;

    @Override
    public void initializeUI(UIBuilder uiBuilder) throws Exception {
        uiBuilder.add(version);

        version.setDefaultValue(() -> ShamrockFacet.SHAMROCK_VERSION);
    }

    @Override
    public Result execute(UIExecutionContext uiExecutionContext) throws Exception {
        Project selectedProject = getSelectedProject(uiExecutionContext.getUIContext());
        ShamrockFacet facet = factory.create(selectedProject, ShamrockFacet.class);
        factory.install(selectedProject, facet);
        return Results.success("Shamrock project created successfully");
    }

    @Override
    public String name() {
        return "shamrock-setup";
    }
}
