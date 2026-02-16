
package com.lhamacorp.games.tlob.client.managers.renderers;

import com.lhamacorp.games.tlob.client.entities.Player;
import com.lhamacorp.games.tlob.client.weapons.Weapon;
import com.lhamacorp.games.tlob.client.weapons.Weapon.WeaponType;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Renders the inventory page that displays current weapon and player stats.
 * Appears when the 'I' key is pressed. Combines weapon information with key player stats.
 */
public class InventoryRenderer {

    private final Font titleFont;
    private final Font statFont;
    private final Font valueFont;
    private final Font sectionFont;

    private static final Color BACKGROUND_COLOR = new Color(20, 20, 30, 220);
    private static final Color BORDER_COLOR = new Color(100, 150, 255, 180);
    private static final Color TITLE_COLOR = new Color(255, 255, 255);
    private static final Color STAT_COLOR = new Color(200, 200, 200);
    private static final Color VALUE_COLOR = new Color(100, 255, 150);
    private static final Color SECTION_COLOR = new Color(150, 200, 255);
    private static final Color EQUIPPED_COLOR = new Color(255, 200, 100);
    private static final Color NOT_EQUIPPED_COLOR = new Color(100, 100, 100);

    // Weapon type colors
    private static final Color SWORD_COLOR = new Color(255, 100, 100);
    private static final Color BOW_COLOR = new Color(100, 150, 255);
    private static final Color DEFAULT_WEAPON_COLOR = new Color(200, 200, 200);

    /**
     * Creates a new InventoryRenderer with the specified fonts.
     */
    public InventoryRenderer(Font titleFont, Font statFont, Font valueFont, Font sectionFont) {
        this.titleFont = titleFont;
        this.statFont = statFont;
        this.valueFont = valueFont;
        this.sectionFont = sectionFont;
    }

    /**
     * Creates a new InventoryRenderer with default fonts.
     */
    public InventoryRenderer() {
        this(
            new Font("Arial", Font.BOLD, 20),
            new Font("Arial", Font.PLAIN, 14),
            new Font("Arial", Font.BOLD, 14),
            new Font("Arial", Font.BOLD, 16)
        );
    }

    /**
     * Draws the inventory page for the given player.
     */
    public void draw(Graphics2D g2, Player player) {
        if (player == null) return;

        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate dimensions
        int width = 460;
        int height = 540;
        Rectangle clipBounds = g.getClipBounds();
        if (clipBounds == null) {
            clipBounds = new Rectangle(0, 0, 1280, 720); // Default fallback
        }
        int x = (clipBounds.width - width) / 2;
        int y = (clipBounds.height - height) / 2;

        // Draw background
        drawBackground(g, x, y, width, height);

        // Draw title
        drawTitle(g, x, y, width);

        // Draw sections
        int currentY = y + 50;
        currentY = drawWeaponListSection(g, x, y, width, currentY, player);
        currentY = drawCurrentWeaponStats(g, x, y, width, currentY, player);
        currentY = drawPlayerStatsSection(g, x, y, width, currentY, player);

        g.dispose();
    }

    private void drawBackground(Graphics2D g, int x, int y, int width, int height) {
        // Background
        g.setColor(BACKGROUND_COLOR);
        g.fill(new RoundRectangle2D.Double(x, y, width, height, 20, 20));

        // Border
        g.setStroke(new BasicStroke(2));
        g.setColor(BORDER_COLOR);
        g.draw(new RoundRectangle2D.Double(x, y, width, height, 20, 20));
    }

    private void drawTitle(Graphics2D g, int x, int y, int width) {
        g.setFont(titleFont);
        g.setColor(TITLE_COLOR);

        String title = "INVENTORY";
        FontMetrics fm = g.getFontMetrics();
        int titleX = x + (width - fm.stringWidth(title)) / 2;
        int titleY = y + fm.getAscent() + 20;

        g.drawString(title, titleX, titleY);
    }

