package org.iucn.sis.shared.api.structures;

import java.util.ArrayList;
import java.util.List;

import org.iucn.sis.shared.api.models.Field;

import com.google.gwt.user.client.ui.Widget;

public interface DisplayStructure {
	
	public void clearData();
	
	public ArrayList<String> extractDescriptions();
	
	public Widget generate();
	
	public Widget generateViewOnly();
	
	public String getData();
	
	public int getDisplayableData(ArrayList<String> rawData, ArrayList<String> prettyData, int offset);
	
	public List<ClassificationInfo> getClassificationInfo();
	
	public boolean hasChanged();
	
	public void save(Field field);
	
	public void setData(Field field);
	
	public void setEnabled(boolean isEnabled);
	
	public String toXML();

}
