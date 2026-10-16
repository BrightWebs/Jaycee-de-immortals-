package com.brightwebs.jaycee.client;

public final class SkillMagicLayoutTest {
    private static int checks;
    public static void main(String[] args) {
        check(SapphireLayout.statusScrollTarget(390,174)==1,"Trait wheel targets Trait only");
        check(SapphireLayout.statusScrollTarget(390,270)==2,"Affinity wheel targets affinities only");
        check(SapphireLayout.statusScrollTarget(390,234)==0,"Gap between cards consumes no card scrolling");
        check(SapphireLayout.statusScrollTarget(560,300)==0,"Equip Page is outside card scrolling");
        check(SapphireLayout.scrollOffset(0,-12,400,46)==0,"Scrolling clamps at top");
        check(SapphireLayout.scrollOffset(400,12,400,46)==354,"Long trait list reaches final text");
        check(SapphireLayout.scrollOffset(100,12,10,46)==0,"Removed entries reset scroll bounds");
        check(SapphireLayout.scrollOffset(Integer.MAX_VALUE,12,400,46)==354,"Scroll arithmetic cannot overflow");
        var ids=java.util.List.of("zeus","poseidon","hades","athena","ares","apollo","artemis","hermes",
            "hephaestus","aphrodite","demeter","dionysus","hera","hestia","hecate","eris","nemesis","kronos",
            "gaia","ouranos","nyx","chaos");
        check(PatronTheme.retained(ids).size()==22,"Every supported patron has a distinct identity");
        check(PatronTheme.retained(java.util.List.of("zeus","zeus","unknown")).size()==1,"Deduplicate and ignore unsupported lineages");
        check(PatronTheme.palette(java.util.List.of()).equals(PatronTheme.NEUTRAL),"Unblessed uses neutral palette");
        check(PatronTheme.palette(PatronTheme.retained(java.util.List.of("zeus"))).id().equals("zeus"),"Single patron uses own palette");
        check(PatronTheme.palette(PatronTheme.retained(ids)).equals(PatronTheme.NEUTRAL),"Multiple patrons use neutral palette");
        check(PatronTheme.relevant(PatronTheme.find("zeus"),"jaycee:storm","Storm"),"Existing patron domain can be prioritized");
        check(!PatronTheme.relevant(PatronTheme.find("apollo"),"jaycee:flight","Flight"),"Domain priority must not confuse flight with light");
        for(int count=2;count<=22;count++){
            int outer=Math.min(14,count);
            for(int i=0;i<count;i++)for(double ticks:new double[]{0,160,320,640}){
                boolean inner=i>=outer;
                int ringCount=inner?count-outer:outer,index=inner?i-outer:i,radius=inner?32:62;
                int size=count<=4?32:count<=8?25:20;
                double angle=PatronTheme.angle(index,ringCount,ticks,false);
                double x=300+Math.cos(angle)*radius,y=194+Math.sin(angle)*radius;
                check(x-size/2.0>220&&x+size/2.0<382,"Patron orbit stays within original central width");
                check(y+size/2.0<250||x-size/2.0>226,"Patron orbit never intersects Stats");
                check(y-size/2.0>110&&y+size/2.0<278,"Patron orbit stays in original centerpiece");
                check(PatronTheme.angle(index,ringCount,ticks,true)==PatronTheme.angle(index,ringCount,0,true),"Reduced motion stays static");
            }
        }
        check(!SapphireLayout.ordinaryMenu(true, false), "Jaycee normally sees approved Sapphire");
        check(SapphireLayout.ordinaryMenu(true, true), "Jaycee can preview ordinary navigation without losing authority");
        check(SapphireLayout.ordinaryMenu(false, false), "Ordinary players keep Divine Ascension");
        check(SapphireLayout.ordinaryMenu(false, true), "Preview never grants sovereign navigation");
        for (boolean ordinary : new boolean[]{false, true}) {
            int total = ordinary ? 6 : 5;
            for (int tab = 0; tab < total; tab++) {
                var button = SapphireLayout.tabButton(tab, ordinary);
                var label = SapphireLayout.tabLabel(tab, ordinary);
                check(button.x() >= 10 && button.x() + button.width() <= 536, "Navigation stays outside Equip Page");
                check(button.contains(label.x(), label.y()) &&
                    button.contains(label.x() + label.width() - 1, label.y() + label.height() - 1), "Labels share tab bounds");
                if (tab + 1 < total) check(button.x() + button.width() <
                    SapphireLayout.tabButton(tab + 1, ordinary).x(), "Tab hitboxes do not overlap");
                if (!ordinary) check(label.equals(SapphireLayout.tabLabel(tab)), "Approved Sapphire label bounds unchanged");
            }
        }
        var left = SkillMagicLayout.CATEGORIES;
        var middle = SkillMagicLayout.LIST;
        var right = SkillMagicLayout.DETAILS;
        for (var r : new SkillMagicLayout.Rect[]{left, middle, right}) {
            check(r.x() >= 14 && r.x() + r.width() <= 524, "Inside approved inner width");
            check(r.y() == 85 && r.y() + r.height() == 400, "Inside approved inner height");
            check(r.x() + r.width() < 544, "Never touch Equip Page");
            check(r.contains(r.x(), r.y()), "Inclusive leading edge");
            check(!r.contains(r.x() + r.width(), r.y()), "Exclusive trailing edge");
        }
        check(left.x() + left.width() <= middle.x(), "Left and middle separate");
        check(middle.x() + middle.width() <= right.x(), "Middle and details separate");
        var rows = SkillMagicLayout.ROWS;
        check(rows.height() == SkillMagicLayout.VISIBLE_ROWS * SkillMagicLayout.ROW_HEIGHT, "Exactly complete list rows");
        check(middle.contains(rows.x(), rows.y()) &&
            middle.contains(rows.x() + rows.width() - 1, rows.y() + rows.height() - 1), "Rows inside middle");
        check(right.contains(SkillMagicLayout.TEXT.x(), SkillMagicLayout.TEXT.y()) &&
            right.contains(SkillMagicLayout.TEXT.x() + SkillMagicLayout.TEXT.width() - 1,
                SkillMagicLayout.TEXT.y() + SkillMagicLayout.TEXT.height() - 1), "Detail scroll region inside panel");
        check(Math.abs(left.width() / 506.0 - .22) < .01, "22 percent categories");
        check(Math.abs(middle.width() / 506.0 - .42) < .01, "42 percent list");
        check(Math.abs(right.width() / 506.0 - .36) < .01, "36 percent details");
        int[][] windows = {{1920, 1080}, {1438, 874}, {1280, 720}, {854, 480}, {640, 360}};
        for (int[] window : windows) for (int guiScale = 1; guiScale <= 4; guiScale++) {
            int width = window[0] / guiScale, height = window[1] / guiScale;
            var layout = new SapphireLayout(width, height);
            for (var r : new SkillMagicLayout.Rect[]{left, middle, right, rows, SkillMagicLayout.TEXT}) {
                double centerX = r.x() + r.width() / 2.0, centerY = r.y() + r.height() / 2.0;
                double sx = layout.left + centerX * layout.scale, sy = layout.top + centerY * layout.scale;
                check(r.contains(layout.localX(sx), layout.localY(sy)), "Transformed pointer stays inside widget");
                check(layout.screenX(r.x()) >= 0 && layout.screenX(r.x() + r.width()) <= width,
                    "Scaled region stays inside viewport horizontally");
                check(layout.screenY(r.y()) >= 0 && layout.screenY(r.y() + r.height()) <= height,
                    "Scaled region stays inside viewport vertically");
            }
        }
        System.out.println("SkillMagicLayoutTest: " + checks + " checks passed");
    }
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
}
