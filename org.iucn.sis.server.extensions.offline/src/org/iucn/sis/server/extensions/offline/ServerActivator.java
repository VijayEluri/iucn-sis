package org.iucn.sis.server.extensions.offline;

import org.iucn.sis.server.api.application.SISActivator;
import org.iucn.sis.server.api.application.SISApplication;

public class ServerActivator extends SISActivator{

	@Override
	protected String getAppDescription() {
		return "SIS Offline Support";
	}
	
	@Override
	protected String getAppName() {
		return "SIS Offline Support";
	}
	
	@Override
	protected SISApplication getInstance() {
		return new ServerApplication();
	}
	
}
