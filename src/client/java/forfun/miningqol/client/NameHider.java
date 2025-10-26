package forfun.miningqol.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Manages client-side name replacement with color and gradient support
 */
public class NameHider {
    private static boolean enabled = false;
    private static String replacementName = "Player";
    private static boolean useGradient = false;
    private static float red1 = 1.0f;
    private static float green1 = 1.0f;
    private static float blue1 = 1.0f;
    private static float red2 = 1.0f;
    private static float green2 = 1.0f;
    private static float blue2 = 1.0f;
    private static String playerUsername = null;

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setReplacementName(String name) {
        replacementName = name;
    }

    public static String getReplacementName() {
        return replacementName;
    }

    public static void setUseGradient(boolean value) {
        useGradient = value;
    }

    public static boolean isUsingGradient() {
        return useGradient;
    }

    public static void setColor1(float r, float g, float b) {
        red1 = r;
        green1 = g;
        blue1 = b;
    }

    public static void setColor2(float r, float g, float b) {
        red2 = r;
        green2 = g;
        blue2 = b;
    }

    public static float getRed1() { return red1; }
    public static float getGreen1() { return green1; }
    public static float getBlue1() { return blue1; }
    public static float getRed2() { return red2; }
    public static float getGreen2() { return green2; }
    public static float getBlue2() { return blue2; }

    /**
     * Get or cache the player's username
     */
    private static String getPlayerUsername() {
        if (playerUsername == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                playerUsername = client.player.getName().getString();
            }
        }
        return playerUsername;
    }

    /**
     * Reset cached username (call when switching worlds/servers)
     */
    public static void resetPlayerUsername() {
        playerUsername = null;
    }

    /**
     * Process a name text component and replace if it matches the player's name
     */
    public static Text processName(Text original) {
        if (!enabled || original == null) {
            return original;
        }

        String originalString = original.getString();
        String username = getPlayerUsername();

        // Case-insensitive check
        if (username == null || !originalString.toLowerCase().contains(username.toLowerCase())) {
            return original;
        }

        // Replace the player's username with the custom name
        if (useGradient) {
            return createGradientText(replacementName);
        } else {
            int color = rgbToInt(red1, green1, blue1);
            return Text.literal(replacementName).setStyle(original.getStyle().withColor(color));
        }
    }

    /**
     * Process text recursively, replacing username in all parts of the text tree
     * This preserves the text structure and formatting
     */
    public static Text processTextRecursive(Text original) {
        if (!enabled || original == null) {
            return original;
        }

        String username = getPlayerUsername();
        if (username == null) {
            return original;
        }

        String fullText = original.getString();
        // Case-insensitive check
        if (!fullText.toLowerCase().contains(username.toLowerCase())) {
            return original;
        }

        // Process the text tree recursively to preserve structure
        return processTextNode(original, username);
    }

    /**
     * Process a single text node and its children
     */
    private static Text processTextNode(Text text, String username) {
        // Get the literal content of this specific node (not including siblings)
        String literalContent = getNodeLiteralContent(text);

        // Process this node's content
        MutableText result;
        if (literalContent != null && literalContent.toLowerCase().contains(username.toLowerCase())) {
            // This node contains the username, replace it
            result = replaceInStringWithStyle(literalContent, username, text.getStyle());
        } else {
            // This node doesn't contain username, preserve it as-is
            result = Text.literal(literalContent != null ? literalContent : "").setStyle(text.getStyle());
        }

        // Process all siblings recursively
        for (Text sibling : text.getSiblings()) {
            result.append(processTextNode(sibling, username));
        }

        return result;
    }

    /**
     * Get the literal content of a text node (not including siblings)
     */
    private static String getNodeLiteralContent(Text text) {
        try {
            net.minecraft.text.TextContent content = text.getContent();
            // Use reflection to get the string value
            if (content.toString().isEmpty()) {
                return "";
            }
            // Try to get literal content
            String contentStr = content.toString();
            if (contentStr.startsWith("literal{") && contentStr.endsWith("}")) {
                return contentStr.substring(8, contentStr.length() - 1);
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Replace username in a string while preserving the original style for non-username parts
     */
    private static MutableText replaceInStringWithStyle(String text, String username, net.minecraft.text.Style originalStyle) {
        // Case-insensitive check
        if (!text.toLowerCase().contains(username.toLowerCase())) {
            return Text.literal(text).setStyle(originalStyle);
        }

        // Use case-insensitive regex pattern with CASE_INSENSITIVE flag
        String escapedUsername = java.util.regex.Pattern.quote(username);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(escapedUsername, java.util.regex.Pattern.CASE_INSENSITIVE);
        String[] parts = pattern.split(text, -1);

        if (parts.length == 1) {
            // No replacement needed
            return Text.literal(text).setStyle(originalStyle);
        }

        MutableText result = Text.literal("");

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                // Preserve original style for non-username parts
                result.append(Text.literal(parts[i]).setStyle(originalStyle));
            }
            if (i < parts.length - 1) {
                // Add the replacement name with custom styling
                if (useGradient) {
                    result.append(createGradientText(replacementName));
                } else {
                    int color = rgbToInt(red1, green1, blue1);
                    result.append(Text.literal(replacementName).setStyle(
                        net.minecraft.text.Style.EMPTY.withColor(color)
                    ));
                }
            }
        }

        return result;
    }

    /**
     * Create gradient text by interpolating between two colors
     */
    private static Text createGradientText(String text) {
        if (text.isEmpty()) {
            return Text.literal("");
        }

        MutableText result = Text.literal("");
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = length == 1 ? 0.5f : (float) i / (length - 1);
            float r = red1 + (red2 - red1) * ratio;
            float g = green1 + (green2 - green1) * ratio;
            float b = blue1 + (blue2 - blue1) * ratio;

            int color = rgbToInt(r, g, b);
            result.append(Text.literal(String.valueOf(text.charAt(i))).setStyle(
                net.minecraft.text.Style.EMPTY.withColor(color)
            ));
        }

        return result;
    }

    /**
     * Convert RGB float values (0-1) to integer color
     */
    private static int rgbToInt(float r, float g, float b) {
        int ri = Math.max(0, Math.min(255, (int)(r * 255)));
        int gi = Math.max(0, Math.min(255, (int)(g * 255)));
        int bi = Math.max(0, Math.min(255, (int)(b * 255)));
        return (ri << 16) | (gi << 8) | bi;
    }

    /**
     * Get hex color string for display in GUI
     */
    public static String getColor1Hex() {
        return String.format("#%02X%02X%02X",
            (int)(red1 * 255), (int)(green1 * 255), (int)(blue1 * 255));
    }

    public static String getColor2Hex() {
        return String.format("#%02X%02X%02X",
            (int)(red2 * 255), (int)(green2 * 255), (int)(blue2 * 255));
    }
}
