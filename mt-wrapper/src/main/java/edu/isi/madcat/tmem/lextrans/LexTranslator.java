package edu.isi.madcat.tmem.lextrans;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import edu.isi.madcat.tmem.backend.messages.TranslationHypothesis;
import edu.isi.madcat.tmem.backend.messages.TranslationRequest;
import edu.isi.madcat.tmem.backend.messages.TranslationResponse;
import edu.isi.madcat.tmem.backend.messages.WordAlignment;
import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class LexTranslator {
  public class RuleApplication implements Comparable<RuleApplication> {
    private TranslationRule rule;

    private int start;
    private int end;

    public RuleApplication(TranslationRule rule, int start, int end) {
      super();
      this.rule = rule;
      this.start = start;
      this.end = end;
    }

    public int compareTo(RuleApplication o) {
      if (end != o.end) {
        return end - o.end;
      }
      return start - o.start;
    }

    public int getEnd() {
      return end;
    }

    public TranslationRule getRule() {
      return rule;
    }

    public int getStart() {
      return start;
    }

    public void setEnd(int end) {
      this.end = end;
    }

    public void setRule(TranslationRule rule) {
      this.rule = rule;
    }

    public void setStart(int start) {
      this.start = start;
    }

    @Override
    public String toString() {
      return "RuleApplication [rule=" + rule + ", start=" + start + ", end=" + end + "]";
    }

  }

  private EnglishDetokenizer detokenizer;
  
  private RuleBasedTrueCaser trueCaser;
  
  private LexTransModel model;

  private int maxSegmentLength;

  private int maxRules;

  public LexTranslator(LexTranslatorConfig config) {
    this.maxSegmentLength = config.getMaxSegmentLength();
    this.maxRules = config.getMaxRules();
    try {
      FileInputStream fileIn = new FileInputStream(config.getModelFile());
      ObjectInputStream in = new ObjectInputStream(fileIn);
      model = (LexTransModel) in.readObject();
      model.initialize();
      in.close();
      fileIn.close();
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (ClassNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
    this.detokenizer = new EnglishDetokenizer();
    this.trueCaser = new RuleBasedTrueCaser();
  }

  public int getMaxRules() {
    return maxRules;
  }

  public int getMaxSegmentLength() {
    return maxSegmentLength;
  }

  public LexTransModel getModel() {
    return model;
  }

  public void setMaxRules(int maxRules) {
    this.maxRules = maxRules;
  }

  public void setMaxSegmentLength(int maxSegmentLength) {
    this.maxSegmentLength = maxSegmentLength;
  }

  public void setModel(LexTransModel model) {
    this.model = model;
  }

  public TranslationResponse translate(TranslationRequest request) {
    List<String> sourceWords = request.getSourceWords();

    Map<Integer, Map<Integer, TranslationRule>> ruleMap =
        new HashMap<Integer, Map<Integer, TranslationRule>>();

    List<RuleApplication> rules = new ArrayList<RuleApplication>();

    for (int n = model.getMaxSourceLength(); n >= 1; n--) {
      for (int start = 0; start + (n - 1) < sourceWords.size(); start++) {
        int end = start + n - 1;
        List<String> sourceNgram = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < n; j++) {
          sourceNgram.add(sourceWords.get(start + j));
        }
        String key = TranslationRule.createKey(sourceNgram);
        TranslationRule rule = model.getRule(key);
        if (rule != null) {
          rules.add(new RuleApplication(rule, start, end));
        }
      }
    }

    Collections.sort(rules);

    Map<Integer, List<RuleApplication>> endToRules = new HashMap<Integer, List<RuleApplication>>();

    for (RuleApplication ruleApp : rules) {
      List<RuleApplication> prevRuleList = endToRules.get(ruleApp.getStart() - 1);
      if (ruleApp.getStart() == 0 || prevRuleList != null) {
        List<RuleApplication> ruleList = endToRules.get(ruleApp.getEnd());
        if (ruleList == null) {
          ruleList = new ArrayList<RuleApplication>();
          endToRules.put(ruleApp.getEnd(), ruleList);
        }
        ruleList.add(ruleApp);
      }
    }

    for (Map.Entry<Integer, List<RuleApplication>> e : endToRules.entrySet()) {
      List<RuleApplication> ruleList = e.getValue();
      // Sort them by length, so that the longest rules are on top
      Collections.sort(ruleList);
    }

    List<RuleApplication> finalRuleList = endToRules.get(sourceWords.size() - 1);
    if (finalRuleList == null) {
      return null;
    }

    List<RuleApplication> ruleApps = new ArrayList<RuleApplication>();
    RuleApplication curRuleApp = finalRuleList.get(0);
    while (true) {
      ruleApps.add(curRuleApp);
      if (curRuleApp.getStart() == 0) {
        break;
      }
      List<RuleApplication> nextRuleList = endToRules.get(curRuleApp.getStart() - 1);
      curRuleApp = nextRuleList.get(0);
    }

    Collections.reverse(ruleApps);
    List<String> translatedWords = new ArrayList<String>();
    List<WordAlignment> wordAlignment = new ArrayList<WordAlignment>();
    int sourceOffset = 0;
    int targetOffset = 0;
    
    for (int i = 0; i < ruleApps.size(); i++) {
      RuleApplication ruleApp = ruleApps.get(i);
      TranslationRule rule = ruleApp.getRule();
      translatedWords.addAll(rule.getTargetWords());
      for (Pair<Integer, Integer> a : rule.getAlignment()) {
        wordAlignment.add(new WordAlignment(new Long(sourceOffset + a.getLeft()), new Long(targetOffset + a.getRight()), new Long(i)));
      }
      sourceOffset += rule.getSourceWords().size();
      targetOffset += rule.getTargetWords().size();
    }
    
    List<String> detokenizedWords = trueCaser.caseWords(detokenizer.detokenize(translatedWords));
    
    TranslationHypothesis hyp =
        new TranslationHypothesis("0", translatedWords, detokenizedWords, wordAlignment,
            new Double(0.0));
    List<TranslationHypothesis> hypotheses = new ArrayList<TranslationHypothesis>();
    hypotheses.add(hyp);
    TranslationResponse response = new TranslationResponse();
    response.setHypotheses(hypotheses);
    return response;
  }
}
