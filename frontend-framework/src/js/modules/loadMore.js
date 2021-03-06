define(['trackAction'], function (trackAction) {
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll("[data-partial]").forEach((cur) => {
            const partialName = cur.getAttribute("data-partial") || "???"
            const initial = parseInt(cur.getAttribute("data-initial"));
            const step = parseInt(cur.getAttribute("data-step"));
            const children = cur.children;
            let showCount = initial;
            if (cur.children.length <= initial) {
                return
            }

            const loadMoreButton = document.createElement("BUTTON");
            loadMoreButton.type = "button"
            loadMoreButton.classList.add("btn", "btn-primary")
            loadMoreButton.innerText = "Mehr anzeigen"
            loadMoreButton.style.display = "block"
            loadMoreButton.style.margin = "0 auto"
            loadMoreButton.onclick = () => {
                const oldShowCount = showCount;
                showCount += step;
                showUntil(oldShowCount, showCount)
            }
            cur.insertAdjacentElement("afterend", loadMoreButton)
                showUntil(null, showCount)

                function showUntil(oldIdx, idx) {
                    for (let i = 0; i < children.length; i++) {
                        const el = children[i];
                        if (i >= idx)
                            el.style.display = "none"
                        else
                            el.style.display = ""
                    }
                    if (idx >= children.length) {
                        loadMoreButton.style.display = "none";
                        trackAction({group: "loadMore", action: 'allLoaded', id: partialName})
                    }
                    if (oldIdx != null && oldIdx > 0) {
                        trackAction({group: "loadMore", action: 'loaded', id: partialName, value: idx - 1})
                        const child = children[oldIdx];
                        child.focus()
                        child.scrollIntoView({
                            transition: "smooth",
                            block: "end"
                        })
                    }
                }
            }
        )
    })
})