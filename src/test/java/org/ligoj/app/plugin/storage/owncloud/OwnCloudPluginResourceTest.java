/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.storage.owncloud;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.transaction.Transactional;

import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractServerTest;
import org.ligoj.app.api.SubscriptionStatusWithData;
import org.ligoj.app.dao.ParameterValueRepository;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.model.ParameterValue;
import org.ligoj.app.model.Project;
import org.ligoj.app.model.Subscription;
import org.ligoj.app.resource.subscription.SubscriptionResource;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link OwnCloudPluginResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class OwnCloudPluginResourceTest extends AbstractServerTest {
	@Autowired
	private OwnCloudPluginResource resource;

	@Autowired
	private SubscriptionResource subscriptionResource;

	@Autowired
	private ParameterValueRepository parameterValueRepository;

	protected int subscription;

	@BeforeEach
	void prepareData() throws IOException {
		// Only with Spring context
		persistEntities("csv", new Class<?>[]{Node.class, Parameter.class, Project.class, Subscription.class, ParameterValue.class},
				StandardCharsets.UTF_8);
		this.subscription = getSubscription("Jupiter");

		// Coverage only
		Assertions.assertEquals("service:storage:owncloud", resource.getKey());
	}

	/**
	 * Return the subscription identifier of Jupiter. Assumes there is only one
	 * subscription for a service.
	 */
	private Integer getSubscription(final String project) {
		return getSubscription(project, OwnCloudPluginResource.KEY);
	}

	@Test
	void delete() throws Exception {
		resource.delete(subscription, false);
		em.flush();
		em.clear();
		// No custom data -> nothing to check;
	}

	@Test
	void getVersion() throws Exception {
		prepareMockAdmin();
		Assertions.assertEquals("9.0.0.19", resource.getVersion(subscription));
	}

	@Test
	void getLastVersion() {
		// For sample 9.1.0
		final int length = resource.getLastVersion().length();
		Assertions.assertTrue(length > 4);
		Assertions.assertTrue(length < 10);
	}

	@Test
	void link() throws Exception {
		prepareMockProject();
		httpServer.start();

		// Invoke create for an already created entity, since for now, there is
		// nothing but validation pour OwnCloudTM
		resource.link(this.subscription);
	}

	@Test
	void linkNotFound() throws Exception {
		prepareMockProject();
		httpServer.start();

		parameterValueRepository.findAllBySubscription(subscription).stream()
				.filter(v -> v.getParameter().getId().equals(OwnCloudPluginResource.PARAMETER_DIRECTORY)).findFirst().get().setData("0");
		em.flush();
		em.clear();

		// Invoke create for an already created entity, since for now, there is
		// nothing but validation pour Owncloud
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.link(this.subscription)), "service:storage:owncloud:directory", "owncloud-directory");
	}

	@Test
	void checkSubscriptionStatus() throws Exception {
		prepareMockProject();
		final SubscriptionStatusWithData nodeStatusWithData = resource
				.checkSubscriptionStatus(subscriptionResource.getParametersNoCheck(subscription));
		Assertions.assertTrue(nodeStatusWithData.getStatus().isUp());
		Assertions.assertEquals(138264416, ((Directory) nodeStatusWithData.getData().get("directory")).getSize());
	}

	private void prepareMockProject() throws IOException {
		// Main entry
		httpServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody("")));

		// Status
		httpServer.stubFor(get(urlEqualTo("/status.php")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(
				IOUtils.toString(new ClassPathResource("mock-server/owncloud/status.php").getInputStream(), StandardCharsets.UTF_8))));

		// Shares json
		httpServer.stubFor(get(urlEqualTo("/ocs/v1.php/apps/files_sharing/api/v1/shares?format=json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(IOUtils
						.toString(new ClassPathResource("mock-server/owncloud/sharing.php").getInputStream(), StandardCharsets.UTF_8))));

		// Directories json
		httpServer.stubFor(get(urlEqualTo("/index.php/apps/files/ajax/list.php?dir=/projects/Sample"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(IOUtils
						.toString(new ClassPathResource("mock-server/owncloud/list.php").getInputStream(), StandardCharsets.UTF_8))));
		httpServer.start();
	}

	private void prepareMockProjectSearch() throws IOException {
		// Status
		httpServer.stubFor(get(urlEqualTo("/status.php")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(
				IOUtils.toString(new ClassPathResource("mock-server/owncloud/status.php").getInputStream(), StandardCharsets.UTF_8))));

		// Shares json
		httpServer.stubFor(get(urlEqualTo("/ocs/v1.php/apps/files_sharing/api/v1/shares?format=json"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(IOUtils
						.toString(new ClassPathResource("mock-server/owncloud/sharing.php").getInputStream(), StandardCharsets.UTF_8))));

		// Directories json
		httpServer.stubFor(get(urlEqualTo("/index.php/apps/files/ajax/list.php?dir=/projects/Sample"))
				.willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(IOUtils
						.toString(new ClassPathResource("mock-server/owncloud/list.php").getInputStream(), StandardCharsets.UTF_8))));
		httpServer.start();
	}

	private void prepareMockAdmin() throws IOException {
		// Main entry
		httpServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody("")));

		// Status
		httpServer.stubFor(get(urlEqualTo("/status.php")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody(
				IOUtils.toString(new ClassPathResource("mock-server/owncloud/status.php").getInputStream(), StandardCharsets.UTF_8))));
		httpServer.start();
	}

	@Test
	void checkStatus() throws Exception {
		prepareMockAdmin();
		Assertions.assertTrue(resource.checkStatus(subscriptionResource.getParametersNoCheck(subscription)));
	}

	@Test
	void checkStatusAuthenticationFailed() {
		// Main entry
		httpServer.stubFor(get(urlPathEqualTo("/")).willReturn(aResponse().withStatus(HttpStatus.SC_OK).withBody("")));

		// Login failed
		httpServer.stubFor(post(urlPathEqualTo("/status.php")).willReturn(aResponse().withStatus(HttpStatus.SC_FORBIDDEN).withBody("")));
		httpServer.start();
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.checkStatus(subscriptionResource.getParametersNoCheck(subscription))), OwnCloudPluginResource.KEY + ":user", "owncloud-login");
	}

	@Test
	void checkStatusInvalidIndex() {
		httpServer.stubFor(get(urlPathEqualTo("/status.php")).willReturn(aResponse().withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)));
		httpServer.start();
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.checkStatus(subscriptionResource.getParametersNoCheck(subscription))), OwnCloudPluginResource.KEY + ":url", "owncloud-connection");
	}

	@Test
	void findAllByName() throws Exception {
		prepareMockProjectSearch();
		httpServer.start();

		final List<Directory> projects = resource.findAllByName("service:storage:owncloud:dig", "p5");
		Assertions.assertEquals(1, projects.size());
		Assertions.assertEquals(8321, projects.getFirst().getId().intValue());
		Assertions.assertEquals("P5-p0", projects.getFirst().getName());

		// Size is never computed in this mode
		Assertions.assertEquals(0, projects.getFirst().getSize());
	}

}
