# System Monitor Widget (Android Nativo)

Aplicativo Android nativo com widget customizado para a tela inicial que exibe métricas em tempo real e lista os processos/aplicativos em execução.

## 🚀 Recursos do Widget
- **Memória RAM**: Exibe porcentagem usada e detalhamento (ex: `5.2 / 8.0 GB`).
- **Armazenamento**: Porcentagem ocupada e espaço livre em GB.
- **Temperatura**: Temperatura da bateria em graus Celsius (°C).
- **Bateria**: Nível de carga e status (Carregando / Bateria).
- **Lista de Processos / Apps**: Lista rolável na tela inicial com ícone do app, nome, pacote e tempo de atividade (`Ativo agora`, `Há 5m`, etc.).
- **Botão de Atualização Instantânea**: Toque no ícone de atualização no widget para recarregar as métricas a qualquer momento.

## ⚙️ Permissões e Segurança
O app utiliza a API nativa do Android `UsageStatsManager` com a permissão:
- `android.permission.PACKAGE_USAGE_STATS`

### Como conceder a permissão:
1. Abra o app **Monitor de Sistema** e clique no botão **"Conceder Permissão"** (você será levado à tela de "Acesso a dados de uso" do Android).
2. Ative a permissão para o aplicativo.
3. *Ou se preferir via ADB no terminal*:
   ```bash
   adb shell appops set com.monitor.widget GET_USAGE_STATS allow
   ```

## 📦 Como Compilar e Instalar o APK

### Opção 1: Via Android Studio (Recomendado)
1. Copie ou abra a pasta `SystemMonitorWidget` no Android Studio.
2. Conecte seu dispositivo ou selecione o target.
3. Clique em **Run** (`Shift + F10`) ou gere o APK em **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

### Opção 2: Linha de comando com Gradle
```bash
cd SystemMonitorWidget
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
