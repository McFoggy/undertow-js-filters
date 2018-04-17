/**
 * Copyright (C) 2018 Matthieu Brouillard [http://oss.brouillard.fr/undertow-jsfilters] (matthieu@brouillard.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.brouillard.oss.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class JSFilter implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(JSFilter.class.getName());

    private final HttpHandler next;
    private String fileName;
    private String refresh;
    private AtomicReference<Optional<ScriptRunner>> currentEngine = new AtomicReference<>();
    private WatchKey currentWatchKey;

    public JSFilter(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // don't do anything on NIO dispatching thread
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        // We check if the engine was initially loaded or at least tried to be loaded
        if (currentEngine.get() == null) {
            Optional<ScriptRunner> optRunner = loadAndSetScriptRunner();
            if (Boolean.parseBoolean(refresh)) {
                optRunner.ifPresent(runner -> {
                    reloadOnChange(runner.getFile());
                });
            }
        }

        Optional<ScriptRunner> runner = currentEngine.get();
        if (runner.isPresent()) {
            try {
                Invocable invocable = (Invocable) runner.get().getEngine();
                JSFilterData data = new JSFilterData(exchange, next, runner.get().getScriptLogger());
                invocable.invokeFunction("handleRequest", data);
            } catch (ScriptException | NoSuchMethodException invocationException) {
                LOGGER.warning("undertow-jsfilters: failure calling method '" + "handleRequest" + "' in file => " + this.fileName);
                LOGGER.throwing(fileName, "handleRequest", invocationException);
                next.handleRequest(exchange);
                return;
            }
        } else {
            next.handleRequest(exchange);
            return;
        }
    }

    private Optional<ScriptRunner> loadAndSetScriptRunner() {
        Optional<ScriptRunner> optRunner = loadScriptRunner();
        currentEngine.set(optRunner);
        return optRunner;
    }

    private void reloadOnChange(File jsFile) {
        try {
            FileSystem fileSystem = FileSystems.getDefault();
            WatchService watchService = fileSystem.newWatchService();
            Path jsDirectory = fileSystem.getPath(jsFile.getParent());
            this.currentWatchKey = jsDirectory.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            Thread watcherThread = new Thread(() -> {
                AtomicBoolean listening = new AtomicBoolean(true);
                while (listening.get()) {
                    try {
                        WatchKey wk;
                        try {
                            wk = watchService.take();
                        } catch (InterruptedException ex) {
                            return;
                        }

                        Thread.sleep( 200 );

                        for (WatchEvent<?> event : wk.pollEvents()) {
                            final Path changed = (Path) event.context();
                            if (changed.endsWith(jsFile.getName())) {
                                LOGGER.info("change detected on " + jsFile.getName() + ", reloading filter script, event " + event.toString());
                                loadAndSetScriptRunner();
                            }
                        }
                        // reset the key
                        boolean valid = wk.reset();
                        if (!valid) {
                            break;
                        }
                    } catch (Exception ioe) {
                        LOGGER.warning("problem occured while watching changes on " + jsFile.getName());
                        LOGGER.throwing(JSFilter.class.getName() + ".thread", "run", ioe);
                        listening.set(false);
                    }
                }
                LOGGER.info("stop watching changes on " + jsFile.getName());
            }, "JSFilter[" + jsFile.getName() + "] change detector");
            watcherThread.setDaemon(true);
            watcherThread.start();
        } catch (IOException ioe) {
            LOGGER.warning("problem occured while trying to watch changes on " + jsFile.getName());
            LOGGER.throwing(JSFilter.class.getName(), "reloadOnChange", ioe);
        }
    }

    private Optional<ScriptRunner> loadScriptRunner() {
        ScriptEngine engine = null;
        File file = null;
        Logger scriptLogger = null;
        if (this.fileName != null) {
            file = new File(this.fileName);
            if (file.canRead()) {
                try {
                    ClassLoader jsClassloader = JSFilter.class.getClassLoader();
                    engine = new NashornScriptEngineFactory().getScriptEngine(jsClassloader);

                    if (engine == null) {
                        LOGGER.warning("undertow-jsfilters: Nashorn JavaScript engine not found");
                    } else {
                        try {
                            LOGGER.config("undertow-jsfilters: using JavaScript engine: " + engine.getFactory().getEngineName() + " to load " + this.fileName);
                            engine.eval(new FileReader(file));
                            LOGGER.config("undertow-jsfilters: file loaded successfully");
                        } catch (Exception evaluationException) {
                            LOGGER.config("undertow-jsfilters: cannot evaluate js file => " + this.fileName);
                            LOGGER.throwing(JSFilter.class.getName(), "handleRequest", evaluationException);
                        }

                        scriptLogger = Logger.getLogger(JSFilter.class.getName() + "." + file.getName());
                    }
                } catch(Exception ex) {
                    LOGGER.warning("undertow-jsfilters: cannot load Nashorn JavaScript engine");
                    LOGGER.throwing(JSFilter.class.getName(), "loadEngine", ex);
                }
            } else {
                LOGGER.warning("undertow-jsfilters: non readable file => " + this.fileName);
            }
        } else {
            LOGGER.warning("undertow-jsfilters: missing expected parameter 'fileName'");
        }

        if (file == null || engine == null || scriptLogger == null) {
            return Optional.empty();
        } else {
            return Optional.of(new ScriptRunner(engine, file, scriptLogger));
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRefresh() {
        return refresh;
    }

    public void setRefresh(String refresh) {
        this.refresh = refresh;
    }

    private static class ScriptRunner {
        private final ScriptEngine engine;
        private final File file;
        private final Logger scriptLogger;

        ScriptRunner(ScriptEngine engine, File file, Logger scriptLogger) {
            this.engine = engine;
            this.file = file;
            this.scriptLogger = scriptLogger;
        }

        public ScriptEngine getEngine() {
            return engine;
        }

        public File getFile() {
            return file;
        }

        public Logger getScriptLogger() {
            return scriptLogger;
        }
    }
}
