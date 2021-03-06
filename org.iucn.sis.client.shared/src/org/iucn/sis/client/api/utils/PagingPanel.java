package org.iucn.sis.client.api.utils;

import java.util.List;

import com.extjs.gxt.ui.client.data.BasePagingLoadConfig;
import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.FilterConfig;
import com.extjs.gxt.ui.client.data.FilterPagingLoadConfig;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.extjs.client.WindowUtils;
import com.solertium.util.gwt.ui.DrawsLazily;

public abstract class PagingPanel<T extends ModelData> extends LayoutContainer {
	
	private BasePagingLoader<BasePagingLoadResult<T>> loader;
	private MemoryProxy<T> proxy;
	
	private int pageCount;
	
	public PagingPanel() {
		super();
		
		proxy = new MemoryProxy<T>();
		
		loader = 
			new BasePagingLoader<BasePagingLoadResult<T>>(proxy) {
			protected Object newLoadConfig() {
				return new PagingPanelLoadConfig();
			}
		};
		loader.setRemoteSort(false);
		
		pageCount = 25;
	}
	
	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}
	
	protected ListStore<T> getStoreInstance() {
		return new ListStore<T>(loader);
	}
	
	public MemoryProxy<T> getProxy() {
		return proxy;
	}
	
	public BasePagingLoader<BasePagingLoadResult<T>> getLoader() {
		return loader;
	}
	
	protected PagingToolBar getPagingToolbar() {
		final PagingToolBar paging = new PagingToolBar(pageCount);
		paging.bind(loader);
		
		return paging;
	}
	
	protected void refresh(final DrawsLazily.DoneDrawingCallback callback) {
		getStore(new GenericCallback<ListStore<T>>() {
			public void onFailure(Throwable caught) {
				WindowUtils.errorAlert("Could not refresh, please try again later.");
				if (callback != null)
					callback.isDrawn();
			}
			public void onSuccess(ListStore<T> result) {
				proxy.setStore(result);
				
				int refreshOffset = loader.getOffset();
				int resultCount = result.getCount();
				if(resultCount <= refreshOffset){
					refreshOffset = refreshOffset - pageCount;
					if(refreshOffset < 0)
						refreshOffset = 0;
				}
				loader.load(refreshOffset, pageCount);
				
				refreshView();
				/*try {
					grid.getView().refresh(false);
				} catch (Throwable e) {
				}*/
				
				WindowUtils.hideLoadingAlert();
				if (callback != null)
					callback.isDrawn();
			}
		});
	}
	
	protected abstract void refreshView();
	
	protected abstract void getStore(final GenericCallback<ListStore<T>> callback);
	
	public static class PagingPanelLoadConfig extends BasePagingLoadConfig implements FilterPagingLoadConfig {
		
		private static final long serialVersionUID = 1L;
		
		private List<FilterConfig> configs;
		
		public PagingPanelLoadConfig() {
			super();
		}
		
		@Override
		public List<FilterConfig> getFilterConfigs() {
			return configs;
		}
		
		@Override
		public void setFilterConfigs(List<FilterConfig> configs) {
			this.configs = configs;
		}
		
	}

}
