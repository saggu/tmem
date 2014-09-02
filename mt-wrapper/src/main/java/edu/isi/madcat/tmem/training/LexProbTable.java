package edu.isi.madcat.tmem.training;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import edu.isi.madcat.tmem.logging.ExceptionHandler;

public class LexProbTable {
  public class WordPair {
    protected String source;

    protected String target;

    public WordPair(String source, String target) {
      super();
      this.source = source;
      this.target = target;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      WordPair other = (WordPair) obj;
      if (!getOuterType().equals(other.getOuterType()))
        return false;
      if (source == null) {
        if (other.source != null)
          return false;
      } else if (!source.equals(other.source))
        return false;
      if (target == null) {
        if (other.target != null)
          return false;
      } else if (!target.equals(other.target))
        return false;
      return true;
    }

    public String getSource() {
      return source;
    }

    public String getTarget() {
      return target;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((source == null) ? 0 : source.hashCode());
      result = prime * result + ((target == null) ? 0 : target.hashCode());
      return result;
    }

    private LexProbTable getOuterType() {
      return LexProbTable.this;
    }

  }

  protected static double DEFAULT_LOG_PROB;

  static {
    DEFAULT_LOG_PROB = -10.0;
  }

  protected Map<WordPair, Double> probs;

  public LexProbTable(String filename) {
    probs = new HashMap<WordPair, Double>();
    Pattern lexProbPattern = Pattern.compile("^(\\S+) (\\S+) (\\S+)$");
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(filename));
      String line = null;
      while ((line = in.readLine()) != null) {
        Matcher matcher = lexProbPattern.matcher(line);
        if (matcher.find()) {
          String source = matcher.group(1);
          String target = matcher.group(2);
          double prob = Math.log(Double.parseDouble(matcher.group(3)));
          probs.put(new WordPair(source, target), new Double(prob));
        } else {
          in.close();
          throw new RuntimeException("Malformed line: " + line);
        }
      }
      
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  protected double getProb(String source, String target) {
    WordPair wordPair = new WordPair(source, target);
    Double prob = probs.get(wordPair);
    if (prob != null) {
      return prob.doubleValue();
    }
    return DEFAULT_LOG_PROB;
  }
}
