package org.iucn.sis.server.extensions.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.iucn.sis.server.api.application.SIS;
import org.iucn.sis.server.api.io.WorkingSetIO;
import org.iucn.sis.server.extensions.integrity.IntegrityValidator;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.AssessmentIntegrityValidation;
import org.iucn.sis.shared.api.models.WorkingSet;
import org.iucn.sis.shared.api.workflow.WorkflowStatus;
import org.iucn.sis.shared.api.workflow.WorkflowUserInfo;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.solertium.db.CanonicalColumnName;
import com.solertium.db.DBException;
import com.solertium.db.query.QConstraint;
import com.solertium.db.query.SelectQuery;
import com.solertium.util.BaseDocumentUtils;
import com.solertium.util.NodeCollection;

@SuppressWarnings("deprecation")
public class WorkflowManagementResource extends WFDBResource {
	
	private final String workingSet;

	public WorkflowManagementResource(Context context, Request request, Response response) {
		super(context, request, response);
		setModifiable(true);
		workingSet = (String)request.getAttributes().get("working-set");
	}
	
	@Override
	public Representation represent(Variant variant, Session session) throws ResourceException {
		final SelectQuery query = new SelectQuery();
		query.select(WorkflowConstants.WORKFLOW_TABLE, "*");
		query.constrain(new CanonicalColumnName(WorkflowConstants.WORKFLOW_TABLE, "workingsetid"), 
			QConstraint.CT_EQUALS, workingSet	
		);
		
		return getRowsAsRepresentation(query);
	}
	
	/**
	 * <root>
	 * 	<user>
	 * 		<name>...</email>
	 * 		<email>...</email>
	 *  </user>
	 * 	<status>...</status>
	 * 	<comment>...</comment>
	 *  <scope>...</scope>
	 *  <notify>
	 *  	<name>...</name>
	 *  	<email>...</email>
	 *  </notify>
	 * </root>
	 */
	@Override
	public void acceptRepresentation(Representation entity, Session session) throws ResourceException {
		final WorkflowManager manager = new WorkflowManager(session);
		final Document document;
		try {
			document = new DomRepresentation(entity).getDocument();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ResourceException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, e);
		}
		
		WorkflowUserInfo user = null;
		String status = null, commentText = null, scope = null;
		final List<WorkflowUserInfo> notify = new ArrayList<WorkflowUserInfo>();
		
		final NodeCollection nodes = new NodeCollection(document.getDocumentElement().getChildNodes());
		for (Node node : nodes) {
			if ("user".equals(node.getNodeName()))
				user = parseUserInfo(node);
			else if ("status".equals(node.getNodeName()))
				status = node.getTextContent();
			else if ("comment".equals(node.getNodeName()))
				commentText = node.getTextContent();
			else if ("scope".equals(node.getNodeName()))
				scope = node.getTextContent();
			else if ("notify".equals(node.getNodeName())) {
				WorkflowUserInfo info = parseUserInfo(node);
				if (info != null)
					notify.add(info);
			}
		}
		
		final WorkflowStatus proposed = WorkflowStatus.getStatus(status);
		final WorkflowComment comment = new WorkflowComment(user, commentText, scope);
		
		if (proposed == null) {
			try {
				manager.addComment(workingSet, comment);
			} catch (WorkflowManagerException e) {
				die(e);
				return;
			}		
		}
		else {
			/*
			 * 1. Run integrity check
			 */
//			final VFS vfs = ServerApplication.getStaticVFS();
			WorkingSetIO workingSetIO = new WorkingSetIO(session);
			final WorkingSet  data = workingSetIO.readWorkingSet(Integer.valueOf(workingSet));
			
			final Collection<Assessment> assessments = 
				WorkflowManager.getAllAssessments(session, data);
			
			if (assessments.isEmpty()) {
				die(new ResourceException(Status.CLIENT_ERROR_EXPECTATION_FAILED, "There are no assessments available in this working set."));
				return;
			}
			
			boolean success = "integrity".equals(comment.getComment());
			if (!success) {
				for (Assessment assessment : assessments) {
					try {
						success &= IntegrityValidator.
							validate_background(session, SIS.get().getVFS(), 
								ec, assessment.getId()
							) == AssessmentIntegrityValidation.SUCCESS;
					} catch (DBException e) {
						Debug.println(e);
						continue;
					}
				}
			}
			
			if (!success) {
				die(new ResourceException(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Integrity validation failed."));
				return;
			}
			
			try {
				manager.changeStatus(Integer.parseInt(workingSet), user, proposed, comment, notify);
			} catch (WorkflowManagerException e) {
				die(e);
				return;
			} catch (Exception e) {
				die(new WorkflowManagerException("Unexpected exception, please try again later.", Status.SERVER_ERROR_INTERNAL, e));
				return;
			}
		}
		
		getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
	}
	
	private WorkflowUserInfo parseUserInfo(Node parent) {
		Integer id = null;
		String username = null, displayName = null, email = null;
		final NodeCollection children = new NodeCollection(parent.getChildNodes());
		for (Node child : children) {
			if ("id".equals(child.getNodeName()))
				id = Integer.valueOf(child.getTextContent());
			else if ("username".equals(child.getNodeName()))
				username = child.getTextContent();
			else if ("displayname".equals(child.getNodeName()))
				displayName = child.getTextContent();
			else if ("email".equals(child.getNodeName()))
				email = child.getTextContent();
		}
		if (id != null && username != null && displayName != null && email != null)
			return new WorkflowUserInfo(id, username, displayName, email);
		else
			return null;
	}
	
	private void die(ResourceException e) {
		Debug.println(e);
		getResponse().setStatus(e.getStatus());
		getResponse().setEntity(new DomRepresentation(MediaType.TEXT_XML, 
			BaseDocumentUtils.impl.createErrorDocument(e.getMessage())));
	}

}
