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
package org.apache.cocoon.portal.profile.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.components.persistance.CastorSourceConverter;
import org.apache.cocoon.portal.profile.ProfileLS;
import org.apache.cocoon.xml.dom.DOMUtil;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.xml.sax.SAXParser;
import org.w3c.dom.Element;

/**
 *
 * @author <a href="mailto:cziegeler@s-und-n.de">Carsten Ziegeler</a>
 * @author <a href="mailto:volker.schmitt@basf-it-services.com">Volker Schmitt</a>
 * @author <a href="mailto:bluetkemeier@s-und-n.de">Bj�rn L�tkemeier</a>
 * 
 * @version CVS $Id: MapSourceAdapter.java,v 1.2 2003/05/22 15:19:42 cziegeler Exp $
 */
public class MapSourceAdapter
    extends AbstractLogEnabled
    implements Component, Composable, Configurable, ProfileLS, ThreadSafe {

    public static final String ROLE = MapSourceAdapter.class.getName();
    private ComponentManager manager;

    /* (non-Javadoc)
     * @see org.apache.cocoon.portal.profile.ProfileLS#loadProfile(java.lang.Object)
     */
    public Object loadProfile(Object key, Map map) throws Exception {
		Map mapKey = (Map) key;
		StringBuffer buffer = new StringBuffer();

		String profile = (String)map.get("profile");

		Configuration config = ((Configuration)mapKey.get("config")).getChild("profiles");
		Object type = map.get("type");
		String uri = null;
		if (type == null) {
			uri = config.getChild(profile+"-load").getAttribute("uri");
		} else if (type.equals("global")) {
			uri = config.getChild(profile+"-global-load").getAttribute("uri");
		} else if (type.equals("role")) {
			uri = config.getChild(profile+"-role-load").getAttribute("uri");
		} else if (type.equals("user")) {
			uri = config.getChild(profile+"-user-load").getAttribute("uri");
		}
		buffer.append(uri);

		if (uri.indexOf("?") == -1) {
			buffer.append("?portal=");
		} else {
			buffer.append("&portal=");
		}
		buffer.append(map.get("portalname"));

		buffer.append("&role=");
		buffer.append(mapKey.get("role"));
        
		buffer.append("&user=");
		buffer.append(mapKey.get("user"));

		String sourceURI = buffer.toString();
		SourceResolver resolver = (SourceResolver) this.manager.lookup(SourceResolver.ROLE);
		Source source = null;
		CastorSourceConverter converter = null;
		try {
			source = resolver.resolveURI(sourceURI);
			converter = (CastorSourceConverter) this.manager.lookup(CastorSourceConverter.ROLE);

			ReferenceFieldHandler.setObjectMap((Map)map.get("objectmap"));
			return converter.getObject(source.getInputStream(), profile);
		} finally {
			resolver.release(source);
			manager.release(converter);
			manager.release(resolver);
		}
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.portal.profile.ProfileLS#saveProfile(java.lang.Object, java.lang.Object)
     */
    public void saveProfile(Object key, Map map, Object profile) throws Exception {
		Map mapKey = (Map) key;
		StringBuffer buffer = new StringBuffer();

		String profileName = (String)map.get("profile");

		Configuration config = ((Configuration)mapKey.get("config")).getChild("profiles");
		Object type = map.get("type");
		String uri = null;
		if (type == null) {
			uri = config.getChild(profileName+"-save").getAttribute("uri");
		} else if (type.equals("global")) {
			uri = config.getChild(profileName+"-global-save").getAttribute("uri");
		} else if (type.equals("role")) {
			uri = config.getChild(profileName+"-role-save").getAttribute("uri");
		} else if (type.equals("user")) {
			uri = config.getChild(profileName+"-user-save").getAttribute("uri");
		}
		buffer.append(uri);

		if (uri.indexOf("?") == -1) {
			buffer.append("?portal=");
		} else {
			buffer.append("&portal=");
		}
		buffer.append(map.get("portalname"));

		buffer.append("&role=");
		buffer.append(mapKey.get("role"));
        
		buffer.append("&user=");
		buffer.append(mapKey.get("user"));

		SourceResolver resolver = (SourceResolver) this.manager.lookup(SourceResolver.ROLE);
		Source source = null;
		CastorSourceConverter converter = null;
		SAXParser parser = null;
		try {
			StringWriter writer = new StringWriter();
			
			converter = (CastorSourceConverter) this.manager.lookup(CastorSourceConverter.ROLE);
			converter.storeObject(writer, profileName, profile);

			buffer.append("&content=");
			buffer.append(writer.toString());

			source = resolver.resolveURI(buffer.toString());
			parser = (SAXParser)this.manager.lookup(SAXParser.ROLE);
			Element element = DOMUtil.getDocumentFragment(parser, new InputStreamReader(source.getInputStream())).getOwnerDocument().getDocumentElement();
			if (!DOMUtil.getValueOf(element, "descendant::sourceResult/execution").trim().equals("success")) {
				throw new IOException("Could not save profile: "+DOMUtil.getValueOf(element, "descendant::sourceResult/message"));
			}
		} finally {
			resolver.release(source);
			manager.release((Component)parser);
			manager.release(converter);
			manager.release(resolver);
		}
    }

    /* (non-Javadoc)
     * @see org.apache.cocoon.portal.profile.ProfileLS#getValidity(java.lang.Object)
     */
    public SourceValidity getValidity(Object key, Map map) {
		SourceResolver resolver = null;
		Source source = null;
		try {
			Map mapKey = (Map) key;
			StringBuffer buffer = new StringBuffer();

			String profile = (String)map.get("profile");

			Configuration config = ((Configuration)mapKey.get("config")).getChild("profiles");
			Object type = map.get("type");
			String uri = null;
			if (type == null) {
				uri = config.getChild(profile+"-load").getAttribute("uri");
			} else if (type.equals("global")) {
				uri = config.getChild(profile+"-global-load").getAttribute("uri");
			} else if (type.equals("role")) {
				uri = config.getChild(profile+"-role-load").getAttribute("uri");
			} else if (type.equals("user")) {
				uri = config.getChild(profile+"-user-load").getAttribute("uri");
			}
			buffer.append(uri);

			if (uri.indexOf("?") == -1) {
				buffer.append("?portal=");
			} else {
				buffer.append("&portal=");
			}
			buffer.append(map.get("portalname"));

			buffer.append("&role=");
			buffer.append(mapKey.get("role"));
        
			buffer.append("&user=");
			buffer.append(mapKey.get("user"));

			String sourceURI = buffer.toString();
			resolver = (SourceResolver) this.manager.lookup(SourceResolver.ROLE);
			source = resolver.resolveURI(sourceURI);
			return source.getValidity();
		} catch (Exception e) {
			getLogger().warn(e.getMessage(), e);
			return null;
		} finally {
			if (source != null)
				resolver.release(source);
			manager.release(resolver);
		}
    }

    /* (non-Javadoc)
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.apache.avalon.framework.component.Composable#compose(org.apache.avalon.framework.component.ComponentManager)
     */
    public void compose(ComponentManager manager) throws ComponentException {
        this.manager = manager;
    }

}
