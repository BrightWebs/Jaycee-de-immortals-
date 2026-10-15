package com.brightwebs.jaycee.client;
import net.minecraft.client.gui.GuiGraphics;

/** Centralized procedural ability/navigation art, inspired by silver-blue fantasy ornament. */
public final class DivineGlyph {
 private static final int[] COLORS={0xFFCB5CFF,0xFFF4CD82,0xFF75C6FF,0xFFF77667,0xFF7DF7A6,0xFF8CC8FF};
 public static void icon(GuiGraphics g,int x,int y,int size,int id,boolean selected){
  int col=COLORS[Math.floorMod(id,COLORS.length)];
  g.fill(x-1,y-1,x+size+1,y+size+1,selected?0xFFB8E7FF:0xFF355D9A);
  g.fill(x,y,x+size,y+size,0xFF100E2D);
  int cx=x+size/2,cy=y+size/2,r=Math.max(2,size/2-3);
  for(int d=-r;d<=r;d++){
   int span=r-Math.abs(d);
   if(id%3==0){g.fill(cx-span,cy+d,cx+span+1,cy+d+1,0xAA4C216E);if(d%3==0)g.fill(cx-span,cy+d,cx-span+2,cy+d+1,col);}
   else if(id%3==1){g.fill(cx-1,cy+d,cx+1,cy+d+1,col);if(Math.abs(d)<r/2)g.fill(cx-span,cy+d,cx+span+1,cy+d+1,0x888B79F5);}
   else{if(d%2==0){g.fill(cx-span,cy+d,cx-span+2,cy+d+1,col);g.fill(cx+span-1,cy+d,cx+span+1,cy+d+1,col);}}
  }
  g.fill(cx-2,cy-2,cx+2,cy+2,0xFFE7F8FF);
  // Stable binary corner marks distinguish abilities even when base glyph motifs repeat.
  for(int bit=0;bit<4;bit++)if((id&(1<<bit))!=0)
   g.fill(x+2+bit*3,y+size-3,x+4+bit*3,y+size-1,0xFFE9D7FF);
 }
 static int color(int id){return COLORS[Math.floorMod(id,COLORS.length)];}
 /** Six unboxed, 19-pixel silver-blue symbols centered above navigation labels. */
 public static void navigation(GuiGraphics g,int cx,int cy,int id,boolean selected){
  int c=selected?0xFFD2F2FF:0xFF91AFC4;
  int dim=selected?0xFF729FBA:0xFF526D85;
  int light=selected?0xFFF0FCFF:0xFFB4CDD9;
  switch(Math.floorMod(id,6)){
   case 0 -> {
    DivineFrame.stroke(g,cx-3,cy-6,cx-2,cy-8,c);
    DivineFrame.stroke(g,cx-2,cy-8,cx+2,cy-8,c);
    DivineFrame.stroke(g,cx+2,cy-8,cx+3,cy-6,c);
    DivineFrame.line(g,cx-3,cy-5,1,4,c);DivineFrame.line(g,cx+3,cy-5,1,4,c);
    DivineFrame.stroke(g,cx-3,cy-1,cx,cy+1,light);
    DivineFrame.stroke(g,cx,cy+1,cx+3,cy-1,light);
    DivineFrame.stroke(g,cx-2,cy+2,cx-7,cy+4,c);
    DivineFrame.stroke(g,cx+2,cy+2,cx+7,cy+4,c);
    DivineFrame.stroke(g,cx-7,cy+4,cx-8,cy+8,c);
    DivineFrame.stroke(g,cx+7,cy+4,cx+8,cy+8,c);
    DivineFrame.line(g,cx-8,cy+8,17,1,c);
    DivineFrame.stroke(g,cx-3,cy+3,cx,cy+6,dim);
    DivineFrame.stroke(g,cx,cy+6,cx+3,cy+3,dim);
   }
   case 1 -> {
    path(g,cx,cy,c,new int[]{0,-9,-4,-4,-3,-1,-6,-3,-7,2,-5,6,0,8,5,6,7,2,5,-3,3,0,2,-5,0,-9});
    path(g,cx,cy,light,new int[]{0,-1,-3,4,0,7,3,4,0,-1});
    DivineFrame.line(g,cx,cy+3,1,4,dim);
   }
   case 2 -> {
    DivineFrame.line(g,cx,cy-3,1,12,c);
    path(g,cx,cy,c,new int[]{-7,-4,-7,0,0,4,7,0,7,-4});
    DivineFrame.stroke(g,cx-4,cy+7,cx,cy+4,dim);
    DivineFrame.stroke(g,cx,cy+4,cx+4,cy+7,dim);
    DivineFrame.diamond(g,cx,cy-6,3,light);
    DivineFrame.diamond(g,cx-7,cy-5,2,c);
    DivineFrame.diamond(g,cx+7,cy-5,2,c);
    DivineFrame.gem(g,cx,cy+8,1,light);
   }
   case 3 -> {
    for(int i=0;i<32;i++){
     double a=i*Math.PI/16,b=(i+1)*Math.PI/16;
     DivineFrame.stroke(g,cx+(int)Math.round(Math.cos(a)*7),cy+(int)Math.round(Math.sin(a)*7),
       cx+(int)Math.round(Math.cos(b)*7),cy+(int)Math.round(Math.sin(b)*7),c);
    }
    DivineFrame.diamond(g,cx,cy,4,dim);
    DivineFrame.line(g,cx-9,cy,4,1,light);DivineFrame.line(g,cx+6,cy,4,1,light);
    DivineFrame.line(g,cx,cy-9,1,4,light);DivineFrame.line(g,cx,cy+6,1,4,light);
    DivineFrame.gem(g,cx,cy,1,light);
   }
   case 4 -> {
    path(g,cx,cy,c,new int[]{-6,5,-7,1,-5,-4,0,-7,7,-8,7,-2,5,3,1,6,-4,6});
    DivineFrame.stroke(g,cx-8,cy+9,cx+5,cy-6,light);
    path(g,cx,cy,dim,new int[]{-4,-2,-3,3,2,3});
    path(g,cx,cy,dim,new int[]{0,-5,0,-1,5,-1});
   }
   case 5 -> {
    path(g,cx,cy,c,new int[]{-6,-4,-8,-4,-8,-7,-6,-8,5,-8,7,-6,7,5});
    path(g,cx,cy,c,new int[]{-5,-6,-5,5,-7,5,-7,7,-5,8,6,8,8,6,8,4,5,4,5,6});
    DivineFrame.line(g,cx-5,cy-5,3,1,dim);
    DivineFrame.line(g,cx-2,cy-3,6,1,light);
    DivineFrame.line(g,cx-2,cy,5,1,c);
    DivineFrame.line(g,cx-2,cy+3,4,1,dim);
   }
  }
 }
 private static void path(GuiGraphics g,int cx,int cy,int color,int[] points){
  for(int i=2;i<points.length;i+=2)
   DivineFrame.stroke(g,cx+points[i-2],cy+points[i-1],cx+points[i],cy+points[i+1],color);
 }
}
