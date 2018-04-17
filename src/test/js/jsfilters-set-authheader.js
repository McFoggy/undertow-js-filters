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
	if (!data.getExchange().getRequestHeaders().contains('Authorization')) {
		var hs = Java.type('io.undertow.util.HttpString');
		var authHeader = hs.tryFromString('Authorization');
		var authValue = 'Basic Ym9iOnRoZWJ1aWRsZXI=';		// bob:thebuilder
		data.getExchange().getRequestHeaders().add(authHeader, authValue);
	}
	
    data.getNextHttpHandler().handleRequest(data.getExchange());
};