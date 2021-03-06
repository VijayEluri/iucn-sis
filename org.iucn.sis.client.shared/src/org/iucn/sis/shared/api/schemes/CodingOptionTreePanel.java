package org.iucn.sis.shared.api.schemes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.iucn.sis.shared.api.views.components.TreeData;
import org.iucn.sis.shared.api.views.components.TreeDataRow;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.SortDir;
import com.extjs.gxt.ui.client.data.BaseTreeModel;
import com.extjs.gxt.ui.client.data.ModelKeyProvider;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.TreePanelEvent;
import com.extjs.gxt.ui.client.store.StoreSorter;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel;
import com.extjs.gxt.ui.client.widget.treepanel.TreePanel.CheckCascade;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.Timer;
import com.solertium.util.events.ComplexListener;
import com.solertium.util.extjs.client.WindowUtils;
import com.solertium.util.portable.PortableAlphanumericComparator;

public class CodingOptionTreePanel extends LayoutContainer {
	
	private Collection<TreeDataRow> selection;
	private TreePanel<CodingOption> tree;
	
	private boolean isRendered;
	
	public CodingOptionTreePanel(TreeData treeData, Collection<TreeDataRow> selected, Collection<String> disabledRows) {
		super(new FillLayout());
		this.selection = selected;
		this.isRendered = false;
		
		setLayoutOnChange(true);
		draw(treeData, selected, disabledRows);
	}
	
	public Set<TreeDataRow> getSelection() {
		if (!isRendered)
			return new HashSet<TreeDataRow>(selection);
		
		final HashSet<TreeDataRow> set = new HashSet<TreeDataRow>();
		for (CodingOption item : tree.getCheckedSelection())
			set.add(item.getRow());
		return set;
	}
	
	public TreePanel<CodingOption> getTree() {
		return tree;
	}
	
	public void draw(final TreeData treeData, final Collection<TreeDataRow> selected, final Collection<String> disabledRows) {
		final Map<String, CodingOption> checked = new HashMap<String, CodingOption>();
		for (TreeDataRow row : selected) 
			checked.put(row.getDisplayId(), new CodingOption(row));
		
		createTreeStore(treeData, checked, disabledRows, new ComplexListener<TreeStore<CodingOption>>() {
			public void handleEvent(TreeStore<CodingOption> eventData) {
				draw(eventData, treeData, checked, disabledRows);
			}
		});
	}
	
