package org.iucn.sis.shared.conversions;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.BatchUpdateException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.gogoego.api.plugins.GoGoEgo;
import org.hibernate.Session;
import org.iucn.sis.server.api.persistance.SISPersistentManager;
import org.iucn.sis.shared.api.debug.Debug;
import org.restlet.data.Form;

import com.solertium.mail.GMailer;

public abstract class Converter {
	
	protected final StringWriter localWriter;
	
	protected BufferedWriter writer;
	protected String lineBreakRule;
	
	protected Session session;
	
	protected Form parameters;
	private boolean clearSessionAfterTransaction;
	
	public Converter() {
		this.clearSessionAfterTransaction = false;
		this.localWriter = new StringWriter();
		this.parameters = new Form();
		
		setWriter(new PrintWriter(System.out));
		setLineBreakRule("\r\n");
	}
	
	public void setClearSessionAfterTransaction(
			boolean clearSessionAfterTransaction) {
		this.clearSessionAfterTransaction = clearSessionAfterTransaction;
	}
	
	public void setWriter(Writer writer) {
		this.writer = new BufferedWriter(writer);
	}
	
	public void setLineBreakRule(String lineBreakRule) {
		this.lineBreakRule = lineBreakRule;
	}
	
	public void setParameters(Form parameters) {
		this.parameters = parameters;
	}
	
	public boolean isEmailResults() {
		return "true".equals(parameters.getFirstValue("email", "false"));
	}
	
	public boolean start() {
		session = SISPersistentManager.instance().openSession();
		session.beginTransaction();
		
		Date start = Calendar.getInstance().getTime();
		printf("! -- Starting %s conversion at %s", getClass().getSimpleName(), start.toString());
		
		boolean success;
		try {
			run();
			success = true;
		} catch (Throwable e) {
			success = false;
			if (e.getCause() instanceof BatchUpdateException) {
				((BatchUpdateException) e.getCause()).getNextException().printStackTrace();
			} else {
				Debug.println(e);
				try {
					print("\n\n\n REALLY CAUSED BY:");
					Debug.println(e.getCause());
					print("\n\n\n REALLY REALLY CAUSED BY:");
					Debug.println(e.getCause().getCause());
				} catch (NullPointerException e1) {

				}
			}
			print(e.getMessage());
		}
		
		Date end = Calendar.getInstance().getTime();
		
		long millis = end.getTime() - start.getTime();
		millis = millis / 1000;
		
		if (success) {
			printf("! -- Finished %s conversion successfully in %s seconds at %s", getClass().getSimpleName(), millis, end.toString());
			try {
				session.getTransaction().commit();
			} catch (Exception e) {
				printf("Conversion successful, but transaction commit failed: %s", e.getMessage());
				e.printStackTrace();
				if (e.getCause() instanceof BatchUpdateException)
					try {
						((BatchUpdateException)e).getNextException().printStackTrace();
					} catch (NullPointerException f) {
						printf("Batch Update exception has no cause.");
					}
				success = false;
			}
		}
		else {
			printf("X -- Failed to finished %s conversion in %s seconds at %s", getClass().getSimpleName(), millis, start.toString());
			try {
				session.getTransaction().rollback();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (isEmailResults())
			emailResults(success);
		
		session.close();
		
		return success;
	}
	
	protected void commitAndStartTransaction() {
		session.getTransaction().commit();
		if (clearSessionAfterTransaction)
			session.clear();
		
		session.beginTransaction();
	}
	
	protected abstract void run() throws Exception;
	
	protected void print(String out) {
		try {
			writer.write(out + lineBreakRule);
			writer.flush();

			localWriter.write(out + lineBreakRule);
			localWriter.flush();
		} catch (IOException e) {
			System.out.println(out);
		}
	}
	
	protected void printf(String out, Object... args) {
		print(String.format(out, args));
	}

	protected void emailResults(boolean success) {
		Properties properties = GoGoEgo.getInitProperties();
		String[] required = new String[] {
			"org.iucn.sis.conversions.mail.account", 
			"org.iucn.sis.conversions.mail.password", 
			"org.iucn.sis.conversions.mail.recipient"
		};
		for (String s : required)
			if (properties.getProperty(s) == null)
				return;
		
		
		GMailer mailer = new GMailer(
			properties.getProperty("org.iucn.sis.conversions.mail.account"), 
			properties.getProperty("org.iucn.sis.conversions.mail.password")
		);
		mailer.setTo(properties.getProperty("org.iucn.sis.conversions.mail.recipient", "org.iucn.sis.conversions.mail.account"));
		mailer.setSubject(getClass().getSimpleName() + " SIS-1 -> SIS-2 Conversion Results");
		mailer.setBody("Success: " + success + lineBreakRule + lineBreakRule + 
			"Output: " + lineBreakRule + lineBreakRule + localWriter.toString());
		
		try {
			mailer.background_send();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
}
