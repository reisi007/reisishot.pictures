define(function () {
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll("[data-partial]").forEach((cur) => {
                const initial = parseInt(cur.getAttribute("data-initial"));
                const step = parseInt(cur.getAttribute("data-step"));
                const children = cur.children;
                let showCount = initial;

                const loadMoreButton = document.createElement("BUTTON");
                loadMoreButton.type = "button"
                loadMoreButton.classList.add("btn", "btn-primary")
                loadMoreButton.innerText = "Mehr anzeigen"
                loadMoreButton.style.display = "block"
                loadMoreButton.style.margin = "0 auto"
                loadMoreButton.onclick = () => {
                    showCount += step;
                    showUntil(showCount)
                }
                cur.insertAdjacentElement("afterend", loadMoreButton)
                showUntil(showCount)

                function showUntil(idx) {
                    for (let i = 0; i < children.length; i++) {
                        const el = children[i];
                        if (i >= idx)
                            el.style.display = "none"
                        else
                            el.style.display = ""
                    }
                    if (idx >= children.length)
                        loadMoreButton.style.display = "none";
                }
            }
        )
    })
})