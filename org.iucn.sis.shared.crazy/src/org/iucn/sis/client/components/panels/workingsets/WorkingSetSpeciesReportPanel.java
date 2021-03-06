package org.iucn.sis.client.components.panels.workingsets;

import org.iucn.sis.client.components.panels.filters.AssessmentFilterPanel;
import org.iucn.sis.client.simple.SimpleSISClient;
import org.iucn.sis.shared.data.WorkingSetCache;
import org.iucn.sis.shared.data.WorkingSetData;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.WindowManager;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.form.CheckBox;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.Radio;
import com.extjs.gxt.ui.client.widget.form.RadioGroup;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.util.extjs.client.WindowUtils;

public class WorkingSetSpeciesReportPanel extends LayoutContainer {

//	final protected CheckBox publishedAssessmentsCheck;
//	final protected CheckBox draftAssessmentsCheck;
//	final protected CheckBox globalAssessmentsCheck;
//	final protected CheckBox regionalAssessmentsCheck;
	final protected Radio oneFile;
	final protected Radio seperateFiles;
	final protected RadioGroup radioGroup;
	final protected Button button;
	final protected WorkingSetData ws;
	final protected AssessmentFilterPanel asmFilterPanel;

	final protected CheckBox useLimited;
	final protected CheckBox showEmpty;
	
	public WorkingSetSpeciesReportPanel() {
//		publishedAssessmentsCheck = new CheckBox();
//		publishedAssessmentsCheck.setBoxLabel("Published Assessments");
//
//		draftAssessmentsCheck = new CheckBox();
//		draftAssessmentsCheck.setBoxLabel("Draft Assessments");
//		draftAssessmentsCheck.addListener(Events.Change, new Listener<BaseEvent>() {
//			public void handleEvent(BaseEvent be) {
//				if (draftAssessmentsCheck.getValue()) {
//					globalAssessmentsCheck.setEnabled(true);
//					globalAssessmentsCheck.setValue(new Boolean(true));
//					regionalAssessmentsCheck.setEnabled(true);
//					regionalAssessmentsCheck.setValue(new Boolean(true));
//				} else {
//					globalAssessmentsCheck.setEnabled(false);
//					globalAssessmentsCheck.setValue(new Boolean(false));
//					regionalAssessmentsCheck.setEnabled(false);
//					regionalAssessmentsCheck.setValue(new Boolean(false));
//				}
//			};
//		});
//
//		globalAssessmentsCheck = new CheckBox();
//		globalAssessmentsCheck.setBoxLabel("Global Draft Assessments");
//		globalAssessmentsCheck.setEnabled(false);
//		regionalAssessmentsCheck = new CheckBox();
//		regionalAssessmentsCheck.setBoxLabel("Regional Draft Assessments");
//		regionalAssessmentsCheck.setEnabled(false);
		
		ws = WorkingSetCache.impl.getCurrentWorkingSet();
		asmFilterPanel = new AssessmentFilterPanel(ws.getFilter(), false, true, false, true);
		
		useLimited = new CheckBox();
		useLimited.setValue(Boolean.valueOf(true));
		useLimited.setFieldLabel("Use limited field set (more compact report)");
		
		showEmpty = new CheckBox();
		showEmpty.setFieldLabel("Show empty fields");
		
		oneFile = new Radio();
		oneFile.setBoxLabel("Single Report");
		oneFile.setValue(new Boolean(true));
		seperateFiles = new Radio();
		seperateFiles.setBoxLabel("Separate Reports For Each Assessment");
		radioGroup = new RadioGroup("files");
		radioGroup.add(oneFile);
		radioGroup.add(seperateFiles);

		button = new Button("Generate Report", new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				submit();
			};
		});

		
		load();
	}

	protected void load() {
		RowLayout layout = new RowLayout();
		setLayout(layout);
		RowData data = new RowData();
		data.setWidth(1);
		data.setMargins(new Margins(5,5,5,5));
		

		HTML instructions = new HTML("To generate a report, select the type of assessments you would like to "
				+ "be included in the report, and if you would like the report as a single file, or individual "
				+ "files for each assessment.  The generated report will be zipped together, and you can save "
				+ "the report to your computer.");
		add(instructions, data);
		add(asmFilterPanel, data);
		add(radioGroup, data);
		
		ButtonBar bar = new ButtonBar();
		bar.setAlignment(HorizontalAlignment.RIGHT);
		bar.add(button);
		add(bar, data);

		final FormPanel form = new FormPanel();
		form.setLabelSeparator("?");
		form.setLabelWidth(300);
		form.setFieldWidth(50);
		form.setHeaderVisible(false);
		form.setBorders(false);
		form.add(useLimited);
		form.add(showEmpty);
		add(form, data);
	}

	protected void submit() {
		button.disable();
		final BaseEvent be = new BaseEvent(this);
		be.setCancelled(false);
		
		String error = asmFilterPanel.checkValidity();
		if (error != null)
		{
			WindowUtils.errorAlert(error);
			return;
		}
		else
		{
			asmFilterPanel.putIntoAssessmentFilter();

			StringBuilder string = new StringBuilder("");
			if (oneFile.getValue().booleanValue())
				string.append("<file>single</file>\r\n");
			else
				string.append("<file>multiple</file>\r\n");

			
			final NativeDocument ndoc = SimpleSISClient.getHttpBasicNativeDocument();
			ndoc.postAsText("/reports/workingset/" + ws.getId() + "/" + ws.isPublic()
					+ "?empty=" + showEmpty.getValue() + "&limited=" + useLimited.getValue(), 
					"<xml>" + asmFilterPanel.getFilter().toXML() + string.toString() + "</xml>", 
					new GenericCallback<String>() {

				public void onFailure(Throwable caught) {
					WindowUtils.errorAlert("Error creating zip file for workingset.");
					button.enable();
				}

				public void onSuccess(String result) {
//					fireEvent(Events.Close, be);
					WindowManager.get().getActive().close();
					Dialog dialog = new Dialog();
					dialog.setButtons(Dialog.OKCANCEL);
					dialog.setSize("400px", "300px");
					dialog.setHeading("Report Generated");
					dialog.addStyleName("my-shell-plain");
					dialog.addText("A report has been generated from the working set " + ws.getWorkingSetName()
							+ ".  The report has been saved as a zip file, and you should save it to your local "
							+ "compute; unzip the file to view the report(s).  "
							+ "If you have problems downloading the file, make sure you have popups "
							+ "enabled for this website.");
					((Button)dialog.getButtonBar().getItemByItemId(Dialog.OK)).setText("Download File");
					((Button)dialog.getButtonBar().getItemByItemId(Dialog.OK)).addListener(Events.Select, new Listener() {

						public void handleEvent(BaseEvent be) {
							Window.open(ndoc.getText(), "_blank", "");
						}

					});
					dialog.setHideOnButtonClick(true);
					dialog.show();

				}
			});

		}

	}
}
