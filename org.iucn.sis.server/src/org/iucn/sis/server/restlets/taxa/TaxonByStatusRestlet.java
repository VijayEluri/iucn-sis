package org.iucn.sis.server.restlets.taxa;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.naming.NamingException;

import org.iucn.sis.server.api.application.SIS;
import org.iucn.sis.server.api.restlets.ServiceRestlet;
import org.iucn.sis.shared.api.models.AssessmentFilter;
import org.iucn.sis.shared.api.models.Taxon;
import org.iucn.sis.shared.api.models.TaxonStatus;
import org.iucn.sis.shared.api.models.User;
import org.iucn.sis.shared.api.models.WorkingSet;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.xml.DomRepresentation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.solertium.db.CanonicalColumnName;
import com.solertium.db.DBException;
import com.solertium.db.ExecutionContext;
import com.solertium.db.Row;
import com.solertium.db.SystemExecutionContext;
import com.solertium.db.query.QComparisonConstraint;
import com.solertium.db.query.QConstraint;
import com.solertium.db.query.QConstraintGroup;
import com.solertium.db.query.SelectQuery;

/**
 * Returns the taxon with the given status.
 * 
 * @author liz.schwartz
 * 
 */
public class TaxonByStatusRestlet extends ServiceRestlet {

	// protected LinkedHashMap<String, String> nameToID;

	public TaxonByStatusRestlet(String path, Context context) {
		super(path, context);
		// nameToID = new LinkedHashMap<String, String>();
	}

	/**
	 * Creates a private working set based on new status TODO: COULD BE EXTENDED
	 * HERE TO TAKE DIFFERENT STATUS
	 * 
	 * @param request
	 * @param response
	 */
	protected void createWorkingSet(Request request, Response response) {
		final String username = SIS.get().getUsername(request);
		final User user = SIS.get().getUserIO().getUserFromUsername(username);
		// final StringBuilder workingSetXML = new StringBuilder("<xml>\r\n");
		final LinkedHashMap<String, String> idsToName = getTaxaIDsToNameResults(TaxonStatus.STATUS_NEW);
		final Date today = new Date();
		final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		final String output = formatter.format(today);

		AssessmentFilter filter = new AssessmentFilter();
		WorkingSet workingSet = new WorkingSet();
		workingSet.setName("New taxa");
		workingSet.setDescription("Taxa with new status as of " + output);
		workingSet.setCreatedDate(new Date());
		workingSet.setCreator(username);
		workingSet.setTaxon(new HashSet<Taxon>());
		workingSet.setUsers(new HashSet<User>());
		workingSet.getUsers().add(user);
		for (Entry<String,String> entry : idsToName.entrySet()) {
			workingSet.getTaxon().add(SIS.get().getTaxonIO().getTaxon(Integer.valueOf(entry.getKey())));
		}
		workingSet.setFilter(filter);


		// SUCCESSFULLY CREATED WORKINGSET
		if (SIS.get().getWorkingSetIO().saveWorkingSet(workingSet, user)) {
			response.setStatus(Status.SUCCESS_OK);
			response.setEntity("Working set was successfully created", MediaType.TEXT_PLAIN);
		}
		// WAS UNABLE TO CREATE WORKING SET
		else {
			response.setStatus(Status.SERVER_ERROR_INTERNAL);
			response.setEntity("There was a server error when trying to create the working set", MediaType.TEXT_PLAIN);

		}
	}

	@Override
	public void definePaths() {
		paths.add("/taxaFinder");
		paths.add("/taxaFinder/workingSet");
	}

	/**
	 * If want to add caching, so that the search doesn't happen every time,
	 * should add it here, also make sure that the getResult function stores a
	 * result to read from.
	 * 
	 * @param request
	 * @param response
	 */
	protected void getCachedResult(Request request, Response response) {
		// TODO IF NEEDED
	}

