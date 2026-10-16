package com.brightwebs.jaycee.client;

/** Reference-image coordinates shared by rendering, widgets and pointer input. */
final class SapphireLayout {
 static final int WIDTH=700, HEIGHT=420;
 static boolean ordinaryMenu(boolean supreme,boolean preview){return !supreme||preview;}
 static final Bounds TRAIT_CARD=new Bounds(382,142,134,88),AFFINITY_CARD=new Bounds(382,238,134,164);
 static int statusScrollTarget(double x,double y){return TRAIT_CARD.contains(x,y)?1:AFFINITY_CARD.contains(x,y)?2:0;}
 static int scrollOffset(int offset,int delta,int content,int visible){
  return (int)Math.clamp((long)offset+delta,0,Math.max(0,content-visible));
 }
 record Bounds(int x,int y,int width,int height){
  boolean contains(double px,double py){return px>=x&&py>=y&&px<x+width&&py<y+height;}
 }
 static Bounds slot(int slot){return new Bounds(556+(slot%2)*68,154+(slot/2)*76,60,60);}
 static Bounds slotNumber(int slot){var b=slot(slot);return new Bounds(b.x()+2,b.y()-11,10,9);}
 static Bounds slotState(int slot){var b=slot(slot);return new Bounds(b.x()+14,b.y()-10,46,8);}
 static Bounds slotCaption(int slot){var b=slot(slot);return new Bounds(b.x()+9,b.y()+36,42,8);}
 static Bounds tabLabel(int tab){return new Bounds(34+tab*103,41,51,9);}
 static Bounds tabButton(int tab,boolean ordinary){int step=ordinary?86:103;return new Bounds(10+tab*step,7,step-4,49);}
 static Bounds tabLabel(int tab,boolean ordinary){return ordinary?new Bounds(14+tab*86,41,74,9):tabLabel(tab);}
 static Bounds pageSelector(int page){return new Bounds(554+page*13,294,page==9?18:12,17);}
 final double scale;
 final double left,top;
 SapphireLayout(int width,int height){
  scale=Math.min(1,Math.min(Math.max(1,width-8)/(double)WIDTH,Math.max(1,height-8)/(double)HEIGHT));
  left=(width-WIDTH*scale)/2;
  top=(height-HEIGHT*scale)/2;
 }
 double localX(double x){return (x-left)/scale;}
 double localY(double y){return (y-top)/scale;}
 int screenX(int x){return (int)Math.round(left+x*scale);}
 int screenY(int y){return (int)Math.round(top+y*scale);}
 int size(int size){return Math.max(1,(int)Math.round(size*scale));}
}
