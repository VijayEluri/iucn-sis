package org.iucn.sis.client.api.caches;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.iucn.sis.client.api.container.SISClientBase;
import org.iucn.sis.client.api.container.StateManager;
import org.iucn.sis.client.api.utils.UriBase;
import org.iucn.sis.shared.api.assessments.AssessmentFetchRequest;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.AssessmentType;
import org.iucn.sis.shared.api.models.RecentlyAccessed;
import org.iucn.sis.shared.api.models.Taxon;

import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.lwxml.shared.NativeElement;
import com.solertium.lwxml.shared.NativeNodeList;

public class AssessmentCache {
	
	public static final AssessmentCache impl = new AssessmentCache();

	private Map<Integer, CacheEntry<Assessment>> cache;
	private Map<Integer, List<CacheEntry<Assessment>>> taxonToAssessmentCache;
	
	private AssessmentCache() {
		cache = new HashMap<Integer, CacheEntry<Assessment>>();
		taxonToAssessmentCache = new HashMap<Integer, List<CacheEntry<Assessment>>>();
	}

	public void addAssessment(Assessment assessment, FetchMode mode) {
		CacheEntry<Assessment> entry = new CacheEntry<Assessment>(assessment, mode);
		CacheEntry<Assessment> old = cache.put(assessment.getId(), entry);
		
		if (taxonToAssessmentCache.containsKey(assessment.getSpeciesID())) {
			List<CacheEntry<Assessment>> list = taxonToAssessmentCache.get(assessment.getSpeciesID());
			if (old != null)
				list.remove(old);
			list.add(entry);
		}
	}
	
	public void clear() {
		cache.clear();
		taxonToAssessmentCache.clear();
	}
	
	public boolean contains(Integer id) {
		return cache.containsKey(id);
	}
	
	public boolean contains(int id) {
		return contains(Integer.valueOf(id));
	}
	
	public void doLogout() {
		clear();
	}

	public void evictAssessments(String ids) {
		if (ids != null) {
			String[] array = ids.indexOf(",") > -1 ? ids.split(",") : new String[] { ids } ;
			for (String id : array)
				remove(id);
		}
	}
	
	public void evictTaxonToAssessment(int taxonID) {
		taxonToAssessmentCache.remove(taxonID);
	}

	public Assessment remove(Integer id) {
		CacheEntry<Assessment> entry = cache.remove(Integer.valueOf(id));
		return entry == null ? null : entry.getEntry();
	}
	
	public Assessment remove(String id) {
		return remove(Integer.valueOf(id));
	}
	
	public boolean isCached(Integer id, FetchMode mode) {
		CacheEntry<Assessment> cached = cache.get(id);
		return cached != null && (FetchMode.PARTIAL.equals(mode) || FetchMode.FULL.equals(cached.getMode()));
	}
	
