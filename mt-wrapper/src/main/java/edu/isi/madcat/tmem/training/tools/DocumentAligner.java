package edu.isi.madcat.tmem.training.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import edu.isi.madcat.tmem.alignment.EditDistance;
import edu.isi.madcat.tmem.alignment.NgramCounts;
import edu.isi.madcat.tmem.alignment.NgramOverlapScorer;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.CharacterTokenizer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;

public class DocumentAligner {

  static String normalizedDocumentId(String str) {
    String output = str;
    output = output.toUpperCase();
    output = output.replaceAll("-+", "-"); // multi dashes
    output = output.replaceAll("\\.[^.]+$", ""); // extension
    output = output.replaceAll("ARABIC", ""); // extension
    output = output.replaceAll("ENGLISH", ""); // extension
    output = output.replaceAll("_HT", ""); // extension
    return output;
  }

  public static void main(String[] args) {
    ParameterMap params = ParameterMap.readFromArgs(args);

    String[] sourceFiles = params.getStringArrayRequired("source_files");
    String[] targetFiles = params.getStringArrayRequired("target_files");
    String outputDocumentMap = params.getStringRequired("output_document_map");
    double errorThreshold = params.getDoubleRequired("error_threshold");
    List<TextSegment> sourceSegments = TextSegment.readSegments(sourceFiles);
    List<TextSegment> targetSegments = TextSegment.readSegments(targetFiles);
    Map<String, List<TextSegment>> sourceDocuments = groupByDocumentId(sourceSegments);
    Map<String, List<TextSegment>> targetDocuments = groupByDocumentId(targetSegments);
    
    try {
      FileWriter outputFile = new FileWriter(outputDocumentMap);

      CharacterTokenizer dashTokenizer = new CharacterTokenizer('-');
      EditDistance editDistance = new EditDistance(1.0, 1.0, 1.0);

      for (Map.Entry<String, List<TextSegment>> sourceEntry : sourceDocuments.entrySet()) {
        String sourceDocumentId = sourceEntry.getKey();
        String normSourceDocumentId = normalizedDocumentId(sourceDocumentId);
        List<Pair<Double, String>> targetScores = new ArrayList<Pair<Double, String>>();
        double bestError = 1e30;
        Map.Entry<String, List<TextSegment>> bestTarget = null;

        for (Map.Entry<String, List<TextSegment>> targetEntry : targetDocuments.entrySet()) {
          String targetDocumentId = targetEntry.getKey();
          String normTargetDocumentId = normalizedDocumentId(targetDocumentId);
          double error = editDistance.compute(normSourceDocumentId, normTargetDocumentId);
          if (bestTarget == null || error < bestError) {
            bestError = error;
            bestTarget = targetEntry;
          }
          targetScores.add(Pair.of(new Double(error), targetEntry.getKey()));
        }

        // If they don't match very closely, fall back to n-gram overlap
        if (bestError > 5.0) {
          targetScores = new ArrayList<Pair<Double, String>>(); 
          List<String> sourceTokens = dashTokenizer.tokenize(sourceDocumentId);
          bestTarget = null;
          for (Map.Entry<String, List<TextSegment>> targetEntry : targetDocuments.entrySet()) {
            String targetDocumentId = targetEntry.getKey();
            List<String> targetTokens = dashTokenizer.tokenize(targetDocumentId);
            NgramCounts sourceCounts = new NgramCounts(sourceTokens, 1);
            NgramCounts targetCounts = new NgramCounts(targetTokens, 1);
            double numerator = NgramOverlapScorer.computeNgramOverlap(sourceCounts.getNgrams(1),
                targetCounts.getNgrams(1));
            double denominator = Math.min((double) sourceTokens.size(), (double) targetTokens.size());
            double error = 100.0-100.0*numerator/denominator;
            if (bestTarget == null || error < bestError) {
              bestError = error;
              bestTarget = targetEntry;
            }
            targetScores.add(Pair.of(new Double(error), targetEntry.getKey()));
          }
        }
        
        System.out.println("Source: " + sourceDocumentId);
        Collections.sort(targetScores);
        for (Pair<Double, String> p : targetScores) {
          System.out.println("  Score: " + p.getLeft() + "; Document: " + p.getRight());
        }
        if (bestTarget != null && bestError <= errorThreshold) {
          TextSegment documentMapSegment = new TextSegment();
          documentMapSegment.insert("SOURCE_DOCUMENT_ID", sourceDocumentId);
          documentMapSegment.insert("TARGET_DOCUMENT_ID", bestTarget.getKey());
          documentMapSegment.write(outputFile);
        }
      }
      outputFile.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  protected static String concatenateFields(List<TextSegment> segments, int start, int end,
      String field) {
    StringBuilder output = new StringBuilder();
    for (int i = start; i <= end; i++) {
      TextSegment segment = segments.get(i);
      String text = segment.getRequired(field);
      output.append(text);
      if (i < end) {
        output.append("\n");
      }
    }
    return output.toString();
  }

  protected static List<List<String>> extractText(List<TextSegment> segments, String field) {
    List<List<String>> tokenList = new ArrayList<List<String>>();
    for (TextSegment segment : segments) {
      String text = segment.getRequired(field);
      List<String> tokens = CharacterTokenizer.WHITESPACE.tokenize(text);
      tokenList.add(tokens);
    }
    return tokenList;
  }

  protected static Map<String, List<TextSegment>> groupByDocumentId(List<TextSegment> segments) {
    Map<String, List<TextSegment>> files = new HashMap<String, List<TextSegment>>();
    for (TextSegment segment : segments) {
      String filename = segment.getRequired("DOCUMENT_ID");
      if (!files.containsKey(filename)) {
        files.put(filename, new ArrayList<TextSegment>());
      }
      List<TextSegment> fileSegments = files.get(filename);
      fileSegments.add(segment);
    }
    return files;
  }

  static Map<String, NgramCounts> getDocumentCounts(List<TextSegment> segments, String field) {
    Map<String, List<TextSegment>> docidToSegments = new HashMap<String, List<TextSegment>>();
    for (TextSegment segment : segments) {
      String filename = segment.getRequired("DOCUMENT_ID");
      if (!docidToSegments.containsKey(filename)) {
        docidToSegments.put(filename, new ArrayList<TextSegment>());
      }
      List<TextSegment> fileSegments = docidToSegments.get(filename);
      fileSegments.add(segment);
    }
    Map<String, NgramCounts> docidToNgrams = new HashMap<String, NgramCounts>();
    for (Map.Entry<String, List<TextSegment>> entry : docidToSegments.entrySet()) {
      List<String> words = new ArrayList<String>();
      for (TextSegment segment : entry.getValue()) {
        String text = segment.getRequired(field);
        words.addAll(CharacterTokenizer.WHITESPACE.tokenize(text));
      }
      docidToNgrams.put(entry.getKey(), new NgramCounts(words));
    }
    return docidToNgrams;
  }

}
