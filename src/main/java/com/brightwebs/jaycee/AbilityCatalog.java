package com.brightwebs.jaycee;

/** Menu entries describe the planned powers. Equipping does not cast a power yet. */
public final class AbilityCatalog {
 public record Art(String name,String category,String description,int icon) {}
 public static final Art[] ARTS={
  new Art("Reality Severance","Reality","Tear through reality with a spatial rift. Planned effects: area damage and spatial disruption.",0),
  new Art("Existence Renewal","Support","Restore life energy through a divine renewal field.",1),
  new Art("Reality Step","Movement","Travel to the location you are looking at within loaded chunks.",2),
  new Art("Primordial Rupture","Offense","Release a concentrated burst of primordial energy.",3),
  new Art("Divine Pulse","Offense","Send a wave of divine energy through nearby enemies.",4),
  new Art("Immortal Recovery","Support","Recover from harm with divine restoration.",5),
  new Art("Celestial Resolve","Defense","Protect yourself with a radiant divine barrier.",6),
  new Art("Sovereign Strike","Offense","Focus your power into a single devastating strike.",7),
  new Art("Olympian Benediction","Support","Share a divine blessing with your allies.",8),
  new Art("Aether Flight","Movement","Take flight, with adjustable speed and a transferable blessing.",9)
 };
 public static final String[] CATEGORIES={"All","Offense","Defense","Support","Movement","Reality"};
 private AbilityCatalog(){}
}
