package org.iucn.sis.shared.api.displays;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.iucn.sis.client.api.caches.DefinitionCache;
import org.iucn.sis.shared.api.data.DisplayDataProcessor;
import org.iucn.sis.shared.api.data.TreeData;
import org.iucn.sis.shared.api.data.TreeDataRow;
import org.iucn.sis.shared.api.models.Field;
import org.iucn.sis.shared.api.models.PrimitiveField;
import org.iucn.sis.shared.api.models.primitivefields.ForeignKeyPrimitiveField;
import org.iucn.sis.shared.api.schemes.BasicClassificationSchemeViewer;
import org.iucn.sis.shared.api.schemes.ClassificationSchemeModelData;
import org.iucn.sis.shared.api.schemes.ClassificationSchemeRowEditorWindow;
import org.iucn.sis.shared.api.schemes.ClassificationSchemeViewer;
import org.iucn.sis.shared.api.schemes.ClassificationSchemeRowEditorWindow.EditMode;
import org.iucn.sis.shared.api.structures.DisplayStructure;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.solertium.util.events.ComplexListener;
import com.solertium.util.events.SimpleListener;
import com.solertium.util.extjs.client.WindowUtils;
import com.solertium.util.gwt.ui.StyledHTML;

/**
 * Represents a classification scheme field.
 * 
 * @author adam.schwartz
 */
public class ClassificationScheme extends Display {

	protected final TreeData treeData;
	protected final Map<String, TreeDataRow> flatTree;
	
	protected final ClassificationSchemeViewer viewer;

	public ClassificationScheme(TreeData displayData) {
		super(displayData);
		treeData = displayData;
		flatTree = treeData.flattenTree();
		
		viewer = createViewer(description, displayData);
		
		buildDefinition();
	}
	
	protected ClassificationSchemeViewer createViewer(String description, TreeData displayData) {
		return new BasicClassificationSchemeViewer(description, displayData);
	}
	
	private void buildDefinition() {
		final StringBuilder definition = new StringBuilder();
		for (TreeDataRow curRow : treeData.getTreeRoots()) {
			String curDesc = curRow.getDescription();
			String curLevelID = curRow.getRowNumber();

			definition.append(curLevelID + " - " + curDesc + " (" + curRow.getChildren().size() + ")" + "<br />");

			buildChildrenDefinition(definition, curRow);
		}
		DefinitionCache.impl.setDefinition(description.toLowerCase(), definition.toString());
	}

	private void buildChildrenDefinition(StringBuilder definition, TreeDataRow curParent) {
		for (TreeDataRow curRow : curParent.getChildren()) {
			String curDesc = curRow.getDescription();
			String curLevelID = curRow.getRowNumber();

			try {
				if (curLevelID.indexOf(".") < 0) {
					if (Integer.parseInt(curLevelID) >= 100)
						continue;
				} else if (Integer.parseInt(curLevelID.split("\\.")[0]) >= 100)
					continue;
			} catch (NumberFormatException ignored) {
			}

			try {
				int depth = Integer.parseInt(curRow.getDepth());
				for (int i = 0; i < depth; i++)
					definition.append("&nbsp;&nbsp;");
			} catch (Exception e) {
			}

			definition.append(curLevelID + " - " + curDesc + "<br />");

			buildChildrenDefinition(definition, curRow);
		}
	}

