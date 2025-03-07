package nl.architolk.rdfdiff;

import java.io.File;
import java.io.FileOutputStream;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.update.UpdateExecution;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Compare {

  // Works good for 'normal' triples and triples with a blank node structure not more than two levels deep

  static final String QUERY_UNIQUE1 = "INSERT { GRAPH <urn:unique1> {?s?p?o.?o?op?oo.?oo?oop?ooo}} WHERE {" +
          "{GRAPH <urn:model1> {?s?p?o FILTER(!isBlank(?s) && !isBlank(?o))} FILTER NOT EXISTS {GRAPH <urn:model2> {?s?p?o}}} UNION" +
          "{GRAPH <urn:model1> {?s?p?o.?o?op?oo FILTER(!isBlank(?s) && isBlank(?o) && !isBlank(?oo))} FILTER NOT EXISTS {GRAPH <urn:model2> {?s?p?o2.?o2?op?oo}}} UNION" +
          "{GRAPH <urn:model1> {?s?p?o.?o?op?oo.?oo?oop?ooo FILTER(isBlank(?o) && isBlank(?oo))} FILTER NOT EXISTS {GRAPH <urn:model2> {?s?p?o2.?o2?op?oo2.?oo2?oop?ooo}}}}";
  static final String QUERY_UNIQUE2 = "INSERT { GRAPH <urn:unique2> {?s?p?o.?o?op?oo.?oo?oop?ooo}} WHERE {" +
          "{GRAPH <urn:model2> {?s?p?o FILTER(!isBlank(?s) && !isBlank(?o))} FILTER NOT EXISTS {GRAPH <urn:model1> {?s?p?o}}} UNION" +
          "{GRAPH <urn:model2> {?s?p?o.?o?op?oo FILTER(!isBlank(?s) && isBlank(?o) && !isBlank(?oo))} FILTER NOT EXISTS {GRAPH <urn:model1> {?s?p?o2.?o2?op?oo}}} UNION" +
          "{GRAPH <urn:model2> {?s?p?o.?o?op?oo.?oo?oop?ooo FILTER(isBlank(?o) && isBlank(?oo))} FILTER NOT EXISTS {GRAPH <urn:model1> {?s?p?o2.?o2?op?oo2.?oo2?oop?ooo}}}}";
  static final String QUERY_PRESENT1 = "SELECT DISTINCT ?s WHERE { GRAPH <urn:unique1> {?s?p?o} FILTER NOT EXISTS {GRAPH <urn:model2> {?s?p2?o2}}}";
  static final String QUERY_PRESENT2 = "SELECT DISTINCT ?s WHERE { GRAPH <urn:unique2> {?s?p?o} FILTER NOT EXISTS {GRAPH <urn:model1> {?s?p2?o2}}}";

  private static final Logger LOG = LoggerFactory.getLogger(Compare.class);

  private static final Dataset dataset = DatasetFactory.create();;

  private static void printUniques(String name, String querystr) {
    System.out.println("Unique resources in model " + name);
    System.out.println("-------------------------");
    Query query = QueryFactory.create(querystr);
    ResultSet items = QueryExecution.dataset(dataset).query(query).build().execSelect();
    for (; items.hasNext() ;) {
      QuerySolution row = items.nextSolution();
      System.out.println(row.get("s"));
    }
    System.out.println(".");
  }

  public static void main(String[] args) {

    if (args.length >= 3) {

      LOG.info("Starting conversion");
      LOG.info("File #1: {}",args[0]);
      LOG.info("File #2: {}",args[1]);
      LOG.info("Output: {}",args[2]);

      try {
        Model firstModel = RDFDataMgr.loadModel(args[0]);
        Model secondModel = RDFDataMgr.loadModel(args[1]);

        dataset.addNamedModel("urn:model1",firstModel);
        dataset.addNamedModel("urn:model2",secondModel);

        try {
          LOG.info("Find unique values in model #1 (not in model #2)");
          UpdateRequest request1 = UpdateFactory.create(QUERY_UNIQUE1);
          UpdateExecution.dataset(dataset).update(request1).execute();

          LOG.info("Find unique values in model #2 (not in model #1)");
          UpdateRequest request2 = UpdateFactory.create(QUERY_UNIQUE2);
          UpdateExecution.dataset(dataset).update(request2).execute();

          dataset.commit();
        } finally {
          dataset.end();
        }

        if (args.length > 3) {
          if ("-c".equals(args[3])) {
            printUniques("1",QUERY_PRESENT1);
            printUniques("2",QUERY_PRESENT2);
          }
        }

        Model outModel1 = dataset.getNamedModel("urn:unique1");
        outModel1.setNsPrefixes(firstModel);
        RDFDataMgr.write(new FileOutputStream(args[2]+"_1.ttl"),outModel1, RDFLanguages.filenameToLang(args[0],RDFLanguages.JSONLD));

        Model outModel2 = dataset.getNamedModel("urn:unique2");
        outModel2.setNsPrefixes(secondModel);
        RDFDataMgr.write(new FileOutputStream(args[2]+"_2.ttl"),outModel2, RDFLanguages.filenameToLang(args[0],RDFLanguages.JSONLD));

        LOG.info("Done!");
      }
      catch (Exception e) {
        LOG.error(e.getMessage(),e);
      }
    } else {
      LOG.info("Usage: rdfdiff <first.ttl> <second.ttl> <output> [-c]");
    }
  }
}
