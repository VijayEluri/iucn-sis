package org.iucn.sis.client.api.assessment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.iucn.sis.client.api.caches.AssessmentCache;
import org.iucn.sis.client.api.caches.AuthorizationCache;
import org.iucn.sis.client.api.caches.StatusCache;
import org.iucn.sis.client.api.caches.ViewCache;
import org.iucn.sis.client.api.container.SISClientBase;
import org.iucn.sis.client.api.utils.UriBase;
import org.iucn.sis.shared.api.acl.InsufficientRightsException;
import org.iucn.sis.shared.api.acl.base.AuthorizableObject;
import org.iucn.sis.shared.api.criteriacalculator.ExpertUtils;
import org.iucn.sis.shared.api.criteriacalculator.Factors;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.displays.Display;
import org.iucn.sis.shared.api.io.AssessmentChangePacket;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.Field;
import org.iucn.sis.shared.api.models.PrimitiveField;
import org.iucn.sis.shared.api.utils.CanonicalNames;

import com.extjs.gxt.ui.client.widget.Info;
import com.google.gwt.core.client.GWT;
import com.solertium.lwxml.shared.GWTResponseException;
import com.solertium.lwxml.shared.GenericCallback;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.util.events.SimpleListener;
import com.solertium.util.extjs.client.WindowUtils;

public class AssessmentClientSaveUtils {

	/**
	 * Tries to save the assessment passed as an argument. If the assessmentToSave parameter is null
	 * it will try to pull the current assessment to save.
	 * 
	 * @param assessmentToSave
	 * @param callback
	 */
	public static void saveAssessment(final List<Display> fieldWidgets, final Assessment assessmentToSave, final GenericCallback<AssessmentChangePacket> callback)
		throws InsufficientRightsException {
		
		AssessmentChangePacket packet = null;
		if (fieldWidgets != null)
			packet = saveWidgetDataToAssessment(fieldWidgets, assessmentToSave);
		
		saveAssessment(packet, assessmentToSave, callback);
	}
	
	public static void saveAssessment(final AssessmentChangePacket packet, final Assessment assessmentToSave, final GenericCallback<AssessmentChangePacket> callback)
			throws InsufficientRightsException {
		if (!AuthorizationCache.impl.hasRight(AuthorizableObject.WRITE, assessmentToSave))
			throw new InsufficientRightsException();

		StatusCache.impl.checkStatus(assessmentToSave, false, new GenericCallback<Integer>() {
			public void onFailure(Throwable caught) {
				// Nothing extra to print out here.
			}

			public void onSuccess(Integer result) {
				if (result == StatusCache.UNLOCKED || result == StatusCache.HAS_LOCK) {
					final NativeDocument ndoc = SISClientBase.getHttpBasicNativeDocument();
					
					if (packet == null)
						ndoc.post(UriBase.getInstance().getSISBase() + "/assessments", 
								assessmentToSave.toXML(), getSaveHandler(packet, assessmentToSave, ndoc, callback));
					else
						ndoc.post(UriBase.getInstance().getSISBase() + "/changes/assessments/" + assessmentToSave.getId(), 
								packet.toXML(), getSaveHandler(packet, assessmentToSave, ndoc, callback));
				} else {
					callback.onFailure(new Exception("Assessment locked or needs an update."));
				}
			}
		});
	}
	
