package org.iucn.sis.server.restlets.workingsets;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.iucn.sis.server.api.application.SIS;
import org.iucn.sis.server.api.filters.AssessmentFilterHelper;
import org.iucn.sis.server.api.io.AssessmentIO.AssessmentIOWriteResult;
import org.iucn.sis.server.api.locking.LockType;
import org.iucn.sis.server.api.locking.LockRepository.Lock;
import org.iucn.sis.server.api.restlets.ServiceRestlet;
import org.iucn.sis.server.api.utils.DocumentUtils;
import org.iucn.sis.server.api.utils.FileZipper;
import org.iucn.sis.server.api.utils.ServerPaths;
import org.iucn.sis.server.restlets.taxa.TaxonRestlet;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.AssessmentFilter;
import org.iucn.sis.shared.api.models.CommonName;
import org.iucn.sis.shared.api.models.Synonym;
import org.iucn.sis.shared.api.models.Taxon;
import org.iucn.sis.shared.api.models.TaxonLevel;
import org.iucn.sis.shared.api.models.User;
import org.iucn.sis.shared.api.models.WorkingSet;
import org.restlet.Context;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.representation.StringRepresentation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.solertium.lwxml.factory.NativeDocumentFactory;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.vfs.VFSPath;

public class WorkingSetExportImportRestlet extends ServiceRestlet {

	public WorkingSetExportImportRestlet(String vfsroot, Context context) {
		super(vfsroot, context);
	}


	
	@Override
	public void definePaths() {
		paths.add("/workingSetExporter/private/{username}/{workingsetID}");
		paths.add("/workingSetImporter/{username}");
		paths.add("/workingSetExporter/public/{username}/{workingsetID}");
	}

	private void export(final User user, final Integer workingsetID, boolean lockParam, final Response response,
			final Request request) throws IOException {

		final StringBuilder lockLog = new StringBuilder();
		final HashMap<Integer, String> locked = new HashMap<Integer, String>();

		WorkingSet ws = SIS.get().getWorkingSetIO().readWorkingSet(workingsetID);
		if (ws == null) {
			response.setStatus(Status.SERVER_ERROR_INTERNAL);
			response.setEntity("Working set " + workingsetID + " does not exist.", MediaType.TEXT_PLAIN);
			return;
		}

		AssessmentFilter filter = ws.getFilter();
		AssessmentFilterHelper helper = new AssessmentFilterHelper(filter);

		try {
			// MAKE TEMPORARY FILE REPRESENTING SINGLE WORKINGSET
			String workingSetName = ws.getWorkingSetName();
			workingSetName = workingSetName.replaceAll("\\s", "");
			DocumentUtils
					.writeVFSFile(getWorkingSetTempPath(workingSetName, user.getUsername()), vfs, true, ws.toXML());

			ArrayList<String> filenames = new ArrayList<String>();
			filenames.add(getWorkingSetTempPath(workingSetName, user.getUsername()));

			for (Taxon curNode : ws.getTaxon()) {

				for (Assessment assessment : SIS.get().getAssessmentIO().readPublishedAssessmentsForTaxon(curNode))
					filenames.add(ServerPaths.getAssessmentUrl(assessment.getId() + ""));

				List<Assessment> drafts = SIS.get().getAssessmentIO().readDraftAssessmentsForTaxon(curNode.getId());

				for (Assessment data : drafts) {
					if (helper.allowAssessment(data)) {
						filenames.add(ServerPaths.getAssessmentURL(data));
						if (SIS.get().amIOnline() && lockParam) {
							doLockThings(user, lockLog, ws, data.getId(), curNode, locked);
						}
					}
				}

				for (Integer hierarchicalTaxonID : curNode.getIDFootprint()) {
					filenames.add(ServerPaths.getTaxonURL(hierarchicalTaxonID + ""));
				}

			}

			// CREATING FOLDER IF DOESN'T EXIST
			if (!vfs.exists(new VFSPath(getZippedFolder(user.getUsername())))) {
				vfs.makeCollections(new VFSPath(getZippedFolder(user.getUsername())));
			}

			// MAKE A FILE TO REWRITE ...
			if (!vfs.exists(new VFSPath(getZippedPath(workingSetName, user.getUsername())))) {
				Writer writer = vfs.getWriter(new VFSPath(getZippedPath(workingsetID + "", user.getUsername())));
				writer.close();
			}

			// ZIPPING IT!
			for (String cur : filenames)
				if (!vfs.exists(new VFSPath(cur)))
					System.out.println("Will not be able to export file " + cur + " because it does not exist.");

			FileZipper.zipper(vfs, filenames.toArray(new String[filenames.size()]), getZippedPath(workingsetID + "",
					user.getUsername()));

			// REMOVE TEMPFILE
			vfs.delete(new VFSPath(getWorkingSetTempPath(workingSetName, user.getUsername())));

			String exportPath = request.getResourceRef().getHostIdentifier() + "/raw"
					+ getZippedPath(workingsetID + "", user.getUsername());
			String entity = exportPath + "\r\n";
			entity += "<div><b>" + locked.size() + "</b> assessments successfully locked.<br/> Please"
					+ " note they will be locked until you re-import the working set that "
					+ " contains these assessments.</div><br/>";
			if (lockLog.length() > 0)
				entity += "<div><b><u>Some Assessments Could Not Be Locked!</u></b></div><br/>"
						+ "<div>This means your import will NOT be able to update the online"
						+ " version of the following assessments. You may re-export this working set"
						+ " once the locks are returned to obtain them for yourself.<br/><br/>"
						+ " The follow assessments were not locked:</div>" + lockLog.toString();

			response.setEntity(entity, MediaType.TEXT_ALL);
			response.setStatus(Status.SUCCESS_CREATED);

		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		}

		if (!response.getStatus().isSuccess())
			// Release partially acquired locks...
			for (Entry<Integer, String> curLocked : locked.entrySet())
				SIS.get().getLocker().persistentEagerRelease(curLocked.getKey(), user);
	}

