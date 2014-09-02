package edu.isi.madcat.tmem.training.tools;

import edu.isi.madcat.tmem.utils.ParameterMap;

public class CfgParser {
  public CfgParser(ParameterMap params) {
    
  }
  
  public void process() {
    
  }
  
  public static void main(String[] args) {
    ParameterMap params = ParameterMap.readFromArgs(args);
    CfgParser segmentor = new CfgParser(params);
    segmentor.process();
  }
}
