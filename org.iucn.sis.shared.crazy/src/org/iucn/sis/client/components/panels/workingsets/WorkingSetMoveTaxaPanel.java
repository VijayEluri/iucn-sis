package org.iucn.sis.client.components.panels.workingsets;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.iucn.sis.client.acl.AuthorizationCache;
import org.iucn.sis.client.components.panels.PanelManager;
import org.iucn.sis.client.components.panels.workingsets.WorkingSetTaxaList.TaxaData;
import org.iucn.sis.client.simple.SimpleSISClient;
import org.iucn.sis.client.ui.RefreshLayoutContainer;
import org.iucn.sis.shared.acl.base.AuthorizableObject;
import org.iucn.sis.shared.data.WorkingSetCache;
import org.iucn.sis.shared.data.WorkingSetData;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.DataList;
import com.extjs.gxt.ui.client.widget.DataListItem;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.google.gwt.user.client.ui.HTML;
import com.solertium.lwxml.gwt.debug.SysDebugger;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.extjs.client.WindowUtils;

public class WorkingSetMoveTaxaPanel extends RefreshLayoutContainer {

	public static final boolean MOVE = false;
	public static final boolean COPY = true;

	private PanelManager manager = null;
	private HTML title = null;
	private Button move = null;
	private Button copy = null;
	private DataList list = null;

	public WorkingSetMoveTaxaPanel(PanelManager manager) {
		super();
		this.manager = manager;
		build();
	}

	private String addWithHigherIDs(final WorkingSetData ws, List<TaxaData> checkedTaxa) {
		final StringBuffer speciesIDsToAdd = new StringBuffer();

		Iterator iter = checkedTaxa.iterator();
		while (iter.hasNext()) {
			TaxaData data = (TaxaData) iter.next();
			speciesIDsToAdd.append(data.getChildIDS() + ",");
		}

		return speciesIDsToAdd.substring(0, speciesIDsToAdd.length() - 1);
	}

	private String addWithSpeciesIDS(final WorkingSetData ws, List<TaxaData> checkedTaxa) {
		final StringBuffer speciesIDsToAdd = new StringBuffer();

		Iterator iter = checkedTaxa.iterator();
		while (iter.hasNext()) {
			TaxaData data = (TaxaData) iter.next();
			speciesIDsToAdd.append(data.getID() + ",");
		}

		return speciesIDsToAdd.substring(0, speciesIDsToAdd.length() - 1);

	}

	private void build() {
		BorderLayout layout = new BorderLayout();
		BorderLayoutData north = new BorderLayoutData(LayoutRegion.NORTH, 85f);
		BorderLayoutData center = new BorderLayoutData(LayoutRegion.CENTER);
		BorderLayoutData south = new BorderLayoutData(LayoutRegion.SOUTH, 35f);
		setLayout(layout);

		buildInstructions(north);
		buildContent(center);
		buildButtons(south);
	}

