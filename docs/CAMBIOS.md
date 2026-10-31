# Correcciones de cumplimiento — HerbHopper v4.2.0

Rama: `fix/cumplimiento-manuales`. Este documento resume qué se cambió para que el código cumpla lo que dicen el Manual de Usuario y el Manual Técnico, y qué hay que hacer antes de desplegar.

## ⚠️ Antes de subir y desplegar

1. **Rotar credenciales expuestas.** El historial de Git contiene la contraseña de PostgreSQL (`db.js`) y el `api_secret` de Cloudinary (`cloudinary.js`). Aunque ya no están en el código, siguen en commits anteriores del repositorio público: cámbialas en Render y en Cloudinary.
2. **Configurar variables de entorno en Render** (ver `back-herb-hopper/.env.example`). Sin `DATABASE_URL` y `JWT_SECRET` el servidor no arranca:
   `DATABASE_URL`, `JWT_SECRET`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `EPAYCO_P_CUST_ID_CLIENTE`, `EPAYCO_P_KEY`.
3. **Datos de la base.** Al arrancar, el servidor agrega las columnas y tablas nuevas sin borrar nada, pero los productos existentes quedan con `stock = 0` y sin ficha técnica. Para cargar el catálogo completo (stock, THC, beneficios, etc.) ejecuta una vez `DEMO_PASSWORD='TuClave#2026' npm run migrate` (**borra y recrea todas las tablas**) o completa los productos desde la app del vendedor.
4. **ePayco:** en el panel de ePayco, la URL de confirmación debe ser `https://herb-hopper-v2.onrender.com/api/payment/confirmation`.
5. **Cuentas demo** (después de `migrate`): `paciente.herbhopper@gmail.com`, `vendedor.herbhopper@gmail.com`, `admin.herbhopper@gmail.com`, con la contraseña de `DEMO_PASSWORD`.

## Seguridad

| Problema | Corrección |
|---|---|
| Acceso oculto `admin` / `admin123` | Eliminado. Todo acceso pasa por el backend. |
| Backend sin autenticación ni roles | JWT (1 h) + tokens de renovación revocables; `authenticate` y `requireRole` en cada ruta protegida. |
| Login comparaba contraseñas en texto plano | Solo bcrypt; las contraseñas antiguas se convierten a hash al arrancar. |
| Contraseña guardada en texto plano para la huella | La huella usa un token de dispositivo cifrado con AES-256-GCM en el Android Keystore (clave ligada a la biometría). La app pregunta antes de activarla. |
| Llave privada de ePayco en el APK y log `BODY` con número de tarjeta y CVV | Eliminados. Todos los pagos van por el checkout oficial de ePayco; la confirmación se valida en el servidor con la firma SHA-256. |
| Secretos en el código | Variables de entorno (`config.js`). |
| `usesCleartextTraffic="true"` / `allowBackup="true"` | Ambos en `false`. |
| Sin límite de intentos | Limitador en login, registro y renovación. |

## Funciones que el manual describía y ahora sí existen

- **Registro:** correo con dominio exacto (Gmail, Outlook, Hotmail, Yahoo, iCloud), política de contraseña, celular colombiano y autorización de datos (Ley 1581).
- **Android 9+:** `minSdk = 28`.
- **Detalle de producto:** concentración, % THC, beneficios, modo de uso y certificaciones reales por producto; stock disponible.
- **Fórmulas médicas:** cámara con flash, galería con imagen **o PDF**, nombre y registro del médico, subida a almacenamiento privado, revisión (aprobar/rechazar) por el vendedor, "Mis fórmulas". Los productos con THC ≥ 1 % exigen fórmula.
- **Pedidos:** se registran en el backend con el usuario real, la dirección y coordenadas GPS; el servidor recalcula precios, valida stock y reserva unidades. El vendedor ve los pedidos de todos los pacientes desde cualquier teléfono.
- **Pagos:** tarjeta, PSE y efectivo por ePayco (solo el medio elegido); webhook de confirmación; pago rechazado o abandonado anula el pedido y devuelve el stock.
- **Rastreo:** accesible desde Mis Pedidos; línea de tiempo real, estado de pago y fórmula, y mapa con las coordenadas de entrega.
- **Dashboard del vendedor:** ventas del día (pagos aprobados) y pedidos nuevos reales; alertas CRÍTICO por stock mínimo; botón **Reordenar** funcional.
- **Inventario:** CRUD completo contra el backend (crear, editar, eliminar) con stock, stock mínimo y ficha técnica.
- **Administrador:** rol ADMIN asignable, inactivar/reactivar cuentas, sucursales en la base central, integridad de la BD calculada, bitácora real (accesos, tokens, ePayco…), "Reiniciar Servicios" (recicla el pool de conexiones), solicitudes de reabastecimiento, analítica real y mesa de ayuda para responder el chat.
- **Chat de soporte:** los mensajes se guardan en el backend y el administrador responde desde la mesa de ayuda.
- **Perfil:** editar datos en el servidor, cambiar contraseña, eliminar cuenta; ya no se piden datos de tarjeta.

## Documentos corregidos (`docs/`)

- `Manual_Usuario_HerbHopper_v4.2.0.docx / .pdf`: describe la app tal como funciona (solo Android, sin iOS/web ni mapa GPS en vivo, sin afirmaciones no verificables).
- `Manual_Tecnico_HerbHopper.docx / .pdf`: estructura completa de la actividad (portada, tabla de contenido automática, introducción, enfoque con justificación del nicho, normatividad con la Ley 527 de 1999, RF por módulo y RNF con la columna "Observaciones para el desarrollador", arquitectura modular, E-R, secuencia de pago y mockups por rol).
- `mockups/`: imágenes de las pantallas y diagramas.

## Verificación hecha

- `gradlew :app:assembleDebug` → **BUILD SUCCESSFUL** (sin errores de compilación).
- Backend: revisión de sintaxis de todos los archivos y `npm test` (reglas de validación) en verde.
- No se probó contra una base de datos real ni con ePayco en vivo: pruébalo en Render tras configurar las variables.
