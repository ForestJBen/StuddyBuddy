const profileConfiguration = {
    courseEndpoint: "/api/courses",
    profileEndpoint: "/api/profile",
    profileApiEnabled: true,
    localDraftKey: "studybuddy.profileDraft"
};

const fallbackCourses = [
    { id: 1, courseCode: "COMP101", courseName: "Introduction to Programming" },
    { id: 2, courseCode: "COMP161", courseName: "Foundations of Computing" },
    { id: 3, courseCode: "COMP261", courseName: "Software Development" },
    { id: 4, courseCode: "STAT101", courseName: "Introduction to Statistics" }
];

document.addEventListener("DOMContentLoaded", async () => {
    const form = document.querySelector("[data-profile-form]");

    if (!form) {
        return;
    }

    const page = createProfilePage(form);
    page.bindEvents();

    const courses = await loadCourses(page);
    page.setCourses(courses);

    const profile = await loadProfile(page);
    page.populate(profile);
});

function createProfilePage(form) {
    const fields = {
        programme: form.elements.programme,
        year: form.elements.year,
        goals: form.elements.goals,
        studyStyle: form.elements.studyStyle,
        availability: form.elements.availability
    };
    const courseOptions = form.querySelector("[data-course-options]");
    const courseStatus = form.querySelector("[data-course-status]");
    const saveStatus = form.querySelector("[data-save-status]");
    const saveButton = form.querySelector("[data-save-button]");
    const logoutButton = form.querySelector("[data-logout-button]");
    let availableCourses = [];

    function bindEvents() {
        form.addEventListener("input", () => {
            clearFieldErrors();
            updateCharacterCounts();
            updateSummary();
        });
        form.addEventListener("change", updateSummary);
        form.addEventListener("submit", submitProfile);
        logoutButton.addEventListener("click", logout);
    }

    function setCourses(courses) {
        availableCourses = courses;
        courseOptions.replaceChildren();

        courses.forEach((course) => {
            const label = document.createElement("label");
            label.className = "course-option";

            const checkbox = document.createElement("input");
            checkbox.type = "checkbox";
            checkbox.name = "courseIds";
            checkbox.value = String(course.id);

            const copy = document.createElement("span");
            const code = document.createElement("strong");
            const name = document.createElement("small");
            code.textContent = course.courseCode;
            name.textContent = course.courseName;
            copy.append(code, name);
            label.append(checkbox, copy);
            courseOptions.append(label);
        });
    }

    function populate(profile) {
        fields.programme.value = profile.programme ?? "";
        fields.year.value = profile.year ? String(profile.year) : "";
        fields.goals.value = profile.goals ?? "";
        fields.studyStyle.value = profile.studyStyle ?? "";
        fields.availability.value = profile.availability ?? "";

        const selectedIds = new Set((profile.courses ?? []).map((course) => String(course.id)));
        form.querySelectorAll('input[name="courseIds"]').forEach((checkbox) => {
            checkbox.checked = selectedIds.has(checkbox.value);
        });

        updateCharacterCounts();
        updateSummary();
    }

    function getRequestBody() {
        return {
            programme: fields.programme.value.trim(),
            year: fields.year.value ? Number(fields.year.value) : null,
            courseIds: getSelectedCourseIds(),
            goals: fields.goals.value.trim(),
            studyStyle: fields.studyStyle.value,
            availability: fields.availability.value.trim()
        };
    }

    function getSelectedCourseIds() {
        return Array.from(form.querySelectorAll('input[name="courseIds"]:checked'), (checkbox) => Number(checkbox.value));
    }

    function getSelectedCourses() {
        const selectedIds = new Set(getSelectedCourseIds());
        return availableCourses.filter((course) => selectedIds.has(course.id));
    }

    function validate(request) {
        const errors = {};

        if (!request.programme) {
            errors.programme = "Programme is required.";
        }

        if (!request.year || request.year < 1 || request.year > 10) {
            errors.year = "Please select your year of study.";
        }

        showFieldErrors(errors);
        return Object.keys(errors).length === 0;
    }

    async function submitProfile(event) {
        event.preventDefault();
        const request = getRequestBody();

        if (!validate(request)) {
            setSaveStatus("Please correct the highlighted fields.", "error");
            return;
        }

        setSaving(true);

        try {
            if (profileConfiguration.profileApiEnabled) {
                const response = await requestJson(profileConfiguration.profileEndpoint, {
                    method: "PUT",
                    headers: buildJsonHeaders(),
                    body: JSON.stringify(request)
                });
                populate(response);
                setSaveStatus("Profile saved successfully.", "success");
            } else {
                const localProfile = { ...request, courses: getSelectedCourses() };
                window.localStorage.setItem(profileConfiguration.localDraftKey, JSON.stringify(localProfile));
                setSaveStatus("Draft saved in this browser. Backend saving is not connected yet.", "success");
            }
        } catch (error) {
            handleSaveError(error);
        } finally {
            setSaving(false);
        }
    }

    function handleSaveError(error) {
        if (error.fieldErrors) {
            showFieldErrors(error.fieldErrors);
        }
        setSaveStatus(error.message || "Unable to save the profile.", "error");
    }

    function showFieldErrors(errors) {
        Object.entries(errors).forEach(([fieldName, message]) => {
            const errorElement = form.querySelector(`[data-field-error="${fieldName}"]`);
            const input = fields[fieldName];

            if (errorElement) {
                errorElement.textContent = message;
            }
            if (input) {
                input.setAttribute("aria-invalid", "true");
            }
        });
    }

    function clearFieldErrors() {
        form.querySelectorAll("[data-field-error]").forEach((element) => {
            element.textContent = "";
        });
        Object.values(fields).forEach((input) => input.removeAttribute("aria-invalid"));
    }

    function updateCharacterCounts() {
        ["goals", "availability"].forEach((fieldName) => {
            const counter = form.querySelector(`[data-character-count="${fieldName}"]`);
            counter.textContent = `${fields[fieldName].value.length}/${fields[fieldName].maxLength}`;
        });
    }

    function updateSummary() {
        const request = getRequestBody();
        const selectedCourses = getSelectedCourses();
        setSummary("programme", request.programme || "Not added");
        setSummary("year", request.year ? `Year ${request.year}` : "Not added");
        setSummary("courses", selectedCourses.length ? selectedCourses.map((course) => course.courseCode).join(", ") : "No courses selected");
        setSummary("studyStyle", request.studyStyle || "Not added");
        setSummary("availability", request.availability || "Not added");

        const completed = [request.programme, request.year, request.courseIds.length, request.goals, request.studyStyle, request.availability]
            .filter(Boolean).length;
        document.querySelector("[data-profile-progress]").textContent = `${Math.round((completed / 6) * 100)}%`;
    }

    function setSummary(name, value) {
        document.querySelector(`[data-summary="${name}"]`).textContent = value;
    }

    function setCourseStatus(message, warning = false) {
        courseStatus.textContent = message;
        courseStatus.classList.toggle("is-warning", warning);
    }

    function setSaveStatus(message, type = "") {
        saveStatus.textContent = message;
        saveStatus.className = `save-status${type ? ` is-${type}` : ""}`;
    }

    function setSaving(isSaving) {
        saveButton.disabled = isSaving;
        saveButton.textContent = isSaving ? "Saving..." : profileConfiguration.profileApiEnabled ? "Save profile" : "Save profile draft";
    }

    async function logout() {
        logoutButton.disabled = true;
        logoutButton.textContent = "Logging out...";
        setSaveStatus("Logging you out...");

        try {
            const response = await fetch("/api/auth/logout", {
                method: "POST",
                credentials: "same-origin",
                headers: buildJsonHeaders()
            });

            if (!response.ok) {
                throw new Error("Logout failed");
            }

            window.localStorage.removeItem(profileConfiguration.localDraftKey);
            window.location.assign("/");
        } catch (error) {
            logoutButton.disabled = false;
            logoutButton.textContent = "Log out";
            setSaveStatus("Unable to log out. Please try again.", "error");
        }
    }

    return { bindEvents, populate, setCourses, setCourseStatus };
}

