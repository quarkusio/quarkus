/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logmanager;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;

/**
 * A logging context, for producing isolated logging environments.
 */
@SuppressWarnings("unused")
public final class LogContext {
    static final EmbeddedConfigurator CONFIGURATOR = getConfigurator();

    private static final LogContext INSTANCE = new LogContext();

    private final LoggerNode rootLogger;

    private static final HashMap<String, Level> LEVEL_MAP;

    private static void add(Map<String, Level> map, Level level) {
        map.put(level.getName().toUpperCase(Locale.US), level);
    }

    static {
        final HashMap<String, Level> map = new HashMap<>();
        add(map, Level.OFF);
        add(map, Level.ALL);
        add(map, Level.SEVERE);
        add(map, Level.WARNING);
        add(map, Level.CONFIG);
        add(map, Level.INFO);
        add(map, Level.FINE);
        add(map, Level.FINER);
        add(map, Level.FINEST);

        add(map, org.jboss.logmanager.Level.FATAL);
        add(map, org.jboss.logmanager.Level.ERROR);
        add(map, org.jboss.logmanager.Level.WARN);
        add(map, org.jboss.logmanager.Level.INFO);
        add(map, org.jboss.logmanager.Level.DEBUG);
        add(map, org.jboss.logmanager.Level.TRACE);

        LEVEL_MAP = map;
    }

    // Guarded by treeLock
    private final Set<AutoCloseable> closeHandlers;

    /**
     * This lock is taken any time a change is made which affects multiple nodes in the hierarchy.
     */
    final Object treeLock = new Object();

    LogContext() {
        rootLogger = new LoggerNode(this);
        closeHandlers = new LinkedHashSet<>();
    }

    private static LogContext create() {
        return new LogContext();
    }

    // Attachment mgmt

    /**
     * Get the attachment value for a given key, or {@code null} if there is no such attachment.
     * Log context attachments are placed on the root logger and can also be accessed there.
     *
     * @param key the key
     * @param <V> the attachment value type
     * @return the attachment, or {@code null} if there is none for this key
     */
    @SuppressWarnings({ "unchecked" })
    public <V> V getAttachment(Logger.AttachmentKey<V> key) {
        return rootLogger.getAttachment(key);
    }

    /**
     * Attach an object to this log context under a given key.
     * A strong reference is maintained to the key and value for as long as this log context exists.
     * Log context attachments are placed on the root logger and can also be accessed there.
     *
     * @param key the attachment key
     * @param value the attachment value
     * @param <V> the attachment value type
     * @return the old attachment, if there was one
     * @throws SecurityException if a security manager exists and if the caller does not have {@code LoggingPermission(control)}
     */
    public <V> V attach(Logger.AttachmentKey<V> key, V value) throws SecurityException {
        return rootLogger.attach(key, value);
    }

    /**
     * Attach an object to this log context under a given key, if such an attachment does not already exist.
     * A strong reference is maintained to the key and value for as long as this log context exists.
     * Log context attachments are placed on the root logger and can also be accessed there.
     *
     * @param key the attachment key
     * @param value the attachment value
     * @param <V> the attachment value type
     * @return the current attachment, if there is one, or {@code null} if the value was successfully attached
     * @throws SecurityException if a security manager exists and if the caller does not have {@code LoggingPermission(control)}
     */
    @SuppressWarnings({ "unchecked" })
    public <V> V attachIfAbsent(Logger.AttachmentKey<V> key, V value) throws SecurityException {
        return rootLogger.attachIfAbsent(key, value);
    }

    /**
     * Remove an attachment.
     * Log context attachments are placed on the root logger and can also be accessed there.
     *
     * @param key the attachment key
     * @param <V> the attachment value type
     * @return the old value, or {@code null} if there was none
     * @throws SecurityException if a security manager exists and if the caller does not have {@code LoggingPermission(control)}
     */
    @SuppressWarnings({ "unchecked" })
    public <V> V detach(Logger.AttachmentKey<V> key) throws SecurityException {
        return rootLogger.detach(key);
    }

    public static LogContext getLogContext() {
        return LogContext.getInstance();
    }

    /**
     * Get a logger with the given name from this logging context.
     *
     * @param name the logger name
     * @return the logger instance
     * @see java.util.logging.LogManager#getLogger(String)
     */
    public Logger getLogger(String name) {
        return rootLogger.getOrCreate(name).createLogger();
    }

    /**
     * Get a logger with the given name from this logging context, if a logger node exists at that location.
     *
     * @param name the logger name
     * @return the logger instance, or {@code null} if no such logger node exists
     */
    public Logger getLoggerIfExists(String name) {
        final LoggerNode node = rootLogger.getIfExists(name);
        return node == null ? null : node.createLogger();
    }

    /**
     * Get a logger attachment for a logger name, if it exists.
     *
     * @param loggerName the logger name
     * @param key the attachment key
     * @param <V> the attachment value type
     * @return the attachment or {@code null} if the logger or the attachment does not exist
     */
    public <V> V getAttachment(String loggerName, Logger.AttachmentKey<V> key) {
        final LoggerNode node = rootLogger.getIfExists(loggerName);
        if (node == null) return null;
        return node.getAttachment(key);
    }

    /**
     * Get the level for a name.
     *
     * @param name the name
     * @return the level
     * @throws IllegalArgumentException if the name is not known
     */
    public Level getLevelForName(String name) throws IllegalArgumentException {
        if (name != null) {
            final Level level = LEVEL_MAP.get(name);
            if (level != null) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown level \"" + name + "\"");
    }

    /**
     * Get the system log context.
     *
     * @return the system log context
     */
    public static LogContext getInstance() {
        return INSTANCE;
    }

    static EmbeddedConfigurator getConfigurator() {
        final ServiceLoader<EmbeddedConfigurator> configLoader = ServiceLoader.load(EmbeddedConfigurator.class, LogContext.class.getClassLoader());
        final Iterator<EmbeddedConfigurator> iterator = configLoader.iterator();
        for (;;) try {
            if (! iterator.hasNext()) {
                return EmbeddedConfigurator.EMPTY;
            }
            return iterator.next();
        } catch (ServiceConfigurationError | RuntimeException e) {
            System.err.print("Warning: failed to load configurator: ");
            e.printStackTrace(System.err);
        }
    }

    /**
     * Returns an enumeration of the logger names that have been created. This does not return names of loggers that
     * may have been garbage collected. Logger names added after the enumeration has been retrieved may also be added to
     * the enumeration.
     *
     * @return an enumeration of the logger names
     *
     * @see java.util.logging.LogManager#getLoggerNames()
     */
    public Enumeration<String> getLoggerNames() {
        final ArrayDeque<Iterator<LoggerNode>> nodeStack = new ArrayDeque<>();
        nodeStack.add(Collections.singleton(rootLogger).iterator());
        return new Enumeration<String>() {
            LoggerNode next;
            @Override
            public boolean hasMoreElements() {
                if (next != null) return true;
                while (! nodeStack.isEmpty()) {
                    final Iterator<LoggerNode> itr = nodeStack.peekFirst();
                    if (! itr.hasNext()) {
                        nodeStack.pollFirst();
                    } else {
                        final LoggerNode node = itr.next();
                        nodeStack.addLast(node.getChildren().iterator());
                        if (node.hasLogger()) {
                            next = node;
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public String nextElement() {
                if (!hasMoreElements()) {
                    throw new NoSuchElementException();
                }
                try {
                    return next.getFullName();
                } finally {
                    next = null;
                }
            }
        };
    }

}
