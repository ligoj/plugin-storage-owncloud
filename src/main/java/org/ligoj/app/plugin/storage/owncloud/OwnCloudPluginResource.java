package org.ligoj.app.plugin.storage.owncloud;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Format;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.plugin.storage.StorageResource;
import org.ligoj.app.plugin.storage.StorageServicePlugin;
import org.ligoj.app.resource.NormalizeFormat;
import org.ligoj.app.resource.plugin.AbstractToolPluginResource;
import org.ligoj.app.resource.plugin.AuthCurlProcessor;
import org.ligoj.app.resource.plugin.CurlProcessor;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OwnCloud resource. Also see "index.php/apps/files/ajax/list.php" : My files
 */
@Path(OwnCloudPluginResource.URL)
@Service
@Produces(MediaType.APPLICATION_JSON)
public class OwnCloudPluginResource extends AbstractToolPluginResource implements StorageServicePlugin {

	private static final String VERSION_TAG = "<h3>Version";

	/**
	 * Plug-in key.
	 */
	public static final String URL = StorageResource.SERVICE_URL + "/owncloud";

	/**
	 * Plug-in key.
	 */
	public static final String KEY = URL.replace('/', ':').substring(1);

	/**
	 * Public server URL used to fetch the last available version of the
	 * product.
	 */
	@Value("${service-storage-owncloud-server:https://owncloud.org}")
	private String publicServer;

	/**
	 * OwnCloud user name able to connect to instance.
	 */
	public static final String PARAMETER_USER = KEY + ":user";

	/**
	 * OwnCloud user password able to connect to instance.
	 */
	public static final String PARAMETER_PASSWORD = KEY + ":password";

	/**
	 * OwnCloud project's identifier, an integer
	 */
	public static final String PARAMETER_DIRECTORY = KEY + ":directory";

	/**
	 * Web site URL
	 */
	public static final String PARAMETER_URL = KEY + ":url";

	@Override
	public void link(final int subscription) throws Exception {
		final Map<String, String> parameters = subscriptionResource.getParameters(subscription);

		// Validate the project settings
		validateProject(parameters);
	}

	/**
	 * Validate the project connectivity.
	 * 
	 * @param parameters
	 *            the project parameters.
	 * @return project details.
	 */
	protected Directory validateProject(final Map<String, String> parameters) throws IOException, URISyntaxException {
		// Get project's configuration
		final int id = Integer.parseInt(ObjectUtils.defaultIfNull(parameters.get(PARAMETER_DIRECTORY), "0"));
		final Directory result = getDirectory(parameters, id);

		if (result == null) {
			// Invalid id
			throw new ValidationJsonException(PARAMETER_DIRECTORY, "owncloud-directory", id);
		}

		return result;
	}

	/**
	 * Validate the basic REST connectivity to Owncloud.
	 * 
	 * @param parameters
	 *            the server parameters.
	 * @return the detected Owncloud version.
	 */
	protected String validateAdminAccess(final Map<String, String> parameters) throws Exception {
		final String url = StringUtils.appendIfMissing(parameters.get(PARAMETER_URL), "/");

		// Check access
		CurlProcessor.validateAndClose(url, PARAMETER_URL, "owncloud-connection");

		// Authentication request
		final String version = getVersion(parameters);
		if (version == null) {
			throw new ValidationJsonException(PARAMETER_USER, "owncloud-login");
		}
		return version;
	}

	/**
	 * Return a OwnCloud's resource. Return <code>null</code> when the resource
	 * is not found.
	 */
	protected String getResource(final Map<String, String> parameters, final String resource) {
		return newProcessor(parameters).get(StringUtils.appendIfMissing(parameters.get(PARAMETER_URL), "/") + resource);
	}

	@Override
	public String getVersion(final Map<String, String> parameters) throws Exception {
		// Get the version from the JSON status
		return (String) new ObjectMapper()
				.readValue(StringUtils.defaultIfEmpty(getResource(parameters, "status.php"), "{}"), Map.class)
				.get("version");
	}

