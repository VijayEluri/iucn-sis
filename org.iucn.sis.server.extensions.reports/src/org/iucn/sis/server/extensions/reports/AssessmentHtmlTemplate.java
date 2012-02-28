package org.iucn.sis.server.extensions.reports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.hibernate.Session;
import org.iucn.sis.server.api.application.SIS;
import org.iucn.sis.server.api.io.AssessmentSchemaIO;
import org.iucn.sis.server.api.persistance.SISPersistentManager;
import org.iucn.sis.server.api.persistance.hibernate.PersistentException;
import org.iucn.sis.server.api.schema.AssessmentSchema;
import org.iucn.sis.server.api.schema.redlist.RedListAssessmentSchema;
import org.iucn.sis.server.api.utils.FormattedDate;
import org.iucn.sis.server.api.utils.LookupLoader;
import org.iucn.sis.shared.api.criteriacalculator.ExpertResult.ResultCategory;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.Assessment;
import org.iucn.sis.shared.api.models.CommonName;
import org.iucn.sis.shared.api.models.Field;
import org.iucn.sis.shared.api.models.Notes;
import org.iucn.sis.shared.api.models.PrimitiveField;
import org.iucn.sis.shared.api.models.Reference;
import org.iucn.sis.shared.api.models.Region;
import org.iucn.sis.shared.api.models.Synonym;
import org.iucn.sis.shared.api.models.Taxon;
import org.iucn.sis.shared.api.models.User;
import org.iucn.sis.shared.api.models.fields.RedListCreditedUserField;
import org.iucn.sis.shared.api.models.fields.RedListCriteriaField;
import org.iucn.sis.shared.api.models.fields.RegionField;
import org.iucn.sis.shared.api.models.primitivefields.BooleanRangePrimitiveField;
import org.iucn.sis.shared.api.models.primitivefields.BooleanUnknownPrimitiveField;
import org.iucn.sis.shared.api.models.primitivefields.DatePrimitiveField;
import org.iucn.sis.shared.api.models.primitivefields.FloatPrimitiveField;
import org.iucn.sis.shared.api.models.primitivefields.ForeignKeyListPrimitiveField;
import org.iucn.sis.shared.api.models.primitivefields.ForeignKeyPrimitiveField;
import org.iucn.sis.shared.api.utils.CanonicalNames;
import org.iucn.sis.shared.api.views.FieldParser;
import org.iucn.sis.shared.api.views.Organization;
import org.iucn.sis.shared.api.views.Page;
import org.iucn.sis.shared.api.views.View;
import org.iucn.sis.shared.api.views.ViewParser;
import org.iucn.sis.shared.api.views.components.DisplayData;
import org.iucn.sis.shared.api.views.components.TreeData;
import org.iucn.sis.shared.api.views.components.TreeDataRow;
import org.restlet.util.Couple;
import org.w3c.dom.Document;

import com.solertium.lwxml.java.JavaNativeDocument;
import com.solertium.lwxml.shared.NativeDocument;
import com.solertium.lwxml.shared.NativeElement;
import com.solertium.lwxml.shared.NativeNodeList;
import com.solertium.util.AlphanumericComparator;
import com.solertium.util.portable.PortableCalculator;

public class AssessmentHtmlTemplate {

	private static final String PREFIX = "../..";
	private static final String BLANK_STRING = "(Not specified)";
	
	public static final String RESOURCE_LOCATION = PREFIX + "/resources/";
	public static final String CSS_FILE = "speciesReportStyle.css";
	
	private final boolean showEmptyFields;
	private final Session session;
	private final Assessment assessment;
	private final Taxon taxon;
	private final AssessmentSchema schema;
	
	private StringBuilder theHtml;
	private Organization curOrg;
	private List<String> exclude;
	private String logo;
	
	private boolean isAggregate;

	public AssessmentHtmlTemplate(Session session, Assessment assessment, boolean showEmptyFields, boolean limitedSet) {
		this.session = session;
		this.assessment = assessment;
		this.taxon = assessment.getTaxon();
		this.showEmptyFields = showEmptyFields;
	
		AssessmentSchema schema = null;
		try {
			schema = SIS.get().getAssessmentSchemaBroker()
				.getAssessmentSchema(assessment.getSchema(SIS.get().getDefaultSchema()));
		} catch (Throwable e) {
			schema = new RedListAssessmentSchema();
		}
		this.schema = schema;
		setLogo("redListLogo.jpg");
		
		theHtml = new StringBuilder();
		
		curOrg = null;
		
		exclude = limitedSet ? Arrays.asList(CanonicalNames.limitedSetExcludes) : new ArrayList<String>();
		isAggregate = false;
	}
	
	public void setLogo(String logo) {
		this.logo = logo;
	}
	
	public byte[] getHtmlBytes() {
		return theHtml.toString().getBytes();
	}

	public String getHtmlString() {
		return theHtml.toString();
	}
	
	public void setAggregate(boolean isAggregate) {
		this.isAggregate = isAggregate;
	}
	
