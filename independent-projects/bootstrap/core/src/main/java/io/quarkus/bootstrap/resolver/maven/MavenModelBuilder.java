/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.bootstrap.resolver.maven;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.Result;
import org.apache.maven.model.resolution.WorkspaceModelResolver;
import org.apache.maven.model.validation.ModelValidator;

/**
 *
 * @author Alexey Loubyansky
 */
public class MavenModelBuilder implements ModelBuilder {

    private final ModelBuilder builder;
    private final WorkspaceModelResolver modelResolver;

    public MavenModelBuilder(WorkspaceModelResolver wsModelResolver) {
        builder = new DefaultModelBuilderFactory().newInstance()
                .setModelValidator(new ModelValidator() {
                    @Override
                    public void validateRawModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
                    }

                    @Override
                    public void validateEffectiveModel(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
                    }
                });

        modelResolver = wsModelResolver;
    }

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request) throws ModelBuildingException {
        if(modelResolver != null) {
            request.setWorkspaceModelResolver(modelResolver);
        }
        return builder.build(request);
    }

    @Override
    public ModelBuildingResult build(ModelBuildingRequest request, ModelBuildingResult result) throws ModelBuildingException {
        return builder.build(request, result);
    }

    @Override
    public Result<? extends Model> buildRawModel(File pomFile, int validationLevel, boolean locationTracking) {
        return builder.buildRawModel(pomFile, validationLevel, locationTracking);
    }

}
