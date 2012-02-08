package org.iucn.sis.client.api.caches;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.iucn.sis.client.api.container.SISClientBase;
import org.iucn.sis.client.api.ui.views.SISPageHolder;
import org.iucn.sis.client.api.ui.views.SISView;
import org.iucn.sis.client.api.utils.UriBase;
import org.iucn.sis.shared.api.acl.base.AuthorizableObject;
import org.iucn.sis.shared.api.utils.XMLUtils;

import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.lwxml.shared.NativeElement;
import com.solertium.lwxml.shared.NativeNodeList;
import com.solertium.util.events.ComplexListener;
import com.solertium.util.gwt.ui.DrawsLazily;
import com.solertium.util.portable.PortableAlphanumericComparator;

public class ViewCache {
	
	public enum EditStatus {
		READ_ONLY, EDIT_DATA
	}

	public static final ViewCache impl = new ViewCache();

	private Map<String, Map<String, SISView>> schemaToViews;
	private Map<String, SISView> currentViewMap;
	
	private HashMap<String, Integer> lastPageViewed;

	SISView currentView = null;
	String currentSchema = null;
	
	EditStatus editStatus;

	private ViewCache() {
		schemaToViews = new HashMap<String, Map<String,SISView>>();
		lastPageViewed = new HashMap<String, Integer>();
		
		editStatus = EditStatus.EDIT_DATA;
	}

	public void doLogout() {
		schemaToViews.clear();
		lastPageViewed.clear();
		editStatus = EditStatus.EDIT_DATA;
		
		currentViewMap = null;
		currentView = null;
		currentSchema = null;
	}
	
	private void resetSchema(String schema) {
		currentViewMap = schemaToViews.get(schema);
		if (currentView != null)
			currentView.resetCurPage();
		currentView = null;
		currentSchema = schema;	
	}

	public void fetchViews(final String schema, final GenericCallback<String> wayback) {
		if (schemaToViews.containsKey(schema)) {
			if (!schema.equals(currentSchema))
				resetSchema(schema);
			
			wayback.onSuccess(null);
		}
		else {
			final NativeDocument viewsDocument = SISClientBase.getHttpBasicNativeDocument();
			viewsDocument.get(UriBase.getInstance().getSISBase() + "/application/schema/" + schema + "/view", new GenericCallback<String>() {
				public void onFailure(Throwable arg0) {
					wayback.onFailure(arg0);
				}
	
				public void onSuccess(String arg0) {
					NativeNodeList viewElements = viewsDocument.getDocumentElement().getElementsByTagName("view");
					Map<String, SISView> views = new LinkedHashMap<String, SISView>();
					for (int i = 0; i < viewElements.getLength(); i++) {
						NativeElement root = viewElements.elementAt(i);
						SISView curView = new SISView();
	
						curView.setDisplayableTitle(root.getAttribute("title"));
						curView.setId(root.getAttribute("id"));
	
						NativeNodeList pages = root.getElementsByTagName("page");
						for (int k = 0; k < pages.getLength(); k++) {
							NativeElement rootPageTag = pages.elementAt(k);
							// Create new SISPageHolders for each page in the view
							if (rootPageTag.getNodeName().equalsIgnoreCase("page")) {
								SISPageHolder curPage = new SISPageHolder(XMLUtils.getXMLAttribute(rootPageTag, "title", null), XMLUtils
										.getXMLAttribute(rootPageTag, "id"), rootPageTag);
	
								curView.addPage(curPage);
							}
						}
	
						views.put(curView.getId(), curView);
					}
					schemaToViews.put(schema, views);
					
					resetSchema(schema);
					wayback.onSuccess(null);
				}
			});
		}
	}
	
	public void getGrantableViews(final String schema, final ComplexListener<List<SISView>> callback) {
		fetchViews(schema, new GenericCallback<String>() {
			public void onSuccess(String result) {
				List<SISView> grantable = new ArrayList<SISView>();
				for (SISView view : getAvailableViews())
					if (AuthorizationCache.impl.hasRight(AuthorizableObject.GRANT, view))
						grantable.add(view);
				Collections.sort(grantable, new SISViewComparator());
				callback.handleEvent(grantable);
			}
			public void onFailure(Throwable caught) {
				callback.handleEvent(new ArrayList<SISView>());	
			}
		});
	}

	public Set<String> getAvailableKeys() {
		if (currentViewMap != null)
			return currentViewMap.keySet();
		else
			return new HashSet<String>();
	}

	public Collection<SISView> getAvailableViews() {
		if (currentViewMap != null)
			return currentViewMap.values();
		else
			return new ArrayList<SISView>();
	}

	public SISView getCurrentView() {
		return currentView;
	}
	
	public String getCurrentSchema() {
		return currentSchema;
	}

	public int getLastPageViewed(String viewID) {
		try {
			return lastPageViewed.get(viewID).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	public SISView getView(String viewID) {
		return currentViewMap.get(viewID);
	}

	public boolean isEmpty() {
		return currentViewMap.isEmpty();
	}

	public boolean needPageChange(String viewID, int pageNum, boolean viewOnly) {
		if (currentView == null || !currentView.getId().equals(viewID))
			return true;
		else
			return currentViewMap.get(viewID).needPageChange(pageNum, viewOnly);
	}

	public void showPage(String viewID, int pageNum, boolean viewOnly, 
			DrawsLazily.DoneDrawingCallbackWithParam<SISPageHolder> callback) {
		currentView = currentViewMap.get(viewID);
		lastPageViewed.put(viewID, new Integer(pageNum));
		
		currentViewMap.get(viewID).showPage(pageNum, viewOnly, callback);
	}
	
	public EditStatus getEditStatus() {
		return editStatus;
	}
	
	public void setEditStatus(EditStatus editStatus) {
		this.editStatus = editStatus;
	}
	
	private static class SISViewComparator implements Comparator<SISView> {
		
		private final PortableAlphanumericComparator comparator;
		
		public SISViewComparator() {
			this.comparator = new PortableAlphanumericComparator();
		}
		
		@Override
		public int compare(SISView arg0, SISView arg1) {
			String t1 = arg0.getDisplayableTitle();
			String t2 = arg1.getDisplayableTitle();
			
			if (t1 == null) t1 = "";
			if (t2 == null) t2 = "";
			
			return comparator.compare(t1, t2);
		}
		
	}
}
