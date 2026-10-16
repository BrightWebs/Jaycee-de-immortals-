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
 /** The original Sapphire's dark glass, nested chamfers and silver-blue filigree. */
 static void sapphire(GuiGraphics g,int x,int y,int w,int h,boolean active){
  int edge=active?0xFFB4DCEC:0xFF6B98AE;
  int cut=Math.min(8,Math.min(w,h)/3);
  for(int row=1;row<h-1;row++){
   int inset=Math.max(1,cut-Math.min(row,h-1-row));
   line(g,x+inset,y+row,w-2*inset,1,active?0xED173A59:0xED081523);
  }
  chamfer(g,x,y,w,h,cut,0xFF355D74);
  chamfer(g,x+2,y+2,w-4,h-4,Math.max(2,cut-1),edge);
  if(w>45&&h>28){
   chamfer(g,x+5,y+5,w-10,h-10,Math.max(2,cut-2),0xFF3E6176);
   gem(g,x+w/2,y+3,2,edge);gem(g,x+w/2,y+h-4,2,edge);
   for(int side=0;side<2;side++)for(int end=0;end<2;end++){
    int cx=side==0?x+11:x+w-12,cy=end==0?y+10:y+h-11;
    int sx=side==0?1:-1,sy=end==0?1:-1;
    for(int i=0;i<3;i++){
     line(g,cx+sx*i*3-(sx<0?5:0),cy+sy*i*2,5,1,edge);
     line(g,cx+sx*i*3,cy+sy*(i*2+1),1,2,0xFF48788F);
    }
    gem(g,cx,cy,1,0xFF9FCBDA);
   }
  }
 }
 private static void chamfer(GuiGraphics g,int x,int y,int w,int h,int cut,int color){
  line(g,x+cut,y,w-2*cut,1,color);line(g,x+cut,y+h-1,w-2*cut,1,color);
  line(g,x,y+cut,1,h-2*cut,color);line(g,x+w-1,y+cut,1,h-2*cut,color);
  for(int i=1;i<cut;i++){
   line(g,x+cut-i,y+i,1,1,color);line(g,x+w-cut-1+i,y+i,1,1,color);
   line(g,x+cut-i,y+h-1-i,1,1,color);line(g,x+w-cut-1+i,y+h-1-i,1,1,color);
  }
 }
 static void tabIcon(GuiGraphics g,int cx,int cy,int id,boolean active){
  int color=active?0xFFB9E6F5:0xFF85AFBF;
  if(id==0){
   chamfer(g,cx-2,cy-8,5,7,1,color);
   line(g,cx-5,cy+1,11,1,color);line(g,cx-7,cy+5,15,1,color);
   line(g,cx-5,cy+2,1,3,color);line(g,cx+5,cy+2,1,3,color);
  }else if(id==1){
   for(int i=0;i<3;i++)chamfer(g,cx-5+i*3,cy-7+i*2,5,12-i*2,2,color);
   line(g,cx-6,cy+6,13,1,color);
  }else if(id==2){
   line(g,cx,cy-6,1,15,color);line(g,cx-6,cy-4,13,1,color);
   gem(g,cx,cy-8,2,color);gem(g,cx-7,cy-6,2,color);gem(g,cx+7,cy-6,2,color);
   line(g,cx-4,cy+7,9,1,color);
  }else if(id==3){
   for(int i=0;i<36;i++){
    double a=i*Math.PI/18;
    line(g,cx+(int)(Math.cos(a)*7),cy+(int)(Math.sin(a)*7),1,1,color);
   }
   gem(g,cx,cy,2,color);line(g,cx-10,cy,4,1,color);line(g,cx+7,cy,4,1,color);
   line(g,cx,cy-10,1,4,color);line(g,cx,cy+7,1,4,color);
  }else{
   chamfer(g,cx-6,cy-7,12,15,1,color);
   for(int i=0;i<3;i++)line(g,cx-3,cy-3+i*4,6,1,color);
  }
 }
 static void orbit(GuiGraphics g,int cx,int cy,double ticks){
  double turn=ticks*.004;
  for(int ring=0;ring<5;ring++){
   double radius=43+ring*6;
   for(int i=0;i<180;i++){
    if((i+ring*7)%45<4)continue;
    double a=i*Math.PI/90+(ring%2==0?turn:-turn);
    int x=cx+(int)(Math.cos(a)*radius),y=cy+(int)(Math.sin(a)*radius);
    line(g,x,y,i%3==0?2:1,1,ring%2==0?0xFF629EB5:0xFF345E79);
   }
  }
  for(int i=0;i<12;i++){
   double a=i*Math.PI/6+turn*.5;
   int x=cx+(int)(Math.cos(a)*56),y=cy+(int)(Math.sin(a)*56);
   gem(g,x,y,i%3==0?2:1,0xFFA1D7E5);
  }
  // Counter-rotating elliptical strands retain the reference's luminous orbital eye.
  for(int strand=0;strand<9;strand++)for(int i=0;i<150;i++){
   double a=i*Math.PI/75,tilt=turn*(strand%2==0?1:-1)+strand*Math.PI/9;
   double x=Math.cos(a)*(27+strand*.35),y=Math.sin(a)*(9+strand*.8);
   int px=cx+(int)(x*Math.cos(tilt)-y*Math.sin(tilt));
   int py=cy+(int)(x*Math.sin(tilt)+y*Math.cos(tilt));
   line(g,px,py,1,1,strand%3==0?0xFFDBE4FA:strand%3==1?0xFF9CD6E6:0xFF8BA8E5);
  }
  gem(g,cx,cy,4,0xFF183149);gem(g,cx,cy,1,0xFFECFCFF);
 }
}