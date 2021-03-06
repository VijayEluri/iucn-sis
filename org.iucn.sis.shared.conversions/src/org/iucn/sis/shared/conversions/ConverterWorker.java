package org.iucn.sis.shared.conversions;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Properties;

import javax.naming.NamingException;

import org.iucn.sis.server.api.application.SIS;
import org.iucn.sis.server.api.utils.SISGlobalSettings;
import org.iucn.sis.shared.conversions.AssessmentConverter.ConversionMode;
import org.iucn.sis.shared.helpers.ServerPaths;
import org.iucn.sis.shared.migration.GlobalReferenceConverter;
import org.iucn.sis.shared.migration.LeftoverCountryConverter;
import org.iucn.sis.shared.migration.MovementPatternsConverter;
import org.iucn.sis.shared.migration.OccurrenceConverter;
import org.iucn.sis.shared.migration.RedListEvaluatedConverter;
import org.iucn.sis.shared.migration.StressesConverter;
import org.iucn.sis.shared.migration.SynonymConverter;
import org.restlet.data.Form;

import com.solertium.util.TrivialExceptionHandler;
import com.solertium.vfs.NotFoundException;
import com.solertium.vfs.VFS;
import com.solertium.vfs.VFSFactory;

public class ConverterWorker implements Runnable {
	
	public static boolean running = false;
	
	private final String step;
	private final boolean proceed;
	private final PrintWriter writer;
	private final Form parameters;
	
	protected VFS oldVFS;
	protected VFS newVFS;
	
	public ConverterWorker(PrintWriter writer, String step, boolean proceed, Form parameters) {
		this.step = step;
		this.proceed = proceed;
		this.writer = writer;
		this.parameters = parameters;
	}
	
	@Override
	public void run() {
		running = true;
		
		if (!init(proceed, writer)) {
			//arg1.setEntity(writer.toString(), MediaType.TEXT_PLAIN);
			running = false;
			return;
		}
		
		boolean success;
		if ("libraries".equals(step))
			success = convertLibrary(proceed, writer);
		else if ("definitions".equals(step))
			success = convertDefinitions(proceed, writer);
		else if ("permissions".equals(step))
			success = convertPermissions(proceed, writer);
		else if ("users".equals(step))
			success = convertUsers(proceed, writer);
		else if ("references".equals(step))
			success = convertReferences(proceed, writer);
		else if ("regions".equals(step))
			success = convertRegions(proceed, writer);
		else if ("taxa".equals(step))
			success = convertTaxa(proceed, writer);
		else if ("images".equals(step))
			success = convertImages(proceed, writer);
		else if ("draft".equals(step))
			success = convertDrafts(proceed, writer);
		else if ("published".equals(step))
			success = convertPublished(proceed, writer);
		else if ("attachments".equals(step))
			success = convertAttachments(proceed, writer);
		else if ("workingsets".equalsIgnoreCase(step))
			success = convertWorkingSets(proceed, writer);
		else if ("userworkingsets".equals(step))
			success = convertUserWorkingSets(proceed, writer);
		else if ("userrecent".equals(step))
			success = convertUserRecent(proceed, writer);
		else if ("globalreferences".equals(step))
			success = convertGlobalReferences(writer);
		else if ("synonyms".equals(step))
			success = convertSynonyms(writer);
		else if ("occurrence".equals(step))
			success = convertOccurrence(writer);
		else if ("redlistevaluated".equals(step))
			success = convertRedListEvaluated(writer);
		else if ("stresses".equals(step))
			success = convertStresses(writer);
		else if ("movementpatterns".equals(step))
			success = convertMovementPatterns(writer);
		else if ("legacycountries".equals(step))
			success = convertLegacyCountries(writer);
		else if ("950".equals(step))
			success = convertMissingAttachments950(writer);
		else if ("citations".equals(step))
			success = convertMissingCitations(writer);
		else if ("948".equals(step))
			success = convertTextData948(writer);
		else {
			success = true;
			writer.write("Conversion for " + step + " complete, cascade was " + proceed);
		}
			
		if (success) {
			writer.write("++ DONE, SUCCESS");
		}
		else {
			writer.write("-- Failed, see statements for more...");
		}
		
		writer.close();
		
		running = false;
	}

