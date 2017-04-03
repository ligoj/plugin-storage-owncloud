package org.ligoj.app.plugin.storage.owncloud;

import org.junit.Test;
import org.ligoj.app.plugin.storage.owncloud.SharedDirectory;
import org.ligoj.bootstrap.model.AbstractBusinessEntityTest;

/**
 * Test class of {@link SharedDirectory}
 */
public class SharedDirectoryTest extends AbstractBusinessEntityTest {

	@Test
	public void testEqualsAndHash() throws Exception {
		testEqualsAndHash(SharedDirectory.class);
	}
}
