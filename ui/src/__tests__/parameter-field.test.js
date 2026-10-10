/*
 * The subscribe wizard's input of `service:storage:owncloud:directory`: the directories of the
 * selected instance, searched as the user types (the host LjRemoteSearchField).
 */
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount, flushPromises } from '@vue/test-utils'
import def from '../index.js'

const PARAMETER = { id: 'service:storage:owncloud:directory', type: 'INTEGER', mandatory: true }

function jsonResponse(body) {
  return { ok: true, status: 200, headers: { get: () => 'application/json' }, json: async () => body }
}
// Expose the bindings of the field; the test types through `update:search`
const inputStub = (name) => ({ name, props: ['items'], emits: ['update:search', 'update:modelValue'], template: '<input />' })
const stubs = { LigojAutocomplete: inputStub('LigojAutocomplete'), LigojCombobox: inputStub('LigojCombobox') }

beforeEach(() => {
  // The search waits for a pause in the typing
  vi.useFakeTimers({ toFake: ['setTimeout', 'clearTimeout'] })
  setActivePinia(createPinia())
  def.install()
  globalThis.fetch = vi.fn(async () => jsonResponse([{ id: 42, name: 'docs' }]))
})
afterEach(() => { vi.useRealTimers() })

describe('service:storage:owncloud:directory parameter field', () => {
  it('is a remote search to subscribe, the default input to edit a node or for another parameter', () => {
    const field = def.feature('parameterField', { parameter: PARAMETER, isNode: false })
    expect(field).toBeTruthy()
    // The same component on each render, else the wizard would remount the input
    expect(def.feature('parameterField', { parameter: PARAMETER, isNode: false })).toBe(field)
    expect(def.feature('parameterField', { parameter: PARAMETER, isNode: true })).toBeNull()
    expect(def.feature('parameterField', { parameter: { id: 'service:storage:owncloud:url' }, isNode: false })).toBeNull()
  })

  it('searches the directories and stores the directory identifier', async () => {
    const Field = def.feature('parameterField', { parameter: PARAMETER, isNode: false })
    const w = mount(Field, { props: { modelValue: '', parameter: PARAMETER, formValues: {}, mode: 'link', isNode: false, instanceNodeId: 'service:storage:owncloud:local' }, global: { stubs } })
    const input = w.findComponent({ name: 'LigojAutocomplete' })
    input.vm.$emit('update:search', 'docs')
    vi.advanceTimersByTime(300)
    await flushPromises()
    expect(fetch.mock.calls[0][0]).toBe('rest/service/storage/owncloud/service%3Astorage%3Aowncloud%3Alocal/docs')
    expect(input.props('items')).toEqual([{ title: 'docs', subtitle: '42', value: 42 }])
  })
})
