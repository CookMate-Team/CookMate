import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
	plugins: [react(), tailwindcss()],
	server: {
		proxy: {
			'/api': {
				target: process.env.GATEWAY_URL || 'http://localhost:8085',
				changeOrigin: true,
			},
			'/realms': {
				target: process.env.VITE_KEYCLOAK_URL || 'http://localhost:8080',
				changeOrigin: true,
			},
			'/resources': {
				target: process.env.VITE_KEYCLOAK_URL || 'http://localhost:8080',
				changeOrigin: true,
			},
		},
	},
	preview: {
		proxy: {
			'/api': {
				target: process.env.GATEWAY_URL || 'http://localhost:8085',
				changeOrigin: true,
			},
		},
	},
})
