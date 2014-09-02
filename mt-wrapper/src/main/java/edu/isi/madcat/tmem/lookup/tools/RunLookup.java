package edu.isi.madcat.tmem.lookup.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.lookup.api.CorpusResultSet;
import edu.isi.madcat.tmem.lookup.api.DictionaryResultSet;
import edu.isi.madcat.tmem.lookup.api.LookupQuerySet;
import edu.isi.madcat.tmem.lookup.api.LookupTool;
import edu.isi.madcat.tmem.lookup.api.UserInfo;
import edu.isi.madcat.tmem.utils.CateInitializer;

public class RunLookup {
  private LookupTool lookupTool;
  private int maxDictionaryResults;
  private int maxCorpusResults;
  private List<String> queryStrings;
  private List<String> addStrings;
  private static Pattern numberPattern = Pattern.compile("^(\\d+)$");
  private static Pattern bilingualPattern = Pattern.compile("^(.*)\t(.*)$");
  private static Pattern addPattern = Pattern.compile("^(.*)\t(.*)\t(.*)\t(.*)$");
  
  public RunLookup(String[] args) {
    Options options = new Options();
    options.addOption("l", true, "lookup tool parameter file");
    options.addOption("n", true, "max dictionary results");
    options.addOption("m", true, "max corpus results");
    options.addOption("q", true, "query string");
    options.addOption("f", true, "query file");
    options.addOption("a", true, "dictionary add file");
    options.addOption("d", true, "delete this dictionary id");
    options.addOption("c", false, "clear user entries");
    CommandLineParser parser = new BasicParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      ExceptionHandler.handle(e);
    }
    System.out.println("*** Initializing lookup tool ***");
    String lookupToolFile = cmd.getOptionValue("l");
    maxDictionaryResults = 10;
    if (cmd.hasOption("n")) {
      maxDictionaryResults = Integer.parseInt(cmd.getOptionValue("n"));
    }
    maxCorpusResults = 10;
    if (cmd.hasOption("m")) {
      maxCorpusResults = Integer.parseInt(cmd.getOptionValue("m"));
    }
    lookupTool = new LookupTool(lookupToolFile);
    queryStrings = new ArrayList<String>();
    if (cmd.hasOption("c")) {
      System.out.println("*** Deleting all user terms ***");
      lookupTool.deleteAllUserTerms();
    }
    if (cmd.hasOption("d")) {
      int dictionaryId = Integer.parseInt(cmd.getOptionValue("d"));
      lookupTool.deleteTermByDictionaryId(dictionaryId);
    }
    if (cmd.hasOption("q")) {
      queryStrings.add(cmd.getOptionValue("q"));
    }
    if (cmd.hasOption("f")) {
      try {
        List<String> queryFileStrings = FileUtils.readLines(new File(cmd.getOptionValue("f")));
        queryStrings.addAll(queryFileStrings);
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      }
    }
    addStrings = new ArrayList<String>();
    if (cmd.hasOption("a")) {
      try {
        List<String> lines = FileUtils.readLines(new File(cmd.getOptionValue("a")));
        addStrings.addAll(lines);
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      }
    }
  }

  public static void main(String[] args) {
    CateInitializer.initialize();
    
    RunLookup processor = new RunLookup(args);
    processor.runLookup();
  }

  private void runLookup() {
    for (int i = 0; i < addStrings.size(); i++) {
      Matcher m = null;
      m = addPattern.matcher(addStrings.get(i));
      if (!m.find()) {
        throw new RuntimeException("Malformed add string: "+addStrings.get(i));
      }
      String sourceTerm = m.group(1);
      String sourceAcronym = m.group(2);
      String targetTerm = m.group(3);
      String targetAcronym = m.group(4);
      
      UserInfo userInfo = new UserInfo(1, 1);
      lookupTool.addTermToDictionary(sourceTerm, sourceAcronym, targetTerm, targetAcronym, userInfo);
    }
    for (int i = 0; i < queryStrings.size(); i++) {
      String queryString = queryStrings.get(i);
      LookupQuerySet querySet = buildLookupQuery(queryString);
      System.out.println("*** Running query [" + i + "]: " + queryString + "***");
      System.out.println("*** Dictionary results ***");
      DictionaryResultSet dictResults = lookupTool.dictionaryLookup(querySet, maxDictionaryResults);
      System.out.println(dictResults.toTextTable());
      System.out.println("\n");
      System.out.println("*** Corpus results ***");
      CorpusResultSet corpusResults = lookupTool.corpusLookup(querySet, maxCorpusResults);
      System.out.println(corpusResults.toTextTable());
    }
  }

  public LookupQuerySet buildLookupQuery(String rawTerm) {
    Matcher m = null;
    m = bilingualPattern.matcher(rawTerm);
    if (m.find()) {
      return lookupTool.buildBilingualQuery(m.group(1), m.group(2));
    }
    m = numberPattern.matcher(rawTerm);
    if (m.find()) {
      return lookupTool.buildQueryFromDictionaryId(Integer.parseInt(m.group(1)));
    }
    return lookupTool.buildMonolingualQuery(rawTerm);
  }
}
