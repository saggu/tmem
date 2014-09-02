package edu.isi.madcat.tmem.lookup.tools;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.AlignmentGenerator;
import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.alignment.WerAlignmentScorer;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;
import edu.isi.madcat.tmem.utils.Utils;

public class AddTokToRawAlignment {
  String[] inputFiles;
  String outputFile;
  Tokenizer singleCharTokenizer;

  List<Integer> maxTokenList;

  public static void main(String[] args) {

    CateInitializer.initialize();

    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    AddTokToRawAlignment processor = new AddTokToRawAlignment(params);
    processor.process();
  }

  public AddTokToRawAlignment(ParameterMap params) {
    inputFiles = params.getStringArrayRequired("input_files");
    outputFile = params.getStringRequired("output_file");
    singleCharTokenizer = TokenizerFactory.create("single_character");
    maxTokenList = new ArrayList<Integer>();
//    maxTokenList.add(5);
//    maxTokenList.add(10);
    maxTokenList.add(20);
  }

  public void process() {
    Writer writer = Utils.createWriter(outputFile);

    TextSegmentIterator segIt = new TextSegmentIterator(inputFiles);
    TextSegment segment = null;
    while ((segment = segIt.next()) != null) {
      String rawSource = Utils.processWhitespace(segment.getRequired("RAW_SOURCE"));
      String rawTarget = Utils.processWhitespace(segment.getRequired("RAW_TARGET"));
      segment.insert("RAW_SOURCE", rawSource);
      segment.insert("RAW_TARGET", rawTarget);
      if (!segment.contains("SOURCE_ALIGNMENT")) {
        segment.insert("SOURCE_ALIGNMENT", getTokAlignment(rawSource,
            segment.getRequired("TOKENIZED_SOURCE")).toSimpleString());
      }
      if (!segment.contains("TARGET_ALIGNMENT")) {
        segment.insert("TARGET_ALIGNMENT", getTokAlignment(rawTarget,
            segment.getRequired("TOKENIZED_TARGET")).toSimpleString());
      }
      segment.write(writer);
    }
    IOUtils.closeQuietly(writer);
  }

  private TokenAlignment getTokAlignment(String rawText, String tokText) {
    List<String> rawTokens = singleCharTokenizer.tokenize(rawText);
    List<String> tokTokens = Arrays.asList(StringUtils.split(tokText, ' '));
        
    TokenAlignment alignment = null;
    WerAlignmentScorer scorer = new WerAlignmentScorer(tokTokens, rawTokens);
    for (int i = 0; i < maxTokenList.size() && (alignment == null); i++) {
      scorer.setMaxTokensPerChunk(maxTokenList.get(i));
      scorer.setOneToManyOnly(true);
      alignment = AlignmentGenerator.heuristicAlignment(scorer);
    }
    if (alignment == null) {
      alignment = AlignmentGenerator.backoffAlignment(tokTokens, rawTokens);
    }
    TokenAlignment finalAlignment = new TokenAlignment();
    // Make sure that whitespace on the edges isn't aligned
    for (AlignmentPair ap : alignment) {
      int start = ap.getOutput().getStart();
      int end = ap.getOutput().getEnd();
      if (rawTokens.get(start).equals(" ") && end > start) {
        start = start+1;
      }      
      if (rawTokens.get(end).equals(" ") && start < end) {
        end = end-1;
      }
      
      finalAlignment.add(ap.getInput(), new Range(start, end));
    }
    return finalAlignment;
  }
}
