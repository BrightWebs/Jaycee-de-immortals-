package com.brightwebs.jaycee.client;

import java.util.UUID;

/** Pure reference-space layout and transaction terms; no client gameplay rules. */
final class MobBestowalUi {
    record Rect(int x, int y, int width, int height) {
        boolean contains(double px, double py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }
    static final Rect LIST = new Rect(12, 85, 204, 307);
    static final Rect CENTER = new Rect(224, 85, 244, 307);
    static final Rect QUOTE = new Rect(476, 85, 212, 307);
    static final Rect PREVIEW = new Rect(232, 112, 92, 110);
    static final Rect INFO = new Rect(332, 112, 126, 110);
    static final Rect POWERS = new Rect(232, 342, 228, 46);
    static final Rect QUOTE_TEXT = new Rect(486, 110, 192, 214);
    static final Rect DESPAWN_WARNING = new Rect(486, 110, 192, 50);
    static final Rect UNBOUND_QUOTE_TEXT = new Rect(486, 166, 192, 158);
    static final String DESPAWN_TEXT = "Unbound gifts do not prevent vanilla natural despawn. Naming follows vanilla custom-name rules. Bind explicitly for subordinate persistence.";
    static final int ROWS = 7, POWER_ROWS = 2;
    static final Rect PLAYER_AMOUNT = new Rect(306, 158, 322, 16);
    static final Rect MOB_AMOUNT = new Rect(264, 279, 162, 16);
    static final Rect GRADE_BADGE = new Rect(124, 184, 90, 18);
    record Terms(UUID target, long amount, String ability, String name, boolean bind) {
        Terms {
            name = name.strip();
        }
    }
    static final class QuoteReview {
        private long revision, requestedRevision = -1;
        private UUID previous, reviewed;
        void changed() { revision++; reviewed = null; }
        void requested(UUID previousNonce) {
            changed(); requestedRevision = revision; previous = previousNonce;
        }
        void receive(UUID nonce, boolean termsMatch) {
            if (nonce != null && !nonce.equals(previous) && requestedRevision == revision && termsMatch) reviewed = nonce;
        }
        boolean matches(UUID nonce, boolean termsMatch) {
            return requestedRevision == revision && reviewed != null && reviewed.equals(nonce) && termsMatch;
        }
        UUID nonce() { return reviewed; }
    }
    static final class QuoteLifetime {
        private long remaining, receivedAt;
        void receive(long serverRemainingMillis, long monotonicMillis) {
            remaining = Math.max(0, serverRemainingMillis); receivedAt = monotonicMillis;
        }
        long remaining(long monotonicMillis) {
            return Math.max(0, remaining - Math.max(0, monotonicMillis - receivedAt));
        }
    }
    static final class InitialTarget {
        private final UUID request;
        private boolean consumed, chosen;
        InitialTarget(UUID request) { this.request = request; }
        UUID requestId() { return request; }
        boolean pending() { return !consumed && !chosen; }
        void userChose() { chosen = true; }
        boolean receive(UUID echoedProfileRequest) {
            if (consumed || !request.equals(echoedProfileRequest)) return false;
            consumed = true;
            return !chosen;
        }
    }
    static final class RecipientName {
        private UUID target;
        private boolean mob;
        private String suggestion = "";
        boolean select(UUID id, boolean isMob, String proposed) {
            if (id == null || id.equals(target) && mob == isMob) return false;
            target = id; mob = isMob;
            suggestion = proposed.length() > 48 ? proposed.substring(0, 48) : proposed;
            return true;
        }
        String suggestion() { return suggestion; }
        String networkName(String edited) { return MobBestowalUi.networkName(edited, suggestion); }
        void clear() { target = null; suggestion = ""; }
    }
    static Rect decrease(Rect amount) { return new Rect(amount.x() - 32, amount.y() - 4, 26, 22); }
    static Rect increase(Rect amount) { return new Rect(amount.x() + amount.width() + 8, amount.y() - 4, 26, 22); }
    static int offset(int current, int delta, int size, int visible) {
        return (int)Math.clamp((long)current + delta, 0, Math.max(0, size - visible));
    }
    static boolean matching(Terms expected, Terms quoted) {
        return expected != null && expected.amount() >= 0 && expected.equals(quoted);
    }
    static String mobTitle(String divineName, String customName, String localizedOriginal) {
        return !divineName.isEmpty() ? divineName : !customName.isEmpty() ? customName : localizedOriginal;
    }
    static String playerTitle(String divineName, String username) {
        return divineName.isEmpty() ? username : divineName;
    }
    static String networkName(String edited, String suggestion) {
        String value = edited.strip();
        return value.equals(suggestion.strip()) ? "" : value;
    }
    static long adjustAmount(long value, long delta) {
        value = Math.max(0, value);
        if (delta > 0) return value > Long.MAX_VALUE - delta ? Long.MAX_VALUE : value + delta;
        return delta < -value ? 0 : value + delta;
    }
    static double progress(long current, long required) {
        return required <= 0 ? 0 : Math.clamp(current / (double)required, 0, 1);
    }
    static String standing(boolean supreme, String rank) {
        return supreme ? "Authority: Supreme Existence" : "Rank: " + (rank.isBlank() ? "Unavailable" : rank);
    }
    static int gradeColor(String grade) {
        return switch (grade) {
            case "F" -> 0xFF91A2B3;
            case "E" -> 0xFF91B5A0;
            case "D" -> 0xFF8ABCC5;
            case "C" -> 0xFF91B6D8;
            case "B" -> 0xFFB19CCD;
            case "A" -> 0xFFC4B18B;
            case "S" -> 0xFFD3BD86;
            case "SS" -> 0xFFCCA0AD;
            case "SSS" -> 0xFFD3C7E4;
            default -> 0xFF788DAB;
        };
    }
    static String metricHint(String metric) {
        return switch (metric) {
            case "Level" -> "Divine Level is derived from lifetime earned Essence, not Minecraft XP. The bar shows server-synced progress toward the next Essence threshold.";
            case "Grade" -> "Grade is the server-assigned Essence progression band. It is separate from divine rank and Supreme Existence authority.";
            case "Rank" -> "Divine rank governs divine standing and privileges. It is separate from Level, Grade, and the unique Supreme Existence authority.";
            case "Essence" -> "Essence is the transferable, spendable reserve. Lifetime earned Essence drives Level progress; spending the reserve does not remove earned progress.";
            case "EP" -> "EP is permanent existence power. It is separate from Essence and Mana and is never spent by bestowal.";
            case "Mana" -> "Mana fuels casting and upkeep. It is separate from transferable Essence, earned progression, and permanent EP.";
            default -> "";
        };
    }
    static String mobStatus(String status, boolean terminalRemoval, String removalReason) {
        if (status.equals("deceased") || terminalRemoval && removalReason.strip().equals("KILLED")) return "deceased";
        if (terminalRemoval)
            return "removed (" + (removalReason.isBlank() ? "reason unavailable" : removalReason.strip()) + ")";
        return status.isBlank() ? "unavailable" : status;
    }
    static String eligibilityReason(String eligibilityReason, String legacyReason) {
        return eligibilityReason.isBlank() ? legacyReason : eligibilityReason;
    }
    static String healthLabel(String status, boolean terminalRemoval) {
        return status.equals("active") && !terminalRemoval ? "Synced health" : "Last observed health";
    }
    static boolean bindingAllowed(boolean bind, boolean compatibilitySupplied, boolean bindingCompatible) {
        return !bind || !compatibilitySupplied || bindingCompatible;
    }
    static boolean dominionRecord(int filter, boolean supreme, String source, boolean bonded) {
        if (filter == 3 && !supreme) filter = 0;
        return switch (filter) {
            case 0 -> source.equals("Followers") || source.equals("MobFollowers") && bonded;
            case 1 -> source.equals("Followers");
            case 2 -> source.equals("MobFollowers") && bonded;
            case 3 -> supreme && (source.equals("Petitions") || source.equals("Nearby") || source.equals("Followers"));
            default -> false;
        };
    }
    private MobBestowalUi() {}
}
