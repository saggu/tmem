package edu.isi.madcat.tmem.lookup.api;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.lookup.DictLookupValue;
import edu.isi.madcat.tmem.lookup.QueryHash;
import edu.isi.madcat.tmem.sql.SqlHandler;
import edu.isi.madcat.tmem.sql.SqlManager;
import edu.isi.madcat.tmem.tokenize.Tokenizer;
import edu.isi.madcat.tmem.tokenize.TokenizerFactory;
import edu.isi.madcat.tmem.utils.ParameterMap;

public class LookupTool {
  private Tokenizer sourceTokenizer;
  private Tokenizer targetTokenizer;
  private SqlManager sqlManager;
  private int maxAddNgramLength;

  public LookupTool(String parameterFile) {
    ParameterMap params = new ParameterMap(parameterFile);
    String sqlConfigFile = params.getStringRequired("sql_config_file");
    String sourceTokenizerName = params.getStringRequired("source_tokenizer_name");
    String sourceTokenizerParamFile = params.getStringRequired("source_tokenizer_param_file");
    String targetTokenizerName = params.getStringRequired("target_tokenizer_name");
    String targetTokenizerParamFile = params.getStringRequired("target_tokenizer_param_file");
    maxAddNgramLength = params.getIntRequired("max_add_ngram_length");

    sourceTokenizer = TokenizerFactory.create(sourceTokenizerName, sourceTokenizerParamFile);
    targetTokenizer = TokenizerFactory.create(targetTokenizerName, targetTokenizerParamFile);

    sqlManager = new SqlManager(sqlConfigFile);
  }

  public LookupQuerySet buildMonolingualQuery(String rawTerm) {
    List<String> sourceTokens = sourceTokenizer.tokenize(rawTerm);
    List<String> targetTokens = targetTokenizer.tokenize(rawTerm);
    LookupQuerySet querySet = new LookupQuerySet();
    querySet.addQuery(new LookupQuery(StringUtils.join(sourceTokens, " ")));
    querySet.addQuery(new LookupQuery(StringUtils.join(targetTokens, " ")));
    return querySet;
  }

  public LookupQuerySet buildQueryFromDictionaryId(int dictionaryId) {
    LookupQuerySet querySet = new LookupQuerySet();
    querySet.addQuery(new LookupQuery(dictionaryId));
    return querySet;
  }

  public LookupQuerySet buildBilingualQuery(String tokSourceTerm, String tokTargetTerm) {
    LookupQuerySet querySet = new LookupQuerySet();
    querySet.addQuery(new LookupQuery(tokSourceTerm, tokTargetTerm));
    querySet.addQuery(new LookupQuery(tokTargetTerm, tokSourceTerm));
    return querySet;
  }

  public void deleteTermByDictionaryId(int dictionaryId) {
    SqlHandler sqlHandler = sqlManager.createHandler();
    PreparedStatement lookStatement =
        sqlHandler.prepareStatement("DELETE FROM dict_lookup WHERE dictionary_id = ?");

    try {
      lookStatement.setInt(1, dictionaryId);
      lookStatement.execute();
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }

    PreparedStatement dictStatement =
        sqlHandler.prepareStatement("DELETE FROM dictionary WHERE dictionary_id = ?");

    try {
      dictStatement.setInt(1, dictionaryId);
      dictStatement.execute();
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }


    PreparedStatement userLookStatement =
        sqlHandler.prepareStatement("DELETE FROM user_dict_lookup WHERE dictionary_id = ?");

    try {
      userLookStatement.setInt(1, dictionaryId);
      userLookStatement.execute();
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }

    PreparedStatement userDictStatement =
        sqlHandler.prepareStatement("DELETE FROM user_dictionary WHERE dictionary_id = ?");

    try {
      userDictStatement.setInt(1, dictionaryId);
      userDictStatement.execute();
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }

    sqlHandler.close();
  }

  public void deleteAllUserTerms() {
    SqlHandler sqlHandler = sqlManager.createHandler();

    PreparedStatement dictStatement =
        sqlHandler.prepareStatement("DELETE FROM user_dictionary");

    try {
      dictStatement.execute();
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }

    PreparedStatement lookupStatement =
        sqlHandler.prepareStatement("DELETE FROM user_dict_lookup");

    try {
      lookupStatement.execute();
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }

    sqlHandler.close();
  }

