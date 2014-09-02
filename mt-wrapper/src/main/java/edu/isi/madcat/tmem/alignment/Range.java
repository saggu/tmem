package edu.isi.madcat.tmem.alignment;

public class Range implements Comparable<Range> {
  protected int end;

  protected int start;

  public Range(int start, int end) {
    super();
    this.start = start;
    this.end = end;
  }

  public Range(Range other) {
    super();
    this.start = other.start;
    this.end = other.end;
  }

  public int compareTo(Range rhs) {
    int r = 0;
    r = this.start - rhs.start;
    if (r != 0) {
      return r;
    }
    r = this.end - rhs.end;
    if (r != 0) {
      return r;
    }
    return r;
  }

  public int computeOverlap(Range rhs) {
    int overlap = Math.max(0, (Math.min(end, rhs.end) - Math.max(start, rhs.start) + 1));
    return overlap;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Range other = (Range) obj;
    if (end != other.end)
      return false;
    if (start != other.start)
      return false;
    return true;
  }

  public int getEnd() {
    return end;
  }

  public int getStart() {
    return start;
  }

  public void setEnd(int end) {
    this.end = end;
  }

  public void setStart(int start) {
    this.start = start;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + end;
    result = prime * result + start;
    return result;
  }

  @Override
  public String toString() {
    return start + ":" + end;
  }
}
