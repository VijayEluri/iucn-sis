package org.iucn.sis.client.panels.workingsets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.iucn.sis.client.api.caches.TaxonomyCache;
import org.iucn.sis.client.api.caches.WorkingSetCache;
import org.iucn.sis.client.panels.utils.RefreshLayoutContainer;
import org.iucn.sis.client.panels.workingsets.WorkingSetTaxaList.TaxaData;
import org.iucn.sis.shared.api.models.Taxon;
import org.iucn.sis.shared.api.models.WorkingSet;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.DataList;
import com.extjs.gxt.ui.client.widget.DataListItem;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.InfoConfig;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.user.client.ui.HTML;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.extjs.client.WindowUtils;
import com.solertium.util.portable.PortableAlphanumericComparator;

/**
 * Events:
 * 
 * Change - fired when taxa are removed.
 *
 */
@SuppressWarnings("deprecation")
public class WorkingSetDeleteTaxa extends RefreshLayoutContainer {

	HTML instructions = null;
	DataList list = null;

	public WorkingSetDeleteTaxa() {
		super();
		build();
		layout();
	}

	private void build() {
		RowLayout layout = new RowLayout();
		RowData north = new RowData(1d, 40);
		RowData center = new RowData(1d, -1);
		RowData south = new RowData(1d, -1);
		setLayout(layout);

		buildInstructions(north);
		buildContent(center);
		buildButtons(south);
	}

	private void buildButtons(RowData data) {
		ButtonBar buttons = new ButtonBar();
		buttons.setAlignment(HorizontalAlignment.LEFT);
		buttons.add(new Button("Delete Taxa", new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				deleteTaxa();
			}
		}));
		add(buttons, data);
	}

	private void buildContent(RowData data) {
		list = new DataList();
		list.setFlatStyle(true);
		list.addStyleName("gwt-background");
		list.setBorders(true);
		list.setScrollMode(Scroll.AUTO);
		add(list, data);
	}

	private void buildInstructions(RowData data) {
		instructions = new HTML();
		add(instructions, data);
	}

	private void deleteTaxa() {
		final WorkingSet ws = WorkingSetCache.impl.getCurrentWorkingSet();
		if (ws == null) {
			WindowUtils.errorAlert("Please first select a working set.");
			return;
		}
		
		if (list.getItemCount() > 0) {
			final Collection<Taxon> taxaToDelete = new HashSet<Taxon>();
			for (int i = 0; i < list.getItemCount(); i++)
				taxaToDelete.add(TaxonomyCache.impl.getTaxon(list.getItem(i).getId()));
			
			WindowUtils.showLoadingAlert("Saving Changes...");
			WorkingSetCache.impl.editTaxaInWorkingSet(ws, null, taxaToDelete, new GenericCallback<String>() {
				public void onSuccess(String result) {
					WindowUtils.hideLoadingAlert();
					Info.display("Taxa Removed", "Taxa was successfully removed from working set "
							+ ws.getWorkingSetName() + ".", "");
					//manager.workingSetOptionsPanel.listChanged();
					fireEvent(Events.Change);
					list.removeAll();
				}
				public void onFailure(Throwable caught) {
					WindowUtils.hideLoadingAlert();
					Info.display(new InfoConfig("Error", "Error removing taxa from working set "
							+ ws.getWorkingSetName() + "."));
				}
			});
		}
	}

	/*
	 * clears list
	 */
	@Override
	public void refresh() {

		if (list.getItemCount() != 0)
			list.removeAll();

		if (WorkingSetCache.impl.getCurrentWorkingSet() != null) {
			instructions.setHTML("<b>Instructions:</b> Select the taxa that you would like to delete from the "
					+ WorkingSetCache.impl.getCurrentWorkingSet().getWorkingSetName() + " working set.  The taxa that "
					+ "will be removed are added to the list below.");
		} else {
			instructions.setHTML("<b>Instructions:</b> Please first select a working set.");
		}
	}

	public void refreshTaxa(List<TaxaData> checked) {
		list.removeAll();
		
		List<DataListItem> items = new ArrayList<DataListItem>();
		
		for (TaxaData data : checked)
			addItem(data.getName(), data.getType(), data.getID(), data.getChildIDS(), items);
		
		Collections.sort(items, new ListItemComparator());
		
		for (DataListItem item : items)
			list.add(item);
		
		layout();
	}

	private void addItem(String name, String type, String id, String childIDS, List<DataListItem> list) {
		// ADDING TO LIST
		if (type.equals(TaxaData.FULLNAME)) {
			DataListItem item = new DataListItem();
			item.setIconStyle("icon-trash");
			item.setText(name);
			item.setId(id);
			list.add(item);
		} else {
			String[] ids = childIDS.split(",");
			for (int i = 0; i < ids.length; i++) {
				Taxon node = TaxonomyCache.impl.getTaxon(ids[i]);
				addItem(node.getFullName(), TaxaData.FULLNAME, node.getId()+"", "", list);
			}
		}
	}

	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);

		list.setHeight(height - 40 - 30);
	}

	private static class ListItemComparator implements Comparator<DataListItem> {
		
		private final PortableAlphanumericComparator comparator = 
			new PortableAlphanumericComparator();
		
		 @Override
		public int compare(DataListItem o1, DataListItem o2) {
			return comparator.compare(o1.getText(), o2.getText());
		}
		
	}
	
}
