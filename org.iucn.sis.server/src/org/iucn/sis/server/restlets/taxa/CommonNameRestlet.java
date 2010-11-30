package org.iucn.sis.server.restlets.taxa;

import java.util.HashSet;
import java.util.Set;

import org.iucn.sis.server.api.application.SIS;
import org.iucn.sis.server.api.persistance.CommonNameDAO;
import org.iucn.sis.server.api.persistance.SISPersistentManager;
import org.iucn.sis.server.api.persistance.hibernate.PersistentException;
import org.iucn.sis.server.api.restlets.BaseServiceRestlet;
import org.iucn.sis.server.api.utils.TaxomaticException;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.CommonName;
import org.iucn.sis.shared.api.models.Notes;
import org.iucn.sis.shared.api.models.Reference;
import org.iucn.sis.shared.api.models.Taxon;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;

import com.solertium.lwxml.java.JavaNativeDocument;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.lwxml.shared.NativeElement;
import com.solertium.lwxml.shared.NativeNodeList;

public class CommonNameRestlet extends BaseServiceRestlet {

	public CommonNameRestlet(Context context) {
		super(context);
	}

	@Override
	public void definePaths() {
		paths.add("/taxon/{taxon_id}/commonname/{id}/reference");
		paths.add("/taxon/{taxon_id}/commonname/{id}");
		paths.add("/taxon/{taxon_id}/commonname");
	}
	
	protected Integer getCommonNameID(Request request) throws ResourceException {
		try {
			return Integer.valueOf((String) request.getAttributes().get("id"));
		} catch (NumberFormatException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e);
		} catch (NullPointerException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e);
		}
	}
	
	protected Integer getTaxonID(Request request) throws ResourceException {
		try {
			return Integer.valueOf((String) request.getAttributes().get("taxon_id"));
		} catch (NumberFormatException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e);
		} catch (NullPointerException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, e);
		}
	}

	protected void addOrRemoveReference(Representation entity, Request request, Response response) throws ResourceException {
		NativeDocument newDoc = SIS.get().newNativeDocument(request.getChallengeResponse());
		try {
			newDoc.parse(entity.getText());
		} catch (Exception e) {
			throw new ResourceException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, e);
		}
		
		Integer id = getCommonNameID(request);
		
		try {
			CommonName commonName = SIS.get().getManager().getObject(CommonName.class, id);
			NativeNodeList list = newDoc.getDocumentElement().getElementsByTagName("action");
			for (int i = 0; i < list.getLength(); i++) {
				NativeElement element = list.elementAt(i);
				Integer refID = Integer.valueOf(element.getAttribute("id"));
				String action = element.getTextContent();
				if (action.equalsIgnoreCase("add")) {
					Reference ref = SIS.get().getManager().getObject(Reference.class, id);
					commonName.getReference().add(ref);
				} else {
					Reference toDelete = null;
					for (Reference ref : commonName.getReference()) {
						if (ref.getId() == refID) {
							toDelete = ref;
							break;
						}
					}
					commonName.getReference().remove(toDelete);
					
					SISPersistentManager.instance().mergeObject(commonName);
					commonName.getTaxon().toXML();
					response.setStatus(Status.SUCCESS_OK);
					response.setEntity(commonName.getId() + "", MediaType.TEXT_PLAIN);
				}
			}
		} catch (PersistentException e) {
			Debug.println(e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
	}
	
	@Override
	public void handlePut(Representation entity, Request request, Response response) throws ResourceException {
		//Adding a new common name
		addOrEditCommonName(entity, request, response);
	}

	private void addOrEditCommonName(Representation entity, Request request, Response response) throws ResourceException {
		NativeDocument newDoc = new JavaNativeDocument();
		try {
			newDoc.parse(entity.getText());
		} catch (Exception e) {
			throw new ResourceException(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY, e);
		}
		
		Taxon taxon = SIS.get().getTaxonIO().getTaxon(getTaxonID(request));
		
		CommonName commonName = CommonName.fromXML(newDoc.getDocumentElement());
		commonName.setIso(SIS.get().getIsoLanguageIO().getIsoLanguageByCode(commonName.getIsoCode()));
		Set<Notes> notes = new HashSet<Notes>();
		for (Notes note : commonName.getNotes()) {
			if (note.getId() != 0)
				notes.add(SIS.get().getNoteIO().get(note.getId()));
		}
		commonName.setNotes(notes);
		if (commonName.getId() == 0) {
			taxon.getCommonNames().add(commonName);
			commonName.setTaxon(taxon);
			
			try {
				SIS.get().getTaxonIO().writeTaxon(taxon, SIS.get().getUser(request));
			} catch (TaxomaticException e) {
				throw new ResourceException(e.isClientError() ? Status.CLIENT_ERROR_BAD_REQUEST : Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
			}
			
			response.setStatus(Status.SUCCESS_OK);
			response.setEntity(commonName.getId() + "", MediaType.TEXT_PLAIN);

		} else {
			commonName.setTaxon(taxon);
			try {
				SISPersistentManager.instance().mergeObject(commonName);
				taxon.getCommonNames().add(commonName);
				taxon.toXML();
				response.setStatus(Status.SUCCESS_OK);
				response.setEntity(commonName.getId() + "", MediaType.TEXT_PLAIN);
			} catch (PersistentException e) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
			}
		}
	}

	public void handleDelete(Request request, Response response) throws ResourceException {
		Integer id = getCommonNameID(request);

		Taxon taxon = SIS.get().getTaxonIO().getTaxon(getTaxonID(request));
		CommonName toDelete = null;
		for (CommonName syn : taxon.getCommonNames()) {
			if (syn.getId() == id) {
				toDelete = syn;
				break;
			}
		}
		if (toDelete == null)
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST);
			
		taxon.getCommonNames().remove(toDelete);
		taxon.toXML();
		
		try {
			SIS.get().getTaxonIO().writeTaxon(taxon, SIS.get().getUser(request));
		} catch (TaxomaticException e) {
			throw new ResourceException(e.isClientError() ? Status.CLIENT_ERROR_BAD_REQUEST : Status.SERVER_ERROR_INTERNAL, e.getMessage(), e);
		}
		
		try {
			CommonNameDAO.delete(toDelete);
		} catch (PersistentException e) {
			// TODO Auto-generated catch block
			Debug.println(e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
		}
	}
	
	@Override
	public void handlePost(Representation entity, Request request, Response response) throws ResourceException {
		if (request.getResourceRef().getPath().contains("reference"))
			addOrRemoveReference(entity, request, response);
		else {
			addOrEditCommonName(entity, request, response);
		}
	}

}
