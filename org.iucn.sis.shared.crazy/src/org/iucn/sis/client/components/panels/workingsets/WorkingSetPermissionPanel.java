package org.iucn.sis.client.components.panels.workingsets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.iucn.sis.client.acl.AuthorizationCache;
import org.iucn.sis.client.acl.ui.PermissionUserModel;
import org.iucn.sis.client.acl.ui.WorkingSetPermissionGiverPanel;
import org.iucn.sis.client.userui.UserModelTabPanel;
import org.iucn.sis.client.userui.BrowseUsersWindow.SearchResults;
import org.iucn.sis.shared.acl.User;
import org.iucn.sis.shared.acl.base.AuthorizableObject;
import org.iucn.sis.shared.acl.base.PermissionGroup;
import org.iucn.sis.shared.acl.base.PermissionSet;
import org.iucn.sis.shared.data.WorkingSetCache;

import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.Info;
import com.solertium.lwxml.factory.NativeDocumentFactory;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.lwxml.shared.utils.RowData;
import com.solertium.lwxml.shared.utils.RowParser;
import com.solertium.util.extjs.client.WindowUtils;

public class WorkingSetPermissionPanel extends WorkingSetPermissionGiverPanel {

	public WorkingSetPermissionPanel() {
		final String permGroupName = "ws" + WorkingSetCache.impl.getCurrentWorkingSet().getId();
		final String query = "?quickgroup=" + permGroupName + "";
		final NativeDocument document = NativeDocumentFactory.newNativeDocument();
		document.get(UserModelTabPanel.CONSTANTS_ATTACHMENT_POINT
				+ "/browse/profile" + query, new GenericCallback<String>() {
			public void onFailure(Throwable caught) {
				draw();
				layout();
			}

			public void onSuccess(String result) {
				try {
					final RowParser parser = new RowParser(document);
					ListStore<PermissionUserModel> model = new ListStore<PermissionUserModel>();

					for( RowData curData : parser.getRows() ) {
						SearchResults curSearchResult = new SearchResults(curData);
						String quickGroup = curSearchResult.getUser().getProperty("quickGroup");
						String permString = null;
						boolean assessor = quickGroup.matches(".*?" + permGroupName + "assessor(\\b|,).*");
						
						if( quickGroup.matches(".*?" + permGroupName + "r(\\b|,).*"))
							permString = "read";
						else if( quickGroup.matches(".*?" + permGroupName + "rw(\\b|,).*"))
							permString = "read,write";
						else if( quickGroup.matches(".*?" + permGroupName + "rwg(\\b|,).*"))
							permString = "read,write,grant";
						else if( quickGroup.matches(".*?" + permGroupName + "rg(\\b|,).*"))
							permString = "read,grant";
						
						if( permString != null )
							model.add(new PermissionUserModel(curSearchResult.getUser(), permString, assessor));
					}

					setAssociatedPermissions(model);
					draw();
					layout();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@Override
	protected void onRemoveUsers(final List<PermissionUserModel> removed) {
		StringBuilder body = new StringBuilder("<updates>\r\n");
		
		for( final PermissionUserModel cur : removed ) {
			final User user = cur.getUser();
			final String newGroup = removeGroupsFromUser(user);
			
			body.append("<user id=\"" + user.getId() + "\"><field name=\"quickGroup\">" + newGroup 
					+ "</field></user>");
		}
		body.append("</updates>");

		final NativeDocument document = NativeDocumentFactory.newNativeDocument();
		document.post(UserModelTabPanel.CONSTANTS_ATTACHMENT_POINT + "/list/batch", body.toString(),
				new GenericCallback<String>() {
			public void onFailure(Throwable caught) {
				Info.display("Error", "Could not save changes, please try again later.");
			}

			public void onSuccess(String result) {
				for( final PermissionUserModel cur : removed ) {
					final User user = cur.getUser();
					final String newGroup = removeGroupsFromUser(user);
					user.setProperty("quickGroup", newGroup);
				}

				associatedPermissions.filter("all");
				if( associatedPermissions.getModels().size() == 0 ) {
					deletePermissionGroups();
				} else
					Info.display("Success", "Changes saved.");
				associatedPermissions.filter("permission");
			}
		});
	}
	
	@Override
	public void onSave() {
		final HashMap<User, String> usersToUpdate = new HashMap<User, String>();
		final List<PermissionGroup> groupsToAdd = new ArrayList<PermissionGroup>();
		final ListStore<PermissionUserModel> models = getAssociatedPermissions();
		models.commitChanges();
		
		for( final PermissionUserModel cur : models.getModels() ) {
			String [] perms = cur.getPermission().split(",");
			String permGroupName = "ws" + WorkingSetCache.impl.getCurrentWorkingSet().getId();
			String permAssessorGroup = permGroupName + "assessor";
			boolean read = false, write = false, grant = false; 
			for( String curPerm : perms ) {
				switch( curPerm.charAt(0) ) {
				case 'r':
					read = true;
					break;
				case 'w':
					write = true;
					break;
				case 'g':
					grant = true;
					break;
				}
			}
			if( read && !write && !grant )
				permGroupName += "r";
			else if( write && !grant )
				permGroupName += "rw";
			else if( write && grant )
				permGroupName += "rwg";
			else if( !write && grant )
				permGroupName += "rg";

			final PermissionGroup perm = new PermissionGroup(permGroupName);
			
			if( cur.isAssessor() ) {
				if( !AuthorizationCache.impl.getGroups().containsKey(permAssessorGroup) && 
						!groupsToAdd.contains(permAssessorGroup) ) {
					final PermissionGroup permAssessor = new PermissionGroup(permAssessorGroup);
					permAssessor.setScopeURI("workingSet/" + WorkingSetCache.impl.getCurrentWorkingSet().getId());
					permAssessor.addInheritence(AuthorizationCache.impl.getGroups().get("assessor"));
					groupsToAdd.add(permAssessor);
				}
			}

			if( !AuthorizationCache.impl.getGroups().containsKey(permGroupName) && !groupsToAdd.contains(perm) ) {
				PermissionSet set = new PermissionSet();
				set.set(AuthorizableObject.CREATE, Boolean.FALSE);
				set.set(AuthorizableObject.DELETE, Boolean.FALSE);
				set.set(AuthorizableObject.WRITE, Boolean.FALSE);
				set.set(AuthorizableObject.GRANT, Boolean.FALSE);
				set.set(AuthorizableObject.READ, Boolean.FALSE);
				
				for( String curPerm : perms ) {
					switch( curPerm.charAt(0) ) {
					case 'r':
						set.set(AuthorizableObject.READ, Boolean.TRUE);
						break;
					case 'w':
						set.set(AuthorizableObject.READ, Boolean.TRUE);
						set.set(AuthorizableObject.WRITE, Boolean.TRUE);
						break;
					case 'g':
						set.set(AuthorizableObject.READ, Boolean.TRUE);
						set.set(AuthorizableObject.GRANT, Boolean.TRUE);
						break;
					}
				}
				perm.addPermissionResource(WorkingSetCache.impl.getCurrentWorkingSet().getFullURI(), set);
				groupsToAdd.add(perm);
			}

			StringBuilder newGroups = new StringBuilder();
			String curGroup = cur.getUser().getProperty("quickGroup");
			boolean needsChange = 
				(!cur.isAssessor() && curGroup.matches(".*?" + permAssessorGroup + "(\\b|,).*?")) ||
				!curGroup.matches(".*?" + permGroupName + "(\\b|,).*?") ||
				(cur.isAssessor() && !curGroup.matches(".*?" + permAssessorGroup + "(\\b|,).*?")); 
			if( needsChange ) {
				newGroups.append(permGroupName);
				if( cur.isAssessor() )
					newGroups.append("," + permAssessorGroup);
			}
			
			if( newGroups.length() > 0 )
				usersToUpdate.put(cur.getUser(), newGroups.toString());
		}

		if( groupsToAdd.size() > 0 )
			AuthorizationCache.impl.saveGroups(groupsToAdd, new GenericCallback<String>() {
				public void onSuccess(String result) {
					updateQuickGroup(usersToUpdate);
				}
				public void onFailure(Throwable caught) {
					WindowUtils.errorAlert("Error!", "Unable to create working set permission group. " +
					"Please check your Internet connection and try again.");
				}
			});
		else
			updateQuickGroup(usersToUpdate);
	}

	private void updateQuickGroup(final HashMap<User, String> users) {
		if( users.size() == 0 ) {
			Info.display("No Changes", "No changes to save.");
			return;
		}
		
		StringBuilder body = new StringBuilder("<updates>\r\n");

		for(Entry<User, String> entry : users.entrySet()) {
			final User user = entry.getKey();
			final String newGroup = removeGroupsFromUser(user) + "," + entry.getValue();
			
			body.append("<user id=\"" + user.getId() + "\"><field name=\"quickGroup\">" + newGroup 
					+ "</field></user>");
		}

		body.append("</updates>");

		final NativeDocument document = NativeDocumentFactory.newNativeDocument();
		document.post(UserModelTabPanel.CONSTANTS_ATTACHMENT_POINT + "/list/batch", body.toString(),
				new GenericCallback<String>() {
			public void onFailure(Throwable caught) {
				Info.display("Error", "Could not save changes, please try again later.");
			}

			public void onSuccess(String result) {
				for(Entry<User, String> entry : users.entrySet())
					entry.getKey().setProperty("quickGroup", removeGroupsFromUser(entry.getKey()) 
							+ "," + entry.getValue());

				Info.display("Success", "Changes saved.");
			}
		});
	}

	private String removeGroupsFromUser(final User user) {
		return user.getProperty("quickGroup").replaceAll(",?" + "ws" + 
				WorkingSetCache.impl.getCurrentWorkingSet().getId() + "(assessor|[rwg]+)", "");
	}
	
	private void deletePermissionGroups() {
		StringBuilder groupsToRemove = new StringBuilder();
		final String prefix = "ws" + WorkingSetCache.impl.getCurrentWorkingSet().getId();
		if( AuthorizationCache.impl.getGroups().containsKey(prefix + "r") )
			groupsToRemove.append(prefix + "r,");
		if( AuthorizationCache.impl.getGroups().containsKey(prefix + "rw") )
			groupsToRemove.append(prefix + "rw,");
		if( AuthorizationCache.impl.getGroups().containsKey(prefix + "rwg") )
			groupsToRemove.append(prefix + "rwg,");
		if( AuthorizationCache.impl.getGroups().containsKey(prefix + "rg") )
			groupsToRemove.append(prefix + "rg,");
		if( AuthorizationCache.impl.getGroups().containsKey(prefix + "assessor") )
			groupsToRemove.append(prefix + "assessor,");
		
		if( groupsToRemove.length() > 0 )
			AuthorizationCache.impl.removeGroups(groupsToRemove.substring(0, groupsToRemove.length()-1), 
					new GenericCallback<String>() {
				public void onSuccess(String result) {
					Info.display("Success!", "Changes saved and permission groups removed for cleanliness.");
				}
				public void onFailure(Throwable caught) {
					Info.display("Error", "Error automatically removing permission groups. You should " +
							"manually delete them if you are done delegating permissions for this " +
							"working set. They will all start with \"" + prefix + "\".");		
				}
			});
	}

}
