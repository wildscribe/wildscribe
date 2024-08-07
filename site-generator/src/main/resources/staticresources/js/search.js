(() => {
    "use strict";

    window.addEventListener("load", () => {
        const searchResults = document.querySelector("#search-results");
        const searchModal = document.querySelector("#search-modal");
        const searchButton = document.querySelector("#search-button");
        const searchData = document.querySelector("#search-data");
        searchModal.addEventListener("hide.bs.modal", clearResults);
        searchModal.addEventListener("shown.bs.modal", () => {
            searchData.focus();
        });

        /**
         * Find the context path
         * @param contextPath the context path to check
         * @returns {Promise<* | Response | String>}
         */
        const findContextPath = (contextPath) => {
            let url;
            if (contextPath.endsWith("/")) {
                url = contextPath + "search-index.json";
            } else {
                url = contextPath + "/search-index.json";
            }
            return fetch(url)
                .then(response => {
                    if (response.ok) {
                        sessionStorage.setItem("contextPath", contextPath);
                        return contextPath;
                    } else if (response.status === 404) {
                        const i = contextPath.lastIndexOf("/");
                        if (i > 0) {
                            let newContext = contextPath.substring(0, i);
                            return findContextPath(newContext);
                        } else {
                            return "/";
                        }
                    }
                })
                .catch(error => {
                    console.error("Failed find context path. ", error);
                });
        };

        // Find the context path
        let contextPathPromise;
        if (!sessionStorage.getItem("contextPath")) {
            let contextPath = window.location.pathname;
            if (contextPath.endsWith("/index.html")) {
                contextPath = contextPath.substring(0, contextPath.lastIndexOf("/index.html"));
            }
            contextPathPromise = findContextPath(contextPath);
        } else {
            contextPathPromise = Promise.resolve(sessionStorage.getItem("contextPath"));
        }

        let index;
        if (sessionStorage.getItem("wildscribe-index")) {
            index = lunr.Index.load(JSON.parse(sessionStorage.getItem("wildscribe-index")));
            searchButton.classList.remove("visually-hidden");
        } else {
            contextPathPromise.then(contextPath => {
                let url;
                if (contextPath.endsWith("/")) {
                    url = contextPath + "search-index.json";
                } else {
                    url = contextPath + "/search-index.json";
                }
                fetch(url)
                    .then(response => {
                        if (response.status === 200) {
                            response.json()
                                .then(json => {
                                    index = lunr(function () {
                                        this.field("attribute", {boost: 10});
                                        this.field("description");
                                        for (let x in json) {
                                            const item = json[x];
                                            sessionStorage.setItem(item.id, JSON.stringify(item));
                                            this.add(item);
                                        }
                                    });
                                    sessionStorage.setItem("wildscribe-index", JSON.stringify(index));
                                    searchButton.classList.remove("visually-hidden");
                                });
                        }
                    });

            });
        }

        searchData.addEventListener("keyup", (event) => {
            if (event.isComposing) {
                return;
            }
            if (index) {
                if (event.target.value.length > 2) {
                    const searchTerm = searchData.value + "*";
                    const results = index.search(searchTerm);
                    showResults(results, sessionStorage);
                } else {
                    clearResults();
                }
            }
        });


        function showResults(results, store) {
            clearResults();
            if (results.length) {
                for (let i = 0; i < results.length; i++) {
                    const json = JSON.parse(store.getItem(results[i].ref));
                    const link = document.createElement("a");
                    link.addEventListener("click", () => {
                        bootstrap.Modal.getOrCreateInstance(searchModal).hide();
                        searchData.value = "";
                    });
                    let contextPath = sessionStorage.getItem("contextPath");
                    if (contextPath.endsWith("/")) {
                        contextPath = contextPath.substring(0, contextPath.lastIndexOf("/"));
                    }
                    const url = contextPath + "/" + json.url + "#attr-" + json.attribute.replace(/['"]+/g, '');
                    link.setAttribute("href", `${url}`);
                    link.classList.add("list-group-item", "list-group-item-action");

                    // Create the content for the link
                    let desc = json.description;
                    if (desc.length > 250) {
                        desc = desc.substring(0, 250) + "...";
                    }
                    link.innerHTML = `
                        <div class="d-flex w-100 justify-content-between">
                            <h5 class="mb-1">${json.attribute}</h5>
                            <small>${json.url}</small>
                        </div>
                        <p class="mb-1">${desc}</p>
                    `;

                    searchResults.appendChild(link);
                }
            } else {
                searchResults.innerHTML = 'No Results';
            }
        }

        function clearResults() {
            // Clear the results
            while (searchResults.firstChild) {
                searchResults.removeChild(searchResults.firstChild);
            }
        }
    });
})();