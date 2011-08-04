package core;

import junit.framework.Assert;

import org.hibernate.Session;
import org.iucn.sis.server.api.io.RegionIO;
import org.iucn.sis.server.api.persistance.RegionCriteria;
import org.iucn.sis.server.api.persistance.SISPersistentManager;
import org.iucn.sis.server.api.persistance.hibernate.PersistentException;
import org.iucn.sis.shared.api.debug.Debug;
import org.iucn.sis.shared.api.models.Region;
import org.junit.After;
import org.junit.Test;

public class RegionIOTest extends BasicHibernateTest {
	
	/**
	 * Ensure I can add and edit regions.  If this 
	 * doesn't work, other tests will clearly fail.
	 */
	@Test
	public void testAddEditRegion() {
		Session session = openTransaction();
		RegionIO io = new RegionIO(session);
		
		Region newRegion = new Region();
		newRegion.setName("JUnit Test Region");
		newRegion.setDescription("JUnit Test Region");
		
		try {
			io.saveRegion(newRegion);
			session.getTransaction().commit();
		} catch (PersistentException e) {
			Assert.fail("Exception thrown: " + e.getMessage());
		}
		
		int id = newRegion.getId();
		session.beginTransaction();
		Assert.assertTrue(id != 0);
		
		newRegion.setName("JUnit Test Region Renamed");
		
		try {
			io.saveRegion(newRegion);
			closeTransation(session);
		} catch (PersistentException e) {
			Assert.fail("Exception thrown: " + e.getMessage());
		}
		
		session = openSession();
		io = new RegionIO(session);
		
		Region existing = io.getRegion(id);
		Assert.assertNotNull(existing);
		Assert.assertEquals(id, existing.getId());
		Assert.assertEquals("JUnit Test Region Renamed", existing.getName());
	}
	
	/**
	 * Ensure that no duplicates are allowed, that is,
	 * if I add two regions with the same name, it 
	 * should not work.
	 */
	@Test
	public void testNoDuplicatesAllowed() {
		final String sameName = "JUnit Test Region";
		Session session = openTransaction();
		RegionIO io = new RegionIO(session);
		
		Region newRegion = new Region();
		newRegion.setName(sameName);
		newRegion.setDescription("JUnit Test Region");
		
		try {
			io.saveRegion(newRegion);
			session.getTransaction().commit();
		} catch (PersistentException e) {
			Assert.fail("Exception thrown: " + e.getMessage());
		}
		
		int firstID = newRegion.getId();
		closeSession(session);
		
		Assert.assertTrue(firstID != 0);
		
		session = openTransaction();
		io = new RegionIO(session);
		
		int size = 0;
		try {
			size = io.getRegions().size();
		} catch (PersistentException e) {
			Assert.fail("Dependency failed: list regions");
		}
	
		Region brandNewRegion = new Region();
		brandNewRegion.setName(sameName);
		brandNewRegion.setDescription("Second JUnit Test Region, could be a duplicate");
		
		try {
			io.saveRegion(brandNewRegion);
			session.getTransaction().commit();
			Debug.println("This should have failed...");
		} catch (Exception e) {
			session.getTransaction().rollback();
			Debug.println("Failed to save regions, probably a good thing...: {0}", e.getMessage());
		}
		
		int newSize = 0;
		try {
			newSize = io.getRegions().size();
		} catch (PersistentException e) {
			Assert.fail("Dependency failed: list regions");
		}
		
		Assert.assertTrue(brandNewRegion.getId() == 0);
		Assert.assertEquals(size, newSize);
	}
	
	
	/**
	 * Ensure that no duplicates are allowed, even 
	 * if the case doesn't match.
	 */
	@Test
	public void testNoDuplicatesAllowedCaseInsensitive() {
		final String sameName = "JUnit Test Region";
		Session session = openTransaction();
		RegionIO io = new RegionIO(session);
		
		Region newRegion = new Region();
		newRegion.setName(sameName);
		newRegion.setDescription("JUnit Test Region");
		
		try {
			io.saveRegion(newRegion);
			session.getTransaction().commit();
		} catch (PersistentException e) {
			Assert.fail("Exception thrown: " + e.getMessage());
		}
		
		int firstID = newRegion.getId();
		closeSession(session);
		
		Assert.assertTrue(firstID != 0);
		
		session = openTransaction();
		io = new RegionIO(session);
		
		int size = 0;
		try {
			size = io.getRegions().size();
		} catch (PersistentException e) {
			Assert.fail("Dependency failed: list regions");
		}
	
		//Name is now lower-case... but should still fail.
		Region brandNewRegion = new Region();
		brandNewRegion.setName(sameName.toLowerCase());
		brandNewRegion.setDescription("Second JUnit Test Region, could be a duplicate");
		
		try {
			io.saveRegion(brandNewRegion);
			session.getTransaction().commit();
			Debug.println("This should have failed...");
		} catch (Exception e) {
			session.getTransaction().rollback();
			Debug.println("Failed to save regions, probably a good thing...: {0}", e.getMessage());
		}
		
		int newSize = 0;
		try {
			newSize = io.getRegions().size();
		} catch (PersistentException e) {
			Assert.fail("Dependency failed: list regions");
		}
		
		Assert.assertTrue(brandNewRegion.getId() == 0);
		Assert.assertEquals(size, newSize);
	}
	
	/**
	 * This method wipes any regions created via this 
	 * JUnit test (named something ilike "JUnit Test") 
	 * from the database.
	 */
	@After
	public void afterMethod() {
		Session session = openTransaction();
		RegionCriteria criteria = new RegionCriteria(session);
		criteria.name.ilike("%JUnit Test%");
		Region[] regions = criteria.listRegion();
		if (regions != null) {
			for (Region region : regions) {
				try {
					SISPersistentManager.instance().deleteObject(session, region);
					Debug.println("Deleted test region {0}: {1}", region.getId(), region.getName());
				} catch (Exception e) {
					Debug.println("Failed to delete test region {0}: {1}", region.getId(), region.getName());
				}
			}
			session.getTransaction().commit();
		}
		session.close();
	}

}
