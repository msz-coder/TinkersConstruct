package slimeknights.tconstruct.tables.client.inventory;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag.Default;
import org.lwjgl.glfw.GLFW;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.client.screen.ModuleScreen;
import slimeknights.mantle.client.screen.ScalableElementScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.client.RenderUtils;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.recipe.tinkerstation.ValidatedResult;
import slimeknights.tconstruct.library.tools.item.ITinkerStationDisplay;
import slimeknights.tconstruct.library.tools.layout.LayoutIcon;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.TinkerTooltipFlags;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.module.InfoPanelScreen;
import slimeknights.tconstruct.tables.client.inventory.widget.SlotButtonItem;
import slimeknights.tconstruct.tables.client.inventory.widget.TinkerStationButtonsWidget;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;
import slimeknights.tconstruct.tables.menu.slot.TinkerStationSlot;
import slimeknights.tconstruct.tables.network.TinkerStationRenamePacket;
import slimeknights.tconstruct.tables.network.TinkerStationSelectionPacket;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity.INPUT_SLOT;
import static slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity.TINKER_SLOT;
import static slimeknights.tconstruct.tables.client.inventory.TinkerStationHelper.needsDisplayUpdate;

public class TinkerStationScreen extends BaseTabbedScreen<TinkerStationBlockEntity,TinkerStationContainerMenu> {

  public static ResourceLocation getTinkerStationTexture() {
    return TINKER_STATION_TEXTURE;
  }

  private TinkerStationHelper stationHelper;
  // titles to display
   static final Component COMPONENTS_TEXT = TConstruct.makeTranslation("gui", "tinker_station.components");
  public static final Component MODIFIERS_TEXT = TConstruct.makeTranslation("gui", "tinker_station.modifiers");
  public static final Component UPGRADES_TEXT = TConstruct.makeTranslation("gui", "tinker_station.upgrades");
  public static final Component TRAITS_TEXT = TConstruct.makeTranslation("gui", "tinker_station.traits");
  // fallback text for crafting with no named slots
  public static final Component ASCII_ANVIL = new TextComponent("\n\n")
    .append("       .\n")
    .append("     /( _________\n")
    .append("     |  >:=========`\n")
    .append("     )(  \n")
    .append("     \"\"")
    .withStyle(ChatFormatting.DARK_GRAY);

  // parameters to display the still filled slots when changing layout
  public static final int STILL_FILLED_X = 112;
  public static final int STILL_FILLED_Y = 62;
  public static final int STILL_FILLED_SPACING = 18;

  // texture
  private static final ResourceLocation TINKER_STATION_TEXTURE = TConstruct.getResource("textures/gui/tinker_station.png");
  // texture elements
  private static final ElementScreen ACTIVE_TEXT_FIELD = new ElementScreen(0, 210, 91, 12, 256, 256);
  private static final ElementScreen ITEM_COVER = new ElementScreen(176, 18, 70, 64);
  // slots
  private static final ElementScreen SLOT_BACKGROUND = new ElementScreen(176, 0, 18, 18);
  private static final ElementScreen SLOT_BORDER = new ElementScreen(194, 0, 18, 18);
  private static final ElementScreen SLOT_SPACE_TOP = new ElementScreen(0, 174 + 2, 18, 2);
  private static final ElementScreen SLOT_SPACE_BOTTOM = new ElementScreen(0, 174, 18, 2);
  // panel
  private static final ElementScreen PANEL_SPACE_LEFT = new ElementScreen(0, 174, 5, 4);
  private static final ElementScreen PANEL_SPACE_RIGHT = new ElementScreen(9, 174, 9, 4);
  private static final ElementScreen LEFT_BEAM = new ElementScreen(0, 180, 2, 7);
  private static final ElementScreen RIGHT_BEAM = new ElementScreen(131, 180, 2, 7);
  private static final ScalableElementScreen CENTER_BEAM = new ScalableElementScreen(2, 180, 129, 7);
  // text boxes
  private static final ElementScreen TEXT_BOX = new ElementScreen(0, 222, 90, 12);

