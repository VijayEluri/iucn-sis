package org.iucn.sis.shared.api.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.iucn.sis.shared.api.models.Field;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class SISLabel extends Structure<Field> {

	public static final String LABEL = "label";
	
	private HTML label;

	public SISLabel(String struct, String descript, String structID, Object data) {
		super(struct, descript, structID, data);
		// displayPanel = new ContentPanel();
		buildContentPanel(Orientation.HORIZONTAL);
	}
	
	@Override
	public void save(Field parent, Field field) {
		//Nothing to do here...
	}
	
	@Override
	public boolean hasChanged(Field field) {
		//Labels don't change...
		return false;
	}

	@Override
	public void clearData() {
	}

	@Override
	public Widget createLabel() {
		clearDisplayPanel();
		displayPanel.add(this.label);
		displayPanel.setWidth("100%");
		return displayPanel;
	}

	@Override
	public Widget createViewOnlyLabel() {
		return createLabel();
	}

	@SuppressWarnings("unchecked")
	public void createWidget() {
		Map<String, String> map = (Map)data;
		
		String style = map.get("style");
		String text = map.get("value");
		if (isBlank(text))
			text = this.description;
		
		this.label = new HTML(text);
		this.label.setWidth("100%");
		if (!isBlank(style))
			this.label.addStyleName(style);
	}
	
	private boolean isBlank(String value) {
		return value == null || "".equals(value);
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
		return description;
	}
	
	@Override
	public List<ClassificationInfo> getClassificationInfo() {
		return new ArrayList<ClassificationInfo>();
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
		return offset;
	}
	
	@Override
	public void setData(Field field) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		// Nothing to do here
	}

}
