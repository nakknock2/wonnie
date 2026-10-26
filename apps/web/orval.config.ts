import { defineConfig } from 'orval'

export default defineConfig({
  woonie: {
    input: 'http://localhost:8080/v3/api-docs',
    output: {
      target: './src/shared/api/generated/woonie.ts',
      schemas: './src/shared/api/generated/model',
      client: 'react-query',
      httpClient: 'fetch',
      clean: true,
    },
  },
})
