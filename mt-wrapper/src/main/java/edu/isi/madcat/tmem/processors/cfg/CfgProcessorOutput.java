package edu.isi.madcat.tmem.processors.cfg;

import java.util.ArrayList;
import java.util.List;

public class CfgProcessorOutput {
  public class Chunk {
    private int start;

    private int end;

    private List<Chunk> children;

    private ChunkType type;

    private CfgTransformer transformer;

    public Chunk() {

    }

    public Chunk(int start, int end, List<Chunk> children) {
      super();
      this.start = start;
      this.end = end;
      this.children = children;
      this.type = ChunkType.NODE;
    }

    public List<Chunk> getChildren() {
      return children;
    }

    public int getEnd() {
      return end;
    }

    public int getStart() {
      return start;
    }

    public ChunkType getType() {
      return type;
    }

    public void setChildren(List<Chunk> children) {
      this.children = children;
    }

    public void setEnd(int end) {
      this.end = end;
    }

    public void setStart(int start) {
      this.start = start;
    }

    public void setType(ChunkType type) {
      this.type = type;
    }

  }

  public enum ChunkType {
    LEAF, NODE
  }

  private int start;

  private int end;

  private Chunk root;

  public CfgProcessorOutput(int start, int end) {
    super();
    this.start = start;
    this.end = end;
    this.root = null;
  }

  public Chunk createLeafChunk(int start, int end, CfgTransformer transformer) {
    Chunk chunk = new Chunk();
    chunk.start = start;
    chunk.end = end;
    chunk.children = null;
    chunk.transformer = transformer;
    chunk.type = ChunkType.LEAF;
    return chunk;
  }

  public Chunk getRoot() {
    return root;
  }

  public void setCoveringChildren(List<CfgProcessorOutput> childOutputs) {
    List<Chunk> children = new ArrayList<Chunk>();
    for (int i = 0; i < childOutputs.size(); i++) {
      CfgProcessorOutput childOutput = childOutputs.get(i);
      children.add(childOutput.getRoot());
    }
    this.root = new Chunk(start, end, children);
  }

  public void setOtherChildren(List<CfgParserOutput> otherParserOutputs) {
    List<Chunk> children = new ArrayList<Chunk>();
    for (CfgParserOutput parserOutput : otherParserOutputs) {
      for (CfgParserNode rootNode : parserOutput.getRootNodes()) {
        children.add(createLeafChunk(rootNode.getStart(), rootNode.getEnd(), parserOutput
            .getRuleSet().getTransformer()));
      }
    }
    this.root = new Chunk(start, end, children);
  }

  public void setRoot(Chunk root) {
    this.root = root;
  }

}
