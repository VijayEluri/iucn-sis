package org.iucn.sis.server.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.iucn.sis.server.api.persistance.SISPersistentManager;
import org.iucn.sis.server.api.persistance.hibernate.PersistentException;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.AssessmentChange;
import org.iucn.sis.shared.api.models.Edit;
import org.iucn.sis.shared.api.models.Field;
import org.iucn.sis.shared.api.models.Notes;
import org.iucn.sis.shared.api.models.PrimitiveField;
import org.iucn.sis.shared.api.models.Reference;

import com.solertium.util.events.ComplexListener;

public class AssessmentPersistence {
	
	private final List<AssessmentChange> changeSet;
	private final Session session;
	private final Assessment target;
	
	private ComplexListener<Field> deleteFieldListener;
	private ComplexListener<PrimitiveField<?>> deletePrimitiveFieldListener;
	
	private boolean allowAdd = true;
	private boolean allowDelete = true;
	private boolean allowManageReferences = true;
	private boolean allowManageNotes = true;
	private boolean allowForeignData = false;
	
	public AssessmentPersistence(Session session, Assessment target) {
		this.session = session;
		this.target = target;
		this.changeSet = new ArrayList<AssessmentChange>();
	}
	
	public void addChange(AssessmentChange change) {
		changeSet.add(change);
	}
	
	public List<AssessmentChange> getChangeSet() {
		return changeSet;
	}
	
	public void setDeleteFieldListener(ComplexListener<Field> deleteFieldListener) {
		this.deleteFieldListener = deleteFieldListener;
	}
	
	public void setDeletePrimitiveFieldListener(ComplexListener<PrimitiveField<?>> deletePrimitiveFieldListener) {
		this.deletePrimitiveFieldListener = deletePrimitiveFieldListener;
	}
	
	public void setAllowAdd(boolean allowAdd) {
		this.allowAdd = allowAdd;
	}
	
	public void setAllowDelete(boolean allowDelete) {
		this.allowDelete = allowDelete;
	}
	
	public void setAllowManageReferences(boolean allowManageReferences) {
		this.allowManageReferences = allowManageReferences;
	}
	
	public void setAllowManageNotes(boolean allowManageNotes) {
		this.allowManageNotes = allowManageNotes;
	}
	
	public void setAllowForeignData(boolean allowForeignData) {
		this.allowForeignData = allowForeignData;
	}
	
	public void sink(Assessment source) throws PersistentException {
		sink(new HashSet<Field>(source.getField()));
	}
	
