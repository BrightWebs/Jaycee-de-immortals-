package com.brightwebs.jaycee.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class JayceeScreen extends Screen {
 private static final String[] TABS={"Status","Abilities","Skills","Magic","Species","Quests"};
 private int tab=0,page=0,selected=0;
 private int left,top;
 public JayceeScreen(){super(Component.literal("Jaycee : Di Immortales"));}
 @Override protected void init(){left=(width-520)/2;top=(height-310)/2; for(int i=0;i<6;i++){final int t=i;addRenderableWidget(Button.builder(Component.literal(TABS[i]),b->{tab=t;}).bounds(left+8+i*84,top+10,82,27).build());}addRenderableWidget(Button.builder(Component.literal("<"),b->page=(page+9)%10).bounds(left+420,top+59,22,20).build());addRenderableWidget(Button.builder(Component.literal(">"),b->page=(page+1)%10).bounds(left+490,top+59,22,20).build());for(int i=0;i<4;i++){final int s=i;addRenderableWidget(Button.builder(Component.literal(""+(i+1)),b->selected=s).bounds(left+420+(i%2)*43,top+100+(i/2)*52,39,39).build());}}
 private void frame(GuiGraphics g,int x,int y,int w,int h,int line){g.fill(x,y,x+w,y+h,0xDE061125);g.fill(x,y,x+w,y+1,line);g.fill(x,y+h-1,x+w,y+h,line);g.fill(x,y,x+1,y+h,line);g.fill(x+w-1,y,x+w,y+h,line);g.fill(x+5,y+5,x+18,y+6,0xFF8FCBFF);g.fill(x+5,y+5,x+6,y+18,0xFF8FCBFF);}
 @Override public void render(GuiGraphics g,int mx,int my,float dt){renderBackground(g,mx,my,dt);frame(g,left,top,520,310,0xFF4886CC);frame(g,left+5,top+46,130,255,0xFF4076BA);frame(g,left+141,top+46,270,255,0xFF4076BA);frame(g,left+415,top+46,100,255,0xFF4076BA);frame(g,left+6+tab*84,top+8,85,31,0xFF9CE7FF);g.drawString(font,"JAYCEE",left+20,top+58,0xFFE2F5FF);g.drawString(font,Minecraft.getInstance().player==null?"Player":Minecraft.getInstance().player.getName().getString(),left+19,top+75,0xFF85C3FF);g.drawString(font,TABS[tab].toUpperCase(),left+154,top+60,0xFFEAF6FF);g.drawString(font,"Equip Page "+(page+1)+" / 10",left+423,top+86,0xFFCFDFFF);for(int i=0;i<4;i++)frame(g,left+419+(i%2)*43,top+99+(i/2)*52,41,41,i==selected?0xFFBF8AFF:0xFF42689A);if(tab==0){g.drawString(font,"PLAYER PROFILE",left+156,top+83,0xFF79B7FF);if(Minecraft.getInstance().player!=null)InventoryScreen.renderEntityInInventoryFollowsMouse(g,left+197,top+111,left+340,top+260,45,0f,230f,120f,Minecraft.getInstance().player);g.drawString(font,"HP  /  Mana  /  Soul",left+21,top+156,0xFFC3E1FF);}else if(tab==1){String[] categories={"All","Offense","Defense","Support","Movement","Reality"};for(int i=0;i<categories.length;i++)g.drawString(font,categories[i],left+153,top+95+i*28,0xFF9CCAFF);g.drawString(font,"Ability library: coming next",left+260,top+112,0xFFDBE8FF);}else if(tab==2){String[] names={"Body","Spirit","Divine","Utility"};for(int i=0;i<4;i++){int x=left+173+i*56;g.drawString(font,names[i],x,top+99,0xFFADDFFF);for(int j=0;j<4;j++){int cy=top+134+j*36;g.fill(x+8,cy,x+18,cy+10,j<2?0xFF75BAFA:0xFF3A4D77);}}}else g.drawString(font,"Preparing the "+TABS[tab]+" interface",left+157,top+116,0xFFC8DEFF);super.render(g,mx,my,dt);}
 @Override public boolean isPauseScreen(){return false;}
}
