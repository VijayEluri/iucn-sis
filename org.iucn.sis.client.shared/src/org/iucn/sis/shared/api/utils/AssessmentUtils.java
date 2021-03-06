package org.iucn.sis.shared.api.utils;

import java.util.List;

import org.iucn.sis.client.api.caches.AssessmentCache;
import org.iucn.sis.client.api.caches.FetchMode;
import org.iucn.sis.client.api.caches.WorkingSetCache;
import org.iucn.sis.client.api.container.SISClientBase;
import org.iucn.sis.client.api.container.StateManager;
import org.iucn.sis.client.api.utils.UriBase;
import org.iucn.sis.shared.api.assessments.AssessmentDeepCopyFilter;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.AssessmentFilter;
import org.iucn.sis.shared.api.models.Field;
import org.iucn.sis.shared.api.models.Taxon;
import org.iucn.sis.shared.api.models.fields.RegionField;

import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.lwxml.shared.NativeDocument;

public class AssessmentUtils {
	
	/**
	 * Creates a new assessment.
	 * 
	 * @param taxon - the Taxon that is being assessed
	 * @param type - the assessment type, see AssessmentType
	 * @param template - an assessment to be used as a template, or null to create from scratch
	 * @param region - the Locality specification for this new assessment
	 * @param endemic TODO
	 * @param wayback - a GenericCallback
	 */
	public static void createNewAssessment(final Taxon taxon, final String type, final String schema, Assessment template, 
			List<Integer> regions, Boolean endemic, final GenericCallback<String> wayback) {
		Assessment newAssessment = null;
		
		if (template != null)
			newAssessment = template.deepCopy(new AssessmentDeepCopyFilter());
		else
			newAssessment = new Assessment();
		
		newAssessment.setTaxon(taxon);
		newAssessment.setType(type);
		newAssessment.setSchema(schema);
		
		if (regions != null && endemic != null) {
			Field regionField = new Field(CanonicalNames.RegionInformation, newAssessment);
			
			RegionField proxy = new RegionField(regionField);
			proxy.setRegions(regions);
			proxy.setEndemic(endemic);
			
			newAssessment.setField(regionField);
		}
		
		final NativeDocument ndoc = SISClientBase.getHttpBasicNativeDocument();
		ndoc.putAsText(UriBase.getInstance().getSISBase() + "/assessments", newAssessment.toXML(), new GenericCallback<String>() {
			public void onSuccess(String result) {
				//FIXME: the call should already return the newly created entity...
				final Integer newID = Integer.valueOf(ndoc.getText());
				AssessmentCache.impl.fetchAssessment(newID, FetchMode.FULL, new GenericCallback<Assessment>() {
					public void onSuccess(Assessment result) {
						//AssessmentCache.impl.getAssessment(Integer.valueOf(newID), true);
						WorkingSetCache.impl.uncacheAssessmentsForWorkingSets();
						StateManager.impl.setAssessment(result);
					};
					public void onFailure(Throwable caught) {
						wayback.onFailure(caught);
					};
				});
				
				wayback.onSuccess(ndoc.getText());
			}
			public void onFailure(Throwable caught) {
				wayback.onFailure(caught);
			}
		});
	}
	
	public static void createGlobalDraftAssessments(List<Integer> taxaIDs, boolean useTemplate,
			AssessmentFilter filter, final GenericCallback<String> wayback) {
		if (taxaIDs.isEmpty())
			wayback.onSuccess(null);
		else {
			StringBuilder ret = new StringBuilder("<create>");
			ret.append(filter.toXML());
			ret.append("<useTemplate>" + useTemplate + "</useTemplate>");
			for( Integer curID : taxaIDs ) {
				ret.append("<taxon>");
				ret.append(curID);
				ret.append("</taxon>");
			}
			ret.append("</create>");

			final NativeDocument ndoc = SISClientBase.getHttpBasicNativeDocument();
			ndoc.postAsText(UriBase.getInstance().getSISBase() +"/assessments?action=batch", ret.toString(), new GenericCallback<String>() {
				public void onSuccess(String result) {
					AssessmentCache.impl.clear();
					
					wayback.onSuccess(ndoc.getText());
				}
				public void onFailure(Throwable caught) {
					wayback.onFailure(caught);
				}
			});	
		}
	}
}
