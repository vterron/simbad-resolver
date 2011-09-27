/* SIMBAD-based star coordinates resolver for the PANIC Observation Tool
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

import java.util.ArrayList;

public class TargetInformation {

    public String name;          /* name of the object, used for the resolution */
    public double ra_deg;        /* right ascension... */
    public double dec_deg;       /* and declination, in decimal degrees... */
    public String ra;            /* ... and also in sexagesimal
    public String dec;
    public int epoch;            /* astronomical epoch */
    public int equinox;          /* and equinox */
    public String ref_system;    /* celestial reference system */
    public double pm_ra;         /* The proper motion, for both RA ... */
    public double pm_dec;        /* and declination */
    public String object_type;   /* As reported by SIMBAD */
    /* List of identifiers for the object, as reported by SIMBAD */
    public ArrayList identifiers = new ArrayList();

    public TargetInformation(String name) {
	this.name = name;
    }
}