	private void draw(final TreeStore<CodingOption> store, final TreeData treeData, final Map<String, CodingOption> checked, final Collection<String> disabledRows) {
		
		
		tree = new TreePanel<CodingOption>(store);
		tree.setCheckStyle(CheckCascade.PARENTS);
		tree.setAutoLoad(true);
		tree.setCheckable(true);
		
		tree.setDisplayProperty("text");
		tree.addListener(Events.BeforeCheckChange, new Listener<TreePanelEvent<CodingOption>>() {
			public void handleEvent(TreePanelEvent<CodingOption> be) {
				if (tree.isRendered() && tree.isAttached() && be.getItem() != null) {
					be.setCancelled(be.getItem().isDisabled() || !be.getItem().isCodeable());
					if (be.getItem().isDisabled())
						WindowUtils.infoAlert("Please select this option from Quick Add.");
				}
			}
		});
		
		Button expandAll = new Button("Expand All", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				tree.expandAll();
			}
		});
		
		Button collapseAll = new Button("Collapse All", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				tree.collapseAll();
			}
		});

		final LayoutContainer container = new LayoutContainer(new BorderLayout()) {
			protected void afterRender() {
				super.afterRender();
				Timer t = new Timer() {
					public void run() {
						final List<CodingOption> toRemove = new ArrayList<CodingOption>();
						final List<CodingOption> checkList = new ArrayList<CodingOption>(checked.values());
						for (CodingOption option : checkList)
							if (disabledRows.contains(option.getRow().getRowNumber()))
								toRemove.add(option);
						for (CodingOption option : toRemove)
							checkList.remove(option);
						
						tree.setCheckedSelection(checkList);
						
						isRendered = true;
					}
				};
				t.schedule(1000);
			}
		};
		
		final ToolBar toolBar = new ToolBar();
		toolBar.add(new Html("&nbsp<u><b>Only selections <i>with a check icon</i> " + "will be saved.</b></u>"));
		toolBar.add(new FillToolItem());
		toolBar.add(expandAll);
		toolBar.add(collapseAll);
		
		container.add(toolBar, new BorderLayoutData(LayoutRegion.NORTH, 25, 25, 25));
		container.add(tree, new BorderLayoutData(LayoutRegion.CENTER));
		
		add(container);
	}
	
	private static void createTreeStore(TreeData treeData, Map<String, CodingOption> selection, Collection<String> disabled, final ComplexListener<TreeStore<CodingOption>> callback) {
		final IncrementalTreeStoreBuilder builder = new IncrementalTreeStoreBuilder(treeData, selection, disabled);
		builder.setListener(new ComplexListener<TreeStore<CodingOption>>() {
			public void handleEvent(TreeStore<CodingOption> eventData) {
				WindowUtils.hideLoadingAlert();
				callback.handleEvent(eventData);
			}
		});
		
		WindowUtils.showLoadingAlert("Loading Tree...");
		
		DeferredCommand.addPause();
		DeferredCommand.addCommand(builder);
	}

	@Deprecated
	@SuppressWarnings("unused")
	private static TreeStore<CodingOption> createTreeStore(TreeData treeData, Map<String, CodingOption> selection, Collection<String> disabled) {
		TreeStore<CodingOption> store = new TreeStore<CodingOption>();
		store.setStoreSorter(new StoreSorter<CodingOption>(new PortableAlphanumericComparator()));
		store.setKeyProvider(new ModelKeyProvider<CodingOption>() {
			public String getKey(CodingOption model) {
				return model.getValue();
			}
		});
		for (TreeDataRow row : treeData.getTreeRoots()) {
			CodingOption option;
			if (selection.containsKey(row.getDisplayId()))
				option = selection.get(row.getDisplayId());
			else
				option = new CodingOption(row);
			option.setDisabled(disabled.contains(row.getRowNumber()));
			if (option.isValid()) {
				flattenTree(store, selection, option, disabled);
				store.add(option, true);
			}
		}
		store.sort("text", SortDir.ASC);
		return store;
	}
	
	private static void flattenTree(TreeStore<CodingOption> store, Map<String, CodingOption> selection, CodingOption parent, Collection<String> disabled) {
		if (parent.isValid()) {
			for (TreeDataRow current : parent.getRow().getChildren()) {
				CodingOption child;
				if (selection.containsKey(current.getDisplayId())) {
					child = selection.get(current.getDisplayId());
					parent.incrementNumChildrenSelected();
				}
				else
					child = new CodingOption(current);
				child.setDisabled(disabled.contains(current.getRowNumber()));
				parent.add(child);
				
				flattenTree(store, selection, child, disabled);
			}
		}
	}
	
	private static boolean isLegacyOption(String text) {
		StringBuilder num = new StringBuilder();
		for (char c : text.toCharArray()) {
			if (Character.isDigit(c))
				num.append(c);
			else
				break;
		}
		boolean isLegacy = false;
		try {
			isLegacy = Integer.parseInt(num.toString()) >= 100;
		} catch (Exception e) {
			//Nothing to do
		}
		return isLegacy;
	}
	
	private static class CodingOption extends BaseTreeModel {
		
		private static final long serialVersionUID = 1L;
		
		private final TreeDataRow row;
		private final String rowID;
		
		private int numChildrenSelected;
		private boolean disabled;
		
		public CodingOption(TreeDataRow row) {
			super();
			this.row = row;
			this.rowID = row.getDisplayId();
			this.numChildrenSelected = 0;
			
			set("text", getDescription());
			set("value", row.getDisplayId());
		}
		
		public void incrementNumChildrenSelected() {
			numChildrenSelected++;
			set("text", getDescription());
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
		
		private String getDescription() {
			String curDesc = row.getDescription();
			
			StringBuilder displayableDesc = new StringBuilder();
			displayableDesc.append(row.getPrefix());
			displayableDesc.append(curDesc);

			if (!row.getChildren().isEmpty()) {
				displayableDesc.append(" (" + row.getChildren().size());
				if (numChildrenSelected > 0) {
					displayableDesc.append(", ");
					displayableDesc.append(numChildrenSelected);
					displayableDesc.append(" selected");
				}
				displayableDesc.append(')');
			}
			
			return displayableDesc.toString();
		}
		
		public boolean isValid() {
			return !isLegacyOption((String)get("text"));
		}
		
		public void setDisabled(boolean disabled) {
			this.disabled = disabled;
		}
		
		public boolean isDisabled() {
			return disabled;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((rowID == null) ? 0 : rowID.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CodingOption other = (CodingOption) obj;
			if (rowID == null) {
				if (other.rowID != null)
					return false;
			} else if (!rowID.equals(other.rowID))
				return false;
			return true;
		}
		
	}
	
	private static class IncrementalTreeStoreBuilder implements IncrementalCommand {
		
		private final TreeStore<CodingOption> store;
		private final Map<String, CodingOption> selection;
		private final Collection<String> disabled;
		
		private final List<TreeDataRow> treeRoots;
		
		private int current = 0;
		private int size;
		private ComplexListener<TreeStore<CodingOption>> listener;
		
		public IncrementalTreeStoreBuilder(TreeData treeData, Map<String, CodingOption> selection, Collection<String> disabled) {
			store = new TreeStore<CodingOption>();
			store.setStoreSorter(new StoreSorter<CodingOption>(new PortableAlphanumericComparator()));
			store.setKeyProvider(new ModelKeyProvider<CodingOption>() {
				public String getKey(CodingOption model) {
					return model.getValue();
				}
			});
			
			this.treeRoots = getActiveRoots(treeData.getTreeRoots());
			this.selection = selection;
			this.disabled = disabled;
			
			this.size = treeRoots.size();
		}
		
		private List<TreeDataRow> getActiveRoots(Collection<TreeDataRow> all) {
			List<TreeDataRow> rows = new ArrayList<TreeDataRow>();
			for (TreeDataRow row : all)
				if (new CodingOption(row).isValid())
					rows.add(row);
			return rows;
		}
		
		public void setListener(ComplexListener<TreeStore<CodingOption>> listener) {
			this.listener = listener;
		}
		
		@Override
		public boolean execute() {
			if (size < 5) {
				executeAll();
				store.sort("text", SortDir.ASC);
				if (listener != null)
					listener.handleEvent(store);
				return false;
			}
			else {
				if (current == size) {
					store.sort("text", SortDir.ASC);
					if (listener != null)
						listener.handleEvent(store);
					return false;
				}
	
				WindowUtils.showLoadingAlert("Loading Group " + (current+1) + " of " + size);
				
				TreeDataRow row = treeRoots.get(current);
				
				CodingOption option;
				if (selection.containsKey(row.getDisplayId()))
					option = selection.get(row.getDisplayId());
				else
					option = new CodingOption(row);
				option.setDisabled(disabled.contains(row.getRowNumber()));
				if (option.isValid()) {
					flattenTree(store, selection, option, disabled);
					store.add(option, true);
				}
				
				current++;
				
				return true;
			}
		}
		
		private void executeAll() {
			for (current = 0; current < size; current++) {
				TreeDataRow row = treeRoots.get(current);
				
				CodingOption option;
				if (selection.containsKey(row.getDisplayId()))
					option = selection.get(row.getDisplayId());
				else
					option = new CodingOption(row);
				option.setDisabled(disabled.contains(row.getRowNumber()));
				if (option.isValid()) {
					flattenTree(store, selection, option, disabled);
					store.add(option, true);
				}
			}
		}
	}

}
