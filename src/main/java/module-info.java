import scraper.api.node.NodeHook;
import scraper.api.plugin.Addon;
import scraper.plugins.debugger.DebuggerNodeHookAddon;

// modules need a unique name
open module scraper.plugins.debugger {
    // only depend on api and annotations
    requires scraper.api;
    requires scraper.annotations;
    requires scraper.utils;

    requires java.net.http;

    // export packages
    exports scraper.plugins.debugger;

    // websocket
    requires Java.WebSocket;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    // NodeHook, Addon, Hook, PreHook have to be provided
    provides NodeHook with DebuggerNodeHookAddon;
    provides Addon with DebuggerNodeHookAddon;
}