	private void doLockThings(final User username, final StringBuilder lockLog, WorkingSet ws, Integer assessmentID,
			Taxon curNode, Map<Integer, String> locked) {
		if (SIS.amIOnline()) {
			Status ret = SIS.get().getLocker().persistentLockAssessment(assessmentID, LockType.CHECKED_OUT, username,
					ws.getId() + "");

			if (!ret.isSuccess()) {
				Lock lock = SIS.get().getLocker().getAssessmentPersistentLock(assessmentID);
				if (lock.getLockType().equals(LockType.CHECKED_OUT)) {
					lockLog.append("<div>Global draft assessment for " + curNode.getFullName()
							+ " is already checked out by " + lock.getUsername());
				} else {
					lockLog.append("<div>Global draft assessment for " + curNode.getFullName()
							+ " is locked for saving by " + lock.getUsername() + ". Save"
							+ " locks are granted for a 2 minute period after each save." + " Please try again later.");
				}
			} else
				locked.put(assessmentID, username.getUsername());
		}
	}

	public String getSpeciesID(Document draftDoc) {
		return ((Element) draftDoc.getDocumentElement().getElementsByTagName("basicInformation").item(0))
				.getElementsByTagName("speciesID").item(0).getTextContent();
	}

	private String getImportFUllPath(String username, String entryName) {
		return getImportPath(username) + "/" + entryName;
	}

	private String getImportPath(String username) {
		return getUserPath(username) + "/imports";
	}

	private String getImportURL(String username) {
		return getImportPath(username) + "/lastImported.zip";
	}

