import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
	plugins: [react(), tailwindcss()],
	server: {
		proxy: {
			'/api/simulator': {
				target: process.env.SIMULATOR_URL || 'http://localhost:8082',
				changeOrigin: true,
			},
			'/api/cooking-sessions': {
				target: process.env.COOKING_SESSION_URL || 'http://localhost:8083',
				changeOrigin: true,
			},
			'/api': {
				target: process.env.API_URL || 'http://localhost:8081',
				changeOrigin: true,
			},
		},
	},
	preview: {
		proxy: {
			'/api/simulator': {
				target: process.env.SIMULATOR_URL || 'http://localhost:8082',
				changeOrigin: true,
			},
			'/api/cooking-sessions': {
				target: process.env.COOKING_SESSION_URL || 'http://localhost:8083',
				changeOrigin: true,
			},
			'/api': {
				target: process.env.API_URL || 'http://localhost:8081',
				changeOrigin: true,
			},
		},
	},
})
