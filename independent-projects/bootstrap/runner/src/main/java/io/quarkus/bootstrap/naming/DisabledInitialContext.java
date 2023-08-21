package io.quarkus.bootstrap.naming;

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
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

/**
 * Initial context that won't allow you to actually do anything.
 * <p>
 * Used by Quarkus to disable JNDI.
 */
public class DisabledInitialContext extends InitialLdapContext {

    public static final String MESSAGE = "JNDI has been disabled, enable it with quarkus.naming.enable-jndi=true";

    public DisabledInitialContext() throws NamingException {
    }

    public DisabledInitialContext(Hashtable<?, ?> environment, Control[] connCtls) throws NamingException {
        super(environment, connCtls);
    }

    @Override
    public ExtendedResponse extendedOperation(ExtendedRequest request) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public LdapContext newInstance(Control[] reqCtls) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void reconnect(Control[] connCtls) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Control[] getConnectControls() throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void setRequestControls(Control[] requestControls) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Control[] getRequestControls() throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Control[] getResponseControls() throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Attributes getAttributes(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Attributes getAttributes(String name, String[] attrIds) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Attributes getAttributes(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Attributes getAttributes(Name name, String[] attrIds) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void modifyAttributes(String name, int mod_op, Attributes attrs) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void modifyAttributes(Name name, int mod_op, Attributes attrs) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void modifyAttributes(String name, ModificationItem[] mods) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void modifyAttributes(Name name, ModificationItem[] mods) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void bind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void bind(Name name, Object obj, Attributes attrs) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void rebind(String name, Object obj, Attributes attrs) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void rebind(Name name, Object obj, Attributes attrs) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public DirContext createSubcontext(String name, Attributes attrs) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public DirContext createSubcontext(Name name, Attributes attrs) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public DirContext getSchema(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public DirContext getSchema(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public DirContext getSchemaClassDefinition(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public DirContext getSchemaClassDefinition(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, Attributes matchingAttributes, String[] attributesToReturn)
            throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, Attributes matchingAttributes, String[] attributesToReturn)
            throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filter, SearchControls cons) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, Object[] filterArgs, SearchControls cons)
            throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name name, String filterExpr, Object[] filterArgs, SearchControls cons)
            throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    protected void init(Hashtable<?, ?> environment) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    protected Context getDefaultInitCtx() throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    protected Context getURLOrDefaultInitCtx(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    protected Context getURLOrDefaultInitCtx(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Object lookup(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void unbind(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void unbind(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public void close() throws NamingException {
        throw new NamingException(MESSAGE);
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        throw new NamingException(MESSAGE);
    }
}
