# Compatibilidad TLS de Sistarbanc en Android 11

Fecha: 10 de septiembre de 2026. Equipo físico: OnePlus 6T, Android 11, WebView 150.0.7871.181.

## Problema reproducido

Al avanzar al pago con 0.1.8 apareció el rechazo de conexión segura. Una app de diagnóstico separada, sin cuentas ni operación, reprodujo en `spf.sistarbanc.com.uy` y `pasarelaspe.sistarbanc.com.uy` el fallo `SSLHandshakeException` en HTTPS y `sslError=3` en WebView.

Los servidores entregaron su certificado, Sectigo Public Server Authentication CA DV R36 y la raíz autofirmada R46. Esa raíz no permitió construir una cadena de confianza con el almacén de certificados extraído del teléfono. No se modificó ese almacén.

## Corrección acotada

Se incorporó el certificado oficial Sectigo Public Server Authentication Root R46 firmado por USERTrust RSA, anunciado por [Sectigo](https://www.sectigo.com/knowledge-base/detail/Sectigo-new-Public-Roots-and-Issuing-CAs-Hierarchy). Descarga oficial: `http://crt.sectigo.com/SectigoPublicServerAuthenticationRootR46_USERTrust.crt`. El contenido se autenticó verificando su cadena criptográfica contra las raíces ya presentes en el teléfono; el transporte de descarga no fue la base de confianza.

SHA-256 del certificado DER: `92f351bf3d54164dfa8dd8f9e1139d3150349786485d2b9eecd00e2971c1e6c5`.

La configuración Android agrega ese certificado como ancla adicional exclusivamente para los dos nombres exactos indicados, sin subdominios. Los demás destinos conservan su política anterior. No se agrega al almacén del sistema y los manejadores TLS continúan cancelando ante un error.

## Evidencia

- OpenSSL con el almacén real del teléfono rechazó ambas cadenas entregadas por los servidores.
- Ambas cadenas validaron al aportar el certificado cruzado oficial, con comprobación de nombre de servidor. Un nombre deliberadamente incorrecto fue rechazado.
- La app de diagnóstico, con la misma configuración XML y certificados que la corrección, obtuvo HTTP 200 por HTTPS en ambos dominios y terminó ambas cargas WebView sin errores TLS.
- El diagnóstico solo hizo HEAD/GET de las raíces públicas, sin cookies, JavaScript, identificadores de operación ni formularios. La app de Boletera y su solicitud pendiente se conservaron.

Estas comprobaciones acreditan la corrección TLS en el equipo. No acreditan CAPTCHA, autocompletado de Google, autorización bancaria ni acreditación.
