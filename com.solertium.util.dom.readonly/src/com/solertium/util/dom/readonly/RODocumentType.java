/*******************************************************************************
 * Copyright (C) 2007-2009 Solertium Corporation
 * 
 * This file is part of the open source GoGoEgo project.
 * 
 * Unless you have been granted a different license in writing by the
 * copyright holders for GoGoEgo, you may only modify or redistribute
 * this code under the terms of one of the following licenses:
 * 
 * 1) The Eclipse Public License, v.1.0
 *    http://www.eclipse.org/legal/epl-v10.html
 * 
 * 2) The GNU General Public License, version 2 or later
 *     http://www.gnu.org/licenses
 ******************************************************************************/
package com.solertium.util.dom.readonly;

import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;

public class RODocumentType extends RONode implements DocumentType {

	private final DocumentType peer;

	RODocumentType(final DocumentType peer) {
		super(peer);
		this.peer = peer;
	}

	public NamedNodeMap getEntities() {
		return new RONamedNodeMap(peer.getEntities());
	}

	public String getInternalSubset() {
		return peer.getInternalSubset();
	}

	public String getName() {
		return peer.getName();
	}

	public NamedNodeMap getNotations() {
		return new RONamedNodeMap(peer.getNotations());
	}

	public String getPublicId() {
		return peer.getPublicId();
	}

	public String getSystemId() {
		return peer.getSystemId();
	}

}
