document.addEventListener("DOMContentLoaded", () => {
    const navigationToggle = document.querySelector("[data-navigation-toggle]");
    const navigationLinks = document.querySelector("[data-navigation-links]");
    const currentYear = document.querySelector("[data-current-year]");

    if (currentYear) {
        currentYear.textContent = String(new Date().getFullYear());
    }

    if (!navigationToggle || !navigationLinks) {
        return;
    }

    navigationToggle.addEventListener("click", () => {
        const isOpen = navigationLinks.classList.toggle("is-open");
        navigationToggle.setAttribute("aria-expanded", String(isOpen));
        document.body.classList.toggle("navigation-open", isOpen);
    });
});