  public void addTermToDictionary(String sourceTerm, String sourceAcronym, String targetTerm,
      String targetAcronym, UserInfo userInfo) {
    if (sourceTerm == null) {
      sourceTerm = "";
    }
    if (sourceAcronym == null) {
      sourceAcronym = "";
    }
    if (targetTerm == null) {
      targetTerm = null;
    }
    if (targetAcronym == null) {
      targetAcronym = "";
    }
    String sourceTermTok = "";
    String sourceAcronymTok = "";
    String targetTermTok = "";
    String targetAcronymTok = "";

    if (!sourceTerm.equals("")) {
      sourceTermTok = StringUtils.join(sourceTokenizer.tokenize(sourceTerm), " ");
    }
    if (!sourceAcronym.equals("")) {
      sourceAcronymTok = StringUtils.join(sourceTokenizer.tokenize(sourceAcronym), " ");
    }
    if (!targetTerm.equals("")) {
      targetTermTok = StringUtils.join(targetTokenizer.tokenize(targetTerm), " ");
    }
    if (!targetAcronym.equals("")) {
      targetAcronymTok = StringUtils.join(targetTokenizer.tokenize(targetAcronym), " ");
    }

    // @formatter:off
    SqlHandler sqlHandler = sqlManager.createHandler();
    PreparedStatement statement =
        sqlHandler.prepareStatement("INSERT INTO user_dictionary ("
        + "dictionary_id, "
        + "user_id, "
        + "group_id, "
        + "source_term_raw, "
        + "source_term_tok, "
        + "source_acronym_raw, "
        + "source_acronym_tok, "
        + "target_term_raw, "
        + "target_term_tok, "
        + "target_acronym_raw, "
        + "target_acronym_tok"
        + ") "
        + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
    // @formatter:on
    int dictionaryId = sqlHandler.getNextId("user_dictionary", "dictionary_id");
    
    try {
      statement.setInt(1, dictionaryId);
      statement.setInt(2, userInfo.getUserId());
      statement.setInt(3, userInfo.getGroupId());
      statement.setString(4, sourceTerm);
      statement.setString(5, sourceTermTok);
      statement.setString(6, sourceAcronym);
      statement.setString(7, sourceAcronymTok);
      statement.setString(8, targetTerm);
      statement.setString(9, targetTermTok);
      statement.setString(10, targetAcronym);
      statement.setString(11, targetAcronymTok);
      statement.execute();
    } catch (SQLException e) {
      ExceptionHandler.handle(e);
    }
    sqlHandler.close();

    addLookupTerms(sourceTermTok, dictionaryId, 1);
    addLookupTerms(sourceAcronymTok, dictionaryId, 1);
    addLookupTerms(targetTermTok, dictionaryId, 0);
    addLookupTerms(targetAcronymTok, dictionaryId, 0);
  }

  private void addLookupTerms(String term, int dictionaryId, int doesMatchSource) {
    SqlHandler sqlHandler = sqlManager.createHandler();
    List<DictLookupValue> lookupValues = DictLookupValue.getLookupValues(term, maxAddNgramLength);
    for (DictLookupValue lv : lookupValues) {
      String lookupKey = QueryHash.getHashString(lv.getLookupValue());
      // @formatter:off
    PreparedStatement statement =
        sqlHandler.prepareStatement("INSERT INTO user_dict_lookup ("
          + "lookup_key, "
        + "dictionary_id, "
        + "does_match_source, "
        + "match_score "
        + ") "
        + "VALUES(?, ?, ?, ?)");
    // @formatter:on
      try {
        statement.setString(1, lookupKey);
        statement.setInt(2, dictionaryId);
        statement.setInt(3, doesMatchSource);
        statement.setInt(4, lv.getMatchScore());
        statement.execute();
      } catch (SQLException e) {
        ExceptionHandler.handle(e);
      }
    }
    sqlHandler.close();
  }

  public CorpusResultSet corpusLookup(LookupQuerySet queries, int maxResults) {
    SqlHandler sqlHandler = sqlManager.createHandler();
    CorpusResultSet resultSet = new CorpusResultSet();
    for (int i = 0; i < queries.getQueries().size(); i++) {
      LookupQuery lookupQuery = queries.getQueries().get(i);
      try {
        String whereClause = null;
        if (lookupQuery.getDictionaryId() == -1) {
          whereClause = "WHERE p.lookup_key = ? ";
        } else {
          whereClause = "WHERE p.dictionary_id = ? ";
        }

        // @formatter:off
        PreparedStatement statement =
            sqlHandler.prepareStatement("SELECT " + "p.lookup_key, "
                + "p.source_start, p.source_end, p.target_start, p.target_end, "
                + "c.segment_id, "
                + "c.source_raw, c.source_tok, c.target_raw, c.target_tok, "
                + "c.source_alignment, c.target_alignment, c.parallel_alignment "
                + "FROM corpus_lookup AS p "
                + "LEFT JOIN corpus AS c ON p.segment_id = c.segment_id "
                + whereClause
                + "ORDER BY p.segment_id ASC " + "LIMIT " + maxResults);
        // @formatter:on
        if (lookupQuery.getDictionaryId() == -1) {
          statement.setString(1, QueryHash.getHashString(lookupQuery.getLookupKey()));
        } else {
          statement.setInt(1, lookupQuery.getDictionaryId());
        }
        // System.out.println(statement.toString());

        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
          CorpusResult result = new CorpusResult(rs, lookupQuery);
          resultSet.addResult(result);
        }
      } catch (SQLException e) {
        ExceptionHandler.handle(e);
      }
    }
    sqlHandler.close();
    resultSet.sortAndFilterResults();
    return resultSet;
  }

