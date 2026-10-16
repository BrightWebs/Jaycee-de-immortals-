package com.brightwebs.jaycee.client;

import com.brightwebs.jaycee.gameplay.AbilityCatalog;
import com.brightwebs.jaycee.gameplay.PlayerState;
import com.brightwebs.jaycee.gameplay.StatusData;
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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
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
    private final MobBestowalUi.RecipientName recipientName = new MobBestowalUi.RecipientName();
    private boolean awaitingClear;
    private boolean bind, confirmed, requested;
    private EditBox amountBox, nameBox;
    private Button detailsNarration;
    private CompoundTag snapshot;
    private boolean mobs;
    private MobBestowalUi.InitialTarget initialTarget;
    private UUID mobLifetimeNonce;
    private long lastServerRemaining = Long.MIN_VALUE;
    private final MobBestowalUi.QuoteReview mobReview = new MobBestowalUi.QuoteReview();
    private final MobBestowalUi.QuoteReview playerReview = new MobBestowalUi.QuoteReview();
    private final MobBestowalUi.QuoteLifetime mobQuoteLifetime = new MobBestowalUi.QuoteLifetime();
    private int powerOffset, mobInfoOffset, dominionFilter;
    private String inspectedPower = "";
    private Button mobInfoNarration, mobQuoteNarration, playerQuoteNarration;

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
        String display = isMob(p) ? MobBestowalUi.mobTitle(name, p.getString("CustomName"), localizedOriginal(p)) :
            name.isEmpty() ? p.getString("Username") : name;
        return display + (p.getBoolean("Hidden") ? " · concealed" : "");
    }
    private void request(String op, UUID target, UUID nonce, String value, boolean flag) {
        DivinityNetwork.send(op, target, nonce, 0, value, "", flag);
    }
    private void switchTab(int page) {
        if (tab == 1) cancelDraft();
        tab = page; selection = null; offset = 0; detailOffset = 0; confirmed = false;
        clearWidgets(); init();
    }
    @Override protected void init() {
        layout = new SapphireLayout(width, height);
        controls.clear();
        detailsNarration = null;
        mobInfoNarration = null; mobQuoteNarration = null; playerQuoteNarration = null;
        if (!requested) {
            if (targeted) {
                initialTarget = new MobBestowalUi.InitialTarget(UUID.randomUUID());
                DivinityNetwork.send("profile", null, initialTarget.requestId(), 0, "", "", false);
            } else DivinityNetwork.request("refresh");
            requested = true;
        }
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            button(12 + i * 113, 9, 109, () -> TABS[index], () -> switchTab(index), () -> true);
        }
        button(20, 53, 142, () -> "Back to menu", () -> {
            if (tab == 1) cancelDraft();
            Minecraft.getInstance().setScreen(new JayceeScreen(SapphireLayout.ordinaryMenu(state().supreme(),state().playerMenuPreview())));
        }, () -> true);
        button(170, 53, 96, () -> "Refresh", () -> DivinityNetwork.request("refresh"), () -> true);
        if (tab == 1) {
            button(280, 53, 174, () -> (mobs ? "" : "◆ ") + "Players", () -> chooseCategory(false), () -> true);
            button(462, 53, 216, () -> (mobs ? "◆ " : "") + "Mobs / Entities", () -> chooseCategory(true), () -> true);
        } else if (tab == 4) {
            button(280, 53, 398, () -> switch (dominionFilter) {
                case 1 -> "Player Subordinates"; case 2 -> "Mob Subordinates";
                case 3 -> state().supreme() ? "Player adjudication" : "All Subordinates";
                default -> "All Subordinates";
            }, () -> {
                dominionFilter = (dominionFilter + 1) % (state().supreme() ? 4 : 3); selection = null; offset = 0;
                detailOffset = 0; confirmed = false;
            }, () -> true);
        }
        if (tab > 0 && tab < 5 && !(tab == 1 && mobs)) {
            for (int i = 0; i < 7; i++) {
                final int index = i;
                button(22, 108 + i * 32, 216, () -> rowLabel(index), () -> {
                    var row = row(index);
                    if (row == null) return;
                    if (tab == 1) {
                        if (initialTarget != null) initialTarget.userChose();
                        awaitingClear = false; invalidateMobQuote(); prefillRecipient(row, false);
                    }
                    selection = id(row); detailOffset = 0; confirmed = false;
                    if ((tab == 1 || tab == 3 || tab == 4) && !isMob(row))
                        request("select", selection, null, "", false);
                }, () -> row(index) != null);
            }
            button(22, 337, 104, () -> "Previous", () -> offset = Math.max(0, offset - 7), () -> offset > 0);
            button(134, 337, 104, () -> "Next", () -> offset += 7, () -> offset + 7 < records().size());
        }
        if (tab == 1 && mobs) {
            initMobBestowal();
        } else if (tab == 1) {
            var amountBounds = MobBestowalUi.PLAYER_AMOUNT;
            amountBox = new EditBox(font, amountBounds.x(), amountBounds.y(), amountBounds.width(), amountBounds.height(), Component.literal("Essence to transfer, not EP"));
            amountBox.setMaxLength(19); amountBox.setValue(amount);
            amountBox.setFilter(s -> s.matches("[0-9]*"));
            amountBox.setResponder(s -> { amount = s; invalidateMobQuote(); }); addRenderableWidget(amountBox);
            amountControls(amountBounds);
            button(274, 194, 388, () -> "Power: " + (ability.isEmpty() ? "None" : abilityName(ability)),
                this::cyclePower, this::canBestow);
            nameBox = new EditBox(font, 276, 230, 384, 16, Component.literal("Optional Divine Name; unchanged suggestion keeps the current name; account name never changes"));
            nameBox.setMaxLength(48); nameBox.setValue(name);
            nameBox.setResponder(s -> { name = s; invalidateMobQuote(); }); addRenderableWidget(nameBox);
            button(274, 256, 388, () -> "Invite subordinate bond: " + (bind ? "YES — requires consent" : "NO"),
                () -> { bind = !bind; invalidateMobQuote(); }, this::canBestow);
            button(274, 283, 388, () -> "Request server quote — spends nothing", () -> {
                var target = data().getCompound("Target");
                playerReview.requested(id(data().getCompound("Quote")));
                DivinityNetwork.send("quote", id(target), null, parsedAmount(), ability, networkName(), bind);
            }, () -> canBestow() && parsedAmount() >= 0);
            button(274, 367, 188, this::sendLabel, () -> {
                if (!quoteMatches()) return;
                request("send", null, playerReview.nonce(), "", false); invalidateMobQuote();
            }, this::quoteMatches);
            button(474, 367, 188, () -> "Cancel", this::cancelDraft, () -> true);
            playerQuoteNarration = narrative(new MobBestowalUi.Rect(274, 310, 388, 52));
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
            }, this::canAdjudicate);
            button(274, 310, 388, () -> confirmed ? "Confirmation ON — inspect target and decision" : "Confirm this specific decision: OFF",
                () -> confirmed = !confirmed, this::canAdjudicate);
            button(274, 367, 388, () -> "Apply confirmed adjudication", () -> {
                request("adjudicate", selection, null, DECISIONS[decision], true); confirmed = false;
            }, () -> canAdjudicate() && confirmed);
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
    private void chooseCategory(boolean mob) {
        if (initialTarget != null) initialTarget.userChose();
        if (mobs == mob) return;
        cancelDraft();
        mobs = mob; selection = null;
        offset = 0; detailOffset = 0; powerOffset = 0; mobInfoOffset = 0;
        amount = "0"; name = ""; ability = ""; inspectedPower = ""; bind = false;
        invalidateMobQuote(); clearWidgets(); init();
    }
    private void receiveInitialTarget() {
        if (initialTarget == null || !data().hasUUID("ProfileResponse") ||
                !initialTarget.receive(data().getUUID("ProfileResponse"))) return;
        if (tab == 1) {
            boolean mob = data().getString("TargetKind").equals("mob");
            var target = data().getCompound(mob ? "MobTarget" : "Target");
            selection = target.hasUUID("Id") ? id(target) : null;
            if (mobs != mob) {
                mobs = mob;
                clearWidgets(); init();
            }
        }
    }
    private boolean isMob(CompoundTag p) { return p.contains("EntityId") && p.contains("Type"); }
    private boolean canAdjudicate() { return state().supreme() && !selected().isEmpty() && !isMob(selected()); }
    private String localizedOriginal(CompoundTag p) {
        String value = p.getString("Original");
        return value.startsWith("entity.") ? Component.translatable(value).getString() : value;
    }
    private String mobStatus(CompoundTag p) {
        return MobBestowalUi.mobStatus(p.getString("Status"), p.getBoolean("TerminalRemoval"), p.getString("RemovalReason"));
    }
    private String mobEligibility(CompoundTag p) {
        return MobBestowalUi.eligibilityReason(p.getString("EligibilityReason"), p.getString("Reason"));
    }
    private void invalidateMobQuote() {
        mobReview.changed(); playerReview.changed(); confirmed = false;
    }
    private String networkName() { return recipientName.networkName(name); }
    private void prefillRecipient(CompoundTag target, boolean mob) {
        if (!target.hasUUID("Id")) return;
        String suggestion = mob ? MobBestowalUi.mobTitle(target.getString("DivineName"), target.getString("CustomName"), localizedOriginal(target)) :
            MobBestowalUi.playerTitle(target.getString("DivineName"), target.getString("Username"));
        if (!recipientName.select(id(target), mob, suggestion)) return;
        name = recipientName.suggestion();
        if (nameBox != null) nameBox.setValue(name);
        invalidateMobQuote();
    }
    private void receiveRecipient() {
        if (awaitingClear) {
            if (data().getCompound("Quote").isEmpty() && data().getCompound("MobQuote").isEmpty()) awaitingClear = false;
            return;
        }
        if (initialTarget != null && initialTarget.pending()) return;
        var target = data().getCompound(mobs ? "MobTarget" : "Target");
        if (selection != null && id(target).equals(selection)) prefillRecipient(target, mobs);
    }
    private void cancelDraft() {
        if (initialTarget != null) initialTarget.userChose();
        DivinityNetwork.request("clear");
        awaitingClear = true; selection = null; recipientName.clear();
        amount = "0"; name = ""; ability = ""; inspectedPower = ""; bind = false;
        invalidateMobQuote();
        if (amountBox != null) amountBox.setValue(amount);
        if (nameBox != null) nameBox.setValue(name);
    }
    private void amountControls(MobBestowalUi.Rect amount) {
        var decrease = MobBestowalUi.decrease(amount); var increase = MobBestowalUi.increase(amount);
        button(decrease.x(), decrease.y(), decrease.width(), () -> "−", () -> adjustAmount(-1), () -> parsedAmount() > 0);
        button(increase.x(), increase.y(), increase.width(), () -> "+", () -> adjustAmount(1), () -> parsedAmount() < Long.MAX_VALUE);
    }
    private void adjustAmount(long delta) {
        amountBox.setValue(Long.toString(MobBestowalUi.adjustAmount(parsedAmount(), delta)));
    }
    private MobBestowalUi.Terms mobTerms() {
        return new MobBestowalUi.Terms(id(data().getCompound("MobTarget")), parsedAmount(), ability, networkName(), bind);
    }
    private boolean mobQuoteTermsMatch() {
        var q = data().getCompound("MobQuote");
        return q.hasUUID("Id") && q.hasUUID("Target") && MobBestowalUi.matching(mobTerms(),
            new MobBestowalUi.Terms(q.getUUID("Target"), q.getLong("Amount"), q.getString("Ability"),
                q.getString("Name"), q.getBoolean("Bind")));
    }
    private void receiveMobQuote() {
        var q = data().getCompound("MobQuote");
        UUID nonce = q.hasUUID("Id") ? id(q) : null;
        long remaining = q.getLong("RemainingMillis");
        if (!java.util.Objects.equals(nonce, mobLifetimeNonce) || remaining != lastServerRemaining) {
            mobLifetimeNonce = nonce; lastServerRemaining = remaining;
            mobQuoteLifetime.receive(q.getLong("RemainingMillis"), net.minecraft.Util.getMillis());
        }
        UUID before = mobReview.nonce();
        mobReview.receive(q.hasUUID("Id") ? id(q) : null, mobQuoteTermsMatch());
        if (!java.util.Objects.equals(before, mobReview.nonce())) confirmed = false;
    }
    private boolean mobQuoteMatches() {
        var q = data().getCompound("MobQuote");
        return canMobBestow() && mobReview.matches(id(q), mobQuoteTermsMatch()) &&
            (!q.contains("RemainingMillis") || mobQuoteLifetime.remaining(net.minecraft.Util.getMillis()) > 0);
    }
    private List<CompoundTag> mobPowers() {
        var powers = new ArrayList<CompoundTag>();
        // "None" is a real no-power transaction, never a fabricated grant.
        var none = new CompoundTag();
        none.putString("Name", "No power"); none.putBoolean("Compatible", true);
        none.putString("Description", "Transfer Essence and/or bestow a name without granting a power.");
        powers.add(none);
        for (Tag tag : data().getList("MobPowers", Tag.TAG_COMPOUND)) powers.add((CompoundTag)tag);
        return powers;
    }
    private CompoundTag powerRow(int index) {
        var powers = mobPowers();
        powerOffset = MobBestowalUi.offset(powerOffset, 0, powers.size(), MobBestowalUi.POWER_ROWS);
        return powerOffset + index < powers.size() ? powers.get(powerOffset + index) : null;
    }
    private CompoundTag inspectedMobPower() {
        return mobPowers().stream().filter(p -> p.getString("Id").equals(inspectedPower)).findFirst().orElseGet(CompoundTag::new);
    }
    private boolean canMobBestow() {
        var t = data().getCompound("MobTarget");
        boolean powerAllowed = ability.isEmpty() || mobPowers().stream().anyMatch(p ->
            p.getString("Id").equals(ability) && p.getBoolean("Compatible"));
        return !awaitingClear && !t.isEmpty() && id(t).equals(selection) && t.getString("Status").equals("active") && !t.getBoolean("TerminalRemoval") &&
            (!t.contains("Eligible") || t.getBoolean("Eligible")) &&
            MobBestowalUi.bindingAllowed(bind, t.contains("BindingCompatible"), t.getBoolean("BindingCompatible")) &&
            (state().supreme() || data().getBoolean("Bestow")) && parsedAmount() >= 0 &&
            parsedAmount() <= data().getLong("Essence") && powerAllowed;
    }
    private Button contentButton(int x, int y, int w, int h, Supplier<String> text, Runnable action, BooleanSupplier enabled) {
        var b = new Button(x, y, w, h, Component.literal(text.get()), ignored -> action.run(), narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {
                DivineFrame.sapphire(g, getX(), getY(), width, height, isHoveredOrFocused());
            }
        };
        controls.add(new Control(b, text, enabled)); return addRenderableWidget(b);
    }
    private Button narrative(MobBestowalUi.Rect r) {
        return addRenderableWidget(new Button(r.x(), r.y(), r.width(), r.height(), Component.empty(),
            ignored -> {}, narration -> narration.get()) {
            @Override protected void renderWidget(GuiGraphics g, int mx, int my, float dt) {}
        });
    }
    private void initMobBestowal() {
        for (int i = 0; i < MobBestowalUi.ROWS; i++) {
            final int index = i;
            contentButton(20, 112 + i * 34, 188, 32, () -> {
                var r = row(index); return r == null ? "Empty mob row" : title(r) + ". " + localizedOriginal(r) +
                    ". " + mobStatus(r) + ". " + r.getString("Dimension");
            }, () -> {
                var r = row(index); if (r == null) return;
                if (initialTarget != null) initialTarget.userChose();
                awaitingClear = false; prefillRecipient(r, true);
                selection = id(r); mobInfoOffset = 0; powerOffset = 0; detailOffset = 0;
                ability = ""; inspectedPower = ""; invalidateMobQuote();
                request("mob_select", selection, null, "", false);
            }, () -> row(index) != null);
        }
        button(20, 367, 90, () -> "Previous", () -> offset = Math.max(0, offset - 7), () -> offset > 0);
        button(116, 367, 92, () -> "Next", () -> offset += 7, () -> offset + 7 < records().size());
        nameBox = new EditBox(font, 234, 243, 222, 16, Component.literal("Optional mob Divine Name; unchanged suggestion keeps the current name, including an unnamed mob"));
        nameBox.setMaxLength(48); nameBox.setValue(name);
        nameBox.setResponder(s -> { name = s; invalidateMobQuote(); }); addRenderableWidget(nameBox);
        var amountBounds = MobBestowalUi.MOB_AMOUNT;
        amountBox = new EditBox(font, amountBounds.x(), amountBounds.y(), amountBounds.width(), amountBounds.height(), Component.literal("Essence to transfer, not EP"));
        amountBox.setMaxLength(19); amountBox.setValue(amount); amountBox.setFilter(s -> s.matches("[0-9]*"));
        amountBox.setResponder(s -> { amount = s; invalidateMobQuote(); }); addRenderableWidget(amountBox);
        amountControls(amountBounds);
        button(232, 303, 228, () -> "Subordinate bond: " + (bind ? "YES" : "NO"), () -> {
            bind = !bind; invalidateMobQuote();
        }, () -> !data().getCompound("MobTarget").isEmpty());
        for (int i = 0; i < MobBestowalUi.POWER_ROWS; i++) {
            final int index = i;
            contentButton(232, 342 + i * 23, 228, 22, () -> {
                var p = powerRow(index);
                return p == null ? "No more powers" : p.getString("Name") + ". " + p.getString("Description") +
                    ". " + (p.getBoolean("Compatible") ? "Compatible" : "Unavailable: " + p.getString("Reason")) +
                    ". Effect: " + p.getString("Effect") + ". Requirements: " + p.getString("Requirements") +
                    ". Cost: " + p.getLong("Cost") + " Essence";
            }, () -> {
                var p = powerRow(index); if (p == null) return;
                inspectedPower = p.getString("Id"); detailOffset = 0;
                if (p.getBoolean("Compatible")) { ability = inspectedPower; invalidateMobQuote(); }
            }, () -> powerRow(index) != null && id(data().getCompound("MobTarget")).equals(selection));
        }
        button(486, 337, 192, () -> "Request server quote", () -> {
            confirmed = false; mobReview.requested(id(data().getCompound("MobQuote")));
            DivinityNetwork.send("mob_quote", selection, null, parsedAmount(), ability, networkName(), bind);
        }, this::canMobBestow);
        button(486, 365, 92, () -> "Bless · confirm", () -> {
            if (!mobQuoteMatches()) return;
            request("mob_confirm", null, mobReview.nonce(), "", false); invalidateMobQuote();
        }, this::mobQuoteMatches);
        button(586, 365, 92, () -> "Cancel", this::cancelDraft, () -> true);
        mobInfoNarration = narrative(MobBestowalUi.INFO);
        mobQuoteNarration = narrative(MobBestowalUi.QUOTE_TEXT);
    }
    private List<CompoundTag> records() {
        if (tab == 4) {
            var result = new ArrayList<CompoundTag>();
            for (String source : List.of("Petitions", "Nearby", "Followers", "MobFollowers"))
                for (Tag tag : data().getList(source, Tag.TAG_COMPOUND)) {
                    var record = (CompoundTag)tag;
                    if (MobBestowalUi.dominionRecord(dominionFilter, state().supreme(), source, record.hasUUID("Patron")) &&
                            result.stream().noneMatch(r -> id(r).equals(id(record)))) result.add(record);
                }
            return result;
        }
        String key = switch (tab) {
            case 1 -> mobs ? "NearbyMobs" : "Nearby"; case 2 -> "Pending"; case 3 -> "Followers";
            default -> "";
        };
        var result = new ArrayList<CompoundTag>();
        for (Tag tag : data().getList(key, Tag.TAG_COMPOUND)) result.add((CompoundTag)tag);
        return result;
    }
    private CompoundTag row(int index) {
        var rows = records();
        offset = Math.clamp(offset, 0, Math.max(0, rows.size() - (tab == 1 && mobs ? MobBestowalUi.ROWS : 1)));
        return offset + index < rows.size() ? rows.get(offset + index) : null;
    }
    private CompoundTag selected() {
        return records().stream().filter(r -> id(r).equals(selection)).findFirst().orElseGet(CompoundTag::new);
    }
    private String rowLabel(int index) {
        var r = row(index);
        if (r == null) return "—";
        return tab == 2 ? r.getString("DonorName") + " → " + r.getString("RecipientName") :
            title(r) + (isMob(r) ? " · " + mobStatus(r) : "");
    }
    private boolean canBestow() {
        var d = data(); var t = d.getCompound("Target");
        return !awaitingClear && !t.isEmpty() && id(t).equals(selection) && !t.getBoolean("Hidden") && !id(t).equals(self()) &&
            (state().supreme() || d.getBoolean("Bestow")) && parsedAmount() >= 0 && parsedAmount() <= d.getLong("Essence");
    }
    private long parsedAmount() {
        try { return Long.parseLong(amount); } catch (NumberFormatException e) { return -1; }
    }
    private boolean playerQuoteTermsMatch() {
        var q = data().getCompound("Quote"); var t = data().getCompound("Target");
        return canBestow() && q.hasUUID("Id") && q.hasUUID("Target") && q.getUUID("Target").equals(id(t))
            && q.getLong("Amount") == parsedAmount() && q.getString("Ability").equals(ability)
            && q.getString("Name").equals(networkName()) && q.getBoolean("Bind") == bind;
    }
    private boolean quoteMatches() {
        return playerReview.matches(id(data().getCompound("Quote")), playerQuoteTermsMatch());
    }
    private String sendLabel() {
        return "Bless · send offer";
    }
    private boolean canDecideRelease() {
        var p = selected();
        return !isMob(p) && !p.getString("Release").isEmpty() &&
            (state().supreme() || p.hasUUID("Patron") && p.getUUID("Patron").equals(self()));
    }
    private void cyclePower() {
        var ids = new ArrayList<String>(); ids.add("");
        AbilityCatalog.registered().stream().filter(a -> state().supreme() ||
            data().getCompound("Grants").contains(a.id())).filter(AbilityCatalog.Art::implemented).forEach(a -> ids.add(a.id()));
        ability = ids.get((ids.indexOf(ability) + 1) % ids.size());
        invalidateMobQuote();
    }
    private String abilityName(String id) {
        var art = AbilityCatalog.find(id); return art == null ? id : art.name();
    }
    private List<String> profileLines(CompoundTag p) {
        if (isMob(p)) return mobProfileLines(p);
        var lines = new ArrayList<String>();
        if (p.isEmpty()) { lines.add("No selected player. Choose a nearby player or refresh."); return lines; }
        lines.add("Account: " + p.getString("Username"));
        lines.add("Original account: " + p.getString("Original"));
        lines.add("UUID: " + id(p));
        if (p.getBoolean("Hidden")) { lines.add("Jaycee's true profile is concealed. No private statistics were sent."); return lines; }
        lines.add("Divine Name: " + (p.getString("DivineName").isEmpty() ? "Not bestowed" : p.getString("DivineName")));
        lines.add("Rank: " + p.getString("Rank").replace('_', ' '));
        if (p.getBoolean("Supreme")) lines.add("Authority: Supreme Existence — unique server-assigned Jaycee");
        if (p.contains("DivineLevel")) {
            lines.add("Divine Level: " + p.getInt("DivineLevel") + "; Grade: " + p.getString("Grade"));
            lines.add("Earned Essence: " + p.getLong("EarnedEssence") + "; next level: " +
                p.getLong("LevelProgress") + " / " + p.getLong("LevelRequired"));
        }
        lines.add("EP: " + p.getLong("Ep") + " — permanent power");
        lines.add("Essence: " + p.getLong("Essence") + " — transferable reserve");
        lines.add("Patron: " + (p.hasUUID("Patron") ? p.getUUID("Patron") : "None"));
        lines.add("Release request: " + (p.getString("Release").isEmpty() ? "None" : p.getString("Release")));
        lines.add("NPC lineage metadata: " + p.getList("Npc", Tag.TAG_STRING));
        lines.add("Granted powers: " + String.join(", ", p.getCompound("Grants").getAllKeys().stream().sorted().map(this::abilityName).toList()));
        lines.add("No account rename, natural promotion or NPC offering recipe is implied.");
        return lines;
    }
    private List<String> mobProfileLines(CompoundTag p) {
        var lines = new ArrayList<String>();
        if (p.isEmpty()) {
            lines.add("Choose a nearby eligible mob. Opening this screen never transfers anything.");
            return lines;
        }
        lines.add("Identity: " + title(p));
        lines.add("Original: " + localizedOriginal(p));
        lines.add("Custom name: " + (p.getString("CustomName").isEmpty() ? "None" : p.getString("CustomName")));
        lines.add("Type: " + p.getString("Type"));
        lines.add("UUID: " + id(p));
        lines.add("Status: " + mobStatus(p) + " · " + p.getString("Dimension"));
        if (!mobEligibility(p).isEmpty()) lines.add("Eligibility: " + mobEligibility(p));
        lines.add(MobBestowalUi.healthLabel(p.getString("Status"), p.getBoolean("TerminalRemoval")) +
            ": " + p.getFloat("Health") + " / " + p.getFloat("MaxHealth"));
        lines.add("EP: " + p.getLong("Ep") + " — not consumed");
        lines.add("Rank: " + p.getString("Rank"));
        lines.add("Master: " + (p.getBoolean("MasterHidden") ? "Concealed" :
            p.getString("MasterName").isEmpty() ? "None" : p.getString("MasterName")));
        lines.add("Patron: " + (p.hasUUID("Patron") ? p.getUUID("Patron") : "None"));
        lines.add("Essence: " + p.getLong("Essence") + "; received: " + p.getLong("Received"));
        if (p.contains("EarnedEssence")) lines.add("Lifetime earned Essence: " + p.getLong("EarnedEssence"));
        if (p.contains("DivineLevel")) lines.add("Divine Level: " + p.getInt("DivineLevel"));
        if (p.contains("Grade")) lines.add("Grade: " + p.getString("Grade"));
        if (!p.getString("ProgressionReason").isBlank()) lines.add("Progression: " + p.getString("ProgressionReason"));
        lines.add("Subordinate: " + p.getBoolean("Subordinate"));
        lines.add("Granted powers: " + (p.getCompound("Grants").isEmpty() ? "None" :
            String.join(", ", p.getCompound("Grants").getAllKeys().stream().sorted().map(this::abilityName).toList())));
        if (!p.getString("Rules").isEmpty()) lines.add("Server rules: " + p.getString("Rules"));
        lines.add("Only synced values are shown. No inferred levels or natural evolution.");
        return lines;
    }
    private List<String> mobQuoteLines() {
        var lines = new ArrayList<String>();
        var t = data().getCompound("MobTarget"); var p = inspectedMobPower();
        if (selection != null && !id(t).equals(selection)) return List.of(
            "Waiting for selected mob's server profile and compatible power list.",
            "No resources are spent by selecting a mob. EP conversion and evolution rules are unavailable.");
        lines.add(t.isEmpty() ? "Select a mob first." : "Recipient: " + title(t));
        if (!mobEligibility(t).isEmpty()) lines.add("Eligibility: " + mobEligibility(t));
        String bindingReason = t.getString("BindingReason");
        if (!bindingReason.isBlank()) lines.add((bind ? "Binding restriction: " : "Binding caveat: ") + bindingReason);
        if (!t.getString("Rules").isEmpty()) lines.add("Server rules: " + t.getString("Rules"));
        lines.add("Your Essence: " + data().getLong("Essence"));
        lines.add("Selected power: " + (ability.isEmpty() ? "None" : abilityName(ability)));
        String powerRequirements = data().getString("MobPowerRequirements");
        if (!powerRequirements.isBlank()) lines.add("Power access: " + powerRequirements);
        if (!p.isEmpty()) {
            lines.add("Inspecting: " + p.getString("Name"));
            lines.add(p.getString("Description"));
            lines.add(p.getBoolean("Compatible") ? "Compatible — server-authorized" : "Cannot grant: " + p.getString("Reason"));
            if (!p.getString("Effect").isEmpty()) lines.add("Effect: " + p.getString("Effect"));
            if (!p.getString("Requirements").isEmpty()) lines.add("Requirements: " + p.getString("Requirements"));
            if (!p.getString("Id").isEmpty()) lines.add("Power cost: " + p.getLong("Cost") + " Essence (included in quoted fee)");
        }
        if (mobQuoteMatches()) {
            var q = data().getCompound("MobQuote");
            lines.add("SERVER QUOTE — review before confirming");
            lines.add("Transfer: " + q.getLong("Amount") + " Essence");
            lines.add("Burned fee: " + q.getLong("Fee") + "; total debit: " + q.getLong("Total"));
            lines.add("Your balance after" + (q.getBoolean("BalanceAfterMinimum") ? " (minimum)" : "") + ": " + q.getLong("BalanceAfter"));
            lines.add("Recipient Essence after: " + q.getLong("RecipientAfter"));
            lines.add("Name: " + (networkName().isEmpty() ? "Unchanged" : networkName()) + "; subordinate bond: " + bind);
            if (!q.getString("Effect").isEmpty()) lines.add("Quoted effect: " + q.getString("Effect"));
            if (!q.getString("Requirements").isEmpty()) lines.add("Quoted requirements: " + q.getString("Requirements"));
            lines.add(q.contains("RemainingMillis") ? "Quote remaining: " +
                (mobQuoteLifetime.remaining(net.minecraft.Util.getMillis()) / 1000) + " seconds (server TTL)." :
                "Server expiry: " + q.getLong("Expires") + " epoch ms; no client clock assumption.");
            lines.add("Server rechecks expiry and every requirement before spending.");
            lines.add("After reviewing this exact quote, Bless confirms once. Cancel discards the draft without spending.");
        } else lines.add("No matching reviewed quote. Changes invalidate confirmation; request a new server quote.");
        if (!(state().supreme() || data().getBoolean("Bestow")))
            lines.add("Requires Minor God+ bestowal privileges or unique Jaycee authority.");
        lines.add("EP is never consumed. No EP conversion or evolution rules are available.");
        lines.add("No client-created powers or levels. Unsupported entities/powers are not universally compatible.");
        lines.add("Resource development: unavailable — no offering or evolution action is implemented.");
        return lines;
    }
    private List<String> body() {
        if (tab == 0) {
            var lines = profileLines(data());
            lines.add("Essence maximum: " + data().getLong("EssenceCap") + "; refill " + data().getLong("EssenceRegen") + "/second while online.");
            lines.add("EP cap: " + data().getLong("EpCap") + ". Cap field and ordinary fees are provisional server tuning, not settled canon.");
            lines.add("Mana is independent. Bestowal never deducts EP. /jc debug jayceetest is not sovereign authority.");
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
        if (tab == 4 && isMob(selected())) {
            lines.add("Mob record only. Player adjudication, consent and release operations do not apply.");
            lines.add("Resource development unavailable. No client or server chunk loading is requested.");
        } else if (tab == 4) lines.add(state().supreme() ? "Confirm decisions explicitly. Cap revocation never erases earned EP." :
            "Dominion adjudication is restricted to the unique Jaycee authority.");
        return lines;
    }
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float dt) {}
    @Override public void render(GuiGraphics g, int mx, int my, float dt) {
        super.renderBackground(g, mx, my, dt);
        if (Minecraft.getInstance().player == null) return;
        snapshot = state().divinity();
        receiveInitialTarget();
        if (tab == 1) receiveRecipient();
        if (tab == 1 && mobs) receiveMobQuote();
        else if (tab == 1) {
            var q = data().getCompound("Quote");
            playerReview.receive(q.hasUUID("Id") ? id(q) : null, playerQuoteTermsMatch());
        }
        int x = (int)Math.floor(layout.localX(mx)), y = (int)Math.floor(layout.localY(my));
        for (var c : controls) {
            c.button.setMessage(Component.literal(c.text.get())); c.button.active = c.enabled.getAsBoolean();
            c.button.visible = !(tab == 4 && isMob(selected()) && c.button.getY() >= 283 && c.button.getX() >= 274);
        }
        if (detailsNarration != null) detailsNarration.setMessage(Component.literal(String.join(". ", body()) + ". Page Up and Page Down scroll details."));
        if (mobInfoNarration != null) mobInfoNarration.setMessage(Component.literal(
            String.join(". ", mobProfileLines(data().getCompound("MobTarget"))) + ". Scroll here for identity and synced statistics."));
        if (mobQuoteNarration != null) mobQuoteNarration.setMessage(Component.literal(
            (bind ? "" : MobBestowalUi.DESPAWN_TEXT + " ") + String.join(". ", mobQuoteLines()) +
                ". Scroll here or Page Up and Down to review transaction."));
        if (playerQuoteNarration != null) playerQuoteNarration.setMessage(Component.literal(String.join(". ", playerQuoteSummary())));
        if (tab == 1) { amountBox.active = true; nameBox.active = true; }
        g.pose().pushPose(); g.pose().translate(layout.left, layout.top, 0);
        g.pose().scale((float)layout.scale, (float)layout.scale, 1);
        DivineFrame.sapphire(g, 0, 0, 700, 420, false);
        if (tab != 1 && tab != 4) UiText.line(g, "Divine Ascension · " + TABS[tab], 280, 61, 398, 1, INK, true);
        if (tab > 0 && tab < 5 && !(tab == 1 && mobs)) {
            DivineFrame.sapphire(g, 12, 85, 236, 307, false);
            DivineFrame.sapphire(g, 258, 85, 424, 307, false);
            UiText.line(g, tab == 1 ? "Nearby players" : TABS[tab], 30, 94, 198, .8f, INK, true);
            if (tab != 3) UiText.line(g, records().size() + " records · scroll list", 30, 374, 198, .8f, MUTED, true);
        }
        if (tab == 1 && mobs) renderMobBestowal(g, x, y);
        else if (tab == 1) renderBestowal(g);
        else if (tab == 4 && isMob(selected())) {
            renderMobPreview(g, selected(), new MobBestowalUi.Rect(274, 100, 112, 152), x, y);
            detailOffset = wrapped(g, body(), new MobBestowalUi.Rect(394, 96, 268, 174), detailOffset);
            UiText.paragraph(g, "Mob record · player adjudication disabled. Resource development unavailable. " +
                "Unloaded/deceased records are never chunk-loaded for previews.", 274, 287, 388, 76, .8f, MUTED);
            UiText.line(g, "Scroll identity and synced details · Page Up / Down", 274, 270, 388, .7f, MUTED, false);
        }
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
        if (tab == 1 && mobs) renderMobRows(g, x, y);
        g.pose().popPose();
        for (var c : controls) if (c.button.visible && (c.button.isMouseOver(x, y) || c.button.isFocused())) {
            UiText.tooltip(g, List.of(c.button.getMessage().copy()), mx, my); break;
        }
    }
    private void renderBestowal(GuiGraphics g) {
        var target = data().getCompound("Target");
        UiText.line(g, target.isEmpty() ? "No target — choose a nearby player" : "Account: " + target.getString("Username"), 274, 102, 388, .9f, INK, false);
        UiText.line(g, target.isEmpty() ? "Opening never transfers resources." : "UUID: " + id(target), 274, 122, 388, .7f, MUTED, false);
        UiText.line(g, "Transfer Essence (not EP) · available " + data().getLong("Essence"), 274, 145, 388, .8f, INK, false);
        UiText.line(g, "Owned, implemented power — none is allowed", 274, 182, 388, .8f, MUTED, false);
        UiText.line(g, "Optional name · unchanged suggestion keeps current name", 274, 218, 388, .8f, MUTED, false);
        var summary = playerQuoteSummary();
        for (int i = 0; i < summary.size(); i++) UiText.line(g, summary.get(i), 274, 310 + i * 13, 388, .75f, INK, false);
    }
    private List<String> playerQuoteSummary() {
        var quote = data().getCompound("Quote");
        return quoteMatches() ? List.of(
            "Reviewed: " + quote.getLong("Amount") + " Essence · fee burned " + quote.getLong("Fee"),
            "Total debit " + quote.getLong("Total") + " · balance after " + quote.getLong("BalanceAfter"),
            "Name: " + (networkName().isEmpty() ? "Unchanged" : networkName()) + " · bond " + bind,
            "Bless sends an offer; the recipient must accept. Cancel spends nothing.") :
            List.of("No matching quote. Choose target, edit terms, then request a quote.",
                "Fees/caps are provisional server tuning. EP is never charged.",
                state().supreme() || data().getBoolean("Bestow") ? "A quote spends nothing." : "Bestowal requires Minor God+ privileges or unique Jaycee authority.");
    }
    private int wrapped(GuiGraphics g, List<String> text, MobBestowalUi.Rect r, int scroll) {
        var lines = new ArrayList<net.minecraft.util.FormattedCharSequence>();
        for (String line : text) lines.addAll(font.split(Component.literal(line), (int)(r.width() / .8f)));
        int visible = r.height() / 12;
        scroll = MobBestowalUi.offset(scroll, 0, lines.size(), visible);
        g.enableScissor(r.x(), r.y(), r.x() + r.width(), r.y() + r.height());
        g.pose().pushPose(); g.pose().translate(r.x(), r.y(), 0); g.pose().scale(.8f, .8f, 1);
        for (int i = 0; i < visible && scroll + i < lines.size(); i++)
            g.drawString(font, lines.get(scroll + i), 0, i * 15, INK, false);
        g.pose().popPose(); g.disableScissor();
        return scroll;
    }
    private LivingEntity loadedMob(CompoundTag record) {
        var world = Minecraft.getInstance().level;
        if (world == null || record.getBoolean("TerminalRemoval") || !record.getString("Status").equals("active") ||
                !record.getString("Dimension").equals(world.dimension().location().toString()) || !record.hasUUID("Id"))
            return null;
        var entity = world.getEntity(record.getInt("EntityId"));
        return entity instanceof LivingEntity living && living.getUUID().equals(id(record)) && living.isAlive() ? living : null;
    }
    private void renderMobPreview(GuiGraphics g, CompoundTag record, MobBestowalUi.Rect r, int mx, int my) {
        DivineFrame.sapphire(g, r.x(), r.y(), r.width(), r.height(), false);
        var entity = loadedMob(record);
        if (entity != null) {
            // Inventory rendering and its scissor share this screen's reference-space pose.
            // Passing screen pixels here would apply GUI scaling twice.
            float extent = Math.max(.5f, Math.max(entity.getBbWidth(), entity.getBbHeight()));
            int scale = Math.max(1, (int)(Math.min(r.width() - 12, r.height() - 16) / extent));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, r.x() + 3, r.y() + 3,
                r.x() + r.width() - 3, r.y() + r.height() - 3, scale, .0625f, mx, my, entity);
        } else {
            if (r.height() > 50)
                UiText.paragraph(g, "Live preview unavailable", r.x() + 5, r.y() + r.height() - 26,
                    r.width() - 10, 24, .65f, MUTED);
        }
    }
    private void renderMobBestowal(GuiGraphics g, int mx, int my) {
        for (var r : List.of(MobBestowalUi.LIST, MobBestowalUi.CENTER, MobBestowalUi.QUOTE))
            DivineFrame.sapphire(g, r.x(), r.y(), r.width(), r.height(), false);
        var patrons = PatronTheme.retained(StatusData.patrons(state()));
        var palette = PatronTheme.palette(patrons);
        UiText.line(g, "Nearby eligible entities", 22, 96, 184, .8f, INK, true);
        UiText.line(g, "Identity · your lineage frame", 234, 96, 202, .75f, palette.accent(), true);
        PatronEmblem.icon(g, palette.id(), 442, 91, 16);
        UiText.line(g, "Transaction · scroll to review", 486, 96, 192, .75f, palette.accent(), true);
        var target = data().getCompound("MobTarget");
        boolean waiting = selection != null && !id(target).equals(selection);
        renderMobPreview(g, waiting ? new CompoundTag() : target, MobBestowalUi.PREVIEW, mx, my);
        mobInfoOffset = wrapped(g, waiting ? List.of("Waiting for selected mob's server profile.") : mobProfileLines(target),
            MobBestowalUi.INFO, mobInfoOffset);
        UiText.line(g, "Optional name · unchanged = keep", 234, 232, 222, .73f, MUTED, false);
        UiText.line(g, "Essence, not EP · available " + data().getLong("Essence"), 234, 267, 222, .75f, INK, false);
        UiText.line(g, "Owned powers · scroll · selected: " + (ability.isEmpty() ? "None" : abilityName(ability)),
            234, 331, 222, .7f, MUTED, false);
        if (!bind) {
            var warning = MobBestowalUi.DESPAWN_WARNING;
            UiText.paragraph(g, MobBestowalUi.DESPAWN_TEXT, warning.x(), warning.y(), warning.width(),
                warning.height(), .75f, palette.secondary());
            g.fill(486, 160, 678, 161, palette.secondary());
        }
        detailOffset = wrapped(g, mobQuoteLines(), bind ? MobBestowalUi.QUOTE_TEXT : MobBestowalUi.UNBOUND_QUOTE_TEXT, detailOffset);
        if (records().isEmpty())
            UiText.paragraph(g, "No eligible entities nearby. Refresh after approaching a mob. No entities are spawned or loaded by this menu.",
                28, 144, 172, 124, .8f, MUTED);
    }
    private void renderMobRows(GuiGraphics g, int mx, int my) {
        for (int i = 0; i < MobBestowalUi.ROWS; i++) {
            var r = row(i); if (r == null) continue;
            int y = 112 + i * 34;
            if (id(r).equals(selection)) g.fill(21, y + 2, 23, y + 30, INK);
            renderMobPreview(g, r, new MobBestowalUi.Rect(26, y + 3, 28, 26), mx, my);
            UiText.line(g, title(r), 60, y + 5, 140, .78f, INK, false);
            UiText.line(g, mobStatus(r) + " · " + localizedOriginal(r), 60, y + 18, 140, .65f, MUTED, false);
        }
        for (int i = 0; i < MobBestowalUi.POWER_ROWS; i++) {
            var p = powerRow(i); if (p == null) continue;
            int y = 342 + i * 23; String power = p.getString("Id");
            if (!power.isEmpty()) {
                var art = AbilityCatalog.find(power);
                if (art == null) AbilityIcons.render(g, power, 237, y + 3, 16, power.equals(ability));
                else AbilityIcons.render(g, art, 237, y + 3, 16, power.equals(ability));
            } else DivineFrame.gem(g, 245, y + 11, 3, MUTED);
            if (power.equals(ability)) g.fill(233, y + 2, 235, y + 20, INK);
            UiText.line(g, (power.equals(ability) ? "◆ " : "") + p.getString("Name") +
                (p.getBoolean("Compatible") ? "" : " · unavailable"), 259, y + 7, 194, .75f,
                p.getBoolean("Compatible") ? INK : MUTED, false);
        }
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
        double lx = layout.localX(x), ly = layout.localY(y);
        if (tab == 1 && mobs) {
            if (MobBestowalUi.LIST.contains(lx, ly))
                offset = MobBestowalUi.offset(offset, step, records().size(), MobBestowalUi.ROWS);
            else if (MobBestowalUi.POWERS.contains(lx, ly))
                powerOffset = MobBestowalUi.offset(powerOffset, step, mobPowers().size(), MobBestowalUi.POWER_ROWS);
            else if (MobBestowalUi.INFO.contains(lx, ly)) mobInfoOffset = Math.max(0, mobInfoOffset + step * 3);
            else if (MobBestowalUi.QUOTE_TEXT.contains(lx, ly)) detailOffset = Math.max(0, detailOffset + step * 3);
            else return super.mouseScrolled(lx, ly, dx, dy);
            return true;
        }
        if (layout.localX(x) < 248 && tab > 0 && tab < 5) offset = Math.clamp(offset + step, 0, Math.max(0, records().size() - 7));
        else detailOffset = Math.max(0, detailOffset + step * 3);
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (tab == 1 && (amountBox.isFocused() || nameBox.isFocused())) return super.keyPressed(key, scan, modifiers);
        if (key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP) {
            int step = key == GLFW.GLFW_KEY_PAGE_DOWN ? 1 : -1;
            if (tab == 1 && mobs && mobInfoNarration != null && mobInfoNarration.isFocused())
                mobInfoOffset = Math.max(0, mobInfoOffset + step * 6);
            else if (tab == 1 && mobs && controls.stream().anyMatch(c -> c.button.isFocused() &&
                    c.button.getX() == 232 && c.button.getY() >= 342))
                powerOffset = MobBestowalUi.offset(powerOffset, step * MobBestowalUi.POWER_ROWS,
                    mobPowers().size(), MobBestowalUi.POWER_ROWS);
            else detailOffset = Math.max(0, detailOffset + step * 6);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() {
        if (tab == 1) cancelDraft();
        super.onClose();
    }
}
