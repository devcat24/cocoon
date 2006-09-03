/*
 * Copyright 2006 The Apache Software Foundation
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cocoon.core.container.spring.avalon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.ProcessingUtil;
import org.apache.cocoon.acting.Action;
import org.apache.cocoon.components.pipeline.ProcessingPipeline;
import org.apache.cocoon.components.treeprocessor.ProcessorComponentInfo;
import org.apache.cocoon.configuration.Settings;
import org.apache.cocoon.generation.Generator;
import org.apache.cocoon.matching.Matcher;
import org.apache.cocoon.reading.Reader;
import org.apache.cocoon.selection.Selector;
import org.apache.cocoon.serialization.Serializer;
import org.apache.cocoon.transformation.Transformer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.ResourceLoader;
import org.w3c.dom.Element;

/**
 * This is the main implementation of the Avalon-Spring-bridge.
 * It creates the environment for Avalon components: a logger bean and a context
 * bean, reads the Avalon style configurations and registers the components
 * as beans in the Spring bean definition registry.
 *
 * @version $Id$
 * @since 2.2
 */
public class AvalonElementParser implements BeanDefinitionParser {

    /** Logger (we use the same logging mechanism as Spring!) */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Register a bean definition.
     * @param beanDef  The bean definition.
     * @param beanName The name of the bean.
     * @param registry The registry.
     */
    protected void register(BeanDefinition beanDef,
                            String         beanName,
                            BeanDefinitionRegistry registry) {
        this.register(beanDef, beanName, null, registry);
    }

    /**
     * Register a bean definition.
     * @param beanDef  The bean definition.
     * @param beanName The name of the bean.
     * @param alias    Optional alias.
     * @param registry The registry.
     */
    protected void register(BeanDefinition beanDef,
                            String         beanName,
                            String         alias,
                            BeanDefinitionRegistry registry) {
        if ( this.logger.isDebugEnabled() ) {
            this.logger.debug("Registering bean with name " + beanName +
                              (alias != null ? " (alias=" + alias + ") " : " ") +
                              beanDef);
        }
        final BeanDefinitionHolder holder;
        if ( alias != null ) {
            holder = new BeanDefinitionHolder(beanDef, beanName, new String[] {alias});
        } else {
            holder = new BeanDefinitionHolder(beanDef, beanName);
        }
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
    }

    /**
     * @see org.springframework.beans.factory.xml.BeanDefinitionParser#parse(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
     */
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        final String loggingConfiguration = element.getAttribute("loggingConfiguration");
        // we only add the logger if the configuration is present
        if ( loggingConfiguration != null && loggingConfiguration.trim().length() > 0 ) {
            this.addLogger(loggingConfiguration, parserContext.getRegistry());
        }

        // add context
        this.addComponent(AvalonContextFactoryBean.class,
                ProcessingUtil.CONTEXT_ROLE,
                "init",
                true,
                parserContext.getRegistry());

        // add service manager
        this.addComponent(AvalonServiceManager.class,
                ProcessingUtil.SERVICE_MANAGER_ROLE,
                null,
                false,
                parserContext.getRegistry());

        // read avalon style configuration
        // the schema ensures that location is never null
        final String location = element.getAttribute("location");
        final ResourceLoader resourceLoader = parserContext.getReaderContext().getReader().getResourceLoader();
        try {
            final ConfigurationInfo info = ConfigurationReader.readConfiguration(location, resourceLoader);
            // first handle includes
            final Iterator includeIter = info.getImports().iterator();
            while ( includeIter.hasNext() ) {
                final String uri = (String)includeIter.next();
                parserContext.getDelegate().getReaderContext().getReader().loadBeanDefinitions(resourceLoader.getResource(uri));
            }

            // then create components
            this.createConfig(info, parserContext.getRegistry());

            // register component infos for child factories
            this.registerComponentInfo(info, parserContext.getRegistry());

            // and finally add avalon bean post processor
            final RootBeanDefinition beanDef = this.createBeanDefinition(AvalonBeanPostProcessor.class, null, true);
            beanDef.getPropertyValues().addPropertyValue("logger", new RuntimeBeanReference(ProcessingUtil.LOGGER_ROLE));
            beanDef.getPropertyValues().addPropertyValue("context", new RuntimeBeanReference(ProcessingUtil.CONTEXT_ROLE));
            beanDef.getPropertyValues().addPropertyValue("configurationInfo", new RuntimeBeanReference(ConfigurationInfo.class.getName()));

            this.register(beanDef, AvalonBeanPostProcessor.class.getName(), parserContext.getRegistry());

        } catch (Exception e) {
            throw new BeanDefinitionStoreException("Unable to read Avalon configuration from '" + location + "'.",e);
        }

