package slimeknights.tconstruct.library.materials.definition;

import net.minecraft.resources.ResourceLocation;

public class TestMaterial extends Material {
  public TestMaterial(ResourceLocation identifier, boolean craftable, boolean hidden) {
    super(new Material.MaterialBuilder(identifier)
      .craftable(craftable)
      .hidden(hidden));
  }
}