	@SuppressWarnings("deprecation")
	private boolean init(boolean proceed, Writer writer) {
		try {
			if (oldVFS == null) {
				Properties settings = SIS.get().getSettings(null);
				newVFS = VFSFactory.getVFS(settings.getProperty(SISGlobalSettings.VFS));
				oldVFS = VFSFactory.getVFS(settings.getProperty(Settings.OLD_VFS));
			}
		} catch (NotFoundException e) {
			die("Couldn't instantiate VFS's", e, writer);
			return false;
		}
		
		makeDirs();
		
		return true;
	}
	
	private boolean convertLibrary(boolean proceed, Writer writer) {
		LibraryGenerator converter = new LibraryGenerator();
		initConverter(converter, writer);
		converter.setData(getOldVFSPath());
		
		return converter.start() && (!proceed || convertDefinitions(proceed, writer));
	}
	
	private boolean convertDefinitions(boolean proceed, Writer writer) {
		DefinitionConverter converter = new DefinitionConverter();
		initConverter(converter, writer);
		converter.setData(getOldVFSPath());
		
		return converter.start() && (!proceed || convertPermissions(proceed, writer));
	}
	
	private boolean convertPermissions(boolean proceed, Writer writer) {
		PermissionConverter converter = new PermissionConverter();
		initConverter(converter, writer);
		converter.setData(getOldVFSPath());
		
		return converter.start() && (!proceed || convertUsers(proceed, writer));
	}
	
	private boolean convertUsers(boolean proceed, Writer writer) {
		UserConvertor converter = new UserConvertor();
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start() && (!proceed || convertReferences(proceed, writer));
	}
	
	private boolean convertReferences(boolean proceed, Writer writer) {
		ReferenceConverter converter = new ReferenceConverter();
		initConverter(converter, writer);
		
		return converter.start() && (!proceed || convertRegions(proceed, writer));
	}
	
	private boolean convertRegions(boolean proceed, Writer writer) {
		RegionConverter converter = new RegionConverter();
		initConverter(converter, writer);
		converter.setData(getOldVFSPath());
		
		return converter.start() && (!proceed || convertTaxa(proceed, writer));
	}
	
	private boolean convertTaxa(boolean proceed, Writer writer) {
		TaxonConverter converter = new TaxonConverter();
		initConverter(converter, writer);
		converter.setData(getOldVFSPath());
		
		return converter.start() && (!proceed || convertImages(proceed, writer));
	}
	
	private boolean convertImages(boolean proceed, Writer writer) {
		TaxonImageConverter converter = new TaxonImageConverter();
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start() && (!proceed || convertDrafts(proceed, writer));
	}
	
	private boolean convertDrafts(boolean proceed, Writer writer) {
		AssessmentConverter converter;
		try {
			converter = new AssessmentConverter();
		} catch (NamingException e) {
			die("Failed to locate lookup database", e, writer);
			return false;
		}
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		converter.setConversionMode(ConversionMode.DRAFT);
		
		return converter.start() && (!proceed || convertPublished(proceed, writer));
	}
	
	private boolean convertPublished(boolean proceed, Writer writer) {
		AssessmentConverter converter;
		try {
			converter = new AssessmentConverter();
		} catch (NamingException e) {
			die("Failed to locate lookup database", e, writer);
			return false;
		}
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		converter.setConversionMode(ConversionMode.PUBLISHED);
		
		return converter.start() && (!proceed || convertAttachments(proceed, writer));
	}
	
	private boolean convertAttachments(boolean proceed, Writer writer) {
		AttachmentConverter converter = new AttachmentConverter();
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start() && (!proceed || convertWorkingSets(proceed, writer));
	}
	
	private boolean convertWorkingSets(boolean proceed, Writer writer) {
		WorkingSetConverter converter = new WorkingSetConverter();
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start() && (!proceed || convertUserWorkingSets(proceed, writer));
	}
	