        return null;
    }

    /**
     * Add the logger bean.
     * @param configuration The location of the logging configuration.
     * @param registry      The bean registry.
     */
    protected void addLogger(String configuration,
                             BeanDefinitionRegistry registry) {
        final RootBeanDefinition beanDef = this.createBeanDefinition(AvalonLoggerFactoryBean.class, "init", true);
        beanDef.getPropertyValues().addPropertyValue("loggingConfiguration", configuration);
        this.register(beanDef, ProcessingUtil.LOGGER_ROLE, registry);
    }

    /**
     * Helper method to create a new bean definition.
     * @param componentClass    The class of the implementation.
     * @param initMethod        Optional initialization method.
     * @param requiresSettings  If set to true, this bean has a property "settings" for the settings object.
     * @return A new root bean definition.
     */
    protected RootBeanDefinition createBeanDefinition(Class   componentClass,
                                                      String  initMethod,
                                                      boolean requiresSettings) {
        final RootBeanDefinition beanDef = new RootBeanDefinition();
        beanDef.setBeanClass(componentClass);      
        beanDef.setSingleton(true);
        beanDef.setLazyInit(false);
        if ( initMethod != null ) {
            beanDef.setInitMethodName(initMethod);
        }
        if ( requiresSettings ) {
            beanDef.getPropertyValues().addPropertyValue("settings", new RuntimeBeanReference(Settings.ROLE));
        }
        return beanDef;
    }

    /**
     * Add a new bean definition to the registry.
     * @param componentClass    The class of the implementation.
     * @param beanName          The name of the bean.
     * @param initMethod        Optional initialization method.
     * @param requiresSettings  If set to true, this bean has a property "settings" for the settings object.
     * @param registry          The bean registry.
     */
    protected void addComponent(Class                  componentClass,
                                String                 beanName,
                                String                 initMethod,
                                boolean                requiresSettings,
                                BeanDefinitionRegistry registry) {
        final RootBeanDefinition beanDef = this.createBeanDefinition(componentClass, initMethod, requiresSettings);

        this.register(beanDef, beanName, registry);
    }

    public void createConfig(ConfigurationInfo      info,
                             BeanDefinitionRegistry registry) 
    throws Exception {
        final Map components = info.getComponents();
        final List pooledRoles = new ArrayList();

        // Iterate over all definitions
        final Iterator i = components.entrySet().iterator();
        while ( i.hasNext() ) {
            final Map.Entry entry = (Map.Entry)i.next();
            final ComponentInfo current = (ComponentInfo)entry.getValue();
            final String role = current.getRole();
    
            String className = current.getComponentClassName();
            boolean isSelector = false;
            boolean singleton = true;
            boolean poolable = false;
            // Test for Selector - we just create a wrapper for them to flatten the hierarchy
            if ( current.isSelector() ) {
                // Add selector
                className = AvalonServiceSelector.class.getName();
                isSelector = true;
            } else {
                // test for unknown model
                if ( current.getModel() == ComponentInfo.MODEL_UNKNOWN ) {
                    try {
                        final Class serviceClass = Class.forName(className);
                        if ( ThreadSafe.class.isAssignableFrom(serviceClass) ) {
                            current.setModel(ComponentInfo.MODEL_SINGLETON);
                        } else if ( Poolable.class.isAssignableFrom(serviceClass) ) {
                            current.setModel(ComponentInfo.MODEL_POOLED);
                        } else {
                            current.setModel(ComponentInfo.MODEL_PRIMITIVE);
                        }
                    } catch (NoClassDefFoundError ncdfe) {
                        throw new ConfigurationException("Unable to create class for component with role " + current.getRole() + " with class: " + className, ncdfe);
                    } catch (ClassNotFoundException cnfe) {
                        throw new ConfigurationException("Unable to create class for component with role " + current.getRole() + " with class: " + className, cnfe);
                    }
                }
                if ( current.getModel() == ComponentInfo.MODEL_POOLED ) {
                    poolable = true;
                    singleton = false;
                } else if ( current.getModel() != ComponentInfo.MODEL_SINGLETON ) {
                    singleton = false;
                }
            }
            final String beanName;
            if ( !poolable ) {
                beanName = role;
            } else {
                beanName = role + "Pooled";                
            }
            final RootBeanDefinition beanDef = new RootBeanDefinition();
            beanDef.setBeanClassName(className);      
            if ( current.getInitMethodName() != null ) {
                beanDef.setInitMethodName(current.getInitMethodName());
            }
            if ( current.getDestroyMethodName() != null ) {
                beanDef.setDestroyMethodName(current.getDestroyMethodName());
            }
            beanDef.setSingleton(singleton);
            beanDef.setLazyInit(singleton && current.isLazyInit());
            if ( isSelector ) {
                beanDef.getConstructorArgumentValues().addGenericArgumentValue(role.substring(0, role.length()-8), "java.lang.String");
                if ( current.getDefaultValue() != null ) {
                    beanDef.getPropertyValues().addPropertyValue("default", current.getDefaultValue());
                }
            }
            this.register(beanDef, beanName, current.getAlias(), registry);

            if ( poolable ) {
                // add the factory for poolables
                final RootBeanDefinition poolableBeanDef = new RootBeanDefinition();
                poolableBeanDef.setBeanClass(PoolableFactoryBean.class);
                poolableBeanDef.setSingleton(true);
                poolableBeanDef.setLazyInit(false);
                poolableBeanDef.setInitMethodName("initialize");
                poolableBeanDef.setDestroyMethodName("dispose");
                poolableBeanDef.getConstructorArgumentValues().addIndexedArgumentValue(0, beanName, "java.lang.String");
                poolableBeanDef.getConstructorArgumentValues().addIndexedArgumentValue(1, className, "java.lang.String");
                if ( current.getConfiguration() != null ) {
                    // we treat poolMax as a string to allow property replacements
                    final String poolMax = current.getConfiguration().getAttribute("pool-max", null);
                    if ( poolMax != null ) {
                        poolableBeanDef.getConstructorArgumentValues().addIndexedArgumentValue(2, poolMax);
                        poolableBeanDef.getConstructorArgumentValues().addIndexedArgumentValue(3, new RuntimeBeanReference(Settings.ROLE));
                    }
                }
                if ( current.getPoolInMethodName() != null ) {
                    poolableBeanDef.getPropertyValues().addPropertyValue("poolInMethodName", current.getPoolInMethodName());
                }
                if ( current.getPoolOutMethodName() != null ) {
                    poolableBeanDef.getPropertyValues().addPropertyValue("poolOutMethodName", current.getPoolOutMethodName());
                }
                this.register(poolableBeanDef, role, registry);
                pooledRoles.add(role);
            }
        }

        // now change roles for pooled components (from {role} to {role}Pooled
        final Iterator prI = pooledRoles.iterator();
        while ( prI.hasNext() ) {
            final String role = (String)prI.next();
            final Object pooledInfo = components.remove(role);
            components.put(role + "Pooled", pooledInfo);
        }
    }

    protected void registerComponentInfo(ConfigurationInfo      configInfo,
                                         BeanDefinitionRegistry registry) {
        ProcessorComponentInfo info = new ProcessorComponentInfo(null);
        final Iterator i = configInfo.getComponents().values().iterator();
        while (i.hasNext()) {
            final ComponentInfo current = (ComponentInfo) i.next();
            info.componentAdded(current.getRole(), current.getComponentClassName(), current.getConfiguration());
        }
        prepareSelector(info, configInfo, Generator.ROLE);
        prepareSelector(info, configInfo, Transformer.ROLE);
        prepareSelector(info, configInfo, Serializer.ROLE);
        prepareSelector(info, configInfo, ProcessingPipeline.ROLE);
        prepareSelector(info, configInfo, Action.ROLE);
        prepareSelector(info, configInfo, Selector.ROLE);
        prepareSelector(info, configInfo, Matcher.ROLE);
        prepareSelector(info, configInfo, Reader.ROLE);
        info.lock();
        final RootBeanDefinition beanDef = new RootBeanDefinition();
        beanDef.setBeanClass(ProcessorComponentInfoFactoryBean.class);
        beanDef.setSingleton(true);
        beanDef.setLazyInit(false);
        beanDef.getPropertyValues().addPropertyValue("data", info.getData());
        beanDef.setInitMethodName("init");
        this.register(beanDef, ProcessorComponentInfo.ROLE, registry);

        final RootBeanDefinition ciBeanDef = new RootBeanDefinition();
        ciBeanDef.setBeanClass(ConfigurationInfoFactoryBean.class);
        ciBeanDef.setSingleton(true);
        ciBeanDef.setLazyInit(false);
        ciBeanDef.getPropertyValues().addPropertyValue("configurationInfo", configInfo);
        this.register(ciBeanDef, ConfigurationInfo.class.getName(), registry);
    }

    protected static void prepareSelector(ProcessorComponentInfo info,
                                          ConfigurationInfo configInfo,
                                          String category) {
        final ComponentInfo component = (ComponentInfo) configInfo.getComponents().get(category + "Selector");
        if (component != null) {
            info.setDefaultType(category, component.getDefaultValue());
        }
    }
}
