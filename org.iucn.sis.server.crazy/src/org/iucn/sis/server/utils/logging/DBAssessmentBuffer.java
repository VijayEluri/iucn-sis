package org.iucn.sis.server.utils.logging;

import java.io.IOException;

import javax.naming.NamingException;

import org.iucn.sis.server.utils.DocumentUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.solertium.db.CString;
import com.solertium.db.DBException;
import com.solertium.db.ExecutionContext;
import com.solertium.db.Row;
import com.solertium.db.SystemExecutionContext;
import com.solertium.db.query.InsertQuery;

public class DBAssessmentBuffer extends EventBuffer {

	ExecutionContext ec;

	public DBAssessmentBuffer() throws DBException {
		try {
			Document structDoc = DocumentUtils.newDocumentBuilder().parse(
					EventLogger.class.getResourceAsStream("commitLog.xml"));
			ec = new SystemExecutionContext("default");
			ec.setExecutionLevel(ExecutionContext.ADMIN);
			ec.createStructure(structDoc);
		} catch (IOException iox) {
			throw new DBException("Database could not be initialized", iox);
		} catch (NamingException nx) {
			throw new DBException("Database could not be initialized", nx);
		} catch (SAXException sax) {
			throw new RuntimeException("Database structure could not be parsed; is your build corrupt?", sax);
		}
	}

	@Override
	public void flush() {
		for (int i = 0; i < getBufferSize(); i++) {
			Document e = events.get(i);
			Element event = (Element) e.getElementsByTagName("assessment").item(0);
			InsertQuery isql = new InsertQuery();
			Row row = new Row();
			row.add(new CString("ASSESSMENT", event.getTextContent()));
			row.add(new CString("STATUS", event.getAttribute("status")));
			row.add(new CString("USER", event.getAttribute("user")));
			row.add(new CString("DATE", event.getAttribute("date")));
			row.add(new CString("NAME", event.getAttribute("name")));

			isql.setRow(row);
			isql.setTable("commitLog");
			try {
				ec.doUpdate(isql);
			} catch (DBException dbException) {

			}
		}
		events.clear();
	}

}
