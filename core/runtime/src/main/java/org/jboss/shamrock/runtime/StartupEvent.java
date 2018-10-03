package org.jboss.shamrock.runtime;

/**
 * Event class that is fired on startup.
 *
 * This is fired on main method execution after all startup code has run,
 * so can be used to start threads etc in native image mode
 *
 * TODO: make a real API.
 */
public class StartupEvent {
}
