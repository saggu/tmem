package edu.isi.madcat.tmem.lookup.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.isi.madcat.tmem.lookup.CorpusTerm;
import edu.isi.madcat.tmem.lookup.DictLookupValue;
import edu.isi.madcat.tmem.lookup.QueryHash;
import edu.isi.madcat.tmem.lookup.api.CollectionInfo;
import edu.isi.madcat.tmem.sql.InfileWriter;
import edu.isi.madcat.tmem.sql.SqlHandler;
import edu.isi.madcat.tmem.sql.SqlManager;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.ObjectFileWriter;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;

public class CreateSqlDictionary {
  String[] inputDictionaryFiles;
  String[] inputCollectionFiles;
  String workDir;
  int maxNgramLength;
  SqlManager sqlManager;
  Map<String, Integer> dictMap;
  String outputDictMapFile;

  public CreateSqlDictionary(ParameterMap params) {
    inputDictionaryFiles = params.getStringArrayRequired("input_dictionary_files");
    inputCollectionFiles = params.getStringArrayRequired("input_collection_files");
    String sqlConfigFile = params.getStringRequired("sql_config_file");
    maxNgramLength = params.getIntRequired("max_ngram_length");
    workDir = params.getStringRequired("work_dir");
    outputDictMapFile = params.getStringRequired("output_dict_map_file");
    sqlManager = new SqlManager(sqlConfigFile);
  }

