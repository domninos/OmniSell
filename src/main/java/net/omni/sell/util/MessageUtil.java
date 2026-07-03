package net.omni.sell.util;

import java.util.Arrays;

public class MessageUtil {

    private static ChatRenderer renderer;

    private MessageUtil() {
    }

    public static void init(ChatRenderer r) {
        renderer = r;
    }

    public static String parse(String msg) {
        return renderer.parse(msg);
    }

    public static String color(String msg) {
        return renderer.color(msg);
    }

    public static void append(String command, String description, StringBuilder builder) {
        builder.append(formatString(command, description));
    }

    public static String formatString(String command, String description, String... aliases) {
        StringBuilder builder = new StringBuilder();

        builder.append("  <#00AAFF>/")
                .append(command)
                .append("</#00AAFF> <dark_gray>-</dark_gray> <gray>")
                .append(description)
                .append("</gray>")
                .append("\n");

        if (aliases != null && aliases.length > 0)
            builder.append("  <white> <italic>⤷ Aliases: ")
                    .append(Arrays.toString(aliases))
                    .append("</italic></white>\n");

        return builder.toString();
    }
}