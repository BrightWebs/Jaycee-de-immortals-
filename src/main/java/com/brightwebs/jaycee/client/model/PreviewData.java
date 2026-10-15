package com.brightwebs.jaycee.client.model;

import java.util.List;

/** UI-only fixtures. Replace with synchronized gameplay data when powers are implemented. */
public final class PreviewData {
 public record Ability(String name, String category, String description, int mana) {}
 public record Stat(String name, String value) {}
 public record Affinity(String name,int color) {}
 public static final List<String> CATEGORIES=List.of("All","Offense","Defense","Support","Movement","Reality");
 public static final List<String> TRACKS=List.of("Body","Spirit","Divine","Utility");
 public static final List<Stat> STATS=List.of(new Stat("MP","80 / 80"),new Stat("Attack","18"),
  new Stat("Defense","16"),new Stat("Speed","105"),new Stat("Luck","10"));
 public static final List<Affinity> AFFINITIES=List.of(new Affinity("Fire",0xFFF46569),
  new Affinity("Water",0xFF55B6FF),new Affinity("Wind",0xFF8ACBA0),
  new Affinity("Earth",0xFFCEAA63),new Affinity("Light",0xFFFFE9A1),
  new Affinity("Dark",0xFF9A78EF));
 public static final List<Ability> ABILITIES=List.of(
  new Ability("Reality Severance","Reality","Cleave the veil between worlds with a focused strike.",40),
  new Ability("Celestial Guard","Defense","Conjure a radiant barrier against incoming attacks.",25),
  new Ability("Divine Pulse","Offense","Release a wave of divine energy around you.",30),
  new Ability("Temporal Step","Movement","Take a short step beyond the flow of time.",20),
  new Ability("Soul Restore","Support","Channel a soothing light to restore your spirit.",35),
  new Ability("Aether Flight","Movement","Ride currents of aether above the battlefield.",15),
  new Ability("Solar Lance","Offense","Focus sunlight into a piercing lance.",28),
  new Ability("Astral Ward","Defense","Wrap yourself in a protective mantle of stars.",22),
  new Ability("Blessing","Support","Bolster a companion with celestial resolve.",18));
 private PreviewData() {}
}
