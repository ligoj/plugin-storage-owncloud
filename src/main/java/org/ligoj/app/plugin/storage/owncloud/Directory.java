package org.ligoj.app.plugin.storage.owncloud;

import org.ligoj.bootstrap.core.NamedBean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * OwnClound directory.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Directory extends NamedBean<Integer> {

	private long size;

}
