package com.brightwebs.jaycee.client;

import com.brightwebs.brightcore.api.EnergyAPI;
import com.brightwebs.brightcore.api.race.Race;
import com.brightwebs.brightcore.api.race.RaceAPI;
import com.brightwebs.brightcore.api.race.RaceRegistry;
import com.brightwebs.brightcore.network.BrightNetwork;
import com.brightwebs.jaycee.JayceeMod;
import com.brightwebs.jaycee.race.JayceeRaces;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Race Evolution: your line's evolution tree (branches and all), opened from the Race field.
 * Click a form to see its details, effects and requirements; evolve from here when ready.
 */
final class RacePage extends Page {
    private static final T TF = new T(494.323f, 218f, 0.77189f, 318, 104);
    private static final float TX1 = 334, TX2 = 1044, TY1 = 112, TY2 = 962;
    private static final float NW = 224, NH = 88;

    private record Node(String id, int depth, float x, float y) {}
    private String selected;
    private String builtFor = "";
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final List<String[]> edges = new ArrayList<>();
    private float scale = 1;

    RacePage(MenuScreen m) { super(m); }

    @Override String background() { return "bg_race"; }

    private static Race race(String id) { return RaceRegistry.get(Identifier.fromNamespaceAndPath(JayceeMod.ID, id)); }

    /** Lays out the tree of the player's line: rows by depth, columns by leaf order. */
    private void build(String current) {
        nodes.clear(); edges.clear();
        String root = JayceeRaces.lineOf(current);
        Map<String, Integer> depth = new HashMap<>();
        Map<String, List<String>> kids = new HashMap<>();
        List<String> order = new ArrayList<>();
        List<String> queue = new ArrayList<>(List.of(root));
        depth.put(root, 0);
        while (!queue.isEmpty()) {
            String id = queue.remove(0);
            order.add(id);
            Race r = race(id);
            if (r == null) continue;
            for (Race.Evolution e : r.evolutions()) {
                String c = e.target().getPath();
                kids.computeIfAbsent(id, k -> new ArrayList<>()).add(c);
                edges.add(new String[]{id, c});
                if (!depth.containsKey(c)) {
                    depth.put(c, depth.get(id) + 1);
                    // another line's starting race is shown as a leaf, not expanded
                    JayceeRaces.Form f = JayceeRaces.FORMS.get(c);
                    if (f == null || !f.starting()) queue.add(c);
                }
            }
        }
        int rows = depth.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        Map<Integer, List<String>> byRow = new HashMap<>();
        for (String id : order) byRow.computeIfAbsent(depth.get(id), k -> new ArrayList<>()).add(id);
        int cols = byRow.values().stream().mapToInt(List::size).max().orElse(1);
        float rowH = (TY2 - TY1) / rows, colW = (TX2 - TX1) / cols;
        scale = Math.min(1f, Math.min(rowH / (NH + 26), colW / (NW + 12)));
        for (var en : byRow.entrySet()) {
            List<String> row = en.getValue();
            float w = (TX2 - TX1) / row.size();
            for (int i = 0; i < row.size(); i++) {
                float cx = TX1 + w * (i + 0.5f), cy = TY1 + rowH * (en.getKey() + 0.5f);
                nodes.put(row.get(i), new Node(row.get(i), en.getKey(), cx, cy));
            }
        }
        builtFor = current;
    }

    /** Ancestors of the current form along the tree (what you have already been). */
    private Set<String> acquired(String current) {
        Set<String> out = new HashSet<>();
        boolean grew = true;
        out.add(current);
        while (grew) {
            grew = false;
            for (String[] e : edges) if (out.contains(e[1]) && !out.contains(e[0]) && nodes.get(e[0]).depth() < nodes.get(e[1]).depth()) { out.add(e[0]); grew = true; }
        }
        out.remove(current);
        return out;
    }

