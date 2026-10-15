package com.brightwebs.jaycee.client;
import net.minecraft.client.gui.GuiGraphics;

/** Centralized, reference-inspired navy glass and silver-blue procedural ornament.
 * Drawn independently of controls/text; an interpretation, not a reproduction of the reference. */
public final class DivineFrame {
 static final int LIGHT=0xFF9BDFFF, EDGE=0xFF548DCC, DARK=0xFF081528, GOLD=0xFFF3CF82;
 static void rect(GuiGraphics g,int x,int y,int w,int h,int c){g.fill(x,y,x+w,y+h,c);}
 static void line(GuiGraphics g,int x,int y,int w,int h,int c){if(w>0&&h>0)g.fill(x,y,x+w,y+h,c);}
 static void gem(GuiGraphics g,int cx,int cy,int size,int c){
  for(int d=-size;d<=size;d++)line(g,cx-(size-Math.abs(d)),cy+d,2*(size-Math.abs(d))+1,1,c);
 }
 public static void frame(GuiGraphics g,int x,int y,int w,int h,boolean active){
  if(w<12||h<12)return;
  int c=active?0xFFC2E6F4:0xFF7898B2;
  int cut=Math.min(9,Math.min(w,h)/4);
  // Scanline glass keeps the clipped corners transparent.
  line(g,x+1,y+cut,w-2,h/3-cut,0xDF081424);
  line(g,x+1,y+Math.max(cut,h/3),w-2,h-cut-Math.max(cut,h/3),0xEC081424);
  for(int row=1;row<cut;row++){
   int inset=Math.max(1,cut-Math.min(row,h-1-row));
   line(g,x+inset,y+row,w-2*inset,1,0xDF081424);
   line(g,x+inset,y+h-1-row,w-2*inset,1,0xEC081424);
  }
  chamfer(g,x,y,w,h,cut,0x503E91BB);
  chamfer(g,x+1,y+1,w-2,h-2,cut-1,c);
  if(w>=26&&h>=26)chamfer(g,x+5,y+5,w-10,h-10,Math.max(2,cut-3),0xAA58778E);
  line(g,x+cut+3,y+3,w-2*cut-6,1,0x3455BEE8);
  if(w>=90&&h>=40){
   for(int sx:new int[]{-1,1})for(int sy:new int[]{-1,1}){
    int ox=sx<0?x+8:x+w-9,oy=sy<0?y+8:y+h-9;
    flourish(g,ox,oy,-sx,-sy,c);
   }
   int cx=x+w/2;
   for(int yy:new int[]{y+5,y+h-6}){
    stroke(g,cx-15,yy,cx-5,yy,0xFF617E94);
    stroke(g,cx+5,yy,cx+15,yy,0xFF617E94);
    diamond(g,cx,yy,4,c);
    gem(g,cx,yy,1,active?0xFFB9F5FF:0xFF79BED8);
   }
  }
 }
 private static void chamfer(GuiGraphics g,int x,int y,int w,int h,int cut,int color){
  line(g,x+cut,y,w-2*cut,1,color);line(g,x+cut,y+h-1,w-2*cut,1,color);
  line(g,x,y+cut,1,h-2*cut,color);line(g,x+w-1,y+cut,1,h-2*cut,color);
  for(int i=0;i<cut;i++){
   line(g,x+cut-i,y+i,1,1,color);line(g,x+w-1-cut+i,y+i,1,1,color);
   line(g,x+cut-i,y+h-1-i,1,1,color);line(g,x+w-1-cut+i,y+h-1-i,1,1,color);
  }
 }
 private static void flourish(GuiGraphics g,int x,int y,int sx,int sy,int color){
  int[] px={0,5,11,15,13,9,7,9,12},py={10,6,5,2,0,1,4,7,7};
  for(int i=1;i<px.length;i++)
   stroke(g,x+sx*px[i-1],y+sy*py[i-1],x+sx*px[i],y+sy*py[i],color);
  stroke(g,x,y+sy*10,x,y+sy*17,0x997BA9C3);
  diamond(g,x+sx*3,y+sy*3,2,0xFF80BBD2);
 }
 static void stroke(GuiGraphics g,int x1,int y1,int x2,int y2,int color){
  int steps=Math.max(Math.abs(x2-x1),Math.abs(y2-y1));
  if(x1==x2||y1==y2){
   line(g,Math.min(x1,x2),Math.min(y1,y2),Math.abs(x2-x1)+1,Math.abs(y2-y1)+1,color);
   return;
  }
  for(int i=0;i<=steps;i++)
   line(g,x1+(int)Math.round((x2-x1)*(double)i/steps),y1+(int)Math.round((y2-y1)*(double)i/steps),1,1,color);
 }
 static void diamond(GuiGraphics g,int cx,int cy,int r,int color){
  stroke(g,cx,cy-r,cx+r,cy,color);stroke(g,cx+r,cy,cx,cy+r,color);
  stroke(g,cx,cy+r,cx-r,cy,color);stroke(g,cx-r,cy,cx,cy-r,color);
 }
 /** Animated seal; scanline rings and capped detail keep even large radii below 2,000 fills. */
 public static void sigil(GuiGraphics g,int cx,int cy,int radius,long ticks){
  if(radius<6)return;
  // Detail is capped independently of the requested size: large seals retain fine broken arcs.
  int detail=Math.min(radius,64);
  double turn=(ticks%24000L)*Math.PI/1200.0;
  ring(g,cx,cy,radius,.98,detail,0x805B90AD);
  ring(g,cx,cy,radius,.92,detail,0xFF8ABFD6);
  ring(g,cx,cy,radius,.70,detail,0xB96ABAD9);
  ring(g,cx,cy,radius,.64,detail,0x605997BD);
  // Two inclined, dotted orbital ellipses cross the inner circle.
  for(int orbit=0;orbit<2;orbit++)for(int i=0;i<80;i++){
   double a=i*Math.PI/40+turn*(orbit==0?1:-1);
   double u=Math.cos(a)*radius*.84,v=Math.sin(a)*radius*.28;
   double tilt=orbit==0?.58:-.58;
   int xx=cx+(int)Math.round(u*Math.cos(tilt)-v*Math.sin(tilt));
   int yy=cy+(int)Math.round(u*Math.sin(tilt)+v*Math.cos(tilt));
   line(g,xx,yy,1,1,0x886CB8D9);
  }
  for(int i=0;i<12;i++){
   double a=i*Math.PI/6-Math.PI/2;
   int xx=cx+(int)Math.round(Math.cos(a)*radius*.82);
   int yy=cy+(int)Math.round(Math.sin(a)*radius*.82);
   int r=i%3==0?3:2;
   diamond(g,xx,yy,r,0xFF9ACBDD);
   if(i%3==0){
    gem(g,xx,yy,1,0xFFE0FAFF);
    int tx=cx+(int)Math.round(Math.cos(a)*radius*.98);
    int ty=cy+(int)Math.round(Math.sin(a)*radius*.98);
    gem(g,tx,ty,2,0x5549CFFF);gem(g,tx,ty,0,0xFFBEF6FF);
   }
  }
  // Short rune strokes sit between the orbit diamonds, leaving negative space.
  for(int i=0;i<24;i++){
   double a=(i+.5)*Math.PI/12;
   int xx=cx+(int)Math.round(Math.cos(a)*radius*.96);
   int yy=cy+(int)Math.round(Math.sin(a)*radius*.96);
   int dx=(int)Math.round(Math.cos(a)*2),dy=(int)Math.round(Math.sin(a)*2);
   stroke(g,xx-dx,yy-dy,xx+dx,yy+dy,0xBB8AB7CA);
  }
  flame(g,cx,cy,Math.min(radius*.53,48),turn);
 }
 private static void ring(GuiGraphics g,int cx,int cy,int radius,double scale,int detail,int color){
  int r=Math.max(1,(int)Math.round(radius*scale));
  int samples=Math.max(4,(int)Math.round(detail*scale));
  for(int i=-samples;i<=samples;i++){
   double ny=i/(double)samples;
   int yy=(int)Math.round(ny*r);
   double near=Math.max(0,Math.abs(ny)-.5/samples);
   double far=Math.min(1,Math.abs(ny)+.5/samples);
   int outer=(int)Math.round(Math.sqrt(1-near*near)*r);
   int inner=(int)Math.round(Math.sqrt(1-far*far)*r);
   line(g,cx-outer,cy+yy,outer-inner+1,1,color);
   if(inner!=0)line(g,cx+inner,cy+yy,outer-inner+1,1,color);
  }
 }
 private static void flame(GuiGraphics g,int cx,int cy,double r,double time){
  int rows=Math.max(5,(int)Math.ceil(r*2));
  for(int i=0;i<=rows;i++){
   double t=i/(double)rows;
   int yy=cy+(int)Math.round((t-.58)*r*1.7);
   double taper=Math.sin(Math.PI*t);
   int width=(int)Math.round(r*.34*taper*(.45+t));
   int sway=(int)Math.round(Math.sin(t*7-time)*r*.10*(1-t));
   line(g,cx+sway-width-2,yy,width*2+5,1,0x2549BFFF);
   line(g,cx+sway-width,yy,width*2+1,1,0xC33F9FEF);
   int core=Math.max(0,(int)Math.round(width*.38));
   line(g,cx+sway-core,yy,core*2+1,1,0xFFBBF0FF);
   if(t>.2&&t<.86){
    double wing=Math.sin((t-.2)/.66*Math.PI);
    int reach=(int)Math.round(r*(.35+wing*.62));
    int lift=(int)Math.round(r*.28*wing);
    for(int side:new int[]{-1,1}){
     int xx=cx+side*reach;
     line(g,xx,yy-lift,2,1,0xA75CCFFF);
     line(g,cx+side*(reach-3),yy-lift+2,2,1,0x6055AEF2);
    }
   }
  }
  gem(g,cx,cy+(int)(r*.35),2,0xFFE8FCFF);
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
 public static void portrait(GuiGraphics g,int x,int y,int w,int h){
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
 public static void divider(GuiGraphics g,int x,int y,int w){
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