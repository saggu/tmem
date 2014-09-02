package edu.isi.madcat.tmem.processors.cfg;

import java.util.List;

public class CfgRule {
  enum NodeType {
    NT,
    TERM
  }
  
  private CfgSourceNt sourceLhsNt;
  private CfgTargetNt targetLhsNt;
  private List<NodeType> nodeTypes;
  
}
