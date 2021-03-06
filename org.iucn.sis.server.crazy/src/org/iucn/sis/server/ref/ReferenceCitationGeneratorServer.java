package org.iucn.sis.server.ref;

import java.util.HashMap;

import org.iucn.sis.shared.data.references.ReferenceCitationGeneratorShared;
import org.iucn.sis.shared.data.references.ReferenceCitationGeneratorShared.ReturnedCitation;
import org.iucn.sis.shared.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ReferenceCitationGeneratorServer {

	private static void generateCitation(Element referenceElement, Document doc, boolean checkFirst) {
		try {

			String type = referenceElement.getAttribute("type");
			HashMap<String, String> fields = getFields(referenceElement);

			if (type == null || type.equalsIgnoreCase("") || type.equalsIgnoreCase("null")) {
				type = "rldb";
				referenceElement.setAttribute("type", type);
			}

			if (!checkFirst || shouldGenerateCitation(fields)) {
				ReturnedCitation citation = null;
				if (type.equalsIgnoreCase("book"))
					citation = ReferenceCitationGeneratorShared.generateBookChapterCitation(fields);
				else if (type.equalsIgnoreCase("book_section"))
					citation = ReferenceCitationGeneratorShared.generateBookChapterCitation(fields);
				else if (type.equalsIgnoreCase("edited_book"))
					citation = ReferenceCitationGeneratorShared.generateEditedBookCitation(fields);
				else if (type.equalsIgnoreCase("journal_article"))
					citation = ReferenceCitationGeneratorShared.generateJournalArticleCitation(fields);
				else if (type.equalsIgnoreCase("conference_proceedings"))
					citation = ReferenceCitationGeneratorShared.generateConferenceProceedingsCitation(fields);
				else if (type.equalsIgnoreCase("computer_program"))
					citation = ReferenceCitationGeneratorShared.generateComputerProgramCitation(fields);
				else if (type.equalsIgnoreCase("electronic_source"))
					citation = ReferenceCitationGeneratorShared.generateElectronicSourceCitation(fields);
				else if ((type.equalsIgnoreCase("generic")) || (type.equalsIgnoreCase("other")))
					citation = generateGenericCitation(referenceElement);
				else if (type.equalsIgnoreCase("manuscript"))
					citation = ReferenceCitationGeneratorShared.generateManuscriptCitation(fields);
				else if (type.equalsIgnoreCase("magazine_article"))
					citation = ReferenceCitationGeneratorShared.generateMagazineCitation(fields);
				else if (type.equalsIgnoreCase("newspaper_article"))
					citation = ReferenceCitationGeneratorShared.generateNewspaperCitation(fields);
				else if (type.equalsIgnoreCase("personal_communication"))
					citation = ReferenceCitationGeneratorShared.generatePersonalCommunicationCitation(fields);
				else if (type.equalsIgnoreCase("report"))
					citation = ReferenceCitationGeneratorShared.generateReportCitation(fields);
				else if (type.equalsIgnoreCase("rldb"))
					citation = ReferenceCitationGeneratorShared.generateRLDBCitation(fields);
				else if (type.equalsIgnoreCase("thesis"))
					citation = ReferenceCitationGeneratorShared.generateThesisCitation(fields);

				if (citation == null) {
					citation = new ReturnedCitation(false, "");
				}

				setCitation(referenceElement, doc, citation);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * given an element which represents the reference, it checks to see if
	 * there is already a secondary title -- if there is, it exits without
	 * adding citation, if there isn't, adds a label with the name citation with
	 * the generated citation
	 * 
	 * @param referenceElement
	 */
	public static void generateCitationIfNotAlreadyGenerated(Element referenceElement, Document doc) {
		generateCitation(referenceElement, doc, true);
	}

	protected static ReturnedCitation generateGenericCitation(Element referenceElement) {
		referenceElement.setAttribute("type", "Other");
		return ReferenceCitationGeneratorShared.generateOtherCitation(getFields(referenceElement));
	}

	/**
	 * Generates a new citation and places it in the citation field.
	 * 
	 * @param referenceElement
	 */
	public static void generateNewCitation(Element referenceElement, Document doc) {
		generateCitation(referenceElement, doc, false);
	}

	private static HashMap<String, String> getFields(Element referenceElement) {
		HashMap<String, String> fields = new HashMap<String, String>();
		NodeList list = referenceElement.getElementsByTagName("field");

		for (int i = 0; i < list.getLength(); i++) {
			Element temp = (Element) list.item(i);
			String name = temp.getAttribute("name");
			String textContent = temp.getTextContent();

			fields.put(name, textContent);
		}

		return fields;

	}

	private static void makeElement(String name, String label, String textContent, Element parentElement, Document doc) {
		Element newElement = doc.createElement("field");
		newElement.setAttribute("name", name);
		// newElement.setAttribute("label", label);
		newElement.setTextContent(textContent);
		parentElement.appendChild(newElement);
	}

	private static boolean setCitation(Element referenceElement, Document doc, ReturnedCitation citation) {
		boolean success = true;
		try {
			NodeList fields = referenceElement.getElementsByTagName("field");
			Element citationElement = null;
			Element valid = null;

			for (int i = 0; i < fields.getLength() && (citationElement == null || valid == null); i++) {
				Element temp = (Element) fields.item(i);
				if (temp.getAttribute("name").equalsIgnoreCase("citation"))
					citationElement = temp;
				else if (temp.getAttribute("name").equalsIgnoreCase("citation"))
					valid = temp;
			}

			String citationString = (citation.citation == null) ? "" : citation.citation;
			if (citationElement != null) {
				citationElement.setTextContent(XMLUtils.clean(citationString));
			} else {
				makeElement("citation", "Citation", citationString, referenceElement, doc);
			}

			String bool = "Y";
			if (!citation.allFieldsEntered)
				bool = "N";
			if (valid != null) {
				valid.setTextContent(bool);
			} else {
				makeElement("citation_complete", "Citation_Complete", bool, referenceElement, doc);
			}

		} catch (Exception e) {
			e.printStackTrace();
			success = false;
		}
		return success;
	}

	private static boolean shouldGenerateCitation(HashMap<String, String> fields) {
		boolean generate = false;

		try {
			String citation = ReferenceCitationGeneratorShared.getLabel(fields, "citation");
			if (citation != null && !citation.trim().equalsIgnoreCase(""))
				generate = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return generate;
	}

}