  public DictionaryResultSet dictionaryLookup(LookupQuerySet query, int maxResults) {
    SqlHandler sqlHandler = sqlManager.createHandler();
    DictionaryResultSet dictionaryResults = new DictionaryResultSet();
    for (int i = 0; i < query.getQueries().size(); i++) {
      LookupQuery lookupQuery = query.getQueries().get(i);
      if (lookupQuery.getLookupKey() == null) {
        continue;
      }
      try {
        // @formatter:off
        PreparedStatement statement =
            sqlHandler
                .prepareStatement("SELECT "
                    + "p.lookup_key, d.dictionary_id, "
                    + "d.source_term_raw, d.source_term_tok, d.source_acronym_raw, d.source_acronym_tok, "
                    + "d.target_term_raw, d.target_term_tok, d.target_acronym_raw, d.target_acronym_tok, "
                    + "p.does_match_source, p.match_score, "
                    + "c.collection_type, c.short_name, c.full_name, "
                    + "d.corpus_count "
                    + "FROM dict_lookup AS p "
                    + "LEFT JOIN dictionary AS d ON p.dictionary_id = d.dictionary_id "
                    + "LEFT JOIN collection_info AS c ON p.collection_id = c.collection_id "
                    + "WHERE p.lookup_key = ? "
                    + "ORDER BY p.match_score ASC, p.lookup_key ASC "
                    + "LIMIT " + maxResults);
        // @formatter:on

        statement.setString(1, QueryHash.getHashString(lookupQuery.getLookupKey()));

        // System.out.println(statement.toString());
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
          DictionaryResult dictionaryResult = new DictionaryResult(rs);
          dictionaryResults.addResult(dictionaryResult);
        }
        
        // @formatter:off
        PreparedStatement userDictStatement =
            sqlHandler
                .prepareStatement("SELECT "
                    + "p.lookup_key, d.dictionary_id, "
                    + "d.source_term_raw, d.source_term_tok, d.source_acronym_raw, d.source_acronym_tok, "
                    + "d.target_term_raw, d.target_term_tok, d.target_acronym_raw, d.target_acronym_tok, "
                    + "p.does_match_source, p.match_score "
                    + "FROM user_dict_lookup AS p "
                    + "LEFT JOIN user_dictionary AS d ON p.dictionary_id = d.dictionary_id "
                    + "WHERE p.lookup_key = ? "
                    + "ORDER BY p.match_score ASC, p.lookup_key ASC "
                    + "LIMIT " + maxResults);
        // @formatter:on

        userDictStatement.setString(1, QueryHash.getHashString(lookupQuery.getLookupKey()));
        ResultSet userDictRs = userDictStatement.executeQuery();
        while (userDictRs.next()) {
          DictionaryResult dictionaryResult = DictionaryResult.fromUserDictResultSet(userDictRs);
          dictionaryResults.addResult(dictionaryResult);
        }
      } catch (SQLException e) {
        ExceptionHandler.handle(e);
      }
    }
    sqlHandler.close();
    dictionaryResults.sortAndFilterResults();
    return dictionaryResults;
  }

  public List<String> tokenizeSource(String source) {
    return sourceTokenizer.tokenize(source);
  }

  public List<String> tokenizeTarget(String target) {
    return targetTokenizer.tokenize(target);
  }
}
