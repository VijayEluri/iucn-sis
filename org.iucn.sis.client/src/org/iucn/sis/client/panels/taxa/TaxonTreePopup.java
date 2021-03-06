package org.iucn.sis.client.panels.taxa;

import org.iucn.sis.client.api.caches.TaxonomyCache;
import org.iucn.sis.client.api.container.StateManager;
import org.iucn.sis.client.api.utils.BasicWindow;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.Taxon;
import org.iucn.sis.shared.api.models.TaxonHierarchy;
import org.iucn.sis.shared.api.models.TaxonLevel;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.extjs.client.WindowUtils;

public class TaxonTreePopup extends BasicWindow {
	
	public static void open(final Taxon taxon) {
		TaxonomyCache.impl.fetchPathWithID(taxon.getId(), new GenericCallback<TaxonHierarchy>() {
			public void onFailure(Throwable caught) {
				WindowUtils.errorAlert("Could not load taxon hierarchy, please try again later.");
			}
			public void onSuccess(TaxonHierarchy result) {
				new TaxonTreePopup(taxon, result).show();
			}
		});
	}

	private TaxonTreePopup(final Taxon node, final TaxonHierarchy hierarchy) {
		super("Full Taxonomic View");
		setSize(400, 300);
		
		final String[] footprint = node.getFootprint();

		final FlexTable table = new FlexTable();
		table.setCellSpacing(8);
		table.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				final Cell cell = table.getCellForEvent(event);
				if (cell != null) {
					String id = hierarchy.getFootprintAt(cell.getRowIndex());
					if (id == null) {
						String taxonName = table.getText(cell.getRowIndex(), 1);
						Debug.println("Could not located id in hierarchy for {0}, search by name...", taxonName);
						openTaxonByName(footprint[0], taxonName);
					}
					else
						openTaxonByID(id);
				}
			}
		});
		
		for (int i = 0; i < footprint.length; i++) {
			String fullName = "";

			for (int j = 5; j < (i >= TaxonLevel.SUBPOPULATION ? i - 1 : i); j++)
				fullName += footprint[j] + " ";
			fullName += footprint[i];
			
			table.setText(i, 0, TaxonLevel.getDisplayableLevel(i) + ":");
			table.setText(i, 1, fullName);
			
			table.getCellFormatter().addStyleName(i, 0, "bold");
			table.getCellFormatter().addStyleName(i, 0, "clickable");
			table.getCellFormatter().addStyleName(i, 0, "fontSize14");
			
			table.getCellFormatter().addStyleName(i, 1, "SIS_HyperlinkLookAlike");
			table.getCellFormatter().addStyleName(i, 1, "fontSize14");
		}
		
		add(table);
	}
	
	private void openTaxonByID(String id) {
		hide();
		WindowUtils.showLoadingAlert("Loading...");
		TaxonomyCache.impl.fetchPathWithID(Integer.valueOf(id), new GenericCallback<TaxonHierarchy>() {
			public void onSuccess(TaxonHierarchy result) {
				StateManager.impl.setState(null, result.getTaxon(), null);
			}
			public void onFailure(Throwable caught) {
				WindowUtils.hideLoadingAlert();
				WindowUtils.errorAlert("Failed to find this taxon, please try again later.");
			}
		});
	}

	private void openTaxonByName(String kingdom, String name) {
		hide();
		WindowUtils.showLoadingAlert("Loading...");
		TaxonomyCache.impl.fetchTaxonWithKingdom(kingdom, name, new GenericCallback<Taxon>() {
			public void onFailure(Throwable caught) {
				WindowUtils.hideLoadingAlert();
				WindowUtils.errorAlert("Failed to find this taxon, please try again later.");
			}
			public void onSuccess(Taxon result) {
				StateManager.impl.setState(null, result, null);
			}
		});
	}
}
