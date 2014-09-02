package edu.isi.madcat.tmem.ter;

/*
 * Copyright 2006 by BBN Technologies and University of Maryland (UMD)
 * 
 * BBN and UMD grant a nonexclusive, source code, royalty-free right to use this Software known as
 * Translation Error Rate COMpute (the "Software") solely for research purposes. Provided, you must
 * agree to abide by the license and terms stated herein. Title to the Software and its
 * documentation and all applicable copyrights, trade secrets, patents and other intellectual rights
 * in it are and remain with BBN and UMD and shall not be used, revealed, disclosed in marketing or
 * advertisement or any other activity not explicitly permitted in writing.
 * 
 * BBN and UMD make no representation about suitability of this Software for any purposes. It is
 * provided "AS IS" without express or implied warranties including (but not limited to) all implied
 * warranties of merchantability or fitness for a particular purpose. In no event shall BBN or UMD
 * be liable for any special, indirect or consequential damages whatsoever resulting from loss of
 * use, data or profits, whether in an action of contract, negligence or other tortuous action,
 * arising out of or in connection with the use or performance of this Software.
 * 
 * Without limitation of the foregoing, user agrees to commit no act which, directly or indirectly,
 * would violate any U.S. law, regulation, or treaty, or any other international treaty or agreement
 * to which the United States adheres or with which the United States complies, relating to the
 * export or re-export of any commodities, software, or technical data. This Software is licensed to
 * you on the condition that upon completion you will cease to use the Software and, on request of
 * BBN and UMD, will destroy copies of the Software in your possession.
 * 
 * Matthew Snover (snover@cs.umd.edu)
 */

import java.util.HashMap;
import java.util.ArrayList;

/* Storage Class for TER alignments */
public class TerAlignment {
  public static String join(String delim, char[] arr) {
    if (arr == null)
      return "";
    if (delim == null)
      delim = new String("");
    String s = new String("");
    for (int i = 0; i < arr.length; i++) {
      if (i == 0) {
        s += arr[i];
      } else {
        s += delim + arr[i];
      }
    }
    return s;
  }

  public static String join(String delim, Comparable<?>[] arr) {
    if (arr == null)
      return "";
    if (delim == null)
      delim = new String("");
    String s = new String("");
    for (int i = 0; i < arr.length; i++) {
      if (i == 0) {
        s += arr[i];
      } else {
        s += delim + arr[i];
      }
    }
    return s;
  }

  public static void performShiftArray(HashMap<String, ArrayList<Integer>> hwords, int start,
      int end, int moveto, int capacity) {
    HashMap<String, ArrayList<Integer>> nhwords = new HashMap<String, ArrayList<Integer>>();

    if (moveto == -1) {
      copyHashWords(hwords, nhwords, start, end, 0);
      copyHashWords(hwords, nhwords, 0, start - 1, end - start + 1);
      copyHashWords(hwords, nhwords, end + 1, capacity, end + 1);
    } else if (moveto < start) {
      copyHashWords(hwords, nhwords, 0, moveto, 0);
      copyHashWords(hwords, nhwords, start, end, moveto + 1);
      copyHashWords(hwords, nhwords, moveto + 1, start - 1, end - start + moveto + 2);
      copyHashWords(hwords, nhwords, end + 1, capacity, end + 1);
    } else if (moveto > end) {
      copyHashWords(hwords, nhwords, 0, start - 1, 0);
      copyHashWords(hwords, nhwords, end + 1, moveto, start);
      copyHashWords(hwords, nhwords, start, end, start + moveto - end);
      copyHashWords(hwords, nhwords, moveto + 1, capacity, moveto + 1);
    } else {
      copyHashWords(hwords, nhwords, 0, start - 1, 0);
      copyHashWords(hwords, nhwords, end + 1, end + moveto - start, start);
      copyHashWords(hwords, nhwords, start, end, moveto);
      copyHashWords(hwords, nhwords, end + moveto - start + 1, capacity, end + moveto - start + 1);
    }
    hwords.clear();
    hwords.putAll(nhwords);
  }

  private static void copyHashWords(HashMap<String, ArrayList<Integer>> ohwords,
      HashMap<String, ArrayList<Integer>> nhwords, int start, int end, int nstart) {
    int ind_start = 0;
    int ind_in = 3;
    ArrayList<Integer> val = null;
    int k = nstart;

    for (int i = start; i <= end; ++k, ++i) {
      for (int j = ind_start; j <= ind_in; ++j) {
        val = ohwords.get(i + "-" + j);
        if (val != null) {
          nhwords.put(k + "-" + j, val);
        }
      }
    }
  }

  public Comparable<?>[] aftershift;

  public char[] alignment = null;

  public TerShift[] allshifts = null;

  public String bestRef = "";
  public Comparable<?>[] hyp;
  public int numDel = 0;

  public double numEdits = 0;

  public int numIns = 0;
  public int numSft = 0;
  public int numSub = 0;
  public double numWords = 0.0;

  public int numWsf = 0;
  public Comparable<?>[] ref;

  public double score() {
    if ((numWords <= 0.0) && (this.numEdits > 0.0)) {
      return 1.0;
    }
    if (numWords <= 0.0) {
      return 0.0;
    }
    return numEdits / numWords;
  }

  public void scoreDetails() {
    numIns = numDel = numSub = numWsf = numSft = 0;
    if (allshifts != null) {
      for (int i = 0; i < allshifts.length; ++i)
        numWsf += allshifts[i].size();
      numSft = allshifts.length;
    }

    if (alignment != null) {
      for (int i = 0; i < alignment.length; ++i) {
        switch (alignment[i]) {
          case 'S':
          case 'T':
            numSub++;
            break;
          case 'D':
            numDel++;
            break;
          case 'I':
            numIns++;
            break;
        }
      }
    }
    // if(numEdits != numSft + numDel + numIns + numSub)
    // System.out.println("** Error, unmatch edit erros " + numEdits +
    // " vs " + (numSft + numDel + numIns + numSub));
  }

  @Override
  public String toString() {
    String s =
        "Original Ref: " + join(" ", ref) + "\nOriginal Hyp: " + join(" ", hyp)
            + "\nHyp After Shift: " + join(" ", aftershift);

    if (alignment != null) {
      s += "\nAlignment: (";
      for (int i = 0; i < alignment.length; i++) {
        s += alignment[i];
      }
      s += ")";
    }
    if (allshifts == null) {
      s += "\nNumShifts: 0";
    } else {
      s += "\nNumShifts: " + allshifts.length;
      for (int i = 0; i < allshifts.length; i++) {
        s += "\n  " + allshifts[i];
      }
    }

    s += "\nScore: " + this.score() + " (" + this.numEdits + "/" + this.numWords + ")";
    return s;
  }

}
