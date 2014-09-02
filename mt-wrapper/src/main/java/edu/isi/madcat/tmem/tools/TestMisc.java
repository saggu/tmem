package edu.isi.madcat.tmem.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import edu.isi.madcat.tmem.utils.ThreadedWorkers;

public class TestMisc {
  public class SegmentTranslator implements Runnable {
    private TextSegment segment;

    private UserTranslationInfo userInfo;

    private TranslatedSegment translatedSegment;

    public SegmentTranslator(TextSegment segment, UserTranslationInfo userInfo) {
      super();
      this.segment = segment;
      this.userInfo = userInfo;
    }

    public TextSegment getSegment() {
      return segment;
    }

    public TranslatedSegment getTranslatedSegment() {
      return translatedSegment;
    }

    public UserTranslationInfo getUserInfo() {
      return userInfo;
    }

    public void run() {
      String inputString = segment.getRequired("SOURCE");
      try {
        translatedSegment = manager.translate(inputString, userInfo);
      } catch (CateProcessException e) {
        ExceptionHandler.handle(e);
      }
    }

    public void setSegment(TextSegment segment) {
      this.segment = segment;
    }

    public void setTranslatedSegment(TranslatedSegment translatedSegment) {
      this.translatedSegment = translatedSegment;
    }

    public void setUserInfo(UserTranslationInfo userInfo) {
      this.userInfo = userInfo;
    }

  }

  public static void main(String[] args) throws CateProcessException {
    CateInitializer.initialize();

    TestMisc translator = new TestMisc(args);
    translator.process();
  }

  TranslationManager manager;
  String configFile;
  String inputFile;
  String outputFile;
  int numThreads;

  public TestMisc(String[] args) throws CateProcessException {
    Options options = new Options();
    options.addOption("c", true, "config file");
    options.addOption("i", true, "input file");
    options.addOption("o", true, "output file");
    options.addOption("n", true, "num threads");
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
    numThreads = Integer.parseInt(cmd.getOptionValue("n"));
    System.out.println("Loading Manager");
    manager = TranslationManager.fromXmlFile(configFile);
    System.out.println("Done");
  }

  private void endOutput(FileWriter writer) {
  }

  private void startOutput(FileWriter writer) {

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

  void process() throws CateProcessException {
    TextSegmentIterator segIt = new TextSegmentIterator(inputFile);
    List<SegmentTranslator> translators = new ArrayList<SegmentTranslator>();
    TextSegment segment = null;
    while ((segment = segIt.next()) != null) {
      UserTranslationInfo userInfo = new UserTranslationInfo(1, 1);
      translators.add(new SegmentTranslator(segment, userInfo));
    }
    ThreadedWorkers workers = new ThreadedWorkers(numThreads);
    workers.run(translators);
    try {
      FileWriter writer = new FileWriter(outputFile);
      startOutput(writer);
      for (SegmentTranslator translator : translators) {
        writeOutput(writer, translator.getSegment(), translator.getTranslatedSegment());
      }
      endOutput(writer);
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }
}