  public void process() {
    Map<String, Integer> collectionInfo = new HashMap<String, Integer>();
    String dictionaryFileName = workDir + "/dictionary.csv";
    String dictLookupFileName = workDir + "/dict_lookup.csv";
    String collectionInfoFileName = workDir + "/collection_info.csv";

    InfileWriter dictionaryWriter = new InfileWriter(dictionaryFileName);
    InfileWriter dictLookupWriter = new InfileWriter(dictLookupFileName);
    InfileWriter collectionInfoWriter = new InfileWriter(collectionInfoFileName);

    dictMap = new HashMap<String, Integer>();
    System.out.println("Creating collection table");
    TextSegment segment = null;
    TextSegmentIterator colIt = new TextSegmentIterator(inputCollectionFiles);
    for (int collectionId = 0; (segment = colIt.next()) != null; collectionId++) {
      String shortName = segment.getRequired("SHORT_NAME");
      String fullName = segment.getRequired("FULL_NAME");
      List<String> row = new ArrayList<String>();
      row.add("" + collectionId);
      row.add("" + CollectionInfo.parseCollectionType(segment.getRequired("COLLECTION_TYPE")));
      row.add(shortName);
      row.add(fullName);
      collectionInfoWriter.write(row);
      collectionInfo.put(shortName, collectionId);
    }

    System.out.println("Creating dictionary and lookup table");
    TextSegmentIterator dictIt = new TextSegmentIterator(inputDictionaryFiles);
    for (int dictionaryId = 0; (segment = dictIt.next()) != null; dictionaryId++) {
      List<String> row = new ArrayList<String>();
      int collectionId = getCollectionId(collectionInfo, segment.getRequired("COLLECTION"));
      row.add("" + dictionaryId);
      row.add("" + collectionId);
      String sourceTermTok = segment.getRequired("SOURCE_TERM_TOK");
      String sourceAcronymTok = segment.getRequired("SOURCE_ACRONYM_TOK");
      String targetTermTok = segment.getRequired("TARGET_TERM_TOK");
      String targetAcronymTok = segment.getRequired("TARGET_ACRONYM_TOK");
      row.add(segment.getRequired("SOURCE_TERM_RAW"));
      row.add(sourceTermTok);
      row.add(segment.getRequired("SOURCE_ACRONYM_RAW"));
      row.add(sourceAcronymTok);
      row.add(segment.getRequired("TARGET_TERM_RAW"));
      row.add(targetTermTok);
      row.add(segment.getRequired("TARGET_ACRONYM_RAW"));
      row.add(targetAcronymTok);
      row.add("0");
      List<String> fullTerms = new ArrayList<String>();
      fullTerms.add(CorpusTerm.getJointString(sourceTermTok, targetTermTok));
      fullTerms.add(CorpusTerm.getJointString(sourceAcronymTok, targetAcronymTok));
      for (String fullTerm : fullTerms) {
        dictMap.put(QueryHash.getHashString(fullTerm), new Integer(dictionaryId));
      }
      addLookupTerms(segment.getRequired("SOURCE_TERM_TOK"), dictionaryId, collectionId, 1,
          dictLookupWriter);
      addLookupTerms(segment.getRequired("SOURCE_ACRONYM_TOK"), dictionaryId, collectionId, 1,
          dictLookupWriter);
      addLookupTerms(segment.getRequired("TARGET_TERM_TOK"), dictionaryId, collectionId, 0,
          dictLookupWriter);
      addLookupTerms(segment.getRequired("TARGET_ACRONYM_TOK"), dictionaryId, collectionId, 0,
          dictLookupWriter);
      dictionaryWriter.write(row);
    }
    dictionaryWriter.close();
    dictLookupWriter.close();
    collectionInfoWriter.close();
    
    SqlHandler sqlHandler = sqlManager.createHandler();
    System.out.println("Uploading to database");
    // @formatter:off
    sqlHandler.executeStatement("DROP TABLE IF EXISTS dict_lookup");
    sqlHandler.executeStatement("DROP TABLE IF EXISTS dictionary");
    sqlHandler.executeStatement("DROP TABLE IF EXISTS collection_info");
    // table: collection_info
    sqlHandler.executeStatement("CREATE TABLE collection_info ("
        + "collection_id INT NOT NULL, "
        + "collection_type INT NOT NULL, "
        + "short_name BLOB, "
        + "full_name BLOB, "
        + "INDEX collection_type_index (collection_type), "
        + "PRIMARY KEY (collection_id) "
        + ")");
    
    InfileWriter.writeFile(sqlManager, collectionInfoFileName, "collection_info");
    
    // table: dictionary
    sqlHandler.executeStatement("CREATE TABLE dictionary ("
    		+ "dictionary_id INT NOT NULL, "
        + "collection_id INT NOT NULL, "
        + "source_term_raw BLOB, "
        + "source_term_tok BLOB, "
        + "source_acronym_raw BLOB, "
        + "source_acronym_tok BLOB, "
        + "target_term_raw BLOB, "
        + "target_term_tok BLOB, "
        + "target_acronym_raw BLOB, "
        + "target_acronym_tok BLOB, "
        + "corpus_count INT NOT NULL, "
        + "INDEX collection_id_index (collection_id), "
        + "PRIMARY KEY (dictionary_id) "
        + ")");
    
    InfileWriter.writeFile(sqlManager, dictionaryFileName, "dictionary");
    
    // table: dict_lookup
    sqlHandler.executeStatement("CREATE TABLE dict_lookup ("
        + "dictionary_id INT NOT NULL, "
        + "collection_id INT NOT NULL, "
        + "lookup_key CHAR("+QueryHash.STRING_LENGTH+") NOT NULL, "
        + "does_match_source INT NOT NULL, "
        + "match_score INT NOT NULL, "
        + "INDEX dictionary_id_index (dictionary_id), "
        + "INDEX collection_id_index (collection_id), "
        + "INDEX lookup_key_index (lookup_key), "
        + "INDEX match_score_index (match_score), "
        + "INDEX lookmatch_index (lookup_key, match_score)"
        + ")");
    sqlHandler.close();
    
    InfileWriter.writeFile(sqlManager, dictLookupFileName, "dict_lookup");
    // @formatter:on

    ObjectFileWriter.write(dictMap, outputDictMapFile);
  }

  private Integer getCollectionId(Map<String, Integer> collectionInfo, String shortName) {
    Integer collectionId = collectionInfo.get(shortName);
    if (collectionId == null) {
      throw new RuntimeException("Collection not found: " + shortName);
    }
    return collectionId;
  }

  private void addLookupTerms(String term, int dictionaryId, int collectionId, int doesMatchSource,
      InfileWriter dictLookupWriter) {
    List<DictLookupValue> lookupValues = DictLookupValue.getLookupValues(term, maxNgramLength);
    for (DictLookupValue lv : lookupValues) {
      List<String> row = new ArrayList<String>();
      String lookupKey = QueryHash.getHashString(lv.getLookupValue());
      row.add("" + dictionaryId);
      row.add("" + collectionId);
      row.add(lookupKey);
      row.add("" + doesMatchSource);
      row.add("" + lv.getMatchScore());
      dictLookupWriter.write(row);
    }
  }

  public static void main(String[] args) {
    CateInitializer.initialize();
    
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    CreateSqlDictionary processor = new CreateSqlDictionary(params);
    processor.process();
  }
}
