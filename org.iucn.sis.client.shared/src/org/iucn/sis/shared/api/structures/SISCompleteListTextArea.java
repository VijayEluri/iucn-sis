package org.iucn.sis.shared.api.structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.iucn.sis.client.api.models.ClientUser;
import org.iucn.sis.client.api.ui.users.panels.UserSearchController;
import org.iucn.sis.client.api.ui.users.panels.UserSearchController.SearchResults;
import org.iucn.sis.client.api.utils.SIS;
import org.iucn.sis.shared.api.models.Field;
import org.iucn.sis.shared.api.models.fields.RedListCreditedUserField;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.fx.FxConfig;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.button.ButtonBar;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.util.extjs.client.WindowUtils;

public class SISCompleteListTextArea extends VerticalPanel {

	private final TextArea textArea;
	private final HTML viewOnlyText;
	
	private RedListCreditedUserField field;
	private List<ClientUser> selectedUsers;
	
	private Button edit;

	private boolean ready;
	
	public SISCompleteListTextArea() {
		textArea = new TextArea();
		textArea.setEmptyText("Use \"Manage Credits\" to select users or click \"Edit\" to set specific credits.");
		textArea.setReadOnly(true);
		textArea.setSize("100%", "100%");
		
		viewOnlyText = new HTML();

		setWidth("100%");
		setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
		setSpacing(4);
		
		final ButtonBar bar = new ButtonBar();
		bar.setSize("100%", "100%");
		/*bar.add(generate = new Button("Generate", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				generateTextFromUsers(true);
			}
		}));*/
		bar.add(edit = new Button("Edit", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				if ("Edit".equals(edit.getText())) {
					textArea.setReadOnly(false);
					textArea.el().blink(FxConfig.NONE);
					
					DeferredCommand.addCommand(new Command() {
						public void execute() {
							textArea.focus();
							textArea.setCursorPos(0);
						}
					});
				
					edit.setText("Done");
				}
				else {
					textArea.setReadOnly(true);
					textArea.el().blink(FxConfig.NONE);
					
					viewOnlyText.setHTML(textArea.getValue());
					
					DeferredCommand.addCommand(new Command() {
						public void execute() {
							textArea.el().focus();
							textArea.setCursorPos(0);
						}
					});
				
					edit.setText("Edit");
				}
			}
		}));

		add(textArea);
		add(bar);
		
		ready = false;
	}
	
	public HTML getViewOnlyLabel() {
		return viewOnlyText;
	}
	
	private String getText() {
		String text = textArea.getValue();
		if (text == null)
			text = "";
		
		return text;
	}
	
	public boolean hasChanged(Field other) {
		if (!isReady())
			return false;
		
		String text = getText();
		
		if (other == null)
			return !"".equals(text);
		else {
			RedListCreditedUserField otherProxy = new RedListCreditedUserField(other);
			return !text.equals(otherProxy.getText());
		}
	}
	
	public void save(Field other) {
		RedListCreditedUserField proxy = new RedListCreditedUserField(other);
		//From the saved info, immutable here...
		proxy.setOrder(field.getOrder());
		proxy.setUsers(field.getUsers());
		
		//From the text area...
		proxy.setText(getText());
	}
	
	public void setData(RedListCreditedUserField field) {
		this.ready = false;
		this.field = field;
		if (!field.getUsers().isEmpty()) {
			edit.setVisible(false);
			
			final List<String> users = new ArrayList<String>();
			for (Integer userID : field.getUsers())
				if (userID != 0)
					users.add(userID.toString());
			
			if (users.isEmpty())
				setUsers(new ArrayList<ClientUser>());
			else {
				HashMap<String, List<String>> map = new HashMap<String, List<String>>();
				map.put("userid", users);
				
				UserSearchController.search(map, "or", true, new GenericCallback<List<SearchResults>>() {
					public void onFailure(Throwable caught) {
						ready = true;
						WindowUtils.errorAlert("Error", "An error occurred searching for users. " +
							"Please check your Internet connection, then try again.");
					}
					public void onSuccess(List<SearchResults> results) {
						List<ClientUser> found = new ArrayList<ClientUser>();
						for (SearchResults result : results)
							found.add(result.getUser());
						if (users.size() != results.size() && SIS.isDebugMode())
							WindowUtils.infoAlert("Warning", "Warning: The number of users " +
									"saved for this assessment (" + users.size() + 
									") does not match the number of users found in the " +
									"database (" + results.size() + "). These users may have " +
									"been deleted, but they will not appear here.");
						setUsers(found);
					};
				});
			}
		} else {
			edit.setVisible(!"".equals(field.getText()));
			setUsers(new ArrayList<ClientUser>());
		}
	}
	
	private void setUsers(List<ClientUser> users) {
		selectedUsers = users;
		//generate.setEnabled(!users.isEmpty());
		edit.setText("Edit");
		textArea.setReadOnly(true);
		generateTextFromUsers(false);
		ready = true;
	}
	
	private void generateTextFromUsers(boolean force) {
		String value;
		if (force || "".equals(field.getText()))
			value = RedListCreditedUserField.generateText(selectedUsers, field.getOrder());
		else
			value = field.getText();
		
		textArea.setValue(value);
		viewOnlyText.setHTML(value);
	}
	
	public boolean isReady() {
		return ready;
	}

}
