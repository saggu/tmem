package edu.isi.madcat.tmem.lookup;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class DictLookupValue {
  public String getLookupValue() {
    return lookupValue;
  }

  public int getMatchScore() {
    return matchScore;
  }

  private String lookupValue;
  private int matchScore;

  public DictLookupValue(String lookupValue, int matchScore) {
    super();
    this.lookupValue = lookupValue;
    this.matchScore = matchScore;
  }

  public static List<DictLookupValue> getLookupValues(String term, int maxNgramLength) {
    List<DictLookupValue> lookupValues = new ArrayList<DictLookupValue>();
    String[] words = StringUtils.split(term, " ");
    for (int i = 0; i < words.length; i++) {
      for (int j = i; j < words.length && j - i < maxNgramLength; j++) {
        List<String> ngram = new ArrayList<String>();
        for (int k = i; k <= j; k++) {
          ngram.add(words[k]);
        }
        // match score is equal to how many words are missing. 0 is a perfect match
        int matchScore = words.length - ngram.size();
        DictLookupValue lv = new DictLookupValue(StringUtils.join(ngram, " "), matchScore);
        lookupValues.add(lv);
      }
    }
    return lookupValues;
  }
}
