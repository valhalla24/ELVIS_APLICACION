# ELVIS Account Manager

**Sistema de Gestión de Credenciales del Servidor ELVIS**
Universidad Santiago de Cali · Dirección de Tecnologías y Servicios de Información (DTSI) · Facultad de Ingeniería

Aplicación de escritorio que centraliza la asignación de las cuentas de acceso (licencias) del servidor ELVIS entre los estudiantes de cada curso, controla que dos clases no usen la misma cuenta a la misma hora y envía a cada estudiante sus credenciales por correo.

---

## Contenido

- [Funcionalidades](#funcionalidades)
- [Tecnologías](#tecnologías)
- [Requisitos](#requisitos)
- [Instalación y ejecución](#instalación-y-ejecución)
- [Primer uso](#primer-uso)
- [Configuración del correo](#configuración-del-correo)
- [Formato de los archivos Excel](#formato-de-los-archivos-excel)
- [Datos y respaldo](#datos-y-respaldo)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Solución de problemas](#solución-de-problemas)

---

## Funcionalidades

| Módulo | Qué hace |
|---|---|
| **Inicio** | Panel con la capacidad total, licencias asignadas, licencias libres y personas registradas. |
| **Administrar Cuentas** | Carga de licencias desde Excel, registro de cursos (día, franja horaria y licencias requeridas), carga de listas de estudiantes, asignación automática de licencias y envío de credenciales por correo. |
| **Calendario** | Ocupación de licencias por día y franja horaria, con semáforo: normal (< 70 %), moderada (70–85 %) y alta (> 85 %). |
| **Reportes** | Ocupación por curso, asignaciones por estudiante, ocupación por franja horaria y bitácora de auditoría. Vista previa y exportación a **PDF** o **Excel**. |
| **Usuarios** | Alta de usuarios del sistema, cambio de rol, restablecimiento de contraseña y eliminación. |

**Reglas que aplica la aplicación**

- Una licencia no se asigna a dos cursos cuyos horarios se crucen el mismo día, aunque pertenezcan a profesores distintos.
- Si un correo falla, la asignación queda como *Pendiente* y se puede reenviar después. El envío no bloquea la ventana.
- Todas las acciones importantes (cargas, asignaciones, envíos, cambios de usuarios, reportes) quedan en la bitácora de auditoría.

**Roles**

| Rol | Permisos |
|---|---|
| **Administrador** | Todo: cursos de todos los profesores, usuarios, licencias, reenvíos y bitácora de auditoría. |
| **Profesor** | Solo los cursos de los que es dueño: carga sus listas, asigna y envía sus correos. |

**Seguridad de los datos guardados**

- Las contraseñas de los usuarios del sistema se guardan con **PBKDF2-HMAC-SHA256** (600.000 iteraciones y sal aleatoria). Las cuentas de versiones anteriores (SHA-256) se convierten solas al iniciar sesión.
- Las contraseñas de las licencias y la clave del correo se guardan **cifradas con AES-256-GCM**. La llave está en `elvis.key`.
- Las cuentas nuevas, las claves restablecidas por el administrador y el administrador inicial (`admin123`) **deben elegir una contraseña nueva** antes de entrar.

---

## Tecnologías

| Componente | Herramienta |
|---|---|
| Lenguaje | Java 20 |
| Interfaz | JavaFX 20 (FXML + CSS) |
| Base de datos | SQLite (`sqlite-jdbc` 3.53) |
| Lectura y exportación Excel | Apache POI 5.2.3 |
| Correo | JavaMail 1.6.2 + JavaBeans Activation 1.2.0 (Microsoft 365 por SMTP con STARTTLS, con contraseña u OAuth2) |
| Construcción | Apache Ant (proyecto NetBeans) |

Todas las librerías vienen incluidas en `jar_files/`, así que no hace falta descargar nada aparte.

---

## Requisitos

- **Windows 10/11** (el JavaFX incluido en `jar_files/` es el de Windows x64).
- **JDK 20 o superior** (probado con JDK 20 y JDK 21).
- **Apache NetBeans** (recomendado). Opcional si se compila por línea de comandos.

---

## Instalación y ejecución

### Opción A: NetBeans (recomendada)

1. Clonar el repositorio:
   ```bash
   git clone https://github.com/valhalla24/ELVIS_APLICACION.git
   ```
2. En NetBeans: **File → Open Project** → seleccionar la carpeta `ElvisAccountManager`.
3. Si NetBeans marca *"Resolve Problems"* (referencias a librerías no encontradas), apuntar cada referencia a la carpeta `jar_files/` del repositorio. Las rutas del proyecto están configuradas como `C:\Users\anest\Desktop\aplicacion\jar_files`.
4. **Clean and Build** (`Shift+F11`) y luego **Run** (`F6`).

Las opciones de la máquina virtual (`--add-modules javafx.controls,javafx.fxml`) ya están en `nbproject/project.properties`.

### Opción B: línea de comandos (PowerShell)

Desde la raíz del repositorio, con el JDK en el `PATH`:

```powershell
$fx  = "jar_files\javafx-sdk-20.0.2\lib"
$cp  = (Get-ChildItem jar_files -Recurse -Filter *.jar | Where-Object { $_.FullName -notmatch 'javafx-sdk|javadoc|examples' }).FullName -join ';'
$out = "ElvisAccountManager\build\classes"

# Compilar
javac -encoding UTF-8 --release 20 --module-path $fx --add-modules javafx.controls,javafx.fxml `
      -cp $cp -d $out (Get-ChildItem ElvisAccountManager\src -Recurse -Filter *.java).FullName

# Copiar recursos (FXML, CSS)
Get-ChildItem ElvisAccountManager\src -Recurse -File | Where-Object Extension -ne '.java' | ForEach-Object {
    $dest = Join-Path $out $_.FullName.Substring((Resolve-Path ElvisAccountManager\src).Path.Length)
    New-Item -ItemType Directory -Force (Split-Path $dest) | Out-Null; Copy-Item $_.FullName $dest }

# Ejecutar
java --module-path $fx --add-modules javafx.controls,javafx.fxml -cp "$out;$cp" co.edu.usc.elvis.App
```

---

## Primer uso

1. Al abrir la aplicación por primera vez se crean la base de datos y el archivo de configuración en `C:\ElvisData\`.
2. Iniciar sesión con el administrador inicial:

   | Usuario | Contraseña |
   |---|---|
   | `admin` | `admin123` |

   Al entrar, la aplicación **obliga a elegir una contraseña nueva** (mínimo 8 caracteres, distinta de `admin123`). Si se cancela, no se entra.
3. Crear las cuentas de los profesores en **Usuarios**.
4. Configurar el correo (ver la siguiente sección).
5. En **Administrar Cuentas**:
   1. Cargar el Excel de **licencias**.
   2. Registrar los **cursos** y asignar el profesor dueño de cada uno.
   3. Cargar la **lista de estudiantes** de cada curso.
   4. **Asignar licencias** y luego **Confirmar y enviar correos pendientes**.

---

## Configuración del correo

Las credenciales se envían desde un buzón de **Microsoft 365 / Office 365** (`smtp.office365.com`, puerto 587, STARTTLS con TLS 1.2+). La cuenta **no está en el código**: se configura en `C:\ElvisData\config.properties`, que se crea con una plantilla la primera vez. Los cambios se aplican en el siguiente envío, sin cerrar la aplicación.

```properties
correo.usuario=buzon@dominio.edu.co
correo.remitente=Servidor ELVIS - USC

# clave  = usuario y contraseña del buzón
# oauth2 = aplicación registrada en Entra ID
correo.autenticacion=clave
correo.clave=<contraseña del buzón>

# Solo para oauth2
correo.oauth.tenant=
correo.oauth.cliente_id=
correo.oauth.cliente_secreto=
```

### Con usuario y contraseña

Requiere que el buzón tenga activo **SMTP autenticado** (Centro de administración de Microsoft 365 → Usuarios → *buzón* → Correo → Administrar aplicaciones de correo electrónico).

### Con OAuth2

Microsoft está retirando el envío con usuario y contraseña. Si la clave es correcta y aun así el servidor la rechaza (`535 5.7.3` / `5.7.139`), usar **OAuth2**:

1. Registrar una aplicación en **Entra ID** y crear un **secreto de cliente**.
2. Agregar el permiso de aplicación **`SMTP.SendAsApp`** (*Office 365 Exchange Online*) y otorgar consentimiento de administrador.
3. En Exchange Online PowerShell, registrar la aplicación y darle acceso al buzón:
   ```powershell
   New-ServicePrincipal -AppId <ID_APLICACION> -ObjectId <ID_OBJETO_APLICACION_EMPRESARIAL>
   Add-MailboxPermission -Identity buzon@dominio.edu.co -User <ID_OBJETO_APLICACION_EMPRESARIAL> -AccessRights FullAccess
   ```
4. Configurar:
   ```properties
   correo.autenticacion=oauth2
   correo.oauth.tenant=<ID_DIRECTORIO>
   correo.oauth.cliente_id=<ID_APLICACION>
   correo.oauth.cliente_secreto=<SECRETO>
   ```

Si el envío falla, el mensaje en pantalla indica qué corregir (por ejemplo, *"revisa que correo.clave sea la clave vigente del buzón"*).

La clave se escribe tal cual en `correo.clave` (lo mismo para `correo.oauth.cliente_secreto`). Al abrir la aplicación, o antes del siguiente envío, la línea se reemplaza sola por una versión cifrada que empieza por `enc:v1:`. Para cambiar la clave basta con volver a escribirla en claro.

> 🔒 No subas `config.properties` ni `elvis.key` al repositorio ni los compartas.

---

## Formato de los archivos Excel

Solo se aceptan archivos `.xlsx`, y se lee la primera hoja.

### Licencias

Columnas fijas: **A = Usuario**, **B = Contraseña**, **C = IP**. La fila 1 son los encabezados. Si el usuario ya existe se actualiza; si no, se crea.

| Usuario | Contraseña | IP |
|---|---|---|
| elvis01 | ******** | 10.0.0.10 |

### Lista de estudiantes

Las columnas se reconocen **por el nombre del encabezado**, en cualquier orden: *Código*, *Nombre* (o *Nombre* + *Apellidos* por separado) y *Correo*. El código estudiantil es único: si ya existe, el estudiante se actualiza.

| Código | Nombre completo | Correo |
|---|---|---|
| 2024001 | Ana Gómez | ana.gomez00@usc.edu.co |

En la raíz del repositorio hay dos archivos de ejemplo: `licencias server ELVIS_2026B prueba.xlsx` y `lista prueba.xlsx`.

---

## Datos y respaldo

| Archivo | Contenido |
|---|---|
| `C:\ElvisData\elvis.db` | Base de datos SQLite: cursos, estudiantes, licencias, asignaciones, usuarios y bitácora. |
| `C:\ElvisData\config.properties` | Configuración del correo (clave cifrada). |
| `C:\ElvisData\elvis.key` | Llave de cifrado de las contraseñas de licencias y de la clave del correo. Se crea sola la primera vez. |

- **Cambiar la carpeta:** usar `-Delvis.datos=RUTA` al ejecutar Java, o la variable de entorno `ELVIS_DATOS`.
- **Permisos:** la carpeta debe ser accesible solo para los profesores y el técnico, nunca para las cuentas de estudiantes, porque contiene las contraseñas de las licencias.
- **Respaldo:** copiar la carpeta completa con la aplicación cerrada, **incluida `elvis.key`**. Sin la llave, las contraseñas de las licencias y la clave del correo no se pueden recuperar (habría que volver a cargar el Excel de licencias y escribir la clave del correo).
- **Llave en otra carpeta:** para que una copia de la carpeta de datos no incluya la llave, se puede guardar aparte con `-Delvis.llave=RUTA` o la variable de entorno `ELVIS_LLAVE`, en una carpeta con permisos más estrictos.
- **Migración:** si existe una base de una versión anterior en `C:\Users\Public\ElvisAccountManager`, se copia sola a la carpeta nueva. La antigua no se borra.

---

## Pruebas

`pruebas/` contiene 51 casos que se ejecutan sobre las pantallas, servicios y DAO reales, con una base temporal (no toca `C:\ElvisData`):

| Grupo | Casos | Qué comprueba |
|---|---|---|
| Calendario (CAL) | 9 | Ocupación por franja, niveles normal / moderada / alta, fin de horario exclusivo, alerta y pico. |
| Reportes (REP) | 9 | Los cuatro reportes, alcance por rol, rango de fechas, exportación a PDF y Excel sin contraseñas. |
| Restricción por rol (ROL) | 9 | Menú y secciones visibles, cursos y asignaciones de cada profesor, acciones bloqueadas aunque se invoquen directamente. |
| Seguridad (SEG) | 24 | Cifrado de licencias y del correo, migración de datos antiguos, PBKDF2, cambio obligatorio de clave e inicio de sesión. |

```powershell
powershell -ExecutionPolicy Bypass -File pruebas\correr_pruebas.ps1
```

Requiere un JDK 20+ en el `PATH` o en `JAVA_HOME`. Abre por un momento la ventana de inicio de sesión.

---

## Estructura del proyecto

```
ELVIS_APLICACION/
├── ElvisAccountManager/            Proyecto NetBeans
│   ├── build.xml, nbproject/       Construcción con Ant
│   └── src/co/edu/usc/elvis/
│       ├── App.java                Punto de entrada
│       ├── controller/             Lógica de cada pantalla
│       ├── dao/                    Acceso a SQLite
│       ├── model/                  Entidades
│       ├── util/                   Correo, importadores Excel, exportadores PDF/Excel, seguridad
│       └── view/                   Pantallas FXML y estilos.css
└── jar_files/                      Librerías (JavaFX, POI, SQLite, JavaMail, Activation)
```

El proyecto sigue el patrón **MVC**: las vistas FXML, los controladores que atienden las acciones del usuario y los DAO que leen y escriben en la base.

---

## Solución de problemas

| Síntoma | Causa y solución |
|---|---|
| `NoClassDefFoundError: javax/activation/DataHandler` al enviar | Falta `jar_files/javax.activation-1.2.0.jar` en el classpath. Actualizar el repositorio y hacer *Clean and Build*. |
| `535 5.7.3 Authentication unsuccessful` | Clave incorrecta o vencida (revisar primero `correo.clave`), SMTP autenticado apagado en el buzón o autenticación básica bloqueada. En este último caso, pasar a `oauth2`. |
| `5.7.139` / *basic authentication is disabled* | El tenant bloquea usuario y contraseña: pasar a `oauth2`. |
| *"No se pudo descifrar un dato guardado"* | `elvis.key` no corresponde a la base (se reemplazó o se perdió). Restaurar la llave del respaldo; si no existe, volver a cargar las licencias y escribir la clave del correo. |
| *"El correo todavía no está configurado"* | Completar `correo.usuario` y `correo.clave` (o los datos `correo.oauth.*`) en `config.properties`. |
| *"No hay licencias libres en ese horario"* | Todas las licencias están ocupadas por otros cursos en esa franja. Cargar más licencias o mover el horario. |
| Git no puede actualizar un `.jar` | La aplicación está abierta y tiene el archivo en uso. Cerrarla antes de `git pull`. |

---

<sub>Trabajo de grado · Universidad Santiago de Cali · 2026</sub>
