package edu.isi.madcat.tmem.tools;

import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.api.TranslationManager;
import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.translation.TranslatedSegment;
import edu.isi.madcat.tmem.translation.UserTranslationInfo;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;

public class TextTranslator {
  TranslationManager manager;
  String configFile;
  String inputFile;
  String outputFile;

  public TextTranslator(String[] args) throws CateProcessException {
    Options options = new Options();
    options.addOption("c", true, "config file");
    options.addOption("i", true, "input file");
    options.addOption("o", true, "output file");
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
    System.out.println("Loading Manager");
    manager = TranslationManager.fromXmlFile(configFile);
    System.out.println("Done");
  }

  void process() throws CateProcessException {
    TextSegmentIterator segIt = new TextSegmentIterator(inputFile);

    try {
      FileWriter writer = new FileWriter(outputFile);
      startOutput(writer);
      TextSegment segment = null;
      while ((segment = segIt.next()) != null) {
        String inputString = segment.getRequired("SOURCE");
        UserTranslationInfo userInfo = new UserTranslationInfo(1, 1);
        TranslatedSegment translatedSegment = manager.translate(inputString, userInfo);
        writeOutput(writer, segment, translatedSegment);
      }
      endOutput(writer);
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  private void writeOutput(FileWriter writer, TextSegment inputSegment,
      TranslatedSegment translation) {
    TextSegment segment = new TextSegment();
    segment.insert("GUID", inputSegment.getRequired("GUID"));
    if (translation != null) {
      segment.insert("SOURCE", translation.getInputString());
      segment.insert("TOKENIZED_SOURCE", StringUtils.join(translation.getSourceWords(), " "));
      segment.insert("TOKENZIED_TRANSLATION", StringUtils.join(translation
          .getTokenizedTargetWords(), " "));
      segment.insert("TRANSLATION", translation.getJoinedTargetWords());
      segment.insert("S_TO_TOK_S_ALIGN", translation.getSrcRawToSrcTokAlign().toSimpleString());
      segment.insert("S_TO_T_ALIGN", translation.getSrcRawToTrgDetokAlign().toSimpleString());
      segment.insert("TOK_T_TO_T_ALIGN", translation.getTrgTokToTrgDetokAlign().toSimpleString());
      segment.insert("TOK_S_TO_TOK_T_ALIGN", translation.getSrcTokToTrgTokAlign().toPairString());
    } else {
      segment.insert("TRANSLATION_FAILED", "true");
    }
    segment.write(writer);
  }

  private void startOutput(FileWriter writer) {

  }

  private void endOutput(FileWriter writer) {
  }

  public static void main(String[] args) throws CateProcessException {
    CateInitializer.initialize();
    
    TextTranslator translator = new TextTranslator(args);
    translator.process();
  }
}
