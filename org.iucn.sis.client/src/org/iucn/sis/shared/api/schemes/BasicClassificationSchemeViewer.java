package org.iucn.sis.shared.api.schemes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.iucn.sis.client.api.container.SISClientBase;
import org.iucn.sis.client.panels.notes.NotesViewer;
import org.iucn.sis.client.panels.references.PagingPanel;
import org.iucn.sis.shared.api.citations.Referenceable;
import org.iucn.sis.shared.api.data.DisplayDataProcessor;
import org.iucn.sis.shared.api.data.TreeData;
import org.iucn.sis.shared.api.data.TreeDataRow;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.structures.Structure;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.IconButtonEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.store.StoreSorter;
import com.extjs.gxt.ui.client.widget.HorizontalPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.IconButton;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.user.client.ui.HTML;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.events.ComplexListener;
import com.solertium.util.events.SimpleListener;
import com.solertium.util.extjs.client.WindowUtils;
import com.solertium.util.gwt.ui.DrawsLazily;
import com.solertium.util.portable.PortableAlphanumericComparator;

public class BasicClassificationSchemeViewer extends PagingPanel<ClassificationSchemeModelData> implements ClassificationSchemeViewer {
	
	protected final ListStore<ClassificationSchemeModelData> server;
	protected List<ClassificationSchemeModelData> saved;
	
	protected TreeData treeData;
	protected String description;
	
	protected LayoutContainer innerContainer;
	protected LayoutContainer displayPanel;
	protected Grid<ClassificationSchemeModelData> grid;
	
	protected boolean hasChanged;
	
	public BasicClassificationSchemeViewer(String description, TreeData treeData) {
		super();
		setPageCount(15);
		getProxy().setSort(false);
		
		this.treeData = treeData;
		this.description = description;
		this.hasChanged = false;
		this.server = new ListStore<ClassificationSchemeModelData>();
		server.setStoreSorter(new StoreSorter<ClassificationSchemeModelData>(
			new ClassificationSchemeModelDataComparator()
		));
	}
	
