/* SIMBAD-based target resolver for the PANIC Observation Tool
 *
 * Copyright (c) 2011 Victor Terron. All rights reserved.
 * Institute of Astrophysics of Andalusia, IAA-CSIC
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 **********************************************************************/

import java.util.Random;

/* Celestial reference systems allowed by SIMBAD for coordinate queries */
public enum ReferenceSystem {
	
	ICRS, FK5, FK4, GAL, SGAL, ECL;
	
	/* Returns one of the reference systems, selected randomly */
	public static ReferenceSystem random() {
		int randomIndex = new Random().nextInt(ReferenceSystem.values().length);
		return ReferenceSystem.values()[randomIndex];
	}
	
}