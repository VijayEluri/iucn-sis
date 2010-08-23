package org.iucn.sis.client.panels.references;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.iucn.sis.client.api.caches.AssessmentCache;
import org.iucn.sis.client.api.caches.AuthorizationCache;
import org.iucn.sis.client.container.SimpleSISClient;
import org.iucn.sis.shared.api.acl.base.AuthorizableObject;
import org.iucn.sis.shared.api.citations.Referenceable;
import org.iucn.sis.shared.api.models.Reference;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.store.StoreSorter;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ToggleButton;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.events.ComplexListener;
import com.solertium.util.extjs.client.WindowUtils;
import com.solertium.util.gwt.ui.DrawsLazily;
import com.solertium.util.portable.PortableAlphanumericComparator;

public class BibliographyViewTab extends PagingPanel<ReferenceModel> {
	
	private final ReferenceViewAPI parent;
	
	private Grid<ReferenceModel> grid;
	
	private boolean fieldsShowing;
	private Iterator<Reference> local;
	
	private boolean nonUniqueIDs;
	private HashMap<String, Reference> currentUniqueReferences;
	private Set<Reference> currentReferences;
	
	public BibliographyViewTab(ReferenceViewAPI parent) {
		super();
		this.parent = parent;
		setLayout(new FillLayout());
		draw();
	}
	
	public void draw() {
		final GridSelectionModel<ReferenceModel> sm = 
			new GridSelectionModel<ReferenceModel>();
		sm.setSelectionMode(SelectionMode.MULTI);
		
		grid = new Grid<ReferenceModel>(getStoreInstance(), getColumnModel());
		grid.setAutoExpandColumn("citation");
		grid.setSelectionModel(sm);
		
		final LayoutContainer wrapper = new LayoutContainer();
		wrapper.add(getPagingToolbar());
		
		final LayoutContainer container = new LayoutContainer(new BorderLayout());
		container.add(createToolbar(), new BorderLayoutData(LayoutRegion.NORTH, 25, 25, 25));
		container.add(grid, new BorderLayoutData(LayoutRegion.CENTER));
		container.add(wrapper, new BorderLayoutData(LayoutRegion.SOUTH, 25, 25, 25));
		
		add(container);
	}
	
	@Override
	protected void refreshView() {
		grid.getView().refresh(false);
	}

	public void setReferences(Referenceable referenceable) {
		final Referenceable curReferenceable = parent.getReferenceable();
		
		if (curReferenceable != null) {
			currentReferences = curReferenceable.getReferencesAsList();
			rebuildUniqueRefs(currentReferences);
			
			
			/*biblioContainer.removeAll();
			biblioContainer.getHeader().setEnabled(true);*/
			
			if (currentUniqueReferences.size() > 0) {
				showBibliography();
			} else {
				WindowUtils.infoAlert("No References.");
				//biblioContainer.layout();
			}
			
			//add.setEnabled(true);
		} else {
			currentReferences = null;
			//biblioContainer.getHeader().setEnabled(false);
			
			//FIXME: TODO: implement
			//setSelection(searchContainer);
			
			//add.setEnabled(false);
		}
		
		nonUniqueIDs = false;
	}
	
	private void rebuildUniqueRefs(Set<Reference> refs) {
		currentUniqueReferences.clear();
		for (Reference curRef : refs)
			addToUniqueReferences(curRef);
	}
	
	/**
	 * Puts the referenceUI object into the current unique references map.
	 * Checks first to see if its already in the map, and sets nonUniqueIDs to
	 * true if applicable;
	 * 
	 * @param curRef
	 */
	private void addToUniqueReferences(Reference curRef) {
		if (currentUniqueReferences.containsKey(curRef.getHash()))
			nonUniqueIDs = true;
		else
			currentUniqueReferences.put(curRef.getHash(), curRef);
	}
	
	private void removeFromCurrentList(Reference ref) {
		Reference remove = null;
		for (Reference cur : currentReferences) {
			if (ref.getReferenceID() == cur.getReferenceID() && ref.getField().containsAll(cur.getField()))
				remove = cur;
		}

		currentReferences.remove(remove);
	}
	
