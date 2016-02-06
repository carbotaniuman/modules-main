package org.cubeengine.module.vanillaplus.improvement.removal;

import java.util.Collection;
import java.util.function.Predicate;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.vanillaplus.VanillaPlus;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.matcher.StringMatcher;
import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.world.World;

import static org.cubeengine.service.i18n.formatter.MessageType.*;

public class ButcherCommand extends PermissionContainer<VanillaPlus>
{
    private I18n i18n;

    public ButcherCommand(VanillaPlus module, I18n i18n, CommandManager cm, StringMatcher sm)
    {
        super(module);
        this.i18n = i18n;
        cm.getProviderManager().register(module, new LivingFilterReader(module, i18n, sm), LivingFilter.class);
    }

    public final PermissionDescription COMMAND_BUTCHER_FLAG_LIGHTNING = register("command.butcher.lightning", "", null);
    public final PermissionDescription COMMAND_BUTCHER_FLAG_ALL = register("command.butcher.all", "", null);


    @Command(desc = "Gets rid of mobs close to you. Valid types are:\n" +
        "monster, animal, pet, golem, boss, other, creeper, skeleton, spider etc.")
    public void butcher(CommandSource context, @Label("types...") @Default LivingFilter types,
                        @Optional Integer radius,
                        @Default @Named("in") World world,
                        @Flag boolean lightning, // die with style
                        @Flag boolean all) // infinite radius
    {
        radius = radius == null ? module.getConfig().improve.commandButcherDefaultRadius : radius;
        if (radius < 0 && !(radius == -1 && context.hasPermission(COMMAND_BUTCHER_FLAG_ALL.getId())))
        {
            i18n.sendTranslated(context, NEGATIVE, "The radius has to be a number greater than 0!");
            return;
        }
        if (all && context.hasPermission(COMMAND_BUTCHER_FLAG_ALL.getId()))
        {
            radius = -1;
        }
        lightning = lightning && context.hasPermission(COMMAND_BUTCHER_FLAG_LIGHTNING.getId());

        Integer rSquared = radius * radius;
        Predicate<Entity> filter = radius == -1 ? types :
           types.and(e -> e.getTransform().getPosition().distance(((Player)context).getLocation().getPosition()) <= rSquared);

        Cause lightningCause = Cause.of(context);
        Collection<Entity> remove = world.getEntities(filter);
        for (Entity entity : remove)
        {
            if (lightning)
            {
                world.spawnEntity(world.createEntity(EntityTypes.LIGHTNING, entity.getLocation().getPosition()).get(), lightningCause);
            }
            entity.remove();
        }

        if (remove.size() == 0)
        {
            i18n.sendTranslated(context, NEUTRAL, "Nothing to butcher!");
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "You just slaughtered {amount} living entities!", remove.size());
        }

    }
}
