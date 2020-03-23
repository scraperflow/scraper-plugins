package scraper.plugins.persistentproxies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scraper.annotations.ArgsCommand;
import scraper.annotations.NotNull;
import scraper.api.di.DIContainer;
import scraper.api.plugin.Addon;
import scraper.api.service.ProxyReservation;
import scraper.utils.StringUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;


@ArgsCommand(
        value = "persistent-proxies:<PATH>",
        doc = "Persists proxy scores at PATH if enabled. Default is working directory",
        example = "scraper app.scraper persistent-proxies"
)
public class PersistentProxiesAddon implements Addon {
    /** Logger with the actual class name */
    private Logger l = LoggerFactory.getLogger("PersistentProxies");

    @Override
    public void load(@NotNull DIContainer loadedDependencies, @NotNull String[] args) {
        String persistArg = StringUtil.getArgument(args, "persistent-proxies");
        if (persistArg != null) {

            String persist;
            if(persistArg.isEmpty()) persist = ".";
            else persist = persistArg;

            ProxyReservation proxy = loadedDependencies.get(ProxyReservation.class);
            assert proxy != null;

            try {
                tryReadScores(proxy, persist);
            } catch (IOException e) {
                l.error("Could not read input proxy scores", e);
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {

                File f = new File(Paths.get(persist, "proxy-scores-new.csv").toString());
                try (PrintWriter pw = new PrintWriter(new FileOutputStream(f, false))) {
                    proxy.getAllGroups().forEach(((group, groupInfo) -> groupInfo.getAllProxiesAsString(true).forEach(line -> {
                        pw.println(group+">"+line);
                    })));
                } catch (FileNotFoundException e) {
                    l.error("Could not write scores to file");
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }

                File f_old = new File(Paths.get(persist, "proxy-scores.csv").toString());
                boolean renamed = f.renameTo(f_old);
                if(!renamed) {
                    l.error("Could not finish rename operation {} -> {}", f, f_old);
                } else {
                    l.info("Persisted proxy scores successfully");
                }
            }));
        }
    }

    private void tryReadScores(ProxyReservation proxy, String persist) throws IOException {
        if(persist.isEmpty()) persist = ".";

        File f = new File(Paths.get(persist, "proxy-scores.csv").toString());
        if(f.exists()) {
            l.info("Reading persistent scores at {}", f);

            Files.lines(Paths.get(persist, "proxy-scores.csv"))
                    .forEach(proxy::addProxyLine);
        } else {
            l.info("No persistent scores found at {}", f);
        }
    }

    @Override
    public String toString() {
        return "PersistentProxies";
    }
}

