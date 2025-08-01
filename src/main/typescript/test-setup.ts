import { vi } from 'vitest'

// Mock IntersectionObserver
global.IntersectionObserver = vi.fn(() => ({
  disconnect: vi.fn(),
  observe: vi.fn(),
  unobserve: vi.fn(),
}))

// Mock ResizeObserver
global.ResizeObserver = vi.fn(() => ({
  disconnect: vi.fn(),
  observe: vi.fn(),
  unobserve: vi.fn(),
}))

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(), // deprecated
    removeListener: vi.fn(), // deprecated
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

// Mock scrollTo
global.scrollTo = vi.fn()

// Mock window.location
Object.defineProperty(window, 'location', {
  value: {
    href: 'http://localhost:3000',
    origin: 'http://localhost:3000',
    pathname: '/',
    search: '',
    hash: '',
  },
  writable: true,
})

// Mock import.meta.env for reka-ui
Object.defineProperty(globalThis, 'import', {
  value: {
    meta: {
      env: {
        MODE: 'test',
        DEV: false,
        PROD: false,
        SSR: false
      }
    }
  },
  writable: true,
})

// Additional mock for import.meta
if (typeof globalThis.import === 'undefined') {
  globalThis.import = {
    meta: {
      env: {
        MODE: 'test',
        DEV: false,
        PROD: false,
        SSR: false
      }
    }
  }
}

// Mock reka-ui's problematic modules
vi.mock('reka-ui', async (importOriginal) => {
  const actual = await importOriginal() as any
  return {
    ...actual,
    useHideOthers: () => ({}),
    // Mock other problematic functions
    useBodyScrollLock: () => ({}),
    useEscapeKeydown: () => ({}),
    useFocusGuards: () => ({}),
    useForwardExpose: () => ({}),
    useForwardProps: () => ({}),
    useForwardPropsEmits: () => ({}),
    useId: () => 'test-id',
    useStateMachine: () => ({
      state: { value: 'closed' },
      send: vi.fn()
    }),
    // Mock Button component to avoid $listeners issues
    Button: {
      name: 'Button',
      template: '<button><slot /></button>',
      props: ['variant', 'size', 'disabled', 'type'],
      emits: ['click']
    },
    // Mock Input component
    Input: {
      name: 'Input',
      template: '<input />',
      props: ['modelValue', 'type', 'placeholder', 'disabled'],
      emits: ['update:modelValue', 'input', 'change']
    }
  }
})

// Mock focus-trap
vi.mock('focus-trap', () => ({
  createFocusTrap: () => ({
    activate: vi.fn(),
    deactivate: vi.fn(),
    pause: vi.fn(),
    unpause: vi.fn()
  })
}))
