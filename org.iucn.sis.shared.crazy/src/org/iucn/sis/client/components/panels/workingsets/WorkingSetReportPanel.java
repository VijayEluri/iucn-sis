package org.iucn.sis.client.components.panels.workingsets;

import org.iucn.sis.client.components.panels.PanelManager;
import org.iucn.sis.client.components.panels.filters.AssessmentFilterPanel;
import org.iucn.sis.client.integrity.ClientAssessmentValidator;
import org.iucn.sis.client.ui.RefreshLayoutContainer;
import org.iucn.sis.shared.data.WorkingSetCache;
import org.iucn.sis.shared.data.WorkingSetData;
import org.iucn.sis.shared.data.assessments.AssessmentFilter;

import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.FieldEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.Radio;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.extjs.client.WindowUtils;

public class WorkingSetReportPanel extends RefreshLayoutContainer {

	private PanelManager manager = null;
	private FlexTable grid = null;
	private HTML instructions = null;
	private Button taxaReport = null;
	private Button generalReport = null;
	private Button integrityReport = null;

	public WorkingSetReportPanel(PanelManager manager) {
		super();
		this.manager = manager;
		build();
		addStyleName("gwt-background");
		setScrollMode(Scroll.AUTO);
	}

	private void build() {
		RowLayout layout = new RowLayout();
		// layout.setMargin(6);
		// layout.setSpacing(20);
		setLayout(layout);
		buildInstructions();
		buildGrid();
		setScrollMode(Scroll.AUTO);
	}

