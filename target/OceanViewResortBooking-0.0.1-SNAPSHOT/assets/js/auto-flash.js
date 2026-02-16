
(function() {

  const originalFetch = window.fetch;

  window.fetch = function(...args) {
    return originalFetch.apply(this, args)
      .then(async function(response) {
  
        const clonedResponse = response.clone();
 
        const method = args[1]?.method?.toUpperCase() || 'GET';
        const isMutation = ['POST', 'PUT', 'DELETE'].includes(method);

        const url = args[0];
        const isApiCall = typeof url === 'string' && url.includes('/api/') && !url.includes('/api/flash');
 
        if (isMutation && isApiCall) {
          try {
        
            setTimeout(function() {
              if (window.loadFlashMessages) {
                window.loadFlashMessages();
              }
            }, 100);
          } catch (e) {
            console.error('Error loading flash messages:', e);
          }
        }
        
        return response;
      });
  };

  console.log('Auto-flash initialized - Flash messages will load automatically after API operations');
})();