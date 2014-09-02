package edu.isi.madcat.tmem.alignment;

import java.util.List;

import edu.isi.madcat.tmem.tokenize.CharacterTokenizer;

public class AlignmentPair implements Comparable<AlignmentPair> 
{
  protected static CharacterTokenizer tokenizer;

  static {
    tokenizer = new CharacterTokenizer(':');
  }

  protected Range input;

  protected Range output;
  
  public AlignmentPair(AlignmentPair other) {
    super();
    this.input = new Range(other.input);
    this.output = new Range(other.output);
  }


  public AlignmentPair(Range input, Range output) {
    super();
    this.input = input;
    this.output = output;
  }

  public AlignmentPair(int inputStart, int inputEnd, int outputStart, int outputEnd) {
    super();
    this.input = new Range(inputStart, inputEnd);
    this.output = new Range(outputStart, outputEnd);
  }

  public AlignmentPair(String str) {
    List<String> tokens = tokenizer.tokenize(str);
    if (tokens.size() != 4) {
      throw new RuntimeException("Malformed AlignmentPair: " + str);
    }
    input = new Range(Integer.parseInt(tokens.get(0)), Integer.parseInt(tokens.get(1)));
    output = new Range(Integer.parseInt(tokens.get(2)), Integer.parseInt(tokens.get(3)));
  }

  public int compareTo(AlignmentPair rhs) {
    int r = 0;
    r = this.input.compareTo(rhs.input);
    if (r != 0) {
      return r;
    }
    r = this.output.compareTo(rhs.output);
    if (r != 0) {
      return r;
    }
    return r;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    AlignmentPair other = (AlignmentPair) obj;
    if (input == null) {
      if (other.input != null)
        return false;
    } else if (!input.equals(other.input))
      return false;
    if (output == null) {
      if (other.output != null)
        return false;
    } else if (!output.equals(other.output))
      return false;
    return true;
  }

  public Range getInput() {
    return input;
  }

  public Range getOutput() {
    return output;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((input == null) ? 0 : input.hashCode());
    result = prime * result + ((output == null) ? 0 : output.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return input + ":" + output;
  }
}
