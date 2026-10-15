package com.brightwebs.jaycee.client.model;

import java.util.List;

/** UI-only fixtures. Replace with synchronized gameplay data when powers are implemented. */
public final class PreviewData {
 public record Ability(String name, String category, String description, int mana) {}
 public record Stat(String name, String value) {}
 public static final List<String> CATEGORIES=List.of("All","Offense","Defense","Support","Movement","Reality");
 public static final List<String> TRACKS=List.of("Body","Spirit","Divine","Utility");
 public static final List<Stat> STATS=List.of(new Stat("MP","120 / 120"),new Stat("Attack","24"),
  new Stat("Defense","18"),new Stat("Speed","12"),new Stat("Luck","7"));
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
