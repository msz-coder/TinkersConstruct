package slimeknights.tconstruct.library.materials.definition;

import lombok.Getter;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

public class Material implements IMaterial {
  /** Default white color */
  protected static final TextColor WHITE = TextColor.fromRgb(0xFFFFFF);

  /** This resource location uniquely identifies a material. */
  @Getter
  private final MaterialId identifier;
  /** Materials tier, mostly for sorting right now */
  @Getter
  private final int tier;
  /** Materials order within the tier, for sorting */
  @Getter
  private final int sortOrder;

  /** Material can be crafted into parts in the PartBuilder */
  @Getter
  private final boolean craftable;
  /** if true, this material is hidden */
  @Getter
  private final boolean hidden;

  /**
   * Materials should only be created by the MaterialManager, except when used for data gen
   * They're synced over the network and other classes might lead to unexpected behaviour.
   */
  public Material(MaterialBuilder builder) {
    this.identifier = builder.identifier;
    this.tier = builder.tier;
    this.sortOrder = builder.sortOrder;
    this.craftable = builder.craftable;
    this.hidden = builder.hidden;
  }

  @Override
  public String toString() {
    return "Material{" + identifier + '}';
  }

  public static class MaterialBuilder {
    private final MaterialId identifier;
    private int tier = 0;
    private int sortOrder = -1;
    private boolean craftable = false;
    private boolean hidden = false;

    public MaterialBuilder(ResourceLocation identifier) {
      this.identifier = new MaterialId(identifier);
    }

    public MaterialBuilder tier(int tier) {
      this.tier = tier;
      return this;
    }

    public MaterialBuilder sortOrder(int sortOrder) {
      this.sortOrder = sortOrder;
      return this;
    }

    public MaterialBuilder craftable(boolean craftable) {
      this.craftable = craftable;
      return this;
    }

    public MaterialBuilder hidden(boolean hidden) {
      this.hidden = hidden;
      return this;
    }

    public Material build() {
      return new Material(this);
    }
  }
}
