package org.iucn.sis.shared.api.citations;

import java.util.ArrayList;
import java.util.Set;

import org.iucn.sis.shared.api.models.Reference;

import com.solertium.lwxml.shared.GenericCallback;

public interface Referenceable {
	
	public enum ReferenceGroup {
		Assessment("assessment_reference", "assessmentid"), 
		Field("field_reference", "fieldid"),
		CommonName("common_name_reference", "common_nameid"),
		Synonym("synonym_reference", "synonymid"),
		Taxon("reference_taxon", "taxonid");
		
		private final String table, column;
		
		private ReferenceGroup(String table, String column) {
			this.table = table;
			this.column = column;
		}
		
		public String getColumn() {
			return column;
		}
		
		public String getTable() {
			return table;
		}
	}
	
	/**
	 * Invoked when addition of references has been instigated. The argument
	 * contains ReferenceUI objects that should be added to this object's List
	 * or Map of references, a persisting save then MIGHT occur, then the
	 * callback's onSuccess or onFailure method will be invoked, as appropriate.
	 * 
	 * @param references
	 * @param callback
	 */
	public void addReferences(ArrayList<Reference> references, GenericCallback<Object> callback);

	/**
	 * Returns this object's references in an unsorted ArrayList, duplicates
	 * definitely allowed (the ReferenceViewPanel dedupes based on this list).
	 * 
	 * @return ArrayList of ReferenceUI objects
	 */
	public Set<Reference> getReferencesAsList();

	/**
	 * Invoked when the references belonging to this Referenceable object have
	 * been changed; it should perform some persisting save, then invoke the
	 * callback's onSuccess or onFailure method, as appropriate.
	 * 
	 * @param callback
	 */
	public void onReferenceChanged(GenericCallback<Object> callback);

	/**
	 * Invoked when removal of references has been instigated. The argument
	 * contains ReferenceUI objects that should be removed from this object's
	 * List or Map of references, a persisting save then MIGHT occur, then the
	 * callback's onSuccess or onFailure method will be invoked, as appropriate.
	 * 
	 * @param references
	 * @param callback
	 */
	public void removeReferences(ArrayList<Reference> references, GenericCallback<Object> listener);
	
	/**
	 * Table to group these references by.
	 * @return
	 */
	public ReferenceGroup groupBy();
}
