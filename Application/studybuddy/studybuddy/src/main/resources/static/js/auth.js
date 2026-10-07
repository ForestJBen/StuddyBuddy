document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("[data-auth-form]");
    if (!form) {
        return;
    }

    const status = document.querySelector("[data-form-status]");
    const submitButton = form.querySelector("[data-submit-button]");
    const defaultButtonText = submitButton.textContent;

    form.addEventListener("input", (event) => clearFieldError(form, event.target.name));
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearErrors(form, status);

        const request = Object.fromEntries(new FormData(form).entries());
        if (!validateClientSide(form, request)) {
            showStatus(status, "Please correct the highlighted fields.");
            return;
        }

        setSubmitting(submitButton, true, form.dataset.mode);
        try {
            const response = await fetch(form.dataset.endpoint, {
                method: "POST",
                credentials: "same-origin",
                headers: buildJsonHeaders(),
                body: JSON.stringify(request)
            });
            const body = await readJson(response);

            if (!response.ok) {
                showServerErrors(form, status, body, response.status);
                return;
            }

            if (form.dataset.mode === "register") {
                showStatus(status, "Account created. Taking you to log in…", true);
                window.setTimeout(() => window.location.assign("/login"), 500);
            } else {
                if (new URLSearchParams(window.location.search).has("admin") && body.user?.role !== "ADMIN") {
                    showStatus(status, "This account does not have administrator access.");
                    document.querySelector("[data-regular-entry]").hidden = false;
                    return;
                }
                window.location.assign(body.redirectUrl || "/profile");
            }
        } catch (error) {
            showStatus(status, "Unable to connect to StudyBuddy. Please try again.");
        } finally {
            setSubmitting(submitButton, false, form.dataset.mode, defaultButtonText);
        }
    });
});

function validateClientSide(form, request) {
    let valid = true;
    form.querySelectorAll("input[required]").forEach((input) => {
        if (!input.value.trim()) {
            showFieldError(form, input.name, "This field is required.");
            valid = false;
        }
    });

    const email = form.elements.email;
    if (email.value && !email.validity.valid) {
        showFieldError(form, "email", "Please enter a valid email address.");
        valid = false;
    }

    if (form.dataset.mode === "register") {
        if (request.password && request.password.length < 8) {
            showFieldError(form, "password", "Password must contain at least 8 characters.");
            valid = false;
        }
        if (request.password !== request.confirmPassword) {
            showFieldError(form, "confirmPassword", "Passwords do not match.");
            valid = false;
        }
    }
    return valid;
}

function showServerErrors(form, status, body, responseStatus) {
    Object.entries(body?.fieldErrors || {}).forEach(([field, message]) => {
        showFieldError(form, field, message);
    });
    const fallback = responseStatus === 401
        ? "Invalid email or password."
        : "Unable to complete your request.";
    showStatus(status, body?.message || fallback);
}

function showFieldError(form, fieldName, message) {
    const input = form.elements[fieldName];
    const error = form.querySelector(`[data-field-error="${fieldName}"]`);
    if (input) {
        input.setAttribute("aria-invalid", "true");
    }
    if (error) {
        error.textContent = message;
    }
}

function clearFieldError(form, fieldName) {
    if (!fieldName) {
        return;
    }
    form.elements[fieldName]?.removeAttribute("aria-invalid");
    const error = form.querySelector(`[data-field-error="${fieldName}"]`);
    if (error) {
        error.textContent = "";
    }
}

function clearErrors(form, status) {
    form.querySelectorAll("[aria-invalid]").forEach((input) => input.removeAttribute("aria-invalid"));
    form.querySelectorAll("[data-field-error]").forEach((error) => error.textContent = "");
    status.hidden = true;
    status.textContent = "";
    status.classList.remove("is-success");
}

function showStatus(status, message, success = false) {
    status.textContent = message;
    status.classList.toggle("is-success", success);
    status.hidden = false;
}

function setSubmitting(button, submitting, mode, defaultText) {
    button.disabled = submitting;
    button.textContent = submitting
        ? mode === "register" ? "Creating account…" : "Logging in…"
        : defaultText;
}

async function readJson(response) {
    const contentType = response.headers.get("content-type") || "";
    return contentType.includes("application/json") ? response.json() : null;
}

function buildJsonHeaders() {
    const headers = { "Content-Type": "application/json" };
    const token = document.querySelector('meta[name="_csrf"]')?.content;
    const header = document.querySelector('meta[name="_csrf_header"]')?.content;
    if (token && header) {
        headers[header] = token;
    }
    return headers;
}
