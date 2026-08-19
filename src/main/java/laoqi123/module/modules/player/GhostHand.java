package laoqi123.module.modules.player;

import laoqi123.module.Module;
import laoqi123.util.ItemUtil;
import laoqi123.util.TeamUtil;
import laoqi123.value.properties.BooleanValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class GhostHand extends Module {
    public final BooleanValue teamsOnly = new BooleanValue("team-only", true);
    public final BooleanValue ignoreWeapons = new BooleanValue("ignore-weapons", false);

    public GhostHand() {
        super("GhostHand", false);
    }

    public boolean shouldSkip(Entity entity) {
        return entity instanceof PlayerEntity
                && !TeamUtil.isBot((PlayerEntity) entity)
                && (!this.teamsOnly.getValue() || TeamUtil.isSameTeam((PlayerEntity) entity))
                && (!this.ignoreWeapons.getValue() || !ItemUtil.hasRawUnbreakingEnchant());
    }
}
