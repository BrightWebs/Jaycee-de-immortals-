package com.brightwebs.jaycee.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Measured text boxes keep glyphs out of frame ornaments without shrinking the font. */
final class UiText {
 private UiText(){}
 static String keyLabel(Component key,int width){
  String name=key.getString();
  if(Minecraft.getInstance().font.width(name)<=width)return name;
  if(key.getContents() instanceof TranslatableContents translated){
   String id=translated.getKey();
   String side=id.startsWith("key.keyboard.left.")?"L":id.startsWith("key.keyboard.right.")?"R":"";
   if(!side.isEmpty()){
    String suffix=switch(id.substring(id.lastIndexOf('.')+1)){
     case "shift"->"S";case "control"->"C";case "alt"->"A";case "win"->"W";default->"";
    };
    if(!suffix.isEmpty())return side+suffix;
   }
  }
  StringBuilder initials=new StringBuilder();
  for(String word:name.split("\\s+"))if(!word.isEmpty()){
   if(word.codePoints().allMatch(Character::isDigit))initials.append(word);
   else initials.appendCodePoint(word.codePointAt(0));
  }
  return initials.length()>1?initials.toString():name;
 }
 static String fit(String value,int width,float scale){
  var font=Minecraft.getInstance().font;
  int pixels=Math.max(0,(int)Math.floor(width/scale));
  if(font.width(value)<=pixels)return value;
  String end="…";
  if(font.width(end)>pixels)return "";
  return font.plainSubstrByWidth(value,pixels-font.width(end))+end;
 }
 static void line(GuiGraphics g,String value,int x,int y,int width,float scale,int color,boolean centered){
  String fitted=fit(value,width,scale);
  float offset=centered?(width-Minecraft.getInstance().font.width(fitted)*scale)/2:0;
  g.pose().pushPose();
  g.pose().translate(x+offset,y,0);g.pose().scale(scale,scale,1);
  g.drawString(Minecraft.getInstance().font,fitted,0,0,color,false);
  g.pose().popPose();
 }
 static void paragraph(GuiGraphics g,String value,int x,int y,int width,int height,float scale,int color){
  var font=Minecraft.getInstance().font;
  int step=(int)Math.ceil(font.lineHeight*scale)+2;
  int limit=Math.max(0,(height+2)/step);
  var lines=font.split(Component.literal(value),Math.max(1,(int)Math.floor(width/scale)));
  g.pose().pushPose();
  g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);
  for(int i=0;i<Math.min(limit,lines.size());i++){
   FormattedCharSequence line=i==limit-1&&lines.size()>limit?Component.literal("…").getVisualOrderText():lines.get(i);
   g.drawString(font,line,0,Math.round(i*step/scale),color,false);
  }
  g.pose().popPose();
 }
 static void tooltip(GuiGraphics g,List<Component> lines,int x,int y){
  var font=Minecraft.getInstance().font;
  int width=Math.max(1,Math.min(260,g.guiWidth()-24));
  int limit=Math.max(1,(g.guiHeight()-24)/(font.lineHeight+1));
  List<FormattedCharSequence> wrapped=new ArrayList<>();
  for(var line:lines)wrapped.addAll(font.split(line,width));
  if(wrapped.size()>limit){
   wrapped=new ArrayList<>(wrapped.subList(0,limit));
   wrapped.set(limit-1,Component.literal("…").getVisualOrderText());
  }
  g.renderTooltip(font,wrapped,x,y);
 }
}
