/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 2004 The Apache Software Foundation. All rights reserved.

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

*/
package org.apache.cocoon.portal.pluto.om.common;

import org.apache.pluto.om.common.*;
import org.apache.pluto.util.StringUtils;
import java.util.*;

public class ParameterSetImpl extends HashSet
implements ParameterSet, ParameterSetCtrl, java.io.Serializable {

    public ParameterSetImpl()
    {
    }

    // ParameterSet implementation.

    public Parameter get(String name)
    {
        Iterator iterator = this.iterator();
        while (iterator.hasNext()) {
            Parameter parameter = (Parameter)iterator.next();
            if (parameter.getName().equals(name)) {
                return parameter;
            }
        }
        return null;
    }

    // ParameterSetCtrl implementation.

    public Parameter add(String name, String value)
    {
        ParameterImpl parameter = new ParameterImpl();
        parameter.setName(name);
        parameter.setValue(value);

        super.add(parameter);

        return parameter;
    }

    public Parameter remove(String name)
    {
        Iterator iterator = this.iterator();
        while (iterator.hasNext()) {
            Parameter parameter = (Parameter)iterator.next();
            if (parameter.getName().equals(name)) {
                super.remove(parameter);
                return parameter;
            }
        }
        return null;
    }

    public void remove(Parameter parameter)
    {
        super.remove(parameter);
    }

    // additional methods.
    
    public String toString()
    {
        return toString(0);
    }

    public String toString(int indent)
    {
        StringBuffer buffer = new StringBuffer(50);
        StringUtils.newLine(buffer,indent);
        buffer.append(getClass().toString());
        buffer.append(": ");
        Iterator iterator = this.iterator();
        while (iterator.hasNext()) {
            buffer.append(((ParameterImpl)iterator.next()).toString(indent+2));
        }
        return buffer.toString();
    }     
}
