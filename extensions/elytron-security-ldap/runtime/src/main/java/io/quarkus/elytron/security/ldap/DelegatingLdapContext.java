package io.quarkus.elytron.security.ldap;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ReferralException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.net.SocketFactory;

import org.wildfly.security.auth.realm.ldap.ThreadLocalSSLSocketFactory;

import io.smallrye.common.constraint.Assert;

class DelegatingLdapContext implements LdapContext {

    private final DirContext delegating;
    private final CloseHandler closeHandler;
    private final SocketFactory socketFactory;

    interface CloseHandler {
        void handle(DirContext context) throws NamingException;
    }

    DelegatingLdapContext(DirContext delegating, CloseHandler closeHandler, SocketFactory socketFactory)
            throws NamingException {
        this.delegating = delegating;
        this.closeHandler = closeHandler;
        this.socketFactory = socketFactory;
    }

    // for needs of newInstance()
    private DelegatingLdapContext(DirContext delegating, SocketFactory socketFactory) {
        this.delegating = delegating;
        this.closeHandler = null; // close handler should not be applied to copy
        this.socketFactory = socketFactory;
    }

    public LdapContext newInitialLdapContext(Hashtable<?, ?> environment, Control[] connCtls) throws NamingException {
        ClassLoader previous = setSocketFactory();
        try {
            return new InitialLdapContext(environment, null);
        } finally {
            unsetSocketFactory(previous);
        }
    }

    @Override
    public void close() throws NamingException {
        if (closeHandler == null) {
            delegating.close();
        } else {
            closeHandler.handle(delegating);
        }
    }

    // for needs of search()
    private NamingEnumeration<SearchResult> wrap(NamingEnumeration<SearchResult> delegating) {
        return new NamingEnumeration<SearchResult>() {

            @Override
            public boolean hasMoreElements() {
                ClassLoader previous = setSocketFactory();
                try {
                    return delegating.hasMoreElements();
                } finally {
                    unsetSocketFactory(previous);
                }
            }

            @Override
            public SearchResult nextElement() {
                ClassLoader previous = setSocketFactory();
                try {
                    return delegating.nextElement();
                } finally {
                    unsetSocketFactory(previous);
                }
            }

            @Override
            public SearchResult next() throws NamingException {
                ClassLoader previous = setSocketFactory();
                try {
                    return delegating.next();
                } finally {
                    unsetSocketFactory(previous);
                }
            }

            @Override
            public boolean hasMore() throws NamingException {
                ClassLoader previous = setSocketFactory();
                try {
                    return delegating.hasMore();
                } finally {
                    unsetSocketFactory(previous);
                }
            }

            @Override
            public void close() throws NamingException {
                delegating.close();
            }
        };
    }

    public DelegatingLdapContext wrapReferralContextObtaining(ReferralException e) throws NamingException {
        ClassLoader previous = setSocketFactory();
        try {
            return new DelegatingLdapContext((DirContext) e.getReferralContext(), socketFactory);
        } finally {
            unsetSocketFactory(previous);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "->" + delegating.toString();
    }

    // LdapContext specific

    @Override
    public ExtendedResponse extendedOperation(ExtendedRequest request) throws NamingException {
        if (!(delegating instanceof LdapContext))
            throw Assert.unsupported();
        return ((LdapContext) delegating).extendedOperation(request);
    }

    @Override
    public LdapContext newInstance(Control[] requestControls) throws NamingException {
        if (!(delegating instanceof LdapContext))
            throw Assert.unsupported();
        LdapContext newContext = ((LdapContext) delegating).newInstance(requestControls);
        return new DelegatingLdapContext(newContext, socketFactory);
    }

    @Override
    public void reconnect(Control[] controls) throws NamingException {
        if (!(delegating instanceof LdapContext))
            throw Assert.unsupported();
        ClassLoader previous = setSocketFactory();
        try {
            ((LdapContext) delegating).reconnect(controls);
        } finally {
            unsetSocketFactory(previous);
        }
    }

    @Override
    public Control[] getConnectControls() throws NamingException {
        if (!(delegating instanceof LdapContext))
            throw Assert.unsupported();
        return ((LdapContext) delegating).getConnectControls();
    }

    @Override
    public void setRequestControls(Control[] requestControls) throws NamingException {
        if (!(delegating instanceof LdapContext))
            throw Assert.unsupported();
        ((LdapContext) delegating).setRequestControls(requestControls);
    }

    @Override
    public Control[] getRequestControls() throws NamingException {
        if (!(delegating instanceof LdapContext))
            throw Assert.unsupported();
        return ((LdapContext) delegating).getRequestControls();
    }

    @Override
    public Control[] getResponseControls() throws NamingException {
        if (!(delegating instanceof LdapContext))
            throw Assert.unsupported();
        return ((LdapContext) delegating).getResponseControls();
    }

    // DirContext methods delegates only

    @Override
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        delegating.bind(name, obj, attrs);
    }

