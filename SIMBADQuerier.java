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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Callable;

public class SIMBADQuerier implements Callable<String>{

    /* The URL to which the script is submitted to SIMBAD */
    public static final String _simbadBaseURL =
            "http://simbad.u-strasbg.fr/simbad/sim-script?script=";

    public String targetName;
    public ReferenceSystem system;
    public int epoch;
    public int equinox;

    /* As explained and seen here: http://stackoverflow.com/q/1660000 */
    public SIMBADQuerier(String targetName, ReferenceSystem system, int epoch, int equinox) {
        this.targetName = targetName;
        this.system = system;
        this.epoch = epoch;
        this.equinox = equinox;
    }

    /* Decimal degrees to degrees, arcminutes and arcseconds conversion */
    public static double[] DD_to_DMS(double decimal_degrees){
        double degrees, arcminutes, arcseconds, tmp;

        degrees = (int) decimal_degrees; /* Take integer part */
        /* Get decimal part; do not propagate the minus sign, if any.
         * Then convert from degrees to arcminutes */
        tmp = (Math.abs(decimal_degrees) - Math.abs(degrees)) * 60;
        arcminutes = (int) tmp; /* Take integer part */
        /* Get decimal part, convert from arcminutes to arcsecs */
        arcseconds = (tmp - arcminutes) * 60;

        double[] coords = {degrees, arcminutes, arcseconds};
        return coords;
    }

    /* Decimal degrees to hours, minutes, seconds conversion */
    public static double[] DD_to_HMS(double decimal_degrees){
        double hours, minutes, seconds, tmp;

        tmp = decimal_degrees / 15.0; /* A whole circle is 24 hours */
        hours = (int) tmp; /* Take integer part */
        /* Get decimal part, convert from hours to minutes */
        tmp = (tmp - hours) * 60.0;
        minutes = (int) tmp; /* Take integer part */
        /* Get decimal part, convert from minutes to seconds */
        seconds = (tmp - minutes) * 60;

        double[] coords = {hours, minutes, seconds};
        return coords;
    }

    /* Queries SIMBAD by identifier (in layman's terms, the name of the object)
     * and returns the output as a String. If the connection to SIMBAD fails,
     * for whatever arcane reason, SIMBADQueryException is thrown.
     *
     * The complete syntax of the SIMBAD scripts can be found at:
     * http://simbad.u-strasbg.fr/simbad/sim-help?Page=sim-fscript */

    public String query_SIMBAD (String targetName) throws SIMBADQueryException {

        /* Used for both the SIMBAD script and its output */
        StringBuilder buffer = new StringBuilder();

        /* Mask the script display in the output as well as the execution details */
        buffer.append("output console=off script=off\n");
        /* This defines the data items of the object that we want to retrieve */
        buffer.append("format object form1 \"");

        /* In %COO(options), the option string is made of 5 parts separated by
         * semicolons: formatting options, ('s' for sexagesimal coordinates, 'd'
         * for decimal), element list ('A' for right ascension, 'B' for declination),
         * frame (ICRS, FK5, FK4, GAL, SGAL or ECL), epoch (which must be prefixed
         * by B' or 'J') and equinox. */

        /* Avoid having to type the reference system, epoch and equinox (e.g.,
         * "FK5; J2000; 2000)" if the default TargetResolver is used) at the
         * end of each line over and over */
        String line_remainder = this.system + ";" + "J" + this.epoch + ";" + this.equinox + ")\\n";

        buffer.append("%COO(d;A;" + line_remainder);  /* RA (decimal degrees) */
        buffer.append("%COO(d;D;" + line_remainder);  /* DEC (decimal degrees) */

        buffer.append("%OTYPE(V)\\n");  /* Verbose display of the main object type */
        buffer.append("%PM(A)\\n");     /* Proper motion on the right ascension axis */
        buffer.append("%PM(D)\\n");     /* Proper motion on the declination axis */
        buffer.append("\"\n");          /* marks the end of the parameters we want */
        buffer.append("query id " + targetName);  /* The object to find in SIMBAD */

        final String simbadScript = buffer.toString();
        buffer.setLength(0); /* reset the buffer */

        InputStream scriptOutput = null;
        try {

            /* Forge the full URL of the script, using the UTF-8 unicode charset */
            final String encodedScript = URLEncoder.encode(simbadScript, "UTF-8");
            final String simbadURL = _simbadBaseURL + encodedScript;

            /* Submit the SIMBAD script and read the output, line by line */
            scriptOutput = new URL(simbadURL).openStream();
            final InputStreamReader inputStreamReader = new InputStreamReader(scriptOutput);
            final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            String currentLine = null;
            while ((currentLine = bufferedReader.readLine()) != null) {
                if (buffer.length() > 0) {
                    buffer.append('\n');
                }
                buffer.append(currentLine);
            }

            /* Return the string representation of the SIMBAD output */
            return buffer.toString();

        } catch (IOException ex) {
            throw new SIMBADQueryException();
        } finally {
            try {
                scriptOutput.close();
            }
            catch (IOException ex) {
                /* No need to abort the execution if the connection to SIMBAD
                 * could not be properly closed. Not an ideal scenario, of
                 * course, but neither apocalyptic. We can live with that. */
                }
        }
    }

    public String call() throws SIMBADQueryException {
        return this.query_SIMBAD(this.targetName);
    }
}
