package edu.isi.madcat.tmem.lookup.tools;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;
import edu.isi.madcat.tmem.utils.Utils;

public class ExtractDictionaryFromTmx {
  private String[] inputFiles;
  private Tokenizer sourceTokenizer;
  private Tokenizer targetTokenizer;
  private String outputFile;

  public ExtractDictionaryFromTmx(ParameterMap params) {
    inputFiles = params.getStringArrayRequired("input_files");
    String sourceTokenizerName = params.getStringRequired("source_tokenizer_name");
    String sourceTokenizerParamFile = params.getString("source_tokenizer_param_file");
    String targetTokenizerName = params.getStringRequired("target_tokenizer_name");
    String targetTokenizerParamFile = params.getString("target_tokenizer_param_file");
    outputFile = params.getString("output_file");

    sourceTokenizer = TokenizerFactory.create(sourceTokenizerName, sourceTokenizerParamFile);
    targetTokenizer = TokenizerFactory.create(targetTokenizerName, targetTokenizerParamFile);
  }

  class PrelimBiterm {
    @Override
    public String toString() {
      return "PrelimBiterm [rawSource=" + rawSource + ", rawTarget=" + rawTarget + ", tokSource="
          + tokSource + ", tokTarget=" + tokTarget + ", collectionName=" + collectionName + "]";
    }

    public String getRawSource() {
      return rawSource;
    }

    public String getRawTarget() {
      return rawTarget;
    }

    public List<String> getTokSource() {
      return tokSource;
    }

    public List<String> getTokTarget() {
      return tokTarget;
    }

    public String getCollectionName() {
      return collectionName;
    }

    private String rawSource;
    private String rawTarget;
    private List<String> tokSource;
    private List<String> tokTarget;
    private String collectionName;

    public PrelimBiterm(String rawSource, String rawTarget, List<String> tokSource,
        List<String> tokTarget, String collectionName) {
      super();
      this.rawSource = rawSource;
      this.rawTarget = rawTarget;
      this.tokSource = tokSource;
      this.tokTarget = tokTarget;
      this.collectionName = collectionName;
    }
  }

  class Biterm {
    public void setCollection(String collection) {
      this.collection = collection;
    }

    public String getCollection() {
      return collection;
    }

    public String getSourceTermRaw() {
      return sourceTermRaw;
    }

    public String getSourceTermTok() {
      return sourceTermTok;
    }

    public String getSourceAcronymRaw() {
      return sourceAcronymRaw;
    }

    public String getSourceAcronymTok() {
      return sourceAcronymTok;
    }

    public String getTargetTermRaw() {
      return targetTermRaw;
    }

    public String getTargetTermTok() {
      return targetTermTok;
    }

    public String getTargetAcronymRaw() {
      return targetAcronymRaw;
    }

    public String getTargetAcronymTok() {
      return targetAcronymTok;
    }

    private String sourceTermRaw;
    private String sourceTermTok;
    private String sourceAcronymRaw;
    private String sourceAcronymTok;
    private String targetTermRaw;
    private String targetTermTok;
    private String targetAcronymRaw;
    private String targetAcronymTok;
    private String collection;

    private void setTerms(PrelimBiterm pb) {
      this.sourceTermRaw = pb.getRawSource();
      this.sourceTermTok = StringUtils.join(pb.getTokSource(), " ");
      this.targetTermRaw = pb.getRawTarget();
      this.targetTermTok = StringUtils.join(pb.getTokTarget(), " ");
      this.sourceAcronymRaw = "";
      this.sourceAcronymTok = "";
      this.targetAcronymRaw = "";
      this.targetAcronymTok = "";
    }

    public Biterm(PrelimBiterm pb) {
      super();
      setTerms(pb);
    }

    public Biterm(PrelimBiterm pb, String sourceAcronym, String targetAcronym) {
      super();
      setTerms(pb);
      if (sourceAcronym != null && !sourceAcronym.equals("")) {
        this.sourceAcronymRaw = sourceAcronym;
        this.sourceAcronymTok = StringUtils.join(sourceTokenizer.tokenize(sourceAcronym), " ");
      }
      if (targetAcronym != null && !targetAcronym.equals("")) {
        this.targetAcronymRaw = targetAcronym;
        this.targetAcronymTok = StringUtils.join(targetTokenizer.tokenize(targetAcronym), " ");
      }
    }
  }

