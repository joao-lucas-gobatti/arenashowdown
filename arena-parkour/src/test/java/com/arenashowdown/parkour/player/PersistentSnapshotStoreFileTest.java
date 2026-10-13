package com.arenashowdown.parkour.player;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletionException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PersistentSnapshotStoreFileTest {
    @TempDir Path directory;

    @Test void existingSnapshotMakesNewCaptureFailInsteadOfSilentlySucceeding(){
        UUID id=UUID.randomUUID();PersistentSnapshotStore store=new PersistentSnapshotStore(null,directory);
        store.writeAtomicIfAbsent(id,"original");
        assertThrows(CompletionException.class,()->store.writeAtomicIfAbsent(id,"replacement"));
        assertEquals("original",assertDoesNotThrow(()->Files.readString(directory.resolve(id+".yml"))));store.close();
    }

    @Test void truncatedOrWronglyTypedInventoryIsRejectedBeforeApplyOrDelete()throws Exception{
        UUID id=UUID.randomUUID();YamlConfiguration yaml=new YamlConfiguration();yaml.set("uuid",id.toString());yaml.set("world","world");yaml.set("game-mode","SURVIVAL");
        yaml.set("x",1.5);yaml.set("y",70.0);yaml.set("z",-2.5);yaml.set("yaw",0.0);yaml.set("pitch",0.0);yaml.set("health",20.0);yaml.set("food",20);
        yaml.set("allow-flight",false);yaml.set("flying",false);yaml.set("inventory",List.of("not-an-item"));yaml.set("armor",List.of("a","b","c","d"));
        Files.writeString(directory.resolve(id+".yml"),yaml.saveToString());PersistentSnapshotStore store=new PersistentSnapshotStore(null,directory);
        assertThrows(CompletionException.class,()->store.readValidated(id));assertTrue(Files.exists(directory.resolve(id+".yml")));store.close();
    }

    @Test void completeLegacySnapshotRemainsRecoverable()throws Exception{
        UUID id=UUID.randomUUID();YamlConfiguration yaml=new YamlConfiguration();yaml.set("uuid",id.toString());yaml.set("world","world");yaml.set("game-mode","SURVIVAL");
        yaml.set("x",1.5);yaml.set("y",70.0);yaml.set("z",-2.5);yaml.set("yaw",0.0);yaml.set("pitch",0.0);yaml.set("health",20.0);yaml.set("food",20);
        yaml.set("allow-flight",false);yaml.set("flying",false);yaml.set("inventory",new ArrayList<>(Collections.nCopies(41,null)));yaml.set("armor",new ArrayList<>(Collections.nCopies(4,null)));
        Files.writeString(directory.resolve(id+".yml"),yaml.saveToString());PersistentSnapshotStore store=new PersistentSnapshotStore(null,directory);
        assertNotNull(store.readValidated(id));store.close();
    }
}
