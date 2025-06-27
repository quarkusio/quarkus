package io.quarkus.oidc;

import java.util.Map;

import jakarta.enterprise.event.Event;

import io.quarkus.arc.Arc;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.BackChannelLogoutHandler.NewBackChannelLogoutPath;
import io.quarkus.oidc.runtime.OidcTenantConfig.Logout;
import io.quarkus.oidc.runtime.TenantConfigContext;

public final class LogoutUtils {

    private static final String FORM_POST_LOGOUT_START = "<html>"
            + "   <head><title>Logout Form</title></head>"
            + "   <body onload=\"javascript:document.forms[0].submit()\">"
            + "    <form method=\"post\" action=\"";
    private static final String FORM_POST_LOGOUT_END = "    </form></body></html>";

    private LogoutUtils() {

    }

    public static String createFormPostLogout(Logout logoutConfig, String logoutUrl,
            String idToken, String postLogoutUrl, String postLogoutState) {
        StringBuilder sb = new StringBuilder();
        sb.append(FORM_POST_LOGOUT_START);
        sb.append(logoutUrl).append("\">");
        if (idToken != null) {
            addInput(sb, OidcConstants.LOGOUT_ID_TOKEN_HINT, idToken);
        }
        if (postLogoutUrl != null) {
            addInput(sb, logoutConfig.postLogoutUriParam(), postLogoutUrl);
        }
        if (postLogoutState != null) {
            addInput(sb, OidcConstants.LOGOUT_STATE, postLogoutState);
        }

        Map<String, String> extraParams = logoutConfig.extraParams();
        if (extraParams != null) {
            for (Map.Entry<String, String> entry : extraParams.entrySet()) {
                addInput(sb, entry.getKey(), entry.getValue());
            }
        }
        sb.append(FORM_POST_LOGOUT_END);
        return sb.toString();
    }

    private static void addInput(StringBuilder sb, String name, String value) {
        sb.append("<input type=\"hidden\" name=\"").append(name).append("\" ")
                .append("value=\"").append(value).append("\"")
                .append("/>");
    }

    public static void fireBackChannelLogoutChangedEvent(OidcTenantConfig oidcConfig, TenantConfigContext tenant) {
        if (oidcConfig.logout().backchannel().path().isPresent()) {
            boolean pathChanged = tenant.oidcConfig() == null || !oidcConfig.logout().backchannel().path().get()
                    .equals(tenant.oidcConfig().logout().backchannel().path().orElse(null));
            if (pathChanged) {
                fireBackChannelLogoutEvent();
            }
        }

    }

    public static void fireBackChannelLogoutReadyEvent(OidcTenantConfig oidcConfig) {
        if (oidcConfig.logout().backchannel().path().isPresent()) {
            fireBackChannelLogoutEvent();
        }

    }

    private static void fireBackChannelLogoutEvent() {
        Event<NewBackChannelLogoutPath> event = Arc.container().beanManager().getEvent()
                .select(NewBackChannelLogoutPath.class);
        event.fire(new NewBackChannelLogoutPath());
    }

}
