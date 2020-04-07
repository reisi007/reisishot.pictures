define(['jquery'], function (jquery) {
    'use strict';
    $ = jquery;
    window.addEventListener('load', function () {
        // Fetch all the forms we want to apply custom Bootstrap validation styles to
        const forms = document.querySelectorAll('form[r-form]');

        Array.prototype.filter.call(forms, function (form) {
            form.addEventListener('submit', function (event) {
                if (form.checkValidity() === false) {
                    event.preventDefault();
                    event.stopPropagation();
                } else {
                    const formName = form.getAttribute("r-form");
                    console.log(formName);
                    submitForm(formName);
                    event.preventDefault();
                }
                form.classList.add('was-validated');
            }, false);
        });
    }, false);

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
                if (element.type === 'checkbox')
                    data[name] = element.checked ? 'Ja' : 'Nein';
                else if (typeof name === 'string' && ['submit'].indexOf(name) < 0 && element.value && element.value.length > 0)
                    data[name] = element.value;

        }
        const url = "https://api.reisishot.pictures/send.php";

        const request = new XMLHttpRequest();
        request.open("POST", url);
        request.send(JSON.stringify(data));
        form.style = "display: none";
        const element = document.querySelector("[r-form-submitted=\"" + formName + "\"]");
        element.style = "";
    }
});