	/**
	 * Return all OwnCloud directories without limit.
	 */
	protected List<SharedDirectory> getDirectories(final Map<String, String> parameters) throws IOException {
		return new ObjectMapper()
				.readValue(
						StringUtils.removeEnd(StringUtils.removeStart(StringUtils.defaultIfEmpty(
								getResource(parameters, "ocs/v1.php/apps/files_sharing/api/v1/shares?format=json"),
								"{\"ocs\":{\"data\":[]}}"), "{\"ocs\":"), "}"),
						SharedDirectories.class)
				.getData().stream().filter(d -> "folder".equals(d.getType())).distinct().collect(Collectors.toList());
	}

	/**
	 * Return OwnCloud directory from its identifier.
	 */
	protected Directory getDirectory(final Map<String, String> parameters, final int id)
			throws IOException, URISyntaxException {
		// First, get the directory path
		final SharedDirectory sharedDirectory = getDirectories(parameters).stream()
				.filter(project -> project.getId().equals(id)).findFirst().orElse(null);
		if (sharedDirectory == null) {
			// Shared directory is not found
			return null;
		}

		// The, get the directory size from the content's size
		final String path = "index.php/apps/files/ajax/list.php?dir="
				+ new URI("http", sharedDirectory.getPath(), "").toURL().getPath();
		final String files = StringUtils.removeEnd(StringUtils.removeStart(
				StringUtils.defaultIfEmpty(getResource(parameters, path), "{\"data\":{\"files\":[]}}"), "{\"data\":"),
				"}");
		final Directory directory = new Directory();
		NamedBean.copy(sharedDirectory, directory);
		directory.setSize(new ObjectMapper().readValue(files, Directories.class).getFiles().stream()
				.mapToLong(Directory::getSize).sum());
		return directory;
	}

	/**
	 * Search the OwnCloud the projects matching to the given criteria. Name
	 * only is considered.
	 * 
	 * @param node
	 *            the node to be tested with given parameters.
	 * @param criteria
	 *            the search criteria.
	 * @return project names matching the criteria.
	 */
	@GET
	@Path("{node}/{criteria}")
	@Consumes(MediaType.APPLICATION_JSON)
	public List<Directory> findAllByName(@PathParam("node") final String node,
			@PathParam("criteria") final String criteria) throws IOException {

		// Prepare the context, an ordered set of projects
		final Format format = new NormalizeFormat();
		final String formatCriteria = format.format(criteria);
		final Map<String, String> parameters = pvResource.getNodeParameters(node);

		// Get the projects and parse them
		return getDirectories(parameters).stream().filter(d -> format.format(d.getName()).contains(formatCriteria))
				.map(d -> {
					final Directory dir = new Directory();
					dir.setId(d.getId());
					dir.setName(StringUtils.removeStart(d.getName(), "/"));
					return dir;
				}).collect(Collectors.toList());
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getLastVersion() throws Exception {
		final String changelog = StringUtils.defaultIfEmpty(new CurlProcessor().get(publicServer + "/changelog/"),
				VERSION_TAG);
		final int start = Math.min(Math.max(changelog.indexOf(VERSION_TAG), 0) + VERSION_TAG.length(),
				changelog.length());
		return changelog.substring(start, Math.max(changelog.indexOf('<', start), start));
	}

	@Override
	public SubscriptionStatusWithData checkSubscriptionStatus(final Map<String, String> parameters)
			throws Exception {
		final SubscriptionStatusWithData nodeStatusWithData = new SubscriptionStatusWithData();
		nodeStatusWithData.put("directory", validateProject(parameters));
		return nodeStatusWithData;
	}

	@Override
	public boolean checkStatus(final Map<String, String> parameters) throws Exception {
		// Status is UP <=> Administration access is UP
		validateAdminAccess(parameters);
		return true;
	}

	/**
	 * Return a new processor instance for OwnCloud.
	 */
	private CurlProcessor newProcessor(final Map<String, String> parameters) {
		return new AuthCurlProcessor(parameters.get(PARAMETER_USER), parameters.get(PARAMETER_PASSWORD));
	}

}