	private static GenericCallback<String> getSaveHandler(final AssessmentChangePacket packet, final Assessment assessmentToSave, final NativeDocument ndoc, final GenericCallback<AssessmentChangePacket> callback) {
		return new GenericCallback<String>() {
			public void onFailure(Throwable caught) {
				if (caught instanceof GWTResponseException) {
					int code = ((GWTResponseException)caught).getCode();
					if (code == 409)
						callback.onFailure(new Exception("A draft assessment " +
							"with the specified regions already exists. Please modify " +
							"your choice of regions and try again."));
					else if (code == 403)
						callback.onFailure(new Exception("This assessment has " +
							"been locked since you opened it; no changes will " +
							"be saved.  Please try again in a few minutes."));
					else if (code == 423)
						callback.onFailure(new Exception("This assessment has " +
							"been locked since you opened it; no changes will " +
							"be saved."));
					else
						callback.onFailure(new Exception("An unknown error occurred that " +
							"prevented this assessment from being saved. Please try again later."));
				} else {
					callback.onFailure(new Exception("Please check your internet " +
					"connection and try again."));
				}
			}
			public void onSuccess(String result) {
				try {
					Assessment remote = Assessment.fromXML(ndoc);
					Assessment local = assessmentToSave;
					
					//Copy the edit trail
					local.setEdit(remote.getEdit());
					
					//Set the field IDs
					sink(remote, local);
				} catch (Throwable e) {
					Debug.println(e);
				}
					
				StatusCache.impl.setStatus(assessmentToSave, StatusCache.HAS_LOCK);
				callback.onSuccess(packet);
			}
		};
	}
	
