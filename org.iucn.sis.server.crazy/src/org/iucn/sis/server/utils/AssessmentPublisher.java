package org.iucn.sis.server.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.iucn.sis.client.referenceui.ReferenceUI;
import org.iucn.sis.shared.BaseAssessment;
import org.iucn.sis.shared.acl.User;
import org.iucn.sis.shared.data.assessments.AssessmentData;
import org.iucn.sis.shared.data.assessments.CanonicalNames;

import com.solertium.db.DBException;
import com.solertium.db.ExecutionContext;
import com.solertium.db.Row;
import com.solertium.db.SystemExecutionContext;
import com.solertium.db.query.ExperimentalSelectQuery;

public class AssessmentPublisher {

	private Map<String, User> users;
	
	public AssessmentPublisher() throws NamingException {
		getUserInfo();
	}
	
	private void getUserInfo() throws NamingException {
		users = new HashMap<String, User>();
		
		SystemExecutionContext ec2 = new SystemExecutionContext("users");
		ec2.setExecutionLevel(ExecutionContext.ADMIN);
		ec2.setAPILevel(ExecutionContext.SQL_ALLOWED);
		
		final ExperimentalSelectQuery query = new ExperimentalSelectQuery();
		query.select("user", "*");
		query.select("profile", "firstname");
		query.select("profile", "lastname");
		query.select("profile", "initials");
		query.select("profile", "affiliation");
		
		final Row.Set rs = new Row.Set();
		try {
			ec2.doQuery(query, rs);
			
			for( Row curRow : rs.getSet() ) {
				String f = curRow.get("firstname").getString();
				String l = curRow.get("lastname").getString();
				String i = curRow.get("initials").getString();
				String id = curRow.get("id").getString();
				
				User user = new User();
				user.setFirstName(f);
				user.setLastName(l);
				user.setInitials(i);
				
				users.put(id, user);
			}
		} catch (DBException e) {
			e.printStackTrace();
		}
	}

	
	
	/**
	 * Performs the standard publication steps for draft assessments. 
	 * 
	 * @param data - assessment to be published, must be Draft type
	 * @param pubRef is the reference that will be attached to the RedListPublication field
	 * @return true if assessment parameter was successfully migrated to Published status
	 */
	public boolean publishAssessment(AssessmentData data, ReferenceUI pubRef) {
		
		if (data.getType().equals(BaseAssessment.DRAFT_ASSESSMENT_STATUS)) {
			data.setType(BaseAssessment.PUBLISHED_ASSESSMENT_STATUS);
			data.setDateFinalized(FormattedDate.impl.getDate());
			data.addReference(pubRef, CanonicalNames.RedListPublication);
			if( !data.getDataMap().containsKey(CanonicalNames.RedListPublication) )
				data.getDataMap().put(CanonicalNames.RedListPublication, new ArrayList<String>());
			
			putAuthorsIfNotNull(data);
			
			return true;
		} else
			return false;
	}
	
	private void putAuthorsIfNotNull(AssessmentData data) {
		ArrayList<String> arr;
		
		String curAuthors = data.getFirstDataPiece(CanonicalNames.RedListAssessmentAuthors, "");
		if( curAuthors.equals("") ) {
			arr = new ArrayList<String>();
			List<String> structures = (List<String>)data.getDataMap().get(CanonicalNames.RedListAssessors);
			
			if( structures != null && structures.size() > 0 ) {
				String s = structures.get(0);
				if( s == null || s.equals("") ) {
					List<User> userList = new ArrayList<User>();
					for (int i = 2; i < structures.size(); i++) { 
						//START AT 2 - index 1 is now just the total number of users...
						String curID = structures.get(i);
						if( !curID.equals("0") ) {
							if( users.containsKey(curID) )
								userList.add(users.get(curID));
							else
								System.out.println("Could not find user with ID " + curID);
						}
					}

					s = generateTextFromUsers(userList);
				}
				arr.add(s);
				data.getDataMap().put(CanonicalNames.RedListAssessmentAuthors, arr);
			}
		}
	}
	
	private String generateTextFromUsers(List<User> userList) {
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < userList.size(); i++) {
			text.append(userList.get(i).getCitationName());
			
			if (i + 1 < userList.size() - 1)
				text.append(", ");

			else if (i + 1 == userList.size() - 1)
				text.append(" & ");
		}
		
		return text.toString();
	}
}