  public void process() {
    List<Biterm> allBiterms = new ArrayList<Biterm>();
    for (String inputFile : inputFiles) {
      TextSegmentIterator segIt = new TextSegmentIterator(inputFile);
      TextSegment segment = null;
      String collectionName = null;
      Map<String, List<PrelimBiterm>> sourceToBiterm = new HashMap<String, List<PrelimBiterm>>();
      while ((segment = segIt.next()) != null) {
        String rawSource = segment.getRequired("RAW_SOURCE");
        String rawTarget = segment.getRequired("RAW_TARGET");
        List<String> tokSource = sourceTokenizer.tokenize(rawSource);
        List<String> tokTarget = targetTokenizer.tokenize(rawTarget);
        collectionName = segment.getRequired("COLLECTION");
        PrelimBiterm biterm =
            new PrelimBiterm(rawSource, rawTarget, tokSource, tokTarget, collectionName);
        List<PrelimBiterm> pbs = sourceToBiterm.get(rawSource);
        if (pbs == null) {
          pbs = new ArrayList<PrelimBiterm>();
          sourceToBiterm.put(rawSource, pbs);
        }
        pbs.add(biterm);
      }
      List<Biterm> biterms = new ArrayList<Biterm>();
      for (Map.Entry<String, List<PrelimBiterm>> entry : sourceToBiterm.entrySet()) {
        List<PrelimBiterm> pbs = getUniqueEntries(entry.getValue());
        if (pbs.size() == 0) {

        }
        if (pbs.size() == 1) {
          Biterm biterm = new Biterm(pbs.get(0));
          biterms.add(biterm);
        } else if (pbs.size() == 2) {
          PrelimBiterm pb1 = pbs.get(0);
          PrelimBiterm pb2 = pbs.get(1);

          PrelimBiterm acronymPb = null;
          PrelimBiterm fullPb = null;
          if (couldBeAcronym(pb1.getRawTarget(), pb2.getRawTarget())) {
            acronymPb = pb1;
            fullPb = pb2;
          } else if (couldBeAcronym(pb2.getRawTarget(), pb1.getRawTarget())) {
            acronymPb = pb2;
            fullPb = pb1;
          }

          if (acronymPb != null) {
            // System.out.println("*** Match ***");
            // System.out.println("Acronym");
            // System.out.println(acronymPb.getRawTarget());
            // System.out.println("Full");
            // System.out.println(fullPb.getRawTarget());
            // System.out.println("");
            Biterm biterm = new Biterm(fullPb, "", acronymPb.getRawTarget());
            biterms.add(biterm);
          } else {
            biterms.add(new Biterm(pb1, "", ""));
            biterms.add(new Biterm(pb2, "", ""));
          }
        } else {
          Set<Integer> addedItems = new HashSet<Integer>();
          for (int i = 0; i < pbs.size(); i++) {
            for (int j = 0; j < pbs.size(); j++) {
              if (i == j) {
                continue;
              }
              PrelimBiterm pb1 = pbs.get(i);
              PrelimBiterm pb2 = pbs.get(j);
              if (isLikelyAcronym(pb1.getRawTarget(), pb2.getRawTarget())) {
                // System.out.println("*** Likely Match ***");
                // System.out.println("Acronym");
                // System.out.println(pb1.getRawTarget());
                // System.out.println("Full");
                // System.out.println(pb2.getRawTarget());
                // System.out.println("");
                Biterm biterm = new Biterm(pb2, "", pb1.getRawTarget());
                biterms.add(biterm);
                addedItems.add(i);
                addedItems.add(j);
              }
            }
          }
          for (int i = 0; i < pbs.size(); i++) {
            if (addedItems.contains(i)) {
              continue;
            }
            Biterm biterm = new Biterm(pbs.get(i), "", "");
            biterms.add(biterm);
          }
        }
      }
      for (Biterm biterm : biterms) {
        biterm.setCollection(collectionName);
        allBiterms.add(biterm);
      }
    }

    // De-duplicate biterms
    List<Biterm> filteredBiterms = new ArrayList<Biterm>();
    Map<String, List<Biterm>> sourceToBiterm = new HashMap<String, List<Biterm>>();
    for (Biterm biterm : allBiterms) {
      String key = StringUtils.replace(biterm.getSourceTermTok(), " ", "");
      List<Biterm> bitermList = sourceToBiterm.get(key);
      if (bitermList == null) {
        bitermList = new ArrayList<Biterm>();
        sourceToBiterm.put(key, bitermList);
      }
      bitermList.add(biterm);
    }

    for (Map.Entry<String, List<Biterm>> sourceEntry : sourceToBiterm.entrySet()) {
      List<Biterm> keptBiterms = new ArrayList<Biterm>();
      List<Biterm> biterms = sourceEntry.getValue();
      Map<String, List<Integer>> targetToBiterm = new HashMap<String, List<Integer>>();
      for (int i = 0; i < biterms.size(); i++) {
        Biterm biterm = biterms.get(i);
        String key = StringUtils.replace(biterm.getTargetTermTok(), " ", "");
        List<Integer> bitermList = targetToBiterm.get(key);
        if (bitermList == null) {
          bitermList = new ArrayList<Integer>();
          targetToBiterm.put(key, bitermList);
        }
        bitermList.add(i);
      }
      // If there are duplicate targets, keep the one with the acronym
      for (Map.Entry<String, List<Integer>> targetEntry : targetToBiterm.entrySet()) {
        List<Integer> bitermIndexes = targetEntry.getValue();
        int indexToKeep = -1;
        for (int i = 0; i < bitermIndexes.size(); i++) {
          int index = bitermIndexes.get(i);
          Biterm biterm = biterms.get(index);
          if (indexToKeep == -1 || biterm.getTargetAcronymTok().length() > 0) {
            indexToKeep = index;
          }
        }
        if (indexToKeep != -1) {
          keptBiterms.add(biterms.get(indexToKeep));
        }
      }
      for (int i = 0; i < keptBiterms.size(); i++) {
        Biterm b1 = biterms.get(i);
        boolean doKeep = true;
        for (int j = 0; j < keptBiterms.size(); j++) {
          Biterm b2 = biterms.get(j);
          if (i == j) {
            continue;
          }
          // If b1 doesn't have an acronym, and its term is equal to another term's acronym, then it's really a duplicate
          if (b1.getTargetAcronymTok().length() == 0) {
            if (b1.getTargetTermTok().equals(b2.getTargetAcronymTok())) {
              doKeep = false;
            }
          }
        }
        if (doKeep) {
          filteredBiterms.add(b1);
        }
      }
    }
    // Write biterms
    Writer writer = Utils.createWriter(outputFile);
    for (Biterm biterm : filteredBiterms) {
      TextSegment segment = new TextSegment();
      segment.insert("SOURCE_TERM_RAW", biterm.getSourceTermRaw());
      segment.insert("SOURCE_TERM_TOK", biterm.getSourceTermTok());
      segment.insert("SOURCE_ACRONYM_RAW", biterm.getSourceAcronymRaw());
      segment.insert("SOURCE_ACRONYM_TOK", biterm.getSourceAcronymTok());
      segment.insert("TARGET_TERM_RAW", biterm.getTargetTermRaw());
      segment.insert("TARGET_TERM_TOK", biterm.getTargetTermTok());
      segment.insert("TARGET_ACRONYM_RAW", biterm.getTargetAcronymRaw());
      segment.insert("TARGET_ACRONYM_TOK", biterm.getTargetAcronymTok());
      segment.insert("COLLECTION", biterm.getCollection());
      segment.write(writer);
    }
    IOUtils.closeQuietly(writer);
  }

