package com.brightwebs.jaycee;

/** Can be run with plain Java 21: java -ea com.brightwebs.jaycee.EssenceProgressionSmokeTest. */
public final class EssenceProgressionSmokeTest {
 public static void main(String[] args) {
  check(EssenceProgression.level(0)==1,"initial level");
  check(EssenceProgression.level(399)==1,"below threshold");
  check(EssenceProgression.level(400)==2,"first threshold");
  check(EssenceProgression.level(1300)==3,"cumulative thresholds");
  check(EssenceProgression.costToAdvance(10)==12100,"level cost");
  check(EssenceProgression.grade(80).equals("A"),"A grade");
  check(EssenceProgression.grade(111).equals("SSS"),"SSS grade");
  check(EssenceProgression.saturatingAdd(Long.MAX_VALUE-5,50)==Long.MAX_VALUE,"overflow safety");
  System.out.println("PASS: 8 essence-progression assertions");
 }
 private static void check(boolean condition,String label) { if (!condition) throw new AssertionError(label); }
}