  /** Number of button columns in the UI */
  public static final int COLUMN_COUNT = 5;

  // configurable elements
  protected ElementScreen buttonDecorationTop = SLOT_SPACE_TOP;
  protected ElementScreen buttonDecorationBot = SLOT_SPACE_BOTTOM;
  protected ElementScreen panelDecorationL = PANEL_SPACE_LEFT;
  protected ElementScreen panelDecorationR = PANEL_SPACE_RIGHT;

  protected ElementScreen leftBeam = new ElementScreen(0, 0, 0, 0);
  protected ElementScreen rightBeam = new ElementScreen(0, 0, 0, 0);
  protected ScalableElementScreen centerBeam = new ScalableElementScreen(0, 0, 0, 0);

  /** Gets the default layout to apply, the "repair" button */
  @Nonnull @Getter
  private final StationSlotLayout defaultLayout;
  /** Currently selected tool */
  @Nonnull @Getter
  public StationSlotLayout currentLayout;

  // components
  public EditBox textField;
  protected InfoPanelScreen tinkerInfo;
  public  InfoPanelScreen modifierInfo;
  protected TinkerStationButtonsWidget buttonsScreen;

  /** Maximum available slots */
  @Getter
  public final int maxInputs;
  /** How many of the available input slots are active */
  protected int activeInputs;

  public TinkerStationScreen(TinkerStationContainerMenu container, Inventory playerInventory, Component title) {
    super(container, playerInventory, title);
    this.stationHelper = new TinkerStationHelper(this);
    this.tinkerInfo = new InfoPanelScreen(this, container, playerInventory, title);
    this.tinkerInfo.setTextScale(8/9f);
    this.addModule(this.tinkerInfo);

    this.modifierInfo = new InfoPanelScreen(this, container, playerInventory, title);
    this.modifierInfo.setTextScale(7/9f);
    this.addModule(this.modifierInfo);

    this.tinkerInfo.yOffset = 5;
    this.modifierInfo.yOffset = this.tinkerInfo.imageHeight + 9;

    this.imageHeight = 174;

    // determine number of inputs
    int max = 5;
    TinkerStationBlockEntity te = container.getTile();
    if (te != null) {
      max = te.getInputCount(); // TODO: not station sensitive
    }
    this.maxInputs = max;

    // large if at least 4, todo can configure?
    if (max > 3) {
      this.metal();
    } else {
      this.wood();
    }
    // apply base slot information
    if (te == null) {
      this.defaultLayout = StationSlotLayout.EMPTY;
    } else {
      this.defaultLayout = StationSlotLayoutLoader.getInstance().get(Objects.requireNonNull(te.getBlockState().getBlock().getRegistryName()));
    }
    this.currentLayout = this.defaultLayout;
    this.activeInputs = Math.min(defaultLayout.getInputCount(), max);
    this.passEvents = false;
  }

  @Override
  public void init() {

    assert this.minecraft != null;
    // TODO: pretty sure we don't need this unless we add back the renaming slot
    this.minecraft.keyboardHandler.setSendRepeatsToGui(true);

    // workaround to line up the tabs on switching even though the GUI is a tad higher
    this.topPos += 4;
    this.cornerY += 4;

    this.tinkerInfo.xOffset = 2;
    this.tinkerInfo.yOffset = this.centerBeam.h + this.panelDecorationL.h;
    this.modifierInfo.xOffset = this.tinkerInfo.xOffset;
    this.modifierInfo.yOffset = this.tinkerInfo.yOffset + this.tinkerInfo.imageHeight + 4;

    for (ModuleScreen<?,?> module : this.modules) {
      module.topPos += 4;
    }

    int x = (this.width - this.imageWidth) / 2;
    int y = (this.height - this.imageHeight) / 2;
    textField = new EditBox(this.font, x + 80, y + 5, 82, 9, TextComponent.EMPTY);
    textField.setCanLoseFocus(true);
    textField.setTextColor(-1);
    textField.setTextColorUneditable(-1);
    textField.setBordered(false);
    textField.setMaxLength(50);
    textField.setResponder(this::onNameChanged);
    textField.setValue("");
    addWidget(textField);
    textField.visible = false;
    textField.setEditable(false);

    super.init();

    int buttonsStyle = this.maxInputs > 3 ? TinkerStationButtonsWidget.METAL_STYLE : TinkerStationButtonsWidget.WOOD_STYLE;

    List<StationSlotLayout> layouts = Lists.newArrayList();
    // repair layout
    layouts.add(this.defaultLayout);
    // tool layouts
    layouts.addAll(StationSlotLayoutLoader.getInstance().getSortedSlots().stream()
      .filter(layout -> layout.getInputSlots().size() <= this.maxInputs).toList());

    this.buttonsScreen = new TinkerStationButtonsWidget(this, this.cornerX - TinkerStationButtonsWidget.width(COLUMN_COUNT) - 2,
      this.cornerY + this.centerBeam.h + this.buttonDecorationTop.h, layouts, buttonsStyle);

    stationHelper.updateLayout();
  }

