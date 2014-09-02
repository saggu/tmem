package edu.isi.madcat.tmem.training.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.CharacterTokenizer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;

public class PrepBiasedTranslation {
  public static void main(String[] args) {
    ParameterMap params = ParameterMap.readFromArgs(args);
    String sourceFileList = params.getStringRequired("source_files");
    String targetFileList = params.getStringRequired("target_files");
    String documentMapFile = params.getStringRequired("document_map_file");
    int ngramOrder = params.getIntRequired("ngram_order");
    String outputFile = params.getStringRequired("output_file");
    List<TextSegment> sourceSegments = TextSegment.readSegments(sourceFileList);
    List<TextSegment> targetSegments = TextSegment.readSegments(targetFileList);

    Map<String, String> documentMap = readDocumentMap(documentMapFile);
    Map<String, List<TextSegment>> sourceDocuments = groupByDocumentId(sourceSegments);
    Map<String, List<TextSegment>> targetDocuments = groupByDocumentId(targetSegments);

    try {
      FileWriter writer = new FileWriter(outputFile);
      for (Map.Entry<String, List<TextSegment>> sourceEntry : sourceDocuments.entrySet()) {
        String sourceDocumentId = sourceEntry.getKey();
        String targetDocumentId = documentMap.get(sourceDocumentId);
        System.out.println("Source Document ID: " + sourceDocumentId);
        if (targetDocumentId == null) {
          System.out.println("** SKIPPING -- No corresponding target file.");
          continue;
        }
        System.out.println("Target Document ID: " + targetDocumentId);
        List<TextSegment> targetDocument = targetDocuments.get(targetDocumentId);
        List<List<String>> targetNgrams =
            extractNgrams(extractTokenizedText(targetDocument), ngramOrder);
        List<String> targetBiasedNgrams = new ArrayList<String>();
        for (List<String> ngram : targetNgrams) {
          StringBuilder ngramSb = new StringBuilder();
          ngramSb.append(ngram.size());
          ngramSb.append(" ");
          for (String word : ngram) {
            ngramSb.append(word);
            ngramSb.append(" ");
          }
          ngramSb.append("1.0");
          targetBiasedNgrams.add(ngramSb.toString());
        }
        Collections.sort(targetBiasedNgrams);
        StringBuilder biasedNgramSb = new StringBuilder();
        biasedNgramSb.append(targetNgrams.size());
        biasedNgramSb.append(" ");
        for (String ngram : targetBiasedNgrams) {
          biasedNgramSb.append(ngram);
          biasedNgramSb.append(" ");
        }

        String biasedNgramString = biasedNgramSb.toString();
        for (TextSegment sourceSegment : sourceEntry.getValue()) {
          TextSegment textSegment = new TextSegment();
          textSegment.insert("GUID", sourceSegment.getRequired("GUID"));
          textSegment.insert("SOURCE", sourceSegment.getRequired("RAW_TEXT"));
          textSegment.insert("BIASED_NGRAMS", biasedNgramString);
          textSegment.write(writer);
        }
      }
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }

  }

  private static Map<String, String> readDocumentMap(String documentMapFile) {
    List<TextSegment> sourceSegments = TextSegment.readSegments(documentMapFile);
    Map<String, String> documentMap = new HashMap<String, String>();
    for (TextSegment segment : sourceSegments) {
      documentMap.put(segment.getRequired("SOURCE_DOCUMENT_ID"), segment
          .getRequired("TARGET_DOCUMENT_ID"));
    }
    return documentMap;
  }

  protected static List<List<String>> extractNgrams(List<List<String>> segments, int ngramOrder) {
    HashSet<List<String>> ngramSet = new HashSet<List<String>>();
    for (List<String> segment : segments) {
      for (int i = 0; i < segment.size() - ngramOrder + 1; i++) {
        List<String> ngram = new ArrayList<String>(segment.subList(i, i + ngramOrder));
        ngramSet.add(ngram);
      }
    }

    List<List<String>> output = new ArrayList<List<String>>();
    for (List<String> ngram : ngramSet) {
      output.add(ngram);
    }
    return output;
  }

  protected static List<List<String>> extractTokenizedText(List<TextSegment> segments) {
    List<List<String>> tokenList = new ArrayList<List<String>>();
    for (TextSegment segment : segments) {
      String text = segment.getRequired("TOKENIZED_TEXT");
      List<String> tokens = CharacterTokenizer.WHITESPACE.tokenize(text);
      tokenList.add(tokens);
    }
    return tokenList;
  }

  static Map<String, List<TextSegment>> groupByDocumentId(List<TextSegment> segments) {
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

}