    private int drawWeaponListSection(Graphics2D g, int x, int y, int width, int startY, Player player) {
        g.setFont(sectionFont);
        g.setColor(SECTION_COLOR);
        g.drawString("AVAILABLE WEAPONS", x + 20, startY);

        int currentY = startY + 25;

        if (player.getInventory() == null || player.getInventory().getWeaponCount() == 0) {
            g.setFont(statFont);
            g.setColor(STAT_COLOR);
            g.drawString("No weapons available", x + 30, currentY);
            return currentY + 20;
        }

        // List all weapons with key bindings
        int equippedIndex = player.getInventory().getEquippedIndex();
        for (int i = 0; i < player.getInventory().getWeaponCount(); i++) {
            Weapon w = player.getInventory().getCurrentWeapon();
            if (i == 0 && w == null) continue;

            // Get weapon by switching temporarily to check it (or better way to iterate)
            String weaponName = "Weapon " + i;
            if (i == 0) weaponName = "Sword (Press 1)";
            if (i == 1) weaponName = "Bow (Press 2)";

            if (i == equippedIndex) {
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.setColor(EQUIPPED_COLOR);
                g.drawString("► " + weaponName, x + 30, currentY);
            } else {
                g.setFont(statFont);
                g.setColor(NOT_EQUIPPED_COLOR);
                g.drawString(weaponName, x + 30, currentY);
            }
            currentY += 20;
        }

        return currentY + 10;
    }

    private int drawCurrentWeaponStats(Graphics2D g, int x, int y, int width, int startY, Player player) {
        g.setFont(sectionFont);
        g.setColor(SECTION_COLOR);
        g.drawString("WEAPON STATS", x + 20, startY);

        int currentY = startY + 25;

        Weapon weapon = player.getWeapon();
        if (weapon == null) {
            g.setFont(statFont);
            g.setColor(STAT_COLOR);
            g.drawString("No weapon equipped", x + 30, currentY);
            return currentY + 20;
        }

        // Weapon name with type color
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(getWeaponTypeColor(weapon.getType()));
        g.drawString(weapon.getName(), x + 30, currentY);

        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Damage:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.valueOf(weapon.getDamage()), x + 210, currentY);

        currentY += 20;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Reach:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.valueOf(weapon.getReach()), x + 210, currentY);

        currentY += 20;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Width:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.valueOf(weapon.getWidth()), x + 210, currentY);

        currentY += 20;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Duration:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%d ticks", weapon.getDuration()), x + 210, currentY);

        currentY += 20;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Cooldown:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%d ticks", weapon.getCooldown()), x + 210, currentY);

        return currentY + 15;
    }

    private int drawPlayerStatsSection(Graphics2D g, int x, int y, int width, int startY, Player player) {
        g.setFont(sectionFont);
        g.setColor(SECTION_COLOR);
        g.drawString("PLAYER STATS", x + 20, startY);

        int currentY = startY + 25;

        // Health
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Health:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1f / %.1f", player.getHealth(), player.getMaxHealth()), x + 150, currentY);

        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Armor:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1f", player.getArmor()), x + 150, currentY);

        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Stamina:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1f / %.1f", player.getStamina(), player.getMaxStamina()), x + 150, currentY);

        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Mana:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1f / %.1f", player.getMana(), player.getMaxMana()), x + 150, currentY);

        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Damage Multiplier:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        g.drawString(String.format("%.1fx", player.getDamageMultiplier()), x + 150, currentY);

        currentY += 22;
        g.setFont(statFont);
        g.setColor(STAT_COLOR);
        g.drawString("Effective Speed:", x + 30, currentY);
        g.setFont(valueFont);
        g.setColor(VALUE_COLOR);
        double effectiveSpeed = player.getSpeed() * player.getSpeedMultiplier();
        g.drawString(String.format("%.1f", effectiveSpeed), x + 150, currentY);

        return currentY + 15;
    }

    /**
     * Gets the color for a specific weapon type.
     */
    private Color getWeaponTypeColor(WeaponType type) {
        return switch (type) {
            case SWORD -> SWORD_COLOR;
            case BOW -> BOW_COLOR;
            default -> DEFAULT_WEAPON_COLOR;
        };
    }

}