  @Override
  protected void drawContainerName(PoseStack matrixStack) {
    this.font.draw(matrixStack, this.getTitle(), 8.0F, 8.0F, 4210752);
  }

  public static void renderIcon(PoseStack matrices, LayoutIcon icon, int x, int y) {
    Pattern pattern = icon.getValue(Pattern.class);
    Minecraft minecraft = Minecraft.getInstance();
    if (pattern != null) {
      // draw pattern sprite
      RenderUtils.setup(InventoryMenu.BLOCK_ATLAS);
      RenderSystem.applyModelViewMatrix();
      GuiUtil.renderPattern(matrices, pattern, x, y);
      return;
    }

    ItemStack stack = icon.getValue(ItemStack.class);
    if (stack != null) {
      minecraft.getItemRenderer().renderGuiItem(stack, x, y);
    }
  }

  @Override
  protected void renderBg(PoseStack matrices, float partialTicks, int mouseX, int mouseY) {
    this.drawBackground(matrices, TINKER_STATION_TEXTURE);

    int x = 0;
    int y = 0;

    // draw the item background
    final float scale = 3.7f;
    final float xOff = 12.5f;
    final float yOff = 22f;

    // render the background icon
    PoseStack renderPose = RenderSystem.getModelViewStack();
    renderPose.pushPose();
    renderPose.translate(xOff, yOff, 0.0F);
    renderPose.scale(scale, scale, 1.0f);
    renderIcon(matrices, currentLayout.getIcon(), (int) (this.cornerX / scale), (int) (this.cornerY / scale));
    renderPose.popPose();
    RenderSystem.applyModelViewMatrix();

    // rebind gui texture since itemstack drawing sets it to something else
    RenderUtils.setup(TINKER_STATION_TEXTURE, 1.0f, 1.0f, 1.0f, 0.82f);
    RenderSystem.enableBlend();
    //RenderSystem.enableAlphaTest();
    //RenderHelper.turnOff();
    RenderSystem.disableDepthTest();
    ITEM_COVER.draw(matrices, this.cornerX + 7, this.cornerY + 18);

    // slot backgrounds, are transparent
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.28f);
    if (!this.currentLayout.getToolSlot().isHidden()) {
      Slot slot = this.getMenu().getSlot(TINKER_SLOT);
      SLOT_BACKGROUND.draw(matrices, x + this.cornerX + slot.x - 1, y + this.cornerY + slot.y - 1);
    }
    for (int i = 0; i < this.activeInputs; i++) {
      Slot slot = this.getMenu().getSlot(i + INPUT_SLOT);
      SLOT_BACKGROUND.draw(matrices, x + this.cornerX + slot.x - 1, y + this.cornerY + slot.y - 1);
    }

    // slot borders, are opaque
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    for (int i = 0; i <= maxInputs; i++) {
      Slot slot = this.getMenu().getSlot(i);
      if ((slot instanceof TinkerStationSlot && (!((TinkerStationSlot) slot).isDormant() || slot.hasItem()))) {
        SLOT_BORDER.draw(matrices, x + this.cornerX + slot.x - 1, y + this.cornerY + slot.y - 1);
      }
    }

