/*
 * Copyright (C) 2007-2009 Solertium Corporation
 *
 * This file is part of the open source GoGoEgo project.
 *
 * Unless you have been granted a different license in writing by the
 * copyright holders for GoGoEgo, you may only modify or redistribute
 * this code under the terms of one of the following licenses:
 * 
 * 1) The Eclipse Public License, v.1.0
 *    http://www.eclipse.org/legal/epl-v10.html
 *
 * 2) The GNU General Public License, version 2 or later
 *    http://www.gnu.org/licenses
 */
package org.gogoego.api.scripting;

import java.util.Hashtable;

import javax.script.ScriptEngineFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * ScriptableObjectActivator.java
 * 
 * A simple activator that can be subclassed to guarantee that your 
 * script engine is instantiated correctly within GoGoEgo.
 * 
 * @author <a href="mailto:carl.scott@solertium.com">Carl Scott</a>, <a
 *         href="http://www.solertium.com">Solertium Corporation</a>
 *
 */
public abstract class ScriptEngineActivator implements BundleActivator {
	
	/**
	 * Retrieve the ScriptEngineFactory. If you are porting from the 
	 * service provider method, the name of this class can be found 
	 * in META-INF/services/javax.script.ScriptEngineFactory
	 * 
	 * @return the engine factory
	 */
	public abstract ScriptEngineFactory getService();

	public final void start(BundleContext context) throws Exception {
		extractLibs(context);
		
		final Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(Constants.SERVICE_PID, getClass().getName());
		
		final ScriptEngineFactory service = getService();
		context.registerService(ScriptEngineFactory.class.getName(), service,
				props);
	}
	
	/**
	 * This can be used to extract libraries from the script bundle to a working
	 * directory, for script engines which require a physical file or filesystem
	 * to access their libraries.  By default, this does nothing.  This operation
	 * is completed BEFORE the script engine is registered.
	 */
	protected void extractLibs(BundleContext context){
	}

	public final void stop(BundleContext context) throws Exception {
	}

}
