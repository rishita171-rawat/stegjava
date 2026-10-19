const hideForm = document.getElementById("hide-form");
const hideStatus = document.getElementById("hide-status");
const hideResult = document.getElementById("hide-result");
const hidePreview = document.getElementById("hide-preview");
const hideDownload = document.getElementById("hide-download");

let currentImageUrl = null;

// Turns the chosen file into Base64 text.
function readAsBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result.split(",")[1]);
        reader.onerror = () => reject(new Error("Could not read the image."));
        reader.readAsDataURL(file);
    });
}

hideForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    hideStatus.textContent = "";
    hideResult.hidden = true;

    const image = hideForm.elements["image"].files[0];
    const message = hideForm.elements["message"].value;
    const password = hideForm.elements["password"].value;

    if (!image || !message.trim() || !password) {
        hideStatus.textContent = "Please enter an image, message, and password.";
        return;
    }

    if (image.type !== "image/png") {
        hideStatus.textContent = "Please select a PNG image.";
        return;
    }

    const submitButton = hideForm.querySelector('button[type="submit"]');
    submitButton.disabled = true;
    hideStatus.textContent = "Hiding your message...";

    try {
        const body = new URLSearchParams();
        body.append("image", await readAsBase64(image));
        body.append("message", message);
        // The password is collected, but password-based encryption is not implemented yet.

        const response = await fetch("/encode", {
            method: "POST",
            body: body
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "Unable to encode image.");
        }

        const imageBlob = await response.blob();

        if (currentImageUrl) {
            URL.revokeObjectURL(currentImageUrl);
        }

        currentImageUrl = URL.createObjectURL(imageBlob);

        hidePreview.src = currentImageUrl;
        hideDownload.href = currentImageUrl;
        hideResult.hidden = false;

        hideStatus.textContent = "Message hidden successfully! Download your PNG below.";

    } catch (error) {
        hideStatus.textContent = error.message || "An error occurred. Please try again.";
    } finally {
        submitButton.disabled = false;
    }
});