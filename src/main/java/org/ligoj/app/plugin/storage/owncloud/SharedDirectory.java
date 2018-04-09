package org.ligoj.app.plugin.storage.owncloud;

import org.ligoj.bootstrap.core.INamableBean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * OwnClound shared directory.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(of = "id")
public class SharedDirectory implements INamableBean<Integer> {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	@JsonProperty("file_source")
	private Integer id;

	@JsonProperty("file_target")
	private String name;

	@JsonProperty("item_type")
	private String type;

	@JsonProperty(access = Access.WRITE_ONLY)
	private String path;
}