	public void parse() {
		parseView("Report");
	}
	
	public void parseAvailable() {
		parseView(null);
	}

	public void parseView(String viewName) {
		// Add IUCN logo and species Name/Authority and Taxonomy Info
		buildHeadingTable();

		// if we're not provided with a view just display everything in
		// alphabetical order
		if (viewName == null) {
			buildAssessmentInfo();
		} else {
			View curView = loadView(viewName);
			for (Page page : curView.getPages().values())
				buildSection(page);
		}
		
		buildBibliography();

		if (!isAggregate)
			theHtml.append("</body>\r\n</html>");
	}
	
	private View loadView(String name) {
		ViewParser parser = new ViewParser();
		parser.parse(new ViewNativeDocument(schema.getViews()));
		
		View view = parser.getViews().get(name);
		if (view == null)
			view = parser.getViews().get("Report");
		
		return view;
	}

	private StringBuilder appendInlineTable(String heading, String data) {
		StringBuilder retHtml = new StringBuilder();
		retHtml.append("<table style=\"display: inline-table;\"><tr><th>");
		retHtml.append(heading + "</th></tr>\n");
		retHtml.append("<tr><td>" + data + "</td></tr></table>\n");
		return retHtml;
	}

	private void appendSectionHeader(String title) {
		theHtml.append("<h1>");
		theHtml.append(title);
		theHtml.append("</h1>\n");
	}

	private void buildAssessmentInfo() {
		ArrayList<Field> fields = new ArrayList<Field>(assessment.getField());
		Collections.sort(fields, new TextComparator<Field>() {
			protected String getString(Field model) {
				return model.getName();
			}
		});
		
		curOrg = new Organization("All Fields", "", null, false);

		for (Field field : fields) {
			String canonicalName = field.getName();
			
			if (!exclude.contains(canonicalName)) {
				Couple<DisplayData, Map<String, String>> currentDisplayData = fetchDisplayData(canonicalName);
				if (currentDisplayData != null) {
					StringBuilder orgHtml = new StringBuilder();
					try {
						if (field.isClassificationScheme())
							orgHtml = parseClassificationScheme(field, canonicalName, currentDisplayData);
						else
							orgHtml = parseField(field, canonicalName, canonicalName, 8, currentDisplayData, false, false, false);
						
					} catch (Exception e) {
						debug("DIED TRYING TO BUILD " + canonicalName);
					}
					
					if (orgHtml.length() > 0) {
						theHtml.append("<div id=\"" + canonicalName + "\">\n");
						theHtml.append(orgHtml);
						theHtml.append("</div>\n");
					}
				}
			}
		}
	}

	// Conservation
	private void buildBibliography() {
		appendSectionHeader("Bibliography");
		StringBuffer orgHtml = new StringBuffer("<div id=\"bibliography\">");
		
		Set<Reference> refs = new HashSet<Reference>();
		refs.addAll(assessment.getReference());
		for (Field field : assessment.getField())
			refs.addAll(field.getReference());
		
		if (refs.isEmpty()) {
			if (showEmptyFields)
				orgHtml.append("<p>No references used in this assessment.</p>");
		} else {
			ArrayList<Reference> allRefs = new ArrayList<Reference>(refs);
			Collections.sort(allRefs, new TextComparator<Reference>() {
				protected String getString(Reference model) {
					return model.generateCitationIfNotAlreadyGenerate();
				}
			});
			
			for (Reference reference : allRefs)
				orgHtml.append("<p>" + reference.getCitation() + "</p>");
		}
		
		theHtml.append(orgHtml.toString());
	}
	
	private void buildCommonNames() {
		theHtml.append("<p><strong>Common Names: </strong>");
		if (taxon.getCommonNames().isEmpty()) {
			theHtml.append("No Common Names");
		} else {
			HashMap<String, String> dupes = new HashMap<String, String>();

			for (CommonName curCN : taxon.getCommonNames()) {
				if (!dupes.containsKey(curCN.getName()))
					dupes.put(curCN.getName(), curCN.getLanguage());
				else if (dupes.get(curCN.getName()) == null || dupes.get(curCN.getName()).equals(""))
					if (curCN.getLanguage() != null || curCN.getLanguage().length() > 0)
						dupes.put(curCN.getName(), curCN.getLanguage());
			}

			String commonNames = "";
			for (Entry<String, String> curEntry : dupes.entrySet()) {
				commonNames += curEntry.getKey() + " (" + curEntry.getValue() + "), ";
			}
			// trim off the comma from the last entry
			commonNames = commonNames.substring(0, commonNames.length() - 2);
			theHtml.append(commonNames);
		}
		theHtml.append("<br/>\n");
	}
	
	private void buildSection(Page page) {
		StringBuffer orgHtml = buildPageOrganizations(page.getOrganizations());
		if (orgHtml.length() > 0) {
			theHtml.append("<div id=\""+page.getId()+"\">\n");
			appendSectionHeader(page.getTitle());
			theHtml.append(orgHtml);
			theHtml.append("</div>\n");
		}
	}

