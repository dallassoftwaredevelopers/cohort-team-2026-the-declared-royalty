FROM node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --include=optional --platform=linux --arch=x64
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host"]