	public LayoutContainer draw(final boolean isViewOnly) {
		//inViewOnlyMode = viewOnly;

		if (displayPanel == null) {
			displayPanel = new LayoutContainer(new FillLayout());
			displayPanel.setSize(900, 800);
			//displayPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
			//displayPanel.setSpacing(3);
			displayPanel.addStyleName("thinFrameBorder");
			//displayPanel.setBorderWidth(1);
			
			innerContainer = new LayoutContainer(new FillLayout());
			innerContainer.setLayoutOnChange(true);
			//innerContainer.setWidth(900);
			
			/*pagingLoader = new GenericPagingLoader<ClassificationSchemeModelData>();
			
			store = new ListStore<ClassificationSchemeModelData>(pagingLoader.getPagingLoader());
			store.setStoreSorter(new StoreSorter<ClassificationSchemeModelData>(
				new ClassificationSchemeModelDataComparator()	
			));
			
			pagingLoader.getPagingLoader().addLoadListener(new LoadListener() {
				public void loaderLoad(LoadEvent le) {
					innerContainer.removeAll();
				}
			});*/
			
			server.addStoreListener(new StoreListener<ClassificationSchemeModelData>() {
				public void storeAdd(StoreEvent<ClassificationSchemeModelData> se) {
					refresh(new DrawsLazily.DoneDrawingWithNothingToDoCallback());
				}
				@Override
				public void storeRemove(StoreEvent<ClassificationSchemeModelData> se) {
					refresh(new DrawsLazily.DoneDrawingWithNothingToDoCallback());
				}
				@Override
				public void storeUpdate(StoreEvent<ClassificationSchemeModelData> se) {
					refresh(new DrawsLazily.DoneDrawingWithNothingToDoCallback());
				}
			});

			/**
			 * This listener is a hack that fixes some horrible behavioral issues with the grid's
			 * sizing. Essentially, if a page on the assessment data browser was left open to a 
			 * classification scheme and an assessment was set as current from a different tab, 
			 * e.g. from the recent assessment panel, *without* first triggering a switch to the 
			 * assessment data browser tab (as happens with the monkey navigator), the grid will 
			 * render itself properly at first, but if you page/refresh the grid's data it redraws 
			 * itself with a height only large enough for the headers even though it has the proper 
			 * number of entries.
			 * 
			 * TODO: Low priority as it may be impossible -figure out why the grid does not resize
			 *  its height properly. That is the only misbehaving Widget - the hp and display panel
			 *  retain proper sizing, though the grid is attached to the hp in a RowLayout, so there
			 *  may be something to that combination that contributes.
			 */
			/*pagingLoader.getPagingLoader().addLoadListener(new LoadListener() {
				protected int gridH = 0;

				@Override
				public void loaderBeforeLoad(LoadEvent le) {
					if( grid.isRendered() )
						gridH = grid.getHeight();
				}

				@Override
				public void loaderLoad(LoadEvent le) {
					if( grid.isRendered() ) {
						if( grid.getHeight() <= 30 && grid.getStore().getCount() > 0 ) {
							grid.setHeight(gridH);
							grid.getView().refresh(true);
							hp.layout();
						}
					}
				}
			});*/
		}

		innerContainer.removeAll();
		displayPanel.removeAll();
		
		grid = 
			new Grid<ClassificationSchemeModelData>(getStoreInstance(), new ColumnModel(buildColumnConfig(generateDefaultStructure())));
		
		final ToolBar gridBar = getLabelPanel(grid, isViewOnly);
		
		grid.setBorders(true);
		grid.setWidth(900);
		grid.setHeight(300);
		//grid.getView().setFiresEvents(false);
		grid.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		grid.addListener(Events.RowClick, new Listener<GridEvent<ClassificationSchemeModelData>>() {
			public void handleEvent(GridEvent<ClassificationSchemeModelData> be) {
				ClassificationSchemeModelData selected = be.getModel();
				if (selected != null) {
					gridBar.fireEvent(Events.Change);
					
					updateInnerContainer(selected, false, isViewOnly, new DrawsLazily.DoneDrawingCallbackWithParam<LayoutContainer>() {
						public void isDrawn(LayoutContainer container) {
							innerContainer.removeAll();
							innerContainer.add(container);
						}
					});
				}
				else{
					gridBar.setVisible(false);
				}
			}
		});
		
		/*final PagingToolBar pagingBar = new PagingToolBar(15);
		pagingBar.bind(pagingLoader.getPagingLoader());*/

		final LayoutContainer toolbarContainer = new LayoutContainer();
		toolbarContainer.add(createToolbar(isViewOnly));
		toolbarContainer.add(gridBar);
		
		final LayoutContainer gridContainer = new LayoutContainer(new BorderLayout());
		gridContainer.add(toolbarContainer, new BorderLayoutData(LayoutRegion.NORTH, 50, 50, 50));
		gridContainer.add(grid, new BorderLayoutData(LayoutRegion.CENTER));
		gridContainer.add(getPagingToolbar(), new BorderLayoutData(LayoutRegion.SOUTH, 25, 25, 25));
		
		final LayoutContainer container = new LayoutContainer(new BorderLayout());
		container.add(gridContainer, new BorderLayoutData(LayoutRegion.CENTER));
		container.add(innerContainer, new BorderLayoutData(LayoutRegion.SOUTH));

		/*hp.add(addBar, new RowData(1, 30));
		hp.add(gridBar, new RowData(1, 30));
		hp.add(grid, new RowData(1, -1));
		hp.add(pagingBar, new RowData(1, 40));
		hp.add(innerContainer, new RowData(1, -1));*/

		/*pagingLoader.getFullList().clear();
		if (initValues != null) {
			for (ClassificationSchemeModelData model : initValues) {
				pagingLoader.getFullList().add(model);
			}
		}

		pagingLoader.getPagingLoader().setLimit(15);
		pagingLoader.getPagingLoader().setOffset(0);
		pagingLoader.getPagingLoader().load(0, 15);

		if (grid.isRendered())
			pagingBar.first();*/

		displayPanel.add(container);
		
		//This is a sync operation, so layout should be fine
		refresh(new DrawsLazily.DoneDrawingWithNothingToDoCallback());
		
		return displayPanel;
	}
	
