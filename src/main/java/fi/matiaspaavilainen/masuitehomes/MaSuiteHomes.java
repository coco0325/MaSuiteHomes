package fi.matiaspaavilainen.masuitehomes;

import fi.matiaspaavilainen.masuitecore.Updator;
import fi.matiaspaavilainen.masuitecore.config.Configuration;
import fi.matiaspaavilainen.masuitecore.database.Database;
import fi.matiaspaavilainen.masuitehomes.commands.Delete;
import fi.matiaspaavilainen.masuitehomes.commands.List;
import fi.matiaspaavilainen.masuitehomes.commands.Set;
import fi.matiaspaavilainen.masuitehomes.commands.Teleport;
import net.md_5.bungee.api.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaSuiteHomes extends Plugin {

    static Database db = new Database();
    @Override
    public void onEnable() {
        super.onEnable();

        //Configs
        Configuration config = new Configuration();
        config.create(this, "homes", "messages.yml");
        config.create(this, "homes", "syntax.yml");

        //Commands
        getProxy().getPluginManager().registerCommand(this, new Teleport());
        getProxy().getPluginManager().registerCommand(this, new Set());
        getProxy().getPluginManager().registerCommand(this, new Delete());
        getProxy().getPluginManager().registerCommand(this, new List());

        Logger logger = LoggerFactory.getLogger("com.zaxxer.hikari");
        db.connect();
        db.createTable("homes",
                "(id INT(10) unsigned NOT NULL PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL, owner VARCHAR(36) NOT NULL, server VARCHAR(100) NOT NULL, world VARCHAR(100) NOT NULL, x DOUBLE, y DOUBLE, z DOUBLE, yaw FLOAT, pitch FLOAT) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        new Updator().checkVersion(this.getDescription(), "60632");
    }

    @Override
    public void onDisable(){
        db.hikari.close();
    }
}
