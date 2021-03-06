package org.iucn.sis.shared.api.structures;

import java.util.ArrayList;

import org.iucn.sis.shared.api.models.PrimitiveField;
import org.iucn.sis.shared.api.models.primitivefields.StringPrimitiveField;
import org.iucn.sis.shared.api.views.components.ContentRule;
import org.iucn.sis.shared.api.views.components.Rule;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class SISText extends SISPrimitiveStructure<String> implements DominantStructure<PrimitiveField<String>> {

	private TextBox textbox;

	public SISText(String struct, String descript, String structID) {
		super(struct, descript, structID);
		// displayPanel = new HorizontalPanel();
		buildContentPanel(Orientation.HORIZONTAL);
	}

	@Override
	protected PrimitiveField<String> getNewPrimitiveField() {
		return new StringPrimitiveField(getId(), null);
	}
	
	@Override
	public void addListenerToActiveStructure(ChangeHandler changeListener, ClickHandler clickListener,
			KeyUpHandler keyboardListener) {
		textbox.addKeyUpHandler(keyboardListener);
	}

	@Override
	public void clearData() {
		textbox.setText("");
	}

	@Override
	public Widget createLabel() {
		clearDisplayPanel();
		displayPanel.add(descriptionLabel);
		displayPanel.add(textbox);
		return displayPanel;
	}

	@Override
	public Widget createViewOnlyLabel() {
		clearDisplayPanel();
		displayPanel.add(descriptionLabel);
		displayPanel.add(new HTML(textbox.getText()));
		return displayPanel;
	}

	@Override
	public void createWidget() {
		descriptionLabel = new HTML(description);
		textbox = new TextBox();
	}
	
	@Override
	public String getData() {
		return textbox.getText();
	}

	/**
	 * Pass in the raw data from an Assessment object, and this will return
	 * it in happy, displayable String form
	 * 
	 * @return ArrayList of Strings, having converted the rawData to nicely
	 *         displayable String data. Happy days!
	 */
	@Override
	public int getDisplayableData(ArrayList<String> rawData, ArrayList<String> prettyData, int offset) {
		prettyData.add(offset, rawData.get(offset));
		return ++offset;
	}

	public TextBox getTextbox() {
		return textbox;
	}

	@Override
	public boolean isActive(Rule activityRule) {
		try {
			return ((ContentRule) activityRule).matchesContent(textbox.getText());
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void setData(PrimitiveField<String> field) {
		String datum = field != null ? field.getValue() : "";
		textbox.setText(datum);
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		this.textbox.setEnabled(isEnabled);
	}

	// Put back in implements UsesClipboard and enable these, if necessary
	// public void copyToClipboard() {
	// Clipboard.getInstance().add(textbox.getText(), description);
	// }
	//	
	// public void pasteFromClipboard(ArrayList items) {
	// String text = textbox.getText();
	// for (int i = 0; i < items.size(); i++)
	// text += items.get(i) + " ";
	// textbox.setText(text);
	// }
}
