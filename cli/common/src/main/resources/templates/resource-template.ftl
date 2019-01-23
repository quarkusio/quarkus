<#if package_name??>
package ${package_name};
</#if>

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("${path}")
<<<<<<< HEAD:maven/src/main/templates/templates/resource-template.ftl
public class ${class_name} {
=======
public class ${className} {
>>>>>>> merge cli branch to master:cli/common/src/main/resources/templates/resource-template.ftl

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}
