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
import java.util.Random;


public class TargetResolverTest {

	private static String OBJECTS_PATH = "SIMBAD_objects";
	private static ArrayList<String> targetNames;
	private static ArrayList<TargetResolver> targetResolvers;
    private static Random generator = new Random();
    
	public static ArrayList<String> loadTargetsFile(String path)
			throws FileNotFoundException, IOException{
		
		ArrayList<String> targetNames = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line = null;
		while ((line = br.readLine()) != null) {
			if (!line.replaceAll("\\s", "").startsWith("#"))
				targetNames.add(line);
		}		
		br.close();
		return targetNames;
	}
	
	// http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string-in-java
	public static String randomString() {
		return Long.toHexString(Double.doubleToLongBits(Math.random()));
		
	}

	
	@org.junit.BeforeClass
	public static void SetUp() throws FileNotFoundException, IOException {
		TargetResolverTest.targetNames = TargetResolverTest.loadTargetsFile(OBJECTS_PATH);
		targetNames.add("Trantor"); /* easter egg in TargetResolver.submit(String) */
		targetNames.add(null);
		targetNames.add("");
		for (int i = 0 ; i < 10 ; i++)
			targetNames.add(randomString());
		
		// and now a different resolver for each target
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

	/* Right after calling the constructor, all the attributes of the
	 * instance except for the name of the target should be null */
	
	@org.junit.Test
	public void testTargetInformation() {
		
		for (int index = 0 ; index < TargetResolverTest.targetNames.size() ; index++) {
			String targetName = TargetResolverTest.targetNames.get(index);
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
	
	
	@Test
	public void testTargetResolver() {
		
		TargetResolver resolver = new TargetResolver();
		assertEquals(resolver.system, TargetResolver.DEFAULT_SYSTEM);
		assertEquals(resolver.epoch, TargetResolver.DEFAULT_EPOCH);
		assertEquals(resolver.equinox, TargetResolver.DEFAULT_EQUINOX);				
	}

	
	@Test
	public void testTargetResolverStringIntInt() {
		
		/* Try the parameterized constractor with random values... */
		ReferenceSystem system = ReferenceSystem.random();
		int currentYear = Calendar.getInstance().get(Calendar.YEAR);
		int epoch = generator.nextInt(currentYear + 1);
		int equinox = generator.nextInt(currentYear + 1);
		
		TargetResolver resolver = new TargetResolver(system, epoch, equinox);
		assertEquals(resolver.system, system);
		assertEquals(resolver.epoch, epoch);
		assertEquals(resolver.equinox, equinox);
	}
		
	
	@Test
	public void testSubmit() {
		
		/* Resolve each object using both the default resolver and a random one */
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
				fail("connection to SIMBAD failed (?)");
			} catch (TargetNotFoundException e) {
				/* Nothing... */
				System.out.println("NOT FOUND");
			} catch (Exception e) {
				System.out.println("-- " + targetName + "--");
				e.printStackTrace();				
				fail("unexpected exception WTF");
			}
		}
				
		System.out.print(TargetInformation.newline);
		System.out.printf("Found: %d/%d (%.3f%%)%s", foundCounter, totalTargets,
				100.0 * foundCounter / totalTargets, TargetInformation.newline);
	}
	

}
