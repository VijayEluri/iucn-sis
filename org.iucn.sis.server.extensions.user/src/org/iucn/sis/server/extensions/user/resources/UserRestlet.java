package org.iucn.sis.server.extensions.user.resources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.iucn.sis.server.api.application.SIS;
import org.iucn.sis.server.api.io.PermissionIO;
import org.iucn.sis.server.api.io.UserIO;
import org.iucn.sis.server.api.persistance.SISPersistentManager;
import org.iucn.sis.server.api.persistance.hibernate.PersistentException;
import org.iucn.sis.server.api.restlets.BaseServiceRestlet;
import org.iucn.sis.shared.api.models.PermissionGroup;
import org.iucn.sis.shared.api.models.User;
import org.iucn.sis.shared.api.models.UserPreference;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.lwxml.shared.NativeElement;
import com.solertium.lwxml.shared.NativeNode;
import com.solertium.lwxml.shared.NativeNodeList;
import com.solertium.util.TrivialExceptionHandler;

public class UserRestlet extends BaseServiceRestlet {
	
	public UserRestlet(Context context) {
		super(context);
	}
	
	@Override
	public void definePaths() {
		paths.add("/users/{username}/preferences");
		paths.add("/users/{username}");
		paths.add("/users");
	}

	private String getUsername(Request request) {
		return (String) request.getAttributes().get("username");
	}
	
	private void updatePreferences(Representation entity, Request request, Response response, Session session, User user) throws ResourceException {
		Map<String, UserPreference> targets = new HashMap<String, UserPreference>();
		for (UserPreference preference : user.getPreferences())
			targets.put(preference.getName(), preference);
		
		final UserIO io = new UserIO(session);
		final StringBuilder out = new StringBuilder();
		
		NativeDocument document = getEntityAsNativeDocument(entity);
		NativeNodeList nodes = document.getDocumentElement().getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			NativeNode node = nodes.item(i);
			if (UserPreference.ROOT_TAG.equals(node.getNodeName())) {
				UserPreference source = UserPreference.fromXML((NativeElement)node);
				UserPreference target = targets.get(source.getName());
				
				if (source.getId() == 0) {
					if (source.hasValue()) {
						source.setUser(user);
						session.save(source);
						targets.put(source.getName(), source);
					}
					else if (target != null) {
						io.deletePreference(target);
						targets.remove(source.getName());
					}
				}
				else {
					if (target == null) {
						//This is a problem, not dealing with it...
						continue;
					}
					else {
						target.setValue(source.getValue());
						session.update(target);
					}
				}
			}
		}
		
		out.append("<result>");
		for (UserPreference preference : targets.values())
			out.append(preference.toXML());
		out.append("</result>");
		
