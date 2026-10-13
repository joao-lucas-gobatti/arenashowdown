package com.arenashowdown.core.service;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

public final class YamlScorePolicy implements ScorePolicy {
    private final YamlConfiguration yaml;
    public YamlScorePolicy(File file){this.yaml=YamlConfiguration.loadConfiguration(file);}
    @Override public int points(String game,int position,int submitted){if(yaml.getBoolean("scoring.use-result-points",false))return submitted;String specific="scoring.minigames."+game+".positions."+position;if(yaml.contains(specific))return yaml.getInt(specific);return switch(position){case 1->yaml.getInt("scoring.positions.first",10);case 2->yaml.getInt("scoring.positions.second",7);case 3->yaml.getInt("scoring.positions.third",5);default->yaml.getInt("scoring.participation",0);};}
}
