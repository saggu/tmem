package edu.isi.madcat.tmem.training.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.alignment.AlignmentGenerator;
import edu.isi.madcat.tmem.alignment.AlignmentPair;
import edu.isi.madcat.tmem.alignment.Range;
import edu.isi.madcat.tmem.alignment.TokenAlignment;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.tokenize.CharacterTokenizer;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;

public class ApplySegmentation {
  public static void main(String[] args) {
    ParameterMap params = ParameterMap.readFromArgs(args);
    ApplySegmentation segmentor = new ApplySegmentation(params);
    segmentor.process();
  }

  protected String[] inputFiles;
  protected int maxSentenceLength;

  protected String outputFile;
  protected String tokenizerName;

  public ApplySegmentation(ParameterMap params) {
    inputFiles = params.getStringArrayRequired("input_files");
    maxSentenceLength = -1;
    if (params.hasParam("max_sentence_length")) {
      maxSentenceLength = params.getIntRequired("max_sentence_length");
    }
    outputFile = params.getStringRequired("output_file");
    tokenizerName = params.getStringRequired("tokenizer_name");
  }

  public void process() {
    TextSegmentIterator segIt = new TextSegmentIterator(inputFiles);
    try {
      FileWriter writer = new FileWriter(outputFile);
      TextSegment segment = null;
      while ((segment = segIt.next()) != null) {
        TokenAlignment segToToken = (new TokenAlignment(segment.getRequired("SEGMENTATION")));
        if (segToToken == null || segToToken.size() == 0 || segToToken.size() == 1) {
          segment.write(writer);
        } else {
          TokenAlignment mergedSegToToken = segToToken;
          if (maxSentenceLength != -1) {
            mergedSegToToken = mergeSegments(segToToken);
          }
          Tokenizer tokenizer = TokenizerFactory.create(tokenizerName);
          String rawText = segment.getRequired("RAW_TEXT");
          List<String> tokens =
              CharacterTokenizer.WHITESPACE.tokenize(segment.getRequired("TOKENIZED_TEXT"));
          AlignmentGenerator.MAX_TOKENS_PER_CHUNK = 20;
          TokenAlignment alignment =
              AlignmentGenerator.stringToWordAlignment(rawText, tokens, tokenizer);
          TokenAlignment segToRaw =
              TokenAlignment.projectAlignment(alignment, mergedSegToToken.reverse()).reverse();

          Map<Integer, Range> segToRawRanges = new HashMap<Integer, Range>();
          for (AlignmentPair p : segToRaw) {
            int index = p.getInput().getStart();
            Range range = segToRawRanges.get(index);
            int start = p.getOutput().getStart();
            int end = p.getOutput().getEnd();
            if (range != null) {
              if (range.getStart() < start) {
                start = range.getStart();
              }
              if (range.getEnd() > end) {
                end = range.getEnd();
              }
            }
            segToRawRanges.put(index, new Range(start, end));
          }

          // System.out.println(alignment);
          // System.out.println(mergedSegToToken);
          // System.out.println(segToRaw);
          // System.out.println("\n\n");
          int outputIndex = 0;
          for (AlignmentPair p : mergedSegToToken) {
            int index = p.getInput().getStart();
            Range rawRange = segToRawRanges.get(index);
            // Segments can be very small and not correspond to a whole raw word. In this case just
            // skip it.
            if (rawRange == null) {
              continue;
            }
            String rawSubString =
                rawText.substring(segToRawRanges.get(index).getStart(), segToRawRanges.get(index)
                    .getEnd() + 1);
            List<String> tokSub =
                tokens.subList(p.getOutput().getStart(), p.getOutput().getEnd() + 1);
            String tokSubString = StringUtils.join(tokSub, " ");
            String newGuid =
                String.format("%s[%05d]", segment.getRequired("GUID"), outputIndex + 1);
            TextSegment newSegment = new TextSegment(segment);
            newSegment.insert("RAW_TEXT", rawSubString);
            newSegment.insert("TOKENIZED_TEXT", tokSubString);
            newSegment.insert("ORIG_GUID", segment.getRequired("GUID"));
            newSegment.insert("SEGMENT_INDEX", "" + outputIndex);
            newSegment.insert("GUID", newGuid);
            newSegment.write(writer);
            outputIndex++;
          }
        }
      }
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  protected TokenAlignment mergeSegments(TokenAlignment input) {
    TokenAlignment output = new TokenAlignment();
    List<AlignmentPair> pairs = new ArrayList<AlignmentPair>();
    for (AlignmentPair p : input) {
      pairs.add(p);
    }
    int outputIndex = 0;
    for (int i = 0; i < pairs.size(); i++) {
      int start = pairs.get(i).getOutput().getStart();
      int j = i;
      for (j = i; j < pairs.size(); j++) {
        int end = pairs.get(j).getOutput().getEnd();
        int length = end - start + 1;
        if (length > maxSentenceLength) {
          break;
        }
      }
      if (j > i) {
        j--;
      }
      output.add(outputIndex, outputIndex, start, pairs.get(j).getOutput().getEnd());
      i = j;
      outputIndex++;
    }
    return output;
  }
}
