package laoqi123.util.clicking;

import laoqi123.mixin.MinecraftClientAccessor;
import laoqi123.value.properties.BooleanValue;
import laoqi123.value.properties.IntValue;
import laoqi123.value.properties.ModeValue;
import laoqi123.util.config.Configurable;
import net.minecraft.client.MinecraftClient;

import java.util.Arrays;
import java.util.Random;
import java.util.function.BooleanSupplier;

public class Clicker extends Configurable {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final Random RNG = new Random();
    private static final int DEFAULT_CYCLE_LENGTH = 20;
    private static long lastClickTime = 0L;

    public final ModeValue technique;
    public final BooleanValue attackCooldown;
    public final ItemCooldown itemCooldown;

    private final IntValue minCps;
    private final IntValue maxCps;
    private final RollingClickArray clickArray = new RollingClickArray(DEFAULT_CYCLE_LENGTH, 2);

    private Integer clickAmount = null;

    public Clicker(String name, IntValue minCps, IntValue maxCps) {
        super(name);
        this.minCps = minCps;
        this.maxCps = maxCps;
        this.technique = this.register(new ModeValue("Technique", 0, new String[]{
                "Stabilized", "Efficient", "Spamming", "DoubleClick", "Drag", "Butterfly", "NormalDistribution"
        }));
        this.attackCooldown = this.register(new BooleanValue("AttackCooldown", true));
        this.itemCooldown = new ItemCooldown();
        this.tree(this.itemCooldown);
        this.fill();
    }

    public boolean isClickTick() {
        return this.willClickAt(0);
    }

    public boolean willClickAt(int tick) {
        return this.getClickAmount(tick) > 0;
    }

    public int ticksUntilClick() {
        for (int i = 0; i < this.clickArray.iterations; i++) {
            if (this.willClickAt(i)) {
                return i;
            }
        }
        return this.clickArray.iterations;
    }

    public int getClickAmount(int tick) {
        if (this.isEnforcedClick(tick)) {
            return 1;
        }
        return this.clickArray.get(tick);
    }

    public Integer getClickAmount() {
        return this.clickAmount;
    }

    private boolean isEnforcedClick(int tick) {
        boolean hasCooldown = mc.player.getAttackCooldownProgress(0.0F) < 1.0F;
        if (hasCooldown && this.itemCooldown.isCooldownPassed(tick)) {
            return true;
        }
        return System.currentTimeMillis() - lastClickTime + (long) tick * 50L >= 1000L;
    }

    public void tick() {
        this.clickAmount = null;
        if (this.clickArray.advance()) {
            int[] cycleArray = new int[DEFAULT_CYCLE_LENGTH];
            this.getPattern().fill(cycleArray, this.minCps.getValue(), this.maxCps.getValue(), RNG);
            this.clickArray.push(cycleArray);
        }
    }

    public boolean click(BooleanSupplier block) {
        int clicks = this.getClickAmount(0);
        int clickAmount = 0;
        for (int i = 0; i < clicks; i++) {
            if (this.attackCooldown.getValue() && ((MinecraftClientAccessor) mc).getAttackCooldown() > 0) {
                continue;
            }
            if (this.itemCooldown.isCooldownPassed(0) && block.getAsBoolean()) {
                clickAmount++;
                this.itemCooldown.newCooldown();
                lastClickTime = System.currentTimeMillis();
            }
        }
        this.clickAmount = clickAmount;
        return clickAmount > 0;
    }

    public void reset() {
        this.clickArray.clear();
        this.fill();
    }

    private void fill() {
        this.clickArray.clear();
        int[] cycleArray = new int[DEFAULT_CYCLE_LENGTH];
        for (int i = 0; i < this.clickArray.iterations; i++) {
            Arrays.fill(cycleArray, 0);
            this.getPattern().fill(cycleArray, this.minCps.getValue(), this.maxCps.getValue(), RNG);
            this.clickArray.push(cycleArray);
            this.clickArray.advance(DEFAULT_CYCLE_LENGTH);
        }
    }

    private ClickPattern getPattern() {
        switch (this.technique.getValue()) {
            case 1:
                return ClickPatterns.EFFICIENT;
            case 2:
                return ClickPatterns.SPAMMING;
            case 3:
                return ClickPatterns.DOUBLE_CLICK;
            case 4:
                return ClickPatterns.DRAG;
            case 5:
                return ClickPatterns.BUTTERFLY;
            case 6:
                return ClickPatterns.NORMAL_DISTRIBUTION;
            default:
                return ClickPatterns.STABILIZED;
        }
    }
}
