package org.iucn.sis.client.panels.workingsets;

import org.iucn.sis.client.api.caches.AssessmentCache;
import org.iucn.sis.client.api.caches.TaxonomyCache;
import org.iucn.sis.client.api.caches.WorkingSetCache;
import org.iucn.sis.client.api.ui.models.workingset.WSStore;
import org.iucn.sis.client.container.SimpleSISClient;

import com.extjs.gxt.ui.client.widget.Html;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.extjs.client.WindowUtils;

public class WorkingsetImportWidget extends HorizontalPanel {

	protected FileUpload uploader;
	protected FormPanel uploadForm;
	protected Button submitUpload;
	protected Button cancelUpload;
	protected Button completed;
	protected DockPanel uploadPanel;

	public WorkingsetImportWidget() {
		add(createPanel());
	}

	public Widget createPanel() {

		// Setup uploadForm
		uploadForm = new FormPanel();
		uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadForm.setMethod(FormPanel.METHOD_POST);
		uploadForm.setAction("/workingSetImporter/" + SimpleSISClient.currentUser.getUsername());

		uploadPanel = new DockPanel();
		uploadPanel.setSpacing(5);

		uploader = new FileUpload();
		uploader.setTitle("Hey");
		uploader.setName("HEy");
		uploadPanel.add(uploader, DockPanel.CENTER);

		uploadForm.addSubmitHandler(new FormPanel.SubmitHandler() {
			public void onSubmit(SubmitEvent event) {
				submitUpload.setEnabled(false);
				submitUpload.setText("Uploading file...");
			}
		});
		uploadForm.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
			public void onSubmitComplete(final SubmitCompleteEvent event) {
				WorkingSetCache.impl.update(new GenericCallback<String>() {
					public void onFailure(Throwable caught) {
						WindowUtils.infoAlert("Failed refresh", "This working set may have imported properly, but SIS "
								+ "failed to update its contents. Please log out and log "
								+ "back in and check for the working set. If this file "
								+ "continues to fail to import, please report the issue "
								+ "and include the file for testing.");

						submitUpload.setEnabled(true);
						cancelUpload.setEnabled(true);
						AssessmentCache.impl.clear();
						TaxonomyCache.impl.clear();
					}

					public void onSuccess(String arg0) {
						WSStore.getStore().update();
						submitUpload.setText("Import Working Set");
						submitUpload.setEnabled(true);
						cancelUpload.setEnabled(true);

						if( event.getResults() == null || event.getResults().equals("") || event.getResults().indexOf("<div>") > -1 ) {
							com.extjs.gxt.ui.client.widget.Window w = WindowUtils.newWindow("Successful Import", null, false, true);
							w.add( new Html("The working set was successfully imported.<br/>" + event.getResults()));
							w.setSize(300, 500);
							w.show();
							w.center();
							AssessmentCache.impl.clear();
							TaxonomyCache.impl.clear();
						} else {
							com.extjs.gxt.ui.client.widget.Window w = WindowUtils.newWindow("Import Failed!", null, false, true);
							w.add( new Html("Import failed. Please report the following error:" + event.getResults()) );
							w.setSize(300, 500);
							w.show();
						}
					}
				});
			}
		});

		HorizontalPanel buttonPanel = new HorizontalPanel();
		{
			submitUpload = new Button("Import Working Set", new ClickHandler() {
				public void onClick(ClickEvent event) {
					submit();
				}
			});
			cancelUpload = new Button("Cancel", new ClickHandler() {
				public void onClick(ClickEvent event) {
					//TODO: cancel, changing this to a window will fix the problem.
					//manager.workingSetBrowser.setManagerTab();
				}
			});

			buttonPanel.add(submitUpload);
			buttonPanel.add(cancelUpload);
			buttonPanel.setSpacing(4);
		}

		uploadPanel.add(buttonPanel, DockPanel.SOUTH);
		uploadForm.setWidget(uploadPanel);
		uploadForm.addStyleName("RapidList-TableCell");

		return uploadForm;
	}

	public void onSubmitCompleteOverload(FormSubmitCompleteEvent event) {
		WindowUtils.infoAlert("Successful upload ... refresh page still needed");
	}

	public void submit() {
		if (validate()) {
			uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
			uploadForm.submit();
		}

	}

	protected boolean validate() {
		if (uploader.getFilename().trim().equalsIgnoreCase("")) {
			WindowUtils.errorAlert("Please select a .zip file to import.");
			return false;
		} else if (!uploader.getFilename().endsWith(".zip")) {
			WindowUtils.errorAlert("You must choose a .zip file that contains a working set.");
			return false;
		}
		return true;
	}

}
