package org.ligoj.app.plugin.storage.owncloud;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Simple wrapper of JSON owncloud shared directories.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SharedDirectories {

	/**
	 * Shared files/directories.
	 */
	private List<SharedDirectory> data;
}
