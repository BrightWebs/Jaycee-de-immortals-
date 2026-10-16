package com.brightwebs.jaycee.client;

/** Reference-image coordinates shared by rendering, widgets and pointer input. */
final class SapphireLayout {
 static final int WIDTH=700, HEIGHT=420;
 record Bounds(int x,int y,int width,int height){
  boolean contains(double px,double py){return px>=x&&py>=y&&px<x+width&&py<y+height;}
 }
 static Bounds slot(int slot){return new Bounds(556+(slot%2)*68,158+(slot/2)*68,60,60);}
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
