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

package org.apache.cocoon.components.modules.input;

import java.util.Iterator;
import java.util.Map;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.ComponentSelector;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.thread.ThreadSafe;

/**
 * AbstractMetaModule gives you the infrastructure for easily
 * deploying more "meta" InputModules i.e. InputModules that are
 * composed of other InputModules.  In order to get at the Logger, use
 * getLogger().
 *
 * @author <a href="mailto:haul@apache.org">Christian Haul</a>
 * @version CVS $Id: AbstractMetaModule.java,v 1.1 2003/03/09 00:09:02 pier Exp $
 */
public abstract class AbstractMetaModule extends AbstractInputModule
    implements Composable, Disposable {

    /** The component manager instance */
    protected ComponentManager manager;

    /** The cached InputModule-Selector */
    protected ComponentSelector inputSelector = null;

    /** The cached default InputModule */
    protected InputModule input = null;

    /** The default InputModule name / shorthand. Defaults to 'request-param' */
    protected String defaultInput = "request-param"; // default to request parameters

    /** The default InputModule configuration */
    protected Configuration inputConf = null;  // will become an empty configuration object
                                               // during configure() so why bother here...
    
    /** Is this instance initialized? */
    protected boolean initialized = false;

    /* Constants */

    protected final static String INPUT_MODULE_SELECTOR = InputModule.ROLE+"Selector";

    /* Operation codes */
    private final static int OP_GET = 0;
    private final static int OP_VALUES = 1;
    private final static int OP_NAMES = 2;


    /**
     * Set the current <code>ComponentManager</code> instance used by this
     * <code>Composable</code>.
     */
    public void compose(ComponentManager manager) throws ComponentException {

        this.manager=manager;
    }


    /**
     * Initialize the meta module with exactly one other input
     * module. Since "meta" modules require references to components
     * of the same role, initialization cannot be done in initialize()
     * when also implementing ThreadSafe since at that point the
     * component selector is not yet initialized it would trigger the
     * creation of a new one resulting in an endless loop of
     * initializations. Therefore, every module needs to call this
     * method when it first requires access to another module if the
     * module itself has not been initialized. Override this method
     * and dispose() to keep references to more than one module.
     */
    public synchronized void lazy_initialize() {

        try {
            // obtain input modules
            if (!this.initialized) {
                this.inputSelector=(ComponentSelector) this.manager.lookup(INPUT_MODULE_SELECTOR); 
                if (this.inputSelector != null && this.inputSelector instanceof ThreadSafe) {
                    
                    if (this.defaultInput != null) {
                        this.input = obtainModule(this.defaultInput);
                    }
                    
                    } else if (!(this.inputSelector instanceof ThreadSafe) ) {
                        this.manager.release(this.inputSelector);
                        this.inputSelector = null;
                    }
                
                this.initialized = true;
            }
        } catch (Exception e) {
            if (getLogger().isWarnEnabled()) 
                getLogger().warn("A problem occurred setting up input modules :'" + e.getMessage());
        }
    }


    /**
     * Dispose exactly one cached InputModule. To work on more than
     * one, override this method and initialize().
     */
    public void dispose() {

        if (this.inputSelector != null) {
            if (this.input != null)
                this.inputSelector.release(this.input);
            this.manager.release(this.inputSelector);
        }
    }


    /**
     * Obtain a permanent reference to an InputModule.
     */
    protected InputModule obtainModule(String type) {
        ComponentSelector inputSelector = this.inputSelector;
        InputModule module = null;
        try {
            if (inputSelector == null) 
                inputSelector=(ComponentSelector) this.manager.lookup(INPUT_MODULE_SELECTOR); 

            if (inputSelector.hasComponent(type)){
                
                if (type != null && inputSelector.hasComponent(type))
                    module = (InputModule) inputSelector.select(type);
                
                if (!(module instanceof ThreadSafe) ) {
                    inputSelector.release(module);
                    module = null;
                }
            }
            if (type != null && module == null)
                if (getLogger().isWarnEnabled())
                    getLogger().warn("A problem occurred setting up '" + type
                                     +"': Selector is "+(inputSelector!=null?"not ":"")
                                     +"null, Component is "
                                     +(inputSelector!=null && inputSelector.hasComponent(type)?"known":"unknown"));
            
        } catch (ComponentException ce) {
            if (getLogger().isWarnEnabled())
                getLogger().warn("Could not obtain selector for InputModules: "+ce.getMessage());
        } finally {
            if (this.inputSelector == null) 
                this.manager.release(inputSelector);
            // FIXME: Is it OK to keep a reference to the module when we release the selector?
        }

        return module;
    }


    /**
     * release a permanent reference to an InputModule.
     */
    protected void releaseModule(InputModule module) {
        ComponentSelector inputSelector = this.inputSelector;
        if (module != null) {
            try {
                // FIXME: Is it OK to release a module when we have released the selector before?
                if (inputSelector == null) 
                    inputSelector=(ComponentSelector) this.manager.lookup(INPUT_MODULE_SELECTOR); 
                
                inputSelector.release(module);
                module = null;
                
            } catch (ComponentException ce) {
                if (getLogger().isWarnEnabled())
                    getLogger().warn("Could not obtain selector for InputModules: "+ce.getMessage());
            } finally {
                if (this.inputSelector == null) 
                    this.manager.release(inputSelector);
            }
        }
    }


    protected Iterator getNames(Map objectModel, 
                                InputModule modA, String modAName, Configuration modAConf) {

        return (Iterator) this.get(OP_NAMES, null, objectModel, modA, modAName, modAConf, null, null, null);
    }

    /**
     * If two modules are specified, the second one is used if the name is not null.
     */
    protected Iterator getNames(Map objectModel, 
                                InputModule modA, String modAName, Configuration modAConf,
                                InputModule modB, String modBName, Configuration modBConf) {

        return (Iterator) this.get(OP_NAMES, null, objectModel, modA, modAName, modAConf, modB, modBName, modBConf);
    }


    protected Object getValue(String attr, Map objectModel, 
                              InputModule modA, String modAName, Configuration modAConf) {

        return this.get(OP_GET, attr, objectModel, modA, modAName, modAConf, null, null, null);
    }


    /**
     * If two modules are specified, the second one is used if the name is not null.
     */
    protected Object getValue(String attr, Map objectModel, 
                              InputModule modA, String modAName, Configuration modAConf,
                              InputModule modB, String modBName, Configuration modBConf) {

        return this.get(OP_GET, attr, objectModel, modA, modAName, modAConf, modB, modBName, modBConf);
    }


    protected Object[] getValues(String attr, Map objectModel, 
                                 InputModule modA, String modAName, Configuration modAConf) {

        return (Object[]) this.get(OP_VALUES, attr, objectModel, modA, modAName, modAConf, null, null, null);
    }


    /**
     * If two modules are specified, the second one is used if the name is not null.
     */
    protected Object[] getValues(String attr, Map objectModel, 
                                 InputModule modA, String modAName, Configuration modAConf,
                                 InputModule modB, String modBName, Configuration modBConf) {

        return (Object[]) this.get(OP_VALUES, attr, objectModel, modA, modAName, modAConf, modB, modBName, modBConf);
    }


    /**
     * Capsules use of an InputModule. Does all the lookups and so
     * on. Returns either an Object, an Object[], or an Iterator,
     * depending on the method called i.e. the op specified. The
     * second module is preferred and has an non null name. If an
     * exception is encountered, a warn message is printed and null is
     * returned.
     */
    private Object get(int op, String attr, Map objectModel,
                         InputModule modA, String modAName, Configuration modAConf,
                         InputModule modB, String modBName, Configuration modBConf) {

        ComponentSelector cs = this.inputSelector;
        Object value = null;
        String name = null;
        InputModule input = null;
        Configuration conf = null;
        boolean release = false;

        try {

            if (getLogger().isDebugEnabled())
                getLogger().debug("parameters "+op+": "+modA+", "+modAName+", "+modAConf+" || "+modB+", "+modBName+", "+modBConf);
            if (cs == null)
                cs = (ComponentSelector) this.manager.lookup(INPUT_MODULE_SELECTOR);

            if (modB == null && modBName == null) {
                input = modA;
                name = modAName;
                conf = modAConf;
            } else {
                input = modB;
                name = modBName;
                conf = modBConf;
            }
        
            if (input == null) {
                if (cs.hasComponent(name)) {
                    release = true;
                    input = (InputModule) cs.select(name);
                } else {
                    if (getLogger().isWarnEnabled())
                        getLogger().warn("No such InputModule: "+name);
                }
            }

            switch (op) {
            case OP_GET:    
                value = input.getAttribute(attr, conf, objectModel);
                break;
            case OP_VALUES:
                value = input.getAttributeValues(attr, conf, objectModel);
                break;
            case OP_NAMES:
                value = input.getAttributeNames(conf, objectModel);
                break;
            };

            if (getLogger().isDebugEnabled())
                getLogger().debug("using "+name+" as "+input+" for "+op+" ("+attr+") and "+conf+" gives "+value);
            
        } catch (Exception e) {
            if (getLogger().isWarnEnabled())
                getLogger().warn("A problem obtaining a value from "+name+" occurred : "+e.getMessage());
        } finally {         
            if (release)
                cs.release(input);

            if (this.inputSelector == null)
                this.manager.release(cs);
        }

        return value;
    }
                              


}
