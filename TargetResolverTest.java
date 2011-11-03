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

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;


public class TargetResolverTest {

    /* Path to the file containing the name of a series of objects, each one
     * of which will be resolved in order to test the TargetResolverTest. As
     * of October 3, 2011, there are 1030 different (and actual) objects: stars,
     * exoplanets and galaxies */    
    private static String OBJECTS_PATH = "SIMBAD_objects";
    
    private static ArrayList<String> targetNames;
    private static ArrayList<TargetResolver> targetResolvers;
    private static Random generator = new Random();
    /* The number of random (non-existent) to be (unsuccessfully) resolved) */
    private static int nRandomStrings = 50;
    
    
    /* Receives the path to the file containing the list of targets, one per
     * line, and returns them as an ArrayList of Strings. Lines whose first
     * non-blank character is '#' are treated as comments and ignored */
    
    public static ArrayList<String> loadTargetsFile(String path)
            throws FileNotFoundException, IOException{
        
        ArrayList<String> targetNames = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line = null;
        while ((line = br.readLine()) != null) {
            /* Ignore leading whitespaces */
            if (!line.replaceAll("\\s", "").startsWith("#"))
                targetNames.add(line);
        }        
        br.close();
        return targetNames;
    }
    
    
    /* Returns a randomly-generated String, such as "3fed6a57652ea9b7". A few
     * of these will be used in order to also test TargetResolver for
     * non-existent object. Seen at: http://stackoverflow.com/questions/41107/\
     * how-to-generate-a-random-alpha-numeric-string-in-java/1439556#1439556 */
    
    public static String randomString() {
        return Long.toHexString(Double.doubleToLongBits(Math.random()));
        
    }

    
    /* The global fixture, as the @BeforeClass annotation causes the method to
     * be run once before any of the test methods in the class. In it, we load
     * the list of SIMBAD objects from the file, manually add some outliers
     * (null and empty Strings) and also generate a series of random Strings,
     * to make sure that TargetResolver behaves as expected when a non-existent
     * object is received as input.
     * 
     * For each object, a different instance of TargetResolver is used: the
     * celestial reference frame is randomly selected from those that SIMBAD
     * allows, namely ICRS, FK5, FK4, GAL, SGAL and ECL. For the epoch and
     * equinox, a random year between zero and the current one is used. */
    
    @org.junit.BeforeClass
    public static void SetUp() throws FileNotFoundException, IOException {
        TargetResolverTest.targetNames = TargetResolverTest.loadTargetsFile(OBJECTS_PATH);
        targetNames.add("Trantor"); /* easter egg in TargetResolver.submit(String) */
        targetNames.add(null);
        targetNames.add("");
        for (int i = 0 ; i < nRandomStrings ; i++)
            targetNames.add(randomString());
        
        /* A different, random (in terms of celestial reference syste, epoch
         * and equinox) TargetResolver instance for each target */
        TargetResolverTest.targetResolvers = new ArrayList<TargetResolver>(targetNames.size()); 
        for (int index = 0 ; index < TargetResolverTest.targetNames.size() ; index++) {
            ReferenceSystem system = ReferenceSystem.random();
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int epoch = generator.nextInt(currentYear + 1);
            int equinox = generator.nextInt(currentYear + 1);
            targetResolvers.add(new TargetResolver(system, epoch, equinox));
        }
        
        assert (targetNames.size() == targetResolvers.size());
    }

    
    /* Make sure that, right after invoking the constructor, all the attributes
     * of the instance, expect for the name of the target, are null */
    
    @org.junit.Test
    public void testTargetInformation() {
        
        Iterator<String> it = TargetResolverTest.targetNames.iterator();
        while (it.hasNext()) {
            String targetName = it.next();
            TargetInformation info = new TargetInformation(targetName);
            assertTrue(targetName == null || info.name.equals(targetName));
            assertNull(info.ra_deg);
            assertNull(info.dec_deg);
            assertNull(info.ra);
            assertNull(info.ra_deg);
            assertNull(info.epoch);
            assertNull(info.equinox);    
            assertNull(info.system);
            assertNull(info.pm_ra);
            assertNull(info.pm_dec);
            assertNull(info.object_type);
        }
    }
    
    
    /* Make sure that the default reference system, epoch and equinox values
     * used by TargetResolver (that is, when the non-parameterized constructor
     * is invoked) are the expected ones */
        
    @Test
    public void testTargetResolver() {
        
        TargetResolver resolver = new TargetResolver();
        assertEquals(resolver.system, TargetResolver.DEFAULT_SYSTEM);
        assertEquals(resolver.epoch, TargetResolver.DEFAULT_EPOCH);
        assertEquals(resolver.equinox, TargetResolver.DEFAULT_EQUINOX);                
    }

    
    /* Make sure that the parameterized constructor correctly sets the
     * values of the celestial reference system, epoch and equinox */
         
    @Test
    public void testTargetResolverStringIntInt() {
        
        /* A random reference system as well as epoch and equinox years  */
        ReferenceSystem system = ReferenceSystem.random();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int epoch = generator.nextInt(currentYear + 1);
        int equinox = generator.nextInt(currentYear + 1);
        
        TargetResolver resolver = new TargetResolver(system, epoch, equinox);
        assertEquals(resolver.system, system);
        assertEquals(resolver.epoch, epoch);
        assertEquals(resolver.equinox, equinox);
    }
        
    
    /* The unit test that summarizes them all. Each object is resolved twice:
     * one with one of the random resolvers generated at SetUp() and another
     * one with the default resolver -- as it is the one that the PANIC
     * Observation Tool shall expectedly use. There are only two exceptions
     * that we expect, SIMBADQueryException and TargetNotFoundException, so
     * any one different that those two should never be thrown.  
     * 
     * Just for recreational and useless statistical purposes, right before
     * exiting the method will print to standard output the percentage of
     * resolutions which were successful. In my meandering experience, in many
     * cases SIMBAD is unable to resolve existing targets when an eccentric
     * resolver (say, the SGAL reference system, 570 AD as epoch and and
     * equinox of 1453 AD) is used for the query */
    
    @Test
    public void testSubmit() {
        
        /* Duplicate targetNames, appending it to itself, so that each object
         * can also be tested using the default TargetResolver */
        assert (targetNames.size() == targetResolvers.size());
        targetNames.addAll(targetNames);
        while (targetResolvers.size() < targetNames.size()) {
            targetResolvers.add(new TargetResolver());            
        }

        int foundCounter = 0;
        int totalTargets = TargetResolverTest.targetNames.size();
        for (int index = 0 ; index < totalTargets ; index++) {            
            String targetName = targetNames.get(index);
            TargetResolver resolver = targetResolvers.get(index);
            System.out.printf("%4d | %s --> ", index, targetName);
            try {
                resolver.submit(targetName);
                System.out.println("OK");
                foundCounter++;
            } catch (SIMBADQueryException e) {
                System.out.println("connection to SIMBAD failed");
            } catch (TargetNotFoundException e) {
                System.out.println("NOT FOUND");
            } catch (Exception e) {
                System.out.println("-- " + targetName + "--");
                e.printStackTrace();                
                fail("unexpected exception");
            }
        }
                
        System.out.print(TargetInformation.newline);
        System.out.printf("Found: %d/%d (%.3f%%)%s", foundCounter, totalTargets,
                100.0 * foundCounter / totalTargets, TargetInformation.newline);
    }
    

}