	/**** Header ****/
	private void buildHeadingTable() {
		Taxon taxon = assessment.getTaxon();
		
		if (!isAggregate) {
			theHtml.append("<html>\n" +
				"<head>\n" +
				"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n");
			try {
				theHtml.append(injectCSS());
			} catch (IOException e) {
				theHtml.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + createUrl(CSS_FILE) + "\">\n");
			}
			theHtml.append("<title>");
			theHtml.append(taxon.getFullName());
			theHtml.append("</title>\n</head>\n<body>");
		}
		theHtml.append("<div id=\"header\">\n");
		if (!assessment.isPublished())
			theHtml.append("<div id=\"draftType\">"+assessment.getAssessmentType().getDisplayName(true)+"</div>\n");
		theHtml.append("<img src=\""+createUrl(logo)+"\" alt=\"Logo\" />\n");
		theHtml.append("<div id=\"speciesInfo\">");
		theHtml.append("<h1><em>");
		theHtml.append(taxon.getFullName() + " - " + taxon.getTaxonomicAuthority());
		theHtml.append("</em></h1>");
		buildHierarchyList(taxon);
		theHtml.append("</div>\n</div>\n");
		
		buildTaxonomyInformation();
	}
	
	private String injectCSS() throws IOException {
		final StringBuilder in = new StringBuilder();
		in.append("<style type=\"text/css\"><!--\n");
		final BufferedReader reader = new BufferedReader(new InputStreamReader(
				AssessmentHtmlTemplate.class.getResourceAsStream(CSS_FILE)));
		String line = null;
		while ((line = reader.readLine()) != null)
			in.append(line+"\n");
		in.append("--></style>\n");
		return in.toString();
	}

	private void buildHierarchyList(Taxon taxon) {

		for (int i = 0; i < taxon.getFootprint().length - 1; i++) {
			// Taxon.displayableLevel[i]
			theHtml.append(taxon.getFootprint()[i]);
			theHtml.append(" - ");
		}
		theHtml.append(taxon.getName());
	}

	private StringBuffer buildPageOrganizations(ArrayList<Organization> organizations) {
		StringBuffer retHtml = new StringBuffer();
		for (int i = 0; i < organizations.size(); i++) {
			curOrg = organizations.get(i);

			StringBuilder orgHtml = new StringBuilder();
			for (String fieldId : curOrg.getMyFields())
				if (!"_blank".equals(fieldId))
					orgHtml.append(parsePageField(fieldId, assessment.getField(fieldId)));
			
			if (orgHtml.length() > 0) {
				if (!(curOrg.getTitle().contains("Documentation"))) {
					retHtml.append("<h2>" + curOrg.getTitle().replaceAll("[A-Z][a-z]", " $0") + "</h2>\n");
				}
				retHtml.append(orgHtml);
			}
		}
		return retHtml;
	}

	private void buildSynonyms() {
		theHtml.append("<strong>Synonyms: </strong>");
		if (taxon.getSynonyms().isEmpty()) {
			theHtml.append("No Synonyms");
		} else {
			HashMap<String, String> dupes = new HashMap<String, String>();

			for (Synonym curSyn : taxon.getSynonyms()) {
				if (!dupes.containsKey(curSyn.getName()))
					dupes.put(curSyn.getName(), curSyn.getAuthor());
				else if (dupes.get(curSyn.getName()) == null || dupes.get(curSyn.getName()).equals(""))
					if (curSyn.getAuthor() != null)
						dupes.put(curSyn.getName(), curSyn.getAuthor());
			}

			for (Entry<String, String> curEntry : dupes.entrySet()) {
				theHtml.append(curEntry.getKey());
				theHtml.append(" ");
				theHtml.append(curEntry.getValue());
				theHtml.append("; ");
			}
		}
		theHtml.append("</p>\n");
	}

	private void buildTaxonomyInformation() {
		theHtml.append("<div id=\"taxonomyInfo\">\n");
		buildCommonNames();
		buildSynonyms();
		theHtml.append("<p>");
		for (Notes notes : taxon.getNotes()) {
			theHtml.append(notes.getValue());
			theHtml.append(" ");
		}
		theHtml.append("</p>");
		Field taxonomicNotes = null;
		if (assessment.isPublished())
			taxonomicNotes = assessment.getField(CanonicalNames.TaxonomicNotes);
		if (taxonomicNotes == null || !taxonomicNotes.hasData())
			taxonomicNotes = taxon.getTaxonomicNotes();
		if (taxonomicNotes != null && taxonomicNotes.getPrimitiveField() != null && !taxonomicNotes.getPrimitiveField().isEmpty()) {
			theHtml.append("<p>" + createDataLabel("Taxonomic Note") + "<br/>");
			theHtml.append(taxonomicNotes.getPrimitiveField().iterator().next().getRawValue() + "</p>");
		}
		theHtml.append("</div>\n");
	}

	private String createDataLabel(String label) {
		String ret = "<span class=\"dataLabel\">" + label;
		if (!(label.endsWith(":")) && !(label.endsWith("?")) && !(label.endsWith("."))) {
			ret += ":";
		}
		ret += " </span>\n";
		return ret;
	}

	protected Couple<DisplayData, Map<String, String>> fetchDisplayData(String canonicalName) {
		try {
			AssessmentSchemaIO io = new AssessmentSchemaIO();
			
			NativeDocument jnd = new JavaNativeDocument();
			jnd.parse(io.getFieldAsString(schema, canonicalName));

			FieldParser parser = new FieldParser();
			DisplayData currentDisplayData = parser.parseFieldData(jnd);
			
			return new Couple<DisplayData, Map<String,String>>(currentDisplayData, 
					extractDescriptions(canonicalName, currentDisplayData.getDescription(), jnd));
		} catch (Exception e) {
			debug("DIED FETCHING DISPLAY DATA FOR {0}", canonicalName);
			return null;
		}
	}

	private StringBuilder parseClassificationScheme(Field selected, 
			final String canonicalName, Couple<DisplayData, Map<String, String>> currentDisplayData) {
		final StringBuilder retHtml = new StringBuilder();
		final TreeData currentTreeData = (TreeData) currentDisplayData.getFirst();
		
		if (selected == null || selected.getFields() == null || selected.getFields().size() == 0) {
			if (showEmptyFields) {
				String desc = currentTreeData.getDescription();
				if (isBlank(desc))
					desc = canonicalName;
				retHtml.append("<p>" + createDataLabel(desc));
				retHtml.append(BLANK_STRING);
				retHtml.append("</p>\n");
			}
			return retHtml;
		}
		
		final Map<String, TreeDataRow> row = currentTreeData.flattenTree();
		final int topLevel = CanonicalNames.CountryOccurrence.equals(canonicalName) ? 1 : 0;
		
		final List<Field> subfields = new ArrayList<Field>(selected.getFields());
		Collections.sort(subfields, new TextComparator<Field>() {
			protected String getString(Field model) {
				try {
					String value = model.getPrimitiveField(canonicalName+"Lookup").getRawValue();
					return row.get(value).getFullLineage(topLevel);
				} catch (NullPointerException e) {
					return BLANK_STRING;
				}
			}
		});
		
		retHtml.append("<table>");
		boolean showHeader = true;
		boolean occurrenceHeadingReorder = canonicalName.equals("CountryOccurrence") || 
			canonicalName.equals("LargeMarineEcosystems") || 
			canonicalName.equals("FAOOccurrence");
		
		for (Field subfield : subfields) {
			String value = subfield.getPrimitiveField(canonicalName+"Lookup").getRawValue();
			
			retHtml.append(parseField(subfield, canonicalName,
					row.get(value).getFullLineage(topLevel), 16, currentDisplayData,
					true, showHeader, occurrenceHeadingReorder));
			
			showHeader = false;
		}
		retHtml.append("</table>");
		
		return retHtml;
	}
	
	private String generateTextFromUsers(List<User> userList, String order) {
		return RedListCreditedUserField.generateText(userList, order);
	}
	
	private HashMap<String, User> getUserInfo(List<Integer> userIDs) throws PersistentException {
		HashMap<String, User> users = new HashMap<String, User>();
		
		for (Integer userID : userIDs) {
			User user = SISPersistentManager.instance().getObject(session, User.class, userID);
			if (user != null)
				users.put(userID.toString(), user);
		}
		
		return users;
	}
	
	private boolean isIn(String name, String... set) {
		for (String test : set)
			if (test.equalsIgnoreCase(name))
				return true;
		return false;
	}
	
	private ArrayList<String> getPrettyData(String canonicalName, Field data, Map<String, String> headers) {
		ArrayList<String> pretty = new ArrayList<String>();
		if (isIn(canonicalName, CanonicalNames.credits)) {
			//Sigh. Need to build the user text.
			RedListCreditedUserField proxy = new RedListCreditedUserField(data);
			List<Integer> userIDs = proxy.getUsers();
			if (userIDs.size() > 0) {
				try {
					HashMap<String, User> users = getUserInfo(userIDs);
					String value;
					if (users.isEmpty())
						value = proxy.getText();
					else
						value = generateTextFromUsers(new ArrayList<User>(users.values()), proxy.getOrder());
					
					if (!isBlank(value))
						pretty.add(value);
				} catch (PersistentException e) {
					Debug.println(e);
				}
			}
			else if (!isBlank(proxy.getText()))
				pretty.add(proxy.getText());
			
			if (pretty.isEmpty())
				pretty.add(BLANK_STRING);
		}
		else if (CanonicalNames.RegionExpertQuestions.equals(canonicalName)) {
			ArrayList<String> defaultValues = new ArrayList<String>();
			defaultValues.add(",-1,0");
			defaultValues.add(",-1");
			defaultValues.add(",0");
			defaultValues.add("");
			
			String value = data.getPrimitiveField().iterator().next().getRawValue();
			if (!(value == null || defaultValues.contains(value)))
				pretty.add(value);
		}
		else if (CanonicalNames.RegionInformation.equals(canonicalName)) {
			List<Integer> ids = new RegionField(data).getRegionIDs();
			
			Set<Region> regions = new HashSet<Region>();
			for (Integer id : ids) {
				Region region;
				try { 
					region = SISPersistentManager.instance().getObject(session, Region.class, id);
				} catch (PersistentException e) {
					continue;
				}
				if (region != null)
					regions.add(region);
			}
			
			StringBuilder csv = new StringBuilder();
			int size = regions.size();
			int index = 0;
			for (Region region : regions) {
				csv.append(region.getName());
				if (index + 1 < size)
					csv.append(index + 2 < size ? ", " : " & ");
				index++;
			}
			pretty.add(csv.toString());
		}
		else {
			for (String key : headers.keySet()) {
				PrimitiveField<?> prim = data.getPrimitiveField(key);
				String value = null;
				if (prim != null && !(prim.getRawValue() == null || "".equals(prim.getRawValue()))) {
					if (prim instanceof ForeignKeyPrimitiveField) {
						value = LookupLoader.get(canonicalName, key, ((ForeignKeyPrimitiveField)prim).getValue(), false);
					}
					else if (prim instanceof ForeignKeyListPrimitiveField) {
						StringBuilder b = new StringBuilder();
						List<Integer> values = ((ForeignKeyListPrimitiveField)prim).getValue();
						if (!values.isEmpty()) {
							for (int i = 0; i < values.size(); i++) {
								b.append(LookupLoader.get(canonicalName, key, values.get(i), false));
								if (i + 1 < values.size())
									b.append(", ");
							}
							value = b.toString();
						}
					}
					else if (prim instanceof BooleanRangePrimitiveField) {
						value = BooleanRangePrimitiveField.getDisplayString(prim.getRawValue());
					}
					else if (prim instanceof BooleanUnknownPrimitiveField) {
						Integer index = ((BooleanUnknownPrimitiveField)prim).getValue();
						value = BooleanUnknownPrimitiveField.getDisplayString(index, BLANK_STRING);  
					}
					else if (prim instanceof DatePrimitiveField) {
						value = FormattedDate.impl.getDate(((DatePrimitiveField)prim).getValue());
					}
					else if (prim instanceof FloatPrimitiveField) {
						Float valueF = Float.valueOf(prim.getRawValue());
						if (valueF == 0)
							value = "0";
						else {
							value = PortableCalculator.toString(valueF);
							String[] split = value.split("\\.");
							if (split.length == 1 || split[1].equals("00"))
								value = split[0];
						}
					}
					else {
						value = prim.getRawValue();
					}
				}
				
				if (value == null)
					value = BLANK_STRING;
				
				pretty.add(value);
			}
		}
		
		if (pretty.isEmpty())
			pretty.add(BLANK_STRING);
		
		return pretty;
	}
	
	private void debug(String template, Object... args) {
		//Debug.println(template, args);
	}
	
	private boolean isBlank(String value) {
		return "".equals(value) || BLANK_STRING.equals(value) || value == null;
	}

	private StringBuilder parseField(Field data, String canonicalName, String displayName, int dataIndent,
			Couple<DisplayData, Map<String, String>> displayData,
			boolean forceShowCanonicalName, boolean showTableHeader,
			boolean occurrenceHeadingReorder) {
		StringBuilder retHtml = new StringBuilder();
		if (data.getPrimitiveField().isEmpty() && !showEmptyFields)
			return retHtml;
		
		ArrayList<String> prettyData = getPrettyData(canonicalName, data, displayData.getSecond());
		
		// don't show empty fields if the preference is set
		if (!showEmptyFields) {
			boolean allEmptyFields = true;
			for (int i = 0; i < prettyData.size() && (allEmptyFields &= isBlank(prettyData.get(i))); i++);
			if (allEmptyFields && !CanonicalNames.RedListCriteria.equals(canonicalName) && !data.isClassificationScheme())
				return retHtml;
		}

		Map<String, String> headers = displayData.getSecond();
		
		// if we don't have all the data for a field, just print headers for the
		// data we have
		int numHeadersToUse = headers.size();
		if (prettyData.size() < numHeadersToUse)
			numHeadersToUse = prettyData.size();
		
		List<String> headersInUse = new ArrayList<String>();
		List<String> allHeaders = new ArrayList<String>(headers.values());
		for (int i = 0; i < numHeadersToUse && i < headers.size(); i++)
			headersInUse.add(allHeaders.get(i));
		
		boolean useTable = (headers.size() > 1);
		
		if (isIn(canonicalName, CanonicalNames.credits) && headersInUse.isEmpty())
				headersInUse.add(displayData.getFirst().getDescription());

		// Special cases for parsing
		if (canonicalName.equals(CanonicalNames.UseTradeDetails)) {
			retHtml = parseUseTrade(prettyData, headers);
			return retHtml;
		} else if (canonicalName.equals(CanonicalNames.RedListReasonsForChange)) {
			retHtml = parseReasonsForChange(prettyData);
			return retHtml;
		} else if (canonicalName.equals(CanonicalNames.RedListCriteria)) {
			// Red List status sets it's own header, don't need to do it
			// automatically
			parseRedListStatus(data, headers);
			return retHtml;
		} else if (canonicalName.contains("PopulationReduction") && 
				!isIn(canonicalName, CanonicalNames.PopulationReductionFuture, CanonicalNames.PopulationReductionPast, 
				CanonicalNames.PopulationReductionPastandFuture)) {
			// Print the main percentage in normal table, print basis and causes
			// in
			// specially formatted table
			
			retHtml = parsePopulationReduction(canonicalName, prettyData);
			return retHtml;
		} else if (curOrg.getTitle().equals("Life History") && !canonicalName.equals(CanonicalNames.GenerationLength)) {
			String header = headersInUse.get(0).toString();
			String tableData = prettyData.get(0).toString();
			if (tableData.length() == 0) {
				tableData = "-";
			}
			if (headersInUse.size() == 2 && headersInUse.get(1).equals("Units:")) {
				tableData += " " + prettyData.get(1);
			}
			if (canonicalName.equals(CanonicalNames.EggLaying)) {
				retHtml.append("<h2>Breeding Strategy</h2>\n");
			}
			retHtml.append(appendInlineTable(header, tableData));
			
			if (isIn(canonicalName, CanonicalNames.MaleMaturitySize, CanonicalNames.GestationTime, 
					CanonicalNames.NaturalMortality, CanonicalNames.Parthenogenesis))
				retHtml.append("<br/>");
			
			return retHtml;
		}
		
		if (useTable && !forceShowCanonicalName) {
			retHtml.append("<table>");
		}
		if (forceShowCanonicalName) {
			if (showTableHeader) {
				retHtml.append("<tr><th>");
				if (CanonicalNames.CountryOccurrence.equals(canonicalName)) {
					retHtml.append("Country");
				} else if (CanonicalNames.Threats.equals(canonicalName)) {
					retHtml.append("Threat");
				} else if (CanonicalNames.GeneralHabitats.equals(canonicalName)) {
					retHtml.append("Habitat");
				}
				else if (CanonicalNames.Research.equals(canonicalName))
					retHtml.append("Research");
				else if (CanonicalNames.ConservationActions.equals(canonicalName))
					retHtml.append("Conservation Actions");
				retHtml.append("</th>");
				for (int i = 0; i < numHeadersToUse; i++) {
					if (occurrenceHeadingReorder && (i == 1)) {
						retHtml.append("<th>" + headersInUse.get(i + 1) + "</th>");
						retHtml.append("<th>" + headersInUse.get(i) + "</th>");
						i++;
					} else if (CanonicalNames.Threats.equals(canonicalName)
							&& (headersInUse.get(i).toString().equals("Total Selected"))) {
						retHtml.append(""); // ignore the stresses column, will
						// be its own row
					} else {
						retHtml.append("<th>" + headersInUse.get(i) + "</th>");
					}
				}
				retHtml.append("</tr>\n");
			}
		} else if (useTable) {
			retHtml.append("<tr>");
			for (String header : headersInUse)
				retHtml.append("<th>" + header + "</th>");
			retHtml.append("</tr>\n");
		} else {
			for (String header : headersInUse) {
				if (!canonicalName.endsWith("Documentation") && !canonicalName.equalsIgnoreCase(CanonicalNames.RedListRationale)) {
					if (!(canonicalName.equals(CanonicalNames.RedListCaveat) || canonicalName.equals(CanonicalNames.RedListPetition))) {
						retHtml.append("<p>");
					}
					retHtml.append(createDataLabel(header));
				}
			}
		}
		if (forceShowCanonicalName) {
			retHtml.append("<tr><td ");
			if (!CanonicalNames.Threats.equals(canonicalName)) {
				retHtml.append("class=\"dataBorder\"");
			}
			retHtml.append(">" + displayName + "</td>");
		} else if (useTable) {
			retHtml.append("<tr>");
		}

		boolean actuallyData = false;
		for (int i = 0; i < prettyData.size(); i++) {
			String tempText = prettyData.get(i).toString();
			if (tempText.equals("")) {
				tempText = BLANK_STRING;
			} else if (!tempText.equals(BLANK_STRING))
				actuallyData = true;

			if (canonicalName.endsWith("Documentation") || canonicalName.equalsIgnoreCase(CanonicalNames.RedListRationale)) {
				if (!actuallyData && showEmptyFields) {
					retHtml.append("<p>" + createDataLabel(displayName));
					retHtml.append("<br/>" + tempText + "</p>");
				} else {
					retHtml.append("<p>" + tempText + "</p>");
				}
			} else {
				if (canonicalName.contains("Population") && headersInUse.get(i).toString().contains("Percent")
						&& !tempText.contains("%")) {
					tempText += "%";
				}
				// remove hh:mm:ss from "mm-dd-yyyy hh:mm:ss" formatted date
				else if (canonicalName.contains("Date") && tempText.length() == 19) {
					tempText = tempText.substring(0, 10);
				}
				if (forceShowCanonicalName || useTable) {
					if (tempText.equalsIgnoreCase(BLANK_STRING)) {
						tempText = "-";
					}
					if (occurrenceHeadingReorder && (i == 1)) {
						retHtml.append("<td class=\"dataBorder\">" + prettyData.get(i + 1) + "</td>");
						retHtml.append("<td class=\"dataBorder\">" + tempText + "</td>");
						i++;
					} else if (CanonicalNames.Threats.equals(canonicalName)) {
						if (i <= 4) {
							retHtml.append("<td>" + tempText + "</td>");
						} else if (i == 5) {
							retHtml
									.append("</tr>\n<tr><td colspan=\"6\" class=\"dataBorder\"><em>Total Stresses</em>: "
											+ tempText + " ");
						} else if (i == (prettyData.size() - 1)) {
							retHtml.append(tempText + "</td>");
						} else {
							retHtml.append(tempText + ", ");
						}
					} else {
						retHtml.append("<td ");
						if (!useTable || forceShowCanonicalName) {
							retHtml.append("class=\"dataBorder\"");
						}
						retHtml.append(">" + tempText + "</td>");
					}
				} else {
					retHtml.append(tempText);
					if (!canonicalName.equals("RedListCaveat")) {
						retHtml.append(" ");
					} else {
						retHtml.append("</p>");
					}
				}
			}
		}
		if (useTable && !forceShowCanonicalName)
			retHtml.append("</tr></table>\n");
		else if (forceShowCanonicalName)
			retHtml.append("</tr>\n");

		return retHtml;
	}

	private StringBuilder parsePageField(String canonicalName, Field data) {
		StringBuilder retHtml = new StringBuilder();
		if (exclude.contains(canonicalName))
			return retHtml;
		
		Couple<DisplayData, Map<String, String>> currentDisplayData = fetchDisplayData(canonicalName);
		if (data == null) {
			if (showEmptyFields) {
				String desc = currentDisplayData.getFirst().getDescription();
				if (desc == null || "".equals(desc))
					desc = canonicalName;
				retHtml.append("<p>" + createDataLabel(desc));
				retHtml.append(BLANK_STRING);
				retHtml.append("</p>\n");
			}
			return retHtml;
		}
		
		try {
			if (data.isClassificationScheme())
				retHtml = parseClassificationScheme(data, canonicalName, currentDisplayData);
			else
				retHtml = parseField(data, canonicalName, canonicalName, 8, currentDisplayData, 
						false, false, false);
		} catch (Exception e) {
			debug("DIED TRYING TO BUILD " + canonicalName);
			Debug.println(e);
		}
		return retHtml;
	}

	private StringBuilder parsePopulationReduction(String canonicalName, ArrayList<String> prettyData) {
		String heading = "";
		if (canonicalName.endsWith("Basis")) {
			heading = "Basis?";
		} else if (canonicalName.endsWith("Reversible")) {
			heading = "Reversible?";
		} else if (canonicalName.endsWith("Understood")) {
			heading = "Understood?";
		} else if (canonicalName.endsWith("Ceased")) {
			heading = "Ceased?";
		}
		return appendInlineTable(heading, prettyData.get(0).toString());
	}

	private StringBuilder parseReasonsForChange(ArrayList<String> prettyData) {
		StringBuilder retHtml = new StringBuilder();
		if (prettyData.size() != 5) {
			return retHtml;
		}
		retHtml.append(prettyData.get(0) + ": ");
		if (prettyData.get(0).equals("Genuine Change")) {
			retHtml.append(prettyData.get(1));
		} else if (prettyData.get(0).equals("Nongenuine Change")) {
			retHtml.append(prettyData.get(2));
			if (prettyData.get(2) == "Other") {
				retHtml.append(" - " + prettyData.get(3));
			}
		} else if (prettyData.get(0).equals("No change")) {
			retHtml.append(prettyData.get(4));
		}
		return retHtml;
	}
	
	private String toYesNo(Boolean value) {
		return Boolean.TRUE.equals(value) ? "Yes" : "No";
	}
	
	private String toCatString(String value) {
		if (value == null || "".equals(value))
			return "-";
		
		ResultCategory type = ResultCategory.fromString(value);
		return type == null ? value : type.getShortName() + " - " + type.getName();
	}

	private void parseRedListStatus(Field data, Map<String, String> headers) {
		final RedListCriteriaField proxy = new RedListCriteriaField(data);
		
		final StringBuilder retHtml = new StringBuilder();
		boolean isManual = proxy.isManual();
		String version = LookupLoader.get(data.getName(), RedListCriteriaField.CRIT_VERSION_KEY, proxy.getCriteriaVersion(), false);//prettyData.get(SISCategoryAndCriteria.CRIT_VERSION_INDEX).toString();
		String manualCategory = proxy.getManualCategory();
		String autoCategory = proxy.getGeneratedCategory();
		retHtml.append("<center><table id=\"redListStatus\"><tr><th colspan=\"2\">Red List Status</th></tr>\n");
		
		if ((isManual && isIn(manualCategory, "None", "")) || 
				(!isManual && isIn(autoCategory, "None", ""))) {
			retHtml.append("<tr><td colspan=\"2\">Red List category not determined</td></tr>\n");
			retHtml.append("</table></center>");
		} else {
			retHtml.append("<tr><td id=\"redListStatusInfo\" colspan=\"2\">");
			if (isManual) {
				retHtml.append(toCatString(manualCategory));
				retHtml.append(", ");
				retHtml.append(proxy.getManualCriteria());
			} else {
				retHtml.append(toCatString(autoCategory));
				retHtml.append(", ");
				retHtml.append(proxy.getGeneratedCriteria());
			}
			if (!isBlank(version)) {
				retHtml.append(" (IUCN version " + version + ")");
				retHtml.append("</td></tr>\n");
			}
			// if Manual or Auto Category is CR add the possibly extinct fields
			if ((isManual && manualCategory.startsWith("CR")) || (!isManual && autoCategory.startsWith("CR"))) {
				retHtml.append("<tr><td colspan=\"2\">&nbsp</td></tr><tr><td>");
				retHtml.append("Possibly Extinct" + ": ");
				retHtml.append("</td><td>");
				retHtml.append(toYesNo(proxy.isPossiblyExtinct()));
				retHtml.append("</td></tr>\n<tr><td>");
				retHtml.append("Possibly Extinct in the Wild" + ": ");
				retHtml.append("</td><td>");
				retHtml.append(toYesNo(proxy.isPossiblyExtinctCandidate()));
				retHtml.append("</td></tr>\n<tr><td>");
				retHtml.append("Year Last Seen" + ": ");
				retHtml.append("</td><td>");
				retHtml.append(proxy.getYearLastSeen());
				retHtml.append("</td></tr>\n");
			} else if( manualCategory.startsWith("DD") ) {
				retHtml.append("<tr><td colspan=\"2\">&nbsp</td></tr><tr><td>");
				retHtml.append("Data Deficient reason: " + ": ");
				retHtml.append("</td><td>");
				retHtml.append(proxy.getDataDeficient());
				retHtml.append("</td></tr>\n");
			}
			retHtml.append("</table></center>");
		}
		theHtml.append(retHtml);
	}

	private StringBuilder parseUseTrade(ArrayList<String> prettyData, Map<String, String> headers) {
		StringBuilder retHtml = new StringBuilder();
		//UseTrade utTemp = new UseTrade("", "");
		retHtml.append("<table><tr>");
		int colCount = 0;
		for (String header : headers.keySet()) {
			retHtml.append("<th>" + header + "</th>");
		}
		retHtml.append("</tr>");
		for (int i = 1; i < prettyData.size(); i++) {
			if (colCount == headers.size()) {
				retHtml.append("</tr>\n<tr>");
				colCount = 0;
			}
			String tempText = prettyData.get(i).toString();

			if (tempText.equals(""))
				tempText = BLANK_STRING;

			retHtml.append("<td class=\"dataBorder\">" + tempText + "</td>");
			colCount++;
		}
		retHtml.append("</tr></table>");
		return retHtml;
	}
	
	private Map<String, String> extractDescriptions(String canonicalName, String defaultDescription, NativeDocument document) {
		Map<String, String> map = new LinkedHashMap<String, String>();
		if (CanonicalNames.Threats.equals(canonicalName)) {
			map.put("timing", "Timing");
			map.put("scope", "Scope");
			map.put("severity", "Severity");
			map.put("score", "Impact Score");
			//TODO: stresses... list.add("No. of Stresses");
			
			return map;
		}
		NativeNodeList nodes = document.getDocumentElement().getElementsByTagName("structure");
		for (int i = 0; i < nodes.getLength(); i++) {
			NativeElement node = nodes.elementAt(i);
			String id = node.getAttribute("id");
			String description = node.getAttribute("description");
			if (description == null || "".equals(description))
				description = defaultDescription;
			if (id != null && !"".equals(id)) {
				if ("qualifier".equals(id))
					map.put("qualifier", "Qualifier");
				else if ("justification".equals(id))
					map.put("justification", "Justification");
				else if ("note".equals(id))
					map.put("note", "Note");
				else
					map.put(id, description);
			}
		}
		
		return map;
	}
	
	public static String createUrl(String resource) {
		return RESOURCE_LOCATION + resource;
	}
	
	public static abstract class TextComparator<T> implements Comparator<T> {
		
		private static final long serialVersionUID = 1L;
		
		private final AlphanumericComparator comparator = new AlphanumericComparator();
	
		public int compare(T arg0, T arg1) {
			return comparator.compare(getString(arg0), getString(arg1));
		}
		
		protected abstract String getString(T model);
		
	}
	
	public static class ViewNativeDocument extends JavaNativeDocument {
		
		public ViewNativeDocument(Document peer) {
			super();
			this.peer = peer;
		}
		
	}
	
}