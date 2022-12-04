package slimeknights.tconstruct.tools.item;

import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.tools.data.material.MaterialIds;

import java.util.function.Predicate;

public class ModifiableBowItem extends ModifiableLauncherItem {
  public ModifiableBowItem(Properties properties, ToolDefinition toolDefinition) {
    super(properties, toolDefinition);
  }


  /* Properties */

  @Override
  public Predicate<ItemStack> getAllSupportedProjectiles() {
    return ProjectileWeaponItem.ARROW_ONLY;
  }

  @Override
  public int getDefaultProjectileRange() {
    return 15;
  }

  @Override
  public UseAnim getUseAnimation(ItemStack pStack) {
    return UseAnim.BOW;
  }


  /* Arrow launching */

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack itemstack = player.getItemInHand(hand);
    boolean noAmmo = player.getProjectile(itemstack).isEmpty();
    InteractionResultHolder<ItemStack> override = ForgeEventFactory.onArrowNock(itemstack, level, player, hand, !noAmmo);
    if (override != null) {
      return override;
    }
    if (!player.getAbilities().instabuild && noAmmo) {
      return InteractionResultHolder.fail(itemstack);
    }
    player.startUsingItem(hand);
    return InteractionResultHolder.consume(itemstack);
  }

  @Override
  public void releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
    // need player
    if (!(living instanceof Player player)) {
      return;
    }
    // no broken
    ToolStack tool = ToolStack.from(stack);
    if (tool.isBroken()) {
      return;
    }

    // TODO: modifier hook for inifinity/chance base arrow use
    boolean infinity = player.getAbilities().instabuild; // || tool.getPersistentData().getBoolean();
    // TODO: hook for custom ammo?
    ItemStack ammo = player.getProjectile(stack); // TODO: we could make this stack sensitive instead

    int chargeTime = this.getUseDuration(stack) - timeLeft;
    chargeTime = ForgeEventFactory.onArrowLoose(stack, level, player, chargeTime, !ammo.isEmpty() || infinity);

    // no ammo? no charge? nothing to do
    if (chargeTime < 0 || (ammo.isEmpty() && !infinity)) {
      return;
    }
    // no ammo? sub in vanilla arrows
    if (ammo.isEmpty()) {
      ammo = new ItemStack(Items.ARROW);
    }

    // calculate arrow power
    StatsNBT stats = tool.getStats();
    float charge = chargeTime * stats.get(ToolStats.DRAW_SPEED) / 20f;
    charge = (charge * charge + charge * 2) / 3;
    if (charge > 1) {
      charge = 1;
    }
    float power = charge * stats.get(ToolStats.VELOCITY);
    if (power < 0.1f) {
      return;
    }

    // launch the arrow
    boolean arrowInfinite = player.getAbilities().instabuild || (ammo.getItem() instanceof ArrowItem arrow && arrow.isInfinite(ammo, stack, player));
    if (!level.isClientSide) {
      ArrowItem arrowItem = ammo.getItem() instanceof ArrowItem arrow ? arrow : (ArrowItem)Items.ARROW;
      AbstractArrow arrowEntity = arrowItem.createArrow(level, ammo, player);
      arrowEntity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 3.0F, 3*(1/stats.get(ToolStats.ACCURACY))-1);
      // TODO: modifier hook to add arrow properties
      if (charge == 1.0F) {
        arrowEntity.setCritArrow(true);
      }

      ToolDamageUtil.damageAnimated(tool, 1, player, player.getUsedItemHand());
      // if infinite, skip pickup
      if (arrowInfinite || player.getAbilities().instabuild && !ammo.is(Items.ARROW)) {
        arrowEntity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
      }
      level.addFreshEntity(arrowEntity);
    }

    // consume items
    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + charge * 0.5F);
    if (!arrowInfinite && !player.getAbilities().instabuild) {
      ammo.shrink(1);
      if (ammo.isEmpty()) {
        player.getInventory().removeItem(ammo);
      }
    }

    player.awardStat(Stats.ITEM_USED.get(this));
  }

  @Override
  public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
    if (this.allowdedIn(group)) {
      ToolBuildHandler.addDefaultSubItems(this, items, null, null, null, MaterialIds.string);
    }
  }
}