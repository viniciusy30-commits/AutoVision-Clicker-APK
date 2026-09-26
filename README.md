# AutoVision Clicker

Aplicativo Android nativo em Kotlin para automação visual controlada pelo usuário.

## Requisitos

- Android Studio Koala ou superior
- JDK 17
- Android SDK 35
- Android 10 (API 29) ou superior no dispositivo

## Executar

1. Abra esta pasta no Android Studio.
2. Aguarde a sincronização do Gradle.
3. Execute a configuração `app` em um dispositivo ou emulador.
4. Para automação real, habilite `AutoVision Clicker` em **Configurações > Acessibilidade**.
5. Autorize a captura de tela quando solicitado. O serviço só executa ações iniciadas pelo usuário.

O Vision Lab funciona sem permissões especiais usando o cenário de teste integrado. A importação de imagens usa o seletor de arquivos do Android.

## Arquitetura

`ui → automation → vision → capture/accessibility`

O código está dividido em `ui/`, `accessibility/`, `capture/`, `vision/`, `automation/`, `models/`, `settings/` e `utils/`.