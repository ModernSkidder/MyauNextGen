package laoqi123.module.modules;

import laoqi123.Myau;
import laoqi123.event.EventTarget;
import laoqi123.events.Render2DEvent;
import laoqi123.module.Module;
import laoqi123.oneconfig.Glass;
import laoqi123.util.RenderUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.concurrent.ConcurrentLinkedDeque;

public class Notifications extends Module {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final ConcurrentLinkedDeque<NotificationEntry> queue = new ConcurrentLinkedDeque<>();
    private static final float ANIMATION_SPEED = 0.15f;

    /** Toggle-state accents, matching PolyColor's GREEN and RED. */
    private static final int ENABLED_COLOR = 0xFF44FF44;
    private static final int DISABLED_COLOR = 0xFFFF4444;

    public Notifications() {
        super("Notifications", false);
    }

    public static void postToggle(Module module, boolean enabled) {
        if (module instanceof Notifications) return;
        Notifications self = (Notifications) Myau.moduleManager.getModule(Notifications.class);
        if (self == null || !self.isEnabled()) return;
        String msg = (enabled ? "Enabled " : "Disabled ") + module.getName();

        // The Compose HUD keeps its own queue so it can animate entries inside OneConfig's
        // Skia scene; only one of the two paths is ever fed.
        if (laoqi123.oneconfig.huds.NotificationsComposeHud.isActive()) {
            laoqi123.oneconfig.huds.NotificationsComposeHud.post(module.getName(), enabled);
            return;
        }
        queue.add(new NotificationEntry(msg, 2000L, enabled));
    }

    private float lerp(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        // The Compose HUD owns notifications when available; postToggle feeds only one of
        // the two paths, so this queue stays empty in that case anyway.
        if (laoqi123.oneconfig.huds.NotificationsComposeHud.isActive()) return;
        if (queue.isEmpty()) return;

        DrawContext context = event.getContext();
        float screenRight = mc.getWindow().getScaledWidth();
        float screenBottom = mc.getWindow().getScaledHeight();

        java.util.Iterator<NotificationEntry> it = queue.iterator();
        while (it.hasNext()) {
            NotificationEntry entry = it.next();
            int textWidth = mc.textRenderer.getWidth(entry.message);
            int textHeight = mc.textRenderer.fontHeight;
            int padding = 6;
            int borderWidth = 3;
            int barHeight = 2;
            entry.boxWidth = textWidth + padding * 2 + borderWidth * 2 - 5;
            entry.boxHeight = textHeight + padding * 2 + borderWidth * 2 + barHeight - 5;

            if (entry.isFinished()) {
                entry.targetX = screenRight + 10;
                if (Math.abs(entry.animationX - entry.targetX) < 1.0f) {
                    it.remove();
                    continue;
                }
            } else {
                entry.targetX = screenRight - entry.boxWidth - 10;
            }
        }

        float currentY = screenBottom - 35;
        for (NotificationEntry entry : queue) {
            float yPos = currentY - entry.boxHeight;
            entry.targetY = yPos;
            currentY = yPos - 6;
        }

        for (NotificationEntry entry : queue) {
            if (entry.animationX == 0 && entry.targetX != 0) {
                entry.animationX = screenRight + 10;
            }
            if (entry.animationY == 0 && entry.targetY != 0) {
                entry.animationY = entry.targetY;
            }
            entry.animationX = lerp(entry.animationX, entry.targetX, ANIMATION_SPEED);
            entry.animationY = lerp(entry.animationY, entry.targetY, ANIMATION_SPEED);
        }

        for (NotificationEntry entry : queue) {
            float xPos = entry.animationX;
            float yPos = entry.animationY;

            // OneConfig HUD surface: 50% black with 4px corners, no border.
            Glass.panel(xPos, yPos, entry.boxWidth, entry.boxHeight);

            // Remaining-time bar along the bottom edge, inset to clear the corners.
            float progressWidth = (entry.boxWidth - Glass.RADIUS * 2.0f) * (1.0f - entry.getProgress());
            if (progressWidth > 0.0f) {
                int progressColor = entry.isEnabled ? ENABLED_COLOR : DISABLED_COLOR;
                RenderUtil.drawRect(xPos + Glass.RADIUS, yPos + entry.boxHeight - 2.0f,
                        xPos + Glass.RADIUS + progressWidth, yPos + entry.boxHeight - 1.0f,
                        progressColor);
            }

            String text = entry.message;
            int prefixColor = entry.isEnabled ? ENABLED_COLOR : DISABLED_COLOR;

            String prefix = entry.isEnabled ? "Enabled " : "Disabled ";
            String moduleName = text.substring(prefix.length());

            int prefixWidth = Glass.textWidth(prefix);
            int totalWidth = prefixWidth + Glass.textWidth(moduleName);
            float textX = xPos + (entry.boxWidth - totalWidth) / 2;
            float textY = yPos + (entry.boxHeight - Glass.textHeight()) / 2;

            Glass.text(context, prefix, textX, textY, prefixColor);
            Glass.text(context, moduleName, textX + prefixWidth, textY, Glass.TEXT);
        }
    }

    private static class NotificationEntry {
        private final String message;
        private final long startTime;
        private final long duration;
        private final boolean isEnabled;
        private float animationX;
        private float targetX;
        private float animationY;
        private float targetY;
        private int boxWidth;
        private int boxHeight;

        public NotificationEntry(String message, long duration, boolean isEnabled) {
            this.message = message;
            this.duration = duration;
            this.isEnabled = isEnabled;
            this.startTime = System.currentTimeMillis();
            this.animationX = 0;
            this.targetX = 0;
            this.animationY = 0;
            this.targetY = 0;
        }

        public boolean isFinished() {
            return System.currentTimeMillis() - startTime > duration;
        }

        public float getProgress() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.min(1.0f, (float) elapsed / duration);
        }
    }
}