    @Override
    void draw(GuiGraphicsExtractor g, double fmx, double fmy) {
        LocalPlayer p = player();
        Race cur = RaceAPI.race(p).orElse(null);
        if (cur == null) return;
        String current = cur.id().getPath();
        if (!current.equals(builtFor)) { build(current); selected = current; }
        Set<String> acq = acquired(current);
        TF.push(g);
        // connectors
        for (String[] e : edges) {
            Node a = nodes.get(e[0]), b = nodes.get(e[1]);
            if (a == null || b == null || b.depth() <= a.depth()) continue;
            boolean lit = (acq.contains(e[0]) || e[0].equals(current)) && (acq.contains(e[1]) || e[1].equals(current));
            int col = lit ? 0xFFC060FF : 0xFF3FC8F0;
            float y1 = a.y() + NH * scale / 2, y2 = b.y() - NH * scale / 2, mid = (y1 + y2) / 2;
            UI.fill(g, a.x() - 1.5f, y1, a.x() + 1.5f, mid + 1.5f, col);
            UI.fill(g, Math.min(a.x(), b.x()) - 1.5f, mid - 1.5f, Math.max(a.x(), b.x()) + 1.5f, mid + 1.5f, col);
            UI.fill(g, b.x() - 1.5f, mid, b.x() + 1.5f, y2 - 6, col);
            UI.fill(g, b.x() - 6, y2 - 8, b.x() + 6, y2 - 5, col);
            UI.fill(g, b.x() - 3, y2 - 5, b.x() + 3, y2 - 2, col);
        }
        // nodes
        for (Node n : nodes.values()) drawNode(g, n, n.id().equals(current), acq.contains(n.id()));
        if (selected != null) details(g, p, cur, selected);
        TF.pop(g);
    }

    private void drawNode(GuiGraphicsExtractor g, Node n, boolean current, boolean acquired) {
        float w = NW * scale, h = NH * scale, x = n.x() - w / 2, y = n.y() - h / 2;
        String frame = current ? "node_gold" : acquired ? "node_purple" : "node_blue";
        UI.tex(g, frame, x, y, w, h);
        if (n.id().equals(selected)) UI.rect(g, x - 3, y - 3, x + w + 3, y + h + 3, 2, 0xFFFFFFFF);
        JayceeRaces.Form f = JayceeRaces.FORMS.get(n.id());
        String portrait = "../race/node/" + (f == null ? n.id() : f.portrait());
        float ps = h - 14 * scale;
        UI.tex(g, portrait, x + 10 * scale, y + 7 * scale, ps, ps);
        Race r = race(n.id());
        String name = r == null ? n.id() : r.name().getString();
        float nx = x + w * 0.62f;
        float size = UI.fitSize(name, 20 * scale, 9, w * 0.58f, true);
        UI.text(g, name, nx, y + h * 0.32f, size, 0xFFFFFFFF, 1);
        String pill = current ? "pill_current" : acquired ? "pill_acquired" : n.depth() == 0 ? "pill_base" : "pill_locked";
        int[] s = UI.size(pill);
        float pw = Math.min(w * 0.55f, s[0] * scale * 0.85f), ph = pw * s[1] / s[0];
        UI.tex(g, pill, nx - pw / 2, y + h * 0.7f - ph / 2, pw, ph);
    }

    private Race.Evolution edgeFromCurrent(Race cur, String target) {
        for (Race.Evolution e : cur.evolutions()) if (e.target().getPath().equals(target)) return e;
        return null;
    }

