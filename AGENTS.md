# Flujo de desarrollo y publicaciones

- `desarrollo`: funciones en construcción, prototipos y trabajo que todavía no completa el recorrido real. Actividad y Usuario frecuente permanecen acá hasta conectar y validar los datos reales.
- `beta`: funciones completas que se entregan para detectar errores en uso real. No publicar una versión por cada avance parcial de desarrollo.
- `main`: versión estable para uso habitual.
- Agrupar cambios coherentes antes de promover de desarrollo a beta. Comprobar el recorrido completo y documentar límites; una galería con datos ficticios no demuestra que la integración esté terminada.
- Crear o actualizar una rama no implica publicar un APK ni una actualización. No promover automáticamente cada commit de `desarrollo` a `beta`.
- Mantener el requisito de identidad de STM: no presentar un historial vacío ni un contador inventado cuando faltan permisos o datos.
