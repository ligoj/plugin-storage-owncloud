/*
 * Service layer for plugin "storage-owncloud".
 *
 * Tool-level plugin (lives at `service:storage:owncloud`). The parent
 * `plugin-storage` delegates the subscription-row hooks to us via its
 * `subPluginIdFor` delegation. Mirrors the legacy `owncloud.js`:
 *
 *   - renderFeatures   → a link to the ownCloud files app for the
 *     configured directory (`url + '/apps/files/?dir=%2F' + directory`).
 *   - renderDetailsKey → the directory chip
 *     (`service:storage:owncloud:directory`).
 *
 * Kept free of Vue SFC imports so it can be unit-tested without a DOM.
 */
import { renderServiceLink, renderDetailsChip, useI18nStore } from '@ligoj/host'

const PARAM_URL = 'service:storage:owncloud:url'
const PARAM_DIR = 'service:storage:owncloud:directory'

/** ownCloud files-app link. Mirrors the legacy getOwnCloudLink(). */
function renderFeatures(subscription) {
  const params = subscription?.parameters
  const url = params?.[PARAM_URL]
  if (!url) return []
  const { t } = useI18nStore()
  const base = url.replace(/\/$/, '')
  const dir = params?.[PARAM_DIR]
  const href = dir ? `${base}/apps/files/?dir=%2F${encodeURIComponent(dir)}` : `${base}/apps/files/`
  return [renderServiceLink({ icon: 'mdi-folder-network-outline', href, title: t('service:storage:owncloud:directory') })]
}

/** Directory chip. Mirrors the legacy renderKey('service:storage:owncloud:directory'). */
function renderDetailsKey(subscription) {
  const dir = subscription?.parameters?.[PARAM_DIR]
  if (!dir) return null
  const { t } = useI18nStore()
  return renderDetailsChip({ icon: 'mdi-folder-outline', text: dir, title: t('service:storage:owncloud:directory') })
}

export default { renderFeatures, renderDetailsKey }
