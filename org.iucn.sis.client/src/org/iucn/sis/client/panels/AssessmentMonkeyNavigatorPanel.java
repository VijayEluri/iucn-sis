package org.iucn.sis.client.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.iucn.sis.client.api.assessment.AssessmentClientSaveUtils;
import org.iucn.sis.client.api.caches.AssessmentCache;
import org.iucn.sis.client.api.caches.AuthorizationCache;
import org.iucn.sis.client.api.caches.MarkedCache;
import org.iucn.sis.client.api.caches.RegionCache;
import org.iucn.sis.client.api.caches.SchemaCache;
import org.iucn.sis.client.api.caches.TaxonomyCache;
import org.iucn.sis.client.api.caches.WorkingSetCache;
import org.iucn.sis.client.api.utils.FormattedDate;
import org.iucn.sis.shared.api.acl.base.AuthorizableObject;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.Taxon;
import org.iucn.sis.shared.api.models.TaxonLevel;
import org.iucn.sis.shared.api.models.WorkingSet;
import org.iucn.sis.shared.api.models.comparators.AssessmentDateComparator;
import org.iucn.sis.shared.api.models.comparators.AssessmentNavigationComparator;
import org.iucn.sis.shared.api.utils.AssessmentFormatter;

import com.extjs.gxt.ui.client.data.ModelKeyProvider;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.StoreSorter;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnData;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridCellRenderer;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.events.SimpleListener;
import com.solertium.util.extjs.client.WindowUtils;
import com.solertium.util.gwt.ui.DrawsLazily;

public class AssessmentMonkeyNavigatorPanel extends GridNonPagingMonkeyNavigatorPanel<Assessment> {
	
	private WorkingSet curNavWorkingSet;
	private Taxon curNavTaxon;
	private Assessment curNavAssessment;
	
	public AssessmentMonkeyNavigatorPanel() {
		super();
		
		setHeading("Assessments");
	}
	
	public void refresh(WorkingSet curNavWorkingSet, Taxon curNavTaxon, final Assessment curNavAssessment) {
		this.curNavWorkingSet = curNavWorkingSet;
		this.curNavTaxon = curNavTaxon;
		this.curNavAssessment = curNavAssessment;
		
		refresh(new DrawsLazily.DoneDrawingCallback() {
			public void isDrawn() {
				DeferredCommand.addCommand(new Command() {
					public void execute() {
						setSelection(curNavAssessment);
					}
				});
			}
		});
	}
	
	@Override
	protected String getEmptyText() {
		return "No assessments to list.";
	}
	
	@Override
	protected void setSelection(Assessment curNavAssessment) {
		final NavigationModelData<Assessment> selection;
		if (curNavAssessment != null)
			selection = getProxy().getStore().findModel("" + curNavAssessment.getId());
		else
			selection = null;
		
		Debug.println("Selected assessment from nav is {0}, found {1}", curNavAssessment, selection);
		if (selection != null) {
			((NavigationGridSelectionModel<Assessment>)grid.getSelectionModel()).
				highlight(selection);
			
			DeferredCommand.addPause();
			DeferredCommand.addCommand(new Command() {
				public void execute() {
					grid.getView().focusRow(grid.getStore().indexOf(selection));
				}
			});
		}
	}
	
	@Override
	protected void onLoad() {
		super.onLoad();
		
		setSelection(curNavAssessment);
	}
	
