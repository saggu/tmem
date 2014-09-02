package edu.isi.madcat.tmem.lextrans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnglishDetokenizer {
  private static final int DETOK_RIGHT = 0;
  private static final int DETOK_LEFT = 1;
  private static final int DETOK_BOTH = 2;

  public class DetokItem {
    @Override
    public String toString() {
      return "DetokItem [token=" + token + ", outputToken=" + outputToken + ", numOccurences="
          + numOccurences + ", doClear=" + doClear + "]";
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getOutputToken() {
      return outputToken;
    }

    public void setOutputToken(String outputToken) {
      this.outputToken = outputToken;
    }

    public int getNumOccurences() {
      return numOccurences;
    }

    public void setNumOccurences(int numOccurences) {
      this.numOccurences = numOccurences;
    }

    public boolean doClear() {
      return doClear;
    }

    public void setDoClear(boolean doClear) {
      this.doClear = doClear;
    }

    private String token;
    private String outputToken;
    private int numOccurences;
    private boolean doClear;

    public DetokItem(String token, String outputToken, int numOccurences, boolean doClear) {
      super();
      this.token = token;
      this.outputToken = outputToken;
      this.numOccurences = numOccurences;
      this.doClear = doClear;
    }
    
  }

  List<Map<String, DetokItem>> items;

  public EnglishDetokenizer() {
    items = new ArrayList<Map<String, DetokItem>>();
    for (int i = 0; i < 3; i++) {
      items.add(new HashMap<String, DetokItem>());
    }
    add(DETOK_RIGHT, "\"", "\"", 1, false);
    add(DETOK_RIGHT, "(", "(", 1, true);
    add(DETOK_RIGHT, "[", "[", 1, true);
    add(DETOK_RIGHT, "$", "$", 1, true);
    add(DETOK_RIGHT, "`", "`", 1, false);
    add(DETOK_RIGHT, "{", "{", 1, true);
    add(DETOK_RIGHT, "-@", "-", 1, true);
    add(DETOK_RIGHT, ":@", ":", 1, true);
    add(DETOK_RIGHT, "/@", "/", 1, true);
    add(DETOK_LEFT, ".", ".", 1, true);
    add(DETOK_LEFT, "?", "?", 1, true);
    add(DETOK_LEFT, "!", "!", 1, true);
    add(DETOK_LEFT, "\"", "\"", 2, true);
    add(DETOK_LEFT, ",", ",", 1, true);
    add(DETOK_LEFT, ")", ")", 1, true);
    add(DETOK_LEFT, "]", "]", 1, true);
    add(DETOK_LEFT, ";", ";", 1, true);
    add(DETOK_LEFT, "'", "'", 1, true);
    add(DETOK_LEFT, "`", "`", 2, true);
    add(DETOK_LEFT, "%", "%", 1, true);
    add(DETOK_LEFT, "}", "}", 1, true);
    add(DETOK_LEFT, "'t", "'t", 1, true);
    add(DETOK_LEFT, "'nt", "'nt", 1, true);
    add(DETOK_LEFT, "'s", "'s", 1, true);
    add(DETOK_LEFT, "'re", "'re", 1, true);
    add(DETOK_LEFT, "'ll", "'ll", 1, true);
    add(DETOK_LEFT, "'m", "'m", 1, true);
    add(DETOK_LEFT, "'d", "'d", 1, true);
    add(DETOK_LEFT, "'ve", "'ve", 1, true);
    add(DETOK_LEFT, "th", "th", 1, true);
    add(DETOK_LEFT, "rd", "rd", 1, true);
    add(DETOK_LEFT, "st", "st", 1, true);
    add(DETOK_LEFT, "@-", "-", 1, true);
    add(DETOK_LEFT, "@/", "/", 1, true);
    add(DETOK_LEFT, ":-", ":-", 1, true);
    add(DETOK_LEFT, ":", ":", 1, true);
    add(DETOK_BOTH, "@-@", "-", 1, true);
    add(DETOK_BOTH, "@:@", ":", 1, true);
    add(DETOK_BOTH, "@/@", "/", 1, true);
    add(DETOK_BOTH, "*", "*", 1, true);
    add(DETOK_BOTH, "^", "^", 1, true);
  }

  public List<String> detokenize(List<String> input) {
    List<StringBuilder> outputSbs = new ArrayList<StringBuilder>();
    HashMap<String, Integer> occurences = new HashMap<String, Integer>();
    boolean isNewWord = false;
    outputSbs.add(new StringBuilder());
    for (int i = 0; i < input.size(); i++) {
      String w = input.get(i);
      Integer count = occurences.get(w);
      if (count == null) {
        count = new Integer(0);
      }
      count = count.intValue()+1;
      occurences.put(w, count);
      
      DetokItem leftItem = items.get(DETOK_LEFT).get(w);
      DetokItem rightItem = items.get(DETOK_RIGHT).get(w);
      DetokItem bothItem = items.get(DETOK_BOTH).get(w);
      if (leftItem != null && occurences.get(w) == leftItem.getNumOccurences()) {
        outputSbs.get(outputSbs.size()-1).append(leftItem.getOutputToken());
        isNewWord = true;
        if (leftItem.doClear()) {
          occurences.put(w, 0);
        }
      }
      else if (rightItem != null && occurences.get(w) == rightItem.getNumOccurences()) {
        if (isNewWord) {
          outputSbs.add(new StringBuilder());
        }
        outputSbs.get(outputSbs.size()-1).append(rightItem.getOutputToken());
        isNewWord = false;
        if (rightItem.doClear()) {
          occurences.put(w, 0);
        }
      }
      else if (bothItem != null && occurences.get(w) == bothItem.getNumOccurences()) {
        outputSbs.get(outputSbs.size()-1).append(bothItem.getOutputToken());
        isNewWord = false;
        if (bothItem.doClear()) {
          occurences.put(w, 0);
        }
      }
      else {
        if (isNewWord) {
          outputSbs.add(new StringBuilder());
        }
        outputSbs.get(outputSbs.size()-1).append(input.get(i));
        isNewWord = true;
        occurences.put(w, 0);
      }
    }
    
    if (outputSbs.size() == 1 && outputSbs.get(outputSbs.size()-1).toString() == "") {
      outputSbs.get(outputSbs.size()-1).append(".");
    }
    
    List<String> output = new ArrayList<String>();
    for (StringBuilder sb : outputSbs) {
      output.add(sb.toString());
    }
    return output;
  }
  
  private void add(int detokType, String token, String outputToken, int numOccurences, boolean doClear) {
    items.get(detokType).put(token, new DetokItem(token, outputToken, numOccurences, doClear));
  }
}
