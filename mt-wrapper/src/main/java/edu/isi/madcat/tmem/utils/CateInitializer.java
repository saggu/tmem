package edu.isi.madcat.tmem.utils;


public class CateInitializer {
  public static void initialize() {
    String globalParamFile = System.getProperty("GLOBAL_PARAM_FILE");
    if (globalParamFile == null) {
      globalParamFile = System.getenv().get("GLOBAL_PARAM_FILE");
    }
    
    ParameterMap params = null;
    if (globalParamFile != null) {
      params = new ParameterMap(globalParamFile);
    }
    
    if (params != null) {
      ParameterMap.addStaticVariables(params);
    }
  }
}
