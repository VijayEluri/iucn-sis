package org.iucn.sis.shared.api.structures;

import java.util.ArrayList;
import java.util.Map;

import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.PrimitiveField;
import org.iucn.sis.shared.api.models.primitivefields.FloatPrimitiveField;
import org.iucn.sis.shared.api.views.components.ContentRule;
import org.iucn.sis.shared.api.views.components.Rule;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class SISNumber extends SISPrimitiveStructure<Float> implements DominantStructure<PrimitiveField<Float>> {

	private TextBox textbox;
	private NumberFormat format;

	public SISNumber(String struct, String descript, String structID, Object data) {
		super(struct, descript, structID, data);
		// displayPanel = new ContentPanel();
		buildContentPanel(Orientation.HORIZONTAL);
	}
	
	@Override
	protected PrimitiveField<Float> getNewPrimitiveField() {
		return new FloatPrimitiveField(getId(), null);
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
		final String restrictions = getRestrictions();
		
		if ("0-1".equals(restrictions))
			format = NumberFormat.getFormat("0.00");
		else if ("year".equals(restrictions))
			format = NumberFormat.getFormat("0000");
		else
			format = NumberFormat.getFormat("#############.####");
		
		this.descriptionLabel = new HTML(this.description);
		this.textbox = new TextBox();
		textbox.addKeyPressHandler(new KeyPressHandler() {
			public void onKeyPress(KeyPressEvent event) {
				if (event.getCharCode() == KeyCodes.KEY_BACKSPACE || 
						event.getCharCode() == KeyCodes.KEY_LEFT || 
						event.getCharCode() == KeyCodes.KEY_RIGHT || 
						event.getCharCode() == KeyCodes.KEY_DELETE || 
						event.getCharCode() == KeyCodes.KEY_END || 
						event.getCharCode() == KeyCodes.KEY_HOME)
					return;
				
				if ("year".equals(restrictions)) {
					if (textbox.getText().length() == 4 || !Character.isDigit(event.getCharCode()))
						textbox.cancelKey();
				}
				else {
					if (!Character.isDigit(event.getCharCode()))
						textbox.cancelKey();
				}
			}
		});
		if ("0-1".equals(restrictions)) {
			textbox.addBlurHandler(new BlurHandler() {
				public void onBlur(BlurEvent event) {
					if (textbox.getText() == null || textbox.getText().equals(""))
						return;
					
					Double value = null;
					try {
						value = Double.parseDouble(textbox.getText());
					} catch (NumberFormatException e) {
						value = null;
					}
					
					if (value == null)
						textbox.setText("");
					
					double raw = value.doubleValue();
					if (raw < 0 || raw > 1)
						textbox.setText("");
				}
			});
		}
	}
	
	@SuppressWarnings("unchecked")
	private String getRestrictions() {
		return ((Map<String, String>)data).get("restriction");
	}

	/**
	 * Returns an ArrayList of descriptions (as Strings) for this structure, and
	 * if it contains multiples structures, all of those, in order.
	 */
	@Override
	public ArrayList<String> extractDescriptions() {
		ArrayList<String> ret = new ArrayList<String>();
		ret.add(description);
		return ret;
	}

	@Override
	public String getData() {
		String value = textbox.getText();
		try {
			Float.parseFloat(value);
		} catch (Exception e) {
			value = null;
		}
		return value == null || "".equals(value) ? null : value;
	}
	
	/**
	 * Compares the data this structure was set with, with what it gets from its widget(s).
	 * Returns true if they differ.
	 * @return true or false
	 */
	public boolean hasChanged(PrimitiveField<Float> field) {
		String oldValue = field == null ? null : field.getRawValue();
		if ("".equals(oldValue))
			oldValue = null;
		
		String newValue = getData();
		if ("".equals(newValue))
			newValue = null;
		
		Debug.println("SISNumber comparing {0} to {1}", oldValue, newValue);
		
		if (newValue == null)
			return oldValue != null;
		else {
			if (oldValue == null)
				return true;
			else {
				try {
					Float oldF = Float.valueOf(oldValue);
					Float newF = Float.valueOf(newValue);
					
					return !newF.equals(oldF);
				} catch (NumberFormatException e) {
					//This is bad; and we probably don't want to accept any changes...
					return false;
				}
			}
		}
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
		return ((ContentRule) activityRule).matchesContent(textbox.getText());
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void setData(PrimitiveField field) {
		/*
		 * FIXME: should be typed but fails when data has a number 
		 * field that's not a float.
		 */
		String datum = field != null ? field.getRawValue() : null;
		try {
			textbox.setText(datum == null ? "" : format.format(Float.parseFloat(datum)));
		} catch (Exception e) {
			textbox.setText(datum);
		}
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		textbox.setEnabled(isEnabled);
	}

}
