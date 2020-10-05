

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

/**
 *
 * @author programador
 */
public class EntityFileException extends RuntimeException{

    public EntityFileException(Throwable thrwbl) {
        super(thrwbl);
    }

    public EntityFileException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }

    public EntityFileException(String string) {
        super(string);
    }

    public EntityFileException() {
    }

}