	public void sink(Set<Field> sourceFields) throws PersistentException {
		Map<Integer, Field> existingFieldsByID = mapFields(target.getField());
		Map<String, Field> existingFieldsByName = new HashMap<String, Field>();
		for (Field field : target.getField())
			existingFieldsByName.put(field.getName(), field);
			
		for (Field sourceField : sourceFields) {
			if (sourceField.getId() == 0) {
				if (allowAdd) {
					sourceField.setAssessment(target);
					//FieldDAO.save(sourceField);
					target.getField().add(sourceField);
					changeSet.add(createAddChange(sourceField));
				}
			}
			else if (!existingFieldsByName.containsKey(sourceField.getName())) {
				if (allowAdd && allowForeignData) {
					//Field is new to this assessment, create a copy..
					
					Field copy = sourceField.deepCopy(false);
					copy.setAssessment(target);
					copy.setReference(new HashSet<Reference>(sourceField.getReference()));
					
					target.getField().add(copy);
					changeSet.add(createAddChange(copy));
				}
			}
			else {
				Field targetField = existingFieldsByID.remove(sourceField.getId());
				if (targetField == null && allowForeignData) {
					targetField = existingFieldsByName.remove(sourceField.getName());
					if (targetField != null)
						existingFieldsByID.remove(targetField.getId());
				}
				
				if (targetField != null) {
					AssessmentChange pendingEdit = createEditChange(targetField, sourceField);
					AssessmentChange pendingDelete = createDeleteChange(targetField);
					sink(sourceField, targetField);
					if (isBlank(targetField)) {
						if (allowDelete) {
							changeSet.add(pendingDelete);
							deleteField(targetField);
						}
					}
					else
						changeSet.add(pendingEdit);
				}
			}
		}
		
		/*
		 * Only delete top-level fields
		 */
		if (allowDelete) {
			for (Field field : existingFieldsByID.values()) {
				if (field.getParent() == null) {
					changeSet.add(createDeleteChange(field));
					deleteField(field);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void sink(Field source, Field target) throws PersistentException {
		{
			if (allowManageReferences) {
				Map<Integer, Reference> existingReferences = mapFields(target.getReference());
			
				for (Reference sourceReference : source.getReference()) {
					//Should never be the case...
					if (sourceReference.getId() == 0) {
						sourceReference.getField().add(target);
						target.getReference().add(sourceReference);
					}
					else {
						Reference targetReference = existingReferences.remove(sourceReference.getId());
						if (targetReference == null) {
							targetReference = findReference(sourceReference);
							targetReference.getField().add(target);
							target.getReference().add(targetReference);
						}
					}
				}
				
				target.getReference().removeAll(existingReferences.values());
			}
			
			if (allowManageNotes) {
				Map<Integer, Notes> existingNotes = mapFields(target.getNotes());
				
				for (Notes sourceNotes : source.getNotes()) {
					//Should never be the case...
					if (sourceNotes.getId() == 0) {
						sourceNotes.setField(target);
						target.getNotes().add(sourceNotes);
					}
					else {
						Notes targetNotes = existingNotes.remove(sourceNotes.getId());
						if (targetNotes == null) {
							targetNotes = findNote(sourceNotes);
							targetNotes.setField(target);
							target.getNotes().add(targetNotes);
							session.update(targetNotes);
						}
					}
				}
				
				target.getNotes().removeAll(existingNotes.values());
			}
		}
		{
			/*
			 * Map by name since the equals method here is based on name...
			 */
			Map<String, PrimitiveField> existingFields = new HashMap<String, PrimitiveField>();
			for (PrimitiveField<?> field : target.getPrimitiveField())
				existingFields.put(field.getName(), field);
			
			for (PrimitiveField sourceField : source.getPrimitiveField()) {
				PrimitiveField targetField = existingFields.remove(sourceField.getName());
				if (targetField == null) {
					sourceField.setId(0); //Should already be 0, but...
					sourceField.setField(target);
					//PrimitiveFieldDAO.save(sourceField);
					target.getPrimitiveField().add(sourceField);
				}
				else {
					targetField.setRawValue(sourceField.getRawValue());
				}
			}
			
			for (PrimitiveField field : existingFields.values())
				deletePrimitiveField(field);
		}
		{
			Map<Integer, Field> existingFields = mapFields(target.getFields());
			
			for (Field sourceField : source.getFields()) {
				if (sourceField.getId() == 0) {
					sourceField.setParent(target);
					//FieldDAO.save(sourceField);
					target.getFields().add(sourceField);
				}
				else if (!existingFields.containsKey(sourceField.getId())) {
					if (allowForeignData) {
						Field copy = sourceField.deepCopy(false);
						copy.setReference(sourceField.getReference());
						
						target.getFields().add(copy);
					}
				}
				else {
					Field targetField = existingFields.remove(sourceField.getId());
					if (targetField != null)
						sink(sourceField, targetField);
				}
			}
			
			for (Field field : existingFields.values()) {
				Debug.println("Deleting existing field {0}: {1}", field.getId(), field.getName());
				deleteField(field);
			}
		}
		
		//FieldDAO.save(target);
	}
	
	protected Notes findNote(Notes sourceNote) throws PersistentException {
		return SISPersistentManager.instance().loadObject(session, Notes.class, sourceNote.getId());
	}
	
	protected Reference findReference(Reference sourceReference) throws PersistentException {
		return SISPersistentManager.instance().loadObject(session, Reference.class, sourceReference.getId());
	}
	
	private boolean isBlank(Field field) {
		return field.getReference().isEmpty() && field.getNotes().isEmpty() && !field.hasData();
	}
	
	private void deleteField(Field field) {
		if (deleteFieldListener != null)
			deleteFieldListener.handleEvent(field);
	}
	
	private void deletePrimitiveField(PrimitiveField<?> field) {
		if (deletePrimitiveFieldListener != null)
			deletePrimitiveFieldListener.handleEvent(field);
	}
	
	public AssessmentChange createAddChange(Field newField) {
		AssessmentChange change = new AssessmentChange();
		change.setAssessment(target);
		change.setFieldName(newField.getName());
		change.setOldField(null);
		change.setNewField(deepCopy(newField));
		change.setType(AssessmentChange.ADD);
		
		return change;
	}
	
	public AssessmentChange createDeleteChange(Field removedField) {
		AssessmentChange change = new AssessmentChange();
		change.setAssessment(target);
		change.setFieldName(removedField.getName());
		change.setOldField(deepCopy(removedField));
		change.setNewField(null);
		change.setType(AssessmentChange.DELETE);
		
		return change;
	}
	
	public AssessmentChange createEditChange(Field oldField, Field newField) {
		AssessmentChange change = new AssessmentChange();
		change.setAssessment(target);
		change.setFieldName(oldField.getName());
		change.setOldField(deepCopy(oldField));
		change.setNewField(deepCopy(newField));
		change.setType(AssessmentChange.EDIT);
		
		return change;
	}
	
	private Field deepCopy(Field source) {
		Field target = new Field(source.getName(), null);
		for (PrimitiveField<?> prim : source.getPrimitiveField()) {
			PrimitiveField<?> copy = prim.deepCopy(false);
			copy.setField(target);
			target.getPrimitiveField().add(copy);
		}
		for (Field child : source.getFields()) {
			Field copy = deepCopy(child);
			copy.setParent(target);
			target.getFields().add(copy);
		}
		return target;
	}
	
	@SuppressWarnings("unchecked")
	private <X> Map<Integer, X> mapFields(Collection<X> fields) {
		Map<Integer, X> map = new HashMap<Integer, X>();
		for (X field : fields) {
			if (field instanceof Field) 
				map.put(((Field)field).getId(), field);
			else if (field instanceof PrimitiveField)
				map.put(((PrimitiveField)field).getId(), field);
			else if (field instanceof Reference)
				map.put(((Reference)field).getId(), field);
			else if (field instanceof Notes)
				map.put(((Notes)field).getId(), field);
		}
		return map;
	}
	
	public void saveChanges(Assessment assessment, Edit edit) {
		saveChanges(assessment, edit, SISPersistentManager.instance());
	}
	
	public void saveChanges(Assessment assessment, Edit edit, SISPersistentManager targetManager) {
		ChangeTracker tracker = new ChangeTracker(assessment.getId(), edit.getId(), getChangeSet(), targetManager);
		new Thread(tracker).start();
	}
	
	public static class ChangeTracker implements Runnable {
	
		private Integer assessmentID, editID;
		private List<AssessmentChange> changes;
		private SISPersistentManager targetManager;
		
		public ChangeTracker(Integer assessmentID, Integer editID, List<AssessmentChange> changes, SISPersistentManager targetManager) {
			this.assessmentID = assessmentID;
			this.editID = editID;
			this.changes = changes;
			this.targetManager = targetManager;
		}
		
		@Override
		public void run() {
			try {
				execute();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		public void execute() throws Exception {
			Session session = targetManager.openSession();
			session.beginTransaction();
			
			Debug.println("Saving change for assessment {0} with edit {1}", assessmentID, editID);
			
			Assessment assessment = targetManager.getObject(session, Assessment.class, assessmentID);
			Edit edit = getEdit(session, editID);
			
			for (AssessmentChange change : changes) {
				if (AssessmentChange.EDIT == change.getType()) {
					String oldXML = change.getOldField().toXML();
					String newXML = change.getNewField().toXML();
					
					if (oldXML.equals(newXML))
						continue;
				}
				change.setAssessment(assessment);
				change.setEdit(edit);
				
				if (change.getOldField() != null)
					session.save(change.getOldField());
				if (change.getNewField() != null)
					session.save(change.getNewField());
				
				session.save(change);
			}
			
			session.getTransaction().commit();
		}
		
		private Edit getEdit(Session session, Integer id) throws Exception {
			Edit edit = null;
			int tries = 0, max = 20;
			while (edit == null) {
				Thread.sleep(5000);
				edit = SISPersistentManager.instance().getObject(session, Edit.class, editID);
				if (++tries > max)
					break;
			}
			if (edit == null)
				throw new Exception("Taking too long to get edit " + id);
			return edit;
		}
		
	}

}
