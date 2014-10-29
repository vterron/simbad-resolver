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

public class TargetInformation {

    /* What if the object is found in the SIMBAD database but some of the data
     * (e.g., the proper motion for the Messier 101 galaxy, and this is a real
     * example, at least as of September 2011) is missing? That is why all the
     * attributes are initialized to null; otherwise, a right ascension in
     * decimal degrees of exactly zero, for example, would be indistinguishable
     * from 0.0d, the default value of the double data type. */

    public String name            = null;  /* Identifier used for the SIMBAD query */
    public Double ra_deg          = null;  /* Right ascension... */
    public Double dec_deg         = null;  /* and declination, in decimal degrees ... */
    public String ra              = null;  /* and also in sexagesimal */
    public String dec             = null;
    public Integer epoch          = null;  /* Astronomical epoch */
    public Integer equinox        = null;  /* Equinox */
    public ReferenceSystem system = null;  /* Celestial reference system */
    public Double pm_ra           = null;  /* Proper motion, for right ascension ... */
    public Double pm_dec          = null;  /* and declination */
    public String object_type     = null;  /* The classification of the object */

    /* The name of the target is always known (as that is what we will use as
     * input to SIMBAD!), so it must always be provided to the constructor */
    public TargetInformation(String name) {
        this.name = name;
    }

    /* System-independent newline character */
    public static String newline = System.getProperty("line.separator");

    public String toString(){
        StringBuilder repr = new StringBuilder();
        repr.append(String.format("Name: %s%s", this.name, newline));
        repr.append(String.format("Type: %s%s", this.object_type, newline));
        repr.append(String.format("RA: %s (%f)%s", this.ra, this.ra_deg, newline));
        repr.append(String.format("DEC: %s (%f)%s", this.dec, this.dec_deg, newline));
        repr.append(String.format("Epoch: J%d | Equinox: %d%s", this.epoch, this.equinox, newline));
        repr.append(String.format("Reference system: %s%s", this.system, newline));
        repr.append(String.format("Proper motions: %f %f", this.pm_ra, this.pm_dec));
        return repr.toString();
    }
}