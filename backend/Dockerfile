FROM node:20-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Drop devDependencies after build to keep image smaller.
RUN npm prune --omit=dev

ENV NODE_ENV=production
ENV HOST=0.0.0.0

EXPOSE 3000

# Run node directly (not npm) so Render signals/health checks work correctly.
CMD ["node", "dist/main.js"]
