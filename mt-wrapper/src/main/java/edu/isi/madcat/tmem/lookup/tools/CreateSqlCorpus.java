package edu.isi.madcat.tmem.lookup.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.isi.madcat.tmem.lookup.CorpusTerm;
import edu.isi.madcat.tmem.lookup.CorpusTermExtractor;
import edu.isi.madcat.tmem.lookup.QueryHash;
import edu.isi.madcat.tmem.sql.InfileWriter;
import edu.isi.madcat.tmem.sql.SqlHandler;
import edu.isi.madcat.tmem.sql.SqlManager;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.ObjectFileReader;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.TextSegment;
import edu.isi.madcat.tmem.utils.TextSegmentIterator;

public class CreateSqlCorpus {
  private class BloomFilter {
    private int size1;
    private int size2;
    private short[][] filter;

    public BloomFilter(int size1, int size2) {
      this.size1 = size1;
      this.size2 = size2;
      filter = new short[size1][];
      for (int i = 0; i < size1; i++) {
        filter[i] = new short[size2];
        for (int j = 0; j < filter[i].length; j++) {
          filter[i][j] = 0;
        }
      }
    }

    public void add(String str) {
      int hc1 = getHashCode1(str);
      int hc2 = getHashCode2(str);
      int index1 = Math.abs(hc1) % size1;
      int index2 = Math.abs(hc2) % size2;

      if (filter[index1][index2] < 32000) {
        filter[index1][index2]++;
      }
    }

    public int getCount(String str) {
      int hc1 = getHashCode1(str);
      int hc2 = getHashCode2(str);
      int index1 = Math.abs(hc1) % size1;
      int index2 = Math.abs(hc2) % size2;
      return filter[index1][index2];
    }

    private int getHashCode2(String str) {
      int n = str.length();
      int h = 0;
      for (int i = 0; i < n; i++) {
        h = 31 * h + (int) str.charAt(i);
      }
      return h;
    }

    private int getHashCode1(String str) {
      int n = str.length();
      int h = 0;
      for (int i = 0; i < n; i++) {
        h = 31 * h + (int) str.charAt(n - 1 - i);
      }
      return h;
    }
  }

  String[] inputCorpusFiles;
  String[] inputCollectionFile;
  String workDir;
  int maxNgramLength;
  SqlManager sqlManager;
  Map<String, Integer> dictMap;
  int minFreqToKeep;
  int maxToKeep;
  BloomFilter bloomFilter;
  BloomFilter freqCount;
  
  /**
   * @param args
   */
  public CreateSqlCorpus(ParameterMap params) {
    String inputDictMapFile = params.getStringRequired("input_dict_map_file");
    inputCorpusFiles = params.getStringArrayRequired("input_corpus_files");
    String sqlConfigFile = params.getStringRequired("sql_config_file");
    maxNgramLength = params.getIntRequired("max_ngram_length");
    workDir = params.getStringRequired("work_dir");
    minFreqToKeep = params.getIntRequired("min_freq_to_keep");
    maxToKeep = params.getIntRequired("max_to_keep");
    sqlManager = new SqlManager(sqlConfigFile);

    dictMap = ObjectFileReader.read(inputDictMapFile);
  }

  public static void main(String[] args) {
    CateInitializer.initialize();
    
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    CreateSqlCorpus processor = new CreateSqlCorpus(params);
    processor.process();
  }

