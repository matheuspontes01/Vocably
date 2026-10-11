import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Em desenvolvimento, o Vite repassa /api para o Spring Boot. Assim o navegador acha que
    // front e back estão na mesma origem e não precisamos configurar CORS.
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
});
