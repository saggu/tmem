package edu.isi.madcat.tmem.training.hmmaligner;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;
import edu.isi.madcat.tmem.utils.Utils;

public class HmmAligner {
  public static void main(String[] args) {
    ParameterMap params = ParameterMap.readFromArgs(args);

    HmmConfig config = new HmmConfig(params);
    HmmAligner aligner = new HmmAligner(config);

    aligner.run();
  }

  HmmConfig config;
  List<SentPair> sentences;
  CorpusLexicon sourceLexicon;

  CorpusLexicon targetLexicon;

  double cutoff;
  // mutable state
  int[][] nonEmptyState;
  int[] numNonEmptyStates;
  double[][] forward;
  double[][] backward;
  double[][] bestScore;

  int[][] backtrace;

  public HmmAligner(HmmConfig config) {
    this.config = config;

    nonEmptyState = new int[config.getMaxSentLength() + 2][config.getMaxSentLength() + 2];
    numNonEmptyStates = new int[config.getMaxSentLength() + 2];
    forward = new double[config.getMaxSentLength() + 2][config.getMaxSentLength() + 2];
    backward = new double[config.getMaxSentLength() + 2][config.getMaxSentLength() + 2];
    bestScore = new double[config.getMaxSentLength() + 2][config.getMaxSentLength() + 2];
    backtrace = new int[config.getMaxSentLength() + 2][config.getMaxSentLength() + 2];
    cutoff = 0.0;

    sourceLexicon = new CorpusLexicon();
    targetLexicon = new CorpusLexicon();
  }

