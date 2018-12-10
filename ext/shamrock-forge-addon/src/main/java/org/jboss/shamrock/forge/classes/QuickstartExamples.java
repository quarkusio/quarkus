package org.jboss.shamrock.forge.classes;

import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class QuickstartExamples {

    private static final String lineSeparator = System.getProperty("line.separator");

    @Inject
    private ResourceFactory resourceFactory;

    public String createNewApplication(Project project, String packageName, String applicationClassName) {
        return addMyApplicationClass(project, packageName, applicationClassName);
    }

    private String addMyApplicationClass(Project project, String packageName, String className) {

        JavaSourceFacet source = project.getFacet(JavaSourceFacet.class);

        String topLevelPackage;
        if (packageName == null) {
            topLevelPackage = source.getBasePackage();
        }
        else {
            topLevelPackage = packageName;
        }

        if (className.endsWith(".java")) {
            className = className.substring(0, className.length() - ".java".length());
        }

        JavaClassSource mainApp = Roaster.create(JavaClassSource.class)
                                               .setPackage(topLevelPackage)
                                               .setAbstract(false)
                                               .setName(className)
                                               .setSuperType("Application");
        mainApp.addImport("javax.ws.rs.ApplicationPath");
        mainApp.addImport("javax.ws.rs.core.Application");
        mainApp.addAnnotation("javax.ws.rs.ApplicationPath").setStringValue("/app");

       JavaResource resource = source.saveJavaSource(mainApp.getEnclosingType());

        addGreetingService(project, topLevelPackage);
        addGreetingResource(project, topLevelPackage);

        addTest(project, topLevelPackage, className);

       return resource.toString();
    }

    private String addGreetingService(Project project, String packageName) {
        JavaSourceFacet source = project.getFacet(JavaSourceFacet.class);

        JavaClassSource greetingService = Roaster.create(JavaClassSource.class)
                                               .setPackage(packageName)
                                               .setAbstract(false)
                                               .setName("GreetingService");
        greetingService.addImport("javax.enterprise.context.ApplicationScoped");
        greetingService.addAnnotation("javax.enterprise.context.ApplicationScoped");

        greetingService.addMethod()
                .setName("greeting")
                .setBody("return \"hello \"+name;")
                .setPublic()
                .setReturnType("String")
                .addParameter("String", "name");

       JavaResource resource = source.saveJavaSource(greetingService.getEnclosingType());


       return resource.toString();
    }

    private String addGreetingResource(Project project, String packageName) {
        JavaSourceFacet source = project.getFacet(JavaSourceFacet.class);

        JavaClassSource greetingResource = Roaster.create(JavaClassSource.class)
                                               .setPackage(packageName)
                                               .setAbstract(false)
                                               .setName("GreetingResource");
        greetingResource.addImport("javax.inject.Inject");
        greetingResource.addImport("javax.ws.rs.GET");
        greetingResource.addImport("javax.ws.rs.Path");
        greetingResource.addImport("javax.ws.rs.PathParam");
        greetingResource.addImport("javax.ws.rs.Produces");
        greetingResource.addImport("javax.ws.rs.core.MediaType");

        greetingResource.addAnnotation("javax.ws.rs.Path").setStringValue("/hello");

        greetingResource.addField().setType("GreetingService").setName("service").addAnnotation("javax.inject.Inject");

        greetingResource.addMethod()
                .setName("greeting")
                .setBody("return service.greeting(name);")
                .setPublic()
                .setReturnType("String");

        greetingResource.getMethod("greeting").addAnnotation("javax.ws.rs.GET");
        greetingResource.getMethod("greeting").addAnnotation("javax.ws.rs.Produces").setLiteralValue("MediaType.TEXT_PLAIN");
        greetingResource.getMethod("greeting").addAnnotation("javax.ws.rs.Path").setStringValue("/greeting/{name}");
        greetingResource.getMethod("greeting").addParameter("@PathParam(\"name\") String", "name");

        greetingResource.addMethod()
                .setName("hello")
                .setBody("return \"hello\";")
                .setPublic()
                .setReturnType("String");

        greetingResource.getMethod("hello").addAnnotation("javax.ws.rs.GET");
        greetingResource.getMethod("hello").addAnnotation("javax.ws.rs.Produces").setLiteralValue("MediaType.TEXT_PLAIN");

       JavaResource resource = source.saveJavaSource(greetingResource.getEnclosingType());

       return resource.toString();
    }

    private String addTest(Project project, String packageName, String className) {

        JavaSourceFacet source = project.getFacet(JavaSourceFacet.class);

        final String testClassName = className+"Test";


        final List<String> testImports = new ArrayList<String>() {{
            add("org.jboss.shamrock.test.ShamrockTest");
            add("org.junit.Test");
            add("org.junit.runner.RunWith");
            add("java.util.UUID");
        }};

        JavaClassSource testClass = Roaster.create(JavaClassSource.class)
                                            .setPackage(packageName)
                                            .setAbstract(false)
                                            .setName(testClassName);
        testImports.forEach(testClass::addImport);

        testClass.addImport("io.restassured.RestAssured.given").setStatic(true);
        testClass.addImport("org.hamcrest.CoreMatchers.is").setStatic(true);

        testClass.addAnnotation()
                .setName("RunWith")
                .setLiteralValue("ShamrockTest.class");

        MethodSource testHelloEndpoint = testClass
            .addMethod()
            .setName("testHelloEndpoint")
            .setBody(
                    "given().when().get(\"app/hello\")"+lineSeparator+
                     ".then()"+lineSeparator+
                     ".statusCode(200)"+lineSeparator+
                     ".body(is(\"hello\"));")
            .setPublic()
            .setReturnTypeVoid();

        testHelloEndpoint.addAnnotation("Test");

        MethodSource testGreetingEndpoint = testClass
            .addMethod()
            .setName("testGreetingEndpoint")
            .setBody("String uuid = UUID.randomUUID().toString();"+lineSeparator+
                     "given()"+lineSeparator+
                     ".pathParam(\"name\", uuid)"+lineSeparator+
                     ".when().get(\"app/hello/greeting/{name}\")"+lineSeparator+
                     ".then()"+lineSeparator+
                     ".statusCode(200)"+lineSeparator+
                     ".body(is(\"hello \" + uuid));")
            .setPublic()
            .setReturnTypeVoid();

        testGreetingEndpoint.addAnnotation("Test");


        source.saveTestJavaSource(testClass.getEnclosingType());

        return source.toString();
    }

}