	private static void sink(Assessment remote, Assessment local) {
		final Map<String, Field> localFields = mapByFieldName(local.getField());
		
		for (Field remoteField : remote.getField()) {
			Field localField;
			if (localFields.containsKey(remoteField.getName())) {
				localField = localFields.get(remoteField.getName());
				localField.setId(remoteField.getId());
				
				sink(remoteField, localField);
			}
			else {
				remoteField.setAssessment(local);
				local.getField().add(remoteField);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void sink(Field remoteField, Field localField) {
		for (PrimitiveField remotePrim : remoteField.getPrimitiveField()) {
			PrimitiveField localPrim = localField.getPrimitiveField(remotePrim.getName());
			if (localPrim == null) {
				remotePrim.setField(localField);
				localField.getPrimitiveField().add(remotePrim);
			}
			else {
				if (localPrim.getId() == null || localPrim.getId().intValue() == 0)
					localPrim.setId(remotePrim.getId());
			}
		}
		
		final Map<String, List<Field>> localSubfields = groupByFieldName(localField.getFields());
		for (Field remoteSubfield : sortFields(remoteField.getFields())) {
			if (localSubfields.containsKey(remoteSubfield.getName())) {
				List<Field> list = localSubfields.get(remoteSubfield.getName());
				if (!list.isEmpty()) {
					Field localSubfield = list.remove(0);
					localSubfield.setId(remoteSubfield.getId());
					
					sink(remoteSubfield, localSubfield);
				}
				else {
					Debug.println("Warning: Mis-count, empty field listing, manually adding field");
					remoteSubfield.setParent(localField);
					localField.getFields().add(remoteSubfield);
				}
			}
			else {
				remoteSubfield.setParent(localField);
				localField.getFields().add(remoteSubfield);
			}
		}
	}
	
	
	/**
	 * Compares the saved assessment to what is currently displayed in the field
	 * widgets. Returns whether or not the data needs to be saved.
	 * 
	 * @return boolean - save needed or not
	 */
	public static boolean shouldSaveCurrentAssessment(List<Display> fieldWidgets) {
		if (!AuthorizationCache.impl.hasRight(SISClientBase.currentUser, AuthorizableObject.WRITE, AssessmentCache.impl.getCurrentAssessment())) {
			return false;
		} else {
			for( Display cur : fieldWidgets ) {
				boolean hasChanged = cur.hasChanged();
				Debug.println("Field {0} has changed? {1}", cur.getCanonicalName(), hasChanged);
				if (hasChanged)
					return true;
			}
			return false;
		}
	}

	protected static AssessmentChangePacket saveWidgetDataToAssessment(final List<Display> fieldWidgets, final Assessment assessmentToSave) {
		Debug.println("Saving assessment...");
		
		boolean factorChanged = false;
		
		AssessmentChangePacket packet = new AssessmentChangePacket(assessmentToSave.getId());
		
		for (Display cur : fieldWidgets) {
			//Debug.println("Saving data to Field {0}: {1}", cur.getCanonicalName(), cur.getField().getPrimitiveField());
			if (cur.hasChanged()) {
				try {
					cur.save();
					
					Field curField = cur.getField();
					if (curField == null)
						Debug.println("Display {0} yielded a null field after save", cur.getCanonicalName());
					else { 
						if (!curField.hasData()) {
							if (curField.getId() != 0)
								packet.addDeletion(curField);
							Debug.println("+ Removing {0} with id {1} because it doesn't have any data", curField.getName(), curField.getId());
							assessmentToSave.getField().remove(assessmentToSave.getField(curField.getName()));
							//curField.reset();
							cur.setField(null);
						}
						else {
							Debug.println("+ Adding {0} with id {1}", curField.getName(), curField.getId());
							assessmentToSave.getField().add(curField);
							curField.setAssessment(assessmentToSave);
						
							packet.addChange(curField);
						}
					}
					
					if (Factors.isFactor(cur.getCanonicalName()) || CanonicalNames.RegionExpertQuestions.equals(cur.getCanonicalName()))
						factorChanged = true;
				} catch (Throwable e) {
					GWT.log("Failed to save display " + cur.getCanonicalName(), e);
					Debug.println(e);
				}
			}
		}
		
		if (factorChanged) {
			Debug.println("A factor was changed, re-analyzing criteria");
			ExpertUtils.processAssessment(assessmentToSave);
			
			packet.addChange(assessmentToSave.getField(CanonicalNames.RedListFuzzyResult));
			packet.addChange(assessmentToSave.getField(CanonicalNames.RedListCriteria));
		}
		
		return packet;
	}
	
	public static void saveIfNecessary(final SimpleListener listener) {
		if (AssessmentCache.impl.getCurrentAssessment() != null
				&& ViewCache.impl.getCurrentView() != null 
				&& ViewCache.impl.getCurrentView().getCurPage() != null
				&& shouldSaveCurrentAssessment(
						ViewCache.impl.getCurrentView().getCurPage().getMyFields())) {
			WindowUtils.confirmAlert("By the way...", "Navigating away from this page will"
					+ " revert unsaved changes. Would you like to save?", new WindowUtils.MessageBoxListener() {
				public void onYes() {
					try {
						saveAssessment(ViewCache.impl.getCurrentView().getCurPage().getMyFields(), 
								AssessmentCache.impl.getCurrentAssessment(), new GenericCallback<AssessmentChangePacket>() {
							public void onFailure(Throwable caught) {
								WindowUtils.errorAlert("Could not save, please try again later.");
							}
							public void onSuccess(AssessmentChangePacket arg0) {
								Info.display("Save Complete", "Successfully saved assessment {0}.",
										AssessmentCache.impl.getCurrentAssessment().getSpeciesName());
								listener.handleEvent();
							};
						});
					} catch (InsufficientRightsException e) {
						WindowUtils.errorAlert("Insufficient Permissions", "You do not have "
								+ "permission to modify this assessment. The changes you "
								+ "just made will not be saved.");
					}
				}
				@Override
				public void onNo() {
					listener.handleEvent();
				}
			});
		} else
			listener.handleEvent();
	}
	
	private static Map<String, Field> mapByFieldName(Collection<Field> fields) {
		Map<String, Field> map = new HashMap<String, Field>();
		for (Field field : fields)
			map.put(field.getName(), field);
		return map;
	}
	
	private static Map<String, List<Field>> groupByFieldName(Collection<Field> fields) {
		Map<String, List<Field>> map = new HashMap<String, List<Field>>();
		for (Field field : fields) {
			List<Field> list = map.get(field.getName());
			if (list == null) {
				list = new ArrayList<Field>();
				map.put(field.getName(), list);
			}
			list.add(field);
		}
		for (List<Field> values : map.values())
			Collections.sort(values, new Field.FieldNameComparator());
		
		return map;
	}
	
	private static List<Field> sortFields(Collection<Field> fields) {
		List<Field> list = new ArrayList<Field>(fields);
		Collections.sort(list, new Field.FieldNameComparator());
		return list;
	}
}
