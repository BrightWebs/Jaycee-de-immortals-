package com.brightwebs.jaycee.client.model;

import java.util.List;

/** UI-only fixtures for RPG systems that do not yet have authoritative progression data. */
public final class PreviewData {
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
 private PreviewData() {}
}
