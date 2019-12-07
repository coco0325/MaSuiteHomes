package fi.matiaspaavilainen.masuitehomes.core.services;

import fi.matiaspaavilainen.masuitecore.bungee.chat.Formator;
import fi.matiaspaavilainen.masuitecore.core.channels.BungeePluginChannel;
import fi.matiaspaavilainen.masuitecore.core.configuration.BungeeConfiguration;
import fi.matiaspaavilainen.masuitecore.core.objects.MaSuitePlayer;
import fi.matiaspaavilainen.masuitehomes.bungee.MaSuiteHomes;
import fi.matiaspaavilainen.masuitehomes.core.HibernateUtil;
import fi.matiaspaavilainen.masuitehomes.core.models.Home;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HomeService {

    private EntityManager entityManager = HibernateUtil.getEntityManager();
    public HashMap<UUID, List<Home>> homes = new HashMap<>();

    private MaSuiteHomes plugin;

    private Formator formator = new Formator();
    private BungeeConfiguration config = new BungeeConfiguration();

    public HomeService(MaSuiteHomes plugin) {
        this.plugin = plugin;
    }

    /**
     * Teleport player to home
     *
     * @param msPlayer player to teleport
     * @param home     home to teleport
     */
    public void teleportToHome(MaSuitePlayer msPlayer, Home home) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(msPlayer.getUniqueId());
        this.teleport(player, home);
    }

    /**
     * Teleport player to home
     *
     * @param player player to teleport
     * @param home   home to teleport
     */
    public void teleportToHome(ProxiedPlayer player, Home home) {
        this.teleport(player, home);
    }

    /**
     * Teleport player to home
     *
     * @param uuid uuid of the player to teleport
     * @param home home to teleport
     */
    public void teleportToHome(UUID uuid, Home home) {
        ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
        this.teleport(player, home);
    }

    /**
     * Teleports player to home
     *
     * @param player player to teleport
     * @param home   home to teleport
     */
    private void teleport(ProxiedPlayer player, Home home) {
        if (home == null) {
            formator.sendMessage(player, config.load("homes", "messages.yml").getString("home-not-found"));
            return;
        }

        if (!player.getServer().getInfo().getName().equals(home.getServer())) {
            player.connect(ProxyServer.getInstance().getServerInfo(home.getServer()));
        }
        BungeePluginChannel bpc = new BungeePluginChannel(plugin, ProxyServer.getInstance().getServerInfo(home.getServer()),
                new Object[]{"HomePlayer",
                        player.getUniqueId().toString(),
                        home.getLocation().getWorld(),
                        home.getLocation().getX(),
                        home.getLocation().getY(),
                        home.getLocation().getZ(),
                        home.getLocation().getYaw(),
                        home.getLocation().getPitch()
                });
        if (!player.getServer().getInfo().getName().equals(home.getServer())) {
            plugin.getProxy().getScheduler().schedule(plugin, bpc::send, 500, TimeUnit.MILLISECONDS);
        } else {
            bpc.send();
        }
        formator.sendMessage(player, config.load("homes", "messages.yml").getString("home.teleported").replace("%home%", home.getName()));
    }

    /**
     * Create a new {@link Home}
     *
     * @param home home to create
     */
    public Home createHome(Home home) {
        entityManager.getTransaction().begin();
        entityManager.persist(home);
        entityManager.getTransaction().commit();
        homes.get(home.getOwner()).add(home);

        return home;
    }

    /**
     * Update specific {@link Home}
     */
    public Home updateHome(Home home) {
        entityManager.getTransaction().begin();
        entityManager.merge(home);
        entityManager.getTransaction().commit();

        // Remove home from list and add new back
        List<Home> homeList = homes.get(home.getOwner()).stream().filter(cacheHome -> !cacheHome.getName().equalsIgnoreCase(home.getName())).collect(Collectors.toList());
        homeList.add(home);
        homes.put(home.getOwner(), homeList);
        return home;
    }

    /**
     * Remove home
     *
     * @param home home to remove
     */
    public void removeHome(Home home) {
        entityManager.getTransaction().begin();
        entityManager.remove(home);
        entityManager.getTransaction().commit();

        // Update cache
        homes.put(home.getOwner(), homes.get(home.getOwner()).stream().filter(homeItem -> !homeItem.getName().equalsIgnoreCase(home.getName())).collect(Collectors.toList()));
    }

    /**
     * Gets player home points
     *
     * @param uuid owner of homes
     * @return returns a list of homes
     */
    public List<Home> getHomes(UUID uuid) {
        if (homes.containsKey(uuid)) {
            return homes.get(uuid);
        }

        List<Home> homesList = entityManager.createQuery(
                "SELECT h FROM Home h WHERE h.owner = :owner ORDER BY h.name", Home.class)
                .setParameter("owner", uuid).getResultList();
        homes.put(uuid, homesList);

        return homesList;
    }

    public void initializeHomes(UUID uuid) {
        List<Home> homesList = entityManager.createQuery(
                "SELECT h FROM Home h WHERE h.owner = :owner ORDER BY h.name", Home.class)
                .setParameter("owner", uuid).getResultList();
        homes.put(uuid, homesList);
    }

    /**
     * Get {@link MaSuitePlayer}'s home by name
     *
     * @param player owner of the home
     * @param home   name of the home
     * @return returns home or null
     */
    public Home getHome(MaSuitePlayer player, String home) {
        return this.loadHome(player.getUniqueId(), home, "findHomeByOwnerAndLikeName");
    }

    /**
     * Get username home by name
     *
     * @param username owner of the home
     * @param home     name of the home
     * @return returns home or null
     */
    public Home getHome(String username, String home) {
        MaSuitePlayer player = new MaSuitePlayer().find(username);
        if (player == null) {
            return null;
        }
        return this.loadHome(player.getUniqueId(), home, "findHomeByOwnerAndLikeName");
    }

    /**
     * Get username home by name
     *
     * @param uuid owner of the home
     * @param home name of the home
     * @return returns home or null
     */
    public Home getHome(UUID uuid, String home) {
        return this.loadHome(uuid, home, "findHomeByOwnerAndLikeName");
    }


    /**
     * Get username home by exact name
     *
     * @param uuid owner of the home
     * @param home name of the home
     * @return returns home or null
     */
    public Home getHomeExact(UUID uuid, String home) {
        return this.loadHome(uuid, home, "findHomeByOwnerAndName");
    }

    /**
     * Load home from cache or database
     *
     * @param uuid owner of the home
     * @param name name of the home
     * @return return home or null from cache or database
     */
    private Home loadHome(UUID uuid, String name, String type) {
        // Load the home from cache
        Optional<Home> cachedHome = homes.get(uuid).stream().filter(home -> home.getName().equalsIgnoreCase(name)).findFirst();
        if (cachedHome.isPresent()) {
            return cachedHome.get();
        }

        // Search home from database
        Home home = entityManager.createNamedQuery(type, Home.class)
                .setParameter("owner", uuid)
                .setParameter("name", name)
                .getResultList().stream().findFirst().orElse(null);

        // Add home into cache if not null
        if (home != null) {
            homes.get(uuid).add(home);
        }
        return home;
    }
}
