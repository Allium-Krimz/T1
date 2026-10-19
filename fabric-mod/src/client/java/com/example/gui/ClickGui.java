package com.example.gui;

import com.example.modules.Module;
import com.example.modules.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact dark click GUI inspired by minimal client menus. */
public final class ClickGui extends Screen {
    private static final int PANEL_WIDTH = 600;
    private static final int PANEL_HEIGHT = 390;
    private static final int SIDEBAR_WIDTH = 150;
    private static final int ROW_HEIGHT = 47;

    private static final int ACCENT = 0xFFF2F5F8;
    private static final int PANEL = 0xF50D1216;
    private static final int HEADER = 0xFF161B21;
    private static final int SIDEBAR = 0xFF0F1419;
    private static final int ROW = 0xFF171D24;
    private static final int ROW_HOVER = 0xFF222A33;
    private static final int MUTED = 0xFF8A95A1;
    private static final int WHITE = 0xFFFFFFFF;

    /** The T1 mark as filled rectangles {x1, y1, x2, y2} on a 34x22 grid. */
    private static final int[][] LOGO = {
            {0, 0, 23, 2},
            {26, 0, 34, 2},
            {0, 2, 22, 3},
            {25, 2, 34, 3},
            {0, 3, 21, 4},
            {24, 3, 34, 5},
            {0, 4, 20, 5},
            {8, 5, 13, 22},
            {29, 5, 34, 22}
    };

    private static void drawLogo(GuiGraphics graphics, int originX, int originY) {
        for (int[] r : LOGO) {
            graphics.fill(originX + r[0], originY + r[1], originX + r[2], originY + r[3], WHITE);
        }
    }

    private String selectedCategory = "All";
    private String search = "";
    private boolean searchFocused;
    private int scrollOffset;

