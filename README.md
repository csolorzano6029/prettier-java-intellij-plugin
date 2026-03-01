# Prettier Java — Plugin para IntelliJ IDEA

Plugin de IntelliJ IDEA que formatea archivos Java usando **Prettier** con `prettier-plugin-java`, replicando el comportamiento del [Prettier Java Plugin de VS Code (RudraPatel)](https://marketplace.visualstudio.com/items?itemName=RudraPatel.prettier-plugin-java).

---

## ✨ Características

- **`Ctrl+Alt+L`** — Formatea el archivo Java activo (integrado con "Reformat Code")
- **`Ctrl+S`** — Format on Save (activable en Settings). Guarda inmediatamente y formatea en segundo plano
- **Detección automática de `.prettierrc`** — Si tienes un archivo de configuración en tu proyecto, el plugin lo usa automáticamente
- **Panel de configuración** en Settings → Tools → Prettier Java
- **Node modules bundleados** — No requiere instalar `prettier` ni `prettier-plugin-java` globalmente

---

## 🔧 Requisitos

| Dependencia                          | Versión mínima                           |
| ------------------------------------ | ---------------------------------------- |
| IntelliJ IDEA (Community o Ultimate) | 2024.1+                                  |
| Node.js                              | v18+ (debe estar en el PATH del sistema) |

> ⚠️ **No se necesita** instalar `prettier` ni `prettier-plugin-java` globalmente. Van incluidos dentro del plugin.

---

## 📦 Instalación

1. Descarga o compila el archivo `.zip` (ver sección Build)
2. Abre IntelliJ IDEA
3. Ve a **Settings → Plugins → ⚙️ → Install Plugin from Disk...**
4. Selecciona el archivo `prettier-java-1.0.0.zip`
5. Reinicia IntelliJ cuando lo solicite

---

## ⚙️ Configuración

Ve a **Settings → Tools → Prettier Java**:

| Opción         | Default | Descripción                                      |
| -------------- | ------- | ------------------------------------------------ |
| Enable         | ✅ ON   | Activa/desactiva el plugin                       |
| Format on Save | ❌ OFF  | Formatea automáticamente al guardar con `Ctrl+S` |
| Node.js path   | `node`  | Ruta al ejecutable de Node.js                    |
| Print Width    | 80      | Máximo de caracteres por línea                   |
| Tab Width      | 2       | Espacios por nivel de indentación                |
| Use Tabs       | OFF     | Tabs vs espacios                                 |
| Semicolons     | ON      | Punto y coma al final de sentencias              |
| Trailing Comma | `all`   | Comas finales en listas y parámetros             |
| Single Quotes  | OFF     | Comillas simples vs dobles                       |

### `.prettierrc` en el proyecto

Si tu proyecto tiene un archivo `.prettierrc` (o `.prettierrc.json`, `prettier.config.js`, etc.), el plugin lo detecta automáticamente. Las opciones configuradas en IntelliJ tienen prioridad sobre el archivo de configuración.

Ejemplo de `.prettierrc` mínimo:

```json
{
  "trailingComma": "all",
  "printWidth": 100,
  "tabWidth": 2
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
build\distributions\prettier-java-1.0.0.zip
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

1. Al activarse, el plugin extrae `format.js` y `node_modules.zip` del JAR a un directorio temporal (`%TEMP%/prettier-java-intellij/`)
2. Cuando se formatea, ejecuta `node format.js` pasando el código Java por stdin
3. El script `format.js` llama a `prettier.resolveConfig(filePath)` para detectar `.prettierrc`, luego formatea y devuelve el resultado por stdout
4. El plugin reemplaza el contenido del editor con el resultado

### Format on Save (Ctrl+S)

- Se sobrescribe la acción `SaveAll` de IntelliJ con `overrides="true"`
- **Patrón Save-First**: guarda instantáneamente el archivo original, luego formatea en background thread (~2 segundos), y guarda automáticamente la versión formateada
- Sin bloqueo del hilo de UI (EDT)

---

## 🔍 Troubleshooting

| Problema                 | Solución                                                                                                               |
| ------------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| `node` not found         | Configura la ruta completa en Settings → Tools → Prettier Java → Node.js path (ej: `C:\Program Files\nodejs\node.exe`) |
| Primera vez lenta        | Normal — extrae node_modules del JAR al directorio temporal. Las siguientes veces es inmediato                         |
| Plugin incompatible      | Verifica que tu versión de IntelliJ esté entre 2024.1 y 2025.3                                                         |
| Format on Save no activa | Verifica que el checkbox esté ✅ en Settings → Tools → Prettier Java                                                   |

---

## 📄 Licencia

MIT
