package carpetclient.gui;

import carpetclient.pluginchannel.CarpetPluginChannel;
import com.google.common.collect.ImmutableList;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.PacketBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.ValueType;

/*
This is a scrolling class for implementing scrollable GUI to minecraft.
"Based on code originally in WDL (by pokechu22), used with permission." - Pokechu22
For use of this code, ask permission from Pokechu22.
 */
public class ScrollGUI extends GuiScreen {
    private static final Logger LOGGER = LogManager.getLogger();
    private final GuiScreen parent;
    private GuiGameRuleList list;
    private GameRules rules;
    private String title;
    private final List<String> vanillaGameRules;
    private static final int SET_TEXT_FIELD = 0xE0E0E0, DEFAULT_TEXT_FIELD = 0x808080;
    @Nullable private String hoveredToolTip;

    public static void initGUI(GuiIngameMenu guiIngameMenu) {
        Minecraft.getMinecraft().displayGuiScreen(new ScrollGUI(guiIngameMenu));
    }

    public ScrollGUI(GuiScreen parent) {
        this.parent = parent;
        this.rules = Minecraft.getMinecraft().world.getGameRules();
        this.vanillaGameRules = ImmutableList.copyOf(rules.getRules());
    }

    public void initGui() {
        this.title = "Carpet Rules";
        this.list = new GuiGameRuleList();

        this.buttonList.add(new GuiButton(100, this.width / 2 - 100,
                this.height - 29, I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 100) {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    private class GuiGameRuleList extends GuiListExtended {

        @Nullable
        private String lastClickedRule = null;
        private final List<IGuiListEntry> entries = new ArrayList<>();

        public GuiGameRuleList() {
            super(ScrollGUI.this.mc, ScrollGUI.this.width, ScrollGUI.this.height, 39, ScrollGUI.this.height - 32, 24);

            for (String rule : vanillaGameRules) {
                if (rules.areSameType(rule, ValueType.NUMERICAL_VALUE)) {
                    this.entries.add(new TextRuleEntry(rule));
                } else if (rules.areSameType(rule, ValueType.BOOLEAN_VALUE)) {
                    this.entries.add(new ButtonRuleEntry(rule));
                } else {
                    LOGGER.debug("Couldn't identify type for vanilla game rule " + rule);
                }
            }
        }
        
        public void addNewButton(String str){
            this.entries.add(new ButtonRuleEntry(str));
        }

        @Override
        public int getListWidth() {
            return 180 * 2;
        }

        @Override
        protected int getScrollBarX() {
            return this.width / 2 + getListWidth() / 2 + 4;
        }

        public GuiGameRuleList(Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn) {
            super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
        }

        @Override
        public IGuiListEntry getListEntry(int index) {
            return entries.get(index);
        }

        @Override
        protected int getSize() {
            return entries.size();
        }

        public void update() {
            // Use a manual for loop to avoid concurrent modification exceptions
            for (int i = 0; i < getSize(); i++) {
                IGuiListEntry entry = getListEntry(i);
                if (entry instanceof KeyboardEntry) {
                    ((KeyboardEntry) entry).onUpdate();
                }
            }
        }

        public void keyDown(char typedChar, int keyCode) {
            // Use a manual for loop to avoid concurrent modification exceptions
            for (int i = 0; i < getSize(); i++) {
                IGuiListEntry entry = getListEntry(i);
                if (entry instanceof KeyboardEntry) {
                    ((KeyboardEntry) entry).keyDown(typedChar, keyCode);
                }
            }
        }

        private class ButtonRuleEntry extends RuleEntry {
            private GuiButton button;

            public ButtonRuleEntry(String ruleName) {
                super(ruleName);
                button = new GuiButton(0, 0, 0, 100, 20, "");
            }

            @Override
            protected void draw(int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, float partialTicks) {
                this.button.x = x + listWidth / 2;
                this.button.y = y;
                this.button.displayString = getRule(ruleName);
                button.drawButton(mc, mouseX, mouseY, partialTicks);
            }

            @Override
            protected boolean mouseDown(int x, int y, int button) {
                if (this.button.mousePressed(mc, x, y)) {
                    this.button.playPressSound(mc.getSoundHandler());
//                    boolean oldValue = getRule(ruleName).equals("true");
                    setRule(ruleName, "true");
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            protected void mouseUp(int x, int y, int button) {
                this.button.mouseReleased(x, y);
            }

//            @Override
//            protected boolean isMouseOverControl(int mouseX, int mouseY) {
//                return button.isMouseOver();
//            }

            @Override
            public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
                
            }
        }

        private class TextRuleEntry extends RuleEntry implements KeyboardEntry {
            private GuiNumericTextField field;

            public TextRuleEntry(String ruleName) {
                super(ruleName);
                field = new GuiNumericTextField(0, fontRenderer, 0, 0, 100, 20);
                field.setText(getRule(ruleName));
            }

            @Override
            public void draw(int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, float partialTicks) {
                if (!this.isFocused()) {
                    field.setFocused(false);
                }
                if (isRuleSet(this.ruleName)) {
                    field.setTextColor(SET_TEXT_FIELD);
                } else {
                    field.setTextColor(DEFAULT_TEXT_FIELD);
                }
                field.x = x + listWidth / 2;
                field.y = y;
                field.drawTextBox();
            }

            @Override
            protected boolean mouseDown(int x, int y, int button) {
                field.mouseClicked(x, y, button);
                return false;
            }

            @Override
            protected void mouseUp(int x, int y, int button) {
            }

            @Override
            public void onUpdate() {
                this.field.updateCursorCounter();
            }

            @Override
            public void keyDown(char typedChar, int keyCode) {
                if (this.field.textboxKeyTyped(typedChar, keyCode)) {
                    System.out.println("type " + keyCode + " keytypechar " + typedChar);
                    setRule(ruleName, Integer.toString(this.field.getValue()));
                }
            }

//            @Override
//            protected boolean isMouseOverControl(int mouseX, int mouseY) {
////                return isMouseOverTextBox(mouseX, mouseY, field);
//                return false;
//            }

            @Override
            protected void performResetAction() {
                super.performResetAction();
                this.field.setText(getRule(this.ruleName)); // Reset field text to default
            }

            @Override
            public void updatePosition(int slotIndex, int x, int y, float partialTicks) {
                
            }
        }

        private abstract class RuleEntry implements IGuiListEntry {
            @Nonnull
            protected final String ruleName;
            private GuiButton resetButton;
            private GuiButton infoButton;

            public RuleEntry(@Nonnull String ruleName) {
                this.ruleName = ruleName;
                this.resetButton = new GuiButton(0, 0, 0, 50, 20, "reset");
                this.infoButton = new GuiButton(0, 0, 0, 14, 15, "i");
            }

            @Override
            public final void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks) {
                drawString(fontRenderer, this.ruleName, x, y + 6, 0xFFFFFFFF);
                this.resetButton.x = x + listWidth / 2 + 110;
                this.resetButton.y = y;
                this.resetButton.enabled = isRuleSet(this.ruleName);
                resetButton.drawButton(mc, mouseX, mouseY, partialTicks);

                this.infoButton.x = x + listWidth / 2 - 17;
                this.infoButton.y = y + 2;
                infoButton.drawButton(mc, mouseX, mouseY, partialTicks);
                
                this.draw(x, y, listWidth, slotHeight, mouseX, mouseY, partialTicks);

//                if (this.isMouseOverControl(mouseX, mouseY)) {
//                    String key = ruleName;
//                    hoveredToolTip = ruleName;
////                    if (I18n.hasKey(key)) { // may return false for mods
////                        hoveredToolTip = I18n.format(key);
////                    }
//                }

                if (this.isMouseOverInfo(mouseX, mouseY)) {
                    hoveredToolTip = "Show info";
                }
            }

            @Override
            public final boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
                lastClickedRule = this.ruleName;

                if (resetButton.mousePressed(mc, mouseX, mouseY)) {
                    resetButton.playPressSound(mc.getSoundHandler());
                    this.performResetAction();
                    return true;
                }
                if (infoButton.mousePressed(mc, mouseX, mouseY)) {
                    infoButton.playPressSound(mc.getSoundHandler());
                    System.out.println("Show info box");
                    return true;
                }
                return mouseDown(mouseX, mouseY, mouseEvent);
            }

            @Override
            public final void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
                resetButton.mouseReleased(mouseX, mouseY);
                mouseUp(mouseX, mouseY, mouseEvent);
            }

            protected abstract void draw(int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, float partialTicks);

            protected abstract boolean mouseDown(int x, int y, int button);

            protected abstract void mouseUp(int x, int y, int button);

//            protected abstract boolean isMouseOverControl(int mouseX, int mouseY);
            
            protected boolean isMouseOverInfo(int mouseX, int mouseY) {
                return infoButton.isMouseOver();
            }

            protected boolean isFocused() {
                return lastClickedRule == this.ruleName;  // Ref equals
            }

            /**
             * Called when the reset button is clicked.
             */
            protected void performResetAction() {
                clearRule(this.ruleName);
            }
        }
    }

    private void setRule(@Nonnull String ruleName, @Nonnull String value) {
//        WDL.worldProps.setProperty("GameRule." + ruleName, value);
        PacketBuffer packetbuffer = new PacketBuffer(Unpooled.buffer());
        packetbuffer.writeString("hello");
        CarpetPluginChannel.packageSent(packetbuffer);
    }

    @Nullable private String getRule(@Nonnull String ruleName) {
//        if (isRuleSet(ruleName)) {
//            return WDL.worldProps.getProperty("GameRule." + ruleName);
//        } else {
            return rules.getString(ruleName);
//        }
    }

    private boolean isRuleSet(@Nonnull String ruleName) {
        return false;
    }

    private void clearRule(@Nonnull String ruleName) {
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        hoveredToolTip = null;
        this.list.drawScreen(mouseX, mouseY, partialTicks);

        super.drawScreen(mouseX, mouseY, partialTicks);
//
        this.drawCenteredString(fontRenderer, title, width / 2, 4, 0xFFFFFF);
//
        if (hoveredToolTip != null) {
            drawGuiInfoBox(hoveredToolTip,360, 168, width, height, 48);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws IOException {
        this.list.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.list.mouseReleased(mouseX, mouseY, state);
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.list.keyDown(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.list.handleMouseInput();
    }

    private static interface KeyboardEntry extends IGuiListEntry {
        public abstract void keyDown(char typedChar, int keyCode);

        public abstract void onUpdate();
    }

    public static void drawGuiInfoBox(String text, int infoBoxWidth,
                                      int infoBoxHeight, int guiWidth, int guiHeight, int bottomPadding) {
        if (text == null) {
            return;
        }

        int infoX = guiWidth / 2 - infoBoxWidth / 2;
        int infoY = guiHeight - bottomPadding - infoBoxHeight + 15;
        int y = infoY + 5;

        Gui.drawRect(infoX, infoY, infoX + infoBoxWidth, infoY
                + infoBoxHeight, 0xCF000000);

        List<String> lines = wordWrap(text, infoBoxWidth - 10);

        for (String s : lines) {
            Minecraft.getMinecraft().fontRenderer.drawString(s, infoX + 5, y, 0xFFFFFF);
            y += Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
        }
    }

    public static List<String> wordWrap(String s, int width) {
        s = s.replace("\r", ""); // If we got a \r\n in the text somehow, remove it.

        List<String> lines = Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(s, width);

        return lines;
    }

    public static boolean isMouseOverTextBox(int mouseX, int mouseY,
                                             GuiTextField textBox) {
        int scaledX = mouseX - textBox.x;
        int scaledY = mouseY - textBox.y;

        // Standard text box height -- there is no actual getter for the real
        // one.
        final int height = 20;

        return scaledX >= 0 && scaledX < textBox.getWidth() && scaledY >= 0
                && scaledY < height;
    }

    class GuiNumericTextField extends GuiTextField {
        public GuiNumericTextField(int id, FontRenderer fontRenderer,
                                   int x, int y, int width, int height) {
            super(id, fontRenderer, x, y, width,
                    height);
            setText("0");
        }

        /**
         * Last text that was successfully entered.
         */
        private String lastSafeText = "0";

        @Override
        public void drawTextBox() {
            // Save last safe text.
            try {
                Integer.parseInt("0" + getText());
                lastSafeText = getText();
            } catch (NumberFormatException e) {
                setText(lastSafeText);
            }

            super.drawTextBox();
        }

        /**
         * Gets the current value.
         * @return
         */
        public int getValue() {
            try {
                return Integer.parseInt("0" + getText());
            } catch (NumberFormatException e) {
                // Should not happen, hopefully.
                e.printStackTrace();
                return 0;
            }
        }

        /**
         * Sets the value.
         * @param value
         * @return
         */
        public void setValue(int value) {
            String text = String.valueOf(value);
            lastSafeText = text;
            setText(text);
        }

        @Override
        public String getText() {
            String text = super.getText();

            try {
                int value = Integer.parseInt("0" + text);
                return String.valueOf(value);
            } catch (NumberFormatException e) {
                setText(lastSafeText);
                return lastSafeText;
            }
        }

        @Override
        public void setText(String text) {
            String value;

            try {
                value = String.valueOf(Integer.parseInt("0" + text));
            } catch (NumberFormatException e) {
                value = lastSafeText;
            }

            super.setText(value);
            lastSafeText = value;
        }
    }
}