    // sidebar beams
    x = this.buttonsScreen.getLeftPos() - this.leftBeam.w;
    y = this.cornerY;
    // draw the beams at the top
    x += this.leftBeam.draw(matrices, x, y);
    x += this.centerBeam.drawScaledX(matrices, x, y, this.buttonsScreen.getImageWidth());
    this.rightBeam.draw(matrices, x, y);

    x = tinkerInfo.leftPos - this.leftBeam.w;
    x += this.leftBeam.draw(matrices, x, y);
    x += this.centerBeam.drawScaledX(matrices, x, y, this.tinkerInfo.imageWidth);
    this.rightBeam.draw(matrices, x, y);

    // draw the decoration for the buttons
    for (SlotButtonItem button : this.buttonsScreen.getButtons()) {
      this.buttonDecorationTop.draw(matrices, button.x, button.y - this.buttonDecorationTop.h);
      // don't draw the bottom for the buttons in the last row
      if (button.buttonId < this.buttonsScreen.getButtons().size() - COLUMN_COUNT) {
        this.buttonDecorationBot.draw(matrices, button.x, button.y + button.getHeight());
      }
    }

    // draw the decorations for the panels
    this.panelDecorationL.draw(matrices, this.tinkerInfo.leftPos + 5, this.tinkerInfo.topPos - this.panelDecorationL.h);
    this.panelDecorationR.draw(matrices, this.tinkerInfo.guiRight() - 5 - this.panelDecorationR.w, this.tinkerInfo.topPos - this.panelDecorationR.h);
    this.panelDecorationL.draw(matrices, this.modifierInfo.leftPos + 5, this.modifierInfo.topPos - this.panelDecorationL.h);
    this.panelDecorationR.draw(matrices, this.modifierInfo.guiRight() - 5 - this.panelDecorationR.w, this.modifierInfo.topPos - this.panelDecorationR.h);

    // render slot background icons
    RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
    for (int i = 0; i <= maxInputs; i++) {
      Slot slot = this.getMenu().getSlot(i);
      if (!slot.hasItem()) {
        Pattern icon = currentLayout.getSlot(i).getIcon();
        if (icon != null) {
          GuiUtil.renderPattern(matrices, icon, this.cornerX + slot.x, this.cornerY + slot.y);
        }
      }
    }

    RenderSystem.enableDepthTest();

    super.renderBg(matrices, partialTicks, mouseX, mouseY);

    this.buttonsScreen.render(matrices, mouseX, mouseY, partialTicks);

    // text field
    if (textField != null && textField.visible) {
      RenderUtils.setup(TINKER_STATION_TEXTURE, 1.0f, 1.0f, 1.0f, 1.0f);
      TEXT_BOX.draw(matrices, this.cornerX + 79, this.cornerY + 3);
      this.textField.render(matrices, mouseX, mouseY, partialTicks);
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
    if (this.tinkerInfo.handleMouseClicked(mouseX, mouseY, mouseButton)) {
      return false;
    }

    if (this.modifierInfo.handleMouseClicked(mouseX, mouseY, mouseButton)) {
      return false;
    }

    if(this.buttonsScreen.handleMouseClicked(mouseX, mouseY, mouseButton)) {
      return false;
    }

    return super.mouseClicked(mouseX, mouseY, mouseButton);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int clickedMouseButton, double timeSinceLastClick, double unkowwn) {
    if (this.tinkerInfo.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) {
      return false;
    }

    if (this.modifierInfo.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick)) {
      return false;
    }

    return super.mouseDragged(mouseX, mouseY, clickedMouseButton, timeSinceLastClick, unkowwn);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (this.tinkerInfo.handleMouseScrolled(mouseX, mouseY, delta)) {
      return false;
    }

    if (this.modifierInfo.handleMouseScrolled(mouseX, mouseY, delta)) {
      return false;
    }

