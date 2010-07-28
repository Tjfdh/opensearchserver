/**   
 * License Agreement for Jaeksoft OpenSearchServer
 *
 * Copyright (C) 2010 Emmanuel Keller / Jaeksoft
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

package com.jaeksoft.searchlib.replication;

import java.io.File;

import com.jaeksoft.searchlib.Client;
import com.jaeksoft.searchlib.ClientCatalog;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.process.ThreadAbstract;
import com.jaeksoft.searchlib.util.ReadWriteLock;
import com.jaeksoft.searchlib.util.RecursiveDirectoryBrowser;
import com.jaeksoft.searchlib.web.PushServlet;

public class ReplicationThread extends ThreadAbstract implements
		RecursiveDirectoryBrowser.CallBack {

	final private ReadWriteLock rwl = new ReadWriteLock();

	private Client client;

	private ReplicationItem replicationItem;

	private double totalSize;

	private double sendSize;

	protected ReplicationThread(Client client,
			ReplicationMaster replicationMaster, ReplicationItem replicationItem) {
		super(client, replicationMaster);
		this.replicationItem = replicationItem;
		this.client = client;
		totalSize = 0;
		sendSize = 0;
	}

	public int getProgress() {
		rwl.r.lock();
		try {
			if (sendSize == 0 || totalSize == 0)
				return 0;
			int p = (int) ((sendSize / totalSize) * 100);
			return p;
		} finally {
			rwl.r.unlock();
		}
	}

	@Override
	public void runner() throws Exception {
		setInfo("Running");
		client.push(this);
	}

	@Override
	public void release() {
		Exception e = getException();
		if (e != null)
			setInfo("Error: " + e.getMessage() != null ? e.getMessage() : e
					.toString());
		else if (isAborted())
			setInfo("Aborted");
		else
			setInfo("Completed");
	}

	public ReplicationItem getReplicationItem() {
		return replicationItem;
	}

	public void push() throws SearchLibException {
		setTotalSize(ClientCatalog
				.getLastModifiedAndSize(client.getIndexName()).getSize());
		addSendSize(client.getIndexName().length());
		PushServlet.call_init(replicationItem);
		new RecursiveDirectoryBrowser(client.getDirectory(), this);
		PushServlet.call_switch(replicationItem);
	}

	private void setTotalSize(long size) {
		rwl.w.lock();
		try {
			totalSize = size;
		} finally {
			rwl.w.unlock();
		}
	}

	private void addSendSize(long size) {
		rwl.w.lock();
		try {
			sendSize += size;
		} finally {
			rwl.w.unlock();
		}
	}

	@Override
	public void file(File file) throws SearchLibException {
		try {
			if (file.isFile()) {
				if (!file.getName().equals("replication.xml"))
					if (!file.getName().equals("replication_old.xml"))
						PushServlet.call_file(client, replicationItem, file);
			} else {
				PushServlet.call_directory(client, replicationItem, file);
			}
			addSendSize(file.length());
		} catch (IllegalStateException e) {
			throw new SearchLibException(e);
		}
	}
}
