document.addEventListener("DOMContentLoaded", () => {
    const navigationToggle = document.querySelector("[data-navigation-toggle]");
    const navigationLinks = document.querySelector("[data-navigation-links]");
    const currentYear = document.querySelector("[data-current-year]");
    const logoutForm = document.querySelector("[data-navigation-logout]");

    if (currentYear) {
        currentYear.textContent = String(new Date().getFullYear());
    }

    if (logoutForm) {
        logoutForm.addEventListener("submit", async (event) => {
            event.preventDefault();
            const button = logoutForm.querySelector("[data-navigation-logout-button]");
            const status = document.querySelector("[data-navigation-logout-status]");
            button.disabled = true;
            button.textContent = "Logging out...";

            try {
                const response = await fetch(logoutForm.action, {
                    method: "POST",
                    credentials: "same-origin",
                    body: new FormData(logoutForm)
                });
                if (!response.ok) {
                    throw new Error("Logout failed");
                }
                window.localStorage.removeItem("studybuddy.profileDraft");
                window.location.assign("/");
            } catch (error) {
                button.disabled = false;
                button.textContent = "Log out";
                status.textContent = "Unable to log out.";
            }
        });
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
