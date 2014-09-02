package edu.isi.madcat.tmem.lextrans;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;

public class LexTransModel implements java.io.Serializable {
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < rules.length; i++) {
      sb.append("Rule ["+i+"]: " + rules[i].toString()+"\n");
    }
    return sb.toString();
  }

  static final long serialVersionUID = 1L;

  private static Pattern alignmentPattern;

  static {
    alignmentPattern = Pattern.compile("^(\\d+):(\\d+)$");
  }

  public static LexTransModel create(String inputSegmentFile) {
    TextSegmentIterator segIt = new TextSegmentIterator(inputSegmentFile);
    List<SerializedRule> serializedRuleList = new ArrayList<SerializedRule>();
    TextSegment segment = null;
    LexTransModel model = new LexTransModel();
    model.maxSourceLength = 0;
    while ((segment = segIt.next()) != null) {
      List<String> sourceWords =
          Arrays.asList(StringUtils.split(segment.getRequired("SOURCE_WORDS"), " "));
      List<String> targetWords =
          Arrays.asList(StringUtils.split(segment.getRequired("TARGET_WORDS"), " "));
      List<Pair<Integer, Integer>> alignment = parseAlignment(segment.getRequired("ALIGNMENT"));
      if (sourceWords.size() > model.maxSourceLength) {
        model.maxSourceLength = sourceWords.size();
      }
      TranslationRule rule = new TranslationRule(sourceWords, targetWords, alignment);
      serializedRuleList.add(new SerializedRule(rule));
    }
    model.rules = new SerializedRule[serializedRuleList.size()];
    for (int i = 0; i < serializedRuleList.size(); i++) {
      model.rules[i] = serializedRuleList.get(i);
    }
    serializedRuleList = null;
    Arrays.sort(model.rules);
    return model;
  }

  private static List<Pair<Integer, Integer>> parseAlignment(String str) {
    List<Pair<Integer, Integer>> output = new ArrayList<Pair<Integer, Integer>>();
    String[] tokens = StringUtils.split(str, " ");
    for (String token : tokens) {
      Matcher m = alignmentPattern.matcher(token);
      if (!m.find()) {
        throw new RuntimeException("Malformed alignment string: " + str);
      }
      int sourceIndex = Integer.parseInt(m.group(1));
      int targetIndex = Integer.parseInt(m.group(2));
      output.add(new ImmutablePair<Integer, Integer>(sourceIndex, targetIndex));
    }
    return output;
  }

  private SerializedRule[] rules;

  private int maxSourceLength;

  public int getMaxSourceLength() {
    return maxSourceLength;
  }

  public SerializedRule[] getRules() {
    return rules;
  }

  public void initialize() {

  }

  public void serialize(String outputFile) {
    try {
      FileOutputStream fos = new FileOutputStream(outputFile);
      ObjectOutputStream out = new ObjectOutputStream(fos);
      out.writeObject(this);
      out.close();
      fos.close();
    } catch (Exception e) {
      ExceptionHandler.handle(e);
    }
  }

  public void setMaxSourceLength(int maxSourceLength) {
    this.maxSourceLength = maxSourceLength;
  }

  public void setRules(SerializedRule[] rules) {
    this.rules = rules;
  }

  public TranslationRule getRule(String key) {
    SerializedRule serializedRule = getSerializedRule(key);
    if (serializedRule == null) {
      return null;
    }
    TranslationRule rule = serializedRule.unserializeRule();
    return rule;
  }
  
  private SerializedRule getSerializedRule(String key) {
    int low = 0;
    int high = rules.length - 1;
    SerializedRule outputRule = null;
    while (low <= high) {
      int mid = (high - low) / 2 + low;
      SerializedRule curRule = rules[mid];
      int cmp = key.compareTo(curRule.getKey());      
      if (cmp < 0) {
        high = mid - 1;
      } else if (cmp > 0) {
        low = mid + 1;
      } else {
        outputRule = curRule;
        break;
      }
    }
    return outputRule;
  }

}