	@Override
	protected ColumnModel getColumnModel() {
		final List<ColumnConfig> list = new ArrayList<ColumnConfig>();
		
		ColumnConfig display = new ColumnConfig("name", "Name", 100);
		display.setRenderer(new GridCellRenderer<NavigationModelData<Assessment>>() {
			@Override
			public Object render(NavigationModelData<Assessment> model, String property,
					ColumnData config, int rowIndex, int colIndex,
					ListStore<NavigationModelData<Assessment>> store, Grid<NavigationModelData<Assessment>> grid) {
				Boolean header = model.get("header");
				if (header == null)
					header = Boolean.FALSE;
				Assessment assessment = model.getModel();
				String style;
				String value;
				boolean locked = false;
				if (assessment == null) {
					style = header ? "monkey_navigation_section_header" : MarkedCache.NONE;
					value = model.get(property);
				}
				else {
					style = MarkedCache.impl.getAssessmentStyle(assessment.getId());
					value = !assessment.isPublished() ? 
							getDefaultDisplayableString(assessment) : getPublishedDisplayableString(assessment);
					locked = !AuthorizationCache.impl.hasRight(AuthorizableObject.READ, assessment);
				}
				
				if (!locked)
					return "<div class=\"" + style + "\">" + value + "</div>";
				else {
					return "<div>" + 
						"<span style=\"float:left;\" class=\"" + style + "\">" + value + "</span>" +
						"<span style=\"width:20px;height:20px;float:right;\" class=\"icon-lock\">&nbsp;</span>" + 
						"</div>";
				}
			}
		});
		
		list.add(display);
		
		return new ColumnModel(list);
	}
	
	@Override
	protected void getStore(final GenericCallback<ListStore<NavigationModelData<Assessment>>> callback) {
		if (curNavTaxon == null)
			callback.onSuccess(new ListStore<NavigationModelData<Assessment>>());
		else if (curNavWorkingSet == null) {
			if (curNavTaxon.getLevel() >= TaxonLevel.SPECIES)
				AssessmentCache.impl.fetchPartialAssessmentsForTaxon(curNavTaxon.getId(), new GenericCallback<String>() {
					public void onFailure(Throwable caught) {
						callback.onSuccess(new ListStore<NavigationModelData<Assessment>>());
					}
					public void onSuccess(String unused) {
						callback.onSuccess(createStore(new ArrayList<Assessment>(
							AssessmentCache.impl.getAllAssessmentsForTaxon(curNavTaxon.getId())
						)));
					}
				});
			else
				callback.onSuccess(new ListStore<NavigationModelData<Assessment>>());
		}
		else {
			WorkingSetCache.impl.getAssessmentsForWorkingSet(curNavWorkingSet, curNavTaxon, new GenericCallback<List<Assessment>>() {
				public void onSuccess(List<Assessment> result) {
					callback.onSuccess(createStore(result));
				}
				public void onFailure(Throwable caught) {
					WindowUtils.errorAlert("Could not load assessments for the taxon in this working set.");
				}
			});
		}
	}
	
	private ListStore<NavigationModelData<Assessment>> createStore(List<Assessment> assessments) {
		final ListStore<NavigationModelData<Assessment>> store = 
			new ListStore<NavigationModelData<Assessment>>();
		store.setKeyProvider(new ModelKeyProvider<NavigationModelData<Assessment>>() {
			public String getKey(NavigationModelData<Assessment> model) {
				if (model.getModel() == null)
					return "-1";
				else
					return Integer.toString(model.getModel().getId());
			}
		});
		
		Collections.sort(assessments, new AssessmentGroupedComparator());
		
		String type = null;
		NavigationModelData<Assessment> currentHeader = null;
		int groupCount = 0;
		
		for (Assessment current : assessments) {
			if (!current.getAssessmentType().getDisplayName(true).equals(type)) {
				if (currentHeader != null)
					updateHeaderCount(currentHeader, groupCount);
				
				type = current.getAssessmentType().getDisplayName(true);
				
				NavigationModelData<Assessment> header = new NavigationModelData<Assessment>(null);
				header.set("name", type + " Assessments");
				header.set("header", Boolean.TRUE);
				
				store.add(header);
				
				currentHeader = header;
				groupCount = 0;
			}
			
			NavigationModelData<Assessment> model = new NavigationModelData<Assessment>(current);
			model.set("header", Boolean.FALSE);
			if (current.isPublished()) {
				model.set("name", getPublishedDisplayableString(current));
				model.set("status", current.getType());
			}
			else {
				model.set("name", getDefaultDisplayableString(current));
				model.set("status", current.getType());
				model.set("locked", !AuthorizationCache.impl.hasRight(AuthorizableObject.READ, current));
			}
			
			store.add(model);
			
			groupCount++;
		}
		
		updateHeaderCount(currentHeader, groupCount);
		
		System.out.println("Store contains " + store.getCount() + " / " + assessments.size() + " assessments.");
		
		return store;
	}
	
