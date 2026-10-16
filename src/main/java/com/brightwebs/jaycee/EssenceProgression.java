package com.brightwebs.jaycee;
/** Pure, overflow-safe cumulative essence progression; no vanilla Minecraft XP dependency. */
public final class EssenceProgression {
 private EssenceProgression() {}
 public static long costToAdvance(int level) {
  if(level<1) throw new IllegalArgumentException("level");
  long n=(long)level+1L;
  if(n>3_037_000L) return Long.MAX_VALUE;
  long sq=n*n;
  if(sq>Long.MAX_VALUE/100L) return Long.MAX_VALUE;
  return 100L*sq;
 }
 public static int level(long earnedEssence) {
  long remaining=Math.max(0L,earnedEssence);
  int level=1;
  while(level<10000) {
   long next=costToAdvance(level);
   if(remaining<next)break;
   remaining-=next;
   level++;
  }
  return level;
 }
 public static String grade(int level) {
  if(level>=111)return "SSS";
  if(level>=96)return "SS";
  if(level>=81)return "S";
  if(level>=66)return "A";
  if(level>=51)return "B";
  if(level>=36)return "C";
  if(level>=21)return "D";
  if(level>=11)return "E";
  return "F";
 }
 public static long saturatingAdd(long first,long second) {
  if(first<0||second<0)throw new IllegalArgumentException("Essence cannot be negative");
  return second>Long.MAX_VALUE-first?Long.MAX_VALUE:first+second;
 }
}