	/**
	 * Finds the most recent "new" taxa ... can be extended later to find taxa
	 * of any status. TODO: WHEN CHANGING THIS TO CACHING TAXA
	 * 
	 * @param request
	 * @param response
	 */
	protected void getResult(Request request, Response response) {
		final Document doc;
		final int offset;
		final int limit;
		try {
			doc = new DomRepresentation(request.getEntity()).getDocument();
		} catch (IOException e1) {
			e1.printStackTrace();
			response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		}

		Element docElement = doc.getDocumentElement();
		try {
			limit = Integer.parseInt(docElement.getElementsByTagName("limit").item(0).getTextContent());
			offset = Integer.parseInt(docElement.getElementsByTagName("offset").item(0).getTextContent());
		} catch (NullPointerException e) {
			e.printStackTrace();
			response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		} catch (NumberFormatException e) {
			e.printStackTrace();
			response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		}

		ExecutionContext ec;
		try {
			ec = new SystemExecutionContext("default");
		} catch (NamingException e) {
			e.printStackTrace();
			response.setStatus(Status.SERVER_ERROR_INTERNAL);
			return;
		}
		ec.setExecutionLevel(ExecutionContext.READ_ONLY);

		SelectQuery selectQuery = new SelectQuery();
		selectQuery.select("TAXONKEYS", "NODE_ID");
		selectQuery.select("TAXONKEYS", "SCI_NAME", "ASC");
		selectQuery.constrain(new CanonicalColumnName("TAXONKEYS", "STATUS"), QConstraint.CT_EQUALS, "NEW");
		selectQuery.constrain(QConstraint.CG_OR, new CanonicalColumnName("TAXONKEYS", "STATUS"), QConstraint.CT_EQUALS,
				"N");

		Row.Set rowSet = new Row.Set();
		try {
			ec.doQuery(selectQuery, rowSet);
		} catch (DBException e) {
			e.printStackTrace();
			response.setStatus(Status.SERVER_ERROR_INTERNAL);
			return;
		}

		List<Row> rows = rowSet.getSet();
		int start = offset;
		int end = offset + limit;
		if (rows.size() < end) {
			end = rows.size();
		}
		if (start >= rows.size()) {
			response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		}

		StringBuilder xml = new StringBuilder("<xml>\r\n");
		xml.append("<totalNumOfResults>" + rows.size() + "</totalNumOfResults>\r\n");
		for (Row row : rows.subList(start, end)) {
			xml.append("<result id=\"" + row.get("NODE_ID").toString() + "\">" + row.get("SCI_NAME").toString()
					+ "</result>\r\n");
		}
		xml.append("</xml>\r\n");

		response.setEntity(xml.toString(), MediaType.TEXT_XML);
		response.setStatus(Status.SUCCESS_OK);
	}

	/**
	 * Given a status finds returns a linked hashmap of all ids to names of taxa
	 * with the given status, and limits it only to Species an lower
	 * 
	 * @param status
	 * @return
	 */
	protected LinkedHashMap<String, String> getTaxaIDsToNameResults(String status) {
		LinkedHashMap<String, String> taxaIDsToName = new LinkedHashMap<String, String>();

		if (status.equals(TaxonStatus.STATUS_NEW)) {
			ExecutionContext ec;
			try {
				ec = new SystemExecutionContext("default");
			} catch (NamingException e) {
				e.printStackTrace();
				return null;
			}
			ec.setExecutionLevel(ExecutionContext.SQL_ALLOWED);

			SelectQuery selectQuery = new SelectQuery();
			selectQuery.select("TAXONKEYS", "NODE_ID");
			selectQuery.select("TAXONKEYS", "SCI_NAME", "ASC");
			QConstraintGroup orGroup = new QConstraintGroup();
			orGroup.addConstraint(QConstraint.CG_OR, new QComparisonConstraint(new CanonicalColumnName("TAXONKEYS",
					"STATUS"), QConstraint.CT_EQUALS, "NEW"));
			orGroup.addConstraint(QConstraint.CG_OR, new QComparisonConstraint(new CanonicalColumnName("TAXONKEYS",
					"STATUS"), QConstraint.CT_EQUALS, "N"));
			selectQuery.constrain(QConstraint.CG_AND, new CanonicalColumnName("TAXONKEYS", "LEVEL"), QConstraint.CT_GT,
					"5");
			selectQuery.constrain(orGroup);

			Row.Set rowSet = new Row.Set();
			try {
				ec.doQuery(selectQuery, rowSet);
			} catch (DBException e) {
				e.printStackTrace();
				return null;
			}

			List<Row> rows = rowSet.getSet();
			for (Row row : rows) {
				taxaIDsToName.put(row.get("NODE_ID").toString(), row.get("SCI_NAME").toString());
			}
		} else {
			return null;
		}
		return taxaIDsToName;
	}

	@Override
	public void performService(Request request, Response response) {
		if (request.getMethod().equals(Method.POST)) {
			if (request.getResourceRef().getPath().endsWith("/workingSet")) {
				createWorkingSet(request, response);
			} else {
				getResult(request, response);
			}
		} else {
			response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		}

	}

	protected void replaceResults(LinkedHashMap<String, String> idToName) {
		// TODO:
	}

}