	private void buildButtons(BorderLayoutData data) {
		ButtonBar buttons = new ButtonBar();
		buttons.setAlignment(HorizontalAlignment.LEFT);
		move = new Button("Move", new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				performTaxaOperation(MOVE);
			}
		});
		move.setTitle("Moves the taxa out of the current " + "working set into the selected working set.");

		copy = new Button("Copy", new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				performTaxaOperation(COPY);
			}
		});
		copy.setTitle("Copies the selected taxa and places the taxa in " + "the selected working.");

		Button cancel = new Button("Cancel", new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				cancel();
			};
		});

		buttons.add(copy);
		buttons.add(move);
		buttons.add(cancel);
		add(buttons, data);

	}

	private void buildContent(BorderLayoutData data) {
		list = new DataList();
		list.setSelectionMode(SelectionMode.SINGLE);
		list.setCheckable(true);
		list.setBorders(true);
		list.addStyleName("gwt-background");
		list.setScrollMode(Scroll.AUTO);
		add(list, data);
	}

	private void buildInstructions(BorderLayoutData data) {
		title = new HTML();
		add(title, data);
	}

	private void cancel() {
		this.setVisible(false);
		manager.workingSetOptionsPanel.clearImagePanel();
	}

	private void performTaxaOperation(final boolean mode) {
		if (WorkingSetCache.impl.getCurrentWorkingSet() != null) {
			DataListItem[] checkedList = list.getChecked().toArray(new DataListItem[0]);
			move.setEnabled(false);
			if (checkedList.length < 1) {
				WindowUtils.errorAlert("Please select a working set where you would like to "
						+ "move the selected taxa to.");
				move.setEnabled(true);
			} else if (checkedList.length > 1) {
				WindowUtils.errorAlert("Please select only 1 working set.");
				move.setEnabled(true);
			} else {
				SysDebugger.getInstance().println("i am getting checked");
				List<TaxaData> checkedTaxa = manager.workingSetOptionsPanel.getChecked();
				if (checkedTaxa != null && !checkedTaxa.isEmpty()) {

					final WorkingSetData ws = WorkingSetCache.impl.getWorkingSets().get(checkedList[0].getId());

					if (mode == MOVE
							&& !AuthorizationCache.impl.hasRight(SimpleSISClient.currentUser, AuthorizableObject.WRITE, ws)) {
						WindowUtils.errorAlert("Insufficient Permissions",
								"You cannot modify a public working set you did not create. "
										+ "Check to ensure you created both source and destination working sets.");
					} else if (mode == COPY
							&& !AuthorizationCache.impl.hasRight(SimpleSISClient.currentUser, AuthorizableObject.WRITE, ws)) {
						WindowUtils.errorAlert("Insufficient Permissions",
								"You cannot copy to a public working set you did not create.");
					} else {
						final StringBuffer speciesIDsToAdd = new StringBuffer();

						TaxaData data = checkedTaxa.get(0);
						if (data.getType().equalsIgnoreCase(TaxaData.FULLNAME)) {
							speciesIDsToAdd.append(addWithSpeciesIDS(ws, checkedTaxa));
						} else {
							speciesIDsToAdd.append(addWithHigherIDs(ws, checkedTaxa));
						}

						ws.addSpeciesIDsAsCSV(speciesIDsToAdd.toString());
						ws.setSorted(false);
						ws.sortSpeciesList();
						WorkingSetCache.impl.editWorkingSet(ws, new GenericCallback<String>() {
							public void onFailure(Throwable caught) {
								WindowUtils.errorAlert("Taxa failed to transfer to working set "
										+ ws.getWorkingSetName());
								move.setEnabled(true);
								removeChecks();
							}

							public void onSuccess(String arg0) {
								// REMOVE FROM CURRENT WORKING SET
								if (mode == MOVE) {
									final WorkingSetData ws1 = WorkingSetCache.impl.getCurrentWorkingSet();
									ws1.removeSpeciesIDsAsCSV(speciesIDsToAdd.toString());
									ws1.setSorted(false);
									ws1.sortSpeciesList();
									WorkingSetCache.impl.editWorkingSet(ws1, new GenericCallback<String>() {
										public void onFailure(Throwable caught) {
											WindowUtils.errorAlert("Taxa failed to remove from working set "
													+ ws1.getWorkingSetName());
											move.setEnabled(true);
											removeChecks();
										};

										public void onSuccess(String arg0) {
											WindowUtils.infoAlert("Success", "Taxa successfully removed from "
													+ ws1.getWorkingSetName() + " and added to working set "
													+ ws.getWorkingSetName());
											move.setEnabled(true);
											manager.workingSetOptionsPanel.listChanged();
											removeChecks();
										};
									});
								} else {
									WindowUtils.infoAlert("Taxa successfully added to working set "
											+ ws.getWorkingSetName());
									move.setEnabled(true);
									removeChecks();
								}

							}
						});
					}
				} else {
					WindowUtils.errorAlert("Please select a taxa to move.");
					move.setEnabled(true);
				}
			}
		}
	}

	@Override
	public void refresh() {
		SysDebugger.getInstance().println("I am in refresh working set move Taxa");
		this.setVisible(true);
		refreshMode();
		refreshContent();
	}

	private void refreshContent() {
		list.removeAll();
		if (WorkingSetCache.impl.getCurrentWorkingSet() != null) {
			final HashMap workingSets = WorkingSetCache.impl.getWorkingSets();
			Iterator iter = workingSets.values().iterator();
			while (iter.hasNext()) {
				WorkingSetData ws = (WorkingSetData) iter.next();
				if (!ws.equals(WorkingSetCache.impl.getCurrentWorkingSet())
						&& AuthorizationCache.impl.hasRight(SimpleSISClient.currentUser, AuthorizableObject.WRITE, ws)) {
					DataListItem item = new DataListItem(ws.getWorkingSetName() + " -- " + ws.getSpeciesIDs().size()
							+ " species");
					item.setId(ws.getId());
					list.add(item);
				}

			}
		}

	}

	private void refreshMode() {
		String titleHTML;

		if (WorkingSetCache.impl.getCurrentWorkingSet() == null) {
			titleHTML = "<b>Instructions:<b> Please select a working set";

		} else {
			titleHTML = "<b>Instructions:</b> Select taxa to be copied or moved from the <b>"
					+ WorkingSetCache.impl.getCurrentWorkingSet().getWorkingSetName()
					+ "</b> working set in the opposite table.  Select the working set where the taxa should be copied into.  "
					+ "If you chose a family (or a genus), all of the taxa in the "
					+ WorkingSetCache.impl.getCurrentWorkingSet().getWorkingSetName()
					+ " working set in the selected family (genus) will be copied to the selected working sets. ";

			if ( !AuthorizationCache.impl.hasRight(SimpleSISClient.currentUser, AuthorizableObject.GRANT, WorkingSetCache.impl.getCurrentWorkingSet()) )
				move.setEnabled(false);
			else
				move.setEnabled(true);
		}
		title.setHTML(titleHTML);
	}

	private void removeChecks() {

		if (list.getChecked().toArray(new DataListItem[0]).length > 0) {
			SysDebugger.getInstance().println("I am in removeChecks");
			DataListItem item = list.getChecked().toArray(new DataListItem[0])[0];
			item.setChecked(false);
			WorkingSetData data = WorkingSetCache.impl.getWorkingSet(item.getId());
			item.setText(data.getWorkingSetName() + " -- " + data.getSpeciesIDs().size() + " species");
			SysDebugger.getInstance().println(
					"I just set text to " + data.getWorkingSetName() + " -- " + data.getSpeciesIDs().size()
							+ " species");

		}
	}

}
