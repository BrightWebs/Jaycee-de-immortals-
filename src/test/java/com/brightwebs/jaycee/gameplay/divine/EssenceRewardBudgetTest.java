package com.brightwebs.jaycee.gameplay.divine;

public final class EssenceRewardBudgetTest {
    private static int checks;
    private static void check(boolean value) {
        checks++;
        if (!value) throw new AssertionError("Reward budget check " + checks);
    }
    public static void main(String[] args) {
        var state = new EssenceRewardBudget.State(0, 0);
        for (int i = 0; i < 10; i++) {
            var result = EssenceRewardBudget.claim(state, i, 10, 100, 1200);
            check(result.amount() == 10);
            state = result.state();
        }
        check(EssenceRewardBudget.claim(state, 1000, 10, 100, 1200).amount() == 0);
        check(EssenceRewardBudget.claim(state, 1199, 10, 100, 1200).amount() == 0);
        var next = EssenceRewardBudget.claim(state, 1200, 10, 100, 1200);
        check(next.amount() == 10 && next.state().used() == 10);
        check(EssenceRewardBudget.claim(new EssenceRewardBudget.State(5000, 100), 0, 10, 100, 1200).amount() == 0);
        check(EssenceRewardBudget.claim(new EssenceRewardBudget.State(0, 95), 50, 10, 100, 1200).amount() == 5);
        check(EssenceRewardBudget.claim(state, 100, 10, 0, 1200).amount() == 0);
        check(EssenceRewardBudget.claim(state, 100, 0, 100, 1200).amount() == 0);
        check(EssenceRewardBudget.claim(new EssenceRewardBudget.State(0, Long.MAX_VALUE), 1, 10, 100, 1200).amount() == 0);
        check(EssenceRewardBudget.claim(new EssenceRewardBudget.State(0, 0), Long.MAX_VALUE,
            Long.MAX_VALUE, Long.MAX_VALUE, 1200).amount() == Long.MAX_VALUE);
        for (long invalid : new long[] {-1, Long.MIN_VALUE}) {
            try { EssenceRewardBudget.claim(state, 0, invalid, 100, 1200); throw new AssertionError(); }
            catch (IllegalArgumentException expected) { checks++; }
        }
        System.out.println(checks + " Essence reward budget checks passed");
    }
}