    private void details(GuiGraphicsExtractor g, LocalPlayer p, Race cur, String id) {
        Race r = race(id);
        if (r == null) return;
        JayceeRaces.Form f = JayceeRaces.FORMS.get(id);
        String line = JayceeRaces.lineOf(id);
        UI.tex(g, "../race/node/" + (f == null ? id : f.portrait()), 1081, 174, 104, 104);
        UI.text(g, r.name().getString(), 1212, 174, UI.fitSize(r.name().getString(), 26, 14, 292, true), 0xFFFFFFFF, 0);
        JayceeRaces.Info info = JayceeRaces.INFO.get(line);
        String group = info == null ? "" : switch (info.group()) {
            case "humans" -> "Human"; case "demihumans" -> "Demi-Human"; case "beastfolk" -> "Beastfolk";
            case "monsters" -> "Monster"; case "undead" -> "Undead"; default -> "Spiritual";
        };
        UI.semi(g, group, 1338, 214, 17, 0xFFFFFFFF, 0);
        UI.semi(g, Component.translatable("jaycee.ui.tier", r.tier()).getString() + (r.isDivine() ? " · Divine" : ""), 1338, 246, 17, 0xFFFFFFFF, 0);
        boolean isCur = id.equals(cur.id().getPath()), acq = acquired(cur.id().getPath()).contains(id);
        String pill = isCur ? "pill_current" : acq ? "pill_acquired" : "pill_locked";
        int[] ps = UI.size(pill);
        UI.tex(g, pill, 1338, 280 - ps[1] * 0.42f, ps[0] * 0.84f, ps[1] * 0.84f);
        String descKey = "race.jaycee." + (Component.translatable("race.jaycee." + id + ".desc").getString().startsWith("race.") ? line : id) + ".desc";
        List<String> desc = UI.wrap(Component.translatable(descKey).getString(), 16, 420, false);
        for (int k = 0; k < Math.min(3, desc.size()); k++) UI.semi(g, desc.get(k), 1082, 318 + k * 23, 16, 0xFFF0F0FA, 0);
        // effects
        String[][] rows = {
                {"jaycee.ui.effect.health", UI.num(r.health())},
                {"jaycee.ui.effect.attack", String.format(Locale.US, "%.1f", r.attack())},
                {"jaycee.ui.effect.mana", UI.shortNum(r.minMana()) + " – " + UI.shortNum(r.maxMana())},
                {"jaycee.ui.effect.defense", Math.round(r.knockbackResistance() * 100) + "%"},
                {"jaycee.ui.effect.speed", String.format(Locale.US, "%.3f", r.speed())},
                {"jaycee.ui.effect.alignment", r.alignment().label()}};
        int[] ey = {462, 492, 523, 554, 586, 618};
        for (int i = 0; i < rows.length; i++) {
            UI.semi(g, Component.translatable(rows[i][0]), 1143, ey[i], 17, 0xFFF0F0FA, 0);
            UI.text(g, rows[i][1], 1495, ey[i], 18, 0xFFF060F0, 2);
        }
        // requirements
        double ep = EnergyAPI.get(p).ep();
        Race.Evolution evo = edgeFromCurrent(cur, id);
        String[] req = new String[3];
        boolean[] ok = new boolean[3];
        if (isCur) {
            req[0] = Component.translatable("jaycee.ui.req.ready").getString(); ok[0] = true;
            req[1] = cur.evolutions().isEmpty() ? "" : cur.evolutions().size() + " evolution path(s) ahead"; ok[1] = true;
        } else if (evo != null) {
            req[0] = Component.translatable("jaycee.ui.req.previous", cur.name()).getString(); ok[0] = true;
            req[1] = Component.translatable("jaycee.ui.req.ep", UI.num(evo.epNeeded())).getString() + "  (" + UI.shortNum(ep) + ")";
            ok[1] = ep >= evo.epNeeded();
        } else if (!acq) {
            String parent = null; double need = 0;
            for (String[] e : edges) if (e[1].equals(id)) {
                parent = e[0];
                Race pr = race(parent);
                if (pr != null) for (Race.Evolution pe : pr.evolutions()) if (pe.target().getPath().equals(id)) need = pe.epNeeded();
                break;
            }
            Race pr = parent == null ? null : race(parent);
            req[0] = Component.translatable("jaycee.ui.req.previous", pr == null ? Component.literal("?") : pr.name()).getString();
            req[1] = Component.translatable("jaycee.ui.req.ep", UI.num(need)).getString();
            ok[1] = ep >= need;
        } else {
            req[0] = "Already acquired"; ok[0] = true;
        }
        int[] ry = {742, 786, 830};
        for (int i = 0; i < 3; i++) {
            if (req[i] == null || req[i].isEmpty()) continue;
            UI.text(g, ok[i] ? "✔" : "✘", 1103, ry[i], 18, ok[i] ? 0xFF5FE07A : 0xFFFF7A8A, 1);
            UI.semi(g, UI.fit(req[i], 16, 360, false), 1128, ry[i], 16, ok[i] ? 0xFFFFFFFF : 0xFFFFB0B8, 0);
        }
        if (evo != null && RaceAPI.blocker(p, evo).isEmpty()) {
            UI.fill(g, 1075, 858, 1500, 896, 0xFF0C3A50);
            UI.rect(g, 1075, 858, 1500, 896, 2, 0xFF7FF6FF);
            UI.text(g, Component.translatable("jaycee.ui.evolve"), 1288, 877, 20, 0xFFFFFFFF, 1);
        }
    }

    @Override
    boolean click(double fx, double fy, int b) {
        double x = TF.x(fx), y = TF.y(fy);
        LocalPlayer p = player();
        Race cur = RaceAPI.race(p).orElse(null);
        if (cur == null || b != 0) return false;
        for (Node n : nodes.values()) {
            float w = NW * scale, h = NH * scale;
            if (in(x, y, n.x() - w / 2, n.y() - h / 2, n.x() + w / 2, n.y() + h / 2)) { selected = n.id(); return true; }
        }
        if (selected != null && in(x, y, 1075, 858, 1500, 896)) {
            Race.Evolution evo = edgeFromCurrent(cur, selected);
            if (evo != null && RaceAPI.blocker(p, evo).isEmpty()) {
                ClientPacketDistributor.sendToServer(new BrightNetwork.Evolve(evo.target().toString()));
                return true;
            }
        }
        return false;
    }
}