	private boolean convertUserWorkingSets(boolean proceed, Writer writer) {
		UserWorkingSetConverter converter = new UserWorkingSetConverter();
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start() && (!proceed || convertUserRecent(proceed, writer));
	}
	
	private boolean convertUserRecent(boolean proceed, Writer writer) {
		RecentlyViewedConverter converter = new RecentlyViewedConverter();
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start();
	}
	
	private boolean convertGlobalReferences(Writer writer) {
		GlobalReferenceConverter converter = new GlobalReferenceConverter();
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start();
	}
	
	private boolean convertSynonyms(Writer writer) {
		SynonymConverter converter = new SynonymConverter();
		initConverter(converter, writer);
		converter.setData(getOldVFSPath());
		
		return converter.start();
	}
	
	private boolean convertOccurrence(Writer writer) {
		OccurrenceConverter converter;
		try {
			converter = new OccurrenceConverter();
		} catch (NamingException e) {
			die("Failed to locate lookup database", e, writer);
			return false;
		}
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start();
	}
	
	private boolean convertRedListEvaluated(Writer writer) {
		RedListEvaluatedConverter converter;
		try {
			converter = new RedListEvaluatedConverter();
		} catch (NamingException e) {
			die("Failed to locate lookup database", e, writer);
			return false;
		}
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start();
	}
	
	private boolean convertStresses(Writer writer) {
		StressesConverter converter;
		try {
			converter = new StressesConverter();
		} catch (NamingException e) {
			die("Failed to locate lookup database", e, writer);
			return false;
		}
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start();
	}
	
	private boolean convertMovementPatterns(Writer writer) {
		MovementPatternsConverter converter;
		try {
			converter = new MovementPatternsConverter();
		} catch (NamingException e) {
			die("Failed to locate lookup database", e, writer);
			return false;
		}
		initConverter(converter, writer);
		converter.setData(new VFSInfo(getOldVFSPath(), oldVFS, newVFS));
		
		return converter.start();
	}
	
	private boolean convertLegacyCountries(Writer writer) {
		LeftoverCountryConverter converter;
		try {
			converter = new LeftoverCountryConverter();
		} catch (NamingException e) {
			die("Failed to locate lookup database", e, writer);
			return false;
		}
		initConverter(converter, writer);
		
		return converter.start();
	}
	
	private boolean convertMissingAttachments950(Writer writer) {
		MissingAttachments950Converter converter = new MissingAttachments950Converter();
		initConverter(converter, writer);
		
		return converter.start();
	}
	
	private boolean convertMissingCitations(Writer writer) {
		CitationConverter converter = new CitationConverter();
		initConverter(converter, writer);
		
		return converter.start();
	}
	
	private boolean convertTextData948(Writer writer) {
		CorrectTextFields converter = new CorrectTextFields();
		initConverter(converter, writer);
		
		return converter.start();
	}
	
	private String getOldVFSPath() {
		return SIS.get().getSettings(null).getProperty(Settings.OLD_VFS_PATH);
	}
	
	private void initConverter(Converter converter, Writer writer) {
		converter.setWriter(writer);
		converter.setLineBreakRule("\r\n");
		converter.setParameters(parameters);
	}
	
	private void die(String message, Throwable e, Writer writer) {
		String out = message;
		if (e != null)
			out += "\r\n" + e.getMessage();
		try {
			writer.write(out);
			writer.flush();
		} catch (IOException f) {
			TrivialExceptionHandler.ignore(this, f);
		}
	}

	protected void makeDirs() {
		String[] dirs = new String[] { ServerPaths.getAssessmentRootURL(), ServerPaths.getUserRootPath(),
				ServerPaths.getTaxonRootURL() };

		for (String dir : dirs) {
			File file = new File(SIS.get().getSettings(null).getProperty(Settings.NEW_VFS_PATH) + "/HEAD" + dir);
			if (!file.exists())
				file.mkdirs();
		}
	}
	
}