async function loadCourses(page) {
    try {
        const courses = await requestJson(profileConfiguration.courseEndpoint);
        page.setCourseStatus("Courses loaded from the Course API.");
        return courses;
    } catch (error) {
        page.setCourseStatus("Course API is not available yet. Showing temporary course data.", true);
        return fallbackCourses;
    }
}

async function loadProfile(page) {
    if (profileConfiguration.profileApiEnabled) {
        try {
            return await requestJson(profileConfiguration.profileEndpoint);
        } catch (error) {
            page.setCourseStatus("The Profile API could not be loaded. Using the local draft.", true);
        }
    }

    const savedDraft = window.localStorage.getItem(profileConfiguration.localDraftKey);
    if (!savedDraft) {
        return emptyProfile();
    }

    try {
        return JSON.parse(savedDraft);
    } catch (error) {
        window.localStorage.removeItem(profileConfiguration.localDraftKey);
        return emptyProfile();
    }
}

async function requestJson(url, options = {}) {
    const response = await fetch(url, { credentials: "same-origin", ...options });
    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json") ? await response.json() : null;

    if (!response.ok) {
        throw body || { message: `Request failed with status ${response.status}.` };
    }

    if (!body) {
        throw { message: "The server did not return JSON." };
    }

    return body;
}

function buildJsonHeaders() {
    const headers = { "Content-Type": "application/json" };
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    if (csrfToken && csrfHeader) {
        headers[csrfHeader] = csrfToken;
    }

    return headers;
}

function emptyProfile() {
    return {
        id: null,
        programme: "",
        year: null,
        courses: [],
        goals: "",
        studyStyle: "",
        availability: ""
    };
}
