/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.storage.owncloud;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Simple wrapper of JSON owncloud files.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Directories {

	/**
	 * Files/directories.
	 */
	private List<Directory> files;
}