	protected void updateInnerContainer(final ClassificationSchemeModelData model, 
			final boolean addToPagingLoader, final boolean isViewOnly, final DrawsLazily.DoneDrawingCallbackWithParam<LayoutContainer> callback) {
		final ComboBox<CodingOption> box = createClassificationOptions(model.getSelectedRow());
		
		final ToolBar buttonPanel = new ToolBar();
		buttonPanel.add(new Button("Save Selection", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				//									if(!((Boolean)box.getSelection().get(0).get("enabled")).booleanValue() || (!init.equals(box.getSelection().get(0).get("key")) && selected.containsKey(box.getSelection().get(0).get("key")))){
				/*if( (box.getValue(box.getSelectedIndex()).equals("") || (!init.equals(box.getValue(box.getSelectedIndex())) && selected.containsKey(box.getValue(box.getSelectedIndex())) ))){
					WindowUtils.errorAlert("This is not a selectable option. Please try again.");
					return;
				}*/
				
				//TODO: prevent duplicate entries
				
				model.setSelectedRow(box.getValue().getRow());
				model.updateDisplayableData();
				
				if (addToPagingLoader) {
					/*pagingLoader.getFullList().add(0, model);
					pagingLoader.getPagingLoader().load();*/
					server.add(model);
				}
				else
					server.update(model);
								
				hasChanged = true;
				
				innerContainer.removeAll();
			}
		}));
		buttonPanel.add(new Button("Cancel", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				innerContainer.removeAll();
			}
		}));
		
		final ToolBar top = new ToolBar();
		top.add(new LabelToolItem(description+":"));
		top.add(box);
		
		final LayoutContainer widgetContainer = new LayoutContainer(new FillLayout());
		widgetContainer.setScrollMode(Scroll.AUTO);
		widgetContainer.add(model.getDetailsWidget(isViewOnly));
		
		final LayoutContainer container = new LayoutContainer(new BorderLayout());
		container.add(top, new BorderLayoutData(LayoutRegion.NORTH, 25, 25, 25));
		container.add(widgetContainer ,new BorderLayoutData(LayoutRegion.CENTER));
		container.add(buttonPanel, new BorderLayoutData(LayoutRegion.SOUTH, 25, 25, 25));
		
		callback.isDrawn(container);
	}
	
	public ToolBar getLabelPanel(final Grid<ClassificationSchemeModelData> grid, boolean isViewOnly){
		/*
		 * FIXME: I removed a lot of functionality from here, as I just 
		 * don't know how it works and how to hook it up in the database 
		 * appropriately.  Needs to be re-worked.
		 * 
		 * Also, these images will not load because we need to create 
		 * CSS classes for them instead of specifying images.  I think 
		 * there is an IconButton class in our libraries somewhere that 
		 * does take an image, but it's nowhere to be found right now. 
		 */

		final IconButton remove = new IconButton("icon-cross");
		remove.addStyleName("pointerCursor");
		remove.setSize("18px", "18px");
		remove.addSelectionListener(new SelectionListener<IconButtonEvent>() {
			public void componentSelected(IconButtonEvent ce) {
				final ClassificationSchemeModelData model = grid.getSelectionModel().getSelectedItem();
				if (model == null)
					WindowUtils.errorAlert("Please select a row to delete.");
				else {
					WindowUtils.confirmAlert("Delete Confirm", "Are you sure you want to delete this data?", new WindowUtils.SimpleMessageBoxListener() {
						public void onYes() {
							if (model != null) {
								server.remove(model);
							
								innerContainer.removeAll();
							}
						}
					});
				}
			}
		});

		final IconButton refIcon = new IconButton("icon-book");
		refIcon.addStyleName("SIS_iconPanelIcon");
		refIcon.addSelectionListener(new SelectionListener<IconButtonEvent>() {
			public void componentSelected(IconButtonEvent ce) {
				GenericCallback<Object> callback = new GenericCallback<Object>() {
					public void onFailure(Throwable caught) {
						WindowUtils.errorAlert("Error!", "Error committing changes to the "
								+ "server. Ensure you are connected to the server, then try " + "the process again.");
					}

					public void onSuccess(Object result) {
						
					}
				};
				
				final ClassificationSchemeModelData model = grid.getSelectionModel().getSelectedItem();
				if (model == null)
					WindowUtils.errorAlert("Please select a row to add references.");
				else {
					String title = "Add references to " + treeData.getCanonicalName() + 
						" " + model.getSelectedRow().getLabel(); 
				
					SISClientBase.getInstance().onShowReferenceEditor(title, (Referenceable)model, callback, callback);
				
				/*SISClientBase.getInstance().onShowReferenceEditor("Add a references to " + canonicalName + " "
						+ ((ClassificationSchemeEntry)grid.getSelectionModel().getSelectedItem()).getKey(), 
						(ClassificationSchemeEntry)grid.getSelectionModel().getSelectedItem(), 
				new GenericCallback<Object>() {
					public void onFailure(Throwable caught) {
					};

					public void onSuccess(Object result) {
						ArrayList references = (ArrayList) result;
						ReferenceCache.getInstance().addReferences(
								AssessmentCache.impl.getCurrentAssessment().getId(), references);

						ReferenceCache.getInstance().addReferencesToAssessmentAndSave(references,
								canonicalName + "." + ((ClassificationSchemeEntry)grid.getSelectionModel().getSelectedItem()).getKey(), new GenericCallback<Object>() {
							public void onFailure(Throwable caught) {
							}

							public void onSuccess(Object result) {
								refIcon.changeStyle("images/icon-book.png");
							}
						});
					};
				}, 
				new GenericCallback<Object>() {
					public void onFailure(Throwable caught) {};

					public void onSuccess(Object result) {
						ArrayList list = (ArrayList) result;
						for (int i = 0; i < list.size(); i++) {
							AssessmentCache.impl.getCurrentAssessment().removeReference(
									(Reference) list.get(i), canonicalName + "." + ((ClassificationSchemeEntry)grid.getSelectionModel().getSelectedItem()).getKey());
						}

						if (AssessmentCache.impl.getCurrentAssessment().getReferences(
								canonicalName + "." + ((ClassificationSchemeEntry)grid.getSelectionModel().getSelectedItem()).getKey()).size() == 0)
							refIcon.changeStyle("images/icon-book-grey.png");
						// rebuildIconPanel();
					};
				});*/
				}
			}
		});

		

//		final ClickHandler noteListener = new ClickHandler() {
//			public void onClick(ClickEvent event) {
//				String temp = canonicalName + ((ClassificationSchemeEntry)grid.getSelectionModel().getSelectedItem()).getKey();
//				openEditViewNotesPopup(temp, notesImage);
//			}
//		};
		
		
		final IconButton notesImage = new IconButton("icon-note");
		notesImage.addStyleName("SIS_iconPanelIcon");
		notesImage.addSelectionListener(new SelectionListener<IconButtonEvent>() {
			public void componentSelected(IconButtonEvent ce) {
				final ClassificationSchemeModelData model = grid.getSelectionModel().getSelectedItem();
				if (model == null)
					WindowUtils.errorAlert("Please select a row to add references.");
				else if (model.getField() == null)
					WindowUtils.errorAlert("Please save your changes before adding notes.");
				else {
					NotesViewer.open(model.getField(), null);
				}
				/*String temp = canonicalName + ((ClassificationSchemeEntry)grid.getSelectionModel().getSelectedItem()).getKey();
				openEditViewNotesPopup(temp, new AsyncCallback<String>() {
					public void onSuccess(String result) {
						notesImage.changeStyle("images/icon-book.png");
					}
					public void onFailure(Throwable caught) {
					}
				});*/
			}
		});
		
		final ToolBar toolBar = new ToolBar();
		if (!isViewOnly) {
			toolBar.add(remove);
			toolBar.add(new SeparatorToolItem());
		}
		toolBar.add(refIcon);
		toolBar.add(new SeparatorToolItem());
		toolBar.add(notesImage);

		return toolBar;

	}
	
	protected ToolBar createToolbar(final boolean isViewOnly) {
		final Button addClassification = new Button("Quick Add to " + description);
		addClassification.setIconStyle("icon-add");
		addClassification.addSelectionListener(new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				final ClassificationSchemeModelData model = 
					newInstance(generateDefaultStructure());
				
				updateInnerContainer(model, true, isViewOnly, new DrawsLazily.DoneDrawingCallbackWithParam<LayoutContainer>() {
					public void isDrawn(LayoutContainer container) {
						innerContainer.removeAll();
						innerContainer.add(container);
					}
				});
			}
		});
		
		final Button modClassification = new Button("Add " + description);
		modClassification.setIconStyle("icon-browse-add");
		modClassification.addSelectionListener(new SelectionListener<ButtonEvent>(){
			public void componentSelected(ButtonEvent ce) {
				final Window window = WindowUtils.getWindow(true, false, "Add " + description);
				window.setScrollMode(Scroll.AUTO);
				if (description != null && !description.equals(""))
					window.setHeading(description);
				
				window.add(TreePanelBuilder.build(new ComplexListener<Set<TreeDataRow>>() {
					public void handleEvent(Set<TreeDataRow> eventData) {
						/*
						 * FIXME: add to store...
						 */
						
					}
				}, new SimpleListener() {
					public void handleEvent() {
						window.hide();
					}
				}, treeData));
				window.setSize(500, 500);
				window.show();
				window.center();
			}
		});

		final ToolBar bar = new ToolBar();
		bar.add(addClassification);
		//bar.add(modClassification);
		
		return bar;
	}
	
	protected ClassificationSchemeModelData newInstance(Structure structure) {
		return new ClassificationSchemeModelData(structure);
	}
	
	protected ComboBox<CodingOption> createClassificationOptions(TreeDataRow selected) {
		/*
		 * Flatten the tree into a list...
		 */
		final List<TreeDataRow> list = new ArrayList<TreeDataRow>(treeData.flattenTree().values());
		Collections.sort(list, new TreeDataRowComparator());
		
		final ListStore<CodingOption> store = new ListStore<CodingOption>();
		
		CodingOption selectedOption = null;
		for (TreeDataRow row : list) {
			/*
			 * Weed out legacy data
			 */
			if (row.getRowNumber().indexOf('.') < 0) {
				try {
					if (Integer.parseInt(row.getRowNumber()) >= 100)
						continue;
				} catch (NumberFormatException e) {
					continue;
				}
			}
			
			if ("true".equals(row.getCodeable())) {
				final CodingOption option = new CodingOption(row);
				store.add(option);
				if (row.equals(selected))
					selectedOption = option;
			}
		}
		
		final ComboBox<CodingOption> box = new ComboBox<CodingOption>();
		box.setStore(store);
		box.setForceSelection(true);
		box.setTriggerAction(TriggerAction.ALL);
		box.setWidth(500);
		
		if (selectedOption != null)
			box.setValue(selectedOption);
		
		return box;
	}

	@Override
	public boolean hasChanged() {
		return hasChanged;
	}

	@Override
	public List<ClassificationSchemeModelData> save(boolean deep) {
		if (deep)
			hasChanged = false;
		saved = server.getModels();
		Debug.println("Classification scheme saved {0} models", saved.size());
		return saved;
	}

	@Override
	public void setData(List<ClassificationSchemeModelData> models) {
		this.saved = models;
		if (saved != null) {
			server.removeAll();
			server.add(models);
		}
	}
	
	@Override
	public void revert() {
		setData(saved);
	}
	
	protected Structure generateDefaultStructure() {
		return DisplayDataProcessor.processDisplayStructure(treeData.getDefaultStructure());
	}
	
	protected ArrayList<ColumnConfig> buildColumnConfig(Structure<?> str){
		ArrayList<ColumnConfig> cc = new ArrayList<ColumnConfig>();

		ColumnConfig column = new ColumnConfig();  
		column.setId("text");
		column.setHeader(description);
		column.setWidth(450);
		cc.add(column);

		for (String d : str.extractDescriptions()) {
			column = new ColumnConfig();
			column.setId(d);
			column.setHeader(d);
			column.setWidth(80);
			cc.add(column);
		}
		
		return cc;
	}
	
	@Override
	protected void getStore(GenericCallback<ListStore<ClassificationSchemeModelData>> callback) {
		callback.onSuccess(server);
	}
	
	@Override
	protected void refreshView() {
		grid.getView().refresh(false);
	}
	
	private class ClassificationSchemeModelDataComparator implements Comparator<Object> {
		
		private final TreeDataRowComparator comparator = 
			new TreeDataRowComparator();
		
		@Override
		public int compare(Object o1, Object o2) {
			if (o1 instanceof ClassificationSchemeModelData && o2 instanceof ClassificationSchemeModelData)
				return compare((ClassificationSchemeModelData)o1, (ClassificationSchemeModelData)o2);
			else
				return 0;
		}
		
		public int compare(ClassificationSchemeModelData o1, ClassificationSchemeModelData o2) {
			if (o1.getSelectedRow() == null)
				return 1;
			else if (o2.getSelectedRow() == null)
				return -1;
			
			return comparator.compare(o1.getSelectedRow(), o2.getSelectedRow());
		}
		
	}
	
	private static class TreeDataRowComparator implements Comparator<TreeDataRow> {
		
		private final PortableAlphanumericComparator comparator = 
			new PortableAlphanumericComparator();
		
		public int compare(TreeDataRow o1, TreeDataRow o2) {
			return comparator.compare(o1.getDescription(), o2.getDescription());
		}
		
	}
	
	protected static class CodingOption extends BaseModelData {
		
		private static final long serialVersionUID = 1L;
		
		private final TreeDataRow row;
		
		public CodingOption(TreeDataRow row) {
			super();
			this.row = row;
			
			set("text", row.getLabel());
			set("value", row.getDisplayId());
		}
		
		public String getValue() {
			return get("value");
		}
		
		public TreeDataRow getRow() {
			return row;
		}
		
		public boolean isCodeable() {
			return "true".equals(row.getCodeable());
		} 
		
	}

}