  public void process() {
    String corpusFileName = workDir + "/corpus.csv";
    String corpusLookupFileName = workDir + "/corpus_lookup.csv";

    InfileWriter corpusWriter = new InfileWriter(corpusFileName);
    CorpusTermExtractor extractor = new CorpusTermExtractor(maxNgramLength, false);

    bloomFilter = new BloomFilter(34381, 31231);
    freqCount = new BloomFilter(32491, 33503);
    System.out.println("Creating corpus table");
    TextSegmentIterator segIt = new TextSegmentIterator(inputCorpusFiles);
    TextSegment segment = null;
    for (int segmentId = 0; (segment = segIt.next()) != null; segmentId++) {
      List<String> row = new ArrayList<String>();
      row.add("" + segmentId);
      row.add(segment.getRequired("RAW_SOURCE"));
      row.add(segment.getRequired("TOKENIZED_SOURCE"));
      row.add(segment.getRequired("RAW_TARGET"));
      row.add(segment.getRequired("TOKENIZED_TARGET"));
      row.add(segment.getRequired("SOURCE_ALIGNMENT"));
      row.add(segment.getRequired("TARGET_ALIGNMENT"));
      row.add(segment.getRequired("ALIGNMENT"));
      corpusWriter.write(row);
      List<CorpusTerm> corpusTerms = extractor.extractTerms(segment);
      for (CorpusTerm term : corpusTerms) {
        addLookupTerms(segmentId, segment, term, null);
      }
    }
    corpusWriter.close();

    segIt.close();
    System.out.println("Creating lookup table");
    // Pass 2: Filter Output
    segIt = new TextSegmentIterator(inputCorpusFiles);
    InfileWriter corpusLookupWriter = new InfileWriter(corpusLookupFileName);
    for (int segmentId = 0; (segment = segIt.next()) != null; segmentId++) {
      List<CorpusTerm> corpusTerms = extractor.extractTerms(segment);
      for (CorpusTerm term : corpusTerms) {
        addLookupTerms(segmentId, segment, term, corpusLookupWriter);
      }
    }
    corpusLookupWriter.close();
    segIt.close();

    System.out.println("Uploading to database");
    // @formatter:off
    SqlHandler sqlHandler = sqlManager.createHandler();
    sqlHandler.executeStatement("DROP TABLE IF EXISTS corpus_lookup");
    sqlHandler.executeStatement("DROP TABLE IF EXISTS corpus");
    
    // table: dictionary
    sqlHandler.executeStatement("CREATE TABLE corpus ("
        + "segment_id INT NOT NULL, "
        + "source_raw BLOB, "
        + "source_tok BLOB, "
        + "target_raw BLOB, "
        + "target_tok BLOB, "
        + "source_alignment BLOB, "
        + "target_alignment BLOB, "
        + "parallel_alignment BLOB, "
        + "PRIMARY KEY (segment_id) "
        + ")");
    
    InfileWriter.writeFile(sqlManager, corpusFileName, "corpus");
    
    // table: corpus_lookup
    sqlHandler.executeStatement("CREATE TABLE corpus_lookup ("
        + "segment_id INT NOT NULL, "
        + "lookup_key CHAR("+QueryHash.STRING_LENGTH+") NOT NULL, "
        + "dictionary_id INT NOT NULL, "
        + "source_start TINYINT UNSIGNED, "
        + "source_end TINYINT UNSIGNED, "
        + "target_start TINYINT UNSIGNED, "
        + "target_end TINYINT UNSIGNED "
        + ")");

    InfileWriter.writeSplitFile(sqlManager, corpusLookupFileName, "corpus_lookup", 1000000);
    
    System.out.println("Creating lookup_key_index");
    sqlHandler.executeStatement("CREATE INDEX lookup_key_index ON corpus_lookup (lookup_key)");
    System.out.println("Creating segment_id_index");
    sqlHandler.executeStatement("CREATE INDEX segment_id_index ON corpus_lookup (segment_id)");
    System.out.println("Creating dictionary_id_index");
    sqlHandler.executeStatement("CREATE INDEX dictionary_id_index ON corpus_lookup (dictionary_id)");
    System.out.println("Creating segdict_index");
    sqlHandler.executeStatement("CREATE INDEX segdict_index ON corpus_lookup (dictionary_id,segment_id)");
    System.out.println("Creating lookdict_index");
    sqlHandler.executeStatement("CREATE INDEX lookdict_index ON corpus_lookup (lookup_key,segment_id)");

    System.out.println("Updating corpus counts");
    sqlHandler.executeStatement("UPDATE dictionary d SET corpus_count = 0");
    sqlHandler.executeStatement("UPDATE dictionary d SET corpus_count = ("
          + "SELECT COUNT(*) "
          + "FROM corpus_lookup c "
          + "WHERE d.dictionary_id = c.dictionary_id"
        + ");");
    // @formatter:on
    sqlHandler.close();
  }


  private void addLookupTerm(int segmentId, String lookupString, int sourceStart, int sourceEnd,
      int targetStart, int targetEnd, InfileWriter corpusLookupWriter) {
    String lookupKey = QueryHash.getHashString(lookupString);
    if (corpusLookupWriter == null) {
      bloomFilter.add(lookupKey);
    } else {
      int dictLookupId = -1;
      if (dictMap != null) {
        Integer result = dictMap.get(lookupKey);
        if (result != null) {
          dictLookupId = result.intValue();
        }
      }
      if (dictLookupId == -1 && bloomFilter.getCount(lookupKey) < minFreqToKeep) {
        return;
      }
      if (freqCount.getCount(lookupKey) >= maxToKeep) {
        return;
      }
      freqCount.add(lookupKey);
      List<String> row = new ArrayList<String>();
      row.add("" + segmentId);
      row.add(lookupKey);
      row.add("" + dictLookupId);
      row.add("" + getTinyIntValue(sourceStart));
      row.add("" + getTinyIntValue(sourceEnd));
      row.add("" + getTinyIntValue(targetStart));
      row.add("" + getTinyIntValue(targetEnd));
      corpusLookupWriter.write(row);
    }
  }

  private int getTinyIntValue(int value) {
    if (value == -1) {
      return 255;
    }
    if (value >= 255) {
      throw new RuntimeException("Value of out tinyint range: " + value);
    }
    return value;
  }

  private void addLookupTerms(int segmentId, TextSegment segment, CorpusTerm term,
      InfileWriter corpusLookupWriter) {
    if (term.getTokSource() != null && term.getTokTarget() != null) {
      addLookupTerm(segmentId, term.getTokJoint(), term.getSourceStart(), term.getSourceEnd(), term
          .getTargetStart(), term.getTargetEnd(), corpusLookupWriter);
    } else {
      if (term.getTokSource() != null) {
        addLookupTerm(segmentId, term.getTokSource(), term.getSourceStart(), term.getSourceEnd(),
            -1, -1, corpusLookupWriter);
      }
      if (term.getTokTarget() != null) {
        addLookupTerm(segmentId, term.getTokTarget(), -1, -1, term.getTargetStart(), term
            .getTargetEnd(), corpusLookupWriter);
      }
    }
  }
}
