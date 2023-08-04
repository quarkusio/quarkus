package io.quarkus.arc.test.profile;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.profile.IfBuildProfile;

@ApplicationScoped
@IfBuildProfile(value = "prod")
public class AdditionalIfBuildProfileBean {

}
