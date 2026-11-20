package dev.soulsmp;

import java.util.*;

public final class SoulRegistry {
    private static final List<Soul> ALL = new ArrayList<>();
    private static final Map<String, Soul> BY_ID = new HashMap<>();

    static {
        // ORDER MATTERS: it defines each soul's font glyph and must match make_pack.py
        reg(Souls.vitality()); reg(Souls.might()); reg(Souls.swiftness()); reg(Souls.inferno());
        reg(Souls.frost()); reg(Souls.tempest()); reg(Souls.gale()); reg(Souls.terra());
        reg(Souls.tide()); reg(Souls.shadow()); reg(Souls.venom()); reg(Souls.radiance());
    }
    private static void reg(Soul s) { ALL.add(s); BY_ID.put(s.id, s); }

    public static List<Soul> all() { return Collections.unmodifiableList(ALL); }
    public static Soul get(String id) { return id == null ? null : BY_ID.get(id.toLowerCase()); }
    public static Soul random() { return ALL.get(new Random().nextInt(ALL.size())); }
}
