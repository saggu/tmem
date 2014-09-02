package edu.isi.madcat.tmem.lookup.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.sql.SqlManager;
import edu.isi.madcat.tmem.tokenize.ReversableKoreanTokenizer;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.ParameterMap;

public class CreateSqlTokenizer {
  ReversableKoreanTokenizer tokenizer;
  String sqlConfigFile;
  ParameterMap tokenizerParams;
  SqlManager sqlManager;
  String workDir;
  String outputParamFile;
  
  public CreateSqlTokenizer(ParameterMap params) {
    String tokenizerParamFile = params.getStringRequired("tokenizer_param_file");
    sqlConfigFile = params.getStringRequired("sql_config_file");
    workDir = params.getStringRequired("work_dir");
    outputParamFile = params.getStringRequired("output_param_file");
    
    tokenizer = new ReversableKoreanTokenizer();
    tokenizerParams = new ParameterMap(tokenizerParamFile);
    tokenizer.initialize(tokenizerParams);
    
    sqlManager = new SqlManager(sqlConfigFile);
  }

  public void process() {
    tokenizer.setSqlManager(sqlManager);
    tokenizer.createSqlLexicon(workDir);
    
    ParameterMap outputParams = new ParameterMap();
    outputParams.put("use_lexicon", "1");
    outputParams.put("use_sql", "1");
    outputParams.put("sql_config_file", sqlConfigFile);
    
    try {
      FileWriter writer = new FileWriter(new File(outputParamFile));
      writer.write(outputParams.toString());
      writer.close();
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }
  }

  public static void main(String[] args) {
    CateInitializer.initialize();
    
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    CreateSqlTokenizer processor = new CreateSqlTokenizer(params);
    processor.process();
  }
}
