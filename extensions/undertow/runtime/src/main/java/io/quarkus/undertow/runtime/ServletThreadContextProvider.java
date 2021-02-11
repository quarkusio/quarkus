package io.quarkus.undertow.runtime;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.undertow.servlet.handlers.ServletRequestContext;

public class ServletThreadContextProvider implements ThreadContextProvider {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        ServletRequestContext captured = ServletRequestContext.current();
        return () -> {
            ServletRequestContext current = restore(captured);
            return () -> restore(current);
        };
    }

    private ServletRequestContext restore(ServletRequestContext context) {
        ServletRequestContext currentContext = ServletRequestContext.current();
        if (context == null)
            ServletRequestContext.clearCurrentServletAttachments();
        else
            ServletRequestContext.setCurrentRequestContext(context);
        return currentContext;
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return () -> {
            ServletRequestContext current = restore(null);
            return () -> restore(current);
        };
    }

    @Override
    public String getThreadContextType() {
        return "Servlet";
    }
}
