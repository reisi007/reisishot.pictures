function submitForm(formName) {
    const form = document.querySelector("[r-form=\"" + formName + "\"]");
    const data = {};
    for (let element of form.querySelectorAll("[name]")) {
        const name = element.name;
        if (
            element instanceof HTMLInputElement ||
            element instanceof HTMLTextAreaElement ||
            element instanceof HTMLSelectElement
        )
            if (typeof name === 'string' && ['submit'].indexOf(name) < 0 && element.value.length > 0)
                data[name] = element.value;
            else if (element instanceof HTMLInputElement && element.type === 'checkbox')
                data[name] = element.checked ? 'Ja' : 'Nein';
    }

    const url = "https://api.reisishot.pictures/send.php";

    const request = new XMLHttpRequest();
    request.open("POST", url);
    request.send(JSON.stringify(data));
    form.style = "display: none";
    const element = document.querySelector("[r-form-submitted=\"" + formName + "\"]");
    element.style = "";
}