	protected Widget generateContent(final boolean viewOnly) {
		final VerticalPanel panel = new VerticalPanel();
		
		final VerticalPanel container = new VerticalPanel();
		buildReadOnlyContainer(container);
		panel.add(container);
		
		final ButtonBar buttons = new ButtonBar();
		buttons.add(new Button(viewOnly ? "View Details" : "Make Changes", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				final Window window = new Window();
				window.setClosable(false);
				window.setModal(true);
				window.setSize(800, 600);
				window.setHeading(description);
				window.setLayout(new FillLayout());
				window.setLayoutOnChange(true);
				window.add(viewer.draw(viewOnly));
				window.setButtonAlign(HorizontalAlignment.CENTER);
				window.addButton(new Button("Done", new SelectionListener<ButtonEvent>() {
					public void componentSelected(ButtonEvent ce) {
						buildReadOnlyContainer(container, viewer.save(false));
						window.hide();
					}
				}));
				window.addButton(new Button("Cancel", new SelectionListener<ButtonEvent>() {
					public void componentSelected(ButtonEvent ce) {
						viewer.revert();
						window.hide();
					}
				}));
				window.addListener(Events.Show, new Listener<ComponentEvent>() {
					@Override
					public void handleEvent(ComponentEvent be) {
						window.layout();
					}
				});
				window.show();
			}
		}));
		if (!viewOnly)
			buttons.add(new Button("Quick Add", new SelectionListener<ButtonEvent>() {
				public void componentSelected(ButtonEvent ce) {
					final ClassificationSchemeModelData model = 
						viewer.newInstance(viewer.generateDefaultStructure(null));
					final ClassificationSchemeRowEditorWindow window = 
						viewer.createRowEditorWindow(model, true, viewOnly);
					window.setSaveListener(new ComplexListener<ClassificationSchemeModelData>() {
						public void handleEvent(ClassificationSchemeModelData eventData) {
							eventData.updateDisplayableData();
							viewer.addModel(eventData);
								
							buildReadOnlyContainer(container, viewer.save(false));
						}
					});
					window.setCancelListener(new SimpleListener() {
						public void handleEvent() {
							buildReadOnlyContainer(container, viewer.save(false));
						}
					});
					window.show();
				}
			}));
		
		panel.add(buttons);
		
		return panel;
	}
	
	@Override
	public void removeStructures() {
		/*
		 * Since we re-draw each time, no need for this implementation
		 */
	}
	
	private void buildReadOnlyContainer(VerticalPanel container) {
		final List<ClassificationSchemeModelData> thinData = new ArrayList<ClassificationSchemeModelData>();
		if (field != null && field.getFields() != null) {
			for (Field subfield : field.getFields()) {
				PrimitiveField lookup = subfield.getPrimitiveField(canonicalName+"Lookup"); 
				if (lookup == null || !flatTree.containsKey(lookup.getRawValue()))
					continue;
				
				TreeDataRow row = flatTree.get(lookup.getRawValue());
				
				ClassificationSchemeModelData model = new ClassificationSchemeModelData(null);
				model.setSelectedRow(row);
				
				thinData.add(model); 
			}
		}
		
		buildReadOnlyContainer(container, thinData);
	}
	
	private void buildReadOnlyContainer(VerticalPanel container, Collection<? extends ClassificationSchemeModelData> thinData) {
		container.clear();
		container.add(new StyledHTML("Selections for " + description + ":", "bold"));
		
		if (thinData.isEmpty()) {
			container.add(new HTML("No selections made"));
		}
		else {
			for (ClassificationSchemeModelData model : thinData) {
				container.add(new HTML(model.getSelectedRow().getFullLineage()));
			}
		}
	}

	public TreeData getTreeData() {
		return treeData;
	}

	public void save() {
		if (field == null)
			initializeField();
		
		for (ClassificationSchemeModelData model : viewer.save(true)) {
			Field subfield = model.getField();
			if (subfield == null) {
				subfield = new Field();
				subfield.setParent(field);
				subfield.setName(canonicalName+"Subfield");
				
				model.setField(subfield);
			}
			
			model.save(field, subfield);
			
			PrimitiveField lookup = subfield.getPrimitiveField(canonicalName+"Lookup");
			if (lookup == null)
				lookup = new ForeignKeyPrimitiveField(canonicalName+"Lookup", subfield);
			
			lookup.setRawValue(model.getSelectedRow().getDisplayId());
			
			subfield.addPrimitiveField(lookup);
			
			/*
			 * Since I know these two fields have the same name, I want 
			 * to let the set's pure equals method go to work here, and 
			 * eliminate based on ID.  This way, I can have two fields 
			 * with the same name.
			 */
			field.getFields().add(subfield);
		}
	}
	
	@Override
	public void setField(Field field) {
		this.field = field;
		if (field != null) {
			//TODO: This should create the appropriate models based off the subfields...
			final List<ClassificationSchemeModelData> models = 
				new ArrayList<ClassificationSchemeModelData>();
			
			for (Field subfield : field.getFields()) {
				PrimitiveField lookup = subfield.getPrimitiveField(canonicalName+"Lookup"); 
				if (lookup == null || !flatTree.containsKey(lookup.getRawValue()))
					continue;
				
				TreeDataRow row = flatTree.get(lookup.getRawValue());
				
				final DisplayStructure structure = generateDefaultDisplayStructure(row);
					
				ClassificationSchemeModelData model = createModelData(structure, subfield);
				model.setSelectedRow(row);
				
				models.add(model); 
			}
			
			viewer.setData(models);
		}
		else
			viewer.setData(new ArrayList<ClassificationSchemeModelData>());
	}
	
	protected DisplayStructure generateDefaultDisplayStructure(TreeDataRow row) {
		return DisplayDataProcessor.processDisplayStructure(treeData.getDefaultStructure());
	}
	
	protected ClassificationSchemeModelData createModelData(DisplayStructure structure, Field field) {
		return new ClassificationSchemeModelData(structure, field);
	}
	
	@Override
	public Widget showDisplay(boolean viewOnly) {
		setupIconPanel();

		VerticalPanel vert = new VerticalPanel();
		vert.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		vert.setSpacing(5);

		vert.add(iconPanel);
		vert.add(generateContent(viewOnly));
		
		VerticalPanel outer = new VerticalPanel();
		outer.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		outer.setSize("100%", "100%");
		outer.add(vert);

		return outer;
	}
	
	@Override
	public boolean hasChanged() {
		return viewer.hasChanged();
	}
}
