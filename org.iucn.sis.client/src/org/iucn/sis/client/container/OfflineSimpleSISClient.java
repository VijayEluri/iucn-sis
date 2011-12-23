package org.iucn.sis.client.container;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.iucn.sis.client.api.caches.OfflineCache;
import org.iucn.sis.client.api.container.SISClientBase;
import org.iucn.sis.client.api.utils.HasCache;

public class OfflineSimpleSISClient extends SimpleSISClient {
	
	@Override
	public void loadModule() {
		SISClientBase.iAmOnline = false;
		
		super.loadModule();
	}
	
	@Override
	protected Collection<HasCache> getCachesToInitialize() {		
		List<HasCache> list = new ArrayList<HasCache>(super.getCachesToInitialize());
		list.add(OfflineCache.impl);		
		return list;
	}
	
}
