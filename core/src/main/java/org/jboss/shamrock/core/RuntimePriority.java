package org.jboss.shamrock.core;

/**
 * TODO: delete this class, we don't want to replicate the Phase.java mess that is in WF
 */
public class RuntimePriority {

    public static final int UNDERTOW_CREATE_DEPLOYMENT = 100;
    public static final int UNDERTOW_REGISTER_SERVLET = 200;
    public static final int UNDERTOW_START = 300;

}