    @Override
    public Attributes getAttributes(Name name) throws NamingException {
        return delegating.getAttributes(name);
    }

    @Override
    public Attributes getAttributes(String name) throws NamingException {
        return delegating.getAttributes(name);
    }

    @Override
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        return delegating.getAttributes(name, attrIds);
    }

    @Override
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        return delegating.getAttributes(name, attrIds);
    }

    @Override
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        delegating.modifyAttributes(name, mod_op, attrs);
    }

    @Override
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        delegating.modifyAttributes(name, mod_op, attrs);
    }

    @Override
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        delegating.modifyAttributes(name, mods);
    }

    @Override
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        delegating.modifyAttributes(name, mods);
    }

    @Override
    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        delegating.bind(name, obj, attrs);
    }

    @Override
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        delegating.rebind(name, obj, attrs);
    }

    @Override
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        delegating.rebind(name, obj, attrs);
    }

    @Override
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        return delegating.createSubcontext(name, attrs);
    }

    @Override
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        return delegating.createSubcontext(name, attrs);
    }

    @Override
    public DirContext getSchema(Name name) throws NamingException {
        return delegating.getSchema(name);
    }

    @Override
    public DirContext getSchema(String name) throws NamingException {
        return delegating.getSchema(name);
    }

    @Override
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        return delegating.getSchemaClassDefinition(name);
    }

    @Override
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        return delegating.getSchemaClassDefinition(name);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn)
            throws NamingException {
        return wrap(delegating.search(name, matchingAttributes, attributesToReturn));
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn)
            throws NamingException {
        return wrap(delegating.search(name, matchingAttributes, attributesToReturn));
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
        return wrap(delegating.search(name, matchingAttributes));
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        return wrap(delegating.search(name, matchingAttributes));
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
        return wrap(delegating.search(name, filter, cons));
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        return wrap(delegating.search(name, filter, cons));
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons)
            throws NamingException {
        return wrap(delegating.search(name, filterExpr, filterArgs, cons));
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons)
            throws NamingException {
        return wrap(delegating.search(name, filterExpr, filterArgs, cons));
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        return delegating.lookup(name);
    }

    @Override
    public Object lookup(String name) throws NamingException {
        return delegating.lookup(name);
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        delegating.bind(name, obj);
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        delegating.bind(name, obj);
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        delegating.rebind(name, obj);
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        delegating.rebind(name, obj);
    }

    @Override
    public void unbind(Name name) throws NamingException {
        delegating.unbind(name);
    }

    @Override
    public void unbind(String name) throws NamingException {
        delegating.unbind(name);
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        delegating.rename(oldName, newName);
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {
        delegating.rename(oldName, newName);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return delegating.list(name);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return delegating.list(name);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return delegating.listBindings(name);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return delegating.listBindings(name);
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        delegating.destroySubcontext(name);
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        delegating.destroySubcontext(name);
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        return delegating.createSubcontext(name);
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        return delegating.createSubcontext(name);
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        return delegating.lookupLink(name);
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        return delegating.lookupLink(name);
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return delegating.getNameParser(name);
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return delegating.getNameParser(name);
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        return delegating.composeName(name, prefix);
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return delegating.composeName(name, prefix);
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return delegating.addToEnvironment(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return delegating.removeFromEnvironment(propName);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return delegating.getEnvironment();
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return delegating.getNameInNamespace();
    }

    private ClassLoader setSocketFactory() {
        if (socketFactory != null) {
            ThreadLocalSSLSocketFactory.set(socketFactory);
            return setClassLoaderTo(getSocketFactoryClassLoader());
        }
        return null;
    }

    private void unsetSocketFactory(ClassLoader previous) {
        if (socketFactory != null) {
            ThreadLocalSSLSocketFactory.unset();
            setClassLoaderTo(previous);
        }
    }

    private ClassLoader getSocketFactoryClassLoader() {
        return ThreadLocalSSLSocketFactory.class.getClassLoader();
    }

    private ClassLoader setClassLoaderTo(final ClassLoader targetClassLoader) {
        final Thread currentThread = Thread.currentThread();
        final ClassLoader original = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(targetClassLoader);
        return original;
    }

}
