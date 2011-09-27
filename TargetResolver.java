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

import java.net.URL;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class TargetResolver {

    public static String ref_system = "FK5";
    public static int epoch = 2000;
    public static int equinox = 2000;  // these values are Matilde-certified.

    public static final String _simbadBaseURL = "http://simbad.u-strasbg.fr/simbad/sim-script?script=";

    public static String query_SIMBAD (String targetName) {

	// For the SIMBAD script and the result
	StringBuilder buffer = new StringBuilder();

	/* Refer to the following (and ugly) URL for a complete explanation of
	   the parameters being used here: http://swedishdanish.appspot.com/u?\
           purl=dHBpcmNzZi1taXM9ZWdhUD9wbGVoLW1pcy9kYWJtaXMvcmYuZ2JzYXJ0cy11Lm\
           RhYm1pcy8vOnB0%0AdGg%3D%0A */

	buffer.append("output console=off script=off\n"); // Just data
	buffer.append("format object form1 \""); // Simbad script preambule

	/* %COO(options) --> the option string is made of 5 parts separated by semicolons:
	   formatting-options ';' element-list ';' frame ';' epoch ';' equinox.
	   Note that the epoch must be prefixed by 'B' or 'J'
	   Formatting option: 'd' --> display the coordinates in decimal numbers
	   Element list: 'A' --> right ascension, 'D' --> declination */

	String line_remainder = TargetResolver.ref_system + ";" +
                                "J" + TargetResolver.epoch + ";" +
                                TargetResolver.equinox + ")\\n";

	buffer.append("%COO(d;A;" + line_remainder);  // RA (decimal degrees)
        buffer.append("%COO(A;"   + line_remainder);  // RA (sexagesimal)
	buffer.append("%COO(d;D;" + line_remainder);  // DEC (decimal degrees)
        buffer.append("%COO(D;"   + line_remainder);  // DEC (sexagesimal)

	buffer.append("%OTYPE(V)\\n");  // Object type (verbose)
	buffer.append("%PM(A)\\n");     // Proper motion on the RA axis

	buffer.append("%PM(D)\\n");     // Proper motion on the DEC axis
	//buffer.append("%IDLIST[%*,]\\n");    // all the identifier of the object, one per line
	buffer.append("\"\n"); // end of SIMBAD script
	buffer.append("query id " + targetName); // Add the object name we are looking for

	final String simbadScript = buffer.toString();

	InputStream inputStream = null;

	try {
	    // Forge the URL int UTF8 unicode charset
	    final String encodedScript = URLEncoder.encode(simbadScript, "UTF-8");
	    final String simbadURL = _simbadBaseURL + encodedScript;

	    inputStream = new URL(simbadURL).openStream();
	    final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
	    final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

	    // Read incoming data line by line
	    String currentLine = null;

	    // reset buffer :
	    buffer.setLength(0);

	    while ((currentLine = bufferedReader.readLine()) != null) {
		if (buffer.length() > 0) {
		    buffer.append('\n');
		}

		buffer.append(currentLine);
	    }

	    return buffer.toString();

	} catch (Exception ex) {
	    //System.out.println("SIMBAD query failed");
	    return null;
	}
    }

    	/* If the identifier is not found in the database, SIMBAD returns the following:
	::error:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	[empty line]
	[3] Identifier not found in the database : WASP-234
	*/

    /* Returns 0 if everything went wrong, 1 otherwise.
       TODO: is there (somewhere) a list of SIMBAD error codes?
    */
    public static int errorCheck(String simbad_output) {

	/* In case of error, SIMBAD returns something like this:
	::error:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	[empty line]
	[3] Identifier not found in the database : WASP-234
	*/

	final String errorStart = "::error::";
	StringTokenizer lineTokenizer = new StringTokenizer(simbad_output, "\n");
	if(lineTokenizer.nextToken().startsWith(errorStart))
	    return 1;
	else
	    return 0;
    }


    public static TargetInformation resolve (String targetName){

	/*******************************************************************/
	/* See: http://mariotti.fr/MCS/MAR2011/doc/jmcs/api/html/StarResolver_8java-source.html */
	/*******************************************************************/

	String simbadResult = query_SIMBAD(targetName);
	// in case the query to SIMBAD failed
	if (simbadResult == null)
	    return null;

	/* Check that the query went all right */
	int retcode = TargetResolver.errorCheck(simbadResult);

	if(retcode != 0)
	    return null;  /* TODO: throw our own type of exception */

	/**************** TEMPORARILY COMMENTED OUT. DO WE NEED THIS? *****/
	/* Remove any blanking character (~)
	simbadResult = simbadResult.replaceAll("~[ ]*", "");
	System.out.println(simbadResult);
	*******************************************************************/

	final TargetInformation info = new TargetInformation(targetName);
	info.epoch      = TargetResolver.epoch;
	info.equinox    = TargetResolver.equinox;
	info.ref_system = TargetResolver.ref_system;

	/* Parse the output of SIMBAD, line by line.
	   SIMBAD returns "~" when something is not known */
	StringTokenizer lineTokenizer = new StringTokenizer(simbadResult, "\n");

	/* 1st line: RA, in decimal degrees */
	info.ra_deg = Double.parseDouble(lineTokenizer.nextToken());

	/* 2nd line: RA, in sexagesimal */
	info.ra = lineTokenizer.nextToken();

	/* 3rd line: DEC, in decimal degrees */
	info.dec_deg = Double.parseDouble(lineTokenizer.nextToken());

	/* 4th line: DEC, in sexagesimal */
	info.dec = lineTokenizer.nextToken();

	/* 5th line: object type */
	info.object_type = lineTokenizer.nextToken();

	/* 6th line: proper motion on the RA axis */
	try {
	    info.pm_ra = Double.parseDouble(lineTokenizer.nextToken());
	}
	catch (NumberFormatException e) {
	    info.pm_ra = Double.NaN;
	}

	/* 7th line: motion on the DEC axis */
	try {
	    info.pm_dec = Double.parseDouble(lineTokenizer.nextToken());
	}
	catch (NumberFormatException e) {
	    info.pm_dec = Double.NaN;
	}

	/* 8th and subsequent lines store the different identifiers for the
	 * object returned by SIMBAD. For now we are only interested in the
	 * first one (which is apparently also the most 'populer'); the rest is
	 * silently ignored
	info.name = lineTokenizer.nextToken();
	************************/
	return info;
    }

    public static void main(String[] args) {
	TargetInformation info = TargetResolver.resolve(args[0]);
	System.out.println(info);
    }
}
