# Flujo de desarrollo y publicaciones

- `desarrollo`: funciones en construcción, prototipos y trabajo que todavía no completa el recorrido real. Actividad y Usuario frecuente permanecen acá hasta conectar y validar los datos reales.
- `beta`: funciones completas que se entregan para detectar errores en uso real. No publicar una versión por cada avance parcial de desarrollo.
- `main`: versión estable para uso habitual.
- `lab`: laboratorio personal del dueño (Boletera Lab). Cambios bruscos y frecuentes, puede romperse; no se recomienda a nadie. Es otra app (`uy.boletera.lab`, ícono violeta) que se instala al lado de Boletera y nunca la reemplaza. Se publica como pre-release con tag `v<versión>-lab.N` y el APK `boletera-lab-<versión>-lab.N.apk`; solo la ofrecen las apps desde 0.2.40 Alpha 5, las anteriores no reconocen ese tag. Lo que funciona en Lab pasa a `alpha` por el recorrido normal; nada salta de Lab a beta o main.
- Agrupar cambios coherentes antes de promover de desarrollo a beta. Comprobar el recorrido completo y documentar límites; una galería con datos ficticios no demuestra que la integración esté terminada.
- Crear o actualizar una rama no implica publicar un APK ni una actualización. No promover automáticamente cada commit de `desarrollo` a `beta`.
- Mantener el requisito de identidad de STM: no presentar un historial vacío ni un contador inventado cuando faltan permisos o datos.
