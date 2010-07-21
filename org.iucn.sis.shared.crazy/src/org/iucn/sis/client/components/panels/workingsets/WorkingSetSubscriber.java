package org.iucn.sis.client.components.panels.workingsets;

import java.util.ArrayList;

import org.iucn.sis.client.components.ClientUIContainer;
import org.iucn.sis.client.components.panels.PanelManager;
import org.iucn.sis.client.ui.RefreshLayoutContainer;
import org.iucn.sis.shared.data.WorkingSetCache;
import org.iucn.sis.shared.data.WorkingSetData;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.Style.SelectionMode;
import com.extjs.gxt.ui.client.binder.TableBinder;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.RowData;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.extjs.gxt.ui.client.widget.table.CellRenderer;
import com.extjs.gxt.ui.client.widget.table.Table;
import com.extjs.gxt.ui.client.widget.table.TableColumn;
import com.extjs.gxt.ui.client.widget.table.TableColumnModel;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.extjs.client.GenericPagingLoader;
import com.solertium.util.extjs.client.PagingLoaderFilter;
import com.solertium.util.extjs.client.WindowUtils;

public class WorkingSetSubscriber extends RefreshLayoutContainer {

	private PanelManager manager = null;
	private Button actionButton = null;
//	private StoreFilter<WSModel> filterByName = null;
//	private StoreFilter<WSModel> filterByDate = null;
//	private StoreFilter<WSModel> filterByCreator = null;
	
	private Table table = null;
	private ListStore<WSModel> store;
	private TableBinder<WSModel> binder = null;
	
	private PagingToolBar pagingBar = null;
	private GenericPagingLoader<WSModel> pagingLoader = null;
	

	public WorkingSetSubscriber(PanelManager manager) {
		super();
		this.manager = manager;
		build();
	}

	private void build() {

		addStyleName("gwt-background");

		BorderLayout layout = new BorderLayout();
		// layout.setSpacing(8);

		setLayout(new BorderLayout());
		BorderLayoutData north = new BorderLayoutData(LayoutRegion.NORTH, 70f);
		BorderLayoutData center = new BorderLayoutData(LayoutRegion.CENTER);

		buildInstructions(north);
		buildList(center);

	}

	private void buildInstructions(BorderLayoutData data) {
		HTML bar = new HTML("<b>Instructions:</b> Select any public working set that you would like "
				+ "to add to your your working sets, and click on the subscribe button below. "
				+ "This will allow you to view the public working sets, however "
				+ "you can only edit working sets which you create.  You can filter the possible "
				+ "working sets by name, " + "creator or date by clicking on the applicable button.", true);
		add(bar, data);
	}

