# Prettier Java — Plugin para IntelliJ IDEA

Plugin de IntelliJ IDEA que formatea archivos Java usando **Prettier** con `prettier-plugin-java`, replicando el comportamiento del [Prettier Java Plugin de VS Code (RudraPatel)](https://marketplace.visualstudio.com/items?itemName=RudraPatel.prettier-plugin-java).

---

## ✨ Características

- **`Ctrl+Alt+L`** — Formatea el archivo Java activo (integrado con "Reformat Code")
- **`Ctrl+S`** — Format on Save (activable en Settings). Guarda inmediatamente y formatea en segundo plano
- **Detección automática de `.prettierrc`** — Si tienes un archivo de configuración en tu proyecto, el plugin lo usa automáticamente
- **Panel de configuración** en Settings → Tools → Prettier Java
- **Zero-latency (Javet)** — No requiere instalar Node.js, `prettier` ni `prettier-plugin-java` globalmente. Todo viene integrado mediante un motor V8 embebido.

---

## 🔧 Requisitos

| Dependencia                          | Versión mínima |
| ------------------------------------ | -------------- |
| IntelliJ IDEA (Community o Ultimate) | 2024.1+        |

> ⚠️ **No se necesita Node.js**. El plugin utiliza la librería Javet (V8 engine) incrustada en el IDE para inyectar y ejecutar Prettier de forma nativa.

---

## 📦 Instalación

1. Descarga o compila el archivo `.zip` (ver sección Build)
2. Abre IntelliJ IDEA
3. Ve a **Settings → Plugins → ⚙️ → Install Plugin from Disk...**
4. Selecciona el archivo `prettier-java-1.0.2.zip`
5. Reinicia IntelliJ cuando lo solicite

---

## ⚙️ Configuración

Ve a **Settings → Tools → Prettier Java**. Tienes las siguientes opciones generales:

- **Enable Prettier Java formatter**: Activa/desactiva el plugin.
- **Format on Save**: Ejecuta Prettier cada vez que guardas un archivo Java.

### Perfiles de Formateo

El plugin incorpora perfiles preconfigurados para inyectar automáticamente configuraciones a Prettier. Elige el perfil que más se adapte al estándar de tu proyecto:

1. **Enterprise/Spring** (Por defecto)
   - _Reglas:_ `printWidth: 120`, `tabWidth: 4`, `useTabs: true`, `semi: true`, `singleQuote: false`.
2. **Google Style**
   - _Reglas:_ `printWidth: 100`, `tabWidth: 2`, `useTabs: false`, `semi: true`, `singleQuote: false`.
3. **Custom**
   - _Reglas:_ El plugin no inyecta ninguna regla estricta interna, sino que permite configurar dinámicamente **Custom Print Width** y **Custom Tab Width** desde este mismo panel.
   - ⚡ **Auto-generación Reactiva:** Si seleccionas "Custom", cambias los números numéricos y das clic en _Aplicar_, el plugin escaneará instantáneamente la raíz de tu proyecto. Si el archivo `.prettierrc` no existe, lo creará automáticamente. Si ya existe, sobreescribirá en tiempo real sus claves `printWidth` y `tabWidth` con tus nuevos valores elegidos.

### `.prettierrc` en el proyecto

Si tu proyecto tiene un archivo `.prettierrc` (o `.prettierrc.json`, `.prettierrc.yml`, etc.) en la **raíz del mismo**, el plugin lo detectará y usará automáticamente.

> [!IMPORTANT]
> **Prioridad de Configuración:** Los valores definidos en tu archivo `.prettierrc` **siempre tendrán prioridad** y sobrescribirán cualquier configuración visual que hayas establecido en _Settings → Tools → Prettier Java_.

### Opciones de `.prettierrc` para Java

A diferencia de los formateadores clásicos de Java, **Prettier** tiene un enfoque dictatorial y fuertemente predeterminado (_opinionated_), por lo que deliberadamente expone muy pocas configuraciones estéticas. Las opciones soportadas en tu `.prettierrc` son:

| Propiedad       | Tipo      | Default  | Descripción                                                                                                                                                                            |
| :-------------- | :-------- | :------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `printWidth`    | `int`     | `80`     | Límite máximo de caracteres por línea. Al excederlo, el formateador fragmentará parámetros o métodos encadenados en múltiples líneas. Para proyectos Java se recomienda `100` o `120`. |
| `tabWidth`      | `int`     | `4`      | Número de espacios para cada nivel de sangría / indentación.                                                                                                                           |
| `useTabs`       | `boolean` | `false`  | Establece la indentación horizontal mediante el uso de tabuladores (`\t`) en lugar de espacios.                                                                                        |
| `trailingComma` | `string`  | `"none"` | Define la impresión de comas finales. Valores permitidos: `"none"` (sin comas) o `"all"` (añadir comas donde sea válido, ej: fin de Arrays o inicialización Enum).                     |
| `endOfLine`     | `string`  | `"lf"`   | Forzar el tipo de salto de línea (`"lf"`, `"crlf"`, `"cr"`, `"auto"`).                                                                                                                 |

_(Importante: En los archivos `.java` opciones genéricas de Prettier de web como `singleQuote`, `semi` o `bracketSpacing` carecen de impacto visual ya que el compilador Java obliga semánticamente al uso de `"` y `;`)._

#### Retener parámetros en una sola línea

Si deseas que un método con 2 o 3 parámetros se mantenga escrito en la misma línea visualmente, debes aumentar tu **`printWidth`**. Si la firma de tu método (anotaciones + alcance + retorno + nombre + parámetros) es más ancha que este valor configurado, forzará el salto. Este es el **único mecanismo** por el cual Prettier toma esta decisión.

Ejemplo recomendado de `.prettierrc`:

```json
{
  "printWidth": 120,
  "tabWidth": 4,
  "useTabs": false,
  "trailingComma": "none"
}
```

---

## 🏗️ Build (compilar el plugin)

### Prerequisitos de build

- JDK 17+ instalado
- Node.js con npm disponible en PATH

### Pasos

```powershell
# 1. Ve al directorio del proyecto
cd C:\ruta\al\proyecto\FORMATER_V2

# 2. Compila el plugin (incluye npm install automáticamente)
.\gradlew.bat buildPlugin
```

El archivo `.zip` se genera en:

```
build\distributions\prettier-java-1.0.2.zip
```

### Actualizar versión de Prettier

Para actualizar `prettier` o `prettier-plugin-java`:

1. Edita `src/main/resources/prettier-node/package.json`
2. Cambia las versiones de las dependencias
3. Vuelve a ejecutar `.\gradlew.bat buildPlugin`

El build ejecuta `npm install` automáticamente y empaqueta los módulos en el `.zip`.

---

## 🏛️ Arquitectura

```
FORMATER_V2/
├── build.gradle.kts                          ← Build: Gradle + npm install + zip node_modules
├── src/main/
│   ├── kotlin/com/prettierjavaplugin/
│   │   ├── PrettierJavaFormattingService.kt  ← Ctrl+Alt+L: AsyncDocumentFormattingService
│   │   ├── PrettierJavaFormatAndSaveAction.kt ← Ctrl+S: override de SaveAll
│   │   ├── PrettierJavaSettings.kt           ← Configuración persistente
│   │   ├── PrettierJavaSettingsConfigurable.kt ← Integración con Settings de IntelliJ
│   │   └── PrettierJavaSettingsPanel.kt      ← UI Swing del panel
│   └── resources/
│       ├── META-INF/plugin.xml               ← Descriptor del plugin
│       └── prettier-node/
│           ├── format.js                     ← Script Node.js que llama a Prettier
│           └── package.json                  ← Dependencias: prettier + prettier-plugin-java
```

### Cómo funciona internamente

1. Al inicializarse, el plugin extrae `format.js` y el bundle de dependencias en un directorio temporal.
2. Usa la librería **Javet** para inicializar un motor nativo V8 dentro de un Thread seguro del IDE.
3. Se invoca directamente la función asíncrona de Prettier pasándole los parámetros según el _Perfil_ seleccionado o para que lea un `.prettierrc`.
4. El resultado se devuelve en memoria mediante un contenedor `Promise` asíncrono, logrando un formateo sin bloqueos de UI ni requerir un subproceso de OS como Node.js.

### Format on Save (Ctrl+S)

- Se sobrescribe la acción `SaveAll` de IntelliJ con `overrides="true"`
- **Patrón Save-First**: guarda instantáneamente el archivo original, luego formatea en background thread (~2 segundos), y guarda automáticamente la versión formateada
- Sin bloqueo del hilo de UI (EDT)

---

## 🔍 Troubleshooting

| Problema                 | Solución                                                                                       |
| ------------------------ | ---------------------------------------------------------------------------------------------- |
| Primera vez lenta        | Normal — extrae node_modules del JAR al directorio temporal. Las siguientes veces es inmediato |
| Plugin incompatible      | Verifica que tu versión de IntelliJ esté entre 2024.1 y 2025.3                                 |
| Format on Save no activa | Verifica que el checkbox esté ✅ en Settings → Tools → Prettier Java                           |

---

## 📄 Licencia

MIT