	private void buildGrid() {
		grid = new FlexTable();
		grid.setCellSpacing(10);

		taxaReport = new Button("Taxa List", new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				createTaxaList();
			}
		});
		taxaReport.setWidth("140px");
		HTML html = new HTML("-- Creates a .csv file that you can open with Excel to "
				+ "view the entire taxa footprint for each taxa in the current working set.");
		grid.setWidget(0, 0, taxaReport);
		grid.setWidget(0, 1, html);

		generalReport = new Button("General Report", new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				createGeneralReport();
			}
		});
		generalReport.setWidth("140px");
		html = new HTML("-- Creates reports as HTML pages for the current working set.");
		grid.setWidget(1, 0, generalReport);
		grid.setWidget(1, 1, html);

		integrityReport = new Button("Integrity Check Report", new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				runIntegrityChecker();
			}
		});
		integrityReport.setWidth("180px");
		html = new HTML("-- Generates a report with the result of the integrity validation engine - " +
				"only operates on global draft assessments.");
		grid.setWidget(2, 0, integrityReport);
		grid.setWidget(2, 1, html);
		
		grid.getColumnFormatter().setWidth(0, "150px");

		disableButtons();

		grid.setWidth("100%");
		add(grid, new RowData(1d, 25));
	}

	private void buildInstructions() {
		instructions = new HTML();
		add(instructions, new RowData(1d, 25));
	}

	private void createGeneralReport() {
		final com.extjs.gxt.ui.client.widget.Window window = WindowUtils.getWindow(true, false, "Report Generator");
		window.addListener(Events.Close, new Listener() {
			public void handleEvent(BaseEvent be) {
				window.hide();
			}
		});
		window.setLayout(new FitLayout());
		window.add(new WorkingSetSpeciesReportPanel());
		window.show();
		window.setSize(500, 310);
		window.center();
	}

	/**
	 * creates a csv list with no categories and criteria 
	 */
	private void createTaxaListNoCats() {
		final WorkingSetData ws = WorkingSetCache.impl.getCurrentWorkingSet();
		WorkingSetCache.impl.createTaxaList(ws.getId(), new GenericCallback<String>() {

			public void onFailure(Throwable caught) {
				String message = "An error occurred while creating your taxa report.  Try again later but not now.";
				error(message);
				enableButtons();
			}

			public void onSuccess(String arg0) {

				String message = "A report of the taxa in working set " + ws.getWorkingSetName() + " has been successfully created.  "
						+ "The file is in .csv form and can be imported into a spreadsheet application (i.e. Excel).";
				saveReport((String)arg0, message);
				enableButtons();
			}

		});
	}
	
	private void createTaxaListYesCats(AssessmentFilter filter) {
		final WorkingSetData ws = WorkingSetCache.impl.getCurrentWorkingSet();
		WorkingSetCache.impl.createTaxaListWithCats(ws.getId(), filter, new GenericCallback<String>() {

			public void onFailure(Throwable caught) {
				String message = "An error occurred while creating your taxa report.  Try again later.";
				error(message);
				enableButtons();
			}

			public void onSuccess(String arg0) {

				String message = "A report of the taxa in working set " + ws.getWorkingSetName()
						+ " has been successfully created.  "
						+ "The file is in .csv form and can be imported into a spreadsheet application (i.e. Excel).";
				saveReport((String) arg0, message);
				enableButtons();
			}

		});
	}
	
	private void createTaxaList() {
		disableButtons();		
		getTaxaListDialog().show();		
	}

	private void disableButtons() {
		taxaReport.disable();
		generalReport.disable();
	}

	private void enableButtons() {
		taxaReport.enable();
		generalReport.enable();
	}

	private void error(String message) {
		WindowUtils.errorAlert(message);
		enableButtons();
	}

	@Override
	public void refresh() {
		refreshInstructions();
		if (WorkingSetCache.impl.getCurrentWorkingSet() != null) {
			enableButtons();
		} else
			disableButtons();
	}

	private void refreshInstructions() {
		if (WorkingSetCache.impl.getCurrentWorkingSet() != null) {
			instructions.setHTML("<b>Instructions: </b> Select from the options below which type of report "
					+ " you would like to generate for the working set -- "
					+ WorkingSetCache.impl.getCurrentWorkingSet().getWorkingSetName() + ".");

		} else {
			instructions.setHTML("<b>Instructions: </b> Select a working set using the navigator, and then select "
					+ "from the options below which type of report "
					+ " you would like to generate for the chosen working set.");
		}
	}

	private void runIntegrityChecker() {
		ClientAssessmentValidator.validate(WorkingSetCache.impl.getCurrentWorkingSet());
	}
	
	private void saveReport(final String url, String message) {
		Dialog dialog = new Dialog();
		dialog.setButtons(Dialog.OKCANCEL);
		dialog.setSize("400px", "300px");
		dialog.setHeading("Successful Export");
		dialog.addStyleName("my-shell-plain");
		dialog.addText(message);
		((Button)dialog.getButtonBar().getItemByItemId(Dialog.OK)).setText("Download Report");
		dialog.addListener(Events.Close, new Listener<BaseEvent>() {
			public void handleEvent(BaseEvent be) {
				enableButtons();
			}

		});
		((Button)dialog.getButtonBar().getItemByItemId(Dialog.OK)).addSelectionListener(new SelectionListener<ButtonEvent>() {

			@Override
			public void componentSelected(ButtonEvent ce) {
				Window.open(url, "_blank", "");
			}

		});
		dialog.setHideOnButtonClick(true);
		dialog.show();
	}
	
	
	private Dialog getTaxaListDialog() {
		try{
		
		final Dialog dialog = new Dialog();
		dialog.setBodyBorder(false);
		dialog.setHeading("Download CSV File.");
		dialog.setButtons(Dialog.OKCANCEL);
		dialog.addText("Choose whether or not to also include the RedList Categories and Criteria in the .csv?  If the RedList Categories and Criteria are included, please choose which assessments generate this information.");
		dialog.setHeight(500);
		dialog.setWidth(500);
		
		RadioGroup includeCats = new RadioGroup("a");
		final Radio no = new Radio();
		final Radio yes = new Radio();
		final AssessmentFilterPanel filterPanel = new AssessmentFilterPanel(WorkingSetCache.impl.getCurrentWorkingSet().getFilter().deepCopy(), true);
		filterPanel.setVisible(false);
		no.setBoxLabel("taxa only");
		no.setValue(new Boolean(true));
		no.addListener(Events.Change, new Listener<FieldEvent>() {
			public void handleEvent(FieldEvent be) {	

				filterPanel.setVisible(!(Boolean)be.getValue());		
			}
		});
		includeCats.add(no);		
		yes.setBoxLabel("also include RedList Category and Criteria");
		includeCats.add(yes);
		
		VerticalPanel panel = new VerticalPanel();
		panel.add(includeCats);
		panel.add(filterPanel);
		
		dialog.add(panel);
		
		((Button)dialog.getButtonBar().getItemByItemId(Dialog.CANCEL)).addSelectionListener(new SelectionListener<ButtonEvent>() {
		
			public void componentSelected(ButtonEvent ce) {
				dialog.close();		
				enableButtons();
			}
		});
		
		((Button)dialog.getButtonBar().getItemByItemId(Dialog.CANCEL)).addSelectionListener(new SelectionListener<ButtonEvent>() {

			public void componentSelected(ButtonEvent ce) {
				if (yes.getValue())
				{
					String error = filterPanel.checkValidity();

					if (error == null){
						String errorMessage = filterPanel.checkValidity();
						if (errorMessage != null) {
							WindowUtils.errorAlert(errorMessage);
						} else {
							createTaxaListYesCats(filterPanel.getFilter());
							dialog.close();
							enableButtons();
						}
					}
					else
					{
						WindowUtils.errorAlert(error);
					}
				}
				else
				{
					createTaxaListNoCats();
					dialog.close();
					enableButtons();
				}
			}

		});
		
		return dialog;
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

}
