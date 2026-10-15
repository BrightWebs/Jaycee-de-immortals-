package com.brightwebs.jaycee.client.model;

/** Pure, deterministic curve/color sampling, independent of frame rate and GUI scale. */
public final class EnergyCoreMotion {
 public enum State { IDLE, ANALYZING, LEARNING, LEVEL_UP, ULTIMATE, AWAKENING }
 public record Sample(double x,double y,double depth,int color,double light) {}
 private static final int[] PALETTE={
  0x4080FF,0xB9EFFF,0x45E4FF,0xF2FAFF,0x9875FF,0xAF4AE5,0xFFD57B,0x55DBA4,0xDC7F94
 };
 private EnergyCoreMotion() {}

 /** u spans a closed strand. Time is never wrapped, so no periodic reset is visible. */
 public static Sample sample(int strand,double u,double seconds,State state,double strength){
  double p=strand*2.399963229728653;
  double t=seconds;
  double a=u*Math.PI*2;
  double chaos=Math.pow(.5+.5*Math.sin(t*.19),8);
  double active=strength*(state==State.ULTIMATE?1:state==State.ANALYZING?.55:0);
  double twist=.22*Math.sin(t*.63+p)+active*.35*Math.sin(t*2.7+p);
  double wave=.12*Math.sin(3*a+t*.73+p)+(.04+chaos*.07)*Math.sin(5*a-t*1.13+p*2);
  double phase=a+twist*Math.sin(2*a+p)+.13*Math.sin(t*.41+p);
  double latitude=.7*Math.sin(p+t*.17)+wave;
  double radius=(.62+.13*Math.sin(p*1.7+t*.37)+wave*.45);
  if(state==State.LEARNING)radius*=1-strength*.48;
  radius*=1+.08*Math.sin(t*.53+p)+active*.12*Math.sin(4*a+t*2+p);
  double x=radius*Math.cos(phase);
  double y=radius*Math.sin(phase)*Math.cos(latitude);
  double z=radius*Math.sin(phase)*Math.sin(latitude);
  // Independent planes and frequencies: changing inter-point distances, not a rigid rotation.
  double tilt=p*.7+.18*Math.sin(t*.29+p);
  double xx=x*Math.cos(tilt)-y*Math.sin(tilt);
  double yy=x*Math.sin(tilt)+y*Math.cos(tilt);
  yy*=.82+.15*Math.sin(t*.33+p);
  xx+=.06*Math.sin(2*a-t*.47+p)*Math.sin(t*.23+p);
  double perspective=1+z*.17;
  xx*=perspective;yy*=perspective;
  double bound=Math.max(1,Math.sqrt(xx*xx+yy*yy)/.92);
  xx/=bound;yy/=bound;

  double colorWave=t*.13+strand*.137+.35*Math.sin(2*a-t*.7+p)+.12*Math.sin(t*.4+p+a);
  int full=palette(colorWave);
  int cool=blend(0x72CFFF,0xE8FAFF,.5+.5*Math.sin(colorWave*3));
  double accent=.18+.72*Math.pow(.5+.5*Math.sin(p+t*.31+a),5);
  int color=blend(cool,full,accent);
  if(state==State.ANALYZING)color=blend(color,blend(0x986CFF,0x82F4FF,.5+.5*Math.sin(a*2-t*2+p)),strength*.7);
  if(state==State.LEARNING||state==State.LEVEL_UP)color=blend(color,0xFFE1A0,strength*.9);
  if(state==State.AWAKENING)color=blend(color,blend(0xFFD47A,0xFFFFFF,.5+.5*Math.sin(a-t*3+p)),strength);
  double pulse=Math.pow(.5+.5*Math.sin(a*2-t*(2.1+strand%7*.09)+p),14);
  double light=.22+.33*(z+1)/2+.45*pulse;
  color=blend(color,0xF4FDFF,pulse*.55);
  return new Sample(xx,yy,z,color,light);
 }
 private static int palette(double phase){
  double f=(phase-Math.floor(phase))*PALETTE.length;
  int i=(int)f;
  double u=f-i;
  return blend(PALETTE[i],PALETTE[(i+1)%PALETTE.length],u*u*(3-2*u));
 }
 public static int blend(int from,int to,double amount){
  double f=Math.clamp(amount,0,1);
  int r=(int)Math.round(((from>>16)&255)*(1-f)+((to>>16)&255)*f);
  int g=(int)Math.round(((from>>8)&255)*(1-f)+((to>>8)&255)*f);
  int b=(int)Math.round((from&255)*(1-f)+(to&255)*f);
  return (r<<16)|(g<<8)|b;
 }
}
