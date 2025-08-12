package com.lhamacorp.games.tlob.client.managers.renderers;

import com.lhamacorp.games.tlob.client.entities.Player;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public final class HudRenderer {

    private final Font bodyFont;
    
    // Animation and visual constants
    private static final int BAR_HEIGHT = 15;
    private static final int BAR_WIDTH = 150;
    private static final int BAR_SPACING = 8;
    private static final int CORNER_RADIUS = 8;
    private static final int BORDER_WIDTH = 2;
    
    // Color schemes
    private static final Color HP_BG_COLOR = new Color(40, 20, 20, 180);
    private static final Color HP_FILL_COLOR = new Color(220, 60, 60);
    private static final Color HP_FILL_COLOR_LOW = new Color(255, 100, 100);
    private static final Color HP_BORDER_COLOR = new Color(80, 40, 40);
    
    private static final Color SHIELD_OVERLAY_COLOR = new Color(255, 255, 255, 200);
    private static final Color SHIELD_BORDER_COLOR = new Color(200, 200, 200);
    private static final Color SHIELD_PATTERN_COLOR = new Color(220, 220, 220, 150);
    
    private static final Color STAMINA_BG_COLOR = new Color(40, 40, 20, 180);
    private static final Color STAMINA_FILL_COLOR = new Color(255, 255, 100);
    private static final Color STAMINA_FILL_COLOR_LOW = new Color(255, 200, 50);
    private static final Color STAMINA_BORDER_COLOR = new Color(80, 80, 40);
    
    private static final Color MANA_BG_COLOR = new Color(20, 20, 40, 180);
    private static final Color MANA_FILL_COLOR = new Color(100, 150, 255);
    private static final Color MANA_BORDER_COLOR = new Color(60, 60, 120);
    
    // Animation variables
    private long lastUpdateTime = System.currentTimeMillis();
    private double pulsePhase = 0.0;

    public HudRenderer(Font bodyFont) {
        this.bodyFont = bodyFont;
    }

    public void draw(Graphics2D g2, Player player, int x, int y) {
        // Update animation time
        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastUpdateTime) / 1000.0;
        lastUpdateTime = currentTime;
        
        // Update animation phases
        pulsePhase += deltaTime * 3.0; // 3 Hz pulse
        
        // Enable anti-aliasing for smoother bars
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        g2.setFont(bodyFont);
        
        int nextY = drawHpShieldBar(g2, player, x, y);
        nextY = drawStaminaBar(g2, player, x, nextY);
        nextY = drawManaBar(g2, player, x, nextY);
        
        // Reset rendering hints
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
    }

    private int drawHpShieldBar(Graphics2D g2, Player player, int x, int y) {
        double health = player.getHealth();
        double maxHealth = player.getMaxHealth();
        double shield = player.getShield();
        double maxShield = player.getMaxShield();
        
        // Draw HP bar background
        RoundRectangle2D background = new RoundRectangle2D.Double(
            x, y, BAR_WIDTH, BAR_HEIGHT, CORNER_RADIUS, CORNER_RADIUS
        );
        g2.setColor(HP_BG_COLOR);
        g2.fill(background);
        
        // Draw border
        g2.setColor(HP_BORDER_COLOR);
        g2.setStroke(new BasicStroke(BORDER_WIDTH));
        g2.draw(background);
        
        // Calculate health fill
        double healthRatio = Math.max(0, Math.min(1, health / maxHealth));
        int healthWidth = (int) (healthRatio * BAR_WIDTH);
        
        // Draw health fill with gradient
        if (healthWidth > 0) {
            Color healthColor = healthRatio < 0.3 ? HP_FILL_COLOR_LOW : HP_FILL_COLOR;
            drawGradientFill(g2, x, y, healthWidth, BAR_HEIGHT, 
                           healthColor, healthColor.darker(), CORNER_RADIUS);
        }
        
        // Draw shield overlay within the HP bar if shield exists
        if (maxShield > 0 && shield > 0) {
            double shieldRatio = Math.max(0, Math.min(1, shield / maxShield));
            int shieldWidth = (int) (shieldRatio * BAR_WIDTH);
            
            if (shieldWidth > 0) {
                // Draw shield as a white overlay that starts from the right and extends leftward
                // Shield starts from the right side of the HP bar and extends left
                int shieldEndX = x + BAR_WIDTH; // Right edge of the bar
                int shieldStartX = shieldEndX - shieldWidth; // Left edge of shield
                
                // Ensure shield doesn't go beyond the left edge
                shieldStartX = Math.max(x, shieldStartX);
                
                if (shieldStartX < shieldEndX) {
                    // Create shield overlay rectangle
                    RoundRectangle2D shieldOverlay = new RoundRectangle2D.Double(
                        shieldStartX, y, shieldEndX - shieldStartX, BAR_HEIGHT, 
                        CORNER_RADIUS, CORNER_RADIUS
                    );
                    
                    // Draw shield overlay with white color
                    g2.setColor(SHIELD_OVERLAY_COLOR);
                    g2.fill(shieldOverlay);
                    
                    // Draw shield pattern for better visibility
                    g2.setColor(SHIELD_PATTERN_COLOR);
                    g2.setStroke(new BasicStroke(2));
                    
                    // Draw diagonal lines pattern on shield (right to left)
                    int patternSpacing = 8;
                    for (int i = 0; i < shieldEndX - shieldStartX; i += patternSpacing) {
                        int lineX = shieldStartX + i;
                        if (lineX < shieldEndX) {
                            g2.drawLine(lineX, y, lineX + 4, y + BAR_HEIGHT);
                        }
                    }
                    
                    // Draw shield border
                    g2.setColor(SHIELD_BORDER_COLOR);
                    g2.setStroke(new BasicStroke(2));
                    g2.draw(shieldOverlay);
                }
            }
        }
        
        // Draw health text
        g2.setColor(Color.WHITE);
        g2.setFont(bodyFont.deriveFont(Font.BOLD, 12f));
        String healthText = String.format("%.1f/%.1f", health, maxHealth);
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (BAR_WIDTH - fm.stringWidth(healthText)) / 2;
        int textY = y + (BAR_HEIGHT + fm.getAscent()) / 2;
        
        // Draw text shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.drawString(healthText, textX + 1, textY + 1);
        
        // Draw main text
        g2.setColor(Color.WHITE);
        g2.drawString(healthText, textX, textY);
        
        // Draw shield text if shield exists
        if (maxShield > 0 && shield > 0) {
            String shieldText = String.format("+%.1f", shield);
            int shieldTextX = x + BAR_WIDTH - fm.stringWidth(shieldText) - 5;
            int shieldTextY = y + (BAR_HEIGHT + fm.getAscent()) / 2;
            
            // Draw shield text shadow
            g2.setColor(new Color(0, 0, 0, 100));
            g2.drawString(shieldText, shieldTextX + 1, shieldTextY + 1);
            
            // Draw shield text in white color to match the overlay
            g2.setColor(Color.WHITE);
            g2.drawString(shieldText, shieldTextX, shieldTextY);
        }
        
        // Add pulsing effect when health is low
        if (healthRatio < 0.3) {
            double pulse = (Math.sin(pulsePhase) + 1) * 0.3 + 0.7;
            g2.setColor(new Color(255, 255, 255, (int)(50 * pulse)));
            g2.setStroke(new BasicStroke(3));
            g2.draw(background);
        }
        
        return y + BAR_HEIGHT + BAR_SPACING;
    }

    private int drawStaminaBar(Graphics2D g2, Player player, int x, int y) {
        double stamina = player.getStamina();
        double maxStamina = player.getMaxStamina();
        
        if (maxStamina <= 0) return y;
        
        // Draw background
        RoundRectangle2D background = new RoundRectangle2D.Double(
            x, y, BAR_WIDTH, BAR_HEIGHT, CORNER_RADIUS, CORNER_RADIUS
        );
        g2.setColor(STAMINA_BG_COLOR);
        g2.fill(background);
        
        // Draw border
        g2.setColor(STAMINA_BORDER_COLOR);
        g2.setStroke(new BasicStroke(BORDER_WIDTH));
        g2.draw(background);
        
        // Calculate stamina fill
        double staminaRatio = Math.max(0, Math.min(1, stamina / maxStamina));
        int staminaWidth = (int) (staminaRatio * BAR_WIDTH);
        
        // Draw stamina fill with gradient
        if (staminaWidth > 0) {
            Color staminaColor = staminaRatio < 0.2 ? STAMINA_FILL_COLOR_LOW : STAMINA_FILL_COLOR;
            drawGradientFill(g2, x, y, staminaWidth, BAR_HEIGHT,
                           staminaColor, staminaColor.darker(), CORNER_RADIUS);
        }
        
        // Draw stamina text
        g2.setColor(Color.WHITE);
        g2.setFont(bodyFont.deriveFont(Font.BOLD, 12f));
        String staminaText = String.format("%.1f/%.1f", stamina, maxStamina);
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (BAR_WIDTH - fm.stringWidth(staminaText)) / 2;
        int textY = y + (BAR_HEIGHT + fm.getAscent()) / 2;
        
        // Draw text shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.drawString(staminaText, textX + 1, textY + 1);
        
        // Draw main text
        g2.setColor(Color.WHITE);
        g2.drawString(staminaText, textX, textY);
        
        // Add energy effect when stamina is low
        if (staminaRatio < 0.2) {
            double energy = (Math.sin(pulsePhase * 2) + 1) * 0.4 + 0.6;
            g2.setColor(new Color(255, 255, 100, (int)(60 * energy)));
            g2.setStroke(new BasicStroke(2));
            g2.draw(background);
        }
        
        return y + BAR_HEIGHT + BAR_SPACING;
    }

    private int drawManaBar(Graphics2D g2, Player player, int x, int y) {
        double mana = player.getMana();
        double maxMana = player.getMaxMana();
        
        if (maxMana <= 0) return y;
        
        // Draw background
        RoundRectangle2D background = new RoundRectangle2D.Double(
            x, y, BAR_WIDTH, BAR_HEIGHT, CORNER_RADIUS, CORNER_RADIUS
        );
        g2.setColor(MANA_BG_COLOR);
        g2.fill(background);
        
        // Draw border
        g2.setColor(MANA_BORDER_COLOR);
        g2.setStroke(new BasicStroke(BORDER_WIDTH));
        g2.draw(background);
        
        // Calculate mana fill
        double manaRatio = Math.max(0, Math.min(1, mana / maxMana));
        int manaWidth = (int) (manaRatio * BAR_WIDTH);
        
        // Draw mana fill with gradient
        if (manaWidth > 0) {
            drawGradientFill(g2, x, y, manaWidth, BAR_HEIGHT,
                           MANA_FILL_COLOR, MANA_FILL_COLOR.darker(), CORNER_RADIUS);
        }
        
        // Draw mana text
        g2.setColor(Color.WHITE);
        g2.setFont(bodyFont.deriveFont(Font.BOLD, 12f));
        String manaText = String.format("%.1f/%.1f", mana, maxMana);
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + (BAR_WIDTH - fm.stringWidth(manaText)) / 2;
        int textY = y + (BAR_HEIGHT + fm.getAscent()) / 2;
        
        // Draw text shadow
        g2.setColor(new Color(0, 0, 0, 100));
        g2.drawString(manaText, textX + 1, textY + 1);
        
        // Draw main text
        g2.setColor(Color.WHITE);
        g2.drawString(manaText, textX, textY);
        
        // Add dash indicator when mana is sufficient
        if (mana >= 2.0) {
            // Draw a small dash icon or indicator
            g2.setColor(new Color(255, 255, 255, 150));
            g2.setStroke(new BasicStroke(2));
            
            // Draw a simple dash symbol (two horizontal lines)
            int dashX = x + BAR_WIDTH - 25;
            int dashY = y + BAR_HEIGHT / 2;
            g2.drawLine(dashX, dashY - 3, dashX + 8, dashY - 3);
            g2.drawLine(dashX, dashY + 3, dashX + 8, dashY + 3);
        }
        
        return y + BAR_HEIGHT + BAR_SPACING;
    }

    private void drawGradientFill(Graphics2D g2, int x, int y, int width, int height, 
                                 Color startColor, Color endColor, int cornerRadius) {
        if (width <= 0) return;
        
        // Create gradient paint
        GradientPaint gradient = new GradientPaint(
            x, y, startColor,
            x + width, y, endColor
        );
        
        // Create rounded rectangle for the fill
        RoundRectangle2D fillRect = new RoundRectangle2D.Double(
            x, y, width, height, cornerRadius, cornerRadius
        );
        
        // Apply gradient and fill
        g2.setPaint(gradient);
        g2.fill(fillRect);
    }
}
