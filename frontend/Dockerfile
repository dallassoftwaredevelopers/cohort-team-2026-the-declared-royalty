FROM node:24-alpine AS base
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# --- Dev: Vite dev server with hot reload (source is bind-mounted by compose) ---
FROM base AS dev
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]

# --- Build: compile the production bundle ---
FROM base AS build
COPY . .
RUN npm run build

# --- Prod: serve the static bundle with nginx ---
FROM nginx:1.29-alpine AS prod
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
