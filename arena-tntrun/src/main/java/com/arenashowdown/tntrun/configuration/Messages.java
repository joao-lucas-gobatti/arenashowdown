package com.arenashowdown.tntrun.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

/** Immutable message bundle captured with each match. Plain text, explicit placeholders. */
public record Messages(Map<String,String> values) {
    public Messages { values=Map.copyOf(values); }
    public String text(String key) {
        String value=values.get(key);
        if (value==null) throw new IllegalArgumentException("Unknown message: "+key);
        return value;
    }
    public String format(String key,Map<String,String> variables) {
        String value=text(key);
        for (var entry : variables.entrySet()) value=value.replace("{"+entry.getKey()+"}",entry.getValue());
        return value;
    }
    public static Messages parse(YamlConfiguration yaml,YamlConfiguration defaults) {
        Map<String,String> result=new LinkedHashMap<>();
        for (String key : defaults.getKeys(true)) {
            if (key.equals("scoreboard") || key.startsWith("scoreboard.")) continue;
            if (defaults.isConfigurationSection(key)) {
                if (yaml.contains(key) && !yaml.isConfigurationSection(key)) throw new IllegalArgumentException("Message section must be a YAML section: "+key);
                continue;
            }
            Object value=yaml.contains(key) ? yaml.get(key) : defaults.get(key);
            if (!(value instanceof String text) || text.length()>4096) throw new IllegalArgumentException("Invalid message string: "+key);
            result.put(key,text);
        }
        for (String key : yaml.getKeys(true))
            if (!defaults.contains(key)) throw new IllegalArgumentException("Unknown message key: "+key);
        return new Messages(result);
    }
}
