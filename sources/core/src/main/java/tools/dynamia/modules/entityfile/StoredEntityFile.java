/*
 * Copyright (C)  2020. Dynamia Soluciones IT S.A.S - NIT 900302344-1
 * Colombia - South America
 *  All Rights Reserved.
 *
 * This file is free software: you can redistribute it and/or modify it  under the terms of the
 *  GNU Lesser General Public License (LGPL v3) as published by the Free Software Foundation,
 *   either version 3 of the License, or (at your option) any later version.
 *
 *  This file is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *   without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *   See the GNU Lesser General Public License for more details. You should have received a copy of the
 *   GNU Lesser General Public License along with this file.
 *   If not, see <https://www.gnu.org/licenses/>.
 *
 */

package tools.dynamia.modules.entityfile;

import java.io.File;
import java.io.Serializable;

import tools.dynamia.modules.entityfile.domain.EntityFile;

public abstract class StoredEntityFile implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 421213041955145817L;
	private EntityFile entityFile;
	private String url;
	private File realFile;

	public StoredEntityFile(EntityFile entityFile, String url, File realFile) {
		super();
		this.entityFile = entityFile;
		this.url = url;
		this.realFile = realFile;
	}

	public EntityFile getEntityFile() {
		return entityFile;
	}

	public String getUrl() {
		return url;
	}

	public String getThumbnailUrl() {
		return getThumbnailUrl(100, 100);
	}

	public abstract String getThumbnailUrl(int width, int height);

	public File getRealFile() {
		return realFile;
	}

}
