package org.iucn.sis.client.panels.utils;

public interface TreeTableRenderer {

	/**
	 * Called to render a tree item row.
	 * 
	 * @param table
	 * @param item
	 * @param row
	 */
	void renderTreeItem(TreeTable table, TreeItem item, int row);
}
