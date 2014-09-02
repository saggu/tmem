package edu.isi.madcat.tmem.processors;

public class StringTransformerFactory {

  public static StringTransformer create(String name, String transformerParams) {
    StringTransformer transformer = null;
    if (name.equals("self")) {
      transformer = new SelfTransformer();
    }
    else if (name.equals("replace")) {
      transformer = new ReplaceTransformer();
    }
    else if (name.equals("korean_date")) {
      transformer = new KoreanDateTransformer();
    }
    else if (name.equals("english_date")) {
      transformer = new EnglishDateTransformer();
    }
    else if (name.equals("url_etc")) {
      transformer = new UrlEtcTransformer();
    }
    else if (name.equals("korean_currency")) {
      transformer = new KoreanCurrencyTransformer();
    }
    else {
      throw new RuntimeException("Unknown transformer type: "+name);
    }
    transformer.setParams(transformerParams);
    return transformer;
  }
}
