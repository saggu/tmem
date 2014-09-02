package edu.isi.madcat.tmem.lookup.tools;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.lookup.CorpusTerm;
import edu.isi.madcat.tmem.lookup.CorpusTermExtractor;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;
import edu.isi.madcat.tmem.utils.Utils;

public class ExtractDictionaryFromCorpus {
  private String[] inputCorpusFiles;
  private int maxNgramSize;
  private double minFreqToKeep;
  private double minProbToKeep;
  private String outputDictionaryFile;
  private String outputCollectionFile;
  private String baselineDictionaryFile;
  private Set<String> functionWords;
  private Set<String> baselineDictionary;
  
  static Comparator<CorpusTerm> corpusTermComp;

  static {
    corpusTermComp = new Comparator<CorpusTerm>() {
      public int compare(CorpusTerm c1, CorpusTerm c2) {
        return c1.getTokJoint().length() - c2.getTokJoint().length(); // use your logic
      }
    };
  }

  public ExtractDictionaryFromCorpus(ParameterMap params) {
    inputCorpusFiles = params.getStringArrayRequired("input_corpus_files");
    maxNgramSize = params.getIntRequired("max_ngram_size");
    minFreqToKeep = params.getDoubleRequired("min_freq_to_keep");
    minProbToKeep = params.getDoubleRequired("min_prob_to_keep");
    baselineDictionaryFile = params.getString("baseline_dictionary_file");
    String functionWordFile = params.getString("function_word_file");
    outputDictionaryFile = params.getStringRequired("output_dictionary_file");
    outputCollectionFile = params.getStringRequired("output_collection_file");

    functionWords = null;
    if (!functionWordFile.equals("")) {
      try {
        functionWords = new HashSet<String>();
        List<String> lines = FileUtils.readLines(new File(functionWordFile));
        for (String line : lines) {
          functionWords.add(line);
        }
      } catch (IOException e) {
        ExceptionHandler.handle(e);
      }
    }
    baselineDictionary = null;
    if (!baselineDictionaryFile.equals("")) {
      baselineDictionary = new HashSet<String>();
      TextSegmentIterator segIt = new TextSegmentIterator(baselineDictionaryFile);
      TextSegment segment = null;
      while ((segment = segIt.next()) != null) {
        String dictTerm = getDictKey(segment.getRequired("SOURCE_TERM_TOK"), segment.getRequired("TARGET_TERM_TOK"));
        baselineDictionary.add(dictTerm);
      }
      segIt.close();
    }
  }

  private String getDictKey(String key1, String key2) {
    key1 = StringUtils.replace(key1, " ", "");
    key2 = StringUtils.replace(key2, " ", "");
    return "SOURCE= "+key1+"; TARGET= "+key2;
  }

