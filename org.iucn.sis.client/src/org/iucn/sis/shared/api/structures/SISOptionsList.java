package org.iucn.sis.shared.api.structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iucn.sis.shared.api.models.Field;
import org.iucn.sis.shared.api.models.PrimitiveField;
import org.iucn.sis.shared.api.models.User;
import org.iucn.sis.shared.api.models.primitivefields.ForeignKeyListPrimitiveField;
import org.iucn.sis.shared.api.models.primitivefields.StringPrimitiveField;
import org.iucn.sis.shared.api.utils.XMLUtils;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class SISOptionsList extends Structure<Field> {
	public static final int CURRENT_WORKING_SET_USERS = 0;

	public static final String TEXT_ACCOUNT_KEY = "textValue";
	public static final String FK_LIST_KEY = "users";
	
	private SISCompleteListTextArea theList;

	public SISOptionsList(String struct, String descript, String structID, Object data) {
		super(struct, descript, structID, data);
		buildContentPanel(Orientation.VERTICAL);
	}
	
	@Override
	public boolean hasChanged(Field field) {
		Map<String, PrimitiveField> data;
		if (field == null)
			data = new HashMap<String, PrimitiveField>();
		else
			data = field.getKeyToPrimitiveFields();
		
		//super.setData(data);

		String text = data.containsKey(TEXT_ACCOUNT_KEY) ? 
				((StringPrimitiveField)data.get(TEXT_ACCOUNT_KEY)).getValue() : "";
		List<Integer> fks = data.containsKey(FK_LIST_KEY) ? 
				((ForeignKeyListPrimitiveField)data.get(FK_LIST_KEY)).getValue() : new ArrayList<Integer>();
		
		// FOR BACKWARDS COMPATIBILITY
		if (!"".equals(text)) {
			theList.setTextAreaEnabled(true);
			theList.setUserText(text);
		}

		// FOR FORWARDS COMPATIBILITY =)
		else {
			theList.setTextAreaEnabled(false);
			List<String> ids = new ArrayList<String>();
			for( Integer fk : fks )
				ids.add(fk.toString());
			
			theList.setUsersId(ids);
		}
		
		if (theList.hasOldText()) {
			String value = theList.getText();
			if ("".equals(value))
				value = null;
			
			String oldValue;
			if (data.get(TEXT_ACCOUNT_KEY) != null)
				oldValue = data.get(TEXT_ACCOUNT_KEY).getRawValue();
			else
				oldValue = null;
			if ("".equals(oldValue))
				oldValue = null;
			
			if (value == null)
				return oldValue != null;
			else
				return !value.equals(oldValue);
		}
		else {
			List<Integer> newValue = new ArrayList<Integer>();
			for (User user : theList.getSelectedUsers())
				newValue.add(Integer.valueOf(user.getId()));
			
			List<Integer> oldValue = null;
			if (data.get(FK_LIST_KEY) != null)
				oldValue = ((ForeignKeyListPrimitiveField)data.get(FK_LIST_KEY)).getValue();
			
			if (oldValue == null)
				oldValue = new ArrayList<Integer>();
			
			if (newValue.isEmpty())
				return !oldValue.isEmpty();
			else
				return !(newValue.size() == oldValue.size() && newValue.containsAll(oldValue));
		}
	}
	
	@Override
	public void save(Field parent, Field field) {
		if (field == null) {
			field = new Field();
			field.setName(getId());
			field.setParent(parent);
		}
		
		if (theList.hasOldText())
			field.addPrimitiveField(new StringPrimitiveField(
				TEXT_ACCOUNT_KEY, field, theList.getText()
			));
		else if (!theList.getSelectedUsers().isEmpty()) {
			ForeignKeyListPrimitiveField prim = new ForeignKeyListPrimitiveField(FK_LIST_KEY, field);
			
			List<Integer> users = new ArrayList<Integer>();
			for (User user : theList.getSelectedUsers())
				users.add(Integer.valueOf(user.getId()));
			
			prim.setValue(users);
			
			field.addPrimitiveField(prim);
		}
	}
	
	@Override
	public void clearData() {
		theList = new SISCompleteListTextArea();
	}

	@Override
	protected Widget createLabel() {
		displayPanel.clear();
		HTML display = new HTML(description);
		display.setWidth("90%");
		display.setWordWrap(true);
		displayPanel.add(display);
		// displayPanel.add( hideList );
		displayPanel.add(theList);
		return displayPanel;
	}

	@Override
	protected Widget createViewOnlyLabel() {
		displayPanel.clear();
		displayPanel.add(descriptionLabel);
		// displayPanel.add( new HTML( theList.getItemsInListAsCSV() ) );
		displayPanel.add(new HTML(theList.getText()));
		return displayPanel;
	}

	@Override
	public void createWidget() {
		descriptionLabel = new HTML(description);
		theList = new SISCompleteListTextArea();
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
	public List<ClassificationInfo> getClassificationInfo() {
		ArrayList<ClassificationInfo> list = new ArrayList<ClassificationInfo>();
		list.add(new ClassificationInfo(description, getData()));
		return list;
	}

	@Override
	public String getData() {
		return XMLUtils.clean(theList.getText());
	}

	/**
	 * Pass in the raw data from an Assessment object, and this will return
	 * it in happy, displayable String form
	 * 
	 * @return ArrayList of Strings, having converted the rawData to nicely
	 *         displayable String data. Happy days!
	 */
	@Override
	public int getDisplayableData(ArrayList<String> rawData, ArrayList<String> prettyData,
			int offset) {
		prettyData.add(offset, rawData.get(0));
		return ++offset;
	}
	
	@Override
	public void setData(Field field) {
		Map<String, PrimitiveField> data;
		if (field == null)
			data = new HashMap<String, PrimitiveField>();
		else
			data = field.getKeyToPrimitiveFields();
		
		//super.setData(data);

		String text = data.containsKey(TEXT_ACCOUNT_KEY) ? 
				((StringPrimitiveField)data.get(TEXT_ACCOUNT_KEY)).getValue() : "";
		List<Integer> fks = data.containsKey(FK_LIST_KEY) ? 
				((ForeignKeyListPrimitiveField)data.get(FK_LIST_KEY)).getValue() : new ArrayList<Integer>();
		
		// FOR BACKWARDS COMPATIBILITY
		if (!"".equals(text)) {
			theList.setTextAreaEnabled(true);
			theList.setUserText(text);
		}

		// FOR FORWARDS COMPATIBILITY =)
		else {
			theList.setTextAreaEnabled(false);
			List<String> ids = new ArrayList<String>();
			for( Integer fk : fks )
				ids.add(fk.toString());
			
			theList.setUsersId(ids);
		}
	}

	public void setEnabled(boolean isEnabled) {
		
	}

}