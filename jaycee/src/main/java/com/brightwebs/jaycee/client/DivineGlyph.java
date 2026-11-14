package com.brightwebs.jaycee.client;
import net.minecraft.client.gui.GuiGraphics;

/** Procedural per-ability glyphs. No screenshot textures or fake static UI. */
final class DivineGlyph {
 private static final int[] COLORS={0xFFCB5CFF,0xFFF4CD82,0xFF75C6FF,0xFFF77667,0xFF7DF7A6,0xFF8CC8FF};
 static void icon(GuiGraphics g,int x,int y,int size,int id,boolean selected){
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
 }
 static int color(int id){return COLORS[Math.floorMod(id,COLORS.length)];}
}
