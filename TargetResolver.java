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

import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* The mandatory acknowledgment: the process of writing this class was heavily
 * simplified by the fact that the Jean-Marie Mariotti Center has a Java class
 * with the same functionality and available online, which considerably helped
 * me understand how scripts are submitted to SIMBAD:
 * http://mariotti.fr/MCS/MAR2011/doc/jmcs/api/html/StarResolver_8java-source.html */

public class TargetResolver {

    /* The beginning of the output of SIMBAD if an error is encountered when
     * parsing the script. This value will be used so that after submitting
     * our query we can easily know whether everything worked properly. */ 
    public static final String _simbadErrorStart = "::error::";
    
    /* The maximum time to wait when then query is submitted to SIMBAD, given
     * in seconds. If this time is exceeded the connection will be aborted and
     * the SIMBADQueryException thrown. The SIMBAD database is reliable enough
     * as to be expected to be online at all times, but at the very least this
     * will prevent our code from hanging up in case network access is lost */     
    public static final long TIMEOUT = 5;
    
    
    /* These are the default values that TargetResolver uses in order to extract
     * the coordinates of an object from the SIMBAD database. In case different
     * values (such as ICRS as the reference system or 1950 as epoch) are
     * needed, use the parameterized constructor instead */
   
    public static ReferenceSystem DEFAULT_SYSTEM = ReferenceSystem.ICRS;
    public static int DEFAULT_EPOCH = /* The current year (Universal Time) */ 
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR);
    public static int DEFAULT_EQUINOX = 2000;
    
    public ReferenceSystem system;
    public int epoch;
    public int equinox;
       
    public TargetResolver() {
        this.system  = DEFAULT_SYSTEM;
        this.epoch   = DEFAULT_EPOCH;
        this.equinox = DEFAULT_EQUINOX;
    }
        
    public TargetResolver(ReferenceSystem system, int epoch, int equinox){
        this.system = system;
        this.epoch = epoch;
        this.equinox = equinox;
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
         * urge you to read Asimov's "Foundation" (1951) */
        
        final String _easterEggTargetName = "Trantor";
        if (targetName != null &&
                targetName.toLowerCase().equals(_easterEggTargetName.toLowerCase())) {
            TargetInformation easterInfo = new TargetResolver().submit("Sagittarius A*");
            easterInfo.name = _easterEggTargetName; 
            easterInfo.object_type = "Capital of the Galactic Empire";
            return easterInfo;
        }
              

        /* Run our query to SIMBAD in a thread and retrieve the result. But do
         * not wait endlessly for the query to complete; after TIMEOUT seconds,
         * we give up and the wait times out. */        
        
        Callable<String> querier =
                new SIMBADQuerier(targetName, this.system, this.epoch, this.equinox);
        
        ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
        Future<String> threadResult = threadExecutor.submit(querier);
        threadExecutor.shutdown();
        
        String simbadResult;
        
        try {
            simbadResult = threadResult.get(TargetResolver.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new SIMBADQueryException();
        } catch (ExecutionException ex) {
            throw new SIMBADQueryException();
        } catch (TimeoutException ex) {
            threadResult.cancel(true);
            throw new SIMBADQueryException(); 
        }
          
    
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
    
        /* Convert the decimal degrees of the right ascension to hours, minutes
         * and seconds, and format them as a string such as "21 38 08.74. Yes,
         * we could obtain the coordinates in sexagesimal directly from SIMBAD,
         * but doing the conversion ourselves gives us total control over how
         * these coordinates are formatted " */
        
        double coords[] = null;
        if (info.ra_deg == null) {
            info.ra = null;
        }
        
        else {
            coords = SIMBADQuerier.DD_to_HMS(info.ra_deg);
            int hours = (int) coords[0];
            int minutes = (int) coords[1];
            double seconds = coords[2];
            info.ra = String.format("%02d %02d %05.2f", hours, minutes, seconds);    
        }
        
        try {
            /* Second line: declination, in decimal degrees */
            info.dec_deg = Double.parseDouble(lineTokenizer.nextToken());
        } catch (NumberFormatException ex) {}
    
        /* Decimal degrees of the declination to degrees, arcminutes and
         * arcseconds, and format them as a string such as "+63 45 22.3 " */
           
        if (info.dec_deg == null) {
            info.dec = null;
        }
        
        else {
            coords = SIMBADQuerier.DD_to_DMS(info.dec_deg);
            int degrees = (int) coords[0];
            int arcmins = (int) coords[1];
            double arcsecs = coords[2];
            info.dec = String.format("%+03d %02d %4.1f", degrees, arcmins, arcsecs);
        }
        
        /* Third line: classification of the object */
        info.object_type = lineTokenizer.nextToken();
    
        try {
            /* Fourth line: proper motion on the right ascension axis */
            info.pm_ra = Double.parseDouble(lineTokenizer.nextToken());
        } catch (NumberFormatException ex) {}
    
        try {
            /* Fifth line: proper motion on the declination axis */
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
        
        System.exit(0);
    }
}