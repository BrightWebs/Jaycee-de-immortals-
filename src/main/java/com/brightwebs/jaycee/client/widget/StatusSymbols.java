package com.brightwebs.jaycee.client.widget;

import net.minecraft.client.gui.GuiGraphics;

/** Small reference-inspired silhouettes; replace here with icon textures for final artwork. */
public final class StatusSymbols {
 private static final String[][] STATS={
  {" ##   ## ","#### ####","#########","#########"," ####### ","  #####  ","   ###   ","    #    "},
  {"    #    ","    #    ","   ###   ","#########"," ####### ","   ###   ","  ## ##  "," ##   ## "},
  {"       ##","      ###","     ### "," #  ###  "," #####   ","  ###    "," ## ##   ","##   #   "},
  {" ####### ","#########","##     ##","##     ##"," ##   ## ","  ## ##  ","   ###   ","    #    "},
  {"  ###    ","  ###    ","  ###    ","  ###    ","  #####  "," ####### ","#########","####  ###"},
  {" ##   ## ","#### ####"," ####### ","  #####  "," ####### ","#### ####"," ## # ## ","    #    "}
 };
 private static final int[] STAT_COLORS={0xFFED6174,0xFF92CDA6,0xFFADB8CE,
  0xFFACBBD4,0xFFA8D8F1,0xFF6DC498};
 private static final String[][] ELEMENTS={
  {"    #    ","    ##   ","  # ##   "," ## ###  ","#### ### ","#########","#### ####"," ####### ","  #####  "},
  {"    #    ","   ###   ","   ###   ","  #####  ","  #####  "," ####### ","#########"," ####### ","  #####  "},
  {"  #####  "," ##   ## "," #  #  # ","   ##  # "," ##### # ","##    ## ","#   ###  "," ###     "},
  {"    #    ","   ###   ","  ## ##  "," ## # ## ","## ### ##"," ## # ## ","  ## ##  ","   ###   ","    #    "},
  {"    #    "," #  #  # ","  #####  ","  #####  ","#########","  #####  ","  #####  "," #  #  # ","    #    "},
  {"   ##### ","  ###    "," ###     ","###      ","###      ","###      "," ###     ","  ###    ","   ##### "}
 };
 private StatusSymbols() {}
 public static void stat(GuiGraphics g,int x,int y,int index){
  draw(g,x,y,STATS[index],STAT_COLORS[index]);
 }
 public static void affinity(GuiGraphics g,int x,int y,int index,int color){
  draw(g,x,y,ELEMENTS[index],color);
 }
 private static void draw(GuiGraphics g,int x,int y,String[] rows,int color){
  for(int row=0;row<rows.length;row++)for(int col=0;col<rows[row].length();col++)
   if(rows[row].charAt(col)=='#')g.fill(x+col,y+row,x+col+1,y+row+1,color);
 }
}
