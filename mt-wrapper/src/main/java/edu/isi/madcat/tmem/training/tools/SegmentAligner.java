package edu.isi.madcat.tmem.training.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.isi.madcat.tmem.alignment.AlignmentGenerator;
import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.NgramOverlapScorer;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.CharacterTokenizer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;

public class SegmentAligner {

  public static void main(String[] args) {
    try {
      ParameterMap params = ParameterMap.readFromArgs(args);
      String sourceFileList = params.getStringRequired("source_files");
      String targetFileList = params.getStringRequired("target_files");
      String outputFile = params.getStringRequired("output_file");
      List<TextSegment> allSourceSegments = TextSegment.readSegments(sourceFileList);
      List<TextSegment> allTargetSegments = TextSegment.readSegments(targetFileList);

      Map<String, List<TextSegment>> sourceFiles = convertToFiles(allSourceSegments);
      Map<String, List<TextSegment>> targetFiles = convertToFiles(allTargetSegments);

      FileWriter writer = new FileWriter(outputFile);
      for (Map.Entry<String, List<TextSegment>> sourceEntry : sourceFiles.entrySet()) {
        List<TextSegment> sourceSegments = sourceEntry.getValue();

        String sourceDocumentId = sourceEntry.getKey();
        TextSegment sourceSegment = sourceEntry.getValue().get(0);
        String targetDocumentId = sourceSegment.getRequired("TARGET_DOCUMENT_ID");
        List<TextSegment> targetSegments = targetFiles.get(targetDocumentId);
        if (targetSegments == null) {
          throw new RuntimeException("No target document found: " + targetDocumentId);
        }

        NgramOverlapScorer scorer =
            new NgramOverlapScorer(extractText(sourceSegments, "TRANSLATION"), extractText(
                targetSegments, "TOKENIZED_TEXT"));

        TokenAlignment alignment = AlignmentGenerator.heuristicAlignment(scorer);
        if (alignment == null) {
          continue;
        }

        int index = 1;
        for (AlignmentPair p : alignment) {
          String rawSource =
              concatenateFields(sourceSegments, p.getInput().getStart(), p.getInput().getEnd(),
                  "RAW_TEXT");
          String rawTarget =
              concatenateFields(targetSegments, p.getOutput().getStart(), p.getOutput().getEnd(),
                  "RAW_TEXT");
          String tokenizedSource =
              concatenateFields(sourceSegments, p.getInput().getStart(), p.getInput().getEnd(),
                  "TOKENIZED_TEXT");
          String tokenizedTarget =
              concatenateFields(targetSegments, p.getOutput().getStart(), p.getOutput().getEnd(),
                  "TOKENIZED_TEXT");

          String tokenizedTranslation =
              concatenateFields(sourceSegments, p.getInput().getStart(), p.getInput().getEnd(),
                  "TRANSLATION");

          StringBuilder guidSb = new StringBuilder();
          Formatter formatter = new Formatter(guidSb);
          formatter.format("[%s][%05d]", sourceDocumentId, index);

          TextSegment textSegment = new TextSegment();
          textSegment.insert("DOCUMENT_ID", sourceDocumentId);
          textSegment.insert("GUID", guidSb.toString());
          textSegment.insert("RAW_SOURCE", rawSource);
          textSegment.insert("RAW_TARGET", rawTarget);
          textSegment.insert("TOKENIZED_SOURCE", tokenizedSource);
          textSegment.insert("TOKENIZED_TARGET", tokenizedTarget);
          textSegment.insert("TOKENIZED_TRANSLATION", tokenizedTranslation);
          textSegment.write(writer);
          formatter.close();
          index++;
        }
      }

      writer.close();
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

  static Map<String, List<TextSegment>> convertToFiles(List<TextSegment> segments) {
    Map<String, List<TextSegment>> files = new HashMap<String, List<TextSegment>>();
    for (TextSegment segment : segments) {
      String documentId = segment.getRequired("DOCUMENT_ID");
      if (!files.containsKey(documentId)) {
        files.put(documentId, new ArrayList<TextSegment>());
      }
      List<TextSegment> fileSegments = files.get(documentId);
      fileSegments.add(segment);
    }
    return files;
  }
}