    return super.mouseScrolled(mouseX, mouseY, delta);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int state) {
    if (this.tinkerInfo.handleMouseReleased(mouseX, mouseY, state)) {
      return false;
    }

    if (this.modifierInfo.handleMouseReleased(mouseX, mouseY, state)) {
      return false;
    }

    if (this.buttonsScreen.handleMouseReleased(mouseX, mouseY, state)) {
      return false;
    }

    return super.mouseReleased(mouseX, mouseY, state);
  }


  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      this.onClose();
      return true;
    }
    if (needsDisplayUpdate(keyCode)) {
      stationHelper.updateDisplay();
    }
    if (textField.canConsumeInput()) {
      textField.keyPressed(keyCode, scanCode, modifiers);
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
    if (needsDisplayUpdate(keyCode)) {
      stationHelper.updateDisplay();
    }
    return super.keyReleased(keyCode, scanCode, modifiers);
  }

  @Override
  public void renderSlot(PoseStack matrixStack, Slot slotIn) {
    // don't draw dormant slots with no item
    if (slotIn instanceof TinkerStationSlot && ((TinkerStationSlot) slotIn).isDormant() && !slotIn.hasItem()) {
      return;
    }
    super.renderSlot(matrixStack, slotIn);
  }

  @Override
  public boolean isHovering(Slot slotIn, double mouseX, double mouseY) {
    if (slotIn instanceof TinkerStationSlot && ((TinkerStationSlot) slotIn).isDormant() && !slotIn.hasItem()) {
      return false;
    }
    return super.isHovering(slotIn, mouseX, mouseY);
  }

  protected void wood() {
    this.tinkerInfo.wood();
    this.modifierInfo.wood();

    this.buttonDecorationTop = SLOT_SPACE_TOP.shift(SLOT_SPACE_TOP.w, 0);
    this.buttonDecorationBot = SLOT_SPACE_BOTTOM.shift(SLOT_SPACE_BOTTOM.w, 0);
    this.panelDecorationL = PANEL_SPACE_LEFT.shift(18, 0);
    this.panelDecorationR = PANEL_SPACE_RIGHT.shift(18, 0);

    this.leftBeam = LEFT_BEAM;
    this.rightBeam = RIGHT_BEAM;
    this.centerBeam = CENTER_BEAM;
  }

  protected void metal() {
    this.tinkerInfo.metal();
    this.modifierInfo.metal();

    this.buttonDecorationTop = SLOT_SPACE_TOP.shift(SLOT_SPACE_TOP.w * 2, 0);
    this.buttonDecorationBot = SLOT_SPACE_BOTTOM.shift(SLOT_SPACE_BOTTOM.w * 2, 0);
    this.panelDecorationL = PANEL_SPACE_LEFT.shift(18 * 2, 0);
    this.panelDecorationR = PANEL_SPACE_RIGHT.shift(18 * 2, 0);

    this.leftBeam = LEFT_BEAM.shift(0, LEFT_BEAM.h);
    this.rightBeam = RIGHT_BEAM.shift(0, RIGHT_BEAM.h);
    this.centerBeam = CENTER_BEAM.shift(0, CENTER_BEAM.h);
  }



  @Override
  public List<Rect2i> getModuleAreas() {
    List<Rect2i> list = super.getModuleAreas();
    list.add(this.buttonsScreen.getArea());
    return list;
  }

  @Override
  protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
    return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)
      && !this.buttonsScreen.isMouseOver(mouseX, mouseY);
  }


  /* Text field stuff */

  private void onNameChanged(String name) {
    if (tile != null) {
      this.tile.setItemName(name);
      TinkerNetwork.getInstance().sendToServer(new TinkerStationRenamePacket(name));
    }
  }

  @Override
  public void containerTick() {
    super.containerTick();
    this.textField.tick();
  }

  @Override
  public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
    String s = this.textField.getValue();
    super.resize(pMinecraft, pWidth, pHeight);
    this.textField.setValue(s);
  }

  @Override
  public void removed() {
    super.removed();
    assert this.minecraft != null;
    this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
  }

  @Override
  public void onClose() {
    super.onClose();

    assert this.minecraft != null;
    this.minecraft.keyboardHandler.setSendRepeatsToGui(false);
  }
}