    public ClickGui() {
        super(Component.literal("T1 Client"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xA603060A);

        int x = panelX();
        int y = panelY();
        int w = panelWidth();
        int h = panelHeight();
        int contentX = x + SIDEBAR_WIDTH + 18;
        int contentW = w - SIDEBAR_WIDTH - 36;

        graphics.fill(x, y, x + w, y + h, PANEL);
        graphics.fill(x, y, x + w, y + 70, HEADER);
        graphics.fill(x, y + 70, x + SIDEBAR_WIDTH, y + h, SIDEBAR);
        graphics.fill(x, y + 69, x + w, y + 70, 0xFF2E3740);
        graphics.fill(x + SIDEBAR_WIDTH - 1, y + 70, x + SIDEBAR_WIDTH, y + h, 0xFF2E3740);

        drawLogo(graphics, x + 20, y + 16);
        graphics.drawString(font, "CLIENT", x + 64, y + 16, WHITE);
        graphics.drawString(font, "Minecraft 1.21.11  /  Fabric", x + 64, y + 33, MUTED);

        int closeX = x + w - 27;
        int closeY = y + 18;
        graphics.fill(closeX - 4, closeY - 4, closeX + 14, closeY + 14,
                inside(mouseX, mouseY, closeX - 4, closeY - 4, closeX + 14, closeY + 14) ? 0xFF3A2A30 : 0xFF1E252C);
        graphics.drawString(font, "x", closeX + 2, closeY + 1, WHITE);

        drawSidebar(graphics, x, y, mouseX, mouseY);

        int searchY = y + 84;
        int searchBottom = searchY + 25;
        graphics.fill(contentX, searchY, contentX + contentW, searchBottom,
                searchFocused ? 0xFF222A33 : 0xFF1A2027);
        graphics.fill(contentX, searchY, contentX + 2, searchBottom, searchFocused ? ACCENT : 0xFF46515C);
        String searchLabel = search.isEmpty() && !searchFocused ? "Search modules..." : "> " + search;
        graphics.drawString(font, searchLabel, contentX + 10, searchY + 8,
                search.isEmpty() && !searchFocused ? MUTED : WHITE);

        List<Module> visibleModules = filteredModules();
        int listTop = y + 123;
        int listBottom = y + h - 36;
        int availableRows = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, visibleModules.size() - availableRows)));

        graphics.drawString(font, selectedCategory.toUpperCase(Locale.ROOT), contentX, y + 116, MUTED);
        graphics.drawString(font, visibleModules.size() + " MODULE" + (visibleModules.size() == 1 ? "" : "S"),
                contentX + contentW - font.width(visibleModules.size() + " MODULE" + (visibleModules.size() == 1 ? "" : "S")),
                y + 116, MUTED);

        for (int row = 0; row < availableRows; row++) {
            int itemIndex = scrollOffset + row;
            if (itemIndex >= visibleModules.size()) break;
            Module module = visibleModules.get(itemIndex);
            int rowY = listTop + row * ROW_HEIGHT;
            int rowBottom = rowY + ROW_HEIGHT - 5;
            boolean hovered = inside(mouseX, mouseY, contentX, rowY, contentX + contentW, rowBottom);
            graphics.fill(contentX, rowY, contentX + contentW, rowBottom,
                    hovered ? ROW_HOVER : ROW);
            graphics.fill(contentX, rowY, contentX + 3, rowBottom, module.isEnabled() ? ACCENT : 0xFF2E3740);
            graphics.drawString(font, module.getName(), contentX + 12, rowY + 8, WHITE);
            String description = module.getDescription();
            int descMaxWidth = Math.max(50, contentW - 75);
            while (font.width(description) > descMaxWidth && description.length() > 4) {
                description = description.substring(0, description.length() - 2);
            }
            if (!description.equals(module.getDescription())) description += "…";
            graphics.drawString(font, description, contentX + 12, rowY + 25, MUTED);

            int toggleX = contentX + contentW - 48;
            int toggleY = rowY + 14;
            graphics.fill(toggleX, toggleY, toggleX + 34, toggleY + 17,
                    module.isEnabled() ? 0xFF5F7080 : 0xFF2A323B);
            int knobX = module.isEnabled() ? toggleX + 19 : toggleX + 2;
            graphics.fill(knobX, toggleY + 2, knobX + 13, toggleY + 15, WHITE);
        }

        graphics.fill(x, y + h - 25, x + w, y + h, 0xFF0D1216);
        graphics.drawString(font, "RIGHT SHIFT  •  CLOSE", x + 17, y + h - 16, MUTED);
        graphics.drawString(font, "Settings save automatically", x + w - 179, y + h - 16, MUTED);
    }

    private void drawSidebar(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        String[] categories = { "All", "Render", "World" };
        int cy = y + 91;
        for (String category : categories) {
            int rowY = cy;
            int rowBottom = rowY + 31;
            boolean selected = selectedCategory.equals(category);
            boolean hovered = inside(mouseX, mouseY, x + 10, rowY, x + SIDEBAR_WIDTH - 10, rowBottom);
            if (selected) {
                graphics.fill(x + 10, rowY, x + SIDEBAR_WIDTH - 10, rowBottom, 0xFF222A33);
                graphics.fill(x + 10, rowY, x + 13, rowBottom, ACCENT);
            } else if (hovered) {
                graphics.fill(x + 10, rowY, x + SIDEBAR_WIDTH - 10, rowBottom, 0xFF151B21);
            }
            graphics.drawString(font, category, x + 23, rowY + 11, selected ? WHITE : MUTED);
            cy += 37;
        }

        int infoY = y + panelHeight() - 72;
        graphics.fill(x + 16, infoY, x + SIDEBAR_WIDTH - 16, infoY + 1, 0xFF2E3740);
        graphics.drawString(font, "T1 BUILD", x + 20, infoY + 12, ACCENT);
        graphics.drawString(font, "1.2.0", x + 20, infoY + 27, MUTED);
    }

    private List<Module> filteredModules() {
        List<Module> result = new ArrayList<>();
        String query = search.toLowerCase(Locale.ROOT).trim();
        for (Module module : ModuleManager.getModules()) {
            if (!selectedCategory.equals("All") && !module.getCategory().equalsIgnoreCase(selectedCategory)) continue;
            if (!query.isEmpty() && !module.getName().toLowerCase(Locale.ROOT).contains(query)
                    && !module.getDescription().toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(module);
        }
        return result;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int x = panelX();
        int y = panelY();
        int w = panelWidth();
        int h = panelHeight();

        if (inside(mouseX, mouseY, x + w - 32, y + 12, x + w - 8, y + 38)) {
            onClose();
            return true;
        }

        int cy = y + 91;
        for (String category : new String[]{"All", "Render", "World"}) {
            if (inside(mouseX, mouseY, x + 10, cy, x + SIDEBAR_WIDTH - 10, cy + 31)) {
                selectedCategory = category;
                scrollOffset = 0;
                return true;
            }
            cy += 37;
        }

        int contentX = x + SIDEBAR_WIDTH + 18;
        int contentW = w - SIDEBAR_WIDTH - 36;
        int searchY = y + 84;
        if (inside(mouseX, mouseY, contentX, searchY, contentX + contentW, searchY + 25)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        List<Module> visibleModules = filteredModules();
        int listTop = y + 123;
        int listBottom = y + h - 36;
        int availableRows = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        for (int row = 0; row < availableRows; row++) {
            int itemIndex = scrollOffset + row;
            if (itemIndex >= visibleModules.size()) break;
            int rowY = listTop + row * ROW_HEIGHT;
            if (inside(mouseX, mouseY, contentX, rowY, contentX + contentW, rowY + ROW_HEIGHT - 5)) {
                visibleModules.get(itemIndex).toggle();
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<Module> visible = filteredModules();
        int availableRows = Math.max(1, (panelHeight() - 159) / ROW_HEIGHT);
        int maxScroll = Math.max(0, visible.size() - availableRows);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) Math.signum(verticalAmount)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        if (searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                scrollOffset = 0;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused && event.isAllowedChatCharacter() && search.length() < 40) {
            search += event.codepointAsString();
            scrollOffset = 0;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        searchFocused = false;
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelWidth() { return Math.min(PANEL_WIDTH, Math.max(330, width - 24)); }
    private int panelHeight() { return Math.min(PANEL_HEIGHT, Math.max(250, height - 24)); }
    private int panelX() { return (width - panelWidth()) / 2; }
    private int panelY() { return (height - panelHeight()) / 2; }

    private boolean inside(double px, double py, double left, double top, double right, double bottom) {
        return px >= left && px <= right && py >= top && py <= bottom;
    }
}
