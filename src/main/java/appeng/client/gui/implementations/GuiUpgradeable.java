/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.gui.implementations;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Mouse;

import akka.japi.Pair;
import appeng.api.config.ActionItems;
import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.config.YesNo;
import appeng.api.implementations.IUpgradeableHost;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.container.implementations.ContainerUpgradeable;
import appeng.container.slot.SlotFake;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketConfigButton;
import appeng.core.sync.packets.PacketNEIDragClick;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.parts.automation.PartExportBus;
import appeng.parts.automation.PartImportBus;
import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import cpw.mods.fml.common.Optional;

@Optional.Interface(modid = "NotEnoughItems", iface = "codechicken.nei.api.INEIGuiHandler")
public class GuiUpgradeable extends AEBaseGui implements INEIGuiHandler {

    protected final ContainerUpgradeable cvb;
    protected final IUpgradeableHost bc;

    protected GuiImgButton redstoneMode;
    protected GuiImgButton fuzzyMode;
    protected GuiImgButton craftMode;
    protected GuiImgButton schedulingMode;
    protected GuiImgButton oreFilter;

    public GuiUpgradeable(final InventoryPlayer inventoryPlayer, final IUpgradeableHost te) {
        this(new ContainerUpgradeable(inventoryPlayer, te));
    }

    public GuiUpgradeable(final ContainerUpgradeable te) {
        super(te);
        this.cvb = te;

        this.bc = (IUpgradeableHost) te.getTarget();
        this.xSize = this.hasToolbox() ? 246 : 211;
        this.ySize = 184;
    }

    protected boolean hasToolbox() {
        return ((ContainerUpgradeable) this.inventorySlots).hasToolbox();
    }

    @Override
    public void initGui() {
        super.initGui();
        this.addButtons();
    }

    @SuppressWarnings("unchecked")
    protected void addButtons() {
        this.redstoneMode = new GuiImgButton(
                this.guiLeft - 18,
                this.guiTop + 8,
                Settings.REDSTONE_CONTROLLED,
                RedstoneMode.IGNORE);
        this.fuzzyMode = new GuiImgButton(
                this.guiLeft - 18,
                this.guiTop + 28,
                Settings.FUZZY_MODE,
                FuzzyMode.IGNORE_ALL);
        this.craftMode = new GuiImgButton(this.guiLeft - 18, this.guiTop + 48, Settings.CRAFT_ONLY, YesNo.NO);
        this.schedulingMode = new GuiImgButton(
                this.guiLeft - 18,
                this.guiTop + 68,
                Settings.SCHEDULING_MODE,
                SchedulingMode.DEFAULT);
        this.oreFilter = new GuiImgButton(
                this.guiLeft - 18,
                this.guiTop + 28,
                Settings.ACTIONS,
                ActionItems.ORE_FILTER);

        this.buttonList.add(this.craftMode);
        this.buttonList.add(this.redstoneMode);
        this.buttonList.add(this.fuzzyMode);
        this.buttonList.add(this.schedulingMode);
        this.buttonList.add(this.oreFilter);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj.drawString(
                this.getGuiDisplayName(this.getName().getLocal()),
                8,
                6,
                GuiColors.UpgradableTitle.getColor());
        this.fontRendererObj.drawString(
                GuiText.inventory.getLocal(),
                8,
                this.ySize - 96 + 3,
                GuiColors.UpgradableInventory.getColor());

        if (this.redstoneMode != null) {
            this.redstoneMode.set(this.cvb.getRedStoneMode());
        }

        if (this.fuzzyMode != null) {
            this.fuzzyMode.set(this.cvb.getFuzzyMode());
        }

        if (this.craftMode != null) {
            this.craftMode.set(this.cvb.getCraftingMode());
        }

        if (this.schedulingMode != null) {
            this.schedulingMode.set(this.cvb.getSchedulingMode());
        }
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.handleButtonVisibility();

        this.bindTexture(this.getBackground());
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, 211 - 34, this.ySize);
        if (this.drawUpgrades()) {
            this.drawTexturedModalRect(offsetX + 177, offsetY, 177, 0, 35, 14 + this.cvb.availableUpgrades() * 18);
        }
        if (this.hasToolbox()) {
            this.drawTexturedModalRect(offsetX + 178, offsetY + this.ySize - 90, 178, this.ySize - 90, 68, 68);
        }
    }

    protected void handleButtonVisibility() {
        if (this.redstoneMode != null) {
            this.redstoneMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.REDSTONE) > 0);
        }
        if (this.fuzzyMode != null) {
            this.fuzzyMode.setVisibility(
                    this.bc.getInstalledUpgrades(Upgrades.FUZZY) > 0
                            && this.bc.getInstalledUpgrades(Upgrades.ORE_FILTER) == 0);
        }
        if (this.craftMode != null) {
            this.craftMode.setVisibility(this.bc.getInstalledUpgrades(Upgrades.CRAFTING) > 0);
        }
        if (this.schedulingMode != null) {
            this.schedulingMode.setVisibility(
                    this.bc.getInstalledUpgrades(Upgrades.CAPACITY) > 0 && this.bc instanceof PartExportBus);
        }
        if (this.oreFilter != null) {
            this.oreFilter.setVisibility(this.bc.getInstalledUpgrades(Upgrades.ORE_FILTER) > 0);
        }
    }

    protected String getBackground() {
        return "guis/bus.png";
    }

    protected boolean drawUpgrades() {
        return true;
    }

    protected GuiText getName() {
        return this.bc instanceof PartImportBus ? GuiText.ImportBus : GuiText.ExportBus;
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        final boolean backwards = Mouse.isButtonDown(1);

        if (btn == this.redstoneMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.redstoneMode.getSetting(), backwards));
        }

        if (btn == this.craftMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.craftMode.getSetting(), backwards));
        }

        if (btn == this.fuzzyMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.fuzzyMode.getSetting(), backwards));
        }

        if (btn == this.schedulingMode) {
            NetworkHandler.instance.sendToServer(new PacketConfigButton(this.schedulingMode.getSetting(), backwards));
        }

        if (btn == this.oreFilter) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis(GuiBridge.GUI_ORE_FILTER));
        }
    }

    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        return currentVisibility;
    }

    @Override
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return Collections.emptyList();
    }

    @Override
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return null;
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mouseX, int mouseY, ItemStack draggedStack, int button) {
        List<Pair<SlotFake, Integer>> slots = new ArrayList<>();

        if (this.inventorySlots.inventorySlots.size() > 0) {
            for (int i = 0; i < this.inventorySlots.inventorySlots.size(); i++) {
                Object slot = this.inventorySlots.inventorySlots.get(i);
                if (slot instanceof SlotFake) {
                    slots.add(new Pair<>((SlotFake) slot, i));
                }
            }
        }
        for (Pair<SlotFake, Integer> fakeSlotPair : slots) {
            SlotFake fakeSlot = fakeSlotPair.first();
            if (fakeSlot.isEnabled() && getSlotArea(fakeSlot).contains(mouseX, mouseY)) {
                fakeSlot.putStack(draggedStack);
                NetworkHandler.instance.sendToServer(new PacketNEIDragClick(draggedStack, fakeSlotPair.second()));
                if (draggedStack != null) {
                    draggedStack.stackSize = 0;
                }
                return true;
            }
        }
        if (draggedStack != null) {
            draggedStack.stackSize = 0;
        }
        return false;
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return false;
    }

    private Rectangle getSlotArea(SlotFake slot) {
        return new Rectangle(guiLeft + slot.getX(), guiTop + slot.getY(), 16, 16);
    }
}
