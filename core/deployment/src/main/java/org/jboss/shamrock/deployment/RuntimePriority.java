package org.jboss.shamrock.deployment;

/**
 * Static list of priorities
 *
 * TODO: delete this class, we don't want to replicate the Phase.java mess that is in WF
 */
public class RuntimePriority {

    public static final int UNDERTOW_CREATE_DEPLOYMENT = 100;
    public static final int UNDERTOW_REGISTER_SERVLET = 200;
    public static final int HEALTH_DEPLOYMENT = 260;
    public static final int WELD_DEPLOYMENT = 300;
    public static final int JAXRS_DEPLOYMENT = 350;
    public static final int ARC_DEPLOYMENT = 300;
    public static final int UNDERTOW_DEPLOY = 400;
    public static final int UNDERTOW_START = 500;

}
