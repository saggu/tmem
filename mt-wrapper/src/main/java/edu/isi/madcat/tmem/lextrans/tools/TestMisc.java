
package edu.isi.madcat.tmem.lextrans.tools;

import java.util.Arrays;
import java.util.List;

import edu.isi.madcat.tmem.lextrans.EnglishDetokenizer;
import edu.isi.madcat.tmem.lextrans.RuleBasedTrueCaser;

public class TestMisc {
  public static void main(String[] args) {
    EnglishDetokenizer detokenizer = new EnglishDetokenizer();
    RuleBasedTrueCaser trueCaser = new RuleBasedTrueCaser();
    
    String str = "hello \" my name is jacob @-@ devlin and i have $ 100 ! \" for you";
    List<String> tokens = Arrays.asList(str.split(" "));
    List<String> detokTokens = detokenizer.detokenize(tokens);
    List<String> casedTokens = trueCaser.caseWords(detokTokens);
    System.out.println("tokens: "+tokens);
    System.out.println("detokTokens: "+detokTokens);
    System.out.println("casedTokens: "+casedTokens);
  }
}
