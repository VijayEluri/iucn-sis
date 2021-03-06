package org.iucn.sis.client.api.assessment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.iucn.sis.client.api.caches.AuthorizationCache;
import org.iucn.sis.client.api.container.SISClientBase;
import org.iucn.sis.client.api.utils.UriBase;
import org.iucn.sis.shared.api.acl.base.AuthorizableObject;
import org.iucn.sis.shared.api.citations.Referenceable;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.Edit;
import org.iucn.sis.shared.api.models.Reference;

import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.util.extjs.client.WindowUtils;

public class ReferenceableAssessment implements Referenceable {
	
	private final Assessment assessment;
	
	public ReferenceableAssessment(Assessment assessment) {
		this.assessment = assessment;
	}
	
	@Override
	public void addReferences(ArrayList<Reference> references, GenericCallback<Object> callback) {
		assessment.getReference().addAll(references);
		persist(callback);
	}
	
	@Override
	public void onReferenceChanged(GenericCallback<Object> callback) {
		// Don't care.
	}
	
	@Override
	public Set<Reference> getReferencesAsList() {
		return new HashSet<Reference>(assessment.getReference());
	}
	
	public Assessment getAssessment() {
		return assessment;
	}
	
	@Override
	public void removeReferences(ArrayList<Reference> references,
			GenericCallback<Object> callback) {
		assessment.getReference().removeAll(references);
		persist(callback);
	}
	
	@Override
	public ReferenceGroup groupBy() {
		return ReferenceGroup.Assessment;
	}
	
	private void persist(final GenericCallback<Object> callback) {
		if (!AuthorizationCache.impl.hasRight(AuthorizableObject.WRITE, assessment)) {
			WindowUtils.errorAlert("You cannot add references to an assessment "
					+ "you don't have permissions to edit.");
			return;
		}
		
		final StringBuilder out = new StringBuilder();
		out.append("<references>");
		for (Reference reference : assessment.getReference())
			out.append("<reference id=\"" + reference.getId() + "\" />");
		out.append("</references>");
		
		final NativeDocument document = SISClientBase.getHttpBasicNativeDocument();
		document.post(UriBase.getInstance().getSISBase() + "/changes/assessments/" + 
				assessment.getId() + "/references", out.toString(), new GenericCallback<String>() {
			public void onSuccess(String result) {
				assessment.addEdit(Edit.fromXML(document.getDocumentElement()));
				callback.onSuccess(result);
			}
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}
		});
	}

}
