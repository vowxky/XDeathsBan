package vowxky.xdeathsban;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * This class was created by Vowxky.
 * All rights reserved to the developer.
 */

public class Constant {
    public static final String MOD_ID = "xdeathsban";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Path pathConfig(){
        return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    }
}
