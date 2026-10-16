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
        void userChose() { chosen = true; }
        boolean receive(UUID echoedProfileRequest) {
            if (consumed || !request.equals(echoedProfileRequest)) return false;
            consumed = true;
            return !chosen;
        }
    }
    static int offset(int current, int delta, int size, int visible) {
        return (int)Math.clamp((long)current + delta, 0, Math.max(0, size - visible));
    }
    static boolean matching(Terms expected, Terms quoted) {
        return expected != null && expected.amount() >= 0 && expected.equals(quoted);
    }
    static String mobTitle(String divineName, String customName, String localizedOriginal) {
        return !divineName.isEmpty() ? divineName : !customName.isEmpty() ? customName : localizedOriginal;
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
