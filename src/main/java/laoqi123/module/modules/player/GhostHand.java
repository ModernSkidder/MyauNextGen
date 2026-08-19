package laoqi123.module.modules.player;

import laoqi123.module.Module;
import laoqi123.util.ItemUtil;
import laoqi123.util.TeamUtil;
import laoqi123.property.properties.BooleanProperty;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class GhostHand extends Module {
    public final BooleanProperty teamsOnly = new BooleanProperty("team-only", true);
    public final BooleanProperty ignoreWeapons = new BooleanProperty("ignore-weapons", false);

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
