/*
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
var handleRequest = function(data) {
    var header = data.httpString('X-jsfilters-version');
    var version = data.getProperties().apply('prj:version');

    data.getExchange().getResponseHeaders().add(header, version);
    data.getNextHttpHandler().handleRequest(data.getExchange());
};