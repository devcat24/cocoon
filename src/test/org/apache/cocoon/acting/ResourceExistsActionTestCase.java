/*
 * Copyright 1999-2004 The Apache Software Foundation.
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

package org.apache.cocoon.acting;

import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.MockLogger;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceNotFoundException;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

/**
 *
 *
 * @author <a href="mailto:stephan@apache.org">Stephan Michels </a>
 * @version CVS $Id$
 */
public class ResourceExistsActionTestCase extends MockObjectTestCase {
    private Map objectModel = new HashMap();

    public ResourceExistsActionTestCase(String name) {
        super(name);
    }

    public void testExists() throws Exception {
        String src = "don't care";
        Parameters parameters = new Parameters();
        ResourceExistsAction action = new ResourceExistsAction();
        action.enableLogging(new MockLogger(action.getClass()));
        Mock resolver = new Mock(SourceResolver.class);
        Mock source = new Mock(Source.class);
        resolver.expects(once()).method("resolveURI").with(same(src)).
                will(returnValue(source.proxy()));
        resolver.expects(once()).method("release").with(same(source.proxy()));
        source.expects(atLeastOnce()).method("exists").will(returnValue(true));
        Map result = action.act(null, (SourceResolver) resolver.proxy(), 
                objectModel, src, parameters);
        assertSame("Test if resource exists", AbstractAction.EMPTY_MAP, result);
        resolver.verify();
        source.verify();
    }

    public void testNotExists() throws Exception {
        String src = "don't care";
        Parameters parameters = new Parameters();
        ResourceExistsAction action = new ResourceExistsAction();
        action.enableLogging(new MockLogger(action.getClass()));
        Mock resolver = new Mock(SourceResolver.class);
        resolver.expects(once()).method("resolveURI").with(same(src)).
                will(this.throwException(new SourceNotFoundException("don't care")));
        Map result = action.act(null, (SourceResolver) resolver.proxy(), 
                objectModel, src, parameters);
        assertNull("Test if resource not exists", result);
        resolver.verify();
    }
}
