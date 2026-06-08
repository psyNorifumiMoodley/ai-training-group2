/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        shell:         '#161b22',
        sidebar:       '#21262d',
        'border-dark': '#30363d',
        primary: {
          DEFAULT: '#2563eb',
          hover:   '#3b82f6',
          pressed: '#1d4ed8',
          light:   '#eff6ff',
          fill:    '#dbeafe',
          text:    '#1d4ed8',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'monospace'],
      },
      fontSize: {
        'h1':       ['22px', { lineHeight: '1.3',  fontWeight: '500' }],
        'h2':       ['18px', { lineHeight: '1.35', fontWeight: '500' }],
        'h3':       ['16px', { lineHeight: '1.4',  fontWeight: '500' }],
        'body':     ['14px', { lineHeight: '1.65' }],
        'caption':  ['13px', { lineHeight: '1.5'  }],
        'label':    ['12px', { lineHeight: '1.4',  fontWeight: '500' }],
        'overline': ['11px', { lineHeight: '1.4',  fontWeight: '500', letterSpacing: '0.07em' }],
      },
      borderRadius: {
        'badge':  '4px',
        'input':  '8px',
        'card':   '10px',
        'modal':  '12px',
        'avatar': '9999px',
      },
      spacing: {
        '1':  '4px',
        '2':  '8px',
        '3':  '12px',
        '4':  '16px',
        '6':  '24px',
        '8':  '32px',
        '12': '48px',
      },
      boxShadow: {
        'focus': '0 0 0 3px rgba(37, 99, 235, 0.3)',
      },
    },
  },
  plugins: [],
};
