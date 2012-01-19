package org.iucn.sis.client.panels;

import java.util.HashMap;
import java.util.Map;

import org.iucn.sis.client.api.container.SISClientBase;
import org.iucn.sis.client.api.utils.BasicWindow;
import org.iucn.sis.client.api.utils.SIS;
import org.iucn.sis.client.api.utils.UriBase;
import org.iucn.sis.shared.api.models.User;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.HtmlContainer;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.TextArea;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FillLayout;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.google.gwt.user.client.Window;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.util.extjs.client.FormBuilder;
import com.solertium.util.extjs.client.WindowUtils;
import com.solertium.util.portable.XMLWritingUtils;

public class AssemblaSupportPanel extends BasicWindow {
	
	private static final String QUESTION = "Ask a Question";
	private static final String BUG = "Report a Bug";
	
	private final TextArea area;
	private final SimpleComboBox<String> type;
	private final TextField<String> reporter;
	private final Map<String, String> text;
	
	public AssemblaSupportPanel() {
		super("Report an Issue", "icon-header-zendesk");
		setLayout(new FillLayout());
		setSize(450, 475);
		
		text = new HashMap<String, String>();
		text.put(QUESTION, "Question:\n");
		text.put(BUG, "Please answer the questions below to best help us resolve the problem:\n\n" +
			"What steps will reproduce the problem?\n" +
			"1. \n" +
			"2. \n" +
			"3. \n" +
			"\n" +
			"What is the expected output? What do you see instead?\n" +
			"\n" +
			"\n" +
			"What web browser and operating system are you using?\n" +
			"\n" +
			"\n" +
			"Please provide any additional information below.\n");
		
		final FormLayout layout = new FormLayout();
		layout.setDefaultWidth(300);
		
		final FormPanel form = new FormPanel();
		form.setBodyBorder(false);
		form.setBorders(false);
		form.setHeaderVisible(false);
		form.setLayout(layout);
		form.add(reporter = FormBuilder.createTextField("reporter", SISClientBase.currentUser.getDisplayableName(), "Reported By", true));
		form.add(type = FormBuilder.createComboBox("type", null, "I'd like to", true, BUG, QUESTION));
		form.add(area = FormBuilder.createTextArea("body", null, "Message", true));
		
		type.setForceSelection(true);
		type.addSelectionChangedListener(new SelectionChangedListener<SimpleComboValue<String>>() {
			public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se) {
				if (se.getSelectedItem() != null)
					area.setValue(text.get(se.getSelectedItem().getValue()));
			}
		});
		
		area.setSize(350, 300);
		
		type.setValue(type.findModel(BUG));
		
		int size = 25;
		final BorderLayoutData south = new BorderLayoutData(LayoutRegion.SOUTH, size, size, size);
		south.setSplit(false);
		
		final LayoutContainer container = new LayoutContainer(new BorderLayout());
		container.add(form, new BorderLayoutData(LayoutRegion.CENTER));
		container.add(createInfoPanel(), south);
		
		add(container);
		
		addButton(new Button("Submit", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				if (form.isValid()) {
					User user = SISClientBase.currentUser;
					String affil = user.getAffiliation();
					if ("".equals(affil))
						affil = "No affiliation";
					String subject = QUESTION.equals(type.getValue().getValue()) ? 
						"Question from SIS user " + user.getUsername() + " (" + user.getDisplayableName() + ", " + affil + ")" :
						"Bug Report from SIS user " + user.getUsername() + " (" + user.getDisplayableName() + ", " + affil + ")";
					String body = area.getValue();
					body += "\n\n" + SIS.getBuildNumber() + "\n";
					body += "Host: " + Window.Location.getHostName() + "\n";
					body += "URL: " + Window.Location.getPath() + Window.Location.getHash();
					
					submit(subject, reporter.getValue(), body);
				}
			}
		}));
		addButton(new Button("Cancel", new SelectionListener<ButtonEvent>() {
			public void componentSelected(ButtonEvent ce) {
				hide();
			}
		}));
	}
	
	private Component createInfoPanel() {
		final HtmlContainer container = new HtmlContainer();
		container.addStyleName("center");
		container.addStyleName("bold");
		container.addStyleName("italic");
		container.setHtml("For more information, visit " +
			"<a target=\"_blank\" href=\"http://sis.iucnsis.org/support\">" + 
			"http://sis.iucnsis.org/support</a>.");
		
		return container;
	}
	
	private void submit(String subject, String reporter, String value) {
		final StringBuilder out = new StringBuilder();
		out.append("<root>");
		out.append(XMLWritingUtils.writeCDATATag("subject", subject));
		out.append(XMLWritingUtils.writeCDATATag("reporter", reporter));
		out.append(XMLWritingUtils.writeCDATATag("body", value));
		out.append("</root>");
		
		final NativeDocument document = SISClientBase.getHttpBasicNativeDocument();
		document.post(UriBase.getInstance().getZendeskBase() + "/assembla/mail", out.toString(), new GenericCallback<String>() {
			public void onSuccess(String result) {
				hide();
				WindowUtils.infoAlert("Thank you", "Your ticket is being processed and will be filed shortly.  Please visit https://www.assembla.com/spaces/sis/support/tickets to track this ticket.");
			}
			public void onFailure(Throwable caught) {
				WindowUtils.errorAlert("Failed to communicate with help desk, please try again later.");
			}
		});
	}

}
