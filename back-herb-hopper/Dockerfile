# Usar una imagen oficial de Node.js ligera como base
FROM node:20-alpine

# Crear y establecer el directorio de trabajo en la imagen
WORKDIR /usr/src/app

# Copiar archivos de dependencias
COPY package*.json ./

# Instalar dependencias de producción
RUN npm install --omit=dev

# Copiar el resto del código del servidor
COPY . .

# Exponer el puerto que Render asignará (el fallback local es 3000)
EXPOSE 3000

# Comando para iniciar la aplicación
CMD ["npm", "start"]