	private ToolBar createToolbar() {
		final ToolBar biblioBar = new ToolBar();
		biblioBar.setHeight(25);

		ToggleButton removeItem = new ToggleButton();
		removeItem.setText("Remove Reference");
		removeItem.setIconStyle("icon-book-delete");
		removeItem.addListener(Events.Select, new SelectionListener<ComponentEvent>() {
			@Override
			public void componentSelected(ComponentEvent ce) {
				if (!fieldsShowing && nonUniqueIDs) {
					WindowUtils.errorAlert("Cannot remove", "Your bibliography has "
							+ "the same reference attached to multiple fields. To ensure "
							+ "the reference is removed from the correct place, please "
							+ "filter your references by field, then try removing again.");
				} else if( !AuthorizationCache.impl.hasRight(SimpleSISClient.currentUser, AuthorizableObject.WRITE, 
						AssessmentCache.impl.getCurrentAssessment()) ) {
					WindowUtils.errorAlert("Cannot remove", "You do not have permission to edit this " +
							"assessment, so you may not remove references.");
				} else {
					ArrayList<Reference> list = new ArrayList<Reference>();
					List<ReferenceModel> items = grid.getSelectionModel().getSelectedItems();
					for (ReferenceModel curItem : items) {
						Reference curRef = curItem.ref;
						list.add(curRef);
						// currentReferences.remove(curRef);
						removeFromCurrentList(curRef);
					}
					if (!list.isEmpty()) {
						parent.onRemoveSelected(list);
						rebuildUniqueRefs(currentReferences);
					}
				}
			}
		});
		Button editExisting = new Button();
		editExisting.setIconStyle("icon-book-edit");
		editExisting.setText("Edit/View Selected Reference");
		editExisting.addListener(Events.Select, new SelectionListener<ComponentEvent>() {
			@Override
			public void componentSelected(ComponentEvent ce) {
				final List<ReferenceModel> selection = grid.getSelectionModel().getSelectedItems();
				if (selection.size() > 1)
					WindowUtils.errorAlert("Error", "Please select only one record to view.");
				else if (selection.isEmpty())
					WindowUtils.errorAlert("Error", "Please select a record to view.");
				else {
					final ComplexListener<ReferenceModel> saveListener = new ComplexListener<ReferenceModel>() {
						public void handleEvent(ReferenceModel reference) {
							if (reference != null && getProxy().getStore() != null) {
								reference.rebuild();
								getProxy().getStore().update(reference);
								//fromStore.update(reference);
							}
							
							final Referenceable curReferenceable = parent.getReferenceable();

							if (curReferenceable != null) {
								setReferences(curReferenceable);
								showBibliography();
							}
							
							curReferenceable.onReferenceChanged(new GenericCallback<Object>() {
								public void onFailure(Throwable caught) {
									WindowUtils.errorAlert("Error!", "Error committing changes to the "
											+ "server. Ensure you are connected to the server, then try " + "the process again.");
								}

								public void onSuccess(Object result) {
									WindowUtils.infoAlert("Success!", "Successfully committed reference " + "changes.");
								}
							});
						}
					};
					
					ReferenceViewTabPanel.openEditor(selection.get(0), saveListener, true, true);
				}
			}
		});
		final ToggleButton showFields = new ToggleButton();
		showFields.setIconStyle("icon-accept");
		showFields.setText("Show Field Info");
		showFields.addListener(Events.Select, new SelectionListener<ComponentEvent>() {
			public void componentSelected(ComponentEvent ce) {
				final ColumnConfig column = grid.getColumnModel().getColumnById("field");
				
				final boolean showing = column.isHidden();
				column.setHidden(!showing);
				
				if (showing) {
					fieldsShowing = true;

					showFields.setText("Hide Field Info");
					showFields.setIconStyle("icon-stop");

					local = currentReferences.iterator();
					refresh(new DrawsLazily.DoneDrawingWithNothingToDoCallback());
					/*populateBibTable(currentReferences.iterator());
					biblioContainer.layout();
					bibTable.sort(0, SortDir.ASC);*/
				} else {
					fieldsShowing = false;

					showFields.setText("Show Field Info");
					showFields.setIconStyle("icon-accept");

					local = currentUniqueReferences.values().iterator();
					refresh(new DrawsLazily.DoneDrawingWithNothingToDoCallback());
					
					/*populateBibTable(currentUniqueReferences.values().iterator());
					biblioContainer.layout();
					bibTable.sort(0, SortDir.ASC);*/
				}
			}
		});

		biblioBar.add(removeItem);
		biblioBar.add(editExisting);
		biblioBar.add(showFields);
		
		return biblioBar;
	}
	
	private ColumnModel getColumnModel() {
		final ColumnConfig citation = new ColumnConfig("citation", "Citation", 633);

		final ColumnConfig field = new ColumnConfig("field", "Field", 67);
		field.setHidden(true);
		
		final List<ColumnConfig> list = new ArrayList<ColumnConfig>();
		list.add(citation);
		list.add(field);
		
		return new ColumnModel(list);
	}
	
	@Override
	protected void getStore(GenericCallback<ListStore<ReferenceModel>> callback) {
		if (local != null) {
			final ListStore<ReferenceModel> store = new ListStore<ReferenceModel>();
			store.setStoreSorter(new StoreSorter<ReferenceModel>(new PortableAlphanumericComparator()));
		
			while (local.hasNext()) {
				final Reference current = local.next();
				current.generateCitationIfNotAlreadyGenerate();

				store.add(new ReferenceModel(current));
			}
			
			callback.onSuccess(store);
		}
	} 
	
	/**
	 * Shows recently used references. We just want to show them all, and we
	 * don't have download concerns since this is store on the client. Hence we
	 * are not reusing the show() function directly.
	 * 
	 */
	private void showBibliography() {
		fieldsShowing = false;
		grid.getColumnModel().getColumnById("field").setHidden(true);

		if (currentUniqueReferences != null)
			local = currentUniqueReferences.values().iterator();
		
		/*if (biblioBar.getParent() == null) {
			HorizontalPanel hp = new HorizontalPanel();
			HTML html = new HTML("* Citation possibly incomplete");
			hp.setSpacing(10);
			hp.add(html);
			html.addStyleName("red-menu");

			RowData fill_horizontal = new RowData();
			fill_horizontal.setWidth(1d);
			fill_horizontal.setHeight(25);
			biblioContainer.add(biblioBar, fill_horizontal);
			biblioContainer.add(bibTable, new RowData(1d, 1d));
			biblioContainer.add(hp, fill_horizontal);
		}*/

		refresh(new DrawsLazily.DoneDrawingWithNothingToDoCallback());
		/*try {
			biblioContainer.layout();
			bibTable.sort(0, SortDir.ASC);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	
}
