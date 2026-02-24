// auto-flash.js
(function () {

  const originalFetch = window.fetch;

  window.fetch = function (...args) {
    return originalFetch.apply(this, args)
      .then(function (response) {

        const method = args[1]?.method?.toUpperCase() || 'GET';
        const isMutation = ['POST', 'PUT', 'DELETE'].includes(method);

        const url = args[0];
        const isApiCall = typeof url === 'string' && url.includes('/api/') && !url.includes('/api/flash');

        if (isMutation && isApiCall) {
          setTimeout(function () {
            if (window.loadFlashMessages) {
              window.loadFlashMessages();
            }
          }, 100);
        }

        return response;
      });
  };

})();
