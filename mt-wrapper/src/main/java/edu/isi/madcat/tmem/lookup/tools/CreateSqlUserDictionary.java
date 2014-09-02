package edu.isi.madcat.tmem.lookup.tools;

import edu.isi.madcat.tmem.lookup.QueryHash;
import edu.isi.madcat.tmem.sql.SqlHandler;
import edu.isi.madcat.tmem.sql.SqlManager;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.ParameterMap;

public class CreateSqlUserDictionary {
  SqlManager sqlManager;

  public CreateSqlUserDictionary(ParameterMap params) {
    String sqlConfigFile = params.getStringRequired("sql_config_file");
    sqlManager = new SqlManager(sqlConfigFile);
  }
  
  public void process() {
    SqlHandler sqlHandler = sqlManager.createHandler();

    // @formatter:off
    sqlHandler.executeStatement("DROP TABLE IF EXISTS user_dict_lookup");
    sqlHandler.executeStatement("DROP TABLE IF EXISTS user_dictionary");
    
    // table: user_dictionary
    sqlHandler.executeStatement("CREATE TABLE user_dictionary ("
        + "dictionary_id INT NOT NULL, "
        + "user_id INT NOT NULL, "
        + "group_id INT NOT NULL, "
        + "source_term_raw BLOB, "
        + "source_term_tok BLOB, "
        + "source_acronym_raw BLOB, "
        + "source_acronym_tok BLOB, "
        + "target_term_raw BLOB, "
        + "target_term_tok BLOB, "
        + "target_acronym_raw BLOB, "
        + "target_acronym_tok BLOB, "
        + "PRIMARY KEY (dictionary_id), "
        + "INDEX user_id_index (user_id), "
        + "INDEX group_id_index (group_id) "
        + ")");
        
    // table: user_dict_lookup
    sqlHandler.executeStatement("CREATE TABLE user_dict_lookup ("
        + "dictionary_id INT NOT NULL, "
        + "lookup_key CHAR("+QueryHash.STRING_LENGTH+") NOT NULL, "
        + "does_match_source INT NOT NULL, "
        + "match_score INT NOT NULL, "
        + "INDEX dictionary_id_index (dictionary_id), "
        + "INDEX lookup_key_index (lookup_key), "
        + "INDEX match_score_index (match_score), "
        + "INDEX lookmatch_index (lookup_key, match_score)"
        + ")");
    // @formatter:on
    sqlHandler.close();
  }
  
  public static void main(String[] args) {
    CateInitializer.initialize();
    
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    CreateSqlUserDictionary processor = new CreateSqlUserDictionary(params);
    processor.process();
  }
}
