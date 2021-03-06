package org.iucn.sis.client.integrity;

import org.iucn.sis.shared.data.WorkingSetData;

import com.google.gwt.user.client.Window;
import com.solertium.lwxml.factory.NativeDocumentFactory;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.util.extjs.client.WindowUtils;

/**
 * ClientAssessmentValidator.java
 * 
 * Utility class that makes a call to the server to validate and assessment
 * against a ruleset.
 * 
 * @author <a href="mailto:carl.scott@solertium.com">Carl Scott</a>, <a
 *         href="http://www.solertium.com">Solertium Corporation</a>
 * 
 */
public class ClientAssessmentValidator {
	
	public static void validate(WorkingSetData workingSet) {
		//List<String> assessmentIDs = workingSet.getSpeciesIDs();
		
		StringBuilder url = new StringBuilder();
		url.append(IntegrityApplicationPanel.createUrl(null, "validate"));
		url.append('?');
		url.append("set=" + workingSet.getId());
		/*for(Iterator<String> iter = workingSet.getSpeciesIDs().iterator(); iter.hasNext(); ) {
			url.append("id=" + iter.next());
			url.append('&');
			url.append("type=" + BaseAssessment.DRAFT_ASSESSMENT_STATUS);
			if (iter.hasNext())
				url.append('&');
		}*/
		
		Window.open(url.toString(), "Assessment Validation Results", "");
	}
	
	public static void validate(final String assessmentID, final String assessmentType) {
		StringBuilder url = new StringBuilder();
		url.append(IntegrityApplicationPanel.createUrl(null, "validate"));
		url.append('?');
		url.append("id=" + assessmentID);
		url.append('&');
		url.append("type=" + assessmentType);
		
		Window.open(url.toString(), assessmentID, "");
	}
	
	public static void validate(final String assessmentID, final String assessmentType, final GenericCallback<NativeDocument> callback) {
		validate(assessmentID, assessmentType, null, callback);
	}

	public static void validate(final String assessmentID, final String assessmentType,
			final String rulesetName,
			final GenericCallback<NativeDocument> callback) {
		final String body = "<root><assessment type=\"" + assessmentType + "\">" + assessmentID
				+ "</assessment></root>";

		final NativeDocument document = NativeDocumentFactory
				.newNativeDocument();
		document.postAsText(IntegrityApplicationPanel.createUrl(rulesetName,
				"validate"), body, new GenericCallback<String>() {
			public void onFailure(Throwable caught) {
				WindowUtils
						.errorAlert("Error in validation process, please try again later.");
			}

			public void onSuccess(String result) {
				callback.onSuccess(document);
			}
		});
	}

}
