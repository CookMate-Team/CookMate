import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8085';

// https://vite.dev/config/
export default defineConfig({
	plugins: [react(), tailwindcss()],
	server: {
		proxy: {
			'/api/simulator': {
				target: GATEWAY_URL,
				changeOrigin: true,
			},
			'/api/cooking-sessions': {
				target: GATEWAY_URL,
				changeOrigin: true,
			},
			'/api': {
				target: GATEWAY_URL,
				changeOrigin: true,
			},
			'/oauth2': {
				target: GATEWAY_URL,
				changeOrigin: false,
			},
			'/login': {
				target: GATEWAY_URL,
				changeOrigin: false,
			},
			'/logout': {
				target: GATEWAY_URL,
				changeOrigin: false,
			},
		},
	},
	preview: {
		proxy: {
			'/api/simulator': {
				target: GATEWAY_URL,
				changeOrigin: true,
			},
			'/api/cooking-sessions': {
				target: GATEWAY_URL,
				changeOrigin: true,
			},
			'/api': {
				target: GATEWAY_URL,
				changeOrigin: true,
			},
			'/oauth2': {
				target: GATEWAY_URL,
				changeOrigin: false,
			},
			'/login': {
				target: GATEWAY_URL,
				changeOrigin: false,
			},
			'/logout': {
				target: GATEWAY_URL,
				changeOrigin: false,
			},
		},
	},
})
