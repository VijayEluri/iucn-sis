package org.iucn.sis.client.panels;

import org.iucn.sis.client.api.caches.AssessmentCache;
import org.iucn.sis.client.api.caches.FetchMode;
import org.iucn.sis.client.api.caches.TaxonomyCache;
import org.iucn.sis.client.api.caches.WorkingSetCache;
import org.iucn.sis.client.api.container.StateChangeEvent;
import org.iucn.sis.client.api.container.StateManager;
import org.iucn.sis.client.api.container.StateManager.StateChangeEventType;
import org.iucn.sis.client.api.utils.SIS;
import org.iucn.sis.client.panels.login.LoginPanel;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.TaxonHierarchy;
import org.iucn.sis.shared.api.models.WorkingSet;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Viewport;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.History;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.events.ComplexListener;
import com.solertium.util.extjs.client.WindowUtils;

/**
 * This is the class on which everything else will lay. It will take up all of
 * the available real estate in the client's browser and arrange child widgets
 * vertically, by default, or by the specified style.
 * 
 * @author adam.schwartz
 * 
 */
public class ClientUIContainer extends Viewport implements ValueChangeHandler<String> {
	
	private LoginPanel loginPanel = null;

	public static BodyContainer bodyContainer = null;
	public static HeaderContainer headerContainer = null;

	private Boolean loggedIn;

	public ClientUIContainer() {
		super();
		setLayout(new FitLayout());
		setLayoutOnChange(true);
	}

	public void buildLogin(String message) {
		if (loginPanel == null)
			loginPanel = new LoginPanel();
		
		if (loggedIn == null || loggedIn.booleanValue()) {
			removeAll();
			add(loginPanel);
		}
		
		if (message == null || message.contains("Log out"))
			loginPanel.clearCredentials();
		else
			loginPanel.clearPassword(true);
		
		loginPanel.setMessage(message);
		
		loggedIn = false;

		bodyContainer = null;
		headerContainer = null;
	}

	public void buildPostLogin(String first, String last, String affiliation) {
		loggedIn = true;

		removeAll();

		bodyContainer = new BodyContainer();
		headerContainer = new HeaderContainer(first, last, affiliation);

		BorderLayoutData headerData = new BorderLayoutData(LayoutRegion.NORTH);
		headerData.setFloatable(true);
		headerData.setCollapsible(true);
		headerData.setSplit(true);
		headerData.setMinSize(200);
		headerData.setMaxSize(400);
		
		BorderLayoutData bodyData =new BorderLayoutData(LayoutRegion.CENTER);
		bodyData.setMinSize(500);
		
		final LayoutContainer container = new LayoutContainer(new BorderLayout());

		container.add(headerContainer, headerData);
		container.add(bodyContainer, bodyData);
		
		if (SIS.isOffline()) {
			container.add(new OfflineFooter(), new BorderLayoutData(LayoutRegion.SOUTH, 25, 25, 25));
		}
		
		add(container);
		
		WindowUtils.hideLoadingAlert();
		
		StateManager.impl.addStateChangeListener(StateChangeEventType.StateChanged, new ComplexListener<StateChangeEvent>() {
			public void handleEvent(StateChangeEvent eventData) {
				boolean updateNavigation = !ClientUIContainer.headerContainer.centerPanel.equals(eventData.getSource());
								
				History.newItem(eventData.getToken(), false);
				
				onHistoryChanged(eventData, updateNavigation);
			}
		});
		
		History.addValueChangeHandler(this);
		
		String bookmark = History.getToken();
		if (bookmark == null || "".equals(bookmark))
			onHistoryChanged(new StateChangeEvent(null, null, null, this), false);
		else
			onHistoryChanged(bookmark);
	}
	
	public void onValueChange(ValueChangeEvent<String> event) {
		onHistoryChanged(event.getValue());
	}
	
	public void onHistoryChanged(String historyToken) {
		if (historyToken == null)
			return;
		
		StringBuilder workingSet = new StringBuilder();
		StringBuilder taxon = new StringBuilder();
		StringBuilder assessment = new StringBuilder();
		
		StringBuilder buffer = null;
		
		for (char c : historyToken.toCharArray()) {
			if ('w' == c || 'W' == c)
				buffer = workingSet;
			else if ('t' == c || 'T' == c)
				buffer = taxon;
			else if ('a' == c || 'A' == c)
				buffer = assessment;
			else if (Character.isDigit(c) && buffer != null)
				buffer.append(c);
		}
		
		final Integer workingSetID = parseID(workingSet.toString());
		final Integer taxonID = parseID(taxon.toString());
		final Integer assessmentID = parseID(assessment.toString());
		
		final WorkingSet ws;
		if (workingSetID != null) {
			ws = WorkingSetCache.impl.getWorkingSet(workingSetID);
			if (ws == null) {
				failAndBail("Could not find working set " + workingSetID);
				return;
			}
		}
		else
			ws = null;
		
		if (taxonID != null) {
			WindowUtils.showLoadingAlert("Loading...");
			TaxonomyCache.impl.fetchPathWithID(taxonID, new GenericCallback<TaxonHierarchy>() {
				public void onSuccess(final TaxonHierarchy result) {
					if (assessmentID != null) {
						AssessmentCache.impl.fetchPartialAssessmentsForTaxon(taxonID, new GenericCallback<String>() {
							public void onSuccess(String list) {
								AssessmentCache.impl.fetchAssessment(assessmentID, FetchMode.FULL, new GenericCallback<Assessment>() {
									public void onSuccess(Assessment assessment) {
										try {
											StateManager.impl.setState(ws, result.getTaxon(), assessment);
										} catch (Throwable e) {
											Debug.println(e);
										}
									}
									public void onFailure(Throwable caught) {
										failAndBail("Could not load assessment " + assessmentID);
									}
								});
							}
							public void onFailure(Throwable caught) {
								failAndBail("Could not load assessments for taxon " + taxonID);
							}
						});
					}
					else {
						StateManager.impl.setState(ws, result.getTaxon(), null);
					}
				}
				public void onFailure(Throwable caught) {
					failAndBail("Could not find taxon " + taxonID);
				}
			});
		}
		else {
			StateManager.impl.setState(ws, null, null);
		}
	}
	
	private void failAndBail(String message) {
		WindowUtils.errorAlert(message);
		
		onHistoryChanged(new StateChangeEvent(null, null, null, this), false);
	}
	
	private Integer parseID(String value) {
		try {
			return Integer.valueOf(value);
		} catch (Exception e) {
			return null;
		}
	}
	
	public void onHistoryChanged(final StateChangeEvent eventData, final boolean updateNavigation) {
		DeferredCommand.addCommand(new Command() {
			public void execute() {
				if (eventData.getAssessment() != null)
					ClientUIContainer.bodyContainer.openAssessment(eventData.getUrl(), updateNavigation);
				else if (eventData.getTaxon() != null)
					ClientUIContainer.bodyContainer.openTaxon(eventData.getUrl(), updateNavigation);
				else if (eventData.getWorkingSet() != null)
					ClientUIContainer.bodyContainer.openWorkingSet(eventData.getUrl(), updateNavigation);
				else
					ClientUIContainer.bodyContainer.openHomePage(true);
			}
		});
		DeferredCommand.addCommand(new Command() {
			public void execute() {
				String display = eventData.getDisplayName();
				if ("".equals(display))
					display = "Home";
				SIS.recordAnalytics(eventData.getToken() + ": " + display);
			}
		});
	}
	
	public boolean isLoggedIn() {
		return loggedIn;
	}
	
	
	
}
