package io.quarkus.it.validator;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.Size;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/validator")
public class TestValidatorEndpoint {

    @Inject
    Validator validator;

    @POST
    @Path("/manual")
    @Consumes("application/json")
    public String manualValidation(MyData data) {
        Set<ConstraintViolation<MyData>> result = validator.validate(data);
        if (result.isEmpty()) {
            return "passed";
        }
        return "failed:" + result.iterator().next().getPropertyPath().toString();
    }

    public static class MyData {
        private String name;
        @Size(min = 3)
        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

}
