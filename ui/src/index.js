/*
 * Plugin "storage-owncloud" — ownCloud implementation of plugin-storage.
 *
 * Tool-level plugin (`service:storage:owncloud`). Augments the parent
 * `plugin-storage` via i18n parameter labels + row features (home link +
 * resource chip) merged in through plugin-storage's `subPluginIdFor`
 * delegation hook.
 *
 * Authored as source — compiled to `/main/storage-owncloud/vue/index.js` by Vite.
 */
import { useI18nStore } from '@ligoj/host'
import enMessages from './i18n/en.js'
import frMessages from './i18n/fr.js'
import service from './service.js'

const features = {
  renderFeatures: service.renderFeatures,
  renderDetailsKey: service.renderDetailsKey,
}

export default {
  id: 'storage-owncloud',
  label: 'ownCloud',
  requires: ['storage'],
  install() {
    const i18n = useI18nStore()
    i18n.merge(enMessages, 'en')
    i18n.merge(frMessages, 'fr')
  },
  feature(action, ...args) {
    const fn = features[action]
    if (!fn) throw new Error(`Plugin "storage-owncloud" has no feature "${action}"`)
    return fn(...args)
  },
  service,
  meta: { icon: 'mdi-folder-network-outline', color: 'blue-darken-2' },
}

export { service }
