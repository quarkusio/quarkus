<#if packageName??>
package ${packageName};
</#if>

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("${docRoot}")
public class MyApplication extends Application {

}
