package edu.isi.madcat.tmem.lextrans.tools;

import edu.isi.madcat.tmem.lextrans.LexTransModel;
import edu.isi.madcat.tmem.utils.ParameterMap;

public class CreateModel {
  public static void main(String[] args) {
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    String inputRuleFile = params.getStringRequired("input_rule_file");
    String outputFile = params.getStringRequired("output_file");

    LexTransModel model = LexTransModel.create(inputRuleFile);
    model.serialize(outputFile);
  }
}
