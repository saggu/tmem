package edu.isi.madcat.tmem.training.hmmaligner;

import java.util.HashMap;
import java.util.Map;

public class CorpusLexicon {
  private int nextId;
  private Map<Integer, String> idToWord;
  private Map<String, Integer> wordToId;

  CorpusLexicon() {
    idToWord = new HashMap<Integer, String>();
    wordToId = new HashMap<String, Integer>();
    nextId = 0;
    addWord("NULL");
  }

  int addWord(String word) {
    Integer idObject = wordToId.get(word);
    int id = -1;
    if (idObject == null) {
      id = nextId;
      nextId++;
      wordToId.put(word, id);
      idToWord.put(id, word);
    } else {
      id = idObject.intValue();
    }
    return id;
  }

  int getId(String word) {
    Integer id = wordToId.get(word);
    if (id == null) {
      throw new RuntimeException("Word not found: " + word);
    }
    return id.intValue();
  }

  String getWord(int id) {
    String word = idToWord.get(id);
    if (word == null) {
      throw new RuntimeException("Id not found: " + id);
    }
    return word;
  }
}
