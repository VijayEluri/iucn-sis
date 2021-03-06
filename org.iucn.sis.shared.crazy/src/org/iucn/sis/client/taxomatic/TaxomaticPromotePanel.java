package org.iucn.sis.client.taxomatic;

import org.iucn.sis.client.components.panels.PanelManager;
import org.iucn.sis.shared.data.TaxonomyCache;
import org.iucn.sis.shared.taxonomyTree.TaxonNode;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.google.gwt.user.client.ui.HTML;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.extjs.client.WindowUtils;

public class TaxomaticPromotePanel extends LayoutContainer {

	private HTML message;
	private ButtonBar bar;
	private TaxonNode currentNode;
	private PanelManager manager;
	private boolean error;

	public TaxomaticPromotePanel(PanelManager manager) {
		this.manager = manager;
		currentNode = TaxonomyCache.impl.getCurrentNode();
		build();
	}

	private void build() {
		RowLayout layout = new RowLayout(Orientation.VERTICAL);
		// layout.setMargin(5);
		// layout.setSpacing(5);
		setLayout(layout);
		setSize(500, 300);

		buildMessage();
		buildButtons();

		add(message, new RowData(1d, 1d));
		add(bar, new RowData(1d, 25));
	}

	private void buildButtons() {

		bar = new ButtonBar();
		bar.setAlignment(HorizontalAlignment.RIGHT);
		final Button cancelButton = new Button("OK", new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				close();
			}

		});
		bar.add(cancelButton);

		if (!error) {
			cancelButton.setText("Cancel");
			final Button promoteButton = new Button("Promote taxon");
			SelectionListener listener = new SelectionListener<ComponentEvent>() {
				@Override
				public void componentSelected(ComponentEvent ce) {
					cancelButton.setEnabled(false);
					promoteButton.setEnabled(false);

					TaxomaticUtils.impl.performPromotion(currentNode, new GenericCallback<String>() {

						public void onFailure(Throwable arg0) {
							cancelButton.setEnabled(true);
							promoteButton.setEnabled(true);
							WindowUtils.errorAlert("Error", "There was an error while trying to " + " promote "
									+ currentNode.getFullName() + ". Please make sure "
									+ "that no one else is currently using SIS and try again later.");
						}

						public void onSuccess(String arg0) {
							close();
							WindowUtils
									.infoAlert("Success", currentNode.getName() + " has successfully been promoted.");
						}

					});

				}

			};
			promoteButton.addSelectionListener(listener);
			bar.add(promoteButton);
		}

	}

	private void buildMessage() {
		if (currentNode == null) {
			message = new HTML("<b>ERROR:</b> Please first select a taxa.");
			error = true;
		} else if (currentNode.getLevel() != TaxonNode.INFRARANK) {
			message = new HTML("<b>ERROR:</b> You may only promote infraranks.");
			error = true;
		} else {
			message = new HTML("<b>Instructions:</b> By promoting " + currentNode.getFullName() + ", "
					+ currentNode.getFullName() + " will become a species " + " and will have the same parent that "
					+ currentNode.getParentName() + " has.");
			error = false;
		}

	}

	private void close() {
		// manager.taxonomicSummaryPanel.update(currentNode.getId()+"");
		BaseEvent be = new BaseEvent(this);
		be.setCancelled(false);
		fireEvent(Events.Close, be);
	}
}
