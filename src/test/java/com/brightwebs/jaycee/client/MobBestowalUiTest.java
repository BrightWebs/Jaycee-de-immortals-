package com.brightwebs.jaycee.client;

import java.util.List;
import java.util.UUID;

public final class MobBestowalUiTest {
    private static int checks;
    public static void main(String[] args) {
        var panels = List.of(MobBestowalUi.LIST, MobBestowalUi.CENTER, MobBestowalUi.QUOTE);
        for (int i = 0; i < panels.size(); i++) {
            var r = panels.get(i);
            check(r.x() >= 12 && r.x() + r.width() <= 688, "Panel stays inside fantasy frame");
            check(r.y() == 85 && r.y() + r.height() == 392, "Panel does not cover tabs or result");
            if (i > 0) check(panels.get(i - 1).x() + panels.get(i - 1).width() < r.x(), "Three separate panels");
        }
        inside(MobBestowalUi.PREVIEW, MobBestowalUi.CENTER);
        inside(MobBestowalUi.INFO, MobBestowalUi.CENTER);
        inside(MobBestowalUi.POWERS, MobBestowalUi.CENTER);
        inside(MobBestowalUi.QUOTE_TEXT, MobBestowalUi.QUOTE);
        inside(MobBestowalUi.DESPAWN_WARNING, MobBestowalUi.QUOTE);
        inside(MobBestowalUi.UNBOUND_QUOTE_TEXT, MobBestowalUi.QUOTE);
        check(MobBestowalUi.DESPAWN_WARNING.y() + MobBestowalUi.DESPAWN_WARNING.height() <
            MobBestowalUi.UNBOUND_QUOTE_TEXT.y(), "Unbound despawn warning remains outside transaction scroll");
        check(MobBestowalUi.PREVIEW.x() + MobBestowalUi.PREVIEW.width() < MobBestowalUi.INFO.x(),
            "Live preview cannot cover synced text");
        check(MobBestowalUi.POWERS.height() == MobBestowalUi.POWER_ROWS * 23, "Power viewport contains whole icon rows");
        for (int[] window : new int[][]{{1920, 1080}, {1280, 720}, {854, 480}, {640, 360}}) {
            for (int guiScale = 1; guiScale <= 4; guiScale++) {
                var layout = new SapphireLayout(window[0] / guiScale, window[1] / guiScale);
                for (var r : List.of(MobBestowalUi.LIST, MobBestowalUi.PREVIEW, MobBestowalUi.INFO,
                        MobBestowalUi.POWERS, MobBestowalUi.QUOTE_TEXT)) {
                    double x = r.x() + r.width() / 2.0, y = r.y() + r.height() / 2.0;
                    check(r.contains(layout.localX(layout.left + x * layout.scale),
                        layout.localY(layout.top + y * layout.scale)), "Preview, scroll and click coordinates share inverse transform");
                    check(layout.screenX(r.x()) >= 0 && layout.screenY(r.y()) >= 0 &&
                        layout.screenX(r.x() + r.width()) <= window[0] / guiScale &&
                        layout.screenY(r.y() + r.height()) <= window[1] / guiScale, "Scaled viewport remains visible");
                }
            }
        }
        check(MobBestowalUi.offset(0, -1, 20, 7) == 0, "List clamps at start");
        check(MobBestowalUi.offset(13, 1, 20, 7) == 13, "List clamps at final complete page");
        check(MobBestowalUi.offset(Integer.MAX_VALUE, 3, 20, 2) == 18, "Scroll arithmetic cannot overflow");
        check(MobBestowalUi.offset(7, 0, 0, 2) == 0, "Pending/cleared server powers reset scroll");
        UUID target = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        var terms = new MobBestowalUi.Terms(target, 30, "divine_speed", "  Companion  ", true);
        check(MobBestowalUi.matching(terms, new MobBestowalUi.Terms(target, 30, "divine_speed", "Companion", true)),
            "Names match server's stripped transaction name");
        for (var changed : List.of(
                new MobBestowalUi.Terms(new UUID(0, 1), 30, "divine_speed", "Companion", true),
                new MobBestowalUi.Terms(target, 31, "divine_speed", "Companion", true),
                new MobBestowalUi.Terms(target, 30, "", "Companion", true),
                new MobBestowalUi.Terms(target, 30, "divine_speed", "Other", true),
                new MobBestowalUi.Terms(target, 30, "divine_speed", "Companion", false)))
            check(!MobBestowalUi.matching(terms, changed), "Every changed term rejects old quote");
        check(!MobBestowalUi.matching(null, terms), "No absent quote accepted");
        var invalid = new MobBestowalUi.Terms(target, -1, "", "", false);
        check(!MobBestowalUi.matching(invalid, invalid), "Invalid/overflowed numeric input cannot match a quote");
        var review = new MobBestowalUi.QuoteReview();
        UUID oldNonce = new UUID(0, 10), freshNonce = new UUID(0, 11);
        review.receive(oldNonce, true);
        check(!review.matches(oldNonce, true), "Opening/receiving a snapshot never authorizes a quote");
        review.requested(oldNonce); review.receive(oldNonce, true);
        check(!review.matches(oldNonce, true), "Request must not revive previous nonce");
        review.receive(freshNonce, true);
        check(review.matches(freshNonce, true), "Matching new server quote is reviewable");
        check(!review.matches(freshNonce, false), "Reviewed nonce is insufficient if terms differ");
        review.changed(); review.receive(freshNonce, true);
        check(!review.matches(freshNonce, true), "Editing then reverting terms cannot revive a stale quote");
        review.requested(freshNonce); review.changed(); review.receive(new UUID(0, 12), true);
        check(!review.matches(new UUID(0, 12), true), "Delayed quote for pre-edit request cannot be confirmed");
        var lifetime = new MobBestowalUi.QuoteLifetime();
        check(lifetime.remaining(100) == 0, "No server TTL means no live quote");
        lifetime.receive(30_000, 500);
        check(lifetime.remaining(5_500) == 25_000, "Only monotonic elapsed time reduces server TTL");
        check(lifetime.remaining(40_000) == 0, "Quote expires without comparing server/client wall clocks");
        lifetime.receive(2_000, 80_000);
        check(lifetime.remaining(80_500) == 1_500, "Fresh snapshot updates remaining server lifetime");
        lifetime.receive(-1, 90_000);
        check(lifetime.remaining(90_000) == 0, "Missing/expired server lifetime cannot enable confirmation");
        var initial = new MobBestowalUi.InitialTarget(target);
        check(!initial.receive(null), "Periodic snapshot without profile echo cannot choose category");
        check(!initial.receive(oldNonce), "Cached or another screen's profile response cannot choose category");
        check(initial.receive(target), "Only exact profile request echo can choose initial category");
        check(!initial.receive(target), "Refresh with same profile echo never overrides later category");
        var chosen = new MobBestowalUi.InitialTarget(target);
        chosen.userChose();
        check(!chosen.receive(target), "User selection made before response always wins");
        check(!chosen.receive(target), "Ignored initial response cannot override on a later refresh");
        for (boolean supreme : new boolean[]{false, true}) {
            for (int filter = 0; filter < 3; filter++) {
                check(!MobBestowalUi.dominionRecord(filter, supreme, "Nearby", false), "Nearby players are not labelled subordinates");
                check(!MobBestowalUi.dominionRecord(filter, supreme, "Petitions", true), "Petitions remain distinct from subordinate lists");
                check(!MobBestowalUi.dominionRecord(filter, supreme, "MobFollowers", false), "Unbound mob gifts are not subordinates");
            }
        }
        check(MobBestowalUi.dominionRecord(0, false, "Followers", true), "All subordinates includes player followers");
        check(MobBestowalUi.dominionRecord(0, false, "MobFollowers", true), "All subordinates includes bonded mobs");
        check(!MobBestowalUi.dominionRecord(1, true, "MobFollowers", true), "Player filter excludes mobs");
        check(!MobBestowalUi.dominionRecord(2, true, "Followers", true), "Mob filter excludes player followers");
        for (String source : List.of("Petitions", "Nearby", "Followers"))
            check(MobBestowalUi.dominionRecord(3, true, source, true), "Sovereign player adjudication preserves existing access");
        check(!MobBestowalUi.dominionRecord(3, true, "MobFollowers", true), "Player adjudication never selects mob records");
        check(!MobBestowalUi.dominionRecord(3, false, "Petitions", true), "Loss of authority cannot retain petition/admin source");
        check(!MobBestowalUi.dominionRecord(3, false, "Nearby", false), "Ordinary viewers do not receive admin nearby selection");
        check(MobBestowalUi.mobTitle("", "", "Cow").equals("Cow"), "Unnamed mobs display localized species, never Username description keys");
        check(MobBestowalUi.mobTitle("", "Companion", "Cow").equals("Companion"), "Actual custom name takes precedence over species");
        check(MobBestowalUi.mobTitle("Divine Companion", "Companion", "Cow").equals("Divine Companion"), "Divine name takes precedence without renaming original identity");
        check(MobBestowalUi.mobStatus("active", false, "").equals("active"), "Live mobs retain active status");
        check(MobBestowalUi.mobStatus("unloaded", false, "").equals("unloaded"), "Normal unloaded mobs are not called removed");
        check(MobBestowalUi.mobStatus("deceased", false, "KILLED").equals("deceased"), "Death remains distinct from terminal removal");
        check(MobBestowalUi.mobStatus("deceased", true, "KILLED").equals("deceased"), "Production killed payload remains deceased despite terminal marker");
        check(MobBestowalUi.mobStatus("unloaded", true, "KILLED").equals("deceased"), "Killed removal reason remains death even with stale internal status");
        check(MobBestowalUi.mobStatus("unloaded", true, "DISCARDED").equals("removed (DISCARDED)"), "Discarded tombstones never appear merely unloaded");
        check(MobBestowalUi.mobStatus("active", true, "CONVERTED").equals("removed (CONVERTED)"), "Terminal reason overrides stale internal active status");
        check(MobBestowalUi.mobStatus("unloaded", true, "").equals("removed (reason unavailable)"), "Missing terminal reason remains honest");
        check(MobBestowalUi.mobStatus("", false, "").equals("unavailable"), "Missing lifecycle status is not invented");
        check(MobBestowalUi.eligibilityReason("Unsupported species", "Legacy").equals("Unsupported species"), "Server EligibilityReason takes precedence");
        check(MobBestowalUi.eligibilityReason("", "Legacy").equals("Legacy"), "Legacy target Reason remains supported");
        check(MobBestowalUi.eligibilityReason("", "").isEmpty(), "Eligible target has no invented restriction");
        check(MobBestowalUi.healthLabel("active", false).equals("Synced health"), "Active health remains a synced value");
        check(MobBestowalUi.healthLabel("unloaded", false).equals("Last observed health"), "Unloaded health is explicitly historical");
        check(MobBestowalUi.healthLabel("deceased", false).equals("Last observed health"), "Deceased health is explicitly historical");
        check(MobBestowalUi.healthLabel("active", true).equals("Last observed health"), "Terminal removal overrides stale active health label");
        check(!MobBestowalUi.bindingAllowed(true, true, false), "Server-incompatible binding disables quotes even with donor authority");
        check(MobBestowalUi.bindingAllowed(false, true, false), "Binding restriction does not block unbound gifts");
        check(MobBestowalUi.bindingAllowed(true, true, true), "Server-compatible binding remains available");
        check(MobBestowalUi.bindingAllowed(true, false, false), "Absent optional binding flag leaves final decision to server");
        System.out.println("MobBestowalUiTest: " + checks + " checks passed");
    }
    private static void inside(MobBestowalUi.Rect inner, MobBestowalUi.Rect outer) {
        check(outer.contains(inner.x(), inner.y()) &&
            outer.contains(inner.x() + inner.width() - 1, inner.y() + inner.height() - 1), "Content is clipped inside its panel");
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
