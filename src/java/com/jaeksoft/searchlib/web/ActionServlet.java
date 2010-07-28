/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2008-2009 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of Jaeksoft OpenSearchServer.
 *
 * Jaeksoft OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Jaeksoft OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Jaeksoft OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.web;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.SearchLibException;

public class ActionServlet extends AbstractServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -369063857059673597L;

	@Override
	protected void doRequest(ServletTransaction transaction)
			throws ServletException {
		try {
			HttpServletRequest request = transaction.getServletRequest();
			Client client = transaction.getClient();
			String action = request.getParameter("action");
			if ("optimize".equalsIgnoreCase(action))
				client.optimize();
			else if ("swap".equalsIgnoreCase(action)) {
				String p = request.getParameter("version");
				long version = (p == null) ? 0 : Long.parseLong(p);
				boolean deleteOld = (request.getParameter("deleteOld") != null);
				client.getIndex().swap(version, deleteOld);
			} else if ("reload".equalsIgnoreCase(action)) {
				client.reload();
			} else if ("online".equalsIgnoreCase(action))
				client.getIndex().setOnline(true);
			else if ("offline".equalsIgnoreCase(action))
				client.getIndex().setOnline(false);
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	public static void optimize(URI uri, String indexName)
			throws SearchLibException, URISyntaxException {
		call(buildUri(uri, "/action", indexName, "action=optimize"));
	}

	public static void reload(URI uri) throws SearchLibException,
			URISyntaxException {
		call(buildUri(uri, "/action", null, "action=reload"));
	}

	public static void swap(URI uri, String indexName, long version,
			boolean deleteOld) throws SearchLibException, URISyntaxException {
		StringBuffer query = new StringBuffer("action=swap");
		query.append("&version=");
		query.append(version);
		if (deleteOld)
			query.append("&deleteOld");
		call(buildUri(uri, "/action", indexName, query.toString()));
	}

	public static void online(URI uri, String indexName)
			throws SearchLibException, URISyntaxException {
		call(buildUri(uri, "/action", indexName, "action=online"));
	}

	public static void offline(URI uri, String indexName)
			throws SearchLibException, URISyntaxException {
		call(buildUri(uri, "/action", indexName, "action=offline"));
	}
}
