import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;

import com.solertium.db.CanonicalColumnName;
import com.solertium.db.DBException;
import com.solertium.db.DBSessionFactory;
import com.solertium.db.ExecutionContext;
import com.solertium.db.SystemExecutionContext;
import com.solertium.db.TableCopier;
import com.solertium.db.query.SelectQuery;
import com.solertium.db.vendor.PostgreSQLDBSession;
import com.solertium.util.AlphanumericComparator;
import com.solertium.util.TrivialExceptionHandler;

public class CloneLookupsToPostgres {

	public static void main(String[] args) throws Exception {
		
		DBSessionFactory.registerDataSource(
				"source",
				"jdbc:postgresql://localhost:5435/sis_lookups",
				"org.postgresql.Driver",
				"???",
				"???"
		);
		
		DBSessionFactory.registerDataSource(
				"target",
				"jdbc:postgresql://localhost:5435/targetdb",
				"org.postgresql.Driver",
				"???",
				"???"
		);

		System.out.println("Connecting to source database.");
		ExecutionContext ecsource = new SystemExecutionContext("source");
		ecsource.setExecutionLevel(ExecutionContext.ADMIN);
		
		Document structDoc = ecsource.analyzeExistingStructure();
		
		List<String> tables = ecsource.getDBSession().listTables(ecsource);
		System.out.println("Connecting to target database.");
		ExecutionContext ectarget = new SystemExecutionContext("target");
		ectarget.setExecutionLevel(ExecutionContext.ADMIN);
		ectarget.setAPILevel(ExecutionContext.SQL_ALLOWED);
		ectarget.getDBSession().setSchema("lookups");

		System.out.println("Applying new structure");
		Collections.sort(tables, new AlphanumericComparator());
		for(String tn : tables){
			try{
				ectarget.dropTable(tn);
			    System.out.println("Dropped existing table:" + tn);
			} catch (DBException reallyNotAProblem) {
				// this is OK.  Really.
				ectarget.doUpdate(String.format("%s lookups.\"%s\"", 
					(ectarget.getDBSession() instanceof PostgreSQLDBSession ? "TRUNCATE " : "DELETE FROM "), 
					tn));
			}
		}
		ectarget.createStructure(structDoc);

		for(String tn : tables){
			System.out.println("Export table:" + tn);
			SelectQuery sq = new SelectQuery();
	    	sq.select(new CanonicalColumnName(tn,"*"));
			ecsource.doQuery(sq, new TableCopier(ectarget, tn, null, 0));
		}
		
		System.out.println("Creating indices and access privileges...");
		for (String tn : tables) {
			List<String> queries = new ArrayList<String>();
			queries.add("GRANT SELECT ON lookups.\"" + tn + "\" TO PUBLIC");
			queries.add("CREATE INDEX idx_" + tn + " ON lookups.\"" + tn + "\" (\"ID\")");
			if (tn.endsWith("LOOKUP")) {
				queries.add("CREATE INDEX idx_" + tn + "_code ON lookups.\"" + tn + "\" (\"CODE\")");
				queries.add("CREATE INDEX idx_" + tn + "_parentid ON lookups.\"" + tn + "\" (\"PARENTID\")");
			}
			for (String query : queries) {
				try {
					ectarget.doUpdate(query);
				} catch (Exception e) {
					TrivialExceptionHandler.ignore(ectarget, e);
				}
			}
		}
		
	}
	
}