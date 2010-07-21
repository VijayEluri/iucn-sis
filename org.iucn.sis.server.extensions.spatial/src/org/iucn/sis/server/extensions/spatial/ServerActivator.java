package org.iucn.sis.server.extensions.spatial;

import org.iucn.sis.server.api.application.SISActivator;
import org.iucn.sis.server.api.application.SISApplication;

public class ServerActivator extends SISActivator{

	@Override
	protected String getAppDescription() {
		return "SIS Spatial Information";
	}
	
	@Override
	protected String getAppName() {
		return "SIS Spatial Information";
	}
	
	@Override
	protected SISApplication getInstance() {
		return new ServerApplication();
	}
	
}
