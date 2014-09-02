package edu.isi.madcat.tmem.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.api.TranslationManager;
import edu.isi.madcat.tmem.backend.messages.TranslationHypothesis;
import edu.isi.madcat.tmem.backend.messages.TranslationRequest;
import edu.isi.madcat.tmem.backend.messages.TranslationResponse;
import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.translation.RawTranslationResult;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;

public class RawTranslator {
  TranslationManager manager;
  String configFile;
  String inputFile;
  String outputFile;
  int nbestSize;

  public RawTranslator(String[] args) throws CateProcessException {
    Options options = new Options();
    options.addOption("c", true, "config file");
    options.addOption("i", true, "input file");
    options.addOption("o", true, "output file");
    options.addOption("n", true, "nbest size");
    CommandLineParser parser = new BasicParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      ExceptionHandler.handle(e);
    }

    configFile = cmd.getOptionValue("c");
    inputFile = cmd.getOptionValue("i");
    outputFile = cmd.getOptionValue("o");
    nbestSize = 1;
    if (cmd.hasOption("n")) {
      nbestSize = Integer.parseInt(cmd.getOptionValue("n"));
    }
    manager = TranslationManager.fromXmlFile(configFile);
  }

  void process() throws CateProcessException {
    TextSegmentIterator segIt = new TextSegmentIterator(inputFile);

    try {
      FileWriter writer = new FileWriter(outputFile);
      startOutput(writer);
      TextSegment segment = null;
      while ((segment = segIt.next()) != null) {
        String inputString = segment.getRequired("SOURCE");
        RawTranslationResult result = manager.translateWithRawResults(inputString, nbestSize);
        writeOutput(writer, segment, inputString, result);
      }
      endOutput(writer);
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  private void writeOutput(FileWriter writer, TextSegment inputSegment, String inputString,
      RawTranslationResult result) {
    boolean isFailure = true;
    if (result.getSegmentorResponse() != null) {
      TokenAlignment fullSrcRawToTokAlign = manager.getTranslationHelper().getRawSourceToTokAlignment(inputString, result.getTokenizerResponse().getTokens());

      if (fullSrcRawToTokAlign != null) {
        Map<Range, Range> fullSrcTokToRawMap = fullSrcRawToTokAlign.reverse().createRangeMap();
        isFailure = false;
        int sourceIndex = 0;
        for (int i = 0; i < result.getTranslationRequests().size(); i++) {
          TranslationRequest translationRequest = result.getTranslationRequests().get(i);
          TranslationResponse translationResponse = result.getTranslationResponses().get(i);
          for (int j = 0; j < translationResponse.getHypotheses().size(); j++) {
            TranslationHypothesis hypothesis = translationResponse.getHypotheses().get(j);
            TokenAlignment srcTokToTrgTokAlign = new TokenAlignment(hypothesis.getWordAlignment());

            TokenAlignment trgTokToTrgDetokAlign =
                manager.getTranslationHelper().getTargetTokToDetokAlignment(
                    hypothesis.getDetokenizedWords(), hypothesis.getTranslatedWords());

            TokenAlignment srcRawToTokAlign = new TokenAlignment();
            for (int k = 0; k < translationRequest.getSourceWords().size(); k++) {
              int index = sourceIndex + k;
              Range rawRange = fullSrcTokToRawMap.get(new Range(index, index));
              if (rawRange != null) {
                srcRawToTokAlign.add(new Range(rawRange), new Range(k, k));
              }
            }
            int rawCharStart = 0;
            int rawCharEnd = inputString.length() - 1;
            if (i > 0) {
              rawCharStart = -1;
              for (AlignmentPair ap : srcRawToTokAlign) {
                if (rawCharStart == -1 || ap.getInput().getStart() < rawCharStart) {
                  rawCharStart = ap.getInput().getStart();
                }
              }
            }
            if (i < result.getTranslationRequests().size() - 1) {
              rawCharEnd = -1;
              for (AlignmentPair ap : srcRawToTokAlign) {
                if (rawCharEnd == -1 || ap.getInput().getEnd() > rawCharEnd) {
                  rawCharEnd = ap.getInput().getEnd();
                }
              }
            }
            for (AlignmentPair ap : srcRawToTokAlign) {
              ap.getInput().setStart(ap.getInput().getStart() - rawCharStart);
              ap.getInput().setEnd(ap.getInput().getEnd() - rawCharStart);
            }

            TokenAlignment srcTokToTrgDetokAlign =
                TokenAlignment.projectAlignment(srcTokToTrgTokAlign, trgTokToTrgDetokAlign);

            TokenAlignment srcRawToTrgDetokAlign =
                TokenAlignment.projectAlignment(srcRawToTokAlign, srcTokToTrgDetokAlign);

            if (trgTokToTrgDetokAlign == null || srcTokToTrgDetokAlign == null
                || srcTokToTrgDetokAlign.size() == 0 || srcRawToTrgDetokAlign == null
                || srcRawToTrgDetokAlign.size() == 0) {
              isFailure = true;
            }

            if (!isFailure) {
              TextSegment segment = new TextSegment();
              segment.insert("GUID", inputSegment.getRequired("GUID"));
              segment.insert("SEGMENT_INDEX", "" + i);
              segment.insert("NBEST_INDEX", "" + j);
              segment.insert("SOURCE", inputString.substring(rawCharStart, rawCharEnd + 1));
              segment.insert("TOKENIZED_SOURCE", StringUtils.join(translationRequest
                  .getSourceWords(), " "));
              segment.insert("TOKENZIED_TRANSLATION", StringUtils.join(hypothesis
                  .getTranslatedWords(), " "));
              segment
                  .insert("TRANSLATION", StringUtils.join(hypothesis.getDetokenizedWords(), " "));
              segment.insert("TOK_S_TO_TOK_T_ALIGN", srcTokToTrgTokAlign.toPairString());
              segment.insert("TOK_T_TO_T_ALIGN", trgTokToTrgDetokAlign.toSimpleString());
              segment.insert("S_TO_TOK_S_ALIGN", srcRawToTokAlign.toSimpleString());
              segment.insert("S_TO_T_ALIGN", srcRawToTrgDetokAlign.toSimpleString());
              segment.insert("MT_SCORE", "" + hypothesis.getTotalScore());
              segment.write(writer);
            }
          }
          sourceIndex += translationRequest.getSourceWords().size();
        }
      }
    }
    if (isFailure) {
      TextSegment segment = new TextSegment();
      segment.insert("GUID", inputSegment.getRequired("GUID"));
      segment.insert("TRANSLATION_FAILED", "true");
      segment.write(writer);
    }
  }

  private void startOutput(FileWriter writer) {

  }

  private void endOutput(FileWriter writer) {
  }

  public static void main(String[] args) throws CateProcessException {
    CateInitializer.initialize();

    RawTranslator translator = new RawTranslator(args);
    translator.process();
  }
}
