package org.iucn.sis.shared.structures;

import java.util.ArrayList;
import java.util.Iterator;

import org.iucn.sis.shared.DisplayData;
import org.iucn.sis.shared.DisplayDataProcessor;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MessageBoxEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.WindowEvent;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.solertium.util.extjs.client.WindowUtils;

/**
 * This structure allows the user to create many instances of a single structure
 * inside of itself, allowing for a one-to-many "collection".
 * 
 * @author adam.schwartz
 */
public class SISOneToMany extends Structure {

	/**
	 * ArrayList<Structure>
	 */
	private ArrayList<Structure> selected;

	private DisplayData defaultStructureData;

	private Button addNew;

	private VerticalPanel selectedPanel;

	public SISOneToMany(String struct, String descript, DisplayData defaultStructure) {
		super(struct, descript);
		buildContentPanel(Orientation.VERTICAL);

		selected = new ArrayList<Structure>();
		defaultStructureData = defaultStructure;
	}

	@Override
	public void clearData() {
		selected.clear();
	}

	@Override
	protected Widget createLabel() {
		clearDisplayPanel();
		selectedPanel.clear();
		((CellPanel) displayPanel).setSpacing(2);

		for (Iterator<Structure> iter = selected.listIterator(); iter.hasNext();) {
			final Structure curStruct = (Structure) iter.next();

			HorizontalPanel structWrapper = new HorizontalPanel();

			Button remove = new Button();
			remove.setIconStyle("icon-remove");
			remove.setSize("18px", "18px");
			remove.addSelectionListener(new SelectionListener<ButtonEvent>() {
				@Override
				public void componentSelected(ButtonEvent ce) {
					WindowUtils.confirmAlert("Confirm", "Are you sure you want to delete this data?",
							new Listener<MessageBoxEvent>() {
								public void handleEvent(MessageBoxEvent be) {
									if (be.getButtonClicked().getText().equalsIgnoreCase("yes")) {
										selected.remove(curStruct);
										createLabel();
									}
								}
							});
				}
			});

			structWrapper.add(remove);
			structWrapper.add(curStruct.createLabel());

			selectedPanel.add(structWrapper);
		}

		selectedPanel.add(addNew);

		displayPanel.add(descriptionLabel);
		displayPanel.add(selectedPanel);
		return displayPanel;
	}

	@Override
	protected Widget createViewOnlyLabel() {
		clearDisplayPanel();
		selectedPanel.clear();
		((CellPanel) displayPanel).setSpacing(2);

		if (selected.size() == 0)
			selectedPanel.add(new HTML("No information available."));
		else
			for (Iterator iter = selected.listIterator(); iter.hasNext();)
				selectedPanel.add(((Structure) iter.next()).createViewOnlyLabel());

		displayPanel.add(descriptionLabel);
		displayPanel.add(selectedPanel);
		return displayPanel;
	}

	@Override
	public void createWidget() {
		selectedPanel = new VerticalPanel();

		descriptionLabel = new HTML(description);
		descriptionLabel.addStyleName("bold");

		addNew = new Button("Add");
		addNew.setIconStyle("icon-add");
		addNew.addSelectionListener(new SelectionListener<ButtonEvent>() {
			@Override
			public void componentSelected(ButtonEvent ce) {
				Structure newOne = DisplayDataProcessor.processDisplayStructure(defaultStructureData);
				selected.add(newOne);

				createLabel();
			}
		});
	}

	/**
	 * Returns an ArrayList of descriptions (as Strings) for this structure, and
	 * if it contains multiples structures, all of those, in order.
	 */
	@Override
	public ArrayList extractDescriptions() {
		Structure structureData = DisplayDataProcessor.processDisplayStructure(defaultStructureData);
		ArrayList ret = new ArrayList();
		ret.addAll(structureData.extractDescriptions());
		return ret;
	}

	/**
	 * This structure returns null
	 */
	@Override
	public Object getData() {
		return null;
	}

	public DisplayData getDefaultStructureData() {
		return defaultStructureData;
	}
	
	/**
	 * Pass in the raw data from an AssessmentData object, and this will return
	 * it in happy, displayable String form
	 * 
	 * @return ArrayList of Strings, having converted the rawData to nicely
	 *         displayable String data. Happy days!
	 */
	@Override
	public int getDisplayableData(ArrayList<String> rawData, ArrayList<String> prettyData, int offset) {
		int num = ((String) rawData.get(offset)).matches("\\d") ? Integer.parseInt((String) rawData.get(offset)) : 0;
		prettyData.add(offset, num + " selected.");

		offset++;

		Structure def = DisplayDataProcessor.processDisplayStructure(defaultStructureData);

		for (int i = 0; i < num; i++)
			offset = def.getDisplayableData(rawData, prettyData, offset);

		return offset;
	}

	public ArrayList<Structure> getSelected() {
		return selected;
	}
	
	@Override
	public int setData(ArrayList dataList, int dataOffset) {
		super.setData(dataList, dataOffset);
		selected.clear();
		int numSelected = Integer.parseInt(dataList.get(dataOffset).toString());
		dataOffset++;

		for (int i = 0; i < numSelected; i++)
			selected.add(DisplayDataProcessor.processDisplayStructure(defaultStructureData));

		for (Iterator iter = selected.listIterator(); iter.hasNext();)
			dataOffset = ((Structure) iter.next()).setData(dataList, dataOffset);

		return dataOffset;
	}

	@Override
	protected void setEnabled(boolean isEnabled) {
		for (Iterator iter = selected.listIterator(); iter.hasNext();)
			((Structure) iter.next()).setEnabled(isEnabled);
	}

	@Override
	public String toXML() {
		String ret = "<!-- This tag is for the OneToMany, noting how many selections it has -->\r\n";
		ret += "<structure>" + selected.size() + "</structure>\r\n";

		for (Iterator iter = selected.listIterator(); iter.hasNext();)
			ret += ((Structure) iter.next()).toXML();

		return ret;
	}
}
