package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.gameplay.AbilityCatalog;
import com.brightwebs.jaycee.gameplay.PlayerState;
import com.brightwebs.jaycee.gameplay.divine.DivinityNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Presentation only: every quote, grant, identity and resource mutation is server-owned. */
final class DivineBondsScreen extends Screen {
    private static final String[] TABS = {"Profile", "Bestowal", "Offers", "Bonds", "Dominion", "History"};
    private static final String[] DECISIONS = {"cap_lift", "cap_revoke", "revoke_privileges", "demote", "strip", "release"};
    private static final int INK = 0xFFB6CCDD, MUTED = 0xFF788DAB;
    private record Control(Button button, Supplier<String> text, BooleanSupplier enabled) {}
    private final List<Control> controls = new ArrayList<>();
    private final boolean targeted;
    private SapphireLayout layout;
    private int tab, offset, detailOffset, decision;
    private UUID selection;
    private String amount = "0", name = "", ability = "";
    private boolean bind, confirmed, requested;
    private EditBox amountBox, nameBox;
    private Button detailsNarration;
    private CompoundTag snapshot;

    DivineBondsScreen(boolean targeted) {
        super(Component.literal("Divine Ascension · Bonds"));
        this.targeted = targeted;
        tab = targeted ? 1 : 0;
    }
    private PlayerState state() { return PlayerState.get(Minecraft.getInstance().player); }
    private CompoundTag data() { return snapshot == null ? state().divinity() : snapshot; }
    private UUID self() { return Minecraft.getInstance().player.getUUID(); }
    private UUID id(CompoundTag tag) { return tag.hasUUID("Id") ? tag.getUUID("Id") : DivinityNetwork.NONE; }
    private String title(CompoundTag p) {
        String name = p.getString("DivineName");
        return (name.isEmpty() ? p.getString("Username") : name) + (p.getBoolean("Hidden") ? " · concealed" : "");
    }
    private void request(String op, UUID target, UUID nonce, String value, boolean flag) {
        DivinityNetwork.send(op, target, nonce, 0, value, "", flag);
    }
    private void switchTab(int page) {
        tab = page; selection = null; offset = 0; detailOffset = 0; confirmed = false;
        clearWidgets(); init();
    }
    @Override protected void init() {
        layout = new SapphireLayout(width, height);
        controls.clear();
        detailsNarration = null;
        if (!requested) {
            DivinityNetwork.request(targeted ? "profile" : "refresh");
            requested = true;
        }
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            button(12 + i * 113, 9, 109, () -> TABS[index], () -> switchTab(index), () -> true);
        }
        button(20, 53, 142, () -> "Back to menu", () ->
            Minecraft.getInstance().setScreen(new JayceeScreen(!state().supreme())), () -> true);
        button(170, 53, 96, () -> "Refresh", () -> DivinityNetwork.request("refresh"), () -> true);
        if (tab > 0 && tab < 5) {
            for (int i = 0; i < 7; i++) {
                final int index = i;
                button(22, 108 + i * 32, 216, () -> rowLabel(index), () -> {
                    var row = row(index);
                    if (row == null) return;
                    selection = id(row); detailOffset = 0; confirmed = false;
                    if (tab == 1 || tab == 3 || tab == 4)
                        request("select", selection, null, "", false);
                }, () -> row(index) != null);
            }
            button(22, 337, 104, () -> "Previous", () -> offset = Math.max(0, offset - 7), () -> offset > 0);
            button(134, 337, 104, () -> "Next", () -> offset += 7, () -> offset + 7 < records().size());
        }
        if (tab == 1) {
            amountBox = new EditBox(font, 276, 158, 384, 16, Component.literal("Essence to transfer, not EP"));
            amountBox.setMaxLength(19); amountBox.setValue(amount);
            amountBox.setFilter(s -> s.matches("[0-9]*"));
            amountBox.setResponder(s -> amount = s); addRenderableWidget(amountBox);
            button(274, 194, 388, () -> "Power: " + (ability.isEmpty() ? "None" : abilityName(ability)),
                this::cyclePower, this::canBestow);
            nameBox = new EditBox(font, 276, 230, 384, 16, Component.literal("Proposed Divine Name; account name never changes"));
            nameBox.setMaxLength(48); nameBox.setValue(name);
            nameBox.setResponder(s -> name = s); addRenderableWidget(nameBox);
            button(274, 256, 388, () -> "Invite subordinate bond: " + (bind ? "YES — requires consent" : "NO"),
                () -> bind = !bind, this::canBestow);
            button(274, 283, 388, () -> "Request server quote — spends nothing", () -> {
                var target = data().getCompound("Target");
                DivinityNetwork.send("quote", id(target), null, parsedAmount(), ability, name, bind);
            }, () -> canBestow() && parsedAmount() >= 0);
            button(274, 367, 388, this::sendLabel, () ->
                request("send", null, id(data().getCompound("Quote")), "", false), this::quoteMatches);
        } else if (tab == 2) {
            button(274, 337, 188, () -> "Accept selected offer: " + String.join(". ", body()), () ->
                request("accept", null, selection, "", false), () ->
                selected().hasUUID("Recipient") && selected().getUUID("Recipient").equals(self()));
            button(474, 337, 188, () -> "Decline / cancel", () ->
                request("cancel", null, selection, "", false), () -> !selected().isEmpty());
        } else if (tab == 3) {
            button(274, 337, 188, () -> "Approve release", () ->
                request("approve_release", selection, null, "", false), () -> canDecideRelease());
            button(474, 337, 188, () -> "Deny release", () ->
                request("deny_release", selection, null, "", false), () -> canDecideRelease());
            button(22, 367, 216, () -> "Request my release", () ->
                DivinityNetwork.request("request_release"), () -> data().hasUUID("Patron") && data().getString("Release").isEmpty());
            button(274, 367, 388, () -> "Petition Jaycee after denied release", () ->
                DivinityNetwork.request("petition"), () -> data().getString("Release").equals("DENIED"));
        } else if (tab == 4) {
            button(274, 283, 388, () -> "Decision: " + DECISIONS[decision], () -> {
                decision = (decision + 1) % DECISIONS.length; confirmed = false;
            }, () -> state().supreme());
            button(274, 310, 388, () -> confirmed ? "Confirmation ON — inspect target and decision" : "Confirm this specific decision: OFF",
                () -> confirmed = !confirmed, () -> state().supreme() && !selected().isEmpty());
            button(274, 367, 388, () -> "Apply confirmed adjudication", () -> {
                request("adjudicate", selection, null, DECISIONS[decision], true); confirmed = false;
            }, () -> state().supreme() && confirmed && !selected().isEmpty());
        }
        if (tab != 1) {
            int x = tab == 0 || tab == 5 ? 28 : 274, w = tab == 0 || tab == 5 ? 644 : 388;
            detailsNarration = addRenderableWidget(new Button(x, 94, w, tab == 4 ? 174 : 220,
                Component.literal("Profile details"), b -> {}, narration -> narration.get()) {
                @Override protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {}
            });
        }
    }
    private Button button(int x, int y, int w, Supplier<String> text, Runnable action, BooleanSupplier enabled) {
        var b = new Button(x, y, w, 22, Component.literal(text.get()), ignored -> action.run(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
                DivineFrame.sapphire(g, getX(), getY(), width, height, isHoveredOrFocused());
                UiText.line(g, getMessage().getString(), getX() + 7, getY() + 7, width - 14, .8f, active ? INK : MUTED, true);
            }
        };
        controls.add(new Control(b, text, enabled)); return addRenderableWidget(b);
    }
    private List<CompoundTag> records() {
        String key = switch (tab) { case 1 -> "Nearby"; case 2 -> "Pending"; case 3 -> "Followers"; case 4 -> "Petitions"; default -> ""; };
        var result = new ArrayList<CompoundTag>();
        for (Tag tag : data().getList(key, Tag.TAG_COMPOUND)) result.add((CompoundTag)tag);
        // Dominion can also adjudicate a selected nearby player or an existing follower.
        if (tab == 4 && state().supreme()) {
            for (String extra : List.of("Nearby", "Followers"))
                for (Tag tag : data().getList(extra, Tag.TAG_COMPOUND)) {
                    var p = (CompoundTag)tag;
                    if (result.stream().noneMatch(r -> id(r).equals(id(p)))) result.add(p);
                }
        }
        return result;
    }
    private CompoundTag row(int index) {
        var rows = records();
        offset = Math.clamp(offset, 0, Math.max(0, rows.size() - 1));
        return offset + index < rows.size() ? rows.get(offset + index) : null;
    }
    private CompoundTag selected() {
        return records().stream().filter(r -> id(r).equals(selection)).findFirst().orElseGet(CompoundTag::new);
    }
    private String rowLabel(int index) {
        var r = row(index);
        if (r == null) return "—";
        return tab == 2 ? r.getString("DonorName") + " → " + r.getString("RecipientName") : title(r);
    }
    private boolean canBestow() {
        var d = data(); var t = d.getCompound("Target");
        return !t.isEmpty() && !t.getBoolean("Hidden") && !id(t).equals(self()) &&
            (state().supreme() || d.getBoolean("Bestow")) && parsedAmount() >= 0 && parsedAmount() <= d.getLong("Essence");
    }
    private long parsedAmount() {
        try { return Long.parseLong(amount); } catch (NumberFormatException e) { return -1; }
    }
    private boolean quoteMatches() {
        var q = data().getCompound("Quote"); var t = data().getCompound("Target");
        return canBestow() && q.hasUUID("Id") && q.hasUUID("Target") && q.getUUID("Target").equals(id(t))
            && q.getLong("Amount") == parsedAmount() && q.getString("Ability").equals(ability)
            && q.getString("Name").equals(name.strip()) && q.getBoolean("Bind") == bind;
    }
    private String sendLabel() {
        if (!quoteMatches()) return "Send reviewed offer — request a matching quote first";
        var q = data().getCompound("Quote");
        return "Send offer to " + data().getCompound("Target").getString("Username") + ": " + q.getLong("Amount") +
            " Essence; burned fee " + q.getLong("Fee") + "; total debit " + q.getLong("Total") +
            "; power " + (ability.isEmpty() ? "none" : abilityName(ability)) + "; Divine Name " +
            (name.isBlank() ? "unchanged" : name) + "; subordinate bond " + bind + ". Recipient must accept.";
    }
    private boolean canDecideRelease() {
        var p = selected();
        return !p.getString("Release").isEmpty() &&
            (state().supreme() || p.hasUUID("Patron") && p.getUUID("Patron").equals(self()));
    }
    private void cyclePower() {
        var ids = new ArrayList<String>(); ids.add("");
        AbilityCatalog.registered().stream().filter(a -> state().supreme() ||
            data().getCompound("Grants").contains(a.id())).filter(AbilityCatalog.Art::implemented).forEach(a -> ids.add(a.id()));
        ability = ids.get((ids.indexOf(ability) + 1) % ids.size());
    }
    private String abilityName(String id) {
        var art = AbilityCatalog.find(id); return art == null ? id : art.name();
    }
    private List<String> profileLines(CompoundTag p) {
        var lines = new ArrayList<String>();
        if (p.isEmpty()) { lines.add("No selected player. Choose a nearby player or refresh."); return lines; }
        lines.add("Account: " + p.getString("Username"));
        lines.add("Original account: " + p.getString("Original"));
        lines.add("UUID: " + id(p));
        if (p.getBoolean("Hidden")) { lines.add("Jaycee's true profile is concealed. No private statistics were sent."); return lines; }
        lines.add("Divine Name: " + (p.getString("DivineName").isEmpty() ? "Not bestowed" : p.getString("DivineName")));
        lines.add(p.getBoolean("Supreme") ? "Authority: unique server-assigned Jaycee" : "Rank: " + p.getString("Rank"));
        lines.add("EP: " + p.getLong("Ep") + " — permanent power");
        lines.add("Essence: " + p.getLong("Essence") + " — transferable reserve");
        lines.add("Patron: " + (p.hasUUID("Patron") ? p.getUUID("Patron") : "None"));
        lines.add("Release request: " + (p.getString("Release").isEmpty() ? "None" : p.getString("Release")));
        lines.add("NPC lineage metadata: " + p.getList("Npc", Tag.TAG_STRING));
        lines.add("Granted powers: " + String.join(", ", p.getCompound("Grants").getAllKeys().stream().sorted().map(this::abilityName).toList()));
        lines.add("No account rename, natural promotion or NPC offering recipe is implied.");
        return lines;
    }
    private List<String> body() {
        if (tab == 0) {
            var lines = profileLines(data());
            lines.add("Essence maximum: " + data().getLong("EssenceCap") + "; refill " + data().getLong("EssenceRegen") + "/second while online.");
            lines.add("EP cap: " + data().getLong("EpCap") + ". Cap field and ordinary fees are provisional server tuning, not settled canon.");
            lines.add("Mana is independent. Bestowal never deducts EP. /jayceetest is not sovereign authority.");
            return lines;
        }
        if (tab == 5) {
            var lines = new ArrayList<String>();
            for (Tag tag : data().getList("History", Tag.TAG_STRING)) lines.add(tag.getAsString());
            if (lines.isEmpty()) lines.add("No recorded operations.");
            return lines;
        }
        if (tab == 2) {
            var o = selected(); var lines = new ArrayList<String>();
            if (o.isEmpty()) { lines.add("Choose an offer. Decline or timeout costs nothing."); return lines; }
            lines.add("From: " + o.getString("DonorName") + " → " + o.getString("RecipientName"));
            lines.add("Essence received: " + o.getLong("Amount") + "; donor burns an additional fee of " + o.getLong("Fee"));
            lines.add("Power: " + (o.getString("Ability").isEmpty() ? "None" : abilityName(o.getString("Ability"))));
            lines.add("Proposed Divine Name: " + (o.getString("Name").isEmpty() ? "Unchanged" : o.getString("Name")));
            lines.add("Subordinate binding: " + (o.getBoolean("Bind") ? "YES — acceptance creates a patron bond" : "NO"));
            lines.add("Expires at epoch milliseconds: " + o.getLong("Expires"));
            lines.add("Accept rechecks funds, ownership, mastery, proximity and visibility. One acceptance only. No EP or mana is spent.");
            return lines;
        }
        var lines = profileLines(selected());
        if (tab == 3) {
            var patron = data().getCompound("PatronProfile");
            lines.add("My patron: " + (patron.isEmpty() ? "None" : title(patron)));
            lines.add("My release state: " + data().getString("Release"));
            lines.add("Challenge eligible: " + data().getBoolean("ChallengeEligible") + ". No automatic victory, combat resolution or leadership reversal is implemented.");
        }
        if (tab == 4) lines.add(state().supreme() ? "Confirm decisions explicitly. Cap revocation never erases earned EP." :
            "Dominion adjudication is restricted to the unique Jaycee authority.");
        return lines;
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float dt) {}
    @Override public void render(GuiGraphics g, int mx, int my, float dt) {
        super.renderBackground(g, mx, my, dt);
        if (Minecraft.getInstance().player == null) return;
        snapshot = state().divinity();
        int x = (int)Math.floor(layout.localX(mx)), y = (int)Math.floor(layout.localY(my));
        for (var c : controls) { c.button.setMessage(Component.literal(c.text.get())); c.button.active = c.enabled.getAsBoolean(); }
        if (detailsNarration != null) detailsNarration.setMessage(Component.literal(String.join(". ", body()) + ". Page Up and Page Down scroll details."));
        if (tab == 1) { amountBox.active = true; nameBox.active = true; }
        g.pose().pushPose(); g.pose().translate(layout.left, layout.top, 0);
        g.pose().scale((float)layout.scale, (float)layout.scale, 1);
        DivineFrame.sapphire(g, 0, 0, 700, 420, false);
        UiText.line(g, "Divine Ascension · " + TABS[tab], 280, 61, 398, 1, INK, true);
        if (tab > 0 && tab < 5) {
            DivineFrame.sapphire(g, 12, 85, 236, 307, false);
            DivineFrame.sapphire(g, 258, 85, 424, 307, false);
            UiText.line(g, tab == 1 ? "Nearby players" : TABS[tab], 30, 94, 198, .8f, INK, true);
            if (tab != 3) UiText.line(g, records().size() + " records · scroll list", 30, 374, 198, .8f, MUTED, true);
        }
        if (tab == 1) renderBestowal(g);
        else {
            int bx = tab == 0 || tab == 5 ? 28 : 274, bw = tab == 0 || tab == 5 ? 644 : 388;
            int bh = tab == 4 ? 174 : tab == 3 ? 220 : 228;
            if (tab == 0 || tab == 5) bh = 280;
            var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
            for (String line : body()) lines.addAll(font.split(Component.literal(line), (int)(bw / .8f)));
            int visible = bh / 12;
            detailOffset = Math.clamp(detailOffset, 0, Math.max(0, lines.size() - visible));
            g.enableScissor(bx, 94, bx + bw, 94 + bh);
            g.pose().pushPose(); g.pose().translate(bx, 96, 0); g.pose().scale(.8f, .8f, 1);
            for (int i = 0; i < visible && detailOffset + i < lines.size(); i++)
                g.drawString(font, lines.get(detailOffset + i), 0, i * 15, INK, false);
            g.pose().popPose(); g.disableScissor();
            UiText.line(g, "Scroll details · Page Up / Down", bx, tab == 0 || tab == 5 ? 382 : tab == 4 ? 270 : 323, bw, .7f, MUTED, false);
        }
        UiText.line(g, data().getString("Result"), 22, 401, 656, .75f, INK, false);
        super.render(g, x, y, dt);
        g.pose().popPose();
        for (var c : controls) if (c.button.isMouseOver(x, y) || c.button.isFocused()) {
            UiText.tooltip(g, List.of(c.button.getMessage().copy()), mx, my); break;
        }
    }
    private void renderBestowal(GuiGraphics g) {
        var target = data().getCompound("Target");
        UiText.line(g, target.isEmpty() ? "No target — choose a nearby player" : "Account: " + target.getString("Username"), 274, 102, 388, .9f, INK, false);
        UiText.line(g, target.isEmpty() ? "Opening never transfers resources." : "UUID: " + id(target), 274, 122, 388, .7f, MUTED, false);
        UiText.line(g, "Transfer Essence (not EP) · available " + data().getLong("Essence"), 274, 145, 388, .8f, INK, false);
        UiText.line(g, "Owned, implemented power — none is allowed", 274, 182, 388, .8f, MUTED, false);
        UiText.line(g, "Proposed Divine Name — account identity unchanged", 274, 218, 388, .8f, MUTED, false);
        var quote = data().getCompound("Quote");
        String[] summary = quoteMatches() ? new String[]{
            "Reviewed: " + quote.getLong("Amount") + " Essence · fee burned " + quote.getLong("Fee"),
            "Total debit " + quote.getLong("Total") + " · balance after " + quote.getLong("BalanceAfter"),
            "Name: " + (name.isBlank() ? "Unchanged" : name) + " · bond " + bind,
            "Recipient must accept nearby; decline/timeout costs nothing."} :
            new String[]{"No matching quote. Choose target, edit terms, then request a quote.",
                "Fees/caps are provisional server tuning. EP is never charged.",
                state().supreme() || data().getBoolean("Bestow") ? "A quote spends nothing." : "Bestowal requires Minor God+ privileges or unique Jaycee authority."};
        for (int i = 0; i < summary.length; i++) UiText.line(g, summary[i], 274, 310 + i * 13, 388, .75f, INK, false);
    }
    @Override public boolean mouseClicked(double x, double y, int button) { return super.mouseClicked(layout.localX(x), layout.localY(y), button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased(layout.localX(x), layout.localY(y), button); }
    @Override public void mouseMoved(double x, double y) { super.mouseMoved(layout.localX(x), layout.localY(y)); }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return super.mouseDragged(layout.localX(x), layout.localY(y), button, dx / layout.scale, dy / layout.scale);
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        if (dy == 0) return false;
        int step = dy < 0 ? 1 : -1;
        if (layout.localX(x) < 248 && tab > 0 && tab < 5) offset = Math.clamp(offset + step, 0, Math.max(0, records().size() - 7));
        else detailOffset = Math.max(0, detailOffset + step * 3);
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (tab == 1 && (amountBox.isFocused() || nameBox.isFocused())) return super.keyPressed(key, scan, modifiers);
        if (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP) {
            detailOffset = Math.max(0, detailOffset + (key == GLFW.GLFW_KEY_PAGE_DOWN ? 6 : -6)); return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
}
