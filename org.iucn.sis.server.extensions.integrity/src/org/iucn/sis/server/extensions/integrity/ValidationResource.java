package org.iucn.sis.server.extensions.integrity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;

import javax.naming.NamingException;

import org.hibernate.Session;
import org.iucn.sis.server.api.filters.AssessmentFilterHelper;
import org.iucn.sis.server.api.io.AssessmentIO;
import org.iucn.sis.server.api.io.WorkingSetIO;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.AssessmentIntegrityValidation;
import org.iucn.sis.shared.api.models.AssessmentType;
import org.iucn.sis.shared.api.models.Taxon;
import org.iucn.sis.shared.api.models.WorkingSet;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.xml.DomRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.representation.Variant;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.solertium.db.DBException;
import com.solertium.db.DBSessionFactory;
import com.solertium.db.ExecutionContext;
import com.solertium.db.SQLDateHelper;
import com.solertium.db.SystemExecutionContext;
import com.solertium.util.BaseDocumentUtils;
import com.solertium.util.ElementCollection;
import com.solertium.util.NodeCollection;
import com.solertium.util.portable.XMLWritingUtils;
import com.solertium.vfs.NotFoundException;
import com.solertium.vfs.VFS;
import com.solertium.vfs.VFSPathToken;

/**
 * ValidationResource.java
 * 
 * POST to this resource to validate an assessment
 * 
 * @author <a href="mailto:carl.scott@solertium.com">Carl Scott</a>, <a
 *         href="http://www.solertium.com">Solertium Corporation</a>
 * 
 */
@SuppressWarnings("deprecation")
public class ValidationResource extends BaseIntegrityResource {
	
	private static final String DATE_NOW = "${date.now}";

	private final String rule;

	public ValidationResource(Context context, Request request,
			Response response) {
		super(context, request, response);
		setModifiable(true);

		rule = (String) request.getAttributes().get("rule");

		getVariants().add(new Variant(MediaType.TEXT_HTML));
	}
	
	protected ExecutionContext getExecutionContext() {
		try {
			SystemExecutionContext ec = new SystemExecutionContext(DBSessionFactory.getDBSession("integrity"));
			ec.setAPILevel(ExecutionContext.SQL_ALLOWED);
			ec.setExecutionLevel(ExecutionContext.READ_WRITE);
			
			return ec;
		} catch (NamingException e) {
			return null;
		}
	}
	
	public Representation represent(Variant variant, Session session) throws ResourceException {
		final Form form = getRequest().getResourceRef().getQueryAsForm();
		final Collection<AssessmentInfo> info = new ArrayList<AssessmentInfo>();
		
		if (form.getNames().contains("id")) {
			final String[] id = form.getValuesArray("id");
			final String[] type = form.getValuesArray("type");
			
			for (int i = 0; i < id.length; i++) {
				try {
					info.add(new AssessmentInfo(Integer.valueOf(id[i]), type[i]));
				} catch (IndexOutOfBoundsException e) {
					info.add(new AssessmentInfo(Integer.valueOf(id[i]), AssessmentType.PUBLISHED_ASSESSMENT_TYPE));
				}
			}
		}
		else if (form.getNames().contains("set")) {
			WorkingSetIO workingSetIO = new WorkingSetIO(session);
			final String[] set = form.getValuesArray("set");
			for (int i = 0; i < set.length; i++) {
				final WorkingSet data = workingSetIO.readWorkingSet(Integer.valueOf(set[i]));
				
				AssessmentFilterHelper helper = new AssessmentFilterHelper(session, data.getFilter());
				for (Taxon taxon  : data.getTaxon()) {
					for (Assessment assessment : helper.getAssessments(taxon.getId()) )
						if (!assessment.isPublished()) 
							info.add(new AssessmentInfo(assessment.getId(), assessment.getType()));
				}
			}
		} else
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid protocol");
		
		String html;
		try {
			html = BaseDocumentUtils.impl.serializeDocumentToString(
				handle(info, session), true, false
			);
		} catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
		
		StringBuilder body = new StringBuilder();
		body.append("<html>");
		body.append("<head>");
		body.append(XMLWritingUtils.writeTag("title", "Assessment Validation Results"));
		body.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"/integrity/styles.css\" />");
		body.append("</head>");
		body.append("<body>");
		body.append(html);
		body.append("</body>");
		body.append("</html>");
		
