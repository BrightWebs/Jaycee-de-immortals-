package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.client.model.EnergyCoreMotion;
import com.brightwebs.jaycee.client.model.EnergyCoreMotion.State;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/** GUI-only light sculpture. No textures, global shader changes, or gameplay side effects. */
public final class GreatSageEnergyCore {
 public enum Quality {
  LOW(64,24), MEDIUM(144,32), HIGH(224,40);
  final int strands,segments;
  Quality(int strands,int segments){this.strands=strands;this.segments=segments;}
 }
 private final long started=System.nanoTime();
 private Quality quality=Quality.HIGH;
 private State event=State.IDLE;
 private State pending=State.IDLE;
 private double eventStart=-100,analyzing;
 private double lastTime;
 private boolean analyze;

 public void setQuality(Quality quality){this.quality=java.util.Objects.requireNonNull(quality);}
 public Quality quality(){return quality;}
 public void setAnalyzing(boolean enabled){analyze=enabled;}
 /** Call on the client render thread when real gameplay systems signal one of these events. */
 public void trigger(State state){
  java.util.Objects.requireNonNull(state);
  if(state==State.IDLE||state==State.ANALYZING){setAnalyzing(state==State.ANALYZING);return;}
  double now=seconds();
  // Coalesce bursts into the latest pending event rather than snapping an active curve.
  if(event!=State.IDLE&&now-eventStart<duration()){pending=state;return;}
  event=state;eventStart=now;
 }
 private double seconds(){return (System.nanoTime()-started)*1.0e-9;}
 private double duration(){return event==State.AWAKENING?5:event==State.ULTIMATE?4:3;}

 /** Radius includes all halo geometry; normalized curve bounds provide circular containment. */
 public void render(GuiGraphics g,int cx,int cy,int radius){
  if(radius<4)return;
  double time=seconds(),dt=Math.max(0,Math.min(.1,time-lastTime));lastTime=time;
  analyzing+=( (analyze?1:0)-analyzing)*(1-Math.exp(-dt*5));
  if(event!=State.IDLE&&time-eventStart>=duration()){
   event=pending;pending=State.IDLE;eventStart=time;
  }
  double age=time-eventStart;
  double strength=event!=State.IDLE&&age>=0&&age<duration()?Math.pow(Math.sin(Math.PI*age/duration()),2):0;
  State state=strength>0?event:State.ANALYZING;
  if(strength==0)strength=analyzing;
  // Flush pending panel geometry before using the same standard translucent GUI buffer.
  g.flush();
  Matrix4f matrix=g.pose().last().pose();
  VertexConsumer vertices=g.bufferSource().getBuffer(RenderType.gui());
  float size=radius-2;
  for(int layer=0;layer<3;layer++){
   for(int strand=layer;strand<quality.strands;strand+=3){
    var prev=sample(strand,0,time,state,strength);
    for(int step=1;step<=quality.segments;step++){
     var next=sample(strand,step/(double)quality.segments,time,state,strength);
     float x1=cx+(float)prev.x()*size,y1=cy+(float)prev.y()*size;
     float x2=cx+(float)next.x()*size,y2=cy+(float)next.y()*size;
     int alpha=(int)(50+next.light()*135);
     segment(vertices,matrix,x1,y1,x2,y2,.85f,((alpha/7)<<24)|next.color());
     segment(vertices,matrix,x1,y1,x2,y2,.18f,(alpha<<24)|next.color());
     prev=next;
    }
   }
  }
  int core=0xE8FAFF;
  if(state==State.LEVEL_UP||state==State.LEARNING||state==State.AWAKENING)
   core=EnergyCoreMotion.blend(core,0xFFF0BC,strength);
  // Concentric translucent discs create additive-looking glow without leaking blend state.
  double surge=(state==State.LEVEL_UP||state==State.AWAKENING)?strength:0;
  for(int ring=12;ring>=1;ring--){
   float r=(float)(radius*(.025+ring*.012)*(1+surge*.7));
   int alpha=ring>4?7: ring>1?34:240;
   disc(vertices,matrix,cx,cy,r,(alpha<<24)|core);
  }
  g.flush();
 }
 private EnergyCoreMotion.Sample sample(int strand,double u,double time,State state,double strength){
  var base=EnergyCoreMotion.sample(strand,u,time,State.ANALYZING,analyzing);
  if(state==State.ANALYZING)return base;
  var target=EnergyCoreMotion.sample(strand,u,time,state,1);
  // Crossfade from the live idle/analyzing curve, never reset phase on event entry/exit.
  return new EnergyCoreMotion.Sample(
   base.x()+(target.x()-base.x())*strength,
   base.y()+(target.y()-base.y())*strength,
   base.depth()+(target.depth()-base.depth())*strength,
   EnergyCoreMotion.blend(base.color(),target.color(),strength),
   base.light()+(target.light()-base.light())*strength);
 }
 private static void segment(VertexConsumer v,Matrix4f m,float x1,float y1,float x2,float y2,float halfWidth,int color){
  float dx=x2-x1,dy=y2-y1;
  float length=(float)Math.sqrt(dx*dx+dy*dy);
  if(length<.0001f)return;
  float nx=-dy/length*halfWidth,ny=dx/length*halfWidth;
  v.addVertex(m,x1-nx,y1-ny,0).setColor(color);
  v.addVertex(m,x1+nx,y1+ny,0).setColor(color);
  v.addVertex(m,x2+nx,y2+ny,0).setColor(color);
  v.addVertex(m,x2-nx,y2-ny,0).setColor(color);
 }
 private static void disc(VertexConsumer v,Matrix4f m,float x,float y,float r,int color){
  for(int i=0;i<32;i++){
   double a=i*Math.PI/16,b=(i+1)*Math.PI/16;
   v.addVertex(m,x,y,0).setColor(color);
   v.addVertex(m,x+(float)Math.cos(b)*r,y+(float)Math.sin(b)*r,0).setColor(color);
   v.addVertex(m,x+(float)Math.cos(a)*r,y+(float)Math.sin(a)*r,0).setColor(color);
   v.addVertex(m,x,y,0).setColor(color);
  }
 }
}
