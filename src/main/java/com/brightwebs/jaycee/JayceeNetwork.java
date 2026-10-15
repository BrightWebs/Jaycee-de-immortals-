package com.brightwebs.jaycee;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server owns unlock progress and the forty loadout slots. */
public final class JayceeNetwork {
 public record State(int consumed,int page,int[] slots) implements CustomPacketPayload {
  public static final Type<State> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee","state"));
  public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of(
   (b,s)->{b.writeVarInt(s.consumed);b.writeVarInt(s.page);for(int i=0;i<40;i++)b.writeVarInt(s.slots[i]);},
   b->{int n=b.readVarInt(),p=b.readVarInt();int[] slots=new int[40];for(int i=0;i<40;i++)slots[i]=b.readVarInt();return new State(n,p,slots);});
  @Override public Type<State> type(){return TYPE;}
 }
 public record Edit(int page,int slot,int art) implements CustomPacketPayload {
  public static final Type<Edit> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("jaycee","edit"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Edit> CODEC=StreamCodec.of(
   (b,s)->{b.writeVarInt(s.page);b.writeVarInt(s.slot);b.writeVarInt(s.art);},
   b->new Edit(b.readVarInt(),b.readVarInt(),b.readVarInt()));
  @Override public Type<Edit> type(){return TYPE;}
 }
 public static int[] slots(Player player){
  int[] s=player.getPersistentData().getIntArray("JayceeSlots");
  if(s.length!=40){s=new int[40];java.util.Arrays.fill(s,-1);}return s;
 }
 public static int page(Player player){return Math.max(0,Math.min(9,player.getPersistentData().getInt("JayceePage")));}
 public static void sync(ServerPlayer p){PacketDistributor.sendToPlayer(p,new State(p.getPersistentData().getInt("JayceeConsumed"),page(p),slots(p)));}
 public static void register(RegisterPayloadHandlersEvent event){
  var registrar=event.registrar("2");
  registrar.playToClient(State.TYPE,State.CODEC,(s,c)->{
   var d=c.player().getPersistentData();d.putInt("JayceeConsumed",s.consumed);d.putInt("JayceePage",s.page);d.putIntArray("JayceeSlots",s.slots);
  });
  registrar.playToServer(Edit.TYPE,Edit.CODEC,(s,c)->{
   if(!(c.player() instanceof ServerPlayer p))return;
   if(s.page<0||s.page>9||s.slot < -1||s.slot>3||s.art < -1||s.art>=AbilityCatalog.ARTS.length)return;
   if(!JayceeMod.unlocked(p))return;
   p.getPersistentData().putInt("JayceePage",s.page);
   if(s.slot>=0){int[] a=slots(p);a[s.page*4+s.slot]=s.art;p.getPersistentData().putIntArray("JayceeSlots",a);}
   sync(p);
  });
 }
 private JayceeNetwork(){}
}