	private void buildList(BorderLayoutData data) {

		TableColumn[] columns = new TableColumn[3];

		columns[0] = new TableColumn("name", "Working Set Name", .6f);
		columns[0].setMinWidth(50);
		columns[0].setMaxWidth(500);

		columns[1] = new TableColumn("creator", "Creator", .2f);
		columns[1].setMinWidth(50);
		columns[1].setMaxWidth(200);

		columns[2] = new TableColumn("date", "Date Created", .2f);
		columns[2].setMinWidth(50);
		columns[2].setMaxWidth(100);

		TableColumnModel cm = new TableColumnModel(columns);
		table = new Table(cm);
		table.setSelectionMode(SelectionMode.SINGLE);
		table.addStyleName("gwt-background");
		
		pagingBar = new PagingToolBar(35);
		pagingLoader = new GenericPagingLoader<WSModel>();
		pagingBar.bind(pagingLoader.getPagingLoader());
		
		store = new ListStore<WSModel>(pagingLoader.getPagingLoader());
		binder = new TableBinder<WSModel>(table, store);
		binder.init();

		TableColumn col = cm.getColumn(0);
		col.setRenderer(new CellRenderer() {
			public String render(Component item, String property, Object value) {
				return (String) value;
			}
		});

		col = cm.getColumn(1);
		col.setRenderer(new CellRenderer() {
			public String render(Component item, String property, Object value) {
				return (String) value;
			}
		});

		col = cm.getColumn(2);
		col.setRenderer(new CellRenderer() {
			public String render(Component item, String property, Object value) {
				return (String) value;
			}
		});

//		store.setStoreSorter(new StoreSorter<WSModel>() {
//			@Override
//			public int compare(Store<WSModel> store, WSModel ws1, WSModel ws2, String property) {
//
//				int returnvalue = 0;
//				PortableAlphanumericComparator compare = new PortableAlphanumericComparator();
//				
//				if( ws1.get(property) != null && ws2.get(property) != null )
//					returnvalue = compare.compare(ws1.get(property), ws2.get(property));
//
//				return returnvalue;
//
//			}
//		});

		final TextBox textBox = new TextBox();
		
		pagingLoader.setFilter(new PagingLoaderFilter<WSModel>() {
			public boolean filter(WSModel item, String property) {
				if (textBox.getText().length() == 0)
					return false;

				String compareWith = property.startsWith("name") ? item.getName().toLowerCase() : 
					property.startsWith("date") ? item.getDate().toLowerCase() :
					item.getCreator().toLowerCase();
				String text = textBox.getText().toLowerCase();
				if (compareWith.startsWith(text))
					return false;
				else
					return true;
			}
		});
		
		final Button filterByDateButton = new Button("date");
		final Button filterByCreatorButton = new Button("creator");
		final Button filterByNameButton = new Button("name");
		final SelectionListener<ButtonEvent> event = new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				pagingLoader.applyFilter(ce.getButton().getText());
				pagingBar.setActivePage(1);
				pagingLoader.getPagingLoader().load();
			}
		};
		filterByNameButton.addSelectionListener(event);
		filterByDateButton.addSelectionListener(event);
		filterByCreatorButton.addSelectionListener(event);

		actionButton = new Button();
		actionButton.setText("Subscribe");
		actionButton.setToolTip("Subscribe to checked working sets");
		SelectionListener listener = new SelectionListener<ComponentEvent>() {
			@Override
			public void componentSelected(ComponentEvent ce) {
				if (binder.getSelection().get(0) != null)
					subscribe();

			}
		};
		actionButton.addSelectionListener(listener);
		actionButton.setWidth("50px");

		HorizontalPanel hp = new HorizontalPanel();
		hp.add(new HTML("<b>Filter by:</b> "));
		hp.add(textBox);
		hp.setSpacing(2);
		hp.add(filterByNameButton);
		hp.add(filterByCreatorButton);
		hp.add(filterByDateButton);

		LayoutContainer container = new LayoutContainer();
		container.setLayout(new RowLayout(Orientation.VERTICAL));
		container.add(hp);
		container.add(table, new RowData(1d, 1d));
		container.add(pagingBar, new RowData(1d, 30d));
		container.add(actionButton);

//		store.addFilter(filterByName);
//		filterByNameButton.disable();
		add(container, data);
	}

	@Override
	public void refresh() {

		refreshList();
		layout();
	}

	private void refreshList() {

		actionButton.disable();
		store.removeAll();
		WorkingSetCache.impl.getAllSubscribableWorkingSets(new GenericCallback<String>() {
			public void onFailure(Throwable caught) {
			}

			public void onSuccess(String arg0) {
				try {
					ArrayList workingsets = WorkingSetCache.impl.getSubscribable();
					for (int i = 0; i < workingsets.size(); i++) {
						WorkingSetData wsData = (WorkingSetData) workingsets.get(i);
						WSModel ws = new WSModel(wsData);
//						store.add(ws);
						pagingLoader.getFullList().add(ws);
					}
					
					pagingLoader.getPagingLoader().load();
					actionButton.enable();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void subscribe() {
		actionButton.disable();
		final WSModel ws = binder.getSelection().get(0);
		WorkingSetCache.impl.subscribeToPublicWorkingSet(ws.getID(), new GenericCallback<String>() {

			public void onFailure(Throwable caught) {

				WindowUtils.errorAlert(ws.getName()
						+ " was unable to be added to your working sets.  Please try again.");
				actionButton.enable();
			}

			public void onSuccess(String arg0) {
				WindowUtils.infoAlert("Working Set Added", ws.getName()
						+ " was successfully added to your working sets.  To view this " + ws.getName()
						+ " click on the quick summary tab.");
				actionButton.enable();
				// refreshList();
				store.remove(ws);
				
				ClientUIContainer.bodyContainer.tabManager.workingSetPage.redraw();
			}

		});

	}

}
