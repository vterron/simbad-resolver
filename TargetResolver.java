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

import java.net.URL;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.StringTokenizer;

/* The mandatory acknowledgment: the process of writing this class was heavily
 * simplified by the fact that the Jean-Marie Mariotti Center has a Java class
 * with the same functionality and available online, which considerably helped
 * me understand how scripts are submitted to SIMBAD:
 * http://mariotti.fr/MCS/MAR2011/doc/jmcs/api/html/StarResolver_8java-source.html */

public class TargetResolver {

    /* The URL to which the script is submitted to SIMBAD */
    public static final String _simbadBaseURL = 
            "http://simbad.u-strasbg.fr/simbad/sim-script?script=";

    /* The beginning of the output of SIMBAD if an error is encountered when
     * parsing the script. This value will be used so that after submitting
     * our query we can easily know whether everything worked properly. */ 
    public static final String _simbadErrorStart = "::error::";
    
    /* These are the default values that TargetResolver uses in order to extract
     * the coordinates of an object from the SIMBAD database. In case different
     * values (such as ICRS as the reference system or 1950 as epoch) are
     * needed, use the parameterized constructor instead */
   
    public static ReferenceSystem DEFAULT_SYSTEM = ReferenceSystem.ICRS;
    public static int DEFAULT_EPOCH = 2000;
    public static int DEFAULT_EQUINOX = 2000;
    
    public ReferenceSystem system;
    public int epoch;
    public int equinox;
       
    public TargetResolver() {
        this.system = DEFAULT_SYSTEM;
        this.epoch = DEFAULT_EPOCH;
        this.equinox = DEFAULT_EQUINOX;
    }
        
    public TargetResolver(ReferenceSystem system, int epoch, int equinox){
        this.system = system;
        this.epoch = epoch;
        this.equinox = equinox;
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
        buffer.append("%COO(A;"   + line_remainder);  /* RA (sexagesimal degrees) */
        buffer.append("%COO(d;D;" + line_remainder);  /* DEC (decimal degrees) */
        buffer.append("%COO(D;"   + line_remainder);  /* DEC (sexagesimal degrees) */
    
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
                 * course, but neither apocalyptical. We can live with that. */
                }
        }
    }
    
    /* The main method of the class and the only one you should care about:
     * it receives the name of the object (known by SIMBAD as the identifier),
     * queries SIMBAD and encapsulates its output as a TargetInformation
     * instance. Throws SIMBADQueryException if the connection to SIMBAD
     * failed, and TargetNotFoundException if the object could not be found
     * in the database. Yes, who would have guessed that an exception with
     * that name would do *that*? */
     
    public TargetInformation submit (String targetName)
            throws SIMBADQueryException, TargetNotFoundException {

        /* What follows is an Easter egg, as understood by an admirer of Isaac
         * Asimov: if asked to resolve the target "Trantor", the resolved 
         * coordinates are those of Sagittarius A* (located at the center of
         * the Milky Way), but reported to belong to this fictional planet.
         * In case you do not fully understand this hidden message, I humbly
         * urge you to read Asimov's Foundation (1951) */
        
        final String _easterEggTargetName = "Trantor";
        if (targetName != null &&
                targetName.toLowerCase().equals(_easterEggTargetName.toLowerCase())) {
            TargetInformation easterInfo = new TargetResolver().submit("Sagittarius A*");
            easterInfo.name = _easterEggTargetName; 
            easterInfo.object_type = "Capital of the Galactic Empire";
            return easterInfo;
        }
                
        String simbadResult = this.query_SIMBAD(targetName);
    
        /* There possibly are many things that could go wrong (I have been
         * unable to find a list with all the error codes on the SIMBAD
         * website) but, since the syntax of the script submitted to the
         * database has been thoroughly tested and is known to be correct,
         * we can safely assume that errors will only (most of the time, at
         * least) happen when the object cannot be found. That is why we return
         * a TargetNotFoundException: although a different error could have
         * occurred, for our purposes in the PANIC Observation Tool it is
         * equivalent to the object not being found. */
        
        if (simbadResult.startsWith(TargetResolver._simbadErrorStart))
            throw new TargetNotFoundException();
            
        final TargetInformation info = new TargetInformation(targetName);
        info.epoch   = this.epoch;
        info.equinox = this.equinox;
        info.system  = this.system;
    
        /* Parse the output of SIMBAD, line by line */                
        StringTokenizer lineTokenizer = new StringTokenizer(simbadResult, "\n");
    
        /* For what so far I have seen, SIMBAD seems to always return "~" when
         * a data item is not known. That is why we need to catch the exceptions
         * that may be thrown if a conversion to Double fails and do nothing,
         * so that the missing attributes of the class keep being null */
        
        try {
            /* First line of the output: right ascension, in decimal degrees */ 
            info.ra_deg = Double.parseDouble(lineTokenizer.nextToken());
        } catch (NumberFormatException ex) {}  
    
        /* Second line: right ascension, in sexagesimal numbers (a string) */
        info.ra = lineTokenizer.nextToken();
            
        try {
            /* Third line: declination, in decimal degrees */
            info.dec_deg = Double.parseDouble(lineTokenizer.nextToken());
        } catch (NumberFormatException ex) {}
    
        /* Fourth line: declination, in sexagesimal (string) */
        info.dec = lineTokenizer.nextToken();
    
        /* Fifth line: classification of the object */
        info.object_type = lineTokenizer.nextToken();
    
        try {
            /* Sixth line: proper motion on the right ascension axis */
            info.pm_ra = Double.parseDouble(lineTokenizer.nextToken());
        } catch (NumberFormatException ex) {}
    
        try {
            /* Seventh line: proper motion on the declination axis */
            info.pm_dec = Double.parseDouble(lineTokenizer.nextToken());
        } catch (NumberFormatException ex) {}
        
        return info;
    }

    
    /* An illustration of how TargetResolver might be used in real code. You
     * may opt to catch the Exception generic exception instead of the two
     * (TargetNotFoundException and SIMBADQueryException) that TargetResolver
     * throws, but please bear in mind that this practice is generally frowned
     * upon by experienced programmers as it may lead to error hiding */  
          
    public static void main(String[] args) {
        
        TargetResolver resolver = new TargetResolver();
        try {
            TargetInformation info = resolver.submit(args[0]);
            System.out.println(info);
        } catch (TargetNotFoundException e) {
            System.out.println("not found!");
        } catch (SIMBADQueryException e) {
            System.out.println("connection failed");
        }
    }
}