		response.setEntity(out.toString(), MediaType.TEXT_XML);
	}
	
	public void handlePost(Representation entity, Request request, Response response, Session session) throws ResourceException {
		String username = getUsername(request);
		if (username == null)
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Please provide a username");
		
		UserIO userIO = new UserIO(session);
		PermissionIO permissionIO = new PermissionIO(session);
		
		boolean isID = false;
		try {
			Integer.parseInt(username);
			isID = true;
		} catch (Exception e) {
			TrivialExceptionHandler.ignore(this, e);
		}
		
		final User user;
		
		if (isID) {
			try {
				user = SISPersistentManager.instance().getObject(session, User.class, Integer.valueOf(username));
			} catch (PersistentException e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
			}
		}
		else
			user = userIO.getUserFromUsername(username);
		
		if (user == null)
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, "User not found.");
		
		if (request.getResourceRef().getPath().endsWith("preferences")) {
			updatePreferences(entity, request, response, session, user);
			return;
		}
		
		Document doc = getEntityAsDocument(entity);
		
		NodeList fields = doc.getElementsByTagName("field");
		for (int i = 0; i < fields.getLength(); i++) {
			Element field = (Element) fields.item(i);
			String name = field.getAttribute("name");
			String value = field.getTextContent();
			if ("firstname".equalsIgnoreCase(name))
				user.setFirstName(value);
			else if ("lastname".equalsIgnoreCase(name))
				user.setLastName(value);
			else if ("username".equalsIgnoreCase(name)) {
				if (!username.equals(value)) {
					/*
					 * They want to change the username... check if this 
					 * user is active first; if they are disabled, we 
					 * don't care what they do...
					 */
					if (user.getState() == User.ACTIVE) {
						User existing = userIO.getUserFromUsername(value);
						if (existing != null)
							throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, "An active user with this username already exists.");
					}
					
					// Remove old username from the LoginCache
					SIS.get().getLoginCache().remove(user.getUsername(), true);
					
					user.setUsername(value);
					if (user.getEmail() == null || username.equals(user.getEmail()))
						user.setEmail(value);
				}
			}
			else if ("nickname".equalsIgnoreCase(name))
				user.setNickname(value);
			else if ("initials".equalsIgnoreCase(name))
				user.setInitials(value);
			else if ("affiliation".equalsIgnoreCase(name))
				user.setAffiliation(value);
			else if ("sisUser".equalsIgnoreCase(name) || "sis".equalsIgnoreCase(name))
				user.setSisUser(Boolean.valueOf(value));
			else if ("rapidListUser".equalsIgnoreCase(name))
				user.setRapidlistUser(Boolean.valueOf(value));
			else if ("quickgroup".equalsIgnoreCase(name)){
				user.getPermissionGroups().clear();
				for (String permName : value.split(",")) {
					try {
						PermissionGroup group = permissionIO.getPermissionGroup(permName);
						if (group != null)
							user.getPermissionGroups().add(group);
						else {
							response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
							response.setEntity("The permission group " + permName + " does not exist.", MediaType.TEXT_PLAIN);
							return;
						}
					} catch (PersistentException e) {
						throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
					}
				}
			}
			else if ("state".equalsIgnoreCase(name)) {
				Integer state = null;
				try {
					state = Integer.valueOf(value);
				} catch (Exception e) {
					TrivialExceptionHandler.ignore(this, e);
				}
				
				if (state == null || !(state.intValue() == User.ACTIVE || state.intValue() == User.DELETED)) {
					response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					response.setEntity("Invalid user state specified.", MediaType.TEXT_PLAIN);
					return;
				}
				
				if (state.intValue() == User.ACTIVE && user.getState() == User.DELETED) {
					/*
					 * Ensure this is legal!
					 */
					User existing = userIO.getUserFromUsername(user.getUsername());
					if (existing != null)
						throw new ResourceException(Status.CLIENT_ERROR_CONFLICT, "An active user with this username already exists.");
				}
				
				user.setState(state);				
			}
			else {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "The property " + name + " is invalid.");
			}
			
			// Set Offline status to true if User created Offline
			user.setOfflineStatus(!SIS.amIOnline());
			
			try {
				if (userIO.saveUser(user))
					response.setStatus(Status.SUCCESS_OK);
				else
					throw new ResourceException(Status.SERVER_ERROR_INTERNAL);
			} catch (PersistentException e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Representation handleGet(Request request, Response response, Session session) throws ResourceException {
		Criteria criteria = session.createCriteria(User.class);
		if (getUsername(request) != null)
			criteria.add(Restrictions.eq("username", getUsername(request)));
		
		String sort = request.getResourceRef().getQueryAsForm().getFirstValue("sort");
		if (sort != null && !"".equals(sort))
			criteria.addOrder(Order.asc(sort));
		
		List<User> list;
		try {
			list = criteria.list();
		} catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
		
		StringBuilder results = new StringBuilder("<xml>");
		for (User user : list)
			results.append(user.toFullXML());
		results.append("</xml>");
		
		return new StringRepresentation(results.toString(), MediaType.TEXT_XML);
		/*
		ExperimentalSelectQuery query = new ExperimentalSelectQuery();
		query.select("user", "*");
		query.select("user_permission", "permission_group_id");
		query.join("user_permission", new QRelationConstraint(new CanonicalColumnName("user", "id"),
				new CanonicalColumnName("user_permission", "user_id")));
		query.constrain(new QComparisonConstraint(new CanonicalColumnName("user", "state"), QConstraint.CT_EQUALS,
				User.ACTIVE));
		if (getUsername() != null) {
			query.constrain(new QComparisonConstraint(new CanonicalColumnName("user", "username"),
					QConstraint.CT_EQUALS, getUsername()));
		}

		String queryString = query.getSQL(SIS.get().getExecutionContext().getDBSession());
		queryString = queryString.replaceAll("JOIN", "LEFT JOIN");

		final Map<Integer, User> idToUsers = new HashMap<Integer, User>();
		try {
			SIS.get().getExecutionContext().doQuery(queryString, new RowProcessor() {

				@Override
				public void process(Row row) {
					PermissionGroup group = new PermissionGroup();
					group.setID(row.get("permission_group_id").getInteger());

					if (idToUsers.containsKey(group.getId())) {
						if (group.getId() != 0)
							idToUsers.get(group.getId()).getPermissionGroups().add(group);
					} else {
						User user = new User();
						user.setId(row.get("id").getInteger());
						user.setUsername(row.get("username").getString());
						user.setFirstName(row.get("first_name").getString());
						user.setLastName(row.get("last_name").getString());
						user.setInitials(row.get("initials").getString());
						user.setAffiliation(row.get("affiliation").getString());
						user.setSisUser(row.get("sis_user").getInteger().equals(1));
						user.setRapidlistUser(row.get("rapidlist_user").getInteger().equals(1));
						user.setEmail(row.get("email").getString());
						if (group.getId() != 0)
							user.getPermissionGroups().add(group);
						idToUsers.put(user.getId(), user);
					}
				}
			});
		} catch (DBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		StringBuilder results = new StringBuilder("<xml>");
		for (User user : idToUsers.values()) {
			results.append(user.toFullXML());
		}
		results.append("</xml>");
		return new StringRepresentation(results.toString(), MediaType.TEXT_XML);
		*/
	}

}
