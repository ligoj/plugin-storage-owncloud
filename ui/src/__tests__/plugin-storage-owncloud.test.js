/*
 * Contract tests for plugin-storage-owncloud, incl. the parent → child delegation:
 * when storage-owncloud is registered, plugin-storage's renderFeatures/renderDetailsKey
 * resolve to this tool for a matching node.
 */
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { pluginRegistry, useI18nStore } from '@ligoj/host'
import def from '../index.js'
import parentDef from '../../../../plugin-storage/ui/src/index.js'

beforeEach(() => { setActivePinia(createPinia()) })

describe('plugin-storage-owncloud manifest', () => {
  it('exposes a valid tool-level manifest', () => {
    expect(def.id).toBe('storage-owncloud')
    expect(def.requires).toEqual(['storage'])
    expect(def.routes).toBeUndefined()
    expect(typeof def.install).toBe('function')
    expect(typeof def.feature).toBe('function')
    expect(def.service).toBeTypeOf('object')
    expect(def.meta).toMatchObject({ icon: expect.any(String), color: expect.any(String) })
  })

  it('merges i18n on install', () => {
    const i18n = useI18nStore()
    def.install()
    expect(i18n.t('service:storage:owncloud:directory')).toBeTypeOf('string')
    expect(i18n.t('service:storage:owncloud:directory')).not.toBe('service:storage:owncloud:directory')
  })

  it('throws for an unknown feature', () => {
    expect(() => def.feature('nope')).toThrow(/no feature "nope"/)
  })

  it('renderFeatures returns a home-link button when params are set', () => {
    def.install()
    const vnodes = def.feature('renderFeatures', { parameters: {"service:storage:owncloud:url":"https://oc.example.org","service:storage:owncloud:directory":"docs"} })
    expect(vnodes).toHaveLength(1)
    expect(vnodes[0].__v_isVNode).toBe(true)
    expect(vnodes[0].props.target).toBe('_blank')
  })

  it('renderFeatures returns [] without the required params', () => {
    def.install()
    expect(def.feature('renderFeatures', { parameters: {} })).toEqual([])
    expect(def.feature('renderFeatures', {})).toEqual([])
  })

  it('renderDetailsKey returns the resource chip when present', () => {
    def.install()
    expect(def.feature('renderDetailsKey', { parameters: { 'service:storage:owncloud:directory': 'x' } })).toBeTruthy()
    expect(def.feature('renderDetailsKey', { parameters: {} })).toBeNull()
  })
})

describe('plugin-storage → plugin-storage-owncloud delegation', () => {
  beforeEach(() => {
    parentDef.install({ router: { addRoute() {} } })
    def.install()
    pluginRegistry.register('storage-owncloud', def)
  })
  afterEach(() => { pluginRegistry.remove('storage-owncloud') })

  it('parent renderDetailsKey resolves to this tool for a matching node', () => {
    const out = parentDef.feature('renderDetailsKey', {
      node: { id: 'service:storage:owncloud:1' },
      parameters: { 'service:storage:owncloud:directory': 'x' },
    })
    expect(Array.isArray(out)).toBe(true)
    expect(out.length).toBe(1)
    expect(out[0].__v_isVNode).toBe(true)
  })

  it('does not delegate for a different tool', () => {
    const out = parentDef.feature('renderDetailsKey', {
      node: { id: 'service:storage:other:1' },
      parameters: {},
    })
    expect(out).toBeNull()
  })
})
