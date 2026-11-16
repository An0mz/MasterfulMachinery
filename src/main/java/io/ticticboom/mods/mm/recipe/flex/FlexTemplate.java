package io.ticticboom.mods.mm.recipe.flex;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public record FlexTemplate(Pattern match, String item) {

    public static FlexTemplate parse(@Nullable String match, @Nullable String item) {
        if (item == null || item.isBlank()) {
            throw new RuntimeException("Flex entry needs an 'item' template, e.g. \"{ns}:$1_essence\"");
        }
        try {
            return new FlexTemplate(Pattern.compile(match == null || match.isEmpty() ? "(.*)" : match), item);
        } catch (PatternSyntaxException e) {
            throw new RuntimeException("Flex 'match' is not a valid regex: " + match, e);
        }
    }

    @Nullable
    public String apply(String namespace, String path) {
        Matcher m = match.matcher(path);
        if (!m.matches()) {
            return null;
        }
        String out = item.replace("{ns}", namespace).replace("{path}", path);
        for (int g = m.groupCount(); g >= 1; g--) {
            String group = m.group(g);
            out = out.replace("$" + g, group == null ? "" : group);
        }
        return out;
    }
}
