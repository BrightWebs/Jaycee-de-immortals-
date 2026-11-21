package com.brightwebs.brightcore.api.util;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// crash guard. menus, the skill bar, using skills and tick stuff run in here,
// so if something breaks it gets logged once and skipped instead of crashing the game
public final class Safe {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private Safe() {}

    public static void run(String where, Runnable body) {
        try {
            body.run();
        } catch (Throwable t) {
            report(where, t);
        }
    }

    public static <T> T call(String where, Supplier<T> body, T fallback) {
        try {
            return body.get();
        } catch (Throwable t) {
            report(where, t);
            return fallback;
        }
    }

    private static void report(String where, Throwable t) {
        if (t instanceof VirtualMachineError e) throw e;  // out of memory and stuff like that can't be skipped
        if (REPORTED.add(where + "|" + t)) {
            LOG.error("[BrightCore] {} failed and was skipped so the game keeps running", where, t);
        }
    }
}
