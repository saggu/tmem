package edu.isi.madcat.tmem.processors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import edu.isi.madcat.tmem.utils.Utils;

public class SubstringOutput {
  public String getInputText() {
    return inputText;
  }

  public void setInputText(String inputText) {
    this.inputText = inputText;
  }

  public String getMarkedText() {
    return markedText;
  }

  public void setMarkedText(String markedText) {
    this.markedText = markedText;
  }

  public List<Replacement> getReplacementList() {
    return replacementList;
  }

  public void setReplacementList(List<Replacement> replacementList) {
    this.replacementList = replacementList;
  }

  public class Replacement {
    @Override
    public String toString() {
      return "Replacement [inputText=" + inputText + ", inputStart=" + inputStart + ", inputEnd="
          + inputEnd + ", outputText=" + outputText + ", textType=" + textType + ", markerText="
          + markerText + "]";
    }

    public String getTextType() {
      return textType;
    }

    public void setTextType(String textType) {
      this.textType = textType;
    }

    public int getInputStart() {
      return inputStart;
    }

    public void setInputStart(int inputStart) {
      this.inputStart = inputStart;
    }

    public int getInputEnd() {
      return inputEnd;
    }

    public void setInputEnd(int inputEnd) {
      this.inputEnd = inputEnd;
    }

    private String inputText;
    private int inputStart;
    private int inputEnd;
    private String outputText;
    private String textType;
    private String markerText;

    public Replacement(String inputText, int inputStart, int inputEnd, String outputText,
        String textType) {
      super();
      this.inputText = inputText;
      this.inputStart = inputStart;
      this.inputEnd = inputEnd;
      this.outputText = outputText;
      this.textType = textType;
    }

    public String getInputText() {
      return inputText;
    }

    public String getMarkerText() {
      return markerText;
    }

    public String getOutputText() {
      return outputText;
    }

    public void setInputText(String inputText) {
      this.inputText = inputText;
    }

    public void setMarkerText(String markerText) {
      this.markerText = markerText;
    }

    public void setOutputText(String outputText) {
      this.outputText = outputText;
    }

  }

  class ReplacementStartSorter implements Comparator<Replacement> {
    public int compare(Replacement r1, Replacement r2) {
      return r1.getInputStart() - r2.getInputStart();
    }
  }

  private SubstringProcessor processor;
  private String inputText;
  private String markedText;
  private List<Replacement> replacementList;
  private static Random MY_RNG = new Random();

  public SubstringOutput(SubstringProcessor processor, String inputText) {
    super();
    this.processor = processor;
    this.inputText = inputText;
    this.replacementList = new ArrayList<Replacement>();
  }

  public void add(int start, int end, PatternProcessor processor, PatternMatcher matcher) {
    StringTransformer transformer = processor.getTransformer();
    String inputString = inputText.substring(start, end + 1);

    TransformerOutput transOutput = null;
    try {
      transOutput = transformer.transformString(inputString, matcher);
    } catch (Exception e) {
      transOutput = new TransformerOutput(inputString, "default");
      System.err.println("String transformer threw an exception. Backing off to default: PatternProcessor.getId() = "+processor.getId());
      e.printStackTrace();
    }

    replacementList.add(new Replacement(inputString, start, end, transOutput.getOutput(),
        transOutput.getType()));
  }

  public void finalize() {
    Collections.sort(replacementList, new ReplacementStartSorter());

    int lastEnd = 0;
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < replacementList.size(); i++) {
      Replacement replacement = replacementList.get(i);
      String markerText =
          String.format("%s%s%s%s", SubstringProcessor.PRESERVE_TOKEN_PREFIX, processor.getId(),
              replacement.getTextType(), getRandomId());
      replacement.setMarkerText(markerText);
      if (replacement.getInputStart() > lastEnd) {
        sb.append(inputText.substring(lastEnd, replacement.getInputStart()));
      }
      // sb.append(" ");
      sb.append(replacement.getMarkerText());
      // sb.append(" ");
      lastEnd = replacement.getInputEnd() + 1;
    }
    sb.append(inputText.substring(lastEnd));
    markedText = sb.toString();
    markedText = Utils.processWhitespace(markedText);
  }

  public String getRandomId() {
    return String.format("%04x", MY_RNG.nextLong());
  }

  static public List<String> replaceTokens(List<String> input, List<Replacement> replacementList) {
    List<String> output = new ArrayList<String>();
    for (String str : input) {
      if (str.contains(SubstringProcessor.PRESERVE_TOKEN_PREFIX)) {
        str = str.toLowerCase();
        for (Replacement repl : replacementList) {
          str = str.replace(repl.getMarkerText(), repl.getOutputText());
        }
      }
      output.add(str);
    }
    return output;
  }
}
