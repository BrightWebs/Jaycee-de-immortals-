package com.brightwebs.jaycee;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.UUID;

/** One per-world, server-owned claim. The award survives logouts and restarts. */
public final class FirstJayceeAwakening extends SavedData {
 private UUID firstPlayer;
 private FirstJayceeAwakening() {}
 private static FirstJayceeAwakening load(CompoundTag tag,HolderLookup.Provider registries){
  FirstJayceeAwakening state=new FirstJayceeAwakening();
  if(tag.hasUUID("FirstPlayer"))state.firstPlayer=tag.getUUID("FirstPlayer");
  return state;
 }
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
  if(firstPlayer!=null)tag.putUUID("FirstPlayer",firstPlayer);
  return tag;
 }
 public static void onEssenceAbsorbed(ServerPlayer player){
  var advancement=player.server.getAdvancements()
   .get(ResourceLocation.fromNamespaceAndPath("jaycee","jaycee"));
  if(advancement==null)return;
  var state=player.server.overworld().getDataStorage().computeIfAbsent(
   new SavedData.Factory<>(FirstJayceeAwakening::new,FirstJayceeAwakening::load,DataFixTypes.LEVEL),
   "jaycee_first_awakening");
  if(state.firstPlayer!=null)return;
  state.firstPlayer=player.getUUID();
  state.setDirty();
  player.getAdvancements().award(advancement,"first");
 }
}