  private boolean isLikelyAcronym(String acronym, String full) {
    // If it's all lower case, it's not an acronym
    if (acronym.toLowerCase().equals(acronym)) {
      return false;
    }
    // If it's not at least half the length of the full string, it's not an acronym
    if ((double) full.length() / (double) 2.0 < (double) acronym.length()) {
      return false;
    }
    // If 80% of the letters are in the other item, it's an acronym
    String lcAcronym = acronym.toLowerCase();
    String lcFull = full.toLowerCase();

    int numMatch = 0;
    for (int i = 0; i < lcAcronym.length(); i++) {
      int c = lcAcronym.charAt(i);
      if (lcFull.indexOf(c) != -1) {
        numMatch++;
      }
    }
    if ((double) numMatch / (double) acronym.length() >= 0.9) {
      return true;
    }
    // Otherwise it's not
    return false;
  }

  private boolean couldBeAcronym(String acronym, String full) {
    if (acronym.length() >= full.length()) {
      return false;
    }
    // If it's all lower case, it's not an acronym
    if (acronym.toLowerCase().equals(acronym)) {
      return false;
    }
    // If it's all upper case, it is an acronym
    if (acronym.toUpperCase().equals(acronym)) {
      return true;
    }
    // If its shorter than half the length of the full, it is an acronym
    if (full.length() / 2 >= acronym.length()) {
      return true;
    }
    // Otherwise it's not
    return false;
  }

  private List<PrelimBiterm> getUniqueEntries(List<PrelimBiterm> input) {
    List<PrelimBiterm> uniqueItems = new ArrayList<PrelimBiterm>();
    Map<String, PrelimBiterm> uniqueItemMap = new HashMap<String, PrelimBiterm>();
    for (PrelimBiterm pb : input) {
      uniqueItemMap.put(pb.getRawTarget(), pb);
    }
    for (Map.Entry<String, PrelimBiterm> entry : uniqueItemMap.entrySet()) {
      uniqueItems.add(entry.getValue());
    }
    List<PrelimBiterm> filteredItems = new ArrayList<PrelimBiterm>();
    if (uniqueItems.size() <= 2) {
      filteredItems = uniqueItems;
    } else {
      for (int i = 0; i < uniqueItems.size(); i++) {
        boolean doesContainOther = false;
        for (int j = 0; j < uniqueItems.size(); j++) {
          if (i == j) {
            continue;
          }
          String t1 = uniqueItems.get(i).getRawTarget();
          String t2 = uniqueItems.get(j).getRawTarget();
          if (t1.indexOf(t2) != -1) {
            doesContainOther = true;
          }
        }
        if (!doesContainOther) {
          filteredItems.add(uniqueItems.get(i));
        }
      }
    }
    return filteredItems;
  }

  public static void main(String[] args) {
    CateInitializer.initialize();
    
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    ExtractDictionaryFromTmx processor = new ExtractDictionaryFromTmx(params);
    processor.process();
  }
}
