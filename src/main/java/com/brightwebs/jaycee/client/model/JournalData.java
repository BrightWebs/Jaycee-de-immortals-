package com.brightwebs.jaycee.client.model;

import java.util.List;

/** Presentation fixtures, not player unlocks, accepted quests, or working spells. */
public final class JournalData {
 public record Entry(String name,String subtitle,String description,String requirement,String reward,int symbol) {}
 public record Collection(String title,String listTitle,String detailTitle,List<Entry> entries) {
  public Collection {
   entries=List.copyOf(entries);
   if(entries.isEmpty()||entries.size()>6)throw new IllegalArgumentException("A preview collection needs 1-6 entries");
  }
 }
 public static final Collection MAGIC=new Collection("Arcane Grimoire","Elements","Spell Notes",List.of(
  new Entry("Fire","Ember Invocation","A flame sheltered within the soul, waiting to be shaped into light.",
   "Fire affinity / Lv. 0","Sample mana cost: 20",1),
  new Entry("Water","Moonlit Current","Threads of silver water gather beneath a luminous arcane seal.",
   "Water affinity / Lv. 0","Sample mana cost: 18",3),
  new Entry("Wind","Zephyr's Grace","An airy current carries the promise of swift and weightless motion.",
   "Wind affinity / Lv. 0","Sample mana cost: 15",4),
  new Entry("Earth","Stone Covenant","An ancient rune embodies patience, strength and an unbroken resolve.",
   "Earth affinity / Lv. 0","Sample mana cost: 24",2),
  new Entry("Light","Dawn's Blessing","A pale star kindles a gentle radiance at the heart of the grimoire.",
   "Light affinity / Lv. 0","Sample mana cost: 30",0),
  new Entry("Dark","Veil of Night","A quiet constellation of shadows gathers around a hidden spark.",
   "Dark affinity / Lv. 0","Sample mana cost: 28",5)));
 public static final Collection SPECIES=new Collection("Path of Ascension","Lineage","Racial Traits",List.of(
  new Entry("Human","The Mortal Beginning","An unwritten future, shaped by the choices of an ordinary soul.",
   "Starting lineage (sample)","Adaptable spirit (preview)",0),
  new Entry("Awakened","A Stirring Within","The first sign of an immortal legacy glimmers beneath the surface.",
   "Awakening not implemented","Essence perception (preview)",1),
  new Entry("Demigod","Between Two Worlds","Mortal courage and celestial potential meet beneath a rising star.",
   "Ascension not implemented","Celestial resolve (preview)",2),
  new Entry("Divine","Beyond the Veil","A distant destiny is reflected in the silent circles of the heavens.",
   "Divinity not implemented","Divine presence (preview)",3)));
 public static final Collection QUESTS=new Collection("Chronicles","Quest Journal","Objectives",List.of(
  new Entry("Awakening","A Spark in the Silence","An unfamiliar light calls from the edge of a dream.",
   "Discover the source of the light.","Sample reward: 100 EXP",5),
  new Entry("Essence of Existence","A Fragment of Eternity","An ancient fragment hints at a larger world beyond the mortal veil.",
   "Investigate the mysterious essence.","Sample reward: 150 EXP",3),
  new Entry("The First Ascension","The Next Horizon","The path of growth begins with a single deliberate step.",
   "Seek a path toward ascension.","Sample reward: 250 EXP",2),
  new Entry("A New Reality","An Unwritten Chapter","Beyond the familiar sky, a new chapter waits to be discovered.",
   "Explore the unknown horizon.","Sample reward: 300 EXP",4)));
 private JournalData() {}
}
