package edu.isi.madcat.tmem.training.hmmaligner;

import java.util.HashMap;
import java.util.Map;

public class TransTable {
  Map<Integer, Double> marginalCounts;
  Map<Integer, Map<Integer, Double>> jointCounts;

  double espilon;

  TransTable() {
    espilon = 1e-3;
    marginalCounts = new HashMap<Integer, Double>();
    jointCounts = new HashMap<Integer, Map<Integer, Double>>();
  }

  public double getEmission(int sourceId, int targetId) {
    Map<Integer, Double> targetMap = jointCounts.get(sourceId);
    if (targetMap == null) {
      return 0.0;
    }

    Double valueObject = targetMap.get(targetId);
    double value = 0.0;
    if (valueObject != null) {
      value = valueObject.doubleValue();
    }
    return value;
  }

  public void increment(int sourceId, int targetId, double count) {
    addMarginal(sourceId, count);
    addJoint(sourceId, targetId, count);
  }

  void addJoint(int sourceId, int targetId, double count) {
    Map<Integer, Double> targetMap = jointCounts.get(sourceId);
    if (targetMap == null) {
      targetMap = new HashMap<Integer, Double>();
      jointCounts.put(sourceId, targetMap);
    }
    Double totalCountObject = targetMap.get(targetId);
    double totalCount = 0.0;
    if (totalCountObject != null) {
      totalCount = totalCountObject.doubleValue();
    }
    totalCount += count;
    targetMap.put(targetId, totalCount);
  }

  void addMarginal(int sourceId, double count) {
    Double totalCountObject = marginalCounts.get(sourceId);
    double totalCount = 0.0;
    if (totalCountObject != null) {
      totalCount = totalCountObject.doubleValue();
    }
    totalCount += count;
    marginalCounts.put(sourceId, totalCount);
  }

  void normalize() {
    for (Map.Entry<Integer, Map<Integer, Double>> e1 : jointCounts.entrySet()) {
      int sourceId = e1.getKey();
      Map<Integer, Double> targetMap = e1.getValue();
      double marginalCount = marginalCounts.get(sourceId);
      boolean assignUniform = false;
      if (marginalCount < espilon) {
        assignUniform = true;
      }
      double uniformProb = 1.0 / targetMap.size();
      Map<Integer, Double> newTargetMap = new HashMap<Integer, Double>();
      for (Map.Entry<Integer, Double> e2 : targetMap.entrySet()) {
        int targetId = e2.getKey();
        double value = e2.getValue();
        if (assignUniform) {
          newTargetMap.put(targetId, uniformProb);
        } else {
          double prob = value / marginalCount;
          newTargetMap.put(targetId, prob);
        }
      }
      e1.setValue(newTargetMap);
    }
  }
}
