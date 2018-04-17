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

import javax.script.*;
import java.io.File;
import java.io.FileReader;
import java.util.logging.Logger;

public class JSFilter implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(JSFilter.class.getName());

    private final HttpHandler next;
    private String fileName;

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

        if (this.fileName != null) {
            File file = new File(this.fileName);

            if (file.canRead()) {
                ClassLoader jsClassloader = JSFilter.class.getClassLoader();
                ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine(jsClassloader);

                if (engine == null) {
                    LOGGER.warning("undertow-jsfilters: Nashorn JavaScript engine not found");
                    next.handleRequest(exchange);
                    return;
                }
                LOGGER.config("undertow-jsfilters: using JavaScript engine: " + engine.getFactory().getEngineName());
                try {
                    engine.eval(new FileReader(file));
                } catch (Exception evaluationException) {
                    LOGGER.config("undertow-jsfilters: cannot evaluate js file => " + this.fileName);
                    LOGGER.throwing(JSFilter.class.getName(), "handleRequest", evaluationException);
                    next.handleRequest(exchange);
                    return;
                }

                Logger scriptLogger = Logger.getLogger(JSFilter.class.getName() + "." + file.getName());
                Invocable invocable = (Invocable) engine;

                try {
                    JSFilterData data = new JSFilterData(exchange, next, scriptLogger);
                    invocable.invokeFunction("handleRequest", data);
                } catch (ScriptException | NoSuchMethodException invocationException) {
                    LOGGER.warning("undertow-jsfilters: failure calling method '" + "handleRequest" + "' in file => " + this.fileName);
                    LOGGER.throwing(fileName, "handleRequest", invocationException);
                    next.handleRequest(exchange);
                    return;
                }
            } else {
                LOGGER.config("undertow-jsfilters: cannot read file => " + this.fileName);
                next.handleRequest(exchange);
                return;
            }
        } else {
            LOGGER.config("undertow-jsfilters: missing expected parameter 'fileName'");
            next.handleRequest(exchange);
            return;
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