	/**
	 * Given a taxon document, returns the kingdom and full name as comma
	 * separated values.
	 * 
	 * @param document
	 * @return kingdom,fullName
	 */
	public String getKingdomFullName(Document document) {
		String kingdom = null;
		String fullName = null;
		String footprint = null;
		String[] fullFootprint = null;
		String level = null;
		String infraType = null;

		try {
			Element node = document.getDocumentElement();
			level = node.getAttribute("level");
			fullName = node.getAttribute("name");
			infraType = node.getAttribute("infrarankType");
			if (node.getElementsByTagName("footprint") != null
					&& node.getElementsByTagName("footprint").getLength() == 1) {
				footprint = node.getElementsByTagName("footprint").item(0).getTextContent();
				fullFootprint = footprint.split(",");
			}

			if (level.equalsIgnoreCase(TaxonLevel.getDisplayableLevel(TaxonLevel.KINGDOM))) {
				kingdom = fullName;
			} else {
				kingdom = fullFootprint[TaxonLevel.KINGDOM];
			}

			if (level.equalsIgnoreCase(TaxonLevel.getDisplayableLevel(TaxonLevel.SPECIES))) {
				fullName = fullFootprint[TaxonLevel.GENUS] + fullName;
			} else if (level.equalsIgnoreCase(TaxonLevel.getDisplayableLevel(TaxonLevel.INFRARANK))) {
				if (infraType.trim().equals("0"))
					fullName = fullFootprint[TaxonLevel.GENUS] + fullFootprint[TaxonLevel.SPECIES]
							+ " ssp. " + fullName;
				else if (infraType.trim().equals("1"))
					fullName = fullFootprint[TaxonLevel.GENUS] + fullFootprint[TaxonLevel.SPECIES]
							+ " var. " + fullName;
				else {
					fullName = fullFootprint[TaxonLevel.GENUS] + fullFootprint[TaxonLevel.SPECIES]
							+ fullName;
				}
			} else if (level.equalsIgnoreCase(TaxonLevel.getDisplayableLevel(TaxonLevel.INFRARANK))
					|| level.equalsIgnoreCase(TaxonLevel.getDisplayableLevel(TaxonLevel.SUBPOPULATION))) {
				fullName = fullFootprint[TaxonLevel.GENUS] + fullFootprint[TaxonLevel.SPECIES] + fullName;
			} else if (level.equalsIgnoreCase(TaxonLevel.getDisplayableLevel(TaxonLevel.INFRARANK_SUBPOPULATION))) {
				fullName = fullFootprint[TaxonLevel.GENUS] + fullFootprint[TaxonLevel.SPECIES]
						+ fullFootprint[TaxonLevel.INFRARANK] + fullName;
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return kingdom + "," + fullName.replaceAll("\\s", "");
	}

	private String getUserPath(String username) {
		return "/users/" + username;
	}

	private String getWorkingSetTempPath(String workingSetName, String userName) {
		return getUserPath(userName) + "/" + workingSetName + "Temp.xml";
	}

	private String getZippedFolder(String username) {
		return (getUserPath(username) + "/export");
	}

	private String getZippedPath(String workingSetName, String username) {
		return (getZippedFolder(username) + "/" + workingSetName + ".zip");
	}

	private boolean importDraftAssessment(Assessment assessment, String username, Request request) {
		try {
			// TODO: RIGHT NOW IT JUST REWRITES OVER DRAFT ASSESSMENTS ... NOT
			// NECESSARILY WANTED BEHAVIOR

			Integer assessmentID = assessment.getId();
			String url = "riap://host/assessments";

			Method method = Method.PUT;

			Request newRequest = new Request(method, url, new StringRepresentation(assessment.toXML(),
					MediaType.TEXT_XML));
			if (request.getChallengeResponse() != null)
				newRequest.setChallengeResponse(new ChallengeResponse(request.getChallengeResponse().getScheme(),
						request.getChallengeResponse().getIdentifier(), request.getChallengeResponse().getSecret()));
			else
				newRequest.setChallengeResponse(new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, ""));

			Response newResponse = getContext().getClientDispatcher().handle(newRequest);

			if (newResponse.getStatus().isSuccess())
				return true;
			else {
				System.out.println("Unsuccessfully PUT draft assessment " + assessmentID);
				System.out.println(newResponse.getStatus().getCode());
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private HashMap<Integer, Integer> importTaxa(ArrayList<String> taxaFiles, User user) {
		HashMap<Integer, Integer> importedIdsToSISIds = new HashMap<Integer, Integer>();

		// BREAK INTO KINGDOM, PHYLUM, CLASS ,ORDER , FAMILY ,GENUS ,
		// SPECIES , INFRARANK , SUBPOPULATION , INFRARANK_SUBPOPULATION

		Map<Integer, Taxon> idToImportedTaxon = new HashMap<Integer, Taxon>();
		ArrayList<Taxon> kingdomList = new ArrayList<Taxon>();
		ArrayList<Taxon> phylumList = new ArrayList<Taxon>();
		ArrayList<Taxon> classList = new ArrayList<Taxon>();
		ArrayList<Taxon> orderList = new ArrayList<Taxon>();
		ArrayList<Taxon> familyList = new ArrayList<Taxon>();
		ArrayList<Taxon> genusList = new ArrayList<Taxon>();
		ArrayList<Taxon> speciesList = new ArrayList<Taxon>();
		ArrayList<Taxon> infrarankList = new ArrayList<Taxon>();
		ArrayList<Taxon> subpopulationList = new ArrayList<Taxon>();
		ArrayList<Taxon> infrarankSubpopulationList = new ArrayList<Taxon>();

		while (taxaFiles.size() > 0) {
			NativeDocument file = NativeDocumentFactory.newNativeDocument();
			file.parse(DocumentUtils.getVFSFileAsString(taxaFiles.remove(0), vfs));
			Taxon taxon = Taxon.fromXML(file);
			idToImportedTaxon.put(taxon.getId(), taxon);

			Integer nodeLevel = taxon.getLevel();
			if (nodeLevel == TaxonLevel.KINGDOM) {
				kingdomList.add(taxon);
			} else if (nodeLevel == TaxonLevel.PHYLUM) {
				phylumList.add(taxon);
			} else if (nodeLevel == TaxonLevel.CLASS) {
				classList.add(taxon);
			} else if (nodeLevel == TaxonLevel.ORDER) {
				orderList.add(taxon);
			} else if (nodeLevel == TaxonLevel.FAMILY) {
				familyList.add(taxon);
			} else if (nodeLevel == TaxonLevel.GENUS) {
				genusList.add(taxon);
			} else if (nodeLevel == TaxonLevel.SPECIES) {
				speciesList.add(taxon);
			} else if (nodeLevel == TaxonLevel.INFRARANK) {
				infrarankList.add(taxon);
			} else if (nodeLevel == TaxonLevel.SUBPOPULATION) {
				subpopulationList.add(taxon);
			} else if (nodeLevel == TaxonLevel.INFRARANK_SUBPOPULATION) {
				infrarankSubpopulationList.add(taxon);
			}
		}

		while (kingdomList.size() > 0) {
			importNodes(kingdomList.remove(0), idToImportedTaxon, importedIdsToSISIds, user);

		}
		while (phylumList.size() > 0) {
			importNodes(phylumList.remove(0), idToImportedTaxon, importedIdsToSISIds, user);

		}
		while (classList.size() > 0) {
			importNodes(classList.remove(0), idToImportedTaxon, importedIdsToSISIds, user);

		}
		while (orderList.size() > 0) {
			importNodes(orderList.remove(0), idToImportedTaxon, importedIdsToSISIds, user);

		}
		while (familyList.size() > 0) {
			importNodes(familyList.remove(0), idToImportedTaxon, importedIdsToSISIds, user);

		}
		while (genusList.size() > 0) {
			importNodes(genusList.remove(0), idToImportedTaxon, importedIdsToSISIds, user);

		}
		while (speciesList.size() > 0) {
			importNodes(speciesList.remove(0), idToImportedTaxon, importedIdsToSISIds, user);

		}
		while (infrarankList.size() > 0) {
			importNodes(infrarankList.remove(0), idToImportedTaxon, importedIdsToSISIds, user);

		}
		while (subpopulationList.size() > 0) {
			importNodes(subpopulationList.remove(0), idToImportedTaxon, importedIdsToSISIds, user);

		}
		while (infrarankSubpopulationList.size() > 0) {
			importNodes(infrarankSubpopulationList.remove(0), idToImportedTaxon, importedIdsToSISIds, user);

		}
		return importedIdsToSISIds;
	}

	/**
	 * Adds new synonyms created in the importedTaxon to the sisTaxon
	 * 
	 * @param importedTaxon
	 * @param sisTaxon
	 */
	private void importSynonyms(Taxon importedTaxon, Taxon sisTaxon) {
		for (Synonym syn : importedTaxon.getSynonyms()) {
			if (!sisTaxon.getSynonyms().contains(syn)) {
				syn.setId(0);
				sisTaxon.getSynonyms().add(syn);
			}
		}
	}

	/**
	 * Adds new synonyms created in the importedTaxon to the sisTaxon
	 * 
	 * @param importedTaxon
	 * @param sisTaxon
	 */
	private void importCommonNames(Taxon importedTaxon, Taxon sisTaxon) {
		for (CommonName commonName : importedTaxon.getCommonNames()) {
			if (!sisTaxon.getCommonNames().contains(commonName)) {
				commonName.setId(0);
				sisTaxon.getCommonNames().add(commonName);
			}
		}
	}

	/**
	 * Imports the node into the appropriate location, returns the id of the
	 * document and the new id of the document where it is currently saved on
	 * the system. Returns -1 if there was a failure.
	 * 
	 * @param document
	 * @return
	 */
	private void importNodes(Taxon importedTaxon, final Map<Integer, Taxon> idsToImportedTaxon,
			final HashMap<Integer, Integer> oldToNewIDs, User user) {
		if (importedTaxon.getLevel() != TaxonLevel.KINGDOM) {
			importedTaxon.setParent(idsToImportedTaxon.get(importedTaxon.getParent().getId()));
		}

		Integer importedID = importedTaxon.getId();

		// DETERMINE IF IMPORTING NEW TAXON OR IF ALREADY EXIST IN SIS
		String kingdomName = importedTaxon.getFootprint()[0];
		Taxon sisTaxon = SIS.get().getTaxonIO().readTaxonByName(kingdomName, importedTaxon.getFriendlyName());

		if (sisTaxon == null) {
			// THE TAXON IS NEW!
			importedTaxon.setId(0);
			SIS.get().getTaxomaticIO().saveNewTaxon(importedTaxon, user);
		} else {
			importSynonyms(importedTaxon, sisTaxon);
			importCommonNames(importedTaxon, sisTaxon);
			SIS.get().getTaxonIO().writeTaxon(sisTaxon, user);
			idsToImportedTaxon.put(importedID, sisTaxon);
		}

		oldToNewIDs.put(importedID, importedTaxon.getId());

	}

	private boolean importPublishedAssessment(Assessment published, Request request) {

		if (SIS.amIOnline()) {
			return false;
		} else {
			try {

				AssessmentIOWriteResult result = SIS.get().getAssessmentIO().saveNewAssessment(published,
						SIS.get().getUser(request));
				return result.status.isSuccess();
			} catch (Exception e1) {
				e1.printStackTrace();
				return false;
			}
		}
	}

	
	private boolean importWorkingSet(WorkingSet workingSet, String username) {

		
			workingSet.setId(0);
			workingSet.setCreatedDate(new Date());
			workingSet.setCreator(username);
			workingSet.setCreator(SIS.get().getUserIO().getUserFromUsername(username));
			workingSet.getUsers().add(workingSet.getCreator());
			return SIS.get().getWorkingSetIO().saveWorkingSet(workingSet, workingSet.getCreator());
			
		

	}

	@Override
	public void performService(Request request, Response response) {
		try {
			String username = (String) request.getAttributes().get("username");
			String workingSetID = (String) request.getAttributes().get("workingsetID");
			if (request.getMethod().equals(Method.GET)) {
				boolean lockParam = Boolean.valueOf(request.getResourceRef().getQueryAsForm().getFirstValue("lock"))
						.booleanValue();
				export(SIS.get().getUserIO().getUserFromUsername(username), Integer.valueOf(workingSetID), lockParam,
						response, request);
			} else if (request.getMethod().equals(Method.POST) && vfs.exists(new VFSPath(getUserPath(username))))
				postZipFile(username, response, request);
			else
				response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(Status.SERVER_ERROR_INTERNAL);
		}
	}

	private void postZipFile(String username, Response response, Request request) {

		RestletFileUpload fileUploaded = new RestletFileUpload(new DiskFileItemFactory());
		try {
			List<FileItem> list = fileUploaded.parseRequest(request);
			FileItem file = null;

			for (int i = 0; i < list.size() && file == null; i++) {
				FileItem item = list.get(i);
				if (!item.isFormField()) {
					file = item;
				}
			}

			if (file == null) {
				System.out.println("Supposed file attachment is NULL...");
				response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			}

			else {
				if (!vfs.exists(new VFSPath(getImportPath(username)))) {
					vfs.makeCollection(new VFSPath(getImportPath(username)));
				}
				OutputStream outStream = vfs.getOutputStream(new VFSPath(getImportURL(username)));
				outStream.write(file.get());
				outStream.close();

				unZipAndImport(getImportURL(username), username, response, request);
			}

		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		}

	}

	private Document replaceTaxaIDInWorkingSet(Document workingSetDoc, String oldID, String newID) {

		Element taxa = (Element) workingSetDoc.getDocumentElement().getElementsByTagName("taxa").item(0);
		NodeList species = taxa.getElementsByTagName("species");
		Node nodeToReplace = null;

		for (int i = 0; i < species.getLength() && nodeToReplace == null; i++) {
			String tempID = species.item(i).getTextContent();
			if (tempID.trim().equalsIgnoreCase(oldID.trim())) {
				nodeToReplace = species.item(i);
			}
		}

		if (nodeToReplace != null) {
			Node newNode = DocumentUtils.createElementWithText(workingSetDoc, "species", newID);
			(nodeToReplace.getParentNode()).replaceChild(newNode, nodeToReplace);
			return workingSetDoc;
		} else
			return null;
	}

	private void unZipAndImport(String url, String username, Response response, Request request) {

		ArrayList<String> taxaFiles = new ArrayList<String>();
		ArrayList<String> draftFiles = new ArrayList<String>();
		ArrayList<String> publishedFiles = new ArrayList<String>();
		String workingSetFile = "";
		Document workingSetDocument = null;
		User user = SIS.get().getUserIO().getUserFromUsername(username);
		ArrayList<Assessment> successfulImports = new ArrayList<Assessment>();

		try {
			ZipInputStream zipInputStream = new ZipInputStream(vfs.getInputStream(new VFSPath(url)));
			ZipEntry entry = zipInputStream.getNextEntry();
			while (entry != null) {

				// UNZIP
				// System.out.println("Unzipping " +
				// entry.getName());
				if (!vfs.exists(new VFSPath(getImportFUllPath(username, entry.getName())))) {
					Writer writer = vfs
							.getWriter(new VFSPath(getImportFUllPath(username, entry.getName().substring(1))));
					writer.close();
				}

				String filePathToImport = getImportFUllPath(username, entry.getName().substring(1));
				FileOutputStream fout = (FileOutputStream) vfs.getOutputStream(new VFSPath(filePathToImport));

				for (int c = zipInputStream.read(); c != -1; c = zipInputStream.read()) {
					fout.write(c);
				}
				zipInputStream.closeEntry();
				fout.close();

				entry = zipInputStream.getNextEntry();

				// DETERMINE WHICH LIST TO PLACE THE FILE IN

				// IT IS A DRAFT ASSESSMENT
				if (filePathToImport.indexOf("/drafts/") != -1) {
					draftFiles.add(filePathToImport);
				}

				// IT IS A TAXON
				else if (filePathToImport.indexOf("/nodes/") != -1) {
					taxaFiles.add(filePathToImport);
				}

				// IT IS A TAXON
				else if (filePathToImport.indexOf("/browse/assessments/") != -1) {
					publishedFiles.add(filePathToImport);
				}

				// IT IS A WORKING SET
				else {
					workingSetFile = filePathToImport;
				}

			}
			// FINISHED UNZIPPING
			zipInputStream.closeEntry();

			boolean successfulImportThusFar = true;

			// IMPORT TAXA, ADDING THE ID AND THE PREVIOUS ID IN THE TAXANEWID
			// ARRAYLIST VIA COMMA SEPERATED VALUES
			// IF THEY ARE DIFFERENT, ONLY ADD THE SINGLE ID IF THEY ARE THE
			// SAME. SHOULD THROW EXCEPTION IF
			// THERE WAS A PROBLEM AND IMPORTNODES RETURNED -1
			HashMap<Integer, Integer> importedIdsToSISIds = importTaxa(taxaFiles, user);

			// IMPORT DRAFT ASSESSMENTS
			while (!draftFiles.isEmpty() && successfulImportThusFar) {
				String currentFile = draftFiles.remove(0);
				Document currentDraftDoc = DocumentUtils.getVFSFileAsDocument(currentFile, vfs);
				NativeDocument ndoc = NativeDocumentFactory.newNativeDocument();
				ndoc.parse(DocumentUtils.serializeNodeToString(currentDraftDoc.getDocumentElement()));

				Assessment assessment = Assessment.fromXML(ndoc);
				Integer assessmentSpeciesID = assessment.getSpeciesID();

				// IF THE CURRENTID IS NOT IN THE LIST, THAT MEANS THAT IT HAS
				// BEEN CHANGED
				if (importedIdsToSISIds.containsKey(assessmentSpeciesID)) {
					Integer newID = importedIdsToSISIds.get(assessmentSpeciesID);
					assessment.getTaxon().setId(newID);

					List<Assessment> compareTo = SIS.get().getAssessmentIO().readDraftAssessmentsForTaxon(
							assessment.getSpeciesID());

					boolean found = false;
					for (Assessment cur : compareTo) {
						if ((cur.isGlobal() && assessment.isGlobal())
								|| ((cur.getRegionIDs().contains(assessment.getRegionIDs()) && assessment
										.getRegionIDs().size() == cur.getRegionIDs().size()) && cur.isEndemic() == assessment
										.isEndemic())) {
							assessment.setId(cur.getId());
							found = true;
							break;
						}
					}

					if (!found)
						assessment.setId(0);

					if (isImportAllowed(assessment, username) && importDraftAssessment(assessment, username, request)) {
						if (!found && SIS.amIOnline())
							SIS.get().getLocker().persistentEagerRelease(assessment.getId(),
									SIS.get().getUserIO().getUserFromUsername(username));
						successfulImports.add(assessment);
					}

				}
			}

			// IMPORT PUBLISHED ASSESSMENTS
			if (!SIS.amIOnline()) {				
				
				while (!publishedFiles.isEmpty() && successfulImportThusFar) {
					String currentFile = publishedFiles.remove(0);
					NativeDocument ndoc = NativeDocumentFactory.newNativeDocument();
					ndoc.parse(DocumentUtils.getVFSFileAsString(currentFile, vfs));
					Assessment published = Assessment.fromXML(ndoc);
					published.getTaxon().setId(importedIdsToSISIds.get(published.getTaxon().getId()));
					successfulImportThusFar = importPublishedAssessment(published, request);
				}
				
			}

			// IMPORT WORKING SET
			if (successfulImportThusFar) {
				
				NativeDocument ndoc = NativeDocumentFactory.newNativeDocument();
				ndoc.parse(DocumentUtils.getVFSFileAsString(workingSetFile, vfs));
				WorkingSet workingSet = WorkingSet.fromXML(ndoc);
				
				// HAVE TO CHANGE THE WORKING SET TO REFLECT THE NEW IDS

				for (Taxon taxon : workingSet.getTaxon()) {
					taxon.setId(importedIdsToSISIds.get(taxon.getId()));					
				}
				
				successfulImportThusFar = importWorkingSet(workingSet, username);
			}

			if (!successfulImportThusFar) {
				response.setEntity(new StringRepresentation("A failure occurred during the import.",
						MediaType.TEXT_PLAIN));
				response.setStatus(Status.SERVER_ERROR_INTERNAL);
			} else {
				
				// REMOVE ALL UNNECESSARY FILES IN /imports
				String[] inImports = vfs.list(getImportPath(username));
				for (int i = 0; i < inImports.length; i++) {
					String path = getImportPath(username) + "/" + inImports[i];
					if (!(path).equalsIgnoreCase(getImportURL(username)))
						vfs.delete(new VFSPath(path));
				}

				StringBuilder ret = new StringBuilder("<div>");
				if (SIS.amIOnline()) {
					for (Assessment curAss : successfulImports) {
						ret.append("<div>A" + (curAss.isGlobal() ? " global " : " regional "));
						ret.append("draft assessment has been imported for the taxon " + curAss.getSpeciesName()
								+ ".</div>");
					}

					if (successfulImports.size() == 0)
						ret.append("There were no assessments to import. If this seems incorrect, "
								+ "please check with an administrator to ensure you had successfully "
								+ "obtained checkout locks when you first exported this working set.");
				} else {
					ret.append("All taxa and assessments have been imported into your offline copy.");
				}
				ret.append("</div>");

				response.setEntity(new StringRepresentation(ret.toString(), MediaType.TEXT_HTML));
				response.setStatus(Status.SUCCESS_CREATED);
			}

		} catch (Exception e) {
			e.printStackTrace();
			StringBuilder ret = new StringBuilder(e.toString() + "\n");
			for (StackTraceElement el : e.getStackTrace())
				ret.append(el.toString() + "\n");

			response.setEntity(new StringRepresentation(ret.toString(), MediaType.TEXT_PLAIN));
			response.setStatus(Status.SERVER_ERROR_INTERNAL);
		}
	}

	private boolean isImportAllowed(Assessment assessment, String username) {
		if (!SIS.amIOnline())
			return true;
		else if (assessment.getId() == 0)
			return SIS.get().getAssessmentIO().allowedToCreateNewAssessment(assessment);
		else {
			Lock lock = SIS.get().getLocker().getAssessmentPersistentLock(assessment.getId());
			return lock.getUsername().equalsIgnoreCase(username) && lock.getLockType().equals(LockType.CHECKED_OUT);
		}

	}

}
