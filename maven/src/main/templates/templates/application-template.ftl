<#if packageName??>
package ${packageName};
</#if>

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("${root_prefix}")
public class ShamrockApplication extends Application {

}