  public void process() {
    TextSegment segment = null;
    Map<String, Double> allJointCounts = new HashMap<String, Double>();

    CorpusTermExtractor extractor = new CorpusTermExtractor(maxNgramSize, true);
    TextSegmentIterator segIt = new TextSegmentIterator(inputCorpusFiles);
    for (int segmentCount = 0; (segment = segIt.next()) != null; segmentCount++) {
      if (segmentCount % 1000 == 0) {
        System.out.println("Pass 1: Processing segment " + segmentCount);
      }
      List<CorpusTerm> corpusTerms = extractor.extractTerms(segment);
      for (CorpusTerm term : corpusTerms) {
        Double jointCount = allJointCounts.get(term.getTokJoint());
        if (jointCount == null) {
          jointCount = new Double(0.0);
        }
        jointCount = new Double(1.0 + jointCount.doubleValue());
        allJointCounts.put(term.getTokJoint(), jointCount);
      }
    }
    segIt.close();
    System.out.println("Total Terms: " + allJointCounts.size());
    Map<String, Double> jointCounts = new HashMap<String, Double>();
    for (Map.Entry<String, Double> e : allJointCounts.entrySet()) {
      String key = e.getKey();
      double count = e.getValue();
      if (count >= minFreqToKeep) {
        jointCounts.put(key, count);
      }
    }
    System.out.println("After Joint Pruning: " + jointCounts.size());
    allJointCounts = null; // free for gc

    Map<String, Double> sourceCounts = new HashMap<String, Double>();
    Map<String, Double> targetCounts = new HashMap<String, Double>();
    Map<String, CorpusTerm> jointToCorpusTerms = new HashMap<String, CorpusTerm>();

    segIt = new TextSegmentIterator(inputCorpusFiles);
    for (int segmentCount = 0; (segment = segIt.next()) != null; segmentCount++) {
      if (segmentCount % 1000 == 0) {
        System.out.println("Pass 2: Processing segment " + segmentCount);
      }
      List<CorpusTerm> corpusTerms = extractor.extractTerms(segment);
      for (CorpusTerm term : corpusTerms) {

        Double jointCount = jointCounts.get(term.getTokJoint());
        if (jointCount == null) {
          continue;
        }

        Double sourceCount = sourceCounts.get(term.getTokSource());
        if (sourceCount == null) {
          sourceCount = new Double(0.0);
        }
        sourceCount = new Double(1.0 + sourceCount.doubleValue());
        sourceCounts.put(term.getTokSource(), sourceCount);

        Double targetCount = targetCounts.get(term.getTokTarget());
        if (targetCount == null) {
          targetCount = new Double(0.0);
        }
        targetCount = new Double(1.0 + targetCount.doubleValue());
        targetCounts.put(term.getTokTarget(), targetCount);

        jointToCorpusTerms.put(term.getTokJoint(), term);
      }
    }

    List<CorpusTerm> postProbTerms = new ArrayList<CorpusTerm>();

    Writer dictFp = Utils.createWriter(outputDictionaryFile);
    for (Map.Entry<String, CorpusTerm> entry : jointToCorpusTerms.entrySet()) {
      CorpusTerm term = entry.getValue();
      double sourceCount = sourceCounts.get(term.getTokSource());
      double targetCount = targetCounts.get(term.getTokTarget());
      double jointCount = jointCounts.get(term.getTokJoint());

      double fwProb = jointCount / sourceCount;
      double bwProb = jointCount / targetCount;
      if (jointCount >= minFreqToKeep && fwProb >= minProbToKeep && bwProb >= minProbToKeep) {
        postProbTerms.add(term);
      }
    }

    System.out.println("After Prob Pruning: " + postProbTerms.size());

    Map<String, List<CorpusTerm>> strippedTermMap = new HashMap<String, List<CorpusTerm>>();
    for (CorpusTerm term : postProbTerms) {
      String source = stripFunctionWords(term.getTokSource());
      String target = stripFunctionWords(term.getTokTarget());
      String termString = CorpusTerm.getJointString(source, target);
      List<CorpusTerm> terms = strippedTermMap.get(termString);
      if (terms == null) {
        terms = new ArrayList<CorpusTerm>();
        strippedTermMap.put(termString, terms);
      }
      terms.add(term);
    }

    List<CorpusTerm> outputTerms = new ArrayList<CorpusTerm>();
    for (Map.Entry<String, List<CorpusTerm>> entry : strippedTermMap.entrySet()) {
      List<CorpusTerm> terms = entry.getValue();
      Collections.sort(terms, corpusTermComp);
      CorpusTerm term = terms.get(0);
      String dictTerm = getDictKey(term.getTokSource(), term.getTokTarget());
      if (baselineDictionary != null && baselineDictionary.contains(dictTerm)) {
        continue;
      }
      outputTerms.add(term);
    }
    
    System.out.println("After Strip Pruning: " + outputTerms.size());
    for (CorpusTerm term : outputTerms) {
      double sourceCount = sourceCounts.get(term.getTokSource());
      double targetCount = targetCounts.get(term.getTokTarget());
      double jointCount = jointCounts.get(term.getTokJoint());

      TextSegment dictSeg = new TextSegment();
      dictSeg.insert("SOURCE_TERM_RAW", term.getRawSource());
      dictSeg.insert("SOURCE_TERM_TOK", term.getTokSource());
      // Semi-HACK: Do something more principled later?
      dictSeg.insert("TARGET_TERM_RAW", term.getRawTarget().toLowerCase());
      dictSeg.insert("TARGET_TERM_TOK", term.getTokTarget());
      dictSeg.insert("SOURCE_ACRONYM_RAW", "");
      dictSeg.insert("SOURCE_ACRONYM_TOK", "");
      dictSeg.insert("TARGET_ACRONYM_RAW", "");
      dictSeg.insert("TARGET_ACRONYM_TOK", "");
      dictSeg.insert("COLLECTION", "parallel_corpus");
      dictSeg.insert("JOINT_COUNT", "" + jointCount);
      dictSeg.insert("SOURCE_COUNT", "" + sourceCount);
      dictSeg.insert("TARGET_COUNT", "" + targetCount);
      dictSeg.write(dictFp);
    }
    IOUtils.closeQuietly(dictFp);

    Writer collFp = Utils.createWriter(outputCollectionFile);
    TextSegment collSeg = new TextSegment();
    collSeg.insert("COLLECTION_TYPE", "CORPUS");
    collSeg.insert("SHORT_NAME", "parallel_corpus");
    collSeg.insert("FULL_NAME", "Parallel Corpus");
    collSeg.write(collFp);
    IOUtils.closeQuietly(collFp);
  }

  private String stripFunctionWords(String str) {
    String[] words = StringUtils.split(str, " ");
    List<String> output = new ArrayList<String>();
    for (String word : words) {
      if ((functionWords == null) || (!functionWords.contains(word))) {
        output.add(word);
      }
    }
    return StringUtils.join(output, " ");
  }

  public static void main(String[] args) {
    CateInitializer.initialize();
    
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    ExtractDictionaryFromCorpus processor = new ExtractDictionaryFromCorpus(params);
    processor.process();
  }
}
