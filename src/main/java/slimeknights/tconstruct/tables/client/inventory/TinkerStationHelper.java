package slimeknights.tconstruct.tables.client.inventory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.glfw.GLFW;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.recipe.tinkerstation.ValidatedResult;
import slimeknights.tconstruct.library.tools.item.ITinkerStationDisplay;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.TinkerTooltipFlags;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.module.InfoPanelScreen;
import slimeknights.tconstruct.tables.menu.slot.TinkerStationSlot;
import slimeknights.tconstruct.tables.network.TinkerStationRenamePacket;
import slimeknights.tconstruct.tables.network.TinkerStationSelectionPacket;
import slimeknights.mantle.client.SafeClientAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.minecraft.client.gui.screens.Screen.hasControlDown;
import static net.minecraft.client.gui.screens.Screen.hasShiftDown;
import static slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity.TINKER_SLOT;
import static slimeknights.tconstruct.tables.client.inventory.BaseTabbedScreen.COMPONENT_ERROR;
import static slimeknights.tconstruct.tables.client.inventory.BaseTabbedScreen.COMPONENT_WARNING;
import static slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen.ASCII_ANVIL;
import static slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen.COMPONENTS_TEXT;
import static slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen.MODIFIERS_TEXT;
import static slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen.STILL_FILLED_SPACING;
import static slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen.STILL_FILLED_X;
import static slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen.STILL_FILLED_Y;
import static slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen.TRAITS_TEXT;
import static slimeknights.tconstruct.tables.client.inventory.TinkerStationScreen.UPGRADES_TEXT;


public class TinkerStationHelper {

  private TinkerStationScreen screen;

  public TinkerStationHelper(TinkerStationScreen screen) {
    this.screen = screen;
  }



  /** Updates the tool panel area */
  public static void updateToolPanel(InfoPanelScreen tinkerInfo, ToolStack tool, ItemStack result) {
    if (tool.getItem() instanceof ITinkerStationDisplay display) {
      tinkerInfo.setCaption(display.getLocalizedName());
      tinkerInfo.setText(display.getStatInformation(tool, Minecraft.getInstance().player, new ArrayList<>(), SafeClientAccess.getTooltipKey(), TinkerTooltipFlags.TINKER_STATION));
    } else {
      tinkerInfo.setCaption(result.getHoverName());
      List<Component> list = new ArrayList<>();
      result.getItem().appendHoverText(result, Minecraft.getInstance().level, list, TooltipFlag.Default.NORMAL);
      tinkerInfo.setText(list);
    }
  }

  /** Updates all slots for the current slot layout */
  public void updateLayout() {
    int stillFilled = 0;
    for (int i = 0; i <= screen.maxInputs; i++) {
      Slot slot = screen.getMenu().getSlot(i);
      LayoutSlot layoutSlot = screen.currentLayout.getSlot(i);
      if (layoutSlot.isHidden()) {
        // put the position in the still filled line
        slot.x = STILL_FILLED_X - STILL_FILLED_SPACING * stillFilled;
        slot.y = STILL_FILLED_Y;
        stillFilled++;
        if (slot instanceof TinkerStationSlot tinkerSlot) {
          tinkerSlot.deactivate();
        }
      } else {
        slot.x = layoutSlot.getX();
        slot.y = layoutSlot.getY();
        if (slot instanceof TinkerStationSlot tinkerSlot) {
          tinkerSlot.activate(layoutSlot);
        }
      }
    }

    this.updateDisplay();
  }

  public void updateDisplay() {
    if (screen.tile == null) {
      return;
    }

    ItemStack toolStack = screen.getMenu().getResult();

    // if we have a message, display instead of refreshing the tool
    ValidatedResult currentError = screen.tile.getCurrentError();
    if (currentError.hasError()) {
      error(currentError.getMessage());
      return;
    }

    // only get to rename new tool in the station
    // anvil can rename on any tool change
    if (toolStack.isEmpty() || (screen.tile.getInputCount() <= 4 && screen.getMenu().getSlot(TINKER_SLOT).hasItem())) {
      screen.textField.setEditable(false);
      screen.textField.setValue("");
      screen.textField.visible = false;
    } else if (!screen.textField.isEditable()) {
      screen.textField.setEditable(true);
      screen.textField.setValue("");
      screen.textField.visible = true;
    } else {
      // ensure the text matches
      screen.textField.setValue(screen.tile.getItemName());
    }

    // normal refresh
    if (toolStack.isEmpty()) {
      toolStack = screen.getMenu().getSlot(TINKER_SLOT).getItem();
    }

    // if the contained stack is modifiable, display some information
    if (toolStack.is(TinkerTags.Items.MODIFIABLE)) {
      ToolStack toolInstance = ToolStack.from(toolStack);
      updateToolPanel(screen.tinkerInfo, toolInstance, toolStack);
      updateModifierPanel(screen.modifierInfo, toolInstance);
    }
    // tool build info
    else {
      screen.tinkerInfo.setCaption(screen.currentLayout.getDisplayName());
      screen.tinkerInfo.setText(screen.currentLayout.getDescription());

      // for each named slot, color the slot if the slot is filled
      // typically all input slots should be named, or none of them
      MutableComponent fullText = new TextComponent("");
      boolean hasComponents = false;
      for (int i = 0; i <= screen.activeInputs; i++) {
        LayoutSlot layout = screen.currentLayout.getSlot(i);
        String key = layout.getTranslationKey();
        if (!layout.isHidden() && !key.isEmpty()) {
          hasComponents = true;
          MutableComponent textComponent = new TextComponent(" * ");
          ItemStack slotStack = screen.getMenu().getSlot(i).getItem();
          if (!layout.isValid(slotStack)) {
            textComponent.withStyle(ChatFormatting.RED);
          }
          textComponent.append(new TranslatableComponent(key)).append("\n");
          fullText.append(textComponent);
        }
      }
      // if we found any components, set the text, use the anvil if no components
      if (hasComponents) {
        screen.modifierInfo.setCaption(COMPONENTS_TEXT);
        screen.modifierInfo.setText(fullText);
      } else {
        screen.modifierInfo.setCaption(TextComponent.EMPTY);
        screen.modifierInfo.setText(ASCII_ANVIL);
      }
    }
  }


