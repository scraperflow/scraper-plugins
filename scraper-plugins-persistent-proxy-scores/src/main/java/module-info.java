import scraper.api.plugin.Addon;

// modules need a unique name
open module scraper.plugins.persitentproxies {
    // only depend on api and annotations
    requires scraper.api;
    requires scraper.annotations;
    requires scraper.utils;

    // export packages
    exports scraper.plugins.persistentproxies;

    requires org.slf4j;

    provides Addon with scraper.plugins.persistentproxies.PersistentProxiesAddon;
}