  public void run() {
    loadCorpus(config.getCorpusFiles());

    HmmParams hmmParams = new HmmParams(config);
    hmmParams.createInitialProbs(null);
    SentPairProbs sp = new SentPairProbs(config);
    for (int iterId = 0; iterId < config.getNumIterations(); iterId++) {
      HmmParams newHmmParams = new HmmParams(hmmParams);
      newHmmParams.reset();
      System.out.println("Iteration "+iterId);
      double totalLogProb = 0.0;
      for (int sentId = 0; sentId < sentences.size(); sentId++) {
        if (sentId % 1000 == 0) {
          System.out.println(String.format("Processing sentence %d of %d", sentId, sentences.size()));
        }
        SentPair sent = sentences.get(sentId);
        sp.setSent(sent);
        hmmParams.setSentPairProbs(sp);
        double prob = runBaumWelch(sp, newHmmParams);
        totalLogProb += Math.log(prob);
      }
      double meanLogProb = totalLogProb/(double)sentences.size();
      System.out.println(String.format("Mean log prob: %g", meanLogProb));
      newHmmParams.normalize();
      hmmParams = newHmmParams;
    }

    try {
      Writer writer = Utils.createWriter(config.getOutputFile());

      for (int sentId = 0; sentId < sentences.size(); sentId++) {
        SentPair sent = sentences.get(sentId);
        sp.setSent(sent);
        hmmParams.setSentPairProbs(sp);
        List<Integer> alignment = runViterbi(sp);
        TextSegment segment = sent.getSegment();
        segment.insert(config.getAlignmentField(), StringUtils.join(alignment, " "));
        segment.write(writer);
      }
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  public double runBaumWelch(SentPairProbs sp, HmmParams hmmParams) {
    int numObservations = sp.getSent().getTarget().length - 1;
    int numStates = sp.getSent().getSource().length - 1;

    double[][] emission = sp.getEmission();
    double[][] transition = sp.getTransition();
//    System.out.println(sp.toString(sourceLexicon, targetLexicon));

    for (int o = 1; o <= numObservations; o++) {
      numNonEmptyStates[o] = 0;
      for (int s = 1; s <= numStates; s++) {
        forward[s][o] = 0.0;
        backward[s][o] = 0.0;

        if (emission[s][o] > cutoff) {
          nonEmptyState[o][numNonEmptyStates[o]++] = s;
        }
      }
    }

    // calculate forward probabilities
    for (int sidx = 0; sidx < numNonEmptyStates[1]; sidx++) {
      int s = nonEmptyState[1][sidx];
      forward[s][1] = transition[0][s] * emission[s][1];
    }
    for (int o = 2; o <= numObservations; o++) {
      for (int sidx = 0; sidx < numNonEmptyStates[o]; sidx++) {
        int s = nonEmptyState[o][sidx];
        for (int ssidx = 0; ssidx < numNonEmptyStates[o - 1]; ssidx++) {
          int ss = nonEmptyState[o - 1][ssidx];
          forward[s][o] += forward[ss][o - 1] * transition[ss][s];
        }
        forward[s][o] *= emission[s][o];
      }
    }

    // calculate backward probabilities
    for (int sidx = 0; sidx < numNonEmptyStates[numObservations]; sidx++) {
      int s = nonEmptyState[numObservations][sidx];
      backward[s][numObservations] = transition[s][numStates + 1];
    }
    for (int o = numObservations - 1; o >= 1; o--) {
      for (int sidx = 0; sidx < numNonEmptyStates[o]; sidx++) {
        int s = nonEmptyState[o][sidx];
        for (int ssidx = 0; ssidx < numNonEmptyStates[o + 1]; ssidx++) {
          int ss = nonEmptyState[o + 1][ssidx];
          backward[s][o] += transition[s][ss] * emission[ss][o + 1] * backward[ss][o + 1];
        }
      }
    }

    // calculate total probability of sentence
    double totalProb = 0.0;
    for (int s = 1; s <= numStates; s++) {
      totalProb += forward[s][numObservations] * backward[s][numObservations];
    }

    // calculate new emission counts
    for (int o = 1; o <= numObservations; o++) {
      for (int sidx = 0; sidx < numNonEmptyStates[o]; sidx++) {
        int s = nonEmptyState[o][sidx];
        double count = forward[s][o] * backward[s][o] / totalProb;
        hmmParams.countEmission(sp, s, o, count);
      }
    }

    // calculate new transition counts

    // initial count
    for (int sidx = 0; sidx < numNonEmptyStates[1]; sidx++) {
      int s = nonEmptyState[1][sidx];
      double count = transition[0][s] * emission[s][1] * backward[s][1] / totalProb;
      hmmParams.countTransition(sp, 0, s, 1, count);
    }

    // terminal count
    for (int sidx = 0; sidx < numNonEmptyStates[numObservations]; sidx++) {
      int s = nonEmptyState[numObservations][sidx];
      double count = forward[s][numObservations] * transition[s][numStates + 1] / totalProb;
      hmmParams.countTransition(sp, s, numStates + 1, numObservations + 1, count);
    }

    // s->ss count
    for (int o = 2; o <= numObservations; o++) {
      for (int sidx = 0; sidx < numNonEmptyStates[o - 1]; sidx++) {
        int s = nonEmptyState[o - 1][sidx];
        for (int ssidx = 0; ssidx < numNonEmptyStates[o]; ssidx++) {
          int ss = nonEmptyState[o][ssidx];
          double count =
              forward[s][o - 1] * transition[s][ss] * emission[ss][o] * backward[ss][o] / totalProb;
          hmmParams.countTransition(sp, s, ss, o, count);
        }
      }
    }
    return totalProb;
  }

  public List<Integer> runViterbi(SentPairProbs sp) {
    int numObservations = sp.getSent().getTarget().length - 1;
    int numStates = sp.getSent().getSource().length - 1;

    double[][] emission = sp.getEmission();
    double[][] transition = sp.getTransition();

    for (int s = 1; s <= numStates; s++) {
      bestScore[s][1] = Math.log(transition[0][s]) + Math.log(emission[s][1]);
    }
    for (int o = 2; o < numObservations; o++) {
      for (int s = 1; s <= numStates; s++) {
        bestScore[s][o] =
            bestScore[1][o - 1] + Math.log(transition[1][s]) + Math.log(emission[s][o]);
        backtrace[s][o] = 1;
        for (int ss = 2; ss <= numStates; ss++) {
          double score =
              bestScore[ss][o - 1] + Math.log(transition[ss][s]) + Math.log(emission[s][o]);
          if (score > bestScore[s][o]) {
            bestScore[s][o] = score;
            backtrace[s][o] = ss;
          }
        }
      }
    }
    for (int s = 1; s <= numStates; s++) {
      bestScore[s][numObservations] =
          bestScore[1][numObservations - 1] + Math.log(transition[1][s])
              + Math.log(emission[s][numObservations]) + Math.log(transition[s][numStates + 1]);
      backtrace[s][numObservations] = 1;
      for (int ss = 2; ss <= numStates; ss++) {
        double score =
            bestScore[ss][numObservations - 1] + Math.log(transition[ss][s])
                + Math.log(emission[s][numObservations]) + Math.log(transition[s][numStates + 1]);
        if (score > bestScore[s][numObservations]) {
          bestScore[s][numObservations] = score;
          backtrace[s][numObservations] = ss;
        }
      }
    }
    int bestFinalState = 1;
    double bestFinalScore = bestScore[1][numObservations];
    for (int s = 2; s <= numStates; s++) {
      if (bestScore[s][numObservations] > bestFinalScore) {
        bestFinalScore = bestScore[s][numObservations];
        bestFinalState = s;
      }
    }
    int targetLength = sp.getSent().getTarget().length - 1;

    List<Integer> alignment = new ArrayList<Integer>();
    for (int i = 0; i < targetLength; i++) {
      alignment.add(null);
    }

    int bestState = bestFinalState;
    for (int j = targetLength; j > 0; j--) {
      alignment.set(j - 1, bestState - 1);
      bestState = backtrace[bestState][j];
    }

    return alignment;
  }

  private void loadCorpus(String[] inputFiles) {
    TextSegmentIterator segIt = new TextSegmentIterator(inputFiles);
    TextSegment segment = null;
    sentences = new ArrayList<SentPair>();

    while ((segment = segIt.next()) != null) {
      String sourceSent = segment.getRequired(config.getSourceField());
      String targetSent = segment.getRequired(config.getTargetField());
      String[] sourceWords = StringUtils.split(sourceSent, ' ');
      String[] targetWords = StringUtils.split(targetSent, ' ');
      int[] sourceIds = new int[sourceWords.length + 1];
      int[] targetIds = new int[targetWords.length + 1];
      sourceIds[0] = 0; // NULL
      targetIds[0] = 0; // NULL
      for (int j = 0; j < sourceWords.length; j++) {
        sourceIds[j + 1] = sourceLexicon.addWord(sourceWords[j]);
      }
      for (int j = 0; j < targetWords.length; j++) {
        targetIds[j + 1] = targetLexicon.addWord(targetWords[j]);
      }
      sentences.add(new SentPair(segment, sourceIds, targetIds));
    }
    segIt.close();
  }
}
