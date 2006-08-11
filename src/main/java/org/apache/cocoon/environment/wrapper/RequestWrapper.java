/*
 * Copyright 1999-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.environment.wrapper;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cocoon.environment.Environment;
import org.apache.cocoon.environment.Request;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.iterators.IteratorEnumeration;

/**
 * This is a wrapper class for the <code>Request</code> object.
 * It has the same properties except that the url and the parameters
 * are different.
 *
 * @version $Id$
 */
public final class RequestWrapper extends AbstractRequestWrapper {

    /** The query string */
    private String queryString;

    /** The request parameters */
    private final RequestParameters parameters;

    /** The environment */
    private final Environment environment;

    /** raw mode? **/
    private final boolean rawMode;

    /** The request uri */
    private String requestURI;

    private final Map requestAttributes = new HashMap();
    
    /**
     * Constructor
     */
    public RequestWrapper(Request request,
                          String  requestURI,
                          String  queryString,
                          Environment env) {
        this(request, requestURI, queryString, env, false);
    }

    /**
     * Constructor
     */
    public RequestWrapper(Request request,
                          String  requestURI,
                          String  queryString,
                          Environment env,
                          boolean rawMode) {
        super(request);
        this.environment = env;
        this.queryString = queryString;
        this.parameters = new RequestParameters(queryString);
        this.rawMode = rawMode;
        if (this.req.getQueryString() != null && !this.rawMode) {
            if (this.queryString == null)
                this.queryString = this.req.getQueryString();
            else
                this.queryString += '&' + this.req.getQueryString();
        }
        this.requestURI = this.req.getRequestURI();
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#getParameter(java.lang.String)
     */
    public String getParameter(String name) {
        String value = this.parameters.getParameter(name);
        if (value == null && !this.rawMode) {
            return this.req.getParameter(name);
        }
        return value;
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#getParameterNames()
     */
    public Enumeration getParameterNames() {
        if ( !this.rawMode ) {
            // put all parameter names into a set
            Set parameterNames = new HashSet();
            Enumeration names = this.parameters.getParameterNames();
            while (names.hasMoreElements()) {
                parameterNames.add(names.nextElement());
            }
            names = this.req.getParameterNames();
            while (names.hasMoreElements()) {
                parameterNames.add(names.nextElement());
            }
            return new IteratorEnumeration(parameterNames.iterator());
        }
        return this.parameters.getParameterNames();
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#getParameterValues(java.lang.String)
     */
    public String[] getParameterValues(String name) {
        if ( !this.rawMode ) {
            String[] values = this.parameters.getParameterValues(name);
            String[] inherited = this.req.getParameterValues(name);
            if (inherited == null) return values;
            if (values == null) return inherited;
            String[] allValues = new String[values.length + inherited.length];
            System.arraycopy(values, 0, allValues, 0, values.length);
            System.arraycopy(inherited, 0, allValues, values.length, inherited.length);
            return allValues;
        }
        return this.parameters.getParameterValues(name);
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#getQueryString()
     */
    public String getQueryString() {
        return this.queryString;
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#getRequestURI()
     */
    public String getRequestURI() {
        return this.requestURI;
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#getSitemapURI()
     */
    public String getSitemapURI() {
        return this.environment.getURI();
    }

    public String getSitemapURIPrefix() {
        return this.environment.getURIPrefix();
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#getSitemapPath()
     */
    public String getSitemapPath() {
        return this.environment.getURIPrefix();
    }

    public void setRequestURI(String prefix, String uri) {
        StringBuffer buffer = new StringBuffer(this.getContextPath());
        buffer.append('/');
        buffer.append(prefix);
        buffer.append(uri);
        this.requestURI = buffer.toString();
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#getAttribute(java.lang.String, int)
     */
    public Object getAttribute(String name, int scope) {
        if ( scope == Request.GLOBAL_SCOPE ) {
            return super.getAttribute(name, scope);
        }
        return this.requestAttributes.get( name );
    }
        
    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#getAttributeNames(int)
     */
    public Enumeration getAttributeNames(int scope) {
        if ( scope == Request.GLOBAL_SCOPE ) {
            return super.getAttributeNames(scope);
        }
        return IteratorUtils.asEnumeration(this.requestAttributes.keySet().iterator());
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#removeAttribute(java.lang.String, int)
     */
    public void removeAttribute(String name, int scope) {
        if ( scope == Request.GLOBAL_SCOPE ) {
            super.removeAttribute(name, scope);
        } else {
            this.requestAttributes.remove( name );
        }
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.environment.Request#setAttribute(java.lang.String, java.lang.Object, int)
     */
    public void setAttribute(String name, Object o, int scope) {
        if ( scope == Request.GLOBAL_SCOPE ) {
            super.setAttribute(name, o, scope);
        } else {
            this.requestAttributes.put( name, o );
        }
    }

    /**
     * @see org.apache.cocoon.environment.Request#searchAttribute(java.lang.String)
     */
    public Object searchAttribute(String name) {
        Object result = this.getAttribute(name, REQUEST_SCOPE);
        if ( result == null ) {
            result = this.getAttribute(name, GLOBAL_SCOPE);
            if ( result == null ) {
                result = this.req.getAttribute(name, REQUEST_SCOPE);
            }
        }
        return result;
    }
}
