package org.iucn.sis.shared.api.integrity;

import org.iucn.sis.client.api.utils.BasicWindow;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.solertium.util.extjs.client.WindowUtils;

public class IntegrityRulesetPropertiesEditor extends BasicWindow {
	
	private final SISQBQuery query;
	
	private ComboBox<BaseModelData> failureCondition;

	public IntegrityRulesetPropertiesEditor(SISQBQuery query) {
		super("Edit Properties");
		this.query = query;
		
		setSize(450, 200);
		setLayout(new FillLayout());
		
		buildFailureConditionComboBox();
		
		final FormPanel form = new FormPanel();
		form.setBorders(false);
		form.setBodyBorder(false);
		form.setHeaderVisible(false);
		form.setLabelWidth(150);
		form.add(failureCondition);
		
		add(form);
// setAlignment(HorizontalAlignment.CENTER);
		addButton(new Button("Done", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				if (form.isValid()) {
					IntegrityRulesetPropertiesEditor.this.query.setProperty(
						failureCondition.getName(), (String)failureCondition.getValue().get("value")
					);
					
					hide();
				}
				else
					WindowUtils.errorAlert("Please fill in all fields first.");
			}
		}));
		addButton(new Button("Cancel", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				hide();
			}
		}));
	}
	
	private void buildFailureConditionComboBox() {
		final BaseModelData failIfMet = new BaseModelData();
		failIfMet.set("text", "Fail if conditions met (default)");
		failIfMet.set("value", "default");
		
		final BaseModelData failIfNotMet = new BaseModelData();
		failIfNotMet.set("text", "Fail if conditions not met");
		failIfNotMet.set("value", "not_met");
		
		final ListStore<BaseModelData> store = new ListStore<BaseModelData>();
		store.add(failIfMet);
		store.add(failIfNotMet);
		
		failureCondition = new ComboBox<BaseModelData>();
		failureCondition.setTriggerAction(TriggerAction.ALL);
		failureCondition.setAllowBlank(false);
		failureCondition.setFieldLabel("Failure Condition");
		failureCondition.setForceSelection(true);
		failureCondition.setName("failure_condition");
		failureCondition.setStore(store);
		if ("not_met".equals(query.getProperty("failure_condition")))
			failureCondition.setValue(failIfNotMet);
		else
			failureCondition.setValue(failIfMet);
	}

}
