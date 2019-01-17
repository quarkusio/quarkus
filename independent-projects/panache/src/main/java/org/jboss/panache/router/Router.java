package org.jboss.panache.router;

import java.lang.reflect.Method;
import java.net.URI;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.core.ResteasyContext;

public class Router {

	public static <Target> URI getURI(Method0<Target> method, Object... params) {
		return findURI(method, params);
	}

	public static <Target,P1> URI getURI(Method1<Target,P1> method, Object... params) {
		return findURI(method, params);
	}

	public static <Target,P1,P2> URI getURI(Method2<Target,P1,P2> method, Object... params) {
		return findURI(method, params);
	}

	public static <Target,P1,P2,P3> URI getURI(Method3<Target,P1,P2,P3> method, Object... params) {
		return findURI(method, params);
	}

	public static <Target,P1,P2,P3,P4> URI getURI(Method4<Target,P1,P2,P3,P4> method, Object... params) {
		return findURI(method, params);
	}

	public static <Target,P1,P2,P3,P4,P5> URI getURI(Method5<Target,P1,P2,P3,P4,P5> method, Object... params) {
		return findURI(method, params);
	}
	
	public static <Target,P1,P2,P3,P4,P5,P6> URI getURI(Method6<Target,P1,P2,P3,P4,P5,P6> method, Object... params) {
		return findURI(method, params);
	}
	
	public static <Target,P1,P2,P3,P4,P5,P6,P7> URI getURI(Method7<Target,P1,P2,P3,P4,P5,P6,P7> method, Object... params) {
		return findURI(method, params);
	}
	
	public static <Target,P1,P2,P3,P4,P5,P6,P7,P8> URI getURI(Method8<Target,P1,P2,P3,P4,P5,P6,P7,P8> method, Object... params) {
		return findURI(method, params);
	}
	
	public static <Target,P1,P2,P3,P4,P5,P6,P7,P8,P9> URI getURI(Method9<Target,P1,P2,P3,P4,P5,P6,P7,P8,P9> method, Object... params) {
		return findURI(method, params);
	}
	
	public static <Target,P1,P2,P3,P4,P5,P6,P7,P8,P9,P10> URI getURI(Method10<Target,P1,P2,P3,P4,P5,P6,P7,P8,P9,P10> method, Object... params) {
		return findURI(method, params);
	}

	static URI findURI(MethodFinder method, Object[] params, Class<?> klass, String name, Class<?>... parameterTypes) {
	    try {
	        // FIXME: we can probably do that without reflection
            Method m = klass.getMethod(name, parameterTypes);
            return findURI(m, params);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
	}
	   
	private static URI findURI(Method m, Object[] params) {
        UriInfo uriInfo = ResteasyContext.getContextData(UriInfo.class);
        
        UriBuilder builder = uriInfo.getBaseUriBuilder().path(m.getDeclaringClass());
        if(m.isAnnotationPresent(Path.class))
            builder.path(m);
        return builder.build(params);
    }

    private static URI findURI(MethodFinder method, Object[] params) {
		Method m = method.method();
		return findURI(m, params);
	}
}
