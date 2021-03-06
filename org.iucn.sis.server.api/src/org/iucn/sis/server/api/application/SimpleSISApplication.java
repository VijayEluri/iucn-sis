package org.iucn.sis.server.api.application;

import org.iucn.sis.server.api.restlets.BaseServiceRestlet;
import org.iucn.sis.server.api.restlets.SimpleRestlet;

/**
 * SimpleSISApplication.java
 * 
 * Simple SIS Applications run in one mode and one 
 * mode only -- either online, offline, or dual. 
 * There is no mix.  By default, it runs as dual,
 * meaning all paths available both online & offline, 
 * but this can be changed via setting the mode in 
 * the constructor.
 * 
 * @author carl.scott
 *
 */
public abstract class SimpleSISApplication extends SISApplication {
	
	protected static enum RunMode {
		ONLINE, OFFLINE, DUAL
	}
	
	private final RunMode mode;
	
	protected SimpleSISApplication() {
		this(RunMode.DUAL);
	}
	
	protected SimpleSISApplication(RunMode mode) {
		this.mode = mode;
	}
	
	public abstract void init();
	
	@Override
	protected final void initOffline() {
		if (RunMode.DUAL.equals(mode) || RunMode.OFFLINE.equals(mode))
			init();
	}
	
	@Override
	protected final void initOnline() {
		if (RunMode.DUAL.equals(mode) || RunMode.ONLINE.equals(mode))
			init();
	}
	
	protected void addServiceToRouter(BaseServiceRestlet curService) {
		addResource(curService, curService.getPaths(), false);
	}
	
	protected void addServiceToRouter(SimpleRestlet curService) {
		addResource(curService, curService.getPaths(), false);
	}

}
