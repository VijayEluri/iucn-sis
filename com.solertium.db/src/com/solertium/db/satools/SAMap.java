/*
 * Copyright (C) 2004-2005 Cluestream Ventures, LLC
 * Copyright (C) 2006-2009 Solertium Corporation
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
 *    http://www.gnu.org/licenses
 */

package com.solertium.db.satools;

import java.sql.ResultSet;
import java.util.HashMap;

import net.jcip.annotations.NotThreadSafe;

import com.solertium.db.DBProcessor;
import com.solertium.db.DBSession;
import com.solertium.db.ExecutionContext;

/**
 * @author <a href="mailto:rob.heittman@solertium.com">Rob Heittman</a>, <a
 *         href="http://www.solertium.com">Solertium Corporation</a>
 */
@NotThreadSafe
public class SAMap implements DBProcessor {
	public HashMap<Object, Object> m = new HashMap<Object, Object>();

	public void process(final ResultSet rs, final ExecutionContext ec) {
		try {
			while (rs.next())
				m.put(rs.getObject(1), rs.getObject(2));
		} catch (final Exception ignored) {
		}
	}

	public void setDBSess(final DBSession dbsess) {
	}
}
