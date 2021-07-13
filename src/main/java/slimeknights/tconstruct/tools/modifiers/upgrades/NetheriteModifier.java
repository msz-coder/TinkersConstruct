package slimeknights.tconstruct.tools.modifiers.upgrades;

import net.minecraft.item.Rarity;
import slimeknights.tconstruct.library.modifiers.SingleLevelModifier;
import slimeknights.tconstruct.library.tinkering.HarvestLevels;
import slimeknights.tconstruct.library.tools.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IModDataReadOnly;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.StatsNBT;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.library.tools.stat.ToolStats;

public class NetheriteModifier extends SingleLevelModifier {
  public NetheriteModifier() {
    super(0x8E7C7F);
  }

  @Override
  public void addVolatileData(ToolDefinition toolDefinition, StatsNBT baseStats, IModDataReadOnly persistentData, int level, ModDataNBT volatileData) {
    volatileData.putBoolean(IModifiable.INDESTRUCTIBLE_ENTITY, true);
    IModifiable.setRarity(volatileData, Rarity.RARE);
  }

  @Override
  public void addToolStats(ToolDefinition toolDefinition, StatsNBT baseStats, IModDataReadOnly persistentData, IModDataReadOnly volatileData, int level, ModifierStatsBuilder builder) {
    ToolStats.DURABILITY.multiply(builder, 1 + (level * 0.20f));
    ToolStats.ATTACK_DAMAGE.multiply(builder, 1 + (level * 0.10f));
    ToolStats.MINING_SPEED.multiply(builder, 1 + (level * 0.10f));
    ToolStats.HARVEST_LEVEL.set(builder, HarvestLevels.NETHERITE);
  }
}
