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


public class TargetResolver {

    public static TargetCoordinates resolve(String starName){

	/*******************************************************************/
	/* Lots of code here, quering SIMBAD given the name of the star... */
	/* See: http://mariotti.fr/MCS/MAR2011/doc/jmcs/api/html/StarResolver_8java-source.html */
	/*******************************************************************/

	/* Just an example... */
	return new TargetCoordinates(starName, 12.3, 34.213);
    }


    public static void main(String[] args) {

	TargetCoordinates coords = TargetResolver.resolve("M101");
	System.out.println(coords.name + ": " + coords.ra + ", " + coords.dec);

    }
}
