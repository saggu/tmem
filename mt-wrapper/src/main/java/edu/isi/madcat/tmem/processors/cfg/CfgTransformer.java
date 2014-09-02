package edu.isi.madcat.tmem.processors.cfg;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CfgTransformer {
  public enum Type {
    PRESERVE, TRANSLATE, REGEX, FUNCTION
  }

  public static CfgTransformer fromString(String transformerString) {
    Pattern mainPattern = Pattern.compile("^([a-zA-Z])(.*?)");
    Matcher m = mainPattern.matcher(transformerString);
    if (!m.find()) {
      throw new RuntimeException("Malformed CfgTransformer: " + transformerString);
    }
    String typeString = m.group(1).toLowerCase();
    Type type = Type.PRESERVE;
    if (typeString.equals("p")) {
      type = Type.PRESERVE;
    } else if (typeString.equals("t")) {
      type = Type.TRANSLATE;
    } else if (typeString.equals("r")) {
      type = Type.REGEX;
    } else if (typeString.equals("f")) {
      type = Type.FUNCTION;
    } else {
      throw new RuntimeException("Unknown type: " + typeString);
    }
    CfgTransformer transformer = new CfgTransformer();
    transformer.type = type;
    transformer.func = null;
    return transformer;
  }

  private Type type;

  private CfgTransformationFunc func;

  public CfgTransformer() {

  }

  public CfgTransformationFunc getFunc() {
    return func;
  }

  public Type getType() {
    return type;
  }

  public void setFunc(CfgTransformationFunc func) {
    this.func = func;
  }

  public void setType(Type type) {
    this.type = type;
  }

}
