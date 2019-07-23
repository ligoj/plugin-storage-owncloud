define(function () {
	var current = {

		/**
		 * Render ownCloud project name.
		 */
		renderKey: function (subscription) {
			return current.$super('renderKey')(subscription, 'service:storage:owncloud:directory');
		},

		/**
		 * Render Space ownCloud data.
		 */
		renderFeatures: function (subscription) {
			var result = current.$super('renderServiceLink')('home', current.getOwnCloudLink(subscription), 'service:storage:owncloud:directory', undefined, ' target="_blank"');
			// Help
			result += current.$super('renderServiceHelpLink')(subscription.parameters, 'service:storage:help');
			return result;
		},

		/**
		 * Return OwnCloud link the available data of the given subscription. When detailed data is available, the generated link will contain the target path. Otherwise it will
		 * only be a link the the home files of the instance.
		 * @param  {object} subscription Subscription data, w/o detailed data.
		 * @return {string}              The owncloud http link.
		 */
		getOwnCloudLink: function (subscription) {
			return subscription.data && subscription.data.directory && (typeof subscription.data.directory.name === 'string') && (subscription.parameters['service:storage:owncloud:url'] + '/apps/files/?dir=%2F' + subscription.data.directory.name);
		},

		/**
		 * Render ownCloud details : name and display name.
		 */
		renderDetailsKey: function (subscription) {
			return current.$super('generateCarousel')(subscription, [
				['service:storage:owncloud:directory', current.renderKey(subscription)],
				['name', subscription.data.directory.name]
			], 1);
		},

		/**
		 * Display the status of the job, including the storage state
		 */
		renderDetailsFeatures: function (subscription) {
			return '<span data-toggle="tooltip" title="' + current.$messages['service:storage:owncloud:size'] + '" class="label label-default">' + formatManager.formatSize(subscription.data.directory.size, 3) + '</span>';
		},

		/**
		 * Post treatment after the features have been injected in the DOM.
		 */
		configurerFeatures: function ($features, subscription) {
			$features.find('.feature').attr('href', current.getOwnCloudLink(subscription));
		},

		configureSubscriptionParameters: function (configuration) {
			current.$super('registerXServiceSelect2')(configuration, 'service:storage:owncloud:directory', 'service/storage/owncloud/');
		}
	};
	return current;
});