  /** Updates the modifier panel with relevant info */
  static void updateModifierPanel(InfoPanelScreen modifierInfo, ToolStack tool) {
    List<Component> modifierNames = new ArrayList<>();
    List<Component> modifierTooltip = new ArrayList<>();
    Component title;
    // control displays just traits, bit trickier to do
    if (hasControlDown()) {
      title = TRAITS_TEXT;
      Map<Modifier,Integer> upgrades = tool.getUpgrades().getModifiers().stream()
        .collect(Collectors.toMap(ModifierEntry::getModifier, ModifierEntry::getLevel));
      for (ModifierEntry entry : tool.getModifierList()) {
        Modifier mod = entry.getModifier();
        if (mod.shouldDisplay(true)) {
          int level = entry.getLevel() - upgrades.getOrDefault(mod, 0);
          if (level > 0) {
            modifierNames.add(mod.getDisplayName(tool, level));
            modifierTooltip.add(mod.getDescription(tool, level));
          }
        }
      }
    } else {
      // shift is just upgrades/abilities, otherwise all
      List<ModifierEntry> modifiers;
      if (hasShiftDown()) {
        modifiers = tool.getUpgrades().getModifiers();
        title = UPGRADES_TEXT;
      } else {
        modifiers = tool.getModifierList();
        title = MODIFIERS_TEXT;
      }
      for (ModifierEntry entry : modifiers) {
        Modifier mod = entry.getModifier();
        if (mod.shouldDisplay(true)) {
          int level = entry.getLevel();
          modifierNames.add(mod.getDisplayName(tool, level));
          modifierTooltip.add(mod.getDescription(tool, level));
        }
      }
    }

    modifierInfo.setCaption(title);
    modifierInfo.setText(modifierNames, modifierTooltip);
  }

  /** Returns true if a key changed that requires a display update */
  static boolean needsDisplayUpdate(int keyCode) {
    if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
      return true;
    }
    if (Minecraft.ON_OSX) {
      return keyCode == GLFW.GLFW_KEY_LEFT_SUPER || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
    }
    return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL;
  }

  /**
   * Called when a tool button is pressed
   * @param layout      Data of the slot selected
   */
  public void onToolSelection(StationSlotLayout layout) {
    screen.activeInputs = Math.min(layout.getInputCount(), screen.maxInputs);
    screen.currentLayout = layout;
    updateLayout();

    // update the active slots and filter in the container
    // this.container.setToolSelection(layout); TODO: needed?
    TinkerNetwork.getInstance().sendToServer(new TinkerStationSelectionPacket(layout.getName()));
  }

  public void onToolSelectionInternal(StationSlotLayout layout) {
    screen.activeInputs = Math.min(layout.getInputCount(), screen.maxInputs);
    screen.currentLayout = layout;
    updateLayout();

    // update the active slots and filter in the container
    // this.container.setToolSelection(layout); TODO: needed?
    TinkerNetwork.getInstance().sendToServer(new TinkerStationSelectionPacket(layout.getName()));
  }



  public void error(Component message) {
    screen.tinkerInfo.setCaption(COMPONENT_ERROR);
    screen.tinkerInfo.setText(message);
    screen.modifierInfo.setCaption(TextComponent.EMPTY);
    screen.modifierInfo.setText(TextComponent.EMPTY);
  }

  public void warning(Component message) {
    screen.tinkerInfo.setCaption(COMPONENT_WARNING);
    screen.tinkerInfo.setText(message);
    screen.modifierInfo.setCaption(TextComponent.EMPTY);
    screen.modifierInfo.setText(TextComponent.EMPTY);
  }
}
