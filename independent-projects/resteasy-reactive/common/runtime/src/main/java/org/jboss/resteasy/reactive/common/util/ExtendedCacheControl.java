package org.jboss.resteasy.reactive.common.util;

import jakarta.ws.rs.core.CacheControl;

/**
 * Adds support for the public directive which is not supported by {@link CacheControl} for some reason.
 *
 * @author <a href="http://community.jboss.org/people/jharting">Jozef Hartinger</a>
 *
 * @see <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.1">RFC-2616 Section 14</a>
 */
public class ExtendedCacheControl extends CacheControl {

    private boolean _public = false;

    public boolean isPublic() {
        return _public;
    }

    public void setPublic(boolean _public) {
        this._public = _public;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + (_public ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ExtendedCacheControl other = (ExtendedCacheControl) obj;
        if (_public != other._public)
            return false;
        return true;
    }
}