		return new StringRepresentation(body.toString(), variant.getMediaType());
	}

	/*
	 * <root> <assessment type="blah"> ... </assessment> <assessment type="blah"> ... </assessment> ...
	 * </root>
	 */
	public void acceptRepresentation(Representation entity, Session session) throws ResourceException {
		final Collection<AssessmentInfo> assessmentInfo = new HashSet<AssessmentInfo>(); {
			final Document document;
			try {
				document = new DomRepresentation(entity).getDocument();
			} catch (Exception e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
			}
			final NodeCollection nodes = new NodeCollection(document.getDocumentElement().getChildNodes());
			for (Node node : nodes)
				if ("assessment".equals(node.getNodeName()))
					assessmentInfo.add(new AssessmentInfo(Integer.valueOf(node.getTextContent()),BaseDocumentUtils.impl.getAttribute(node, "type")));
		}
		
		getResponse().setEntity(BaseDocumentUtils.impl.serializeDocumentToString(handle(assessmentInfo, session), true, true), 
				MediaType.TEXT_HTML);
	}
	
	protected Document handle(Collection<AssessmentInfo> assessmentInfo, Session session) throws ResourceException {
		//Only one rule is interrogated
		final Document ruleset = getRuleset();
		
		final Document response = BaseDocumentUtils.impl.newDocument();
		final Element root = response.createElement("div");
		root.setAttribute("class", "sis_integrity_single");

		for (AssessmentInfo info : assessmentInfo) {
			info.setName(getSpeciesName(info, session));
			
			if (!isAvailable(info))
				continue;
			
			try {
				runAssessment(session, rule, ruleset, response, info.getName(), info.getID(), info.getType(), root, true, true);
			} catch (ResourceException e) {
				e.printStackTrace();
				final Element failure = BaseDocumentUtils.impl
					.createElementWithText(response, "p", "Failed Due to Server Exception");
				failure.setAttribute("class", "sis_integrity_failure");
				root.appendChild(failure);
			}
		}

		response.appendChild(root);

		return response;
	}
	
	protected boolean runAssessment(final Session session, final String rulesetName, final Document ruleset, 
			final Document response, final String speciesName, final Integer assessmentID, 
			final String type, final Element root, boolean notifyOnSuccess, boolean attachHeader) throws ResourceException{
		final Element header = BaseDocumentUtils.impl
				.createElementWithText(response, "div", speciesName + " -- " + type.substring(0, type.indexOf("_")) + " -- ");
		header.setAttribute("class", "sis_integrity_header");
		
		if (!isAssessmentInDatabase(session, assessmentID)) {
			if (attachHeader)
				root.appendChild(header);
			final Element failure = BaseDocumentUtils.impl
					.createElementWithText(response, "p", "This assessment was not found");
			failure.setAttribute("class", "sis_integrity_failure");
			failure.setAttribute("id", rulesetName);
			root.appendChild(failure);
			return false;
		}
		
		final AssessmentIntegrityValidation resp;
		try {
			resp = IntegrityValidator.validate(session, ec, rulesetName, ruleset,
					assessmentID);
		} catch (DBException e) {
			e.printStackTrace();
			if (attachHeader)
				root.appendChild(header);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
		
		if (resp.isSuccess()) {
			if (notifyOnSuccess) {
				if (attachHeader)
					root.appendChild(header);
				final Element success = BaseDocumentUtils.impl
						.createElementWithText(response, "p",
								"This assessment is valid");
				success.setAttribute("class", "sis_integrity_success");
				success.setAttribute("id", rulesetName);
				root.appendChild(success);
			}
		} else {
			if (attachHeader)
				root.appendChild(header);
			String styleClass = resp.hasWarnings() ? "sis_integrity_warning" : "sis_integrity_failure";
			if (resp.getMessages().isEmpty()) {
				String prefix = resp.hasWarnings() ? "Failed with warnings" : "Failed";
				final Element failure = BaseDocumentUtils.impl
						.createElementWithText(response, "p", prefix + ", but could not determine exact condition.");
				failure.setAttribute("class", styleClass);
				failure.setAttribute("id", rulesetName);
				root.appendChild(failure);
			}
			else {
				for (String message : resp.getMessages()) {
					final Element failure = BaseDocumentUtils.impl
							.createElementWithText(response, "p", message);
					failure.setAttribute("class", styleClass);
					failure.setAttribute("id", rulesetName);
					root.appendChild(failure);
				}
			}
		}
		
		return resp.isSuccess();
	}
	
	private boolean isAssessmentInDatabase(Session session, Integer assessmentID) {
		return new AssessmentIO(session).getAssessment(assessmentID) != null;
	}
	
	private Document getRuleset() throws ResourceException {
		if (rule == null)
			throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
		
		return getRuleset(rule);
	}
	
	protected Document getRuleset(String rule) throws ResourceException {
		return getRuleset(rule, vfs);
	}
	
	public static Document getRuleset(String rule, VFS vfs) throws ResourceException {
		final Document ruleset;
		try {
			ruleset = vfs.getMutableDocument(ROOT_PATH.child(new VFSPathToken(
					rule)));
		} catch (IllegalArgumentException e) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e);
		} catch (NotFoundException e) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e);
		} catch (IOException e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
		
		//TODO: filter
		final SimpleDateFormat format = new SimpleDateFormat(SQLDateHelper.sqlDateFormat);
		final String dateNow = format.format(Calendar.getInstance().getTime());
		
		final ElementCollection collection = new ElementCollection(ruleset.getDocumentElement().getElementsByTagName("constraint"));
		for (Element el : collection) {
			if (DATE_NOW.equals(el.getAttribute("value")) && "com.solertium.db.query.QComparisonConstraint".equals(el.getAttribute("class"))) {
				el.setAttribute("value", dateNow);
				el.setAttribute("valueClass", "java.util.Date");
			}
		}
		
		//System.out.println(BaseDocumentUtils.impl.serializeDocumentToString(ruleset, true, true));
		
		return ruleset;
	}
	
	protected String getSpeciesName(AssessmentInfo info, Session session) {
		AssessmentIO assessmentIO = new AssessmentIO(session);
		Assessment assessment = assessmentIO.getAssessment(info.getID());
		if (assessment == null) {
			return info.getID()+"";
		} else
			return assessment.getSpeciesName();
	}
	
	protected boolean isAvailable(AssessmentInfo info) {
		return "true".equals(System.getProperty("HOSTED_MODE")) || !info.getID().equals(info.getName());
	}

	protected static class AssessmentInfo {
		
		private final String type;
		private String name;
		private final Integer id;
		
		public AssessmentInfo(Integer id, String type) {
			this.id = id;
			this.type = type;
			this.name = null;
		}
		
		public Integer getID() {
			return id;
		}
		
		public String getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
	
}
