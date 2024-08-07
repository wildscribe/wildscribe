(() => {
   "use strict";

   window.addEventListener("load", () => {
      // Allow HTML data in popover's
      const popovers = document.querySelectorAll("[data-bs-toggle='popover']");
      popovers.forEach((e) => {
         let opts = {};
         if (e.hasAttribute("data-bs-target")) {
            opts.html = true;
            opts.content = document.querySelector(e.getAttribute("data-bs-target")).innerHTML;
         }
         new bootstrap.Popover(e, opts);
      });

      // Show the collapsed attribute if found
      if (window.location.hash) {
         const hash = window.location.hash;
         const i = hash.indexOf("#attr-");
         if (i === 0) {
            const attr = document.querySelector(`#attribute-${hash.substring(6)}`);
            if (attr) {
               bootstrap.Collapse.getOrCreateInstance(attr).show();
            }
         }
      }
   });
})()

