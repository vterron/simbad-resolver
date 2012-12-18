/* SIMBAD-based target resolver for the PANIC Observation Tool
 *
 * Copyright (c) 2012 Victor Terron. All rights reserved.
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

import static org.junit.Assert.*;

import org.junit.Test;

public class SIMBADQuerierTest {

    /* Maximum delta between real numbers for which they are considered equal */
    private static double delta = 0.001;

    @Test
    public void test_DD_to_DMS() {

        double[] coords = SIMBADQuerier.DD_to_DMS(-90.0);
        assertEquals(-90, (int)coords[0]); /* Degrees */
        assertEquals(0, (int)coords[1]); /* Arcminutes */
        assertEquals(0, coords[2], delta); /* Arcseconds */

        coords = SIMBADQuerier.DD_to_DMS(-78.83);
        assertEquals(-78, (int)coords[0]);
        assertEquals(49, (int)coords[1]);
        assertEquals(47.999, coords[2], delta);

        coords = SIMBADQuerier.DD_to_DMS(0.0);
        assertEquals(0, (int)coords[0]);
        assertEquals(0, (int)coords[1]);
        assertEquals(0, coords[2], delta);

        coords = SIMBADQuerier.DD_to_DMS(54.34808);
        assertEquals(54, (int)coords[0]);
        assertEquals(20, (int)coords[1]);
        assertEquals(53.088, coords[2], delta);

        coords = SIMBADQuerier.DD_to_DMS(68.23);
        assertEquals(68, (int)coords[0]);
        assertEquals(13, (int)coords[1]);
        assertEquals(48, coords[2], delta);

        coords = SIMBADQuerier.DD_to_DMS(90.0);
        assertEquals(90, (int)coords[0]);
        assertEquals(0, (int)coords[1]);
        assertEquals(0, coords[2], delta);

    }


    @Test
    public void test_DD_to_HMS() {

        double[] coords = SIMBADQuerier.DD_to_HMS(0.0);
        assertEquals(0, (int)coords[0]); /* Hours */
        assertEquals(0, (int)coords[1]); /* Minutes */
        assertEquals(0, coords[2], delta); /* Seconds */

        coords = SIMBADQuerier.DD_to_HMS(34.181);
        assertEquals(2, (int)coords[0]);
        assertEquals(16, (int)coords[1]);
        assertEquals(43.439, coords[2], delta);

        coords = SIMBADQuerier.DD_to_HMS(128.981);
        assertEquals(8, (int)coords[0]);
        assertEquals(35, (int)coords[1]);
        assertEquals(55.44, coords[2], delta);

        coords = SIMBADQuerier.DD_to_HMS(266.185);
        assertEquals(17, (int)coords[0]);
        assertEquals(44, (int)coords[1]);
        assertEquals(44.4, coords[2], delta);

        coords = SIMBADQuerier.DD_to_HMS(315.39);
        assertEquals(21, (int)coords[0]);
        assertEquals(1, (int)coords[1]);
        assertEquals(33.599, coords[2], delta);

        coords = SIMBADQuerier.DD_to_HMS(360);
        assertEquals(24, (int)coords[0]);
        assertEquals(0, (int)coords[1]);
        assertEquals(0, coords[2], delta);

    }

}
