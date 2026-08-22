package laoqi123.oneconfig;

import com.mojang.logging.LogUtils;
import laoqi123.Myau;
import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.backend.Backend;
import org.slf4j.Logger;

/**
 * Entry point that publishes Myau's modules into the OneConfig UI.
 *
 * <p>Values are not persisted by OneConfig: the tree is built from delegating
 * properties that read and write Myau's own {@code Property} objects, and saving
 * stays with {@code laoqi123.config.Config}. Registering the tree with
 * {@link ConfigManager} is therefore only about making it visible and editable.
 */
public final class MyauOneConfig {

    /** Also used as the mod-card id inside OneConfig's UI. */
    public static final String CONFIG_ID = "myaunextgen";

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized;

    private MyauOneConfig() {
    }

    /**
     * Build and register the config tree. Safe to call once, after {@code new Myau()}.
     * Any failure is logged and swallowed so a UI problem can never stop the client
     * from loading.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Tree tree = ModuleTreeBuilder.build();
            tree.addMetadata("category", Config.Category.COMBAT);
            tree.addMetadata("mod_card_title", "Myau NextGen");
            tree.addMetadata("mod_card_icon_path", "/assets/myaunextgen/icon.png");

            // Values live in Myau's own store (config/Myau/<profile>.json) and every
            // property here only delegates to it. Marking the tree UI-only keeps
            // OneConfig from creating a second, competing copy under config/ that
            // would overwrite Myau's values on the next launch.
            tree.addMetadata(Backend.UI_ONLY_METADATA, true);

            ConfigManager.active().register(tree);
            LOGGER.info("[Myau] Registered {} modules with OneConfig", tree.map.size());
            if (Boolean.getBoolean("myau.oneconfig.dumpTree")) {
                dumpTree(tree);
            }
            if (!Boolean.getBoolean("myau.oneconfig.noHuds")) {
                registerHuds();
            }
        } catch (Throwable t) {
            LOGGER.error("[Myau] Failed to register the OneConfig UI", t);
            // The logger can be swallowed depending on when this runs, and a silent
            // failure here means an empty Mods page with no explanation.
            System.err.println("[Myau] Failed to register the OneConfig UI: " + t);
            t.printStackTrace();
        }
    }

    /**
     * Hand the placement of Myau's overlays to OneConfig's HUD Designer.
     *
     * <p>Each wrapper keeps the module drawing itself and only maps the Designer's
     * absolute coordinates onto the position properties the module already owns, so
     * positions stay in Myau's own config file.
     *
     * <p>Registered individually: one overlay whose module is missing must not stop the
     * rest from appearing in the editor.
     */
    private static void registerHuds() {
        int count = 0;

        // Compose HUDs render inside OneConfig's own Skia scene rather than through
        // DrawContext, which is what makes the real frosted background possible. The
        // legacy wrappers for these overlays are deliberately not registered so the two
        // paths cannot both draw the same panel.
        if (laoqi123.oneconfig.huds.WaterMarkComposeHud.isActive()
                || laoqi123.oneconfig.huds.WaterMarkComposeHud.register()) {
            count++;
        }
        if (laoqi123.oneconfig.huds.PotionEffectsComposeHud.isActive()
                || laoqi123.oneconfig.huds.PotionEffectsComposeHud.register()) {
            count++;
        }
        if (laoqi123.oneconfig.huds.ArrayListComposeHud.isActive()
                || laoqi123.oneconfig.huds.ArrayListComposeHud.register()) {
            count++;
        }
        if (laoqi123.oneconfig.huds.NotificationsComposeHud.isActive()
                || laoqi123.oneconfig.huds.NotificationsComposeHud.register()) {
            count++;
        }
        if (laoqi123.oneconfig.huds.TargetHUDComposeHud.isActive()
                || laoqi123.oneconfig.huds.TargetHUDComposeHud.register()) {
            count++;
        }
        if (laoqi123.oneconfig.huds.TabGuiComposeHud.isActive()
                || laoqi123.oneconfig.huds.TabGuiComposeHud.register()) {
            count++;
        }
        LOGGER.info("[Myau] Registered {} HUDs with the OneConfig HUD Designer", count);
    }

    /**
     * Diagnostic dump enabled with {@code -Dmyau.oneconfig.dumpTree=true}. Reports
     * the structure the way OneConfig's {@code buildCategories} reads it, so a
     * regression that would render "No settings available" is visible in the log.
     */
    private static void dumpTree(Tree tree) {
        // Rows are grouped by the category tab, then by the module heading.
        java.util.Map<String, java.util.Set<String>> modules = new java.util.LinkedHashMap<>();
        java.util.Map<String, Integer> rows = new java.util.LinkedHashMap<>();
        int untagged = 0;
        int nested = 0;

        for (org.polyfrost.oneconfig.api.config.v1.Node node : tree.map.values()) {
            if (node instanceof Tree) {
                // A nested tree becomes a two-per-row accordion, which is not wanted here.
                nested++;
                continue;
            }
            String category = node.getMetadata("category");
            String subcategory = node.getMetadata("subcategory");
            if (category == null) {
                untagged++;
                category = "<none>";
            }
            rows.merge(category, 1, Integer::sum);
            modules.computeIfAbsent(category, k -> new java.util.LinkedHashSet<>())
                    .add(subcategory == null ? "<none>" : subcategory);
        }

        LOGGER.info("[Myau] tree dump: {} rows total, {} nested trees, {} untagged",
                tree.map.size(), nested, untagged);
        rows.forEach((category, count) ->
                LOGGER.info("[Myau]   tab '{}': {} rows across {} modules",
                        category, count, modules.get(category).size()));
    }
}
