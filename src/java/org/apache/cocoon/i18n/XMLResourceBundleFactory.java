/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Apache Cocoon" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation and was  originally created by
 Stefano Mazzocchi  <stefano@apache.org>. For more  information on the Apache
 Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.cocoon.i18n;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentSelector;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.thread.ThreadSafe;

import org.xml.sax.SAXParseException;

/**
 * This is the XMLResourceBundleFactory, the method for getting and
 * creating XMLResourceBundles.
 *
 * @author <a href="mailto:mengelhart@earthtrip.com">Mike Engelhart</a>
 * @author <a href="mailto:neeme@one.lv">Neeme Praks</a>
 * @author <a href="mailto:oleg@one.lv">Oleg Podolsky</a>
 * @author <a href="mailto:kpiroumian@apache.org">Konstantin Piroumian</a>
 * @version $Id: XMLResourceBundleFactory.java,v 1.1 2003/03/09 00:09:32 pier Exp $
 */

public class XMLResourceBundleFactory 
    extends DefaultComponentSelector
    implements BundleFactory, Composable, Configurable, Disposable, ThreadSafe, LogEnabled {

    /** Should we load bundles to cache on startup or not? */
    protected boolean cacheAtStartup = false;

    /** Root directory to all bundle names */
    protected String directory;

    /** Cache for the names of the bundles that were not found */
    protected Map cacheNotFound = new HashMap();

    /** The logger */
    protected Logger logger;

    /** Component Manager */
    protected ComponentManager manager = null;

    /**
     * Default constructor.
     */
    public XMLResourceBundleFactory() {
    }

    public void compose(ComponentManager manager) {
        this.manager = manager;
    }

    public void dispose() {
        Collection bundles = getComponentMap().values();
        Iterator i = bundles.iterator();
        while (i.hasNext()) {
            Object bundle = i.next();
            if (bundle instanceof Disposable) {
                ((Disposable)bundle).dispose();
            }
        }
        this.manager = null;
    }

    /**
     * Configure the component.
     *
     * @param configuration the configuration
     */
    public void configure( Configuration configuration ) throws ConfigurationException {
        this.cacheAtStartup = configuration.getChild(ConfigurationKeys.CACHE_AT_STARTUP).getValueAsBoolean(false);

        try {
            this.directory = configuration.getChild(ConfigurationKeys.ROOT_DIRECTORY, true).getValue();
        } catch (ConfigurationException e) {
            if (logger.isWarnEnabled()) logger.warn("Root directory not provided in configuration, using default (root).");
            this.directory = "";
        }

        if (logger.isDebugEnabled()) { 
            logger.debug(
                "XMLResourceBundleFactory configured with: cacheAtStartup = " +
                cacheAtStartup + ", directory = '" + directory + "'"
            );
        }
    }

    /**
     * Select a bundle based on bundle name and locale.
     *
     * @param name              bundle name
     * @param locale            locale
     * @return                  the bundle
     * @exception ComponentException if a bundle is not found
     */
    public Component select(String name, Locale locale)
        throws ComponentException {
        return select(name, locale, cacheAtStartup);
    }

    /**
     * Select a bundle based on bundle name and locale.
     *
     * @param name              bundle name
     * @param locale            locale
     * @param cacheAtStartup    cache all the keys when constructing?
     * @return                  the bundle
     * @exception ComponentException if a bundle is not found
     */
    public Component select(String name, Locale loc, boolean cacheAtStartup)
        throws ComponentException {
        Component bundle = _select(name, loc, cacheAtStartup);
        if (bundle == null)
            throw new ComponentException("Unable to locate resource: " + name);
        return bundle;
    }

    /**
     * Select a bundle based on bundle name and locale.
     *
     * @param name              bundle name
     * @param locale            locale
     * @return                  the bundle
     */
    private Component _select(String name, Locale loc) {
        return _select(name, loc, cacheAtStartup);
    }

    /**
     * Select the parent bundle of the current bundle, based on
     * bundle name and locale.
     *
     * @param name              bundle name
     * @param locale            locale
     * @return                  the bundle
     */
    protected Component selectParent(String name, Locale loc) {
        return selectParent(name, loc, cacheAtStartup);
    }

    /**
     * Select the parent bundle of the current bundle, based on
     * bundle name and locale.
     *
     * @param name              bundle name
     * @param locale            locale
     * @param cacheAtStartup    cache all the keys when constructing?
     * @return                  the bundle
     */
    protected Component selectParent(String name, Locale loc, boolean cacheAtStartup) {
        return _select(name, getParentLocale(loc), cacheAtStartup);
    }

    /**
     * Select a bundle based on bundle name and locale name.
     *
     * @param name              bundle name
     * @param localeName        locale name
     * @return                  the bundle
     * @exception ComponentException if a bundle is not found
     */
    public Component select(String name, String localeName)
        throws ComponentException {
        return select(name, new Locale(localeName, localeName) );
    }

    /**
     * Select a bundle based on source XML file name.
     *
     * @param fileName          file name
     * @return                  the bundle
     * @exception ComponentException if a bundle is not found
     */
    public Component selectFromFilename(String fileName)
        throws ComponentException {
        return selectFromFilename(fileName, cacheAtStartup);
    }

    /**
     * Select a bundle based on source XML file name.
     *
     * @param fileName          file name
     * @param cacheAtStartup    cache all the keys when constructing?
     * @return                  the bundle
     * @exception ComponentException if a bundle is not found
     */
    public Component selectFromFilename(String fileName, boolean cacheAtStartup)
        throws ComponentException {
        Component bundle = _select(fileName, null, cacheAtStartup);
        if (bundle == null)
            throw new ComponentException("Unable to locate resource: " + fileName);
        return bundle;
    }

    /**
     * Select a bundle based on bundle name and locale.
     *
     * @param name              bundle name
     * @param locale            locale
     * @param cacheAtStartup    cache all the keys when constructing?
     * @return                  the bundle
     * @exception ComponentException if a bundle is not found
     */
    private Component _select(String name, Locale loc, boolean cacheAtStartup) {
        if (logger.isDebugEnabled()) logger.debug("_getBundle: " + name + ", locale " + loc);
        String fileName = getFileName(name, loc);
        XMLResourceBundle bundle = (XMLResourceBundle) selectCached(fileName);
        if (bundle == null && !isNotFoundBundle(fileName)) {
            if (logger.isDebugEnabled()) logger.debug("not found in cache, loading: " + fileName);
            synchronized(this) {
                bundle = (XMLResourceBundle) selectCached(fileName);
                if (bundle == null && !isNotFoundBundle(fileName)) {
                    if (logger.isDebugEnabled()) logger.debug("synchronized: not found in cache, loading: " + fileName);
                    bundle = _loadBundle(name, fileName, loc, cacheAtStartup);
                    Locale parentLoc = loc;
                    String parentBundleName;
                    while (bundle == null && parentLoc != null && !parentLoc.getLanguage().equals("")) {
                        if (logger.isDebugEnabled()) logger.debug("synchronized: still not found, trying parent: " + fileName);
                        parentLoc = getParentLocale(parentLoc);
                        parentBundleName = getFileName(name, parentLoc);
                        bundle = _loadBundle(name, parentBundleName, parentLoc, cacheAtStartup);
                        updateCache(parentBundleName, bundle);
                    }
                    updateCache(fileName, bundle);
                }
            }
        }
        return (Component) bundle;
    }

    /**
     * Construct a bundle based on bundle name, file name and locale.
     *
     * @param name              bundle name
     * @param fileName          full path to source XML file
     * @param locale            locale
     * @param cacheAtStartup    cache all the keys when constructing?
     * @return                  the bundle, null if loading failed
     */
    private XMLResourceBundle _loadBundle(String name, String fileName, Locale loc, boolean cacheAtStartup) {
        if (logger.isDebugEnabled()) 
            logger.debug("Trying to load bundle: " + name + ", locale " + loc + ", filename " + fileName);
        XMLResourceBundle bundle = null;
        XMLResourceBundle parentBundle = null;
        try {
            if (loc != null && !loc.getLanguage().equals(""))
                parentBundle = (XMLResourceBundle) selectParent(name, loc);
            bundle = new XMLResourceBundle();
            bundle.enableLogging(logger);
            bundle.compose(this.manager);
            bundle.init(name, fileName, loc, parentBundle, cacheAtStartup);
            return bundle;
        } catch (FileNotFoundException fe) {
            logger.info("Resource not found: " + name + ", locale " + loc
                + ", bundleName " + fileName + ". Exception: " + fe.getMessage());
        } catch (SAXParseException se) {
            logger.error("Incorrect resource format", se);
        } catch (Exception e) {
            logger.error("Resource loading failed", e);
        }
        return null;
    }

    public void release(Component component) {
        // Do nothing
    }

    /**
     * Returns the next locale up the parent hierarchy.
     * E.g. the parent of new Locale("en","us","mac") would be
     * new Locale("en", "us", "").
     *
     * @param locale            the locale
     * @return                  the parent locale
     */
    protected Locale getParentLocale(Locale loc) {
        Locale newloc;
        if (loc.getVariant().equals("")) {
            if (loc.getCountry().equals(""))
                newloc = new Locale("","","");
            else
                newloc = new Locale(loc.getLanguage(), "", "");
        } else
            newloc = new Locale(loc.getLanguage(), loc.getCountry(), "");

        return newloc;
    }

    /**
     * Maps a bundle name and locale to a full path in the filesystem.
     * If you need a different mapping, then just override this method.
     *
     * @param locale            the locale
     * @return                  the parent locale
     */
    protected String getFileName(String name, Locale loc) {
        StringBuffer sb = new StringBuffer(getDirectory());
        sb.append('/').append(name);
        if (loc != null) {
            if (! loc.getLanguage().equals("")) {
                sb.append("_");
                sb.append(loc.getLanguage());
            }
            if (! loc.getCountry().equals("")) {
                sb.append("_");
                sb.append(loc.getCountry());
            }
            if (! loc.getVariant().equals("")) {
                sb.append("_");
                sb.append(loc.getVariant());
            }
        }
        sb.append(".xml");

        String result = sb.toString();
        if (logger.isDebugEnabled()) logger.debug("Resolving bundle name to file name: " + name + ", locale " + loc + " --> " + result);
        return result;
    }

    /**
     * Selects a bundle from the cache.
     *
     * @param fileName          file name of the bundle
     * @return                  the cached bundle; null, if not found
     */
    protected Component selectCached(String fileName) {
        Component bundle = null;
        try {
            bundle = super.select(fileName);
            if (logger.isDebugEnabled()) logger.debug("Returning from cache: " + fileName);
        } catch (ComponentException e) {
            if (logger.isDebugEnabled()) logger.debug("Not found in cache: " + fileName);
        }
        return bundle;
    }

    /**
     * Checks if the bundle is in the &quot;not-found&quot; cache.
     *
     * @param fileName          file name of the bundle
     * @return                  true, if the bundle wasn't found already before;
     *                          otherwise, false.
     */
    protected boolean isNotFoundBundle(String fileName) {
        String result = (String)(cacheNotFound.get(fileName));
        if (result != null) {
            if (logger.isDebugEnabled()) logger.debug("Returning from not_found_cache: " + fileName);
        }
        else {
            if (logger.isDebugEnabled()) logger.debug("Not found in not_found_cache: " + fileName);
        }
        return result != null;
    }

    /**
     * Checks if the bundle is in the &quot;not-found&quot; cache.
     *
     * @param fileName          file name of the bundle
     * @return                  true, if the bundle wasn't found already before;
     *                          otherwise, false.
     */
    protected void updateCache(String fileName, XMLResourceBundle bundle) {
        if (bundle == null) {
            if (logger.isDebugEnabled()) logger.debug("Updating not_found_cache: " + fileName);
            cacheNotFound.put(fileName, fileName);
        }
        else {
            if (logger.isDebugEnabled()) logger.debug("Updating cache: " + fileName);
            super.put((Object) fileName, (Component) bundle);
        }
    }

    /**
     * Returns the root directory to all bundles.
     *
     * @return the directory path
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * Should we load bundles to cache on startup or not?
     *
     * @return true if pre-loading all resources; false otherwise
     */
    public boolean cacheAtStartup() {
        return cacheAtStartup;
    }
    
	/**
	 * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
	 */
	public void enableLogging(Logger logger) {
        this.logger = logger;
	}

}
