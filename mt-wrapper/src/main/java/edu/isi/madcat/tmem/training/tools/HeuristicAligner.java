package edu.isi.madcat.tmem.training.tools;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.AlignmentGenerator;
import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.alignment.WerAlignmentScorer;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.CharacterTokenizer;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;
import edu.isi.madcat.tmem.utils.Utils;

public class HeuristicAligner {
  public static void main(String[] args) {
    ParameterMap params = ParameterMap.readFromArgs(args);

    String[] inputFiles = params.getStringArrayRequired("input_files");
    String tokenizerAString = params.getString("tokenizer_a");
    String tokenizerBString = params.getString("tokenizer_b");
    String fieldA = params.getStringRequired("field_a");
    String fieldB = params.getStringRequired("field_b");
    String outputField = params.getStringRequired("output_field");
    String outputFile = params.getStringRequired("output_file");
    boolean useStringToWord = params.getBoolean("use_string_to_word");
    
    Tokenizer tokenizerA = CharacterTokenizer.WHITESPACE;
    if (!tokenizerAString.equals("")) {
      tokenizerA = TokenizerFactory.create(tokenizerAString);
    }
    
    Tokenizer tokenizerB = CharacterTokenizer.WHITESPACE;
    if (!tokenizerBString.equals("")) {
      tokenizerB = TokenizerFactory.create(tokenizerBString);
    }
    TextSegmentIterator segIt = new TextSegmentIterator(inputFiles);
    try {
      Writer writer = Utils.createWriter(outputFile);
      TextSegment segment = null;
      TokenAlignment alignment = null;
      while ((segment = segIt.next()) != null) {
        String stringA = segment.getRequired(fieldA);
        String stringB = segment.getRequired(fieldB);
        if (useStringToWord) {
          List<String> tokensB =
              tokenizerB.tokenize(stringB);
          alignment = AlignmentGenerator.stringToWordAlignment(stringA, tokensB);
        } else {
          List<String> tokensA =
              tokenizerA.tokenize(stringA);
          List<String> tokensB =
              tokenizerB.tokenize(stringB);

          WerAlignmentScorer scorer = new WerAlignmentScorer(tokensA, tokensB);
          scorer.setMaxTokensPerChunk(20);
          alignment = AlignmentGenerator.heuristicAlignment(scorer);
        }
        if (alignment != null) {
          List<String> tokens = new ArrayList<String>();
          for (AlignmentPair p : alignment) {
            tokens.add(p.getInput().getStart() + ":" + p.getInput().getEnd() + ":"
                + p.getOutput().getStart() + ":" + p.getOutput().getEnd());
          }
          segment.insert(outputField, StringUtils.join(tokens, " "));
        } else {
          System.err.println("Unable to align: " + segment.getRequired("GUID"));
        }
        segment.write(writer);
      }
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }
}
