package laoqi123.module;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.event.types.EventType;
import laoqi123.events.KeyEvent;
import laoqi123.events.TickEvent;
import laoqi123.module.modules.combat.*;
import laoqi123.module.modules.misc.*;
import laoqi123.module.modules.movement.*;
import laoqi123.module.modules.player.*;
import laoqi123.module.modules.render.*;
import laoqi123.module.modules.render.GuiModule;
import laoqi123.module.modules.render.HUD;
import laoqi123.module.modules.render.Notifications;
import laoqi123.util.ChatUtil;
import laoqi123.util.SoundUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public class ModuleManager {
    private boolean sound = false;
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();
    public final LinkedHashMap<Category, ArrayList<Module>> categories = new LinkedHashMap<>();

    public ModuleManager() {
        registerModules();
        sortCategories();
    }

    private void registerModules() {
        
        register(new AimAssist());
        register(new Animations());
        register(new OldHitting());
        register(new AntiAFK());
        register(new AntiDebuff());
        register(new AntiFireball());
        register(new AntiStaff());
        register(new AntiObbyTrap());
        register(new AntiObfuscate());
        register(new Notifications());
        register(new AntiVoid());
        register(new AutoClicker());
        register(new AutoAnduril());
        register(new AutoHeal());
        register(new AutoMLG());
        register(new AutoTool());
        register(new AutoSwap());
        register(new BedNuker());
        register(new Timer());
        register(new BedESP());
        register(new BedTracker());
        register(new BackTrack());
        register(new Blink());
        register(new Chams());
        register(new ChestESP());
        register(new ChestStealer());
        register(new Eagle());
        register(new Telly());
        register(new ESP());
        register(new FastPlace());
        register(new Fly());
        register(new Freeze());
        register(new FakeLag());
        register(new FullBright());
        register(new GhostHand());
        register(new GuiModule());
        register(new HitSelect());
        register(new HUD());
        register(new BlockHit());
        register(new MoreKB());
        register(new Indicators());
        register(new InventoryClicker());
        register(new KnockbackDelay());
        register(new PotionEffects());
        register(new BedPlates());
        register(new InvManager());
        register(new InvWalk());
        register(new ItemESP());
        register(new Jesus());
        register(new KeepSprint());
        register(new TabGui());
        register(new AutoProjectiles());
        register(new HitBox());
        register(new KillAura());
        register(new LagRange());
        register(new LightningTracker());
        register(new LongJump());
        register(new MCF());
        register(new NameTags());
        register(new NickHider());
        register(new NoFall());
        register(new NoHitDelay());
        register(new NoHurtCam());
        register(new NoJumpDelay());
        register(new NoRotate());
        register(new NoSlow());
        register(new Reach());
        register(new RiseVelocity());
        register(new Scaffold());
        register(new Scaffold2());
        register(new AutoBlockIn());
        register(new ClientSpoofer());
        register(new FlagDetector());
        register(new Stasis());
        register(new ChestAura());
        register(new Spammer());
        register(new Speed());
        register(new SpeedMine());
        register(new Sprint());
        register(new Stuck());
        register(new TargetHUD());
        register(new TargetHud2());
        register(new TargetStrafe());
        register(new Teams());
        register(new Tracers());
        register(new Trajectories());
        register(new Velocity());
        register(new ViewClip());
        register(new Wtap());
        register(new WaterMark());
        register(new Xray());
    }

    private void register(Module module) {
        this.modules.put(module.getClass(), module);
        Category category = module.getCategory();
        this.categories.computeIfAbsent(category, c -> new ArrayList<>()).add(module);
    }

    public void sortCategories() {
        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());
        for (ArrayList<Module> list : this.categories.values()) {
            list.sort(comparator);
        }
    }

    public List<Module> getModulesInCategory(Category category) {
        return this.categories.getOrDefault(category, new ArrayList<>());
    }

    public Module getModule(String string) {
        return this.modules.values().stream().filter(mD -> mD.getName().equalsIgnoreCase(string)).findFirst().orElse(null);
    }

    public Module getModule(Class<?> clazz){
        return this.modules.get(clazz);
    }

    public void playSound() {
        this.sound = true;
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        for (Module module : this.modules.values()) {
            if (module.getKey() != event.getKey()) {
                continue;
            }
            boolean wasEnabled = module.isEnabled();
            module.toggle();
            boolean nowEnabled = module.isEnabled();

            boolean shouldNotify = wasEnabled != nowEnabled;
            HUD hud = (HUD) this.modules.get(HUD.class);
            if (hud != null && shouldNotify) {
                shouldNotify = hud.toggleAlerts.getValue();
            }
            if(module instanceof GuiModule){
                shouldNotify = false;
            }
            if (shouldNotify) {
                String status = module.isEnabled() ? "&a&lON" : "&c&lOFF";
                String message = String.format("%s%s: %s&r", Myau.clientName, module.getName(), status);
                ChatUtil.sendFormatted(message);
            }

            if (wasEnabled != nowEnabled && !(module instanceof GuiModule)) {
                Notifications.postToggle(module, nowEnabled);
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.sound) {
                this.sound = false;
                SoundUtil.playSound("random.click");
            }
        }
    }
}
