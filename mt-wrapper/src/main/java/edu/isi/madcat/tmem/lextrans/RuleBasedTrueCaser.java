package edu.isi.madcat.tmem.lextrans;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class RuleBasedTrueCaser {
  private Set<String> funcWords;
  private Pattern numberPattern;

  public RuleBasedTrueCaser() {
    funcWords = new HashSet<String>();
    funcWords.add("a");
    funcWords.add("an");
    funcWords.add("and");
    funcWords.add("are");
    funcWords.add("as");
    funcWords.add("at");
    funcWords.add("be");
    funcWords.add("but");
    funcWords.add("by");
    funcWords.add("for");
    funcWords.add("if");
    funcWords.add("in");
    funcWords.add("into");
    funcWords.add("is");
    funcWords.add("it");
    funcWords.add("no");
    funcWords.add("not");
    funcWords.add("of");
    funcWords.add("on");
    funcWords.add("or");
    funcWords.add("such");
    funcWords.add("that");
    funcWords.add("the");
    funcWords.add("their");
    funcWords.add("then");
    funcWords.add("there");
    funcWords.add("these");
    funcWords.add("they");
    funcWords.add("this");
    funcWords.add("to");
    funcWords.add("was");
    funcWords.add("will");
    funcWords.add("with");

    numberPattern = Pattern.compile("^[0-9]+");
  }

  private boolean doCapitalize(int index, String word) {
    if (index > 0 && funcWords.contains(word)) {
      return false;
    }
    if (numberPattern.matcher(word).find()) {
      return false;
    }
    return true;
  }

  public List<String> caseWords(List<String> words) {
    List<String> output = new ArrayList<String>();
    for (int i = 0; i < words.size(); i++) {
      String word = words.get(i);
      boolean doCap = doCapitalize(i, word);
      if (doCap) {
        char[] chars = word.toCharArray();
        for (int j = 0; j < chars.length; j++) {
          if (chars[j] >= (char) 'a' && chars[j] <= (char) 'z') {
            chars[j] = Character.toUpperCase(chars[j]);
            break;
          }
        }
        word = new String(chars);
      }
      output.add(word);
    }
    return output;
  }
}
