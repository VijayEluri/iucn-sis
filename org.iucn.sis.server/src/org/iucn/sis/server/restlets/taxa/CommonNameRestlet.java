package org.iucn.sis.server.restlets.taxa;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.HibernateException;
import org.iucn.sis.server.api.application.SIS;
import org.iucn.sis.server.api.persistance.CommonNameDAO;
import org.iucn.sis.server.api.persistance.SISPersistentManager;
import org.iucn.sis.server.api.persistance.hibernate.PersistentException;
import org.iucn.sis.server.api.restlets.ServiceRestlet;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.CommonName;
import org.iucn.sis.shared.api.models.Notes;
import org.iucn.sis.shared.api.models.Taxon;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.solertium.lwxml.shared.NativeDocument;

public class CommonNameRestlet extends ServiceRestlet {

	public CommonNameRestlet(Context context) {
		super(context);
	}

	@Override
	public void definePaths() {
		paths.add("/taxon/{taxon_id}/commonname/{id}/note");
		paths.add("/taxon/{taxon_id}/commonname/{id}");
		paths.add("/taxon/{taxon_id}/commonname");
	}

	protected void addNote(Request request, Response response) {
		String text = request.getEntityAsText();
		NativeDocument ndoc = SIS.get().newNativeDocument(null);
		ndoc.parse(text);
		Notes note = Notes.fromXML(ndoc.getDocumentElement());
		String id = (String) request.getAttributes().get("id");
		try {
			CommonName commonName = (CommonName) SIS.get().getManager()
					.getObject(CommonName.class, Integer.valueOf(id));
			note.setCommonName(commonName);
			note = (Notes) SISPersistentManager.instance().getSession().merge(note);
			commonName.getNotes().add(note);
			commonName.getTaxon().toXML();
			response.setStatus(Status.SUCCESS_OK);
			response.setEntity(note.getId() + "", MediaType.TEXT_PLAIN);
		} catch (HibernateException e) {
			// TODO Auto-generated catch block
			Debug.println(e);
			response.setStatus(Status.SERVER_ERROR_INTERNAL);
			return;
		} catch (NumberFormatException e) {
			Debug.println(e);
			response.setStatus(Status.SERVER_ERROR_INTERNAL);
			return;
		} catch (PersistentException e) {
			Debug.println(e);
			response.setStatus(Status.SERVER_ERROR_INTERNAL);
			return;
		}
		

	}

	protected void addOrEditCommonName(Request request, Response response) {

		String text = request.getEntityAsText();
		String taxonID = (String) request.getAttributes().get("taxon_id");
		Taxon taxon = SIS.get().getTaxonIO().getTaxon(Integer.parseInt(taxonID));
		NativeDocument newDoc = SIS.get().newNativeDocument(request.getChallengeResponse());
		newDoc.parse(text);
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
				if (SIS.get().getTaxonIO().writeTaxon(taxon, SIS.get().getUser(request))) {
					response.setStatus(Status.SUCCESS_OK);
					response.setEntity(commonName.getId() + "", MediaType.TEXT_PLAIN);
				} else {
					response.setStatus(Status.SERVER_ERROR_INTERNAL);
				}
			} catch (Exception e) {
				Debug.println(e);
			}

		} else {
			commonName.setTaxon(taxon);
			try {
				SISPersistentManager.instance().getSession().merge(commonName);
				taxon.getCommonNames().add(commonName);
				taxon.toXML();
				response.setStatus(Status.SUCCESS_OK);
				response.setEntity(commonName.getId() + "", MediaType.TEXT_PLAIN);
			} catch (HibernateException e) {
				// TODO Auto-generated catch block
				Debug.println(e);
				response.setStatus(Status.SERVER_ERROR_INTERNAL);
				return;
			}

		}

	}

	protected void deleteCommonName(Request request, Response response) {
		Integer taxonID = Integer.parseInt((String) request.getAttributes().get("taxon_id"));
		Integer id = Integer.parseInt((String) request.getAttributes().get("id"));

		Taxon taxon = SIS.get().getTaxonIO().getTaxon(taxonID);
		CommonName toDelete = null;
		for (CommonName syn : taxon.getCommonNames()) {
			if (syn.getId() == id) {
				toDelete = syn;
				break;
			}
		}
		if (toDelete == null) {
			response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		} else {
			taxon.getCommonNames().remove(toDelete);
			taxon.toXML();
			if (SIS.get().getTaxonIO().writeTaxon(taxon, SIS.get().getUser(request))) {
				try {
					CommonNameDAO.delete(toDelete);
				} catch (PersistentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					response.setStatus(Status.SERVER_ERROR_INTERNAL);
				}
				response.setStatus(Status.SUCCESS_OK);
			}
		}
	}

	@Override
	public void performService(Request request, Response response) {
		if (request.getMethod().equals(Method.POST)) {
			addOrEditCommonName(request, response);
		} else if (request.getMethod().equals(Method.DELETE)) {
			deleteCommonName(request, response);
		} else if (request.getMethod().equals(Method.PUT)) {
			addNote(request, response);
		}

	}

}