	private void updateHeaderCount(NavigationModelData<Assessment> header, int count) {
		if (header != null) {
			String name = header.get("name");
			name += " (" + count + ")";
			header.set("name", name);
		}
	}
	
	private String getDefaultDisplayableString(Assessment current) {
		String type = current.getAssessmentType().getDisplayName(true);
		
		String displayable;
		if (current.getDateAssessed() != null )
			displayable = FormattedDate.impl.getDate();
		else
			displayable = "";
		
		if (AuthorizationCache.impl.hasRight(AuthorizableObject.READ, current)) {
			if (displayable == null || displayable.equals(""))
				if (current.getLastEdit() == null)
					displayable = "New";
				else
					displayable = FormattedDate.impl.getDate(current.getLastEdit().getCreatedDate());

			if (!current.hasRegions())
				displayable += " --- No Regions";
			else if (current.isRegional())
				displayable += " --- " + RegionCache.impl.getRegionName(current.getRegionIDs());
			else
				displayable += " --- " + "Global";
		} else {
			if (!current.hasRegions())
				displayable = SchemaCache.impl.getFromCache(current.getSchema()).getName() + " " + type + " Assessment";
			else {
				if (current.isRegional())
					displayable = "Regional " + type + " Assessment";
				else
					displayable = "Global " + type + " Assessment";
			}
		}
		
		return displayable;
	}
	
	private String getPublishedDisplayableString(Assessment current) {
		String displayable;
		if (current.getDateAssessed() != null)
			displayable = FormattedDate.impl.getDate(current.getDateAssessed());
		else
			displayable = "";

		if (!current.hasRegions())
			displayable += " --- " + "No Regions";
		else if (current.isRegional())
			displayable += " --- " + RegionCache.impl.getRegionName(current.getRegionIDs());
		else
			displayable += " --- " + "Global";

		displayable += " --- " + AssessmentFormatter.getProperCategoryAbbreviation(current);
		
		return displayable;
	}
	
	@Override
	protected void mark(NavigationModelData<Assessment> model, String color) {
		if (!hasSelection())
			return;
		
		final Integer assessmentID = model.getModel().getId();
		
		MarkedCache.impl.markAssement(assessmentID, color);
		
		refreshView();
	}
	
	@Override
	protected void onSelectionChanged(NavigationModelData<Assessment> model) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void open(NavigationModelData<Assessment> model) {
		Assessment selection = getSelected();
		if (selection != null) {
			if (canRead(selection))
				navigate(curNavWorkingSet, curNavTaxon, selection);
			else
				WindowUtils.errorAlert("Sorry, you do not have permission to view this assessment.");
		}
	}
	
	@Override
	protected void setupToolbar() {
		final Button jump = new Button();
		jump.setIconStyle("icon-go-jump");
		jump.addSelectionListener(new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				AssessmentClientSaveUtils.saveIfNecessary(new SimpleListener() {
					public void handleEvent() {
						open(grid.getSelectionModel().getSelectedItem());
					}
				});
			}
		});

		addTool(jump);	
	}

	private boolean canRead(Assessment assessment) {
		return AuthorizationCache.impl.hasRight(AuthorizableObject.READ, assessment);
	}
	
	public static class AssessmentStoreSorter extends StoreSorter<NavigationModelData<Assessment>> {
		
		private final AssessmentDateComparator comparator = 
			new AssessmentDateComparator();
		
		@Override
		public int compare(Store<NavigationModelData<Assessment>> store,
				NavigationModelData<Assessment> m1,
				NavigationModelData<Assessment> m2, String property) {
			return comparator.compare(m1.getModel(), m2.getModel());
		}
		
	}
	
	public static class AssessmentGroupedComparator extends AssessmentNavigationComparator {
		
		@Override
		public Taxon getTaxonForAssessment(Assessment assessment) {
			return TaxonomyCache.impl.getTaxon(assessment.getTaxon().getId());
		}
		
	}

}
