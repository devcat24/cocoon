/*

 ============================================================================
				   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

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
package org.apache.cocoon.portal.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cocoon.portal.coplet.CopletData;
import org.apache.cocoon.portal.coplet.CopletInstanceData;
import org.exolab.castor.mapping.MapItem;

/**
 * Field handler for attributes of a CopletData object.
 *
 * FIXME This is a little bit hacky and should be changed by using
 * reflection
 * 
 * @author <a href="mailto:bluetkemeier@s-und-n.de">Bj&ouml;rn L&uuml;tkemeier</a>
 * 
 * @version CVS $Id: AttributesFieldHandler.java,v 1.6 2003/09/24 21:22:33 cziegeler Exp $
 */
public class AttributesFieldHandler extends AbstractFieldHandler {

    protected Map getAttributes(Object object) {
        if (object instanceof CopletData) {
            return ((CopletData) object).getAttributes();
        } else {
            return ((CopletInstanceData) object).getAttributes();
        }
    }

    public Object getValue(Object object) {
        HashMap map = new HashMap();
        Iterator iterator = this.getAttributes(object).entrySet().iterator();
        Map.Entry entry;
        Object key;
        while (iterator.hasNext()) {
            entry = (Map.Entry) iterator.next();
            key = entry.getKey();
            map.put(key, new MapItem(key, entry.getValue()));
        }
        return map;
    }

    public Object newInstance(Object parent) {
        return new MapItem();
    }

    public void resetValue(Object object) {
        this.getAttributes(object).clear();
    }

    public void setValue(Object object, Object value) {
        MapItem item = (MapItem) value;
        this.getAttributes(object).put(item.getKey(), item.getValue());
    }
}
