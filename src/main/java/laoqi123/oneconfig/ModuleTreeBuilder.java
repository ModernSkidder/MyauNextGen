package laoqi123.oneconfig;

import laoqi123.Myau;
import laoqi123.module.Module;
import laoqi123.module.modules.*;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.Visualizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the OneConfig {@link Tree} that replaces the old hand-drawn ClickGUI.
 *
 * <p>Structure matters here. OneConfig's settings screen ({@code buildCategories})
 * only inspects the <em>direct</em> children of the config tree: a child
 * {@link Property} becomes one row, and a child {@link Tree} becomes a single
 * collapsible accordion. It does not recurse. Grouping into the tab strip comes
 * from each child's {@code category} and {@code subcategory} metadata, not from
 * nesting.
 *
 * <p>So the tree is flat: every setting is a direct child of the root, tagged with
 * its module's category (Combat, Movement, ...) for the tab strip and the module
 * name as its subcategory heading. This reproduces the old five-tab layout, and it
 * also guarantees one setting per row: root-level properties are laid out
 * full-width, whereas settings nested in a child tree get packed two-per-row by the
 * accordion grid.
 *
 * <p>Module keybinds are deliberately not exposed as {@code KeybindVisualizer}
 * properties. OneConfig mirrors every such property into Minecraft's Controls menu
 * and its own Keybinds page, which for ~90 modules produces an unusable wall of
 * entries. Binds stay on Myau's own {@code .bind} command.
 */
public final class ModuleTreeBuilder {

    private ModuleTreeBuilder() {
    }

    private static final Map<String, Class<? extends Module>[]> CATEGORIES = buildCategories();

    @SafeVarargs
    private static <T> T[] group(T... items) {
        return items;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Class<? extends Module>[]> buildCategories() {
        Map<String, Class<? extends Module>[]> map = new LinkedHashMap<>();
        map.put("Combat", group(
                AimAssist.class, AutoClicker.class, AutoProjectiles.class, KillAura.class, Wtap.class,
                Velocity.class, RiseVelocity.class, Reach.class, TargetStrafe.class, NoHitDelay.class,
                AntiFireball.class, KnockbackDelay.class, LagRange.class, HitBox.class, MoreKB.class,
                HitSelect.class, BackTrack.class, BlockHit.class, OldHitting.class, Teams.class
        ));
        map.put("Movement", group(
                AntiAFK.class, Fly.class, Freeze.class, Speed.class, LongJump.class, Sprint.class,
                Jesus.class, Blink.class, NoFall.class, NoSlow.class, KeepSprint.class, Eagle.class,
                NoJumpDelay.class, AntiVoid.class, Stasis.class, Stuck.class
        ));
        map.put("Render", group(
                Animations.class, ESP.class, Chams.class, FullBright.class, Tracers.class, NameTags.class,
                Xray.class, TargetHUD.class, TargetHud2.class, Indicators.class, BedESP.class, ItemESP.class,
                PotionEffects.class, ViewClip.class, NoHurtCam.class, HUD.class,
                ChestESP.class, Trajectories.class, Notifications.class, WaterMark.class, TabGui.class,
                BedPlates.class
        ));
        map.put("Player", group(
                AutoHeal.class, AutoMLG.class, AutoTool.class, AutoSwap.class, ChestAura.class,
                ChestStealer.class, FakeLag.class, InvManager.class, InvWalk.class, Scaffold.class,
                Scaffold2.class, Telly.class, AutoBlockIn.class, SpeedMine.class, FastPlace.class,
                GhostHand.class, MCF.class, AntiDebuff.class, Timer.class
        ));
        map.put("Client", group(
                Spammer.class, BedNuker.class, BedTracker.class, LightningTracker.class, NoRotate.class,
                NickHider.class, AntiObbyTrap.class, AntiObfuscate.class, AutoAnduril.class,
                InventoryClicker.class, ClientSpoofer.class, FlagDetector.class, AntiStaff.class
        ));
        return map;
    }

    /**
     * Assemble the config tree. Must be called after {@code new Myau()} so the
     * module and property managers are populated.
     */
    public static Tree build() {
        Tree root = new Tree(MyauOneConfig.CONFIG_ID, "Myau NextGen", null, null);

        for (Map.Entry<String, Class<? extends Module>[]> entry : CATEGORIES.entrySet()) {
            String category = entry.getKey();
            for (Module module : resolve(entry.getValue())) {
                root.put(buildModule(module, category));
            }
        }
        return root;
    }

    /** Resolve classes to live module instances, sorted by name like the old UI did. */
    private static List<Module> resolve(Class<? extends Module>[] classes) {
        List<Module> out = new ArrayList<>(classes.length);
        for (Class<? extends Module> clazz : classes) {
            Module module = Myau.moduleManager.getModule(clazz);
            if (module != null) {
                out.add(module);
            }
        }
        out.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        return out;
    }

    /**
     * One module becomes one collapsible row under its category tab. The {@code enabled}
     * flag is the accordion's head toggle, which OneConfig detects as the boolean
     * property carrying no explicit {@code visualizer} metadata.
     */
    private static org.polyfrost.oneconfig.api.config.v1.Node buildModule(Module module, String category) {
        String id = PropertyBridge.sanitizeId(module.getName());

        List<Property<?>> settings = new ArrayList<>();
        List<laoqi123.property.Property<?>> sources =
                Myau.propertyManager.properties.get(module.getClass());
        if (sources != null) {
            for (laoqi123.property.Property<?> source : sources) {
                Property<?> converted = PropertyBridge.convert(source, "");
                if (converted == null) {
                    continue;
                }
                // Myau's visibleChecker drives conditional display in the old UI;
                // reuse it so dependent settings keep hiding themselves.
                converted.addDisplayCondition(() -> source.isVisible()
                        ? Property.Display.SHOWN
                        : Property.Display.HIDDEN);
                settings.add(converted);
            }
        }

        // A module with no settings is just one switch row. An accordion requires a
        // non-empty body, so wrapping it in a tree would make it vanish.
        if (settings.isEmpty()) {
            Property<Boolean> toggle = Properties.functional(
                    module::isEnabled, module::setEnabled,
                    id, module.getName(), null, Boolean.class);
            toggle.addMetadata("visualizer", new Visualizer.SwitchVisualizer());
            toggle.addMetadata("category", category);
            return toggle;
        }

        Tree tree = new Tree(id, module.getName(), null, null);
        tree.addMetadata("category", category);
        // Collapsed by default so a tab opens as a plain list of modules; without this
        // every module whose head toggle is on would start expanded.
        tree.addMetadata("collapsed", true);
        tree.put(Properties.functional(
                module::isEnabled, module::setEnabled,
                "enabled", "Enabled", null, Boolean.class));
        for (Property<?> setting : settings) {
            tree.put(setting);
        }
        return tree;
    }

}
