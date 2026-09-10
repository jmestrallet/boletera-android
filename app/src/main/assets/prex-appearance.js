(() => {
  if (location.origin !== 'https://pasarelaspe.sistarbanc.com.uy' || !location.pathname.startsWith('/v2/') || window.top !== window.self) return;
  if (document.getElementById('boletera-payment-appearance')) return;
  const style = document.createElement('style');
  style.id = 'boletera-payment-appearance';
  style.textContent = `
    body {background:#f4f5ef!important;color:#101d2a;font-family:system-ui,sans-serif}
    .mat-card,mat-card {border-radius:20px!important;box-shadow:none!important}
    .mat-form-field,mat-form-field {font-family:system-ui,sans-serif!important}
    .mat-raised-button,.mat-flat-button {border-radius:12px!important;min-height:46px}
    .mat-raised-button.mat-primary,.mat-flat-button.mat-primary {background:#b9f375!important;color:#101d2a!important}
    input {font-size:16px!important}
    .mat-form-field-label {font-size:14px}
  `;
  document.head.append(style);
})();
