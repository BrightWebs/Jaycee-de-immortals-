package com.brightwebs.jaycee.client;
import net.minecraft.client.gui.GuiGraphics;

/** Nine-slice-like procedural sapphire ornament drawn independently of all controls/text.
 * All values are in GUI pixels and remain crisp at Minecraft's integer UI scales. */
final class DivineFrame {
 static final int LIGHT=0xFF9BDFFF, EDGE=0xFF548DCC, DARK=0xFF081528, GOLD=0xFFF3CF82;
 static void rect(GuiGraphics g,int x,int y,int w,int h,int c){g.fill(x,y,x+w,y+h,c);}
 static void line(GuiGraphics g,int x,int y,int w,int h,int c){if(w>0&&h>0)g.fill(x,y,x+w,y+h,c);}
 static void gem(GuiGraphics g,int cx,int cy,int size,int c){
  for(int d=-size;d<=size;d++)line(g,cx-(size-Math.abs(d)),cy+d,2*(size-Math.abs(d))+1,1,c);
 }
 static void frame(GuiGraphics g,int x,int y,int w,int h,boolean active){
  if(w<12||h<12)return;
  int c=active?LIGHT:EDGE;
  rect(g,x+4,y+2,w-8,h-4,0xEC071328);
  rect(g,x+2,y+4,w-4,h-8,0xEC071328);
  rect(g,x+7,y+6,w-14,h-12,0xCD132745);
  // Layered inset edge and sapphire specular glint.
  line(g,x+7,y+2,w-14,1,c);line(g,x+7,y+h-3,w-14,1,c);
  line(g,x+2,y+7,1,h-14,c);line(g,x+w-3,y+7,1,h-14,c);
  line(g,x+10,y+6,Math.min(w-20,35),1,0xA382C9FF);
  line(g,x+6,y+10,1,Math.min(h-20,30),0x8873B7F5);
  line(g,x+w-7,y+h-10,1,Math.min(h-20,30),0x885CA1E9);
  // Chamfered corners and small inset diamonds create the ornamental fantasy outline.
  for(int i=0;i<5;i++){
   line(g,x+7-i,y+2+i,1,1,c);
   line(g,x+w-8+i,y+2+i,1,1,c);
   line(g,x+7-i,y+h-3-i,1,1,c);
   line(g,x+w-8+i,y+h-3-i,1,1,c);
  }
  line(g,x+7,y+5,Math.min(14,w/4),1,0xFF9CCFFF);
  line(g,x+w-7-Math.min(14,w/4),y+h-6,Math.min(14,w/4),1,0xFF8CCAFF);
  if(w>58){gem(g,x+w/2,y+3,2,c);gem(g,x+w/2,y+h-4,2,c);}
  if(active){
   line(g,x+12,y+8,w-24,1,0x665DAAFF);
   line(g,x+12,y+h-9,w-24,1,0x665DAAFF);
  }
 }
 /** Recessed illuminated ribbon and symmetric filigree; labels/widgets draw separately. */
 static void ribbon(GuiGraphics g,int x,int y,int w,int h){
  rect(g,x+6,y+2,w-12,h-4,0xCA0C2445);
  line(g,x+10,y+1,w-20,1,0xFF5186BD);
  line(g,x+10,y+h-2,w-20,1,0xFF3D6A9B);
  gem(g,x+9,y+h/2,3,0xFF8FCCF1);
  gem(g,x+w-10,y+h/2,3,0xFF8FCCF1);
  for(int k=0;k<3;k++){
   line(g,x+14+k*3,y+4+k,7,1,0x88649CD2);
   line(g,x+w-21-k*3,y+4+k,7,1,0x88649CD2);
  }
 }
 /** Fine gold/silver side flourishes framing the player without obscuring the live model. */
 static void portrait(GuiGraphics g,int x,int y,int w,int h){
  frame(g,x,y,w,h,true);
  line(g,x+7,y+11,1,h-22,0x9979BDF2);
  line(g,x+w-8,y+11,1,h-22,0x9979BDF2);
  gem(g,x+w/2,y+6,4,GOLD);
  gem(g,x+w/2,y+h-7,4,GOLD);
  for(int i=0;i<4;i++){
   line(g,x+12+i*2,y+12+i*2,10,1,0x7775AFE1);
   line(g,x+w-22-i*2,y+12+i*2,10,1,0x7775AFE1);
  }
 }
 static void divider(GuiGraphics g,int x,int y,int w){
  line(g,x,y,w,1,0xFF365C90);gem(g,x+w/2,y,2,0xFF86C3F5);
 }
 static void jewel(GuiGraphics g,int cx,int cy,long ticks,int color){
  double theta=ticks*.015;
  for(int i=0;i<12;i++){
   double a=theta+Math.PI*2*i/12;
   int x=cx+(int)(Math.cos(a)*31),y=cy+(int)(Math.sin(a)*31);
   gem(g,x,y,i%3==0?3:1,0xFF4D8BD4);
  }
  for(int size=25;size>12;size-=3){
   int c=size%2==0?0x653DA3EF:0x895DA8F5;
   for(int i=0;i<24;i++){
    double a=2*Math.PI*i/24;
    int x=cx+(int)(Math.cos(a)*size),y=cy+(int)(Math.sin(a)*size);
    line(g,x,y,1,1,c);
   }
  }
  gem(g,cx,cy,10,color);
  gem(g,cx,cy,5,0xFFBDEBFF);
  gem(g,cx,cy,2,0xFFF9FEFF);
 }
}