	public void fetchAssessment(final Integer id, FetchMode mode, final GenericCallback<Assessment> callback) {
		if (isCached(id, mode))
			callback.onSuccess(getAssessment(id));
		else {
			AssessmentFetchRequest req = new AssessmentFetchRequest();
			req.addAssessment(id);
			
			fetchAssessments(req, mode, new GenericCallback<String>() {
				public void onSuccess(String result) {
					callback.onSuccess(getAssessment(id));
				}
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}
			});
		}
	}
	
	public void fetchAssessments(final Collection<Integer> ids, FetchMode mode, final GenericCallback<Collection<Assessment>> callback) {
		AssessmentFetchRequest req = new AssessmentFetchRequest();
		for (Integer id : ids)
			if (!isCached(id, mode))
				req.addAssessment(id);
		
		fetchAssessments(req, mode, new GenericCallback<String>() {
			public void onSuccess(String result) {
				List<Assessment> list = new ArrayList<Assessment>();
				for (Integer id : ids)
					list.add(getAssessment(id));
				callback.onSuccess(list);
			}
			public void onFailure(Throwable caught) {
				callback.onFailure(caught);
			}
		});
	}
	
	public void fetchPartialAssessmentsForTaxon(final Integer taxonID, final GenericCallback<String> callback) {
		List<CacheEntry<Assessment>> list = taxonToAssessmentCache.get(taxonID);
		if (list != null) {
			callback.onSuccess("OK");
		}
		else {
			AssessmentFetchRequest req = new AssessmentFetchRequest();
			req.addForTaxon(taxonID);
			
			taxonToAssessmentCache.put(taxonID, new ArrayList<CacheEntry<Assessment>>());
			fetchAssessments(req, FetchMode.PARTIAL, new GenericCallback<String>() {
				public void onSuccess(String result) {
					if (taxonToAssessmentCache.get(taxonID) == null || taxonToAssessmentCache.get(taxonID).isEmpty())
						taxonToAssessmentCache.remove(taxonID);
					callback.onSuccess(result);
				}
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}
			});
		}
	}
	
	/**
	 * @deprecated use another fetch call!
	 */
	public void fetchAssessments(AssessmentFetchRequest request, final GenericCallback<String> callback) {
		fetchAssessments(request, FetchMode.FULL, callback);
	}
	
	private void fetchAssessments(AssessmentFetchRequest request, final FetchMode mode, final GenericCallback<String> callback) {
		if (request.getTaxonIDs().size() == 0 && request.getAssessmentUIDs().size() == 0)
			callback.onSuccess("OK");
		else {
			final NativeDocument ndoc = SISClientBase.getHttpBasicNativeDocument();
			ndoc.post(UriBase.getInstance().getSISBase() + "/assessments?action=fetch&mode=" + mode.name(), request.toXML(), new GenericCallback<String>() {
				public void onSuccess(String result) {
					NativeNodeList asses = ndoc.getDocumentElement().getElementsByTagName(Assessment.ROOT_TAG);
					for (int i = 0; i < asses.getLength(); i++) {
						NativeElement el = asses.elementAt(i);
						try {
							Assessment current = Assessment.fromXML(el);
							Taxon t = TaxonomyCache.impl.getTaxon(current.getSpeciesID());
							if (t != null)
								current.setTaxon(t);

							addAssessment(current, mode);
						} catch (Throwable e) {
							Debug.println("Error caching assessment: {0}", e);
						}
					}
					callback.onSuccess(ndoc.getStatusText());
				}
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}
			});
		}
	}

	public Assessment getCurrentAssessment() {
		return StateManager.impl.getAssessment();
	}
	
	public Assessment getAssessment(Integer id) {
		CacheEntry<Assessment> cached = cache.get(id);
		return cached == null ? null : cached.getEntry();
	}
	
	public Assessment getUserAssessment(int id) {
		return getUserAssessment(Integer.valueOf(id));
	}
		
	public Assessment getUserAssessment(Integer id) {
		return getAssessment(id);
	}

	public Assessment getDraftAssessment(Integer id) {
		return getAssessment(id);
	}
	
	public Assessment getPublishedAssessment(Integer id) {
		return getAssessment(id);
	}
	
	public Set<Assessment> getDraftAssessmentsForTaxon(Integer taxonID) {
		return getDraftAssessmentsForTaxon(taxonID, SchemaCache.impl.getDefaultSchema());
	}
	
	public Set<Assessment> getDraftAssessmentsForTaxon(Integer taxonID, String schema) {
		return getAssessmentsForTaxon(taxonID, AssessmentType.DRAFT_ASSESSMENT_STATUS_ID, schema);
	}
	
	public Set<Assessment> getPublishedAssessmentsForTaxon(Integer taxonID) {
		return getPublishedAssessmentsForTaxon(taxonID, SchemaCache.impl.getDefaultSchema());
	}
	
	public Set<Assessment> getPublishedAssessmentsForTaxon(Integer taxonID, String schema) {
		return getAssessmentsForTaxon(taxonID, AssessmentType.PUBLISHED_ASSESSMENT_STATUS_ID, schema);
	}
	
	public Set<Assessment> getUnpublishedAssessmentsForTaxon(Integer taxonID) {
		return getUnpublishedAssessmentsForTaxon(taxonID, SchemaCache.impl.getDefaultSchema());
	}
	
	public Set<Assessment> getUnpublishedAssessmentsForTaxon(Integer taxonID, String schema) {
		Set<Integer> allowed = new HashSet<Integer>();
		allowed.add(AssessmentType.DRAFT_ASSESSMENT_STATUS_ID);
		allowed.add(AssessmentType.SUBMITTED_ASSESSMENT_STATUS_ID);
		allowed.add(AssessmentType.FOR_PUBLICATION_ASSESSMENT_STATUS_ID);
		
		return getAssessmentsForTaxon(taxonID, allowed, schema);
	}
	
	public Set<Assessment> getAllAssessmentsForTaxon(Integer taxonID) {
		return getAllAssessmentsForTaxon(taxonID, SchemaCache.impl.getDefaultSchema());
	}
	
	public Set<Assessment> getAllAssessmentsForTaxon(Integer taxonID, String schema) {
		Set<Integer> allowed = new HashSet<Integer>();
		allowed.add(AssessmentType.DRAFT_ASSESSMENT_STATUS_ID);
		allowed.add(AssessmentType.SUBMITTED_ASSESSMENT_STATUS_ID);
		allowed.add(AssessmentType.FOR_PUBLICATION_ASSESSMENT_STATUS_ID);
		allowed.add(AssessmentType.PUBLISHED_ASSESSMENT_STATUS_ID);
		
		return getAssessmentsForTaxon(taxonID, allowed, schema);
	}
	
	public Set<Assessment> getAssessmentsForTaxon(Integer taxonID, Integer assessmentType, String schema) {
		Set<Integer> allowed = new HashSet<Integer>();
		allowed.add(assessmentType);
		
		return getAssessmentsForTaxon(taxonID, allowed, schema);
	}
		
	public Set<Assessment> getAssessmentsForTaxon(Integer taxonID, Set<Integer> allowedAssessmentType, String schema) {
		if (taxonToAssessmentCache.containsKey(taxonID)) {
			Set<Assessment> assessments = new HashSet<Assessment>();
			for (CacheEntry<Assessment> current : taxonToAssessmentCache.get(taxonID)) {
				Assessment cur = current.getEntry();
				String curSchema = cur.getSchema(SchemaCache.impl.getDefaultSchema());
				if ((schema == null || schema.equals(curSchema)) && 
						allowedAssessmentType.contains(cur.getAssessmentType().getId()))
					assessments.add(cur);
			}
			return assessments;
		} else
			return new HashSet<Assessment>();
	}
	
	public List<RecentlyAccessedCache.RecentAssessment> getRecentAssessments() {
		return RecentlyAccessedCache.impl.list(RecentlyAccessed.ASSESSMENT);
	}

	/**
	 * 
	 * @deprecated use RecentlyAccessedCache directly.
	 */
	public void loadRecentAssessments(final GenericCallback<Object> wayBacks) {
		RecentlyAccessedCache.impl.load(RecentlyAccessed.ASSESSMENT, wayBacks);
	}
	
	public void uncache(Integer id) {
		cache.remove(id);
	}
	
	public void uncache(Collection<Integer> ids) {
		for (Integer id : ids)
			uncache(id);
	}

	/*public void resetCurrentAssessment() {
		setCurrentAssessment(null);
	}

	
	public void setCurrentAssessment(final Assessment assessment) {
		setCurrentAssessment(assessment, true);
	}
	
	public void setCurrentAssessment(final Assessment assessment, boolean saveIfNecessary) {
		if (assessment != null) {
			//Used to set the current taxon, no longer needed...
			TaxonomyCache.impl.fetchTaxon(assessment.getSpeciesID(), false, saveIfNecessary, new GenericCallback<Taxon>() {
				public void onSuccess(Taxon result) {
					doSetCurrentAssessment(result, assessment);
				}
				public void onFailure(Throwable caught) {
					WindowUtils.errorAlert("Unable to fetch taxon information, please try again later.");
					//doSetCurrentAssessment(assessment);
				}
			});
		} else
			doSetCurrentAssessment(null, null);
	}
	
	private void doSetCurrentAssessment(final Taxon parent, final Assessment assessment) {
		if( assessment != null && assessment.getType().equals(AssessmentType.DRAFT_ASSESSMENT_TYPE) && 
				!AuthorizationCache.impl.hasRight(SISClientBase.currentUser, AuthorizableObject.READ, assessment) ) {
			WindowUtils.errorAlert("Insufficient Rights", "Sorry, you don't have rights to select this Draft assessment.");
			//StateManager.impl.setState(null, null, null);
		} else {
			//StateManager.impl.setAssessment(parent, assessment);

			if (assessment != null) {
				try {
					updateRecentAssessments();
				} catch (Throwable e) {
					GWT.log("Failed to update recent assessments", e);
				}
				StatusCache.impl.checkStatus(assessment, true, new GenericCallback<Integer>() {
					public void onFailure(Throwable caught) {
						// Nothing to do, really.
					}

					public void onSuccess(Integer result) {
						// Nothing to do, really.
					}
				});
			}
		}

		FieldWidgetCache.impl.resetWidgetContents();
		SISClientBase.getInstance().onAssessmentChanged();
	}*/

	public void updateRecentAssessments() {
		RecentlyAccessedCache.impl.add(RecentlyAccessed.ASSESSMENT, 
			new RecentlyAccessedCache.RecentAssessment(getCurrentAssessment())
		);
	}
	
}
