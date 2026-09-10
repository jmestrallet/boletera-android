(() => {
  if (location.origin !== 'https://pasarelaspe.sistarbanc.com.uy' || !location.pathname.startsWith('/v2/') || window.top !== window.self || window.BoleteraVerification) return;
  let component = null;
  let style = null;
  function restore() {
    component?.removeAttribute('data-boletera-verification');
    style?.remove(); component = null; style = null;
  }
  window.BoleteraVerification = {
    restore,
    present(frame) {
      const root = frame?.closest('angular-recaptcha');
      if (!root || !root.closest('alta-cliente')) return false;
      if (component === root && root.isConnected) return true;
      restore();
      component = root;
      // Position the EXISTING component. Never move/recreate the iframe, load its
      // source separately, inspect its contents or replace the provider callback.
      component.setAttribute('data-boletera-verification', 'true');
      style = document.createElement('style');
      style.textContent = `[data-boletera-verification="true"]{position:fixed!important;top:0!important;left:0!important;right:auto!important;bottom:auto!important;display:block!important;width:max-content!important;max-width:none!important;min-width:0!important;height:auto!important;min-height:0!important;margin:0!important;padding:0!important;z-index:2147480000!important}`;
      document.head.append(style);
      return true;
    }
  };
})();
