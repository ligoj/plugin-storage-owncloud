/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.storage.owncloud;

import org.junit.jupiter.api.Test;
import org.ligoj.bootstrap.model.AbstractBusinessEntityTest;

/**
 * Test class of {@link SharedDirectory}
 */
public class SharedDirectoryTest extends AbstractBusinessEntityTest {

	@Test
	void testEqualsAndHash() throws Exception {
		testEqualsAndHash(SharedDirectory